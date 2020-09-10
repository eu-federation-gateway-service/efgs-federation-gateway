package eu.interop.federationgateway.mtls;

import eu.interop.federationgateway.service.CertificateService;
import eu.interop.federationgateway.utils.CertificateUtils;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import javax.net.ssl.X509TrustManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EfgsCallbackTrustManager implements X509TrustManager {

  private final CertificateService certificateService;

  /**
   * Empty implementation because client certificate validating is not required for outgoing callbacks.
   */
  @Override
  public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
    throw new NotImplementedException();
  }

  /**
   * Certificate check whether any certificate of the given chain is within our authentication certificate whitelist.
   *
   * @param chain    the certificate chain
   * @param authType n/a
   * @throws CertificateException will be thrown if no matching certificate can be found.
   */
  @Override
  public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
    log.info("Checking incoming mTLS Server Certificate.");
    boolean certificateFound = Arrays.stream(chain)
      .map(CertificateUtils::getCertThumbprint)
      .anyMatch(thumbprint -> certificateService.getAuthenticationCertificate(thumbprint).isPresent());

    if (!certificateFound) {
      throw new CertificateException("Could not find mTLS server certificate in whitelist");
    }
  }

  /**
   * Empty implementation because client certificates are directly validated with their thumbprint.
   */
  @Override
  public X509Certificate[] getAcceptedIssuers() {
    return new X509Certificate[0];
  }
}
