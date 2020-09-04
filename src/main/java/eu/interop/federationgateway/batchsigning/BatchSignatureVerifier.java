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

package eu.interop.federationgateway.batchsigning;

import eu.interop.federationgateway.entity.CertificateEntity;
import eu.interop.federationgateway.model.EfgsProto.DiagnosisKeyBatch;
import eu.interop.federationgateway.service.CertificateService;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Date;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.SignerId;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.SignerInformationStore;
import org.bouncycastle.cms.SignerInformationVerifier;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.util.Store;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

/**
 * This class contains the methods to verify a batch signature.
 */
@Slf4j
@Service
public class BatchSignatureVerifier {

  private final CertificateService certificateService;

  public BatchSignatureVerifier(CertificateService certificateService) {
    this.certificateService = certificateService;
  }

  /**
   * Verifies the signature of a batch. The signature is an PKCS#7 object encoded with base64.
   *
   * @param batch                the {@link DiagnosisKeyBatch} object that corresponds to the batch signature.
   * @param base64BatchSignature the base64-encoded batch signature to be verified.
   * @return true if the batch signature is correct. False otherwise.
   */
  public boolean verify(final DiagnosisKeyBatch batch, final String base64BatchSignature) {
    final byte[] batchSignatureBytes = BatchSignatureUtils.b64ToBytes(base64BatchSignature);
    if (batchSignatureBytes != null) {
      try {
        final CMSSignedData signedData = new CMSSignedData(getBatchBytes(batch), batchSignatureBytes);
        final SignerInformation signerInfo = getSignerInformation(signedData);

        if (signerInfo == null) {
          log.error("no signer information");
          return false;
        }

        final X509CertificateHolder signerCert = getSignerCert(signedData.getCertificates(), signerInfo.getSID());

        if (signerCert == null) {
          log.error("no signer certificate");
          return false;
        }

        MDC.put("certNotBefore", signerCert.getNotBefore().toString());
        MDC.put("certNotAfter", signerCert.getNotAfter().toString());

        if (!isCertNotExpired(signerCert)) {
          log.error("signing certificate expired");
          return false;
        }

        if (!isCertValid(signerCert)) {
          log.error("invalid signing certificate signature");
          return false;
        }

        if (!allOriginsMatchingCertCountry(batch, signerCert)) {
          log.error("different origins");
          return false;
        }

        return verifySignerInfo(signerInfo, signerCert);
      } catch (CertificateException | OperatorCreationException | CMSException e) {
        log.error("error verifying batch signature", e);
      }
    }
    return false;
  }

  private boolean allOriginsMatchingCertCountry(DiagnosisKeyBatch batch, X509CertificateHolder certificate) {
    String country = getCountryOfCertificate(certificate);

    if (country == null) {
      return false;
    } else {
      return batch.getKeysList().stream()
        .allMatch(key -> key.getOrigin().equals(country));
    }
  }

  private boolean isCertNotExpired(X509CertificateHolder certificate) {
    Date now = new Date();

    return certificate.getNotBefore().before(now)
      && certificate.getNotAfter().after(now);
  }

  private boolean isCertValid(X509CertificateHolder certificate) {
    try {
      byte[] certHashBytes = MessageDigest.getInstance("SHA-256").digest(certificate.getEncoded());
      String certHash = new BigInteger(1, certHashBytes).toString(16);

      Optional<CertificateEntity> certificateEntity = certificateService.getCertificate(
        certHash,
        getCountryOfCertificate(certificate),
        CertificateEntity.CertificateType.SIGNING);

      MDC.put("certThumbprint", certHash);

      if (certificateEntity.isEmpty()) {
        log.error("unknown signing certificate");
        return false;
      }

      if (certificateEntity.get().getRevoked()) {
        log.error("certificate is revoked");
        return false;
      }
      return true;

    } catch (IOException e) {
      log.error("Failed to hash certificate thumbprint");
      return false;

    } catch (NoSuchAlgorithmException e) {
      log.error("Could not hash certificate thumbprint with SHA-256");
      return false;
    }
  }

  private Signature createSignatureInstance(final X509Certificate x509Certificate)
    throws NoSuchAlgorithmException {
    return Signature.getInstance(x509Certificate.getSigAlgName());
  }

  private X509Certificate toX509Certificate(final X509CertificateHolder certificate) throws CertificateException {
    final JcaX509CertificateConverter converter = new JcaX509CertificateConverter();
    return converter.getCertificate(certificate);
  }

  private String getCountryOfCertificate(X509CertificateHolder certificate) {
    RDN[] rdns = certificate.getSubject().getRDNs(BCStyle.C);
    if (rdns.length != 1) {
      log.info("Certificate has no valid country attribute");
      return null;
    } else {
      return rdns[0].getFirst().getValue().toString();
    }
  }

  private CMSProcessableByteArray getBatchBytes(DiagnosisKeyBatch batch) {
    return new CMSProcessableByteArray(BatchSignatureUtils.generateBytesToVerify(batch));
  }

  private SignerInformation getSignerInformation(final CMSSignedData signedData) {
    final SignerInformationStore signerInfoStore = signedData.getSignerInfos();

    if (signerInfoStore.size() > 0) {
      return signerInfoStore.getSigners().iterator().next();
    }
    return null;
  }

  private X509CertificateHolder getSignerCert(final Store<X509CertificateHolder> certificatesStore,
                                              final SignerId signerId) {
    final Collection certCollection = certificatesStore.getMatches(signerId);

    if (!certCollection.isEmpty()) {
      return (X509CertificateHolder) certCollection.iterator().next();
    }
    return null;
  }

  private boolean verifySignerInfo(final SignerInformation signerInfo, final X509CertificateHolder signerCert)
    throws CertificateException, OperatorCreationException, CMSException {
    return signerInfo.verify(createSignerInfoVerifier(signerCert));
  }

  private SignerInformationVerifier createSignerInfoVerifier(final X509CertificateHolder signerCert)
    throws OperatorCreationException, CertificateException {
    return new JcaSimpleSignerInfoVerifierBuilder().build(signerCert);
  }

}
