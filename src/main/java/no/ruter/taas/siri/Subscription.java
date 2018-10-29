package no.ruter.taas.siri;

import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.Singular;
import no.ruter.taas.dto.SubscriptionDto;
import no.ruter.taas.siri20.util.SiriDataType;
import org.apache.commons.lang3.RandomStringUtils;


@AllArgsConstructor
@Builder
@Data
public class Subscription implements Serializable {

  private static final long serialVersionUID = 1L;

  public Subscription() {
  }

  public Subscription(@Valid SubscriptionDto dto) {
    this.vendor = dto.getVendor();
    this.name = dto.getName();
    this.requestorRef = dto.getRequestorRef();
    this.subscriberRef = dto.getSubscriberRef();
    this.subscriptionId = dto.getSubscriptionId();
    this.subscriptionType = dto.getSubscriptionType();
    this.urlMap = dto.getUrlMap();
    this.ssl = dto.isSsl();
    this.heartbeatInterval = dto.getHeartbeatInterval();
    this.subscriptionDuration = dto.getSubscriptionDuration();
    this.changeBeforeUpdates = dto.getChangeBeforeUpdates();
    this.previewInterval = dto.getPreviewInterval();
    this.incrementalUpdates = dto.isIncrementalUpdates();
  }

  @Default
  private String internalId = RandomStringUtils.random(5);
  @Default
  private SiriDataType subscriptionType = SiriDataType.UNKNOWN;
  @Singular("urlMap")
  private Map<SiriRequestType, String> urlMap;
  @Default
  private boolean active = false;
  @Default
  private boolean ssl = false;

  private String vendor;
  private String name;

  @Default
  private String subscriptionId = UUID.randomUUID().toString();
  @Default
  private Instant activationTime = null;
  @Default
  private String consumerAddress = null;
  private String requestorRef;
  private String subscriberRef;
  private Duration heartbeatInterval;
  private Duration subscriptionDuration;
  private Duration updateInterval;
  private Duration changeBeforeUpdates;
  private Duration previewInterval;
  private boolean incrementalUpdates;

  @Default
  private String contentType = "text/xml";
  @Default
  private String encoding = "UTF-8";
  @Default
  private String datasetId = "RUT";
  @Default
  private String codespace = "RUT";

  public String getStartRoute() {
    return "start." + subscriptionId;
  }

  public String getCancelRoute() {
    return "cancel." + subscriptionId;
  }

  public String getCheckStatusRoute() {
    return "check.status." + subscriptionId;
  }

  public String getSubscribeEndpoint() {
    return this.urlMap.get(SiriRequestType.SUBSCRIBE) +
        (isSsl() ? "?sslContextParameters=#sslParam" : "");
  }

  public String getTerminationEndpoint() {
    return urlMap.get(SiriRequestType.DELETE_SUBSCRIPTION) +
        (isSsl() ? "?sslContextParameters=#sslParam" : "");
  }

  public String getCheckStatusEndpoint() {
    return urlMap.get(SiriRequestType.CHECK_STATUS) +
        (isSsl() ? "?sslContextParameters=#sslParam" : "");
  }


}
