package eu.interop.federationgateway.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class EfgsKeyStore {

  private final EfgsProperties efgsProperties;

  /**
   * Creates a KeyStore instance with keys for EFGS.
   *
   * @return KeyStore Instance
   */
  @Bean
  public KeyStore keyStore() throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {
    KeyStore keyStore = KeyStore.getInstance("JKS");
    keyStore.load(
      new FileInputStream(efgsProperties.getKeyStorePath()),
      efgsProperties.getKeyStorePass().toCharArray()
    );
    return keyStore;
  }
}
