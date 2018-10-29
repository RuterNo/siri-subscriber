package no.ruter.taas.service;

import io.fabric8.kubernetes.api.model.EndpointAddress;
import io.fabric8.kubernetes.api.model.EndpointSubset;
import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import no.ruter.taas.config.AppConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public final class KubernetesService {

  private final AppConfig config;
  private KubernetesClient kube;

  @Autowired
  public KubernetesService(AppConfig config) {
    this.config = config;
  }

  @PostConstruct
  public final void init() {

    if (!config.isKubernetesEnabled()) {
      log.warn("Kubernetes not enabled! Disabling connection!");
      return;
    }
    if (config.getKubernetesUrl() != null && !"".equals(config.getKubernetesUrl())) {
      log.info("Connecting to " + config.getKubernetesUrl());
      kube = new DefaultKubernetesClient(config.getKubernetesUrl());
    } else {
      log.info(
          "Using default settings, as this should auto-configure correctly in the kubernetes cluster");
      kube = new DefaultKubernetesClient();
    }
  }

  @PreDestroy
  public final void end() {
    if (kube != null) {
      kube.close();
    }
  }

  List<String> findEndpoints() {
    String serviceName = findDeploymentName();
    log.info("Shall find endpoints for " + serviceName);
    return findEndpoints(serviceName);
  }

  /**
   * @return Endpoints found for the given service name, both the ready and not ready endpoints
   */
  private List<String> findEndpoints(String serviceName) {
    if (kube == null) {
      return new ArrayList<>();
    }
    Endpoints eps = kube.endpoints().inNamespace(config.getKubernetesNamespace())
        .withName(serviceName).get();
    List<String> ready = addressesFrom(eps, EndpointSubset::getAddresses);
    List<String> notready = addressesFrom(eps, EndpointSubset::getNotReadyAddresses);
    log.info("Got " + ready.size() + " endpoints and " + notready.size() + " NOT ready endpoints");

    List<String> result = new ArrayList<>(ready);
    result.addAll(notready);
    log.info(
        "Ended up with the the following endpoints for endpoint " + serviceName + " : " + result);
    return result;
  }

  /**
   * When running on kubernetes, the deployment name is part of the hostname.
   */
  public String findDeploymentName() {
    String hostname = System.getenv("HOSTNAME");
    if (hostname == null) {
      hostname = "localhost";
    }
    int dash = hostname.indexOf("-");
    return dash == -1 ? hostname : hostname.substring(0, dash);
  }

  private List<String> addressesFrom(Endpoints endpoints,
      Function<EndpointSubset, List<EndpointAddress>> addressFunction) {
    if (endpoints == null || endpoints.getSubsets() == null) {
      return new ArrayList<>();
    }

    return endpoints.getSubsets()
        .stream()
        .map(addressFunction)
        .filter(Objects::nonNull)
        .flatMap(Collection::stream)
        .map(EndpointAddress::getIp)
        .collect(Collectors.toList());
  }


}
