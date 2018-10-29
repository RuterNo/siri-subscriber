package no.ruter.taas.route;

import java.util.ArrayList;
import java.util.List;
import no.ruter.events.avro.EstimatedCall;
import no.ruter.events.avro.FramedVehicleJourneyRef;
import no.ruter.events.avro.MonitoredCall;
import no.ruter.events.avro.OnwardCall;
import no.ruter.events.avro.PreviousCall;
import no.ruter.events.avro.SiriETEvent;
import no.ruter.events.avro.SiriVMData;
import no.ruter.events.avro.SiriVMEvent;
import no.ruter.events.avro.SiriVehicleActivityEvent;
import no.ruter.events.avro.locationFrame;
import no.ruter.events.avro.progressFrame;
import no.ruter.events.kafka.PropertiesFactory;
import no.ruter.events.kafka.SiriETEventProducer;
import no.ruter.events.kafka.SiriVMEventProducer;
import no.ruter.events.kafka.SiriVehicleActivityEventProducer;
import no.ruter.taas.config.AppConfig;
import org.apache.camel.spring.SpringRouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.org.siri.siri20.EstimatedVehicleJourney;
import uk.org.siri.siri20.EstimatedVehicleJourney.EstimatedCalls;
import uk.org.siri.siri20.MonitoredCallStructure;
import uk.org.siri.siri20.OnwardCallsStructure;
import uk.org.siri.siri20.PreviousCallsStructure;
import uk.org.siri.siri20.ProgressBetweenStopsStructure;
import uk.org.siri.siri20.Siri;
import uk.org.siri.siri20.VehicleActivityStructure;
import uk.org.siri.siri20.VehicleActivityStructure.MonitoredVehicleJourney;

@Component
public class KafkaAvroRoute extends SpringRouteBuilder {

  private final AppConfig config;

  @Autowired
  public KafkaAvroRoute(AppConfig config) {
    this.config = config;
  }


  @Override
  public void configure() throws Exception {
    SiriVMEventProducer siriVMEventProducer = new SiriVMEventProducer(PropertiesFactory
        .getAvroEventProducerProps(config.getBootstrapServers(), config.getSchemaRegistryUrl()));

    SiriVehicleActivityEventProducer siriVMPlusProducer = new SiriVehicleActivityEventProducer(
        PropertiesFactory.getAvroEventProducerProps(config.getBootstrapServers(),
            config.getSchemaRegistryUrl()));

    SiriETEventProducer siriETEventProducer = new SiriETEventProducer(PropertiesFactory
        .getAvroEventProducerProps(config.getBootstrapServers(), config.getSchemaRegistryUrl()));


    // @formatter:off
    from("direct:processor.vm")
        .process(p -> {
          Siri data = p.getMessage().getBody(Siri.class);
          final SiriVehicleActivityEvent siriVMPluss = populateSiriVMPlus(data);
          final SiriVMEvent siriVMEvent = populateSiriVMData(
              data.getServiceDelivery()
                  .getVehicleMonitoringDeliveries()
                  .get(0)
                  .getVehicleActivities()
                  .get(0));

          siriVMEventProducer.publish(siriVMEvent, config.getTopicAvroVm());
          siriVMPlusProducer.publish(siriVMPluss, config.getTopicAvroVmPlus());
        })
    ;

    from("direct:processor.et")
      .process(p -> {
        Siri data = p.getMessage().getBody(Siri.class);
        SiriETEvent siriETEvent = populateSiriETAvro(data);
        siriETEventProducer.publish(siriETEvent, config.getTopicAvroEt());
      })
    ;

    // @formatter:on
  }

