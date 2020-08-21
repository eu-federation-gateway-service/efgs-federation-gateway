/*-
 * ---license-start
 * EU-Federation-Gateway-Service / efgs-federation-gateway
 * ---
 * Copyright (C) 2020 T-Systems International GmbH and all other contributors
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

package eu.interop.federationgateway.service;

import eu.interop.federationgateway.entity.CertificateEntity;
import eu.interop.federationgateway.repository.CertificateRepository;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CertificateService {

  private final CertificateRepository certificateRepository;

  /**
   * Method to query the db for a certificate.
   *
   * @param thumbprint RSA-256 thumbprint of certificate.
   * @param country    country of certificate.
   * @param type       type of certificate.
   * @return Optional holding the certificate if found.
   */
  public Optional<CertificateEntity> getCertificate(
    String thumbprint, String country, CertificateEntity.CertificateType type) {
    return certificateRepository.getFirstByThumbprintAndCountryAndType(thumbprint, country, type);
  }

  /**
   * Queries the database for a callback certificate for host of given url.
   *
   * @param url     The url to search a certificate for.
   * @param country the country of the certificate.
   * @return Optional holding the certificate if found.
   */
  public Optional<CertificateEntity> getCallbackCertificateForUrl(String url, String country) {
    try {
      return getCallbackCertificateForHost(new URL(url).getHost(), country);
    } catch (MalformedURLException ignored) {
      log.error("Could not parse url.\", url=\"{}", url);
      return Optional.empty();
    }
  }

  /**
   * Queries the database for a callback certificate for given host.
   *
   * @param host    The host to search a certificate for.
   * @param country the country of the certificate.
   * @return Optional holding the certificate if found.
   */
  public Optional<CertificateEntity> getCallbackCertificateForHost(String host, String country) {
    return certificateRepository.getFirstByHostIsAndCountryIsAndTypeIs(
      host, country, CertificateEntity.CertificateType.CALLBACK);
  }
}
