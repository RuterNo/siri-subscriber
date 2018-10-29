package no.ruter.taas.route;

import no.ruter.taas.App;
import no.ruter.taas.siri20.util.SiriXml;
import no.ruter.taas.test.SiriTestBase;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.CamelSpringBootRunner;
import org.apache.camel.test.spring.MockEndpoints;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@SuppressWarnings( "Duplicates" )
@ActiveProfiles("test")
@RunWith(CamelSpringBootRunner.class)
@SpringBootTest(classes = App.class)
@MockEndpoints
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class KafkaRouteTest extends SiriTestBase {

  @Test
  public void AMQSiriSX_receiveOneSiriXML_should_produceOneMsgToKafka() throws Exception {
    String delivery = SiriXml.toXml(singleSiriSX);
    MockEndpoint result = camelContext.getEndpoint("mock:kafka:siri.sx.complete",
        MockEndpoint.class);
    result.expectedMessageCount(1);

    producerTemplate
        .withBody(delivery)
        .to("activemq:queue:siri.subscriber.process.sx").send();

    result.assertIsSatisfied();
  }

  @Test
  public void AMQSiriVM_receiveOneSiriXML_should_produceOneMsgToKafka() throws Exception {
    String delivery = SiriXml.toXml(singleSiriVM);
    MockEndpoint result = camelContext.getEndpoint("mock:kafka:siri.vm.complete",
        MockEndpoint.class);
    result.expectedMessageCount(1);

    producerTemplate
        .withBody(delivery)
        .to("activemq:queue:siri.subscriber.process.vm").send();

    result.assertIsSatisfied();
  }

  @Test
  public void AMQSiriET_receiveOneSiriXML_should_produceOneMsgToKafka() throws Exception {
    String delivery = SiriXml.toXml(singleSiriET);
    MockEndpoint result = camelContext.getEndpoint("mock:kafka:siri.et.complete",
        MockEndpoint.class);
    result.expectedMessageCount(1);

    producerTemplate
        .withBody(delivery)
        .to("activemq:queue:siri.subscriber.process.et").send();

    result.assertIsSatisfied();
  }

}
