package no.ruter.taas.config;

import javax.annotation.PostConstruct;
import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Slf4j
@Configuration
@ConfigurationProperties(prefix = "siri")
public class AppConfig {

  private int incomingTimeToLive;
  private int concurrentConsumers;

  private String consumerAddress;

  private boolean kubernetesEnabled;
  private String kubernetesUrl;
  private String kubernetesNamespace;

  private boolean hazelcastManagementEnabled;
  private String hazelcastManagementUrl;

  private boolean avroProcessorEnabled;
  private String schemaRegistryUrl;
  private String bootstrapServers;
  private String topicVm;
  private String topicEt;
  private String topicSx;
  private String topicAvroVm;
  private String topicAvroVmPlus;
  private String topicAvroEt;

  private boolean ignorePolicy;

  private String subscriptionPath;
  private String inboundPattern;

  private boolean fetchFromUrl;
  private String fallbackPath;
  private String netexStopplaceUrl;

  private String keyStorePath;
  @ToString.Exclude
  private String keyStorePw;


  @PostConstruct
  public void printConfig() {
    log.info("Started with configuration:\n" + this.toString());
  }
}
