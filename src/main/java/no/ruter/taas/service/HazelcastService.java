package no.ruter.taas.service;

import com.hazelcast.config.Config;
import com.hazelcast.config.GroupConfig;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.ManagementCenterConfig;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.MulticastConfig;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.config.SSLConfig;
import com.hazelcast.config.SerializerConfig;
import com.hazelcast.config.TcpIpConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastException;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.nio.serialization.ByteArraySerializer;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import no.ruter.taas.config.AppConfig;
import no.ruter.taas.siri.Subscription;
import no.ruter.taas.siri20.util.SiriDataType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public final class HazelcastService {

  private final KubernetesService kubernetesService;
  private final AppConfig config;

  private HazelcastInstance hazelcast;
  private boolean startupOk = false;
  private String managementUrl;

  @Autowired
  public HazelcastService(KubernetesService kubernetesService, AppConfig config) {
    this.kubernetesService = kubernetesService;
    this.config = config;
    this.managementUrl = config.getHazelcastManagementUrl();
  }

  @PostConstruct
  public final void init() {
    if (kubernetesService != null && config.isKubernetesEnabled()) {
      log.info("Configuring hazelcast");
      try {
        String name = kubernetesService.findDeploymentName();
        // Consider: The password part could be a kubernetes secret
        hazelcast = runHazelcast(kubernetesService.findEndpoints(), name, name + "_pw");
        startupOk = true;
      } catch (Exception e) {
        throw new HazelcastException("Could not run initialization of hazelcast.", e);
      }
    } else {
      log.warn("Using local hazelcast as we do not have kubernetes");
      hazelcast = initForLocalHazelCast();
    }
    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        hazelcast.shutdown();
      }
    });
  }

  @PreDestroy
  public final void shutdown() {
    log.info("Shutdown initiated");
    hazelcast.shutdown();
    log.info("Shutdown finished");
  }

  /**
   * Method to be used as part of liveness test for a k8s service
   *
   * @return true if started up ok and has cluster members
   */
  public Boolean getStartupOk() {
    return startupOk && numberOfClusterMembers() > 0;
  }

  /**
   * @return Just debug information - format may change over time
   */
  public String information() {
    if (hazelcast == null) {
      return "Hazelcast is null, i.e. not configured.";
    }

    StringBuilder sb = new StringBuilder("cluster members \n");
    hazelcast.getCluster()
        .getMembers()
        .forEach(m -> sb.append(" - localMember: ")
            .append(m.localMember())
            .append(" Address: ")
            .append(m.getAddress())
            .append(" Attributes: ")
            .append(m.getAttributes())
            .append("\n"));

    return sb.toString();
  }

  public int numberOfClusterMembers() {
    return hazelcast == null ? 0 : hazelcast.getCluster().getMembers().size();
  }

  public void updateDefaultMapConfig(MapConfig defaultMapConfig) {
  }

  private HazelcastInstance runHazelcast(final List<String> nodes, String groupName,
      String password) {
    final int HC_PORT = 5701;
    if (nodes.isEmpty()) {
      log.warn("No nodes given - will start lonely HZ");
    }

    // configure Hazelcast instance
    final Config cfg = new Config()
        .setInstanceName(UUID.randomUUID().toString())
        .setGroupConfig(new GroupConfig(groupName, password))
        .setProperty("hazelcast.phone.home.enabled", "false");

    // tcp
    final TcpIpConfig tcpCfg = new TcpIpConfig();
    nodes.forEach(tcpCfg::addMember);
    tcpCfg.setEnabled(true);

    // network join configuration
    final JoinConfig joinCfg = new JoinConfig()
        .setMulticastConfig(new MulticastConfig().setEnabled(false))
        .setTcpIpConfig(tcpCfg);

    final NetworkConfig netCfg = new NetworkConfig()
        .setPortAutoIncrement(false)
        .setPort(HC_PORT)
        .setJoin(joinCfg)
        .setSSLConfig(new SSLConfig().setEnabled(false));

    MapConfig mapConfig = cfg.getMapConfig("default");
    updateDefaultMapConfig(mapConfig);

    log.info("Old config: b_count " + mapConfig.getBackupCount() + " async_b_count " + mapConfig
        .getAsyncBackupCount() + " read_backup_data " + mapConfig.isReadBackupData());
    mapConfig.setBackupCount(2)
        .setAsyncBackupCount(0)
        .setReadBackupData(true);

    log.info("Updated config: b_count " + mapConfig.getBackupCount() + " async_b_count " + mapConfig
        .getAsyncBackupCount() + " read_backup_data " + mapConfig.isReadBackupData());

    addMgmtIfConfigured(cfg);
    cfg.setNetworkConfig(netCfg);
    getAdditionalMapConfigurations().forEach(cfg::addMapConfig);
    getSerializerConfigs().forEach(cfg.getSerializationConfig()::addSerializerConfig);

    return Hazelcast.newHazelcastInstance(cfg);
  }

  private void addMgmtIfConfigured(Config cfg) {
    if (managementUrl != null && !managementUrl.isEmpty()) {
      ManagementCenterConfig mcc = new ManagementCenterConfig(managementUrl, 3);
      mcc.setEnabled(true);
      cfg.setManagementCenterConfig(mcc);
      log.info("Added management URL: " + managementUrl);
    }
  }

  private HazelcastInstance initForLocalHazelCast() {
    log.info("Running hazelcast with LOCAL configuration ONLY"
        + " - this is for junit tests and when you do not have a kubernetes environment");

    final Config cfg = new Config()
        .setInstanceName(UUID.randomUUID().toString())
        .setProperty("hazelcast.phone.home.enabled", "false");
    final JoinConfig joinCfg = new JoinConfig()
        .setMulticastConfig(new MulticastConfig().setEnabled(false))
        .setTcpIpConfig(new TcpIpConfig().setEnabled(false));
    NetworkConfig networkCfg = new NetworkConfig()
        .setJoin(joinCfg);
    networkCfg.getInterfaces()
        .setEnabled(false);

    cfg.setNetworkConfig(networkCfg);

    MapConfig mapConfig = cfg.getMapConfig("default");
    updateDefaultMapConfig(mapConfig);
    getAdditionalMapConfigurations().forEach(cfg::addMapConfig);

    getSerializerConfigs().forEach(cfg.getSerializationConfig()::addSerializerConfig);

    addMgmtIfConfigured(cfg);

    return Hazelcast.newHazelcastInstance(cfg);
  }

  private List<MapConfig> getAdditionalMapConfigurations() {
    return new ArrayList<>();
  }

  private List<SerializerConfig> getSerializerConfigs() {
    return Collections.singletonList(new SerializerConfig()
        .setTypeClass(Object.class)
        .setImplementation(new ByteArraySerializer() {

          @Override
          public byte[] write(Object object) throws IOException {
            try (ByteArrayOutputStream b = new ByteArrayOutputStream()) {
              try (ObjectOutputStream o = new ObjectOutputStream(b)) {
                o.writeObject(object);
              }
              return b.toByteArray();
            }
          }

          @Override
          public Object read(byte[] buffer) throws IOException {
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(buffer);
            ObjectInputStream in = new ObjectInputStream(byteArrayInputStream);
            try {
              return in.readObject();
            } catch (ClassNotFoundException e) {
              e.printStackTrace();
            }
            return null;
          }

          @Override
          public int getTypeId() {
            return 1;
          }

          @Override
          public void destroy() {

          }
        })
    );
  }


  public HazelcastInstance getHazelcastInstance() {
    return hazelcast;
  }


  @Bean
  public IMap<String, Subscription> getSubscriptionMap() {
    return hazelcast.getMap("subscriber.subscriptions");
  }

  @Bean
  public IMap<String, Subscription> getActiveSubscriptionMap() {
    return hazelcast.getMap("subscriber.subscriptions.active");
  }

  @Bean
  public IMap<String, Instant> getSubscriptionActivityMap() {
    return hazelcast.getMap("subscriber.subscriptions.activity");
  }

  @Bean
  public IMap<Enum<HealthCheckKey>, Instant> getHealthCheckMap() {
    return hazelcast.getMap("subscriber.admin.health");
  }

  @Bean
  public IMap<Instant, String> getTmpMap() {
    return hazelcast.getMap("subscriber.tmp.map");
  }

  @Bean
  public IMap<String, Instant> getLockMap() {
    return hazelcast.getMap("subscriber.locks");
  }

  @Bean
  public IMap<String, Map<SiriDataType, Set<String>>> getUnmappedIds() {
    return hazelcast.getMap("subscriber.mapping.unmapped");
  }

  @Bean
  public IMap<String, String> getQuayMap() {
    return hazelcast.getMap("subscriber.quay.map");
  }

  @Bean
  public IMap<String, String> getStopPlaceMap() {
    return hazelcast.getMap("subscriber.stopplace.map");
  }


}
