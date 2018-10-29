package no.ruter.taas.siri;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import lombok.extern.slf4j.Slf4j;
import no.ruter.taas.siri20.util.SiriDataType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.org.siri.siri20.CheckStatusRequestStructure;
import uk.org.siri.siri20.EstimatedTimetableRequestStructure;
import uk.org.siri.siri20.EstimatedTimetableSubscriptionStructure;
import uk.org.siri.siri20.HeartbeatNotificationStructure;
import uk.org.siri.siri20.MessageQualifierStructure;
import uk.org.siri.siri20.RequestorRef;
import uk.org.siri.siri20.Siri;
import uk.org.siri.siri20.SituationExchangeRequestStructure;
import uk.org.siri.siri20.SituationExchangeSubscriptionStructure;
import uk.org.siri.siri20.SubscriptionContextStructure;
import uk.org.siri.siri20.SubscriptionQualifierStructure;
import uk.org.siri.siri20.SubscriptionRequest;
import uk.org.siri.siri20.TerminateSubscriptionRequestStructure;
import uk.org.siri.siri20.VehicleMonitoringRequestStructure;
import uk.org.siri.siri20.VehicleMonitoringSubscriptionStructure;

@SuppressWarnings("WeakerAccess")
@Slf4j
@Component
public class SiriObjectFactory {

  private static final String SIRI_VERSION = "2.0";

  @Autowired
  private Instant serverStartTime;

  private SubscriptionRequest createSituationExchangeSubscriptionRequest(
      String requestorRef, String subscriberRef, String subscriptionId, Duration heartbeatInterval,
      String consumerAddress, Duration subscriptionDuration, Duration previewInterval,
      boolean incrementalUpdates,
      Duration changeBeforeUpdates) {

    SubscriptionRequest request = createSubscriptionRequest(requestorRef, heartbeatInterval,
        consumerAddress);
    SituationExchangeRequestStructure sxRequest = createSituationExchangeRequestStructure(null);

    SituationExchangeSubscriptionStructure sxSubscriptionReq = new SituationExchangeSubscriptionStructure();
    sxSubscriptionReq.setSituationExchangeRequest(sxRequest);
    sxSubscriptionReq.setSubscriptionIdentifier(createSubscriptionIdentifier(subscriptionId));
    sxSubscriptionReq.setInitialTerminationTime(
        ZonedDateTime.now().plusSeconds(subscriptionDuration.getSeconds()));
    sxSubscriptionReq.setSubscriberRef(createRequestorRef(subscriberRef));
    sxSubscriptionReq.setIncrementalUpdates(incrementalUpdates);

    request.getSituationExchangeSubscriptionRequests().add(sxSubscriptionReq);

    return request;
  }

  private SubscriptionRequest createEstimatedTimetableSubscriptionRequest(
      String requestorRef, String subscriberRef, String subscriptionId, Duration heartbeatInterval,
      String consumerAddress, Duration subscriptionDuration, Duration previewInterval,
      boolean incrementalUpdates, Duration changeBeforeUpdates) {

    SubscriptionRequest request = createSubscriptionRequest(requestorRef, heartbeatInterval,
        consumerAddress);

    EstimatedTimetableRequestStructure etRequest = new EstimatedTimetableRequestStructure();
    etRequest.setRequestTimestamp(ZonedDateTime.now());
    etRequest.setVersion(SIRI_VERSION);

    if (previewInterval != null) {
      etRequest.setPreviewInterval(createDataTypeFactory().newDuration(previewInterval.toString()));
    }

    EstimatedTimetableSubscriptionStructure etSubscriptionReq =
        new EstimatedTimetableSubscriptionStructure();
    etSubscriptionReq.setEstimatedTimetableRequest(etRequest);
    etSubscriptionReq.setSubscriptionIdentifier(createSubscriptionIdentifier(subscriptionId));
    etSubscriptionReq.setInitialTerminationTime(
        ZonedDateTime.now().plusSeconds(subscriptionDuration.getSeconds()));
    etSubscriptionReq.setSubscriberRef(createRequestorRef(subscriberRef));

    if (changeBeforeUpdates != null) {
      etSubscriptionReq.setChangeBeforeUpdates(
          createDataTypeFactory().newDuration(changeBeforeUpdates.toString()));
    }

    etSubscriptionReq.setIncrementalUpdates(incrementalUpdates);
    request.getEstimatedTimetableSubscriptionRequests().add(etSubscriptionReq);

    return request;
  }

