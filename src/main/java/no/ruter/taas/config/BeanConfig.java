package no.ruter.taas.config;

import org.apache.camel.util.jsse.KeyManagersParameters;
import org.apache.camel.util.jsse.KeyStoreParameters;
import org.apache.camel.util.jsse.SSLContextParameters;
import org.apache.camel.util.jsse.TrustManagersParameters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class BeanConfig {

  @Autowired
  private AppConfig config;

  @Bean
  public SSLContextParameters sslParam() {
    KeyStoreParameters keyStoreParameters = new KeyStoreParameters();
    keyStoreParameters.setResource(config.getKeyStorePath());
    keyStoreParameters.setPassword(config.getKeyStorePw());

    KeyManagersParameters keyManagersParameters = new KeyManagersParameters();
    keyManagersParameters.setKeyStore(keyStoreParameters);
    keyManagersParameters.setKeyPassword(config.getKeyStorePw());

    TrustManagersParameters trustManagersParameters = new TrustManagersParameters();
    trustManagersParameters.setKeyStore(keyStoreParameters);

    SSLContextParameters sslContextParameters = new SSLContextParameters();
    sslContextParameters.setKeyManagers(keyManagersParameters);
    sslContextParameters.setTrustManagers(trustManagersParameters);

    return sslContextParameters;
  }


}
