package no.ruter.taas.route;

import static no.ruter.taas.test.TestUtils.createSubscription;

import no.ruter.taas.App;
import no.ruter.taas.siri.Subscription;
import no.ruter.taas.siri20.util.SiriDataType;
import no.ruter.taas.test.SiriTestBase;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.CamelSpringBootRunner;
import org.apache.camel.test.spring.MockEndpoints;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@RunWith(CamelSpringBootRunner.class)
@SpringBootTest(classes = App.class)
@MockEndpoints
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class SubscriptionRouteTest extends SiriTestBase {


  @Test
  public void given_terminationResponse_should_renew() {
    Subscription subscription = createSubscription(SiriDataType.VEHICLE_MONITORING);
    subscription.setActive(true);
    service.addSubscription(subscription);

    MockEndpoint result = camelContext.getEndpoint("mock:direct:subscribe", MockEndpoint.class);
    producerTemplate.withBody(subscription)
        .to("mock:activemq:queue:siri.subscriber.process.terminate]").send();

    result.expectedMessageCount(1);
  }

  @Test
  public void given_terminationResponse_should_notRenew() throws Exception {
    Subscription subscription = createSubscription(SiriDataType.VEHICLE_MONITORING);
    subscription.setActive(false);
    service.addSubscription(subscription);
    MockEndpoint result = camelContext.getEndpoint("mock:direct:subscribe", MockEndpoint.class);
    result.expectedMessageCount(0);

    producerTemplate.withBody(subscription)
        .to("mock:activemq:queue:siri.subscriber.process.terminate").send();

    result.assertIsSatisfied();
  }

  @Test
  public void given_subscription_response_empty() throws Exception {
    MockEndpoint result = camelContext.getEndpoint("mock:log:result",
        MockEndpoint.class);
    result.expectedMessageCount(0);

    producerTemplate
        .withBody(" ")
        .to("activemq:queue:siri.subscriber.process.subscription")
        .send();

    result.assertIsSatisfied();
  }

  @Test
  public void given_subscription_response_null() throws Exception {
    MockEndpoint result = camelContext.getEndpoint("mock:log:result",
        MockEndpoint.class);
    result.expectedMessageCount(0);

    producerTemplate
        .withBody(null)
        .to("activemq:queue:siri.subscriber.process.subscription")
        .send();

    result.assertIsSatisfied();
  }

}
