package eu.interop.federationgateway.testconfig;

import eu.interop.federationgateway.TestData;
import java.io.IOException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreSpi;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import lombok.RequiredArgsConstructor;
import org.bouncycastle.operator.OperatorCreationException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
@RequiredArgsConstructor
public class EfgsTestKeyStore {

  /**
   * Creates a KeyStore instance with keys for EFGS.
   *
   * @return KeyStore Instance
   */
  @Bean
  @Primary
  public KeyStore keyStore() throws IOException, CertificateException, NoSuchAlgorithmException,
    UnrecoverableKeyException, OperatorCreationException {
    KeyStoreSpi keyStoreSpiMock = mock(KeyStoreSpi.class);
    KeyStore keyStoreMock = new KeyStore(keyStoreSpiMock, null, "test") {
    };
    keyStoreMock.load(null);
    TestData.createTrustAnchorCertificate();
    when(keyStoreSpiMock.engineGetKey(any(), any())).thenReturn(mock(Key.class));
    when(keyStoreSpiMock.engineGetCertificate(anyString())).thenReturn(TestData.trustAnchor);
    return keyStoreMock;
  }
}