  private SubscriptionRequest createVehicleMonitoringSubscriptionRequest(
      String requestorRef, String subscriberRef, String subscriptionId, Duration heartbeatInterval,
      String consumerAddress, Duration subscriptionDuration, Duration updateInterval,
      boolean incrementalUpdates) {

    SubscriptionRequest request = createSubscriptionRequest(requestorRef, heartbeatInterval,
        consumerAddress);

    VehicleMonitoringRequestStructure vmRequest = new VehicleMonitoringRequestStructure();
    vmRequest.setRequestTimestamp(ZonedDateTime.now());
    vmRequest.setVersion(SIRI_VERSION);

    VehicleMonitoringSubscriptionStructure vmSubscriptionReq =
        new VehicleMonitoringSubscriptionStructure();
    vmSubscriptionReq.setVehicleMonitoringRequest(vmRequest);
    vmSubscriptionReq.setSubscriptionIdentifier(createSubscriptionIdentifier(subscriptionId));
    vmSubscriptionReq.setInitialTerminationTime(
        ZonedDateTime.now().plusSeconds(subscriptionDuration.getSeconds()));
    vmSubscriptionReq.setSubscriberRef(createRequestorRef(subscriberRef));

    if (updateInterval != null) {
      //Requesting updates every second
      vmSubscriptionReq
          .setUpdateInterval(createDataTypeFactory().newDuration(updateInterval.toString()));
    }

    vmSubscriptionReq.setIncrementalUpdates(incrementalUpdates);
    request.getVehicleMonitoringSubscriptionRequests().add(vmSubscriptionReq);

    return request;
  }

  private SituationExchangeRequestStructure createSituationExchangeRequestStructure(
      Duration previewInterval) {
    SituationExchangeRequestStructure sxRequest = new SituationExchangeRequestStructure();
    sxRequest.setRequestTimestamp(ZonedDateTime.now());
    sxRequest.setVersion(SIRI_VERSION);
    sxRequest.setMessageIdentifier(createMessageIdentifier());

    if (previewInterval != null) {
      sxRequest.setPreviewInterval(createDataTypeFactory().newDuration(previewInterval.toString()));
    }
    return sxRequest;
  }

  private SubscriptionRequest createSubscriptionRequest(String requestorRef,
      Duration heartbeatInterval,
      String address) {
    SubscriptionRequest request = new SubscriptionRequest();
    request.setRequestorRef(createRequestorRef(requestorRef));
    request.setMessageIdentifier(createMessageIdentifier(UUID.randomUUID().toString()));
    request.setConsumerAddress(address);
    request.setRequestTimestamp(ZonedDateTime.now());

    if (heartbeatInterval != null) {
      SubscriptionContextStructure ctx = new SubscriptionContextStructure();
      ctx.setHeartbeatInterval(createDataTypeFactory().newDuration(heartbeatInterval.toString()));

      request.setSubscriptionContext(ctx);
    }
    return request;
  }


  private MessageQualifierStructure createMessageIdentifier(String value) {
    MessageQualifierStructure msgId = new MessageQualifierStructure();
    msgId.setValue(value);
    return msgId;
  }

  private MessageQualifierStructure createMessageIdentifier() {
    return createMessageIdentifier(UUID.randomUUID().toString());
  }

  private static RequestorRef createRequestorRef(String value) {
    if (value == null) {
      value = UUID.randomUUID().toString();
    }
    RequestorRef requestorRef = new RequestorRef();
    requestorRef.setValue(value);
    return requestorRef;
  }

  private static DatatypeFactory createDataTypeFactory() {
    try {
      return DatatypeFactory.newInstance();
    } catch (DatatypeConfigurationException e) {
      throw new IllegalStateException(e);
    }
  }

  private static SubscriptionQualifierStructure createSubscriptionIdentifier(
      String subscriptionId) {
    SubscriptionQualifierStructure subscriptionRef = new SubscriptionQualifierStructure();
    subscriptionRef.setValue(subscriptionId);
    return subscriptionRef;
  }

