package no.ruter.taas.route;

public final class Constants {

  public static final String SINGLETON_ROUTE_DEFINITION_GROUP_NAME = "siriSubscriberSingletonRoute";

  private static final String QUEUE_PREFIX = "siri.subscriber";
  private static final String DEFAULT_PROCESSOR_QUEUE = QUEUE_PREFIX + ".process";

  static final String ROUTER_QUEUE = QUEUE_PREFIX + ".router";
  static final String ADMIN_ROUTER_QUEUE = QUEUE_PREFIX + ".admin.router";

  static final String SITUATION_EXCHANGE_QUEUE = DEFAULT_PROCESSOR_QUEUE + ".sx";
  static final String ESTIMATED_TIMETABLE_QUEUE = DEFAULT_PROCESSOR_QUEUE + ".et";
  static final String VEHICLE_MONITORING_QUEUE = DEFAULT_PROCESSOR_QUEUE + ".vm";

  static final String HEARTBEAT_QUEUE = DEFAULT_PROCESSOR_QUEUE + ".heartbeat";
  static final String CHECK_STATUS_QUEUE = DEFAULT_PROCESSOR_QUEUE + ".check.status";
  static final String SUBSCRIPTION_RESPONSE = DEFAULT_PROCESSOR_QUEUE + ".subscription";
  static final String TERMINATION_RESPONSE = DEFAULT_PROCESSOR_QUEUE + ".terminate";

  public static final String INCOMING_HTTP_ROUTE = "undertow:http://0.0.0.0:{{siri.incoming-port}}"
      + "?matchOnUriPrefix=true&httpMethodRestrict=POST";

  static final String TOKENIZE_VM =
      "/siri:Siri/siri:ServiceDelivery/siri:VehicleMonitoringDelivery/siri:VehicleActivity";
  static final String TOKENIZE_SX =
      "/siri:Siri/siri:ServiceDelivery/siri:SituationExchangeDelivery/"
          + "siri:Situations/siri:PtSituationElement";
  static final String TOKENIZE_ET =
      "/siri:Siri/siri:ServiceDelivery/siri:EstimatedTimetableDelivery/"
          + "siri:EstimatedJourneyVersionFrame/siri:EstimatedVehicleJourney";

  static final String SX_XPATH = "/siri:Siri/siri:ServiceDelivery/siri:SituationExchangeDelivery";
  static final String VM_XPATH = "/siri:Siri/siri:ServiceDelivery/siri:VehicleMonitoringDelivery";
  static final String ET_XPATH = "/siri:Siri/siri:ServiceDelivery/siri:EstimatedTimetableDelivery";

  static final String PRODUCER_XPATH = "/siri:Siri/siri:ServiceDelivery/siri:ProducerRef";

  static final String SUBSCRIPTION_RESPONSE_XPATH = "/siri:Siri/siri:SubscriptionResponse";
  static final String HEARTBEAT_XPATH = "/siri:Siri/siri:HeartbeatNotification";
  static final String TERMINATION_RESPONSE_XPATH = "/siri:Siri/siri:TerminateSubscriptionResponse";
  static final String CHECK_RESPONSE_XPATH = "/siri:Siri/siri:CheckStatusResponse";


  static final String AMQ_CONSUMER_PARAM =
      "?asyncConsumer=true&concurrentConsumers={{siri.concurrent-consumers}}";

  static final String AMQ_PRODUCER_PARAM =
      "?disableReplyTo=true&timeToLive={{siri.incoming-time-to-live}}";


}
