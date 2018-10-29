package no.ruter.taas.service;


import static no.ruter.taas.test.TestUtils.createSubscription;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import no.ruter.taas.App;
import no.ruter.taas.siri.Subscription;
import no.ruter.taas.siri20.util.SiriDataType;
import no.ruter.taas.test.SiriTestBase;
import org.apache.camel.test.spring.CamelSpringBootRunner;
import org.apache.camel.test.spring.MockEndpoints;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;


@ActiveProfiles("test")
@RunWith(CamelSpringBootRunner.class)
@SpringBootTest(classes = App.class)
@MockEndpoints
@Slf4j
public class SubscriptionServiceTest extends SiriTestBase {

  @Test
  public void addSubscription() {
    Subscription subscription = createSubscription(SiriDataType.VEHICLE_MONITORING);
    service.addSubscription(subscription);
    service.activateSubscription(subscription.getSubscriptionId());
    assertThat(service.subscriptionCount(), is(3L));
  }

  @Test
  public void updateNonExistingSubscription() {
    Subscription subscription = createSubscription(SiriDataType.VEHICLE_MONITORING);
    service.update(subscription);
    assertThat(service.subscriptionCount(), is(3L));
  }

  @Test
  public void touchSubscription() {
    Subscription subscription = createSubscription(SiriDataType.VEHICLE_MONITORING);
    service.addSubscription(subscription);

    service.touch(subscription.getSubscriptionId());
    Instant lastActivity = service.getLastActivity(subscription.getSubscriptionId());

    assertThat(lastActivity, is(service.getLastActivity(subscription.getSubscriptionId())));
  }

  @Test
  public void stopSubscriptionTest() {
    Subscription subscription = createSubscription(SiriDataType.VEHICLE_MONITORING);
    service.addSubscription(subscription);
    service.stopSubscription(subscription);
  }

}






















