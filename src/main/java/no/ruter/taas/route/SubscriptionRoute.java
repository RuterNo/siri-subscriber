package no.ruter.taas.route;

import static no.ruter.taas.route.Constants.AMQ_PRODUCER_PARAM;
import static no.ruter.taas.route.Constants.SINGLETON_ROUTE_DEFINITION_GROUP_NAME;

import java.net.ConnectException;
import java.time.Instant;
import java.util.List;
import javax.ws.rs.core.MediaType;
import no.ruter.taas.route.dataformat.SiriDataFormat;
import no.ruter.taas.service.SubscriptionService;
import no.ruter.taas.siri.SiriRequestFactory;
import no.ruter.taas.siri.SiriRequestType;
import no.ruter.taas.siri.Subscription;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.component.hazelcast.policy.HazelcastRoutePolicy;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.spi.RouteContext;
import org.apache.camel.spi.RoutePolicy;
import org.apache.camel.spring.SpringRouteBuilder;
import org.springframework.http.HttpMethod;

public class SubscriptionRoute extends SpringRouteBuilder {

  private SubscriptionService service;
  private Instant lastCheckStatus = Instant.now();
  private boolean hasBeenStarted;
  private Subscription subscription;

  public SubscriptionRoute(SubscriptionService service, Subscription subscription) {
    this.service = service;
    this.subscription = subscription;
  }

  /**
   * Create a new singleton route definition from URI. Only one such route should be active
   * throughout the cluster at any time.
   */
  private RouteDefinition singletonFrom(String uri, String routeId) {
    return this.from(uri)
        .group(SINGLETON_ROUTE_DEFINITION_GROUP_NAME)
        .routeId(routeId)
        .autoStartup(true);
  }

  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  private boolean isLeader(String routeId) {
    RouteContext routeContext = getContext().getRoute(routeId).getRouteContext();
    List<RoutePolicy> routePolicyList = routeContext.getRoutePolicyList();
    if (routePolicyList != null) {
      for (RoutePolicy routePolicy : routePolicyList) {
        if (routePolicy instanceof HazelcastRoutePolicy) {
          return ((HazelcastRoutePolicy) (routePolicy)).isLeader();
        }
      }
    }
    return false;
  }

  private boolean shouldCheckStatus(String routeId) {
    if (!isLeader(routeId)) {
      return false;
    }
    boolean isActive = service.isActivated(subscription.getSubscriptionId());
    boolean requiresCheckStatusRequest =
        subscription.getUrlMap().get(SiriRequestType.CHECK_STATUS) != null;
    boolean isTimeToCheckStatus = lastCheckStatus
        .isBefore(Instant.now().minus(subscription.getHeartbeatInterval()));

    // todo: fix this
//    return isActive && requiresCheckStatusRequest && isTimeToCheckStatus;

    return false;
  }

  private boolean shouldBeStarted(String routeId) {
    if (!isLeader(routeId)) {
      return false;
    }
    return (service.isActivated(subscription.getSubscriptionId()) && !hasBeenStarted);
  }

  private boolean shouldBeCancelled(String routeId) {
    if (!isLeader(routeId)) {
      return false;
    }

    boolean isActive = service.isActivated(subscription.getSubscriptionId());
    boolean isHealthy = service.isHealthy(subscription.getSubscriptionId());

    return (hasBeenStarted & !isActive) | (hasBeenStarted & isActive & !isHealthy);
  }

  private void initTriggerRoutes() {
    if (service.getSubscription(subscription.getSubscriptionId()) != null) {
      hasBeenStarted = service.isActivated(subscription.getSubscriptionId());
    } else {
      hasBeenStarted = subscription.isActive();
    }

    // @formatter:off
    singletonFrom("quartz2://subscriber/monitor_" + subscription.getSubscriptionId()
            + "?fireNow=true&trigger.repeatInterval=" + 15000,
        "monitor.subscription." + subscription.getSubscriptionId())

        .choice()
        .when(p -> shouldBeStarted(p.getFromRouteId()))
          .log("Triggering start subscription: " + subscription)
          .process(p -> hasBeenStarted = true)
          .to("direct:" + subscription.getStartRoute())

        .when(p -> shouldBeCancelled(p.getFromRouteId()))
          .log("Triggering cancel subscription: " + subscription)
          .process(p -> hasBeenStarted = false)
          .to("direct:" + subscription.getCancelRoute())

        .when(p -> shouldCheckStatus(p.getFromRouteId()))
          .log("Check status request to: " + subscription)
          .process(p -> lastCheckStatus = Instant.now())
          .to("direct:" + subscription.getCheckStatusRoute())

        .end()
    ;
    // @formatter:on
  }