  private static Siri createSiriObject() {
    Siri siri = new Siri();
    siri.setVersion(SIRI_VERSION);
    return siri;
  }

  public Siri createSubscriptionRequest(Subscription subscription) {
    Siri siri = createSiriObject();

    SubscriptionRequest request = null;

    if (subscription.getSubscriptionType().equals(SiriDataType.VEHICLE_MONITORING)) {
      request = createVehicleMonitoringSubscriptionRequest(
          subscription.getRequestorRef(),
          subscription.getSubscriberRef(),
          subscription.getSubscriptionId(),
          subscription.getHeartbeatInterval(),
          subscription.getConsumerAddress(),
          subscription.getSubscriptionDuration(),
          subscription.getUpdateInterval(),
          subscription.isIncrementalUpdates()
      );
    }

    if (subscription.getSubscriptionType().equals(SiriDataType.ESTIMATED_TIMETABLE)) {
      request = createEstimatedTimetableSubscriptionRequest(
          subscription.getRequestorRef(),
          subscription.getSubscriberRef(),
          subscription.getSubscriptionId(),
          subscription.getHeartbeatInterval(),
          subscription.getConsumerAddress(),
          subscription.getSubscriptionDuration(),
          subscription.getPreviewInterval(),
          subscription.isIncrementalUpdates(),
          subscription.getChangeBeforeUpdates()
      );
    }

    if (subscription.getSubscriptionType().equals(SiriDataType.SITUATION_EXCHANGE)) {
      request = createSituationExchangeSubscriptionRequest(
          subscription.getRequestorRef(),
          subscription.getSubscriberRef(),
          subscription.getSubscriptionId(),
          subscription.getHeartbeatInterval(),
          subscription.getConsumerAddress(),
          subscription.getSubscriptionDuration(),
          subscription.getPreviewInterval(),
          subscription.isIncrementalUpdates(),
          subscription.getChangeBeforeUpdates()
      );
    }

    siri.setSubscriptionRequest(request);

    return siri;
  }

  public Siri createHeartbeatNotification(String requestorRef) {
    Siri siri = createSiriObject();
    HeartbeatNotificationStructure heartbeat = new HeartbeatNotificationStructure();
    heartbeat.setStatus(true);
    heartbeat.setServiceStartedTime(serverStartTime.atZone(ZoneId.systemDefault()));
    heartbeat.setRequestTimestamp(ZonedDateTime.now());
    heartbeat.setProducerRef(createRequestorRef(requestorRef));
    siri.setHeartbeatNotification(heartbeat);

    return siri;
  }


  private Siri createTerminateSubscriptionRequest(String subscriptionId,
      RequestorRef requestorRef) {
    if (requestorRef == null || requestorRef.getValue() == null) {
      log.warn("RequestorRef cannot be null");
      return null;
    }
    TerminateSubscriptionRequestStructure terminationReq = new TerminateSubscriptionRequestStructure();

    terminationReq.setRequestTimestamp(ZonedDateTime.now());
    terminationReq.getSubscriptionReves().add(createSubscriptionIdentifier(subscriptionId));
    terminationReq.setRequestorRef(requestorRef);
    terminationReq.setMessageIdentifier(createMessageIdentifier(UUID.randomUUID().toString()));

    Siri siri = createSiriObject();
    siri.setTerminateSubscriptionRequest(terminationReq);
    return siri;
  }

  public Siri createTerminateSubscriptionRequest(Subscription subscription) {
    if (subscription == null) {
      return null;
    }
    return createTerminateSubscriptionRequest(subscription.getSubscriptionId(),
        createRequestorRef(subscription.getRequestorRef()));
  }

  public Siri createCheckStatusRequest(Subscription subscription) {
    Siri siri = createSiriObject();

    CheckStatusRequestStructure statusRequest = new CheckStatusRequestStructure();
    statusRequest.setRequestTimestamp(ZonedDateTime.now());
    statusRequest.setMessageIdentifier(createMessageIdentifier());
    statusRequest.setRequestorRef(createRequestorRef(subscription.getRequestorRef()));
    siri.setCheckStatusRequest(statusRequest);

    return siri;
  }


}
