package no.ruter.taas.http;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.ws.rs.core.MediaType;
import lombok.extern.slf4j.Slf4j;
import no.ruter.taas.dto.SubscriptionDto;
import no.ruter.taas.service.SubscriptionService;
import no.ruter.taas.siri.Subscription;
import org.apache.camel.EndpointInject;
import org.apache.camel.FluentProducerTemplate;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class SiriRestController {

  @EndpointInject(uri = "seda:incoming")
  private FluentProducerTemplate request;

  @Autowired
  private SubscriptionService service;


  @PostMapping(consumes = {MediaType.TEXT_XML, MediaType.APPLICATION_XML})
  public ResponseEntity anyPath(@RequestBody String xml, HttpServletRequest req) {
    Subscription subscription;
    try {
      subscription = service.lookupURI(req.getRequestURI());
    } catch (Exception e) {
      e.printStackTrace();
      return ResponseEntity.notFound().build();
    }

    request.withBody(xml)
        .withHeader("subscriptionId", subscription.getSubscriptionId())
        .withHeader("subscriptionType", subscription.getSubscriptionType())
        .withHeader("vendorName", subscription.getVendor())
        .withHeader("subscriptionName", subscription.getName())
        .asyncRequest();

    return ResponseEntity.ok().build();
  }

  @PostMapping(value = "/admin/start/{id}")
  public ResponseEntity startSubscription(@PathVariable String id) {
    Subscription subscription = service.getSubscription(id);
    if (subscription.isActive()) {
      return ResponseEntity.ok().build();
    }

    service.activateSubscription(id);
    return ResponseEntity.ok().build();
  }

  @PostMapping(value = "/admin/stop/{id}")
  public ResponseEntity stopSubscription(@PathVariable String id) {
    service.stopSubscription(id);
    return ResponseEntity.ok().build();
  }

  @PostMapping(value = "/admin/add", consumes = MediaType.APPLICATION_JSON)
  public ResponseEntity addSubscription(@Valid SubscriptionDto dto) {
    throw new NotImplementedException("Please implement me!");

//    return ResponseEntity.ok().build();
  }

  @PutMapping(value = "/admin/update", consumes = MediaType.APPLICATION_JSON)
  public ResponseEntity updateSubscription(Subscription dto) {
    Subscription subscription = service.getSubscription(dto.getSubscriptionId());
    subscription.setName(dto.getName());
    subscription.setVendor(dto.getVendor());
    subscription.setUrlMap(dto.getUrlMap());
    subscription.setConsumerAddress(dto.getConsumerAddress());
    subscription.setHeartbeatInterval(dto.getHeartbeatInterval());
    subscription.setSubscriberRef(dto.getSubscriberRef());
    subscription.setRequestorRef(dto.getRequestorRef());
    subscription.setSubscriptionDuration(dto.getSubscriptionDuration());
    subscription.setActive(dto.isActive());
    subscription.setSsl(dto.isSsl());
    subscription.setUpdateInterval(dto.getUpdateInterval());
    subscription.setPreviewInterval(dto.getPreviewInterval());

    service.update(subscription);

    return ResponseEntity.ok().build();
  }

  @GetMapping(value = "/admin/all", produces = MediaType.APPLICATION_JSON)
  public ResponseEntity getSubscriptions() {
    return ResponseEntity.ok(service.getAllSubscriptions());
  }

  @GetMapping(value = "/admin/{id}", produces = MediaType.APPLICATION_JSON)
  public ResponseEntity getSubscription(@PathVariable String id) {
    return ResponseEntity.ok(service.getSubscription(id));
  }

  @GetMapping(value = "/admin/state", produces = MediaType.APPLICATION_JSON)
  public ResponseEntity getState() {
    return ResponseEntity.ok(service.getStates());
  }

  @DeleteMapping(value = "/admin/{id}")
  public ResponseEntity deleteSubscription(@PathVariable String id) {
    service.stopSubscription(id);
    return ResponseEntity.ok().build();
  }
}
