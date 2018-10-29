package no.ruter.taas.route;

import static no.ruter.taas.route.Constants.AMQ_CONSUMER_PARAM;
import static no.ruter.taas.route.Constants.ESTIMATED_TIMETABLE_QUEUE;
import static no.ruter.taas.route.Constants.SITUATION_EXCHANGE_QUEUE;
import static no.ruter.taas.route.Constants.VEHICLE_MONITORING_QUEUE;

import java.io.InputStream;
import no.ruter.taas.siri.kafka.field.DatedVehicleJourney;
import no.ruter.taas.siri.kafka.field.SituationNumber;
import no.ruter.taas.siri.kafka.field.VehicleId;
import no.ruter.taas.siri.kafka.serialization.DatedVehicleJourneyKeySerializer;
import no.ruter.taas.siri.kafka.serialization.SiriSerializer;
import no.ruter.taas.siri.kafka.serialization.SituationNumberKeySerializer;
import no.ruter.taas.siri.kafka.serialization.VehicleKeySerializer;
import no.ruter.taas.siri20.util.SiriXml;
import org.apache.camel.component.kafka.KafkaConstants;
import org.apache.camel.spring.SpringRouteBuilder;
import org.springframework.stereotype.Component;
import uk.org.siri.siri20.Siri;

@Component
public class KafkaRoute extends SpringRouteBuilder {

  @Override
  public void configure() {
    // @formatter:off

    from("activemq:queue:" + VEHICLE_MONITORING_QUEUE + AMQ_CONSUMER_PARAM)
        .routeId("kafka.vm")
        .process(p -> {
          Siri data = SiriXml.parseXml(p.getMessage().getBody(InputStream.class));
          VehicleId key = VehicleId.of(
              data.getServiceDelivery()
                  .getVehicleMonitoringDeliveries()
                  .get(0)
                  .getVehicleActivities()
                  .get(0).getMonitoredVehicleJourney()
                  .getVehicleRef()
                  .getValue());
          p.getOut().setHeader(KafkaConstants.KEY, key);
          p.getOut().setBody(data);
        })
        .to("kafka:{{siri.topic-vm}}" +
            "?serializerClass=" + SiriSerializer.class.getName() +
            "&keySerializerClass=" + VehicleKeySerializer.class.getName())
        .choice()
        .when(simple("{{siri.avro-processor-enabled}}"))
          .to("direct:processor.vm")
    ;

    from("activemq:queue:" + SITUATION_EXCHANGE_QUEUE + AMQ_CONSUMER_PARAM)
        .routeId("kafka.sx")
        .process(p -> {
          Siri data = SiriXml.parseXml(p.getMessage().getBody(InputStream.class));
          SituationNumber key = SituationNumber.of(data.getServiceDelivery()
              .getSituationExchangeDeliveries()
              .get(0)
              .getSituations()
              .getPtSituationElements()
              .get(0)
              .getSituationNumber()
              .getValue());
          p.getOut().setHeader(KafkaConstants.KEY, key);
          p.getOut().setBody(data);
        })
        .to("kafka:{{siri.topic-sx}}" +
            "?serializerClass=" + SiriSerializer.class.getName() +
            "&keySerializerClass=" + SituationNumberKeySerializer.class.getName())
        .choice()
        .when(simple("{{siri.avro-processor-enabled}}"))
          .to("direct:processor.sx")
    ;

    from("activemq:queue:" + ESTIMATED_TIMETABLE_QUEUE + AMQ_CONSUMER_PARAM)
        .routeId("kafka.et")
        .process(p -> {
          Siri data = SiriXml.parseXml(p.getMessage().getBody(InputStream.class));
          DatedVehicleJourney key = DatedVehicleJourney.of(
              data.getServiceDelivery()
                  .getEstimatedTimetableDeliveries()
                  .get(0)
                  .getEstimatedJourneyVersionFrames()
                  .get(0)
                  .getEstimatedVehicleJourneies()
                  .get(0)
                  .getFramedVehicleJourneyRef()
                  .getDatedVehicleJourneyRef());
          p.getOut().setHeader(KafkaConstants.KEY, key);
          p.getOut().setBody(data);
        })
        .to("kafka:{{siri.topic-et}}" +
            "?serializerClass=" + SiriSerializer.class.getName() +
            "&keySerializerClass=" + DatedVehicleJourneyKeySerializer.class.getName())
        .choice()
        .when(simple("{{siri.avro-processor-enabled}}"))
          .to("direct:processor.et")
    ;
    // @formatter:on
  }
}
