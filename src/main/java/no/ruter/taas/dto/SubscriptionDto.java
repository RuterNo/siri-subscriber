package no.ruter.taas.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.io.Serializable;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import no.ruter.taas.siri.SiriRequestType;
import no.ruter.taas.siri20.util.SiriDataType;
import no.ruter.taas.validation.NullOrNotBlank;

@ApiModel(description = "Subscription")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
@ToString
public class SubscriptionDto implements Serializable {

  @NotBlank
  @ApiModelProperty(value = "The vendor this subscription is subscribing to",
      example = "SIS", required = true)
  private String vendor;

  @NotBlank
  @ApiModelProperty(
      value = "The name of this subscription.", example = "SIS SX", required = true)
  private String name;
  @NotBlank
  @ApiModelProperty(value = "The requestor ref.", example = "ruter-sx", required = true)
  private String requestorRef;

  @NotBlank
  @ApiModelProperty(value = "the subscriber ref.", example = "ruter-sx", required = true)
  private String subscriberRef;

  @NullOrNotBlank
  @Size(min = 36, max = 36)
  @ApiModelProperty(
      value = "An unique identifier for the subscription. "
          + "If one is not supplied an UUID will be generated",
      example = "ab5dabd5-2c14-4539-b9a1-d558193dcacb")
  private String subscriptionId = UUID.randomUUID().toString();

  @NotNull
  @ApiModelProperty(value = "The subscription Type.", example = "VEHICLE_MONITORING")
  private SiriDataType subscriptionType = SiriDataType.VEHICLE_MONITORING;

  @NotNull
  @ApiModelProperty(value = "A map of endpoint URLS.",
      example = "http://some-host/endpoint", required = true)
  private Map<SiriRequestType, String> urlMap;

  @ApiModelProperty(value = "If subscription endpoint uses SSL.")
  private boolean ssl;

  @NotNull
  @ApiModelProperty(value = "The interval which heartbeat should be sent.",
      example = "PT5M", required = true)
  private Duration heartbeatInterval;

  @NotNull
  @ApiModelProperty(value = "The duration of the subscription.", example = "PT500H")
  private Duration subscriptionDuration;

  // todo: the fields below needs ApiModelPoperties..

  @ApiModelProperty(value = "", example = "")
  private Duration changeBeforeUpdates;

  @ApiModelProperty(value = "", example = "")
  private Duration previewInterval;

  @ApiModelProperty(value = "", example = "")
  private boolean incrementalUpdates;


}
