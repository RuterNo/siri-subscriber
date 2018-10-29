package no.ruter.taas.route;


import static no.ruter.taas.route.Constants.AMQ_CONSUMER_PARAM;
import static no.ruter.taas.route.Constants.AMQ_PRODUCER_PARAM;
import static no.ruter.taas.route.Constants.CHECK_RESPONSE_XPATH;
import static no.ruter.taas.route.Constants.CHECK_STATUS_QUEUE;
import static no.ruter.taas.route.Constants.ESTIMATED_TIMETABLE_QUEUE;
import static no.ruter.taas.route.Constants.ET_XPATH;
import static no.ruter.taas.route.Constants.HEARTBEAT_QUEUE;
import static no.ruter.taas.route.Constants.HEARTBEAT_XPATH;
import static no.ruter.taas.route.Constants.PRODUCER_XPATH;
import static no.ruter.taas.route.Constants.ROUTER_QUEUE;
import static no.ruter.taas.route.Constants.SITUATION_EXCHANGE_QUEUE;
import static no.ruter.taas.route.Constants.SUBSCRIPTION_RESPONSE;
import static no.ruter.taas.route.Constants.SUBSCRIPTION_RESPONSE_XPATH;
import static no.ruter.taas.route.Constants.SX_XPATH;
import static no.ruter.taas.route.Constants.TERMINATION_RESPONSE;
import static no.ruter.taas.route.Constants.TERMINATION_RESPONSE_XPATH;
import static no.ruter.taas.route.Constants.TOKENIZE_ET;
import static no.ruter.taas.route.Constants.TOKENIZE_SX;
import static no.ruter.taas.route.Constants.TOKENIZE_VM;
import static no.ruter.taas.route.Constants.VEHICLE_MONITORING_QUEUE;
import static no.ruter.taas.route.Constants.VM_XPATH;

import java.io.InputStream;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.UnmarshalException;
import lombok.extern.slf4j.Slf4j;
import no.ruter.taas.route.dataformat.SiriDataFormat;
import no.ruter.taas.service.SiriMapperService;
import no.ruter.taas.service.SubscriptionService;
import no.ruter.taas.siri.Subscription;
import no.ruter.taas.siri20.util.SiriXml;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.InvalidPayloadException;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.xml.Namespaces;
import org.apache.camel.spring.SpringRouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXParseException;
import uk.org.siri.siri20.Siri;


@Slf4j
@Component
public class RequestHandlerRoute extends SpringRouteBuilder {

  private final SubscriptionService service;
  private final SiriMapperService mapperService;

  @Autowired
  public RequestHandlerRoute(SubscriptionService service, SiriMapperService mapperService) {
    this.service = service;
    this.mapperService = mapperService;
  }

