package eu.interop.federationgateway.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@Slf4j
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

  private void loadKeyStore(KeyStore keyStore, String path, char[] password)
    throws CertificateException, NoSuchAlgorithmException, IOException {

    InputStream fileStream = null;

    try {
      if (path.startsWith("classpath:")) {
        String resourcePath = path.substring(10);
        fileStream = getClass().getClassLoader().getResourceAsStream(resourcePath);
      } else {
        File file = new File(path);
        fileStream = file.exists() ? new FileInputStream(file) : null;
      }

      if (fileStream != null && fileStream.available() > 0) {
        keyStore.load(fileStream, password);
        fileStream.close();
      } else {
        keyStore.load(null);
        log.info("Could not find Keystore {}", path);
      }
    } catch (IOException e) {
      log.error("Could not find Keystore {}", path);
      throw e;
    } finally {
      fileStream.close();
    }
  }
}
