package no.ruter.taas.service;

import com.hazelcast.core.HazelcastInstanceNotActiveException;
import com.hazelcast.core.IMap;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import no.ruter.taas.siri20.util.SiriDataType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class HealthManager {

  private final IMap<Enum<HealthCheckKey>, Instant> healthCheckMap;
  private final IMap<String, Map<SiriDataType, Set<String>>> unmappedIds;

  private long allowedInactivityTime = 300;
  private int healthCheckInterval = 30;

  @Autowired
  public HealthManager(
      @Qualifier("getHealthCheckMap") IMap<Enum<HealthCheckKey>, Instant> healthCheckMap,
      @Qualifier("getUnmappedIds") IMap<String, Map<SiriDataType, Set<String>>> unmappedIds) {
    this.healthCheckMap = healthCheckMap;
    this.unmappedIds = unmappedIds;
  }

  public boolean isHazelcastAlive() {
    try {
      healthCheckMap.set(HealthCheckKey.NODE_LIVENESS_CHECK, Instant.now());
      return healthCheckMap.containsKey(HealthCheckKey.NODE_LIVENESS_CHECK);
    } catch (HazelcastInstanceNotActiveException e) {
      log.warn("HazelcastInstance not active - ", e);
      return false;
    }
  }



  @Bean
  public Instant serverStartTime() {
    if (!healthCheckMap.containsKey(HealthCheckKey.SERVER_START_TIME)) {
      healthCheckMap.set(HealthCheckKey.SERVER_START_TIME, Instant.now());
    }
    return healthCheckMap.get(HealthCheckKey.SERVER_START_TIME);
  }

}
