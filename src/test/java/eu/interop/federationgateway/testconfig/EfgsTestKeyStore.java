package eu.interop.federationgateway.testconfig;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import eu.interop.federationgateway.TestData;
import eu.interop.federationgateway.config.EfgsProperties;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreSpi;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import lombok.RequiredArgsConstructor;
import org.bouncycastle.operator.OperatorCreationException;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

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
  @Primary
  public KeyStore testKeyStore() throws IOException, CertificateException, NoSuchAlgorithmException, UnrecoverableKeyException, OperatorCreationException {
    TestData.createCertificates();

    KeyStoreSpi keyStoreSpiMock = mock(KeyStoreSpi.class);
    KeyStore keyStoreMock = new KeyStore(keyStoreSpiMock, null, "test") {
    };
    keyStoreMock.load(null);

    doAnswer((x) -> TestData.keyPair.getPrivate()).when(keyStoreSpiMock).engineGetKey(anyString(), any());

    doAnswer((x) -> TestData.trustAnchor)
      .when(keyStoreSpiMock).engineGetCertificate(eq(efgsProperties.getTrustAnchor().getCertificateAlias()));

    doAnswer((x) -> TestData.validAuthenticationCertificate)
      .when(keyStoreSpiMock).engineGetCertificate(eq(efgsProperties.getCallback().getKeyStoreCertificateAlias()));

    return keyStoreMock;
  }

}
