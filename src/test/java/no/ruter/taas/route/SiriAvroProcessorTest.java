package no.ruter.taas.route;

import no.ruter.taas.App;
import no.ruter.taas.config.AppConfig;
import no.ruter.taas.siri20.util.SiriXml;
import no.ruter.taas.test.SiriTestBase;
import org.apache.camel.CamelContext;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.CamelSpringBootRunner;
import org.apache.camel.test.spring.MockEndpoints;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@SuppressWarnings("Duplicates")
@ActiveProfiles("test")
@RunWith(CamelSpringBootRunner.class)
@SpringBootTest(classes = App.class)
@MockEndpoints
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class SiriAvroProcessorTest extends SiriTestBase {

  @Autowired
  CamelContext camelContext;
  @Autowired
  AppConfig config;


  @Test
  public void produce_to_kafka_and_legacy_endpoint_when_enabled() throws Exception {
    config.setAvroProcessorEnabled(true);
    String delivery = SiriXml.toXml(singleSiriET);
    MockEndpoint result = camelContext.getEndpoint("mock:direct:processor.et",
        MockEndpoint.class);
    result.expectedMessageCount(1);

    producerTemplate
        .withBody(delivery)
        .to("activemq:queue:siri.subscriber.process.et").send();

    result.assertIsSatisfied();
  }

  @Test
  public void produce_to_kafka_and_not_legacy_when_disabled() throws Exception {
    config.setAvroProcessorEnabled(false);
    String delivery = SiriXml.toXml(singleSiriET);
    MockEndpoint result = camelContext.getEndpoint("mock:direct:processor.et",
        MockEndpoint.class);
    result.expectedMessageCount(0);

    producerTemplate
        .withBody(delivery)
        .to("activemq:queue:siri.subscriber.process.et").send();

    result.assertIsSatisfied();
  }


}
