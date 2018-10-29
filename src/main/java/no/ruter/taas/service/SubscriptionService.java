package no.ruter.taas.service;


import com.hazelcast.core.IMap;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import no.ruter.taas.siri.Subscription;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.org.siri.siri20.HeartbeatNotificationStructure;

@SuppressWarnings("WeakerAccess")
@Slf4j
@Service
public class SubscriptionService {

  private final IMap<String, Subscription> subscriptionMap;
  private final IMap<String, Instant> lastActivityMap;

  @Autowired
  public SubscriptionService(
      @Qualifier("getSubscriptionMap") IMap<String, Subscription> subscriptionMap,
      @Qualifier("getSubscriptionActivityMap") IMap<String, Instant> lastActivityMap) {
    this.subscriptionMap = subscriptionMap;
    this.lastActivityMap = lastActivityMap;
  }

  public void addSubscription(Subscription subscription) {
    String id = subscription.getSubscriptionId();
    subscriptionMap.put(id, subscription);
    log.debug("Add {}", subscription);

    if (subscription.isActive()) {
      activateSubscription(id);
    }
  }

  public void activateSubscription(String subscriptionId) {
    Subscription subscription = subscriptionMap.get(subscriptionId);
    subscription.setActivationTime(Instant.now());
    subscription.setActive(true);
    lastActivityMap.put(subscriptionId, Instant.now());
    subscriptionMap.put(subscriptionId, subscription);

    log.debug("Activate {}", subscription);
  }

  public boolean isActivated(String subscriptionId) {
    Subscription subscription = subscriptionMap.get(subscriptionId);
    if (subscription != null) {
      return subscription.isActive();
    }
    return false;
  }

  public Subscription getSubscription(final String subscriptionId) {
    log.debug("Get {}", subscriptionId);
    return subscriptionMap.get(subscriptionId);
  }

  public boolean isHealthy(final String subscriptionId) {
    Instant instant = lastActivityMap.get(subscriptionId);

    if (instant == null) {
      // no activity yet, may not have been started
      return true;
    }

    Subscription subscription = subscriptionMap.get(subscriptionId);
    if (subscription != null && subscription.isActive()) {
      log.debug("SubscriptionId [{}], last activity {}.", subscriptionId, instant);

      // subscription exists, but there has not been any activity recently
      if (instant.isBefore(Instant.now().minusSeconds(60 * 10))) {
        return false;
      }

      // todo: need to look into this, seems like it cancels subscription when it should not
      // if active subscription has existed longer than "initial subscription duration" - restart
      if (subscription.getActivationTime().plusSeconds(
          subscription.getSubscriptionDuration().getSeconds()).isBefore(Instant.now())) {
        log.info("[{}] has lasted longer than initial subscription duration ", subscription);
        // todo: this should not be true fix this
        return true;
      }
    }
    return true;
  }

  public void update(Subscription subscription) {
    String id = subscription.getSubscriptionId();
    subscriptionMap.put(id, subscription);
    log.debug("Update {}", id);
  }

  public void stopSubscription(Subscription subscription) {
    log.info("Stop " + subscription.getSubscriptionId());
    log.debug(subscription.toString());
    subscription.setActive(false);
    update(subscription);
  }

  public void stopSubscription(final String subscriptionId) {
    Subscription subscription = getSubscription(subscriptionId);
    stopSubscription(subscription);
  }

  public void touch(final String id) {
    lastActivityMap.put(id, Instant.now());
    log.debug("Touch activity map for {}", id);
  }

  public void touch(final HeartbeatNotificationStructure heartbeatNotification) {
    touch(heartbeatNotification.getProducerRef().getValue());
  }

  public Instant getLastActivity(String subscriptionId) {
    return lastActivityMap.get(subscriptionId);
  }

  long subscriptionCount() {
    return subscriptionMap.values().size();
  }

  public Collection<Subscription> getAllSubscriptions() {
    return subscriptionMap.values();
  }

  public boolean hasSubscription(String id) {
    return subscriptionMap.containsKey(id);
  }

  @SuppressWarnings("unchecked")
  public JSONArray getStates() {
    Map<String, String> state = new HashMap<>();
    JSONArray states = new JSONArray();

    getAllSubscriptions().forEach(s -> {
      state.put("subscriptionId", s.getSubscriptionId());
      state.put("vendor", s.getVendor());
      state.put("name", s.getName());
      state.put("active", String.valueOf(s.isActive()));
      state.put("ssl", String.valueOf(s.isSsl()));
      state.put("activationTime", s.getActivationTime() == null ? "" :
          s.getActivationTime().toString());
      state.put("consumerAddress", s.getConsumerAddress());
      state.put("requestorRef", s.getRequestorRef());
      state.put("subscriberRef", s.getSubscriberRef());
      state.put("heartbeatInterval", s.getHeartbeatInterval().toString());
      state.put("updateInterval", s.getUpdateInterval().toString());
      state.put("previewInterval", s.getPreviewInterval().toString());
      state.put("incrementalUpdates", String.valueOf(s.isIncrementalUpdates()));
      state.put("subscribeEndpoint", s.getSubscribeEndpoint());
      state.put("terminationEndpoint", s.getTerminationEndpoint());
      state.put("checkStatusEndpoint", s.getCheckStatusEndpoint());
      state.put("lastActivity", lastActivityMap.get(s.getSubscriptionId()) == null ? "" :
          lastActivityMap.get(s.getSubscriptionId()).toString());

      states.add(new JSONObject(state));
    });

    return states;
  }

  public Subscription lookupURI(String requestURI) throws Exception {
    List<String> paths = Arrays.asList(requestURI.split("/"));

    if (paths.size() < 2) {
      throw new Exception("Could not find subscription");
    } else {
      return getSubscription(paths.get(1));
    }
  }


}





