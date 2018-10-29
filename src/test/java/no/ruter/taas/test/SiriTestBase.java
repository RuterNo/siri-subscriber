package no.ruter.taas.test;

import com.google.code.tempusfugit.temporal.Duration;
import com.google.code.tempusfugit.temporal.Timeout;
import java.io.IOException;
import java.io.RandomAccessFile;
import no.ruter.taas.config.AppConfig;
import no.ruter.taas.service.SubscriptionService;
import no.ruter.taas.siri.SiriObjectFactory;
import no.ruter.taas.siri20.util.SiriXml;
import org.apache.camel.CamelContext;
import org.apache.camel.FluentProducerTemplate;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.rule.OutputCapture;
import uk.org.siri.siri20.Siri;

@SuppressWarnings("ALL")
public class SiriTestBase {

  @Autowired
  protected AppConfig config;

  @Autowired
  protected FluentProducerTemplate producerTemplate;

  @Autowired
  protected SiriObjectFactory factory;

  @Autowired
  protected CamelContext camelContext;

  @Autowired
  protected SubscriptionService service;

  @Rule
  public OutputCapture capture = new OutputCapture();


  protected static Siri singleSiriET;
  protected static Siri singleSiriVM;
  protected static Siri singleSiriSX;

  protected static Siri multipleSiriET;
  protected static Siri multipleSiriVM;
  protected static Siri multipleSiriSX;

  protected static Siri subscriptionResponse;
  protected static Siri heartbeat;

  protected static int port = 8010;

  @Before
  public void setupSiri() throws Exception {
    String siriPath = "src/test/resources/xml/siri/";

    singleSiriET = SiriXml.parseXml(readFile(siriPath + "data/et_single.xml"));
    singleSiriVM = SiriXml.parseXml(readFile(siriPath + "data/vm_single.xml"));
    singleSiriSX = SiriXml.parseXml(readFile(siriPath + "data/sx_single.xml"));

    multipleSiriET = SiriXml.parseXml(readFile(siriPath + "data/et_multiple.xml"));
    multipleSiriVM = SiriXml.parseXml(readFile(siriPath + "data/vm_multiple.xml"));
    multipleSiriSX = SiriXml.parseXml(readFile(siriPath + "data/sx_multiple.xml"));

    subscriptionResponse =
        SiriXml.parseXml(readFile(siriPath + "response/subscriptionResponse.xml"));
    heartbeat = SiriXml.parseXml(readFile(siriPath + "response/heartbeatNotification.xml"));
  }

  @After
  public void tearDown() throws Exception {

  }

  protected Timeout defaultTimeout() {
    return Timeout.timeout(Duration.seconds(2));
  }

  private static String readFile(String path) throws IOException {
    RandomAccessFile raf = new RandomAccessFile(path, "rw");
    byte[] contents = new byte[(int) raf.length()];
    raf.readFully(contents);
    return new String(contents);
  }

}
