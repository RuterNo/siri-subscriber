package no.ruter.taas.route.policy;


import static no.ruter.taas.route.Constants.SINGLETON_ROUTE_DEFINITION_GROUP_NAME;

import lombok.extern.slf4j.Slf4j;
import no.ruter.taas.config.AppConfig;
import no.ruter.taas.service.HazelcastService;
import org.apache.camel.CamelContext;
import org.apache.camel.component.hazelcast.policy.HazelcastRoutePolicy;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.spi.RoutePolicy;
import org.apache.camel.spi.RoutePolicyFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Create route policy using Hazelcast as clustered lock.
 */

@Slf4j
@Service
public class SingletonRoutePolicyFactory implements RoutePolicyFactory {

  private final AppConfig config;
  private final HazelcastService hazelcastService;

  @Autowired
  public SingletonRoutePolicyFactory(AppConfig config, HazelcastService hazelcastService) {
    this.config = config;
    this.hazelcastService = hazelcastService;
  }

  private RoutePolicy build(String key) {
    HazelcastRoutePolicy hazelcastRoutePolicy = new HazelcastRoutePolicy(
        hazelcastService.getHazelcastInstance());
    hazelcastRoutePolicy.setLockMapName("subscriberRouteLockMap");
    hazelcastRoutePolicy.setLockKey(key);
    hazelcastRoutePolicy.setLockValue("lockValue");
    hazelcastRoutePolicy.setShouldStopConsumer(false);

    log.info("RoutePolicy: Created HazelcastPolicy for key {}", key);
    return hazelcastRoutePolicy;
  }

  @Override
  public RoutePolicy createRoutePolicy(CamelContext camelContext, String routeId,
      RouteDefinition routeDefinition) {
    try {
      if (!config.isIgnorePolicy() &&
          SINGLETON_ROUTE_DEFINITION_GROUP_NAME.equals(routeDefinition.getGroup())) {
        return build(routeId);
      }
    } catch (Exception e) {
      log.warn("Failed to create singleton route policy", e);
    }
    return null;
  }


}