  @SuppressWarnings("unchecked")
  @Override
  public void configure() {
    getContext().setUseMDCLogging(true);
    final Namespaces ns = new Namespaces("siri", "http://www.siri.org.uk/siri")
        .add("xsd", "http://www.w3.org/2001/XMLSchema");

    // @formatter:off
    onException(UnmarshalException.class, InvalidPayloadException.class, SAXParseException.class)
        .handled(true)
        .setHeader(Exchange.HTTP_RESPONSE_CODE, constant("400"))
        .setBody(constant("Invalid XML"))
    ;


    from("seda:incoming")
        .routeId("http.incoming")
        .streamCaching()
        .choice()
          .when().xpath("/siri:Siri", ns)
            .log(LoggingLevel.INFO, "Incoming from vendor: ${header.vendorName} - "
                + "${header.subscriptionName} - "
                + "${header.subscriptionId}")
            .setHeader(Exchange.CONTENT_TYPE, constant(MediaType.TEXT_XML))
            .to("activemq:queue:" + ROUTER_QUEUE + AMQ_PRODUCER_PARAM)
    ;

    from("activemq:queue:" + ROUTER_QUEUE + AMQ_CONSUMER_PARAM)
        .routeId("siri.router")
        .streamCaching()
        .choice()
          .when().xpath(SUBSCRIPTION_RESPONSE_XPATH, ns)
              .log(LoggingLevel.DEBUG,"SUBSCRIPTION_RESPONSE")
              .to("activemq:queue:" + SUBSCRIPTION_RESPONSE + AMQ_PRODUCER_PARAM)
          .endChoice()

          .when().xpath(HEARTBEAT_XPATH, ns)
              .log(LoggingLevel.DEBUG,"HEARTBEAT")
              .to("activemq:queue:" + HEARTBEAT_QUEUE + AMQ_PRODUCER_PARAM)
          .endChoice()

          .when().xpath(TERMINATION_RESPONSE_XPATH, ns)
              .log(LoggingLevel.DEBUG,"TERMINATION_RESPONSE")
              .to("activemq:queue:" + TERMINATION_RESPONSE + AMQ_PRODUCER_PARAM)
          .endChoice()

          .when().xpath(CHECK_RESPONSE_XPATH, ns)
              .log(LoggingLevel.DEBUG,"CHECK_RESPONSE")
              .to("activemq:queue:" + CHECK_STATUS_QUEUE + AMQ_PRODUCER_PARAM)
          .endChoice()

          .when(xpath(SX_XPATH).namespaces(ns))
              .setProperty("producerRef", xpath(PRODUCER_XPATH, String.class).namespaces(ns))
              .log("Receiving SX from producer: ${header.producerRef}")
              .split()
              .xtokenize(TOKENIZE_SX,'w', ns, 1)
              .streaming()
                .to("activemq:queue:" + SITUATION_EXCHANGE_QUEUE + AMQ_PRODUCER_PARAM)
          .endChoice()

          .when(xpath(VM_XPATH).namespaces(ns))
              .setProperty("producerRef", xpath(PRODUCER_XPATH, String.class).namespaces(ns))
              .log("Receiving VM from producer: ${header.producerRef}")
              .split()
              .xtokenize(TOKENIZE_VM,'w', ns, 1)
              .streaming()
                .to("activemq:queue:" + VEHICLE_MONITORING_QUEUE + AMQ_PRODUCER_PARAM)
          .endChoice()

          .when().xpath(ET_XPATH, ns)
              .setProperty("producerRef", xpath(PRODUCER_XPATH, String.class).namespaces(ns))
              .log("Receiving ET from producer: ${header.producerRef}")
              .choice()
                .when(simple("${header.producerRef} == 'SIS'"))
                  .log("Map to NSR")
                  .unmarshal(SiriDataFormat.getSiriJaxbDataformat())
                  .bean(mapperService, "mapOldToNew(*, RUT)")
                  .marshal(SiriDataFormat.getSiriJaxbDataformat())
              .split()
              .xtokenize(TOKENIZE_ET,'w', ns, 1)
              .streaming()
                .to("activemq:queue:" + ESTIMATED_TIMETABLE_QUEUE + AMQ_PRODUCER_PARAM)
          .endChoice()

          .otherwise()
              .log("Unknown SIRI request:\n${body}")
              .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(406)) // not acceptable
              .setBody(constant("Not Acceptable"))
          .endChoice()
        .end()
    ;

    from("activemq:queue:" + Constants.SUBSCRIPTION_RESPONSE + AMQ_CONSUMER_PARAM)
        .routeId("processor.response")
        .setExchangePattern(ExchangePattern.InOnly)
        .streamCaching()
        .log("Subscription Response Processor: body\n ${body}")
        .process(p -> {
          final String body = p.getIn().getBody(String.class);
          if (body != null) {
            Siri data = SiriXml.parseXml(body);
            String subscriptionRef = data.getSubscriptionResponse().getResponseStatuses().get(0)
                .getSubscriptionRef().getValue();

            service.touch(subscriptionRef);

            if (service.isActivated(subscriptionRef)) {
              log.info("Received subscription response for already activated subscription {} "
                  + "-- ignoring", subscriptionRef);
            } else {
              log.info("Received subscription response: {} ", subscriptionRef);
              service.activateSubscription(subscriptionRef);
            }
          } else {
            log.info("Subscription response is null!");
          }
        })
    ;

    from("activemq:queue:" + Constants.HEARTBEAT_QUEUE + AMQ_CONSUMER_PARAM)
        .routeId("processor.heartbeat")
        .streamCaching()
        .process(p -> {
          Siri data = SiriXml.parseXml(p.getIn().getBody(InputStream.class));

          if (data.getHeartbeatNotification() != null) {
            service.touch(data.getHeartbeatNotification());
            String producer =  data.getHeartbeatNotification().getProducerRef().getValue();
            p.getIn().setHeader("producerRef", producer);
          }
        })
        .log("Received heartbeat from: ${in.header.producerRef}")
        .removeHeader("*")
        .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(200))
        .setBody(constant("OK"))
    ;

    from("activemq:queue:" + CHECK_STATUS_QUEUE + AMQ_CONSUMER_PARAM)
        .routeId("processor.check.status.response")
        .process(p -> {
          log.warn("processor.check.status.response - Not implemented");
        })
    ;

    from("activemq:queue:" + Constants.TERMINATION_RESPONSE + AMQ_CONSUMER_PARAM)
        .routeId("processor.termination.response")
        .setExchangePattern(ExchangePattern.InOnly)
        .streamCaching()
        .process(p -> {
          Siri data = SiriXml.parseXml(p.getIn().getBody(InputStream.class));

          final String subscriberRef = data.getTerminateSubscriptionResponse()
              .getTerminationResponseStatuses()
              .get(0)
              .getSubscriptionRef()
              .getValue();

          Subscription subscription = service.getSubscription(subscriberRef);

          log.info("Received termination response from: {} - {}", subscription.getName(),
              subscription.getSubscriptionId());
        })
    ;



    }
}
























