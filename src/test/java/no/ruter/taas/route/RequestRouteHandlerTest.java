package no.ruter.taas.route;

import java.io.IOException;
import java.io.RandomAccessFile;
import no.ruter.taas.App;
import no.ruter.taas.siri.Subscription;
import no.ruter.taas.siri20.util.SiriDataType;
import no.ruter.taas.siri20.util.SiriXml;
import no.ruter.taas.test.SiriTestBase;
import no.ruter.taas.test.TestUtils;
import org.apache.camel.FluentProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.CamelSpringBootRunner;
import org.apache.camel.test.spring.MockEndpoints;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import uk.org.siri.siri20.Siri;

@ActiveProfiles("test")
@RunWith(CamelSpringBootRunner.class)
@SpringBootTest(classes = App.class)
@MockEndpoints
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class RequestRouteHandlerTest extends SiriTestBase {

  private static Subscription subscription;
  private static String siriPath = "src/test/resources/xml/siri/data/";


  @Before
  public void setupSubscriptions() {
    String id = subscriptionResponse.getSubscriptionResponse().getResponderRef().getValue();
    subscription = TestUtils.createSubscription(SiriDataType.VEHICLE_MONITORING);
    subscription.setSubscriptionId(id);
  }

  @Test
  public void testSubscriptionResponse() throws Exception {
    String delivery = SiriXml.toXml(subscriptionResponse);
    service.addSubscription(subscription);

    MockEndpoint result = camelContext.getEndpoint("mock:activemq:queue:" +
        Constants.SUBSCRIPTION_RESPONSE, MockEndpoint.class);
    result.expectedMessageCount(1);
    FluentProducerTemplate producerTemplate = camelContext.createFluentProducerTemplate();

    producerTemplate
        .withBody(delivery)
        .to("activemq:queue:" + Constants.ROUTER_QUEUE).send();

    result.assertIsSatisfied();
  }

  @Test
  public void testSiriHeartbeat() throws Exception {
    String delivery = SiriXml.toXml(heartbeat);

    MockEndpoint result = camelContext.getEndpoint("mock:activemq:queue:" +
        Constants.HEARTBEAT_QUEUE, MockEndpoint.class);
    result.expectedMessageCount(1);
    FluentProducerTemplate producerTemplate = camelContext.createFluentProducerTemplate();

    producerTemplate
        .withBody(delivery)
        .to("activemq:queue:" + Constants.ROUTER_QUEUE).send();

    result.assertIsSatisfied();
  }

  @Test
  public void testSiriDeliveryET() throws Exception {
    String delivery = SiriXml.toXml(singleSiriET);

    MockEndpoint result = camelContext.getEndpoint("mock:activemq:queue:" +
        Constants.ESTIMATED_TIMETABLE_QUEUE, MockEndpoint.class);
    result.expectedMessageCount(1);
    FluentProducerTemplate producerTemplate = camelContext.createFluentProducerTemplate();

    producerTemplate
        .withBody(delivery)
        .to("activemq:queue:" + Constants.ROUTER_QUEUE).send();

    result.assertIsSatisfied();
  }

  @Test
  public void testSiriDeliveryETMapToNewIds() throws Exception {
    Siri siriOrigIds = getSiriOrigIds();

    MockEndpoint result = camelContext.getEndpoint("mock:activemq:queue:" +
        Constants.ESTIMATED_TIMETABLE_QUEUE, MockEndpoint.class);
    result.expectedMessageCount(1);

    FluentProducerTemplate producerTemplate = camelContext.createFluentProducerTemplate();
    producerTemplate
        .withBody(SiriXml.toXml(siriOrigIds))
        .to("activemq:queue:" + Constants.ROUTER_QUEUE).send();

    result.assertIsSatisfied();
  }

  @Test
  public void testSiriDeliveryVM() throws Exception {
    String delivery = SiriXml.toXml(singleSiriVM);

    MockEndpoint result = camelContext.getEndpoint("mock:activemq:queue:" +
        Constants.VEHICLE_MONITORING_QUEUE, MockEndpoint.class);
    result.expectedMessageCount(1);
    FluentProducerTemplate producerTemplate = camelContext.createFluentProducerTemplate();

    producerTemplate
        .withBody(delivery)
        .to("activemq:queue:" + Constants.ROUTER_QUEUE).send();

    result.assertIsSatisfied();
  }

  @Test
  public void testSiriDeliverySX() throws Exception {
    String delivery = SiriXml.toXml(singleSiriSX);

    MockEndpoint result = camelContext.getEndpoint("mock:activemq:queue:" +
        Constants.SITUATION_EXCHANGE_QUEUE, MockEndpoint.class);
    result.expectedMessageCount(1);
    FluentProducerTemplate producerTemplate = camelContext.createFluentProducerTemplate();

    producerTemplate
        .withBody(delivery)
        .to("activemq:queue:" + Constants.ROUTER_QUEUE).send();

    result.assertIsSatisfied();
  }


  private static Siri getSiriOrigIds() throws Exception {
    return SiriXml.parseXml(readFile(siriPath + "siri-et-origid.xml"));

  }

  private static Siri getSiriNSRIds() throws Exception {
    return SiriXml.parseXml(readFile(siriPath + "siri-et-nsr.xml"));
  }

  private static String readFile(String path) throws IOException {
    RandomAccessFile raf = new RandomAccessFile(path, "rw");
    byte[] contents = new byte[(int) raf.length()];
    raf.readFully(contents);
    return new String(contents);
  }

  private static String removeWhiteSpace(String xml) {
    //Removing indentation and newlines to match unformatted xml
    xml = xml.replace("\n", "");
    while (xml.indexOf("  ") > 0) {
      xml = xml.replace("  ", "");
    }
    return xml;
  }

}

