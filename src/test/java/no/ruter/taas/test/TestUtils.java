package no.ruter.taas.test;

import static no.ruter.taas.test.SiriTestBase.port;

import java.time.Duration;
import java.util.HashMap;
import java.util.Properties;
import no.ruter.taas.siri.SiriRequestType;
import no.ruter.taas.siri.Subscription;
import no.ruter.taas.siri20.util.SiriDataType;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

public class TestUtils {

  public static Subscription createSubscription(SiriDataType dataType) {
    HashMap<SiriRequestType, String> requestTypeStringHashMap = new HashMap<>();
    requestTypeStringHashMap.put(SiriRequestType.SUBSCRIBE,
        "http://localhost:8000/subscribe");

    return Subscription.builder()
        .vendor("test-vm")
        .name("test-vm")
        .subscriptionType(dataType)
        .urlMap(requestTypeStringHashMap)
        .requestorRef("local-test")
        .subscriberRef("local-test")
        .consumerAddress("http://localhost:" + port)
        .subscriptionId("test-id")
        .heartbeatInterval(Duration.ofSeconds(10))
        .subscriptionDuration(Duration.ofHours(2))
        .updateInterval(Duration.ofSeconds(1))
        .incrementalUpdates(true)
        .changeBeforeUpdates(Duration.ofMinutes(1))
        .previewInterval(Duration.ofMinutes(1))
        .active(false)
        .build();
  }

  public static KafkaProducer defaultProducer(int port) {
    Properties properties = new Properties();
    properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "http://0.0.0.0:" + port);
    properties.put("acks", "all");
    properties.put("key.serializer", StringSerializer.class);
    properties.put("value.serializer", StringSerializer.class);

    return new KafkaProducer<>(properties);
  }

  public static KafkaConsumer defaultConsumer(int port) {
    Properties properties = new Properties();
    properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "http://0.0.0.0:" + port);
    properties.put(ConsumerConfig.GROUP_ID_CONFIG, "consumerGroup");
    properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    properties.put("key.deserializer", StringDeserializer.class);
    properties.put("value.deserializer", StringDeserializer.class);

    return new KafkaConsumer<>(properties);
  }


}