  private SiriVMEvent populateSiriVMData(VehicleActivityStructure data) {
    final MonitoredVehicleJourney mvj = data.getMonitoredVehicleJourney();
    final SiriVMData siriVMData = new SiriVMData();
    final SiriVMEvent event = new SiriVMEvent();

    siriVMData
        .setDatedVehicleJourneyRef(mvj.getFramedVehicleJourneyRef().getDatedVehicleJourneyRef());
    siriVMData.setDataFrameRef(mvj.getFramedVehicleJourneyRef().getDataFrameRef().getValue());
    siriVMData.setBlockRef(mvj.getBlockRef().getValue());
    siriVMData.setDelay(mvj.getDelay().toString());
    siriVMData.setDestinationAimedArrivalTime(mvj.getDestinationAimedArrivalTime().toString());
    siriVMData.setOriginAimedDepartureTime(mvj.getOriginAimedDepartureTime().toString());
    siriVMData.setOperatorRef(mvj.getOperatorRef().getValue());
    siriVMData.setPublishedLineName(mvj.getPublishedLineNames().toString());
    siriVMData.setJourneyPatternRef(mvj.getJourneyPatternRef().getValue());
    siriVMData.setDirectionRef(mvj.getDirectionRef().getValue());
    siriVMData.setLatitude(mvj.getVehicleLocation().getLatitude().doubleValue());
    siriVMData.setLongitude(mvj.getVehicleLocation().getLongitude().doubleValue());
    siriVMData.setDestinationName(mvj.getDestinationNames().toString());
    siriVMData.setDestinationRef(mvj.getDestinationRef().getValue());
    siriVMData.setOriginName(mvj.getOriginNames().get(0).getValue());
    siriVMData.setOriginRef(mvj.getOriginRef().getValue());
    siriVMData.setLineRef(mvj.getLineRef().getValue());
    siriVMData.setValidUntil(data.getValidUntilTime().toString());
    siriVMData.setRecordedAt(data.getRecordedAtTime().toString());

    event.setVehicleRef(mvj.getVehicleRef().getValue());
    event.setSiriVMData(siriVMData);

    return event;
  }

