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
   * Creates a KeyStore instance with keys for EFGS TrustAnchor.
   *
   * @return KeyStore Instance
   */
  @Bean
  public KeyStore trustAnchorKeyStore() throws KeyStoreException, IOException,
    CertificateException, NoSuchAlgorithmException {
    KeyStore keyStore = KeyStore.getInstance("JKS");

    loadKeyStore(
      keyStore,
      efgsProperties.getTrustAnchor().getKeyStorePath(),
      efgsProperties.getTrustAnchor().getKeyStorePass().toCharArray());

    return keyStore;
  }

  /**
   * Creates a KeyStore instance with keys for EFGS Callback feature.
   *
   * @return KeyStore Instance
   */
  @Bean
  public KeyStore callbackKeyStore() throws KeyStoreException, IOException,
    CertificateException, NoSuchAlgorithmException {
    KeyStore keyStore = KeyStore.getInstance("JKS");

    loadKeyStore(
      keyStore,
      efgsProperties.getCallback().getKeyStorePath(),
      efgsProperties.getCallback().getKeyStorePass().toCharArray());

    return keyStore;
  }

  private void loadKeyStore(KeyStore keyStore, String path, char[] password) throws CertificateException, NoSuchAlgorithmException, IOException {
    if (path.startsWith("classpath:")) {
      String resourcePath = path.substring(10);
      keyStore.load(getClass().getClassLoader().getResourceAsStream(resourcePath), password);
    } else {
      keyStore.load(new FileInputStream(path), password);
    }
  }
}
