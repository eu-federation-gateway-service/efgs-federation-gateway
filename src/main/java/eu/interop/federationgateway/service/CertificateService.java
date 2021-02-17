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

package eu.interop.federationgateway.service;

import eu.interop.federationgateway.config.EfgsProperties;
import eu.interop.federationgateway.entity.CertificateEntity;
import eu.interop.federationgateway.model.AuditEntry;
import eu.interop.federationgateway.repository.CertificateRepository;
import eu.interop.federationgateway.utils.CertificateUtils;
import eu.interop.federationgateway.utils.EfgsMdc;
import java.io.IOException;
import java.io.StringReader;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.openssl.PEMParser;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CertificateService {

  private static final String MDC_PROP_CERT_THUMBPRINT = "certVerifyThumbprint";
  private final CertificateRepository certificateRepository;
  private final KeyStore trustAnchorKeyStore;
  private final EfgsProperties efgsProperties;

  /**
   * Add operator signatures to the audit entries.
   *
   * @param auditEntries list of audit entries
   * @return list of audit entries with operatorSignatures and certificate raw data
   */
  public List<AuditEntry> addOperatorSignatures(List<AuditEntry> auditEntries) {
    for (AuditEntry auditEntry : auditEntries) {
      String uploaderThumbprint = auditEntry.getUploaderThumbprint();
      String uploaderSigningThumbprint = auditEntry.getUploaderSigningThumbprint();
      String country = auditEntry.getCountry();
      CertificateEntity certificateEntity;

      Optional<CertificateEntity> authenticationCertificate = getCertificate(
        uploaderThumbprint,
        country,
        CertificateEntity.CertificateType.AUTHENTICATION);

      if (authenticationCertificate.isPresent()) {
        certificateEntity = authenticationCertificate.get();
        auditEntry.setUploaderCertificate(certificateEntity.getRawData());
        auditEntry.setUploaderOperatorSignature(certificateEntity.getSignature());
      }

      Optional<CertificateEntity> signingCertificate = getCertificate(
        uploaderSigningThumbprint,
        country,
        CertificateEntity.CertificateType.SIGNING);

      if (signingCertificate.isPresent()) {
        certificateEntity = signingCertificate.get();
        auditEntry.setSigningCertificate(certificateEntity.getRawData());
        auditEntry.setSigningCertificateOperatorSignature(certificateEntity.getSignature());
      }
    }
    return auditEntries;
  }

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

    return certificateRepository.getFirstByThumbprintAndCountryAndType(thumbprint, country, type)
      .map(certificateEntity -> validateCertificateIntegrity(certificateEntity) ? certificateEntity : null);
  }

  /**
   * Method to query the db for a authentication certificate.
   *
   * @param thumbprint RSA-256 thumbprint of certificate.
   * @return Optional holding the certificate if found.
   */
  public Optional<CertificateEntity> getAuthenticationCertificate(String thumbprint) {

    return certificateRepository.getFirstByThumbprintAndType(
      thumbprint, CertificateEntity.CertificateType.AUTHENTICATION)
      .map(certificateEntity -> validateCertificateIntegrity(certificateEntity) ? certificateEntity : null);
  }

  private boolean validateCertificateIntegrity(CertificateEntity certificateEntity) {

    EfgsMdc.put(MDC_PROP_CERT_THUMBPRINT, certificateEntity.getThumbprint());

    // check if entity has signature and certificate information
    if (certificateEntity.getSignature() == null || certificateEntity.getSignature().isEmpty()
      || certificateEntity.getRawData() == null || certificateEntity.getRawData().isEmpty()) {
      log.error("Certificate entity does not contain raw certificate or certificate signature.");
      return false;
    }

    // check if raw data contains a x509 certificate
    X509Certificate x509Certificate = getX509CertificateFromEntity(certificateEntity);
    if (x509Certificate == null) {
      log.error("Raw certificate data does not contain a valid x509Certificate.");
      return false;
    }

    // verify if thumbprint in database matches the certificate in raw data
    if (!verifyThumbprintMatchesCertificate(certificateEntity, x509Certificate)) {
      log.error("Thumbprint in database does not match thumbprint of stored certificate.");
      return false;
    }

    // load EFGS Trust Anchor PublicKey from KeyStore
    X509Certificate trustAnchor = null;
    try {
      trustAnchor = (X509Certificate) trustAnchorKeyStore.getCertificate(
        efgsProperties.getTrustAnchor().getCertificateAlias());
    } catch (KeyStoreException e) {
      log.error("Could not load EFGS-TrustAnchor from KeyStore.");
      return false;
    }

    // verify certificate signature
    try {
      Signature verifier = Signature.getInstance(trustAnchor.getSigAlgName());
      byte[] signatureBytes = Base64.getDecoder().decode(certificateEntity.getSignature());

      verifier.initVerify(trustAnchor);
      verifier.update(certificateEntity.getRawData().getBytes());

      if (verifier.verify(signatureBytes)) {
        EfgsMdc.remove(MDC_PROP_CERT_THUMBPRINT);
        return true;
      } else {
        log.error("Verification of certificate signature failed!");
        EfgsMdc.remove(MDC_PROP_CERT_THUMBPRINT);
        return false;
      }
    } catch (InvalidKeyException e) {
      log.error("Could not use public key to initialize verifier.");
      return false;
    } catch (SignatureException e) {
      log.error("Signature verifier is not initialized");
      return false;
    } catch (NoSuchAlgorithmException e) {
      log.error("Unknown signing algorithm used by EFGS Trust Anchor.");
      return false;
    }
  }

  private boolean verifyThumbprintMatchesCertificate(CertificateEntity certificateEntity, X509Certificate certificate) {
    String certHash = CertificateUtils.getCertThumbprint(certificate);

    return certHash != null && certHash.equals(certificateEntity.getThumbprint());
  }

  private X509Certificate getX509CertificateFromEntity(CertificateEntity certificateEntity) {
    PEMParser pemParser = new PEMParser(new StringReader(certificateEntity.getRawData()));

    try {
      while (pemParser.ready()) {
        Object certificateContent = pemParser.readObject();

        if (certificateContent == null) {
          return null;
        }
        if (certificateContent instanceof X509Certificate) {
          return (X509Certificate) certificateContent;
        } else if (certificateContent instanceof X509CertificateHolder) {
          JcaX509CertificateConverter converter = new JcaX509CertificateConverter();
          return converter.getCertificate((X509CertificateHolder) certificateContent);
        }
      }
      return null;
    } catch (IOException | CertificateException ignored) {
      return null;
    }
  }
}