  private SiriVehicleActivityEvent populateSiriVMPlus(Siri data) {
    final VehicleActivityStructure vas = data.getServiceDelivery()
        .getVehicleMonitoringDeliveries().get(0).getVehicleActivities().get(0);

    final ProgressBetweenStopsStructure pbs = data.getServiceDelivery()
        .getVehicleMonitoringDeliveries().get(0).getVehicleActivities().get(0)
        .getProgressBetweenStops();

    final MonitoredVehicleJourney mvj = data.getServiceDelivery()
        .getVehicleMonitoringDeliveries().get(0).getVehicleActivities()
        .get(0).getMonitoredVehicleJourney();

    final MonitoredCallStructure mc = mvj.getMonitoredCall();

    SiriVehicleActivityEvent event = new SiriVehicleActivityEvent();
    event.setRecordedAtTime(vas.getRecordedAtTime().toString());
    event.setValidUntilTime(vas.getValidUntilTime().toString());

    progressFrame pf = new progressFrame();
    pf.setLinkDistance(pbs.getLinkDistance().doubleValue());
    pf.setPercentage(pbs.getPercentage().doubleValue());
    event.setProgressBetweenStops(pf);

    no.ruter.events.avro.MonitoredVehicleJourney m = new no.ruter.events.avro.MonitoredVehicleJourney();
    m.setLineRef(mvj.getLineRef() == null ? "" : mvj.getLineRef().getValue());
    m.setDirectionRef(mvj.getDirectionRef() == null ? "" : mvj.getDirectionRef().getValue());
    m.setOperatorRef(mvj.getOperatorRef() == null ? "" : mvj.getOperatorRef().getValue());
    m.setJourneyPatternRef(
        mvj.getJourneyPatternRef() == null ? "" : mvj.getJourneyPatternRef().getValue());
    m.setPublishedLineName(
        mvj.getPublishedLineNames() == null ? "" : mvj.getPublishedLineNames().toString());
    m.setOriginRef(mvj.getOriginRef() == null ? "" : mvj.getOriginRef().getValue());
    m.setOriginName(mvj.getOriginNames() == null ? "" : mvj.getOriginNames().get(0).getValue());
    m.setDestinationRef(mvj.getDestinationRef() == null ? "" : mvj.getDestinationRef().getValue());
    m.setDestinationName(
        mvj.getDestinationNames() == null ? "" : mvj.getDestinationNames().toString());
    m.setHeadwayService(mvj.isHeadwayService());
    m.setOriginAimedDepartureTime(mvj.getOriginAimedDepartureTime() == null ? ""
        : mvj.getOriginAimedDepartureTime().toString());
    m.setDestinationAimedArrivalTime(mvj.getDestinationAimedArrivalTime() == null ? ""
        : mvj.getDestinationAimedArrivalTime().toString());
    m.setMonitored(mvj.isMonitored());
    m.setInCongestion(mvj.isInCongestion());
    m.setInPanic(mvj.isInPanic());
    m.setDelay(mvj.getDelay() == null ? "" : mvj.getDelay().toString());
    m.setBlockRef(mvj.getBlockRef() == null ? "" : mvj.getBlockRef().getValue());
    m.setVehicleRef(mvj.getVehicleRef() == null ? "" : mvj.getVehicleRef().getValue());

    FramedVehicleJourneyRef f = new FramedVehicleJourneyRef();

    if (mvj.getFramedVehicleJourneyRef() != null) {
      f.setDataFrameRef(mvj.getFramedVehicleJourneyRef().getDataFrameRef() == null ? ""
          : mvj.getFramedVehicleJourneyRef().getDataFrameRef().getValue());
      f.setDatedVehicleJourneyRef(
          mvj.getFramedVehicleJourneyRef().getDatedVehicleJourneyRef() == null ? ""
              : mvj.getFramedVehicleJourneyRef().getDatedVehicleJourneyRef());
      m.setFramedVehicleJourneyRef(f);
    }

    locationFrame lframe = new locationFrame();
    if (mvj.getVehicleLocation() != null) {
      lframe.setLatitude(mvj.getVehicleLocation().getLatitude() == null ? 0D
          : mvj.getVehicleLocation().getLatitude().doubleValue());
      lframe.setLongitude(mvj.getVehicleLocation().getLongitude() == null ? 0D
          : mvj.getVehicleLocation().getLongitude().doubleValue());
      m.setVehicleLocation(lframe);
    }

    m.setPreviousCalls(buildPreviousCalls(mvj.getPreviousCalls()));
    MonitoredCall monitoredCall = new MonitoredCall();
    monitoredCall.setStopPointRef(
        mc.getStopPointRef() == null ? "" : mc.getStopPointRef().getValue());
    monitoredCall.setVisitNumber(mc.getVisitNumber() == null ? "" : mc.getVisitNumber().toString());
    monitoredCall.setStopPointName(mc.getStopPointNames() == null ? ""
        : mc.getStopPointNames().get(0).getValue());
    monitoredCall.setVehicleAtStop(mc.isVehicleAtStop());
    monitoredCall.setDestinationDisplay(mc.getDestinationDisplaies() == null ? ""
        : mc.getDestinationDisplaies().get(0).getValue());
    monitoredCall.setAimedArrivalTime(
        mc.getAimedArrivalTime() == null ? "" : mc.getAimedArrivalTime().toString());
    monitoredCall.setAimedDepartureTime(
        mc.getAimedDepartureTime() == null ? "" : mc.getAimedDepartureTime().toString());
    monitoredCall.setExpectedArrivalTime(
        mc.getExpectedArrivalTime() == null ? "" : mc.getExpectedArrivalTime().toString());
    monitoredCall.setExpectedDepartureTime(
        mc.getExpectedDepartureTime() == null ? "" : mc.getExpectedDepartureTime().toString());
    monitoredCall.setDeparturePlatformName(mc.getDeparturePlatformName() == null ? ""
        : mc.getDeparturePlatformName().getValue());
    m.setMonitoredCall(monitoredCall);
    m.setOnwardCalls(buildOnwardCall(mvj.getOnwardCalls()));

    event.setMonitoredVehicleJourney(m);

    return event;
  }

