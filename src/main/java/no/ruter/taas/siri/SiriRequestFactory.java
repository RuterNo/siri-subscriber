package no.ruter.taas.siri;

import uk.org.siri.siri20.Siri;

/**
 * Request factory that is dynamically called from camel routes
 */
public class SiriRequestFactory {

  private Subscription subscription;
  private SiriObjectFactory factory;

  public SiriRequestFactory(Subscription subscription) {
    this.subscription = subscription;
    this.factory = new SiriObjectFactory();
  }

  public Siri createSubscriptionRequest() {
    return factory.createSubscriptionRequest(this.subscription);
  }

  public Siri createSiriTerminationRequest() {
    return factory.createTerminateSubscriptionRequest(this.subscription);
  }

  public Siri createSiriCheckStatusRequest() {
    return factory.createCheckStatusRequest(this.subscription);
  }

}