  @Override
  public void configure() throws Exception {
    final SiriRequestFactory factory = new SiriRequestFactory(subscription);
    // @formatter:off

    onException(ConnectException.class)
        .maximumRedeliveries(5)
        .redeliveryDelay(10000)
        .useExponentialBackOff()
    ;

    // start
    from("direct:" + subscription.getStartRoute())
        .routeId(subscription.getStartRoute())

        .log("Starting " + subscription.getName() + " - " + subscription.getSubscriptionId())
        .bean(factory, "createSubscriptionRequest()", false)
        .marshal(SiriDataFormat.getSiriJaxbDataformat())
        .removeHeaders("*")

        .setHeader(Exchange.CONTENT_TYPE, constant(MediaType.TEXT_XML))
        .setHeader(Exchange.HTTP_METHOD, constant(HttpMethod.POST))
        .setHeader(Exchange.CONTENT_ENCODING, constant(subscription.getEncoding()))
        .setHeader("endpoint", constant("undertow:" + subscription.getSubscribeEndpoint()))

        .toD("${header.endpoint}")
        .log("SubscriptionRequest: HTTP status code: ${in.header.CamelHttpResponseCode}")

        .choice()
          .when(p -> p.getIn().getBody() != null || !p.getIn().getBody().toString().isEmpty())
            .to("activemq:queue:" + Constants.SUBSCRIPTION_RESPONSE + AMQ_PRODUCER_PARAM)
    ;

    // cancel
    from("direct:" + subscription.getCancelRoute())
        .routeId(subscription.getCancelRoute())

        .log("Cancel: " + subscription.getName() + " - " + subscription.getSubscriptionId())
        .bean(factory, "createSiriTerminationRequest()", false)
        .marshal(SiriDataFormat.getSiriJaxbDataformat())
        .removeHeaders("*")

        .setHeader(Exchange.CONTENT_TYPE, constant(MediaType.TEXT_XML))
        .setHeader(Exchange.CONTENT_ENCODING, constant(subscription.getEncoding()))
        .setHeader(Exchange.HTTP_METHOD, constant(HttpMethod.POST))
        .setHeader("endpointCancel", constant("undertow:" +
            subscription.getTerminationEndpoint()))

        .toD("${header.endpointCancel}")
        .log("Cancel: HTTP status code: ${in.header.CamelHttpResponseCode}")
        .process(p -> service.stopSubscription(subscription.getSubscriptionId()))
    ;

    // check status
    from("direct:" + subscription.getCheckStatusRoute())
        .routeId(subscription.getCheckStatusRoute())

        .log("CheckStatus: " + subscription.getName() + " - " + subscription.getSubscriptionId())
        .bean(factory, "createSiriCheckStatusRequest()", false)
        .marshal(SiriDataFormat.getSiriJaxbDataformat())
        .removeHeaders("*")

        .setHeader(Exchange.CONTENT_TYPE, constant(MediaType.TEXT_XML))
        .setHeader(Exchange.CONTENT_ENCODING, constant(subscription.getEncoding()))
        .setHeader(Exchange.HTTP_METHOD, constant(HttpMethod.POST))
        .setHeader("endpointCheckstatus", constant("undertow:" +
            subscription.getCheckStatusEndpoint()))

        .toD("${header.endpointCheckstatus}")
        .log("CheckStatus: HTTP status code: ${in.header.CamelHttpResponseCode}")
    ;

    // @formatter:on

    initTriggerRoutes();
  }
}