  private List<OnwardCall> buildOnwardCall(OnwardCallsStructure onwardCalls) {
    List<OnwardCall> list = new ArrayList<>();
    onwardCalls.getOnwardCalls().forEach(oc -> {
      OnwardCall onwardCall = new OnwardCall();
      onwardCall.setStopPointRef(
          oc.getStopPointRef() == null ? "" : oc.getStopPointRef().getValue());
      onwardCall.setVisitNumber(oc.getVisitNumber() == null ? "" : oc.getVisitNumber().toString());
      onwardCall.setStopPointName(oc.getStopPointNames() == null ? ""
          : oc.getStopPointNames().get(0).getValue());
      onwardCall.setAimedArrivalTime(
          oc.getAimedArrivalTime() == null ? "" : oc.getAimedArrivalTime().toString());
      onwardCall.setAimedDepartureTime(
          oc.getAimedDepartureTime() == null ? "" : oc.getAimedDepartureTime().toString());
      onwardCall.setExpectedArrivalTime(
          oc.getExpectedArrivalTime() == null ? "" : oc.getExpectedArrivalTime().toString());
      onwardCall.setExpectedDepartureTime(
          oc.getExpectedDepartureTime() == null ? "" : oc.getExpectedDepartureTime().toString());

      list.add(onwardCall);
    });

    return list;
  }

  private List<PreviousCall> buildPreviousCalls(PreviousCallsStructure pc) {
    List<PreviousCall> list = new ArrayList<>();
    pc.getPreviousCalls().forEach(p -> {
      if (p == null) {
        return;
      }

      PreviousCall prev = new PreviousCall();
      prev.setActualArrivalTime(
          p.getActualArrivalTime() == null ? "" : p.getActualArrivalTime().toString());
      prev.setActualDepartureTime(
          p.getActualDepartureTime() == null ? "" : p.getActualDepartureTime().toString());
      prev.setAimedArrivalTime(
          p.getAimedArrivalTime() == null ? "" : p.getAimedArrivalTime().toString());
      prev.setAimedDepartureTime(
          p.getAimedDepartureTime() == null ? "" : p.getAimedDepartureTime().toString());
      prev.setStopPointName(
          p.getStopPointNames() == null ? "" : p.getStopPointNames().get(0).getValue());
      prev.setVisitNumber(p.getVisitNumber() == null ? "" : p.getVisitNumber().toString());
      prev.setStopPointRef(p.getStopPointRef() == null ? "" : p.getStopPointRef().getValue());

      list.add(prev);
    });

    return list;
  }

  private SiriETEvent populateSiriETAvro(Siri data) {
    EstimatedVehicleJourney evj = data.getServiceDelivery()
        .getEstimatedTimetableDeliveries().get(0).getEstimatedJourneyVersionFrames().get(0)
        .getEstimatedVehicleJourneies().get(0);

   return SiriETEvent.newBuilder()
        .setLineRef(evj.getLineRef().getValue())
        .setDirectionRef(evj.getDirectionRef().getValue())
        .setFramedVehicleJourneyRef(FramedVehicleJourneyRef.newBuilder()
            .setDatedVehicleJourneyRef(evj.getFramedVehicleJourneyRef().getDatedVehicleJourneyRef())
            .setDatedVehicleJourneyRef(
                evj.getFramedVehicleJourneyRef().getDataFrameRef().getValue())
            .build())
        .setCancellation(evj.isCancellation())
        .setJourneyPatternRef(evj.getJourneyPatternRef().getValue())
        .setPublishedLineName(evj.getPublishedLineNames().get(0).getValue())
        .setOperatorRef(evj.getOperatorRef().getValue())
        .setMonitored(evj.isMonitored())
        .setBlockRef(evj.getBlockRef().getValue())
        .setVehicleRef(evj.getVehicleRef().getValue())
        .setEstimatedCalls(populateEstimateCall(evj.getEstimatedCalls()))

        .build();
  }

  private List<EstimatedCall> populateEstimateCall(EstimatedCalls e) {

    return null;
  }

}






















