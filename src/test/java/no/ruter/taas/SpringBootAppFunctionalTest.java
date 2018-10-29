package no.ruter.taas;

import static io.restassured.RestAssured.given;
import static io.restassured.config.LogConfig.logConfig;
import static no.ruter.taas.test.TestUtils.createSubscription;
import static no.ruter.taas.test.TestUtils.defaultConsumer;
import static no.ruter.taas.test.TestUtils.defaultProducer;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.restassured.RestAssured;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.time.Duration;
import java.util.Collections;
import no.ruter.taas.service.SubscriptionService;
import no.ruter.taas.siri.Subscription;
import no.ruter.taas.siri20.util.SiriDataType;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.testcontainers.containers.DockerComposeContainer;


@SuppressWarnings({"unchecked", "WeakerAccess"})
@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@ContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class SpringBootAppFunctionalTest {

  @LocalServerPort
  private int serverPort = 0;

  @Autowired
  private SubscriptionService service;

  private Producer producer;
  private Consumer consumer;
  private static int KAFKA_PORT = 9092;


  @ClassRule
  public static DockerComposeContainer container = new
      DockerComposeContainer(new File("src/test/resources/kafka/docker-compose.yml"));


  @Before
  public void setup() {
    RestAssured.port = serverPort;
    RestAssured.baseURI = "http://localhost";
    RestAssured.config.logConfig(logConfig().enableLoggingOfRequestAndResponseIfValidationFails());

    producer = defaultProducer(KAFKA_PORT);
    consumer = defaultConsumer(KAFKA_PORT);
  }

  @Test
  public void authenticated_post_admin_start_subscription_expected_200_ok() {
    Subscription subscription = createSubscription(SiriDataType.VEHICLE_MONITORING);
    service.addSubscription(subscription);
    String id = subscription.getSubscriptionId();
    // @formatter:off

    given()
        .auth()
          .basic("admin", "admin")
        .when()
          .post("/admin/start/" + id)
        .then()
          .assertThat()
          .statusCode(200);

    // @formatter:on

    assertThat(service.getSubscription(id).isActive(), is(true));
  }

  @Test
  public void unauthenticated_post_admin_start_subscription_expected_401_ok() {
    Subscription subscription = createSubscription(SiriDataType.VEHICLE_MONITORING);
    service.addSubscription(subscription);
    String id = subscription.getSubscriptionId();
    // @formatter:off

    given()
        .when()
          .post("/admin/start/" + id)
        .then()
          .assertThat()
          .statusCode(401);

    // @formatter:on
  }

  @Test
  public void authenticated_post_admin_stop_subscription_expected_200_ok() {
    Subscription subscription = createSubscription(SiriDataType.VEHICLE_MONITORING);
    String id = subscription.getSubscriptionId();
    service.addSubscription(subscription);
    service.activateSubscription(subscription.getSubscriptionId());
    // @formatter:off

    given()
        .auth()
          .basic("admin", "admin")
        .when()
          .post("/admin/stop/" + id)
        .then()
          .assertThat()
          .statusCode(200);

    // @formatter:on

    assertThat(service.getSubscription(id).isActive(), is(false));
  }

  @Test
  public void unauthenticated_post_admin_stop_subscription_expected_401_unauthorized() {
    Subscription subscription = createSubscription(SiriDataType.VEHICLE_MONITORING);
    String id = subscription.getSubscriptionId();
    service.addSubscription(subscription);
    service.activateSubscription(subscription.getSubscriptionId());
    // @formatter:off

    given()
        .when()
          .post("/admin/stop/" + id)
        .then()
          .assertThat()
          .statusCode(401);

    // @formatter:on
  }

  @Test
  public void unauthenticated_get_admin_page_expected_401_unauthorized() {
    // @formatter:off

    given()
        .when()
          .get("/admin/status")
        .then()
          .assertThat()
          .statusCode(401);

    // @formatter:on
  }

  @Test
  public void authenticated_get_admin_page_expected_200_ok() {
    // @formatter:off

    given()
        .when()
          .auth()
            .basic("admin", "admin")
          .get("/admin/status")
        .then()
          .assertThat()
          .statusCode(200);

    // @formatter:on
  }

  @Test
  public void authenticated_admin_get_all_subscriptions_expected_200_ok() {
    // @formatter:off

    given()
        .when()
          .auth()
            .basic("admin", "admin")
          .get("/admin/all")
        .then()
          .assertThat()
          .statusCode(200);

    // @formatter:on
  }

  @Test
  public void unauthenticated_admin_get_all_subscriptions_expected_401_unauthorized() {
    // @formatter:off

    given()
        .when()
          .get("/admin/all")
        .then()
          .assertThat()
          .statusCode(401);

    // @formatter:on
  }


  @Test
  @Ignore // todo: WIP
  public void updated_existing_subscription_expected_success() throws Exception {
    final Subscription beforeUpdate = service
        .getSubscription("2fe54f27-5ba8-4ece-b728-f64be9f844SX");

    ObjectMapper mapper = new ObjectMapper(new JsonFactory());
    mapper.registerModule(new JavaTimeModule());
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    Subscription subscription = mapper.readValue(
        new File("src/test/resources/json/single_subscription.json"),
        Subscription.class);

    subscription.setSubscriberRef("someRef");

    given()
        .body(subscription)
        .auth()
        .basic("admin", "admin")
        .contentType(MediaType.APPLICATION_JSON.toString())
        .when()
        .put("/admin/update")
        .then()
        .assertThat()
        .statusCode(200)
        .wait(1000);

    final Subscription afterUpdate = service
        .getSubscription("2fe54f27-5ba8-4ece-b728-f64be9f844SX");

    assertThat(afterUpdate.getSubscriberRef(), not(beforeUpdate.getSubscriberRef()));

  }


  @Test
  public void testProducerConsumer() {
    producer.send(new ProducerRecord<>("some.topic", "my-key", "my-value"));

    consumer.subscribe(Collections.singleton("some.topic"));
    ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(30));

    assertThat(records.count(), Matchers.is(1));
  }

  private static String readFile(String path) throws IOException {
    RandomAccessFile raf = new RandomAccessFile(path, "rw");
    byte[] contents = new byte[(int) raf.length()];
    raf.readFully(contents);
    return new String(contents);
  }

}
