package eu.interop.federationgateway.testconfig;

import eu.interop.federationgateway.TestData;
import eu.interop.federationgateway.config.EfgsProperties;
import eu.interop.federationgateway.service.CertificateService;
import java.io.IOException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.KeyStoreSpi;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import lombok.RequiredArgsConstructor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.util.ReflectionTestUtils;

@TestConfiguration
@RequiredArgsConstructor
public class EfgsTestKeyStore {

  private final EfgsProperties efgsProperties;

  /**
   * Creates a KeyStore instance with keys for EFGS.
   *
   * @return KeyStore Instance
   */
  @Bean
  public KeyStore keyStore() throws IOException, CertificateException, NoSuchAlgorithmException,
    UnrecoverableKeyException {
    KeyStoreSpi keyStoreSpiMock = mock(KeyStoreSpi.class);
    KeyStore keyStoreMock = new KeyStore(keyStoreSpiMock, null, "test") {
    };
    keyStoreMock.load(null);
    when(keyStoreSpiMock.engineGetKey(any(), any())).thenReturn(mock(Key.class));
    return keyStoreMock;
  }
}
