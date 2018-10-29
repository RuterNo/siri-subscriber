package no.ruter.taas.config;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import no.ruter.taas.route.SubscriptionRoute;
import no.ruter.taas.service.SubscriptionService;
import no.ruter.taas.siri.Subscription;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SubscriptionLoader {

  private final AppConfig config;
  private final SubscriptionService service;
  private final CamelContext camelContext;

  @Autowired
  public SubscriptionLoader(AppConfig config, SubscriptionService service,
      CamelContext camelContext) {
    this.config = config;
    this.service = service;
    this.camelContext = camelContext;
  }


  @SuppressWarnings("unchecked")
  @PostConstruct
  public void loadSubscriptions() {

    // load subscriptions
    ObjectMapper mapper = new ObjectMapper(new JsonFactory());
    mapper.registerModule(new JavaTimeModule());
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    CollectionType collectionType = mapper.getTypeFactory()
        .constructCollectionType(List.class, Subscription.class);
    List<Subscription> subscriptions = new ArrayList<>();
    List<RouteBuilder> subscriptionRoutes = new ArrayList<>();

    try {
      InputStream inputStream = (new ClassPathResource(config.getSubscriptionPath())
          .getInputStream());
      String subscriptionsString = IOUtils.toString(inputStream, "UTF-8");

      subscriptions = mapper.readValue(subscriptionsString, collectionType);
    } catch (IOException e) {
      e.printStackTrace();
    }

    subscriptions.forEach(s -> {
      if (s.getConsumerAddress() == null) {
        s.setConsumerAddress(config.getConsumerAddress());
      }
      service.addSubscription(s);
    });

    subscriptions.forEach(s -> subscriptionRoutes.add(new SubscriptionRoute(service, s)));

    for (RouteBuilder subscriptionRoute : subscriptionRoutes) {
      try {
        camelContext.addRoutes(subscriptionRoute);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }
}
