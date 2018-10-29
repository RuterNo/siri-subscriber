package no.ruter.taas.siri;

public enum SiriRequestType {
  SUBSCRIBE,
  HEARTBEAT,
  DELETE_SUBSCRIPTION,
  CHECK_STATUS,
  GET_VEHICLE_MONITORING,
  GET_SITUATION_EXCHANGE,
  GET_ESTIMATED_TIMETABLE;

  @Override
  public String toString() {
    return super.toString();
  }
}