package eu.interop.federationgateway.mtls;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.interop.federationgateway.entity.CertificateEntity;
import eu.interop.federationgateway.service.CertificateService;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Optional;
import org.apache.commons.lang3.NotImplementedException;
import org.junit.Before;
import org.junit.Test;

public class EfgsCallbackTrustManagerTest {

  private CertificateService certificateServiceMock;
  private EfgsCallbackTrustManager efgsCallbackTrustManager;

  @Before
  public void setUp() {
    certificateServiceMock = mock(CertificateService.class);

    efgsCallbackTrustManager = new EfgsCallbackTrustManager(certificateServiceMock);
  }

  @Test
  public void testTrustManagerShouldNotThrowAnException() throws CertificateException {
    X509Certificate certificateMock = mock(X509Certificate.class);

    when(certificateMock.getEncoded()).thenReturn(new byte[0]);

    X509Certificate[] certChain = {
      certificateMock,
      certificateMock,
      certificateMock
    };

    when(certificateServiceMock.getAuthenticationCertificate(anyString()))
      .thenReturn(Optional.empty())
      .thenReturn(Optional.of(new CertificateEntity()));

    efgsCallbackTrustManager.checkServerTrusted(certChain, "");

    verify(certificateServiceMock, times(2)).getAuthenticationCertificate(anyString());
  }

  @Test(expected = CertificateException.class)
  public void testTrustManagerShouldThrowExceptionIfCertIsNotFound() throws CertificateException {
    X509Certificate certificateMock = mock(X509Certificate.class);

    when(certificateMock.getEncoded()).thenReturn(new byte[0]);

    X509Certificate[] certChain = {
      certificateMock,
      certificateMock,
      certificateMock
    };

    when(certificateServiceMock.getAuthenticationCertificate(anyString()))
      .thenReturn(Optional.empty());

    efgsCallbackTrustManager.checkServerTrusted(certChain, "");

    verify(certificateServiceMock, times(3)).getAuthenticationCertificate(anyString());
  }

  @Test(expected = NotImplementedException.class)
  public void testTrustManagerThrowsNotImplementedWhenUsingClientVerifyMethod() throws CertificateException {
    efgsCallbackTrustManager.checkClientTrusted(new X509Certificate[0], "");
  }
}
