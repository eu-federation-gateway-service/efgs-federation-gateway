/*-
 * ---license-start
 * EU-Federation-Gateway-Service / efgs-federation-gateway
 * ---
 * Copyright (C) 2020 - 2021 T-Systems International GmbH and all other contributors
 * ---
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ---license-end
 */

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
   * @return the empty implementation.
   */
  @Override
  public X509Certificate[] getAcceptedIssuers() {
    return new X509Certificate[0];
  }
}
