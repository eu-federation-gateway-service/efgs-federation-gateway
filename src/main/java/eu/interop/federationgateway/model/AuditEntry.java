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

package eu.interop.federationgateway.model;

import eu.interop.federationgateway.entity.CertificateEntity;
import eu.interop.federationgateway.service.CertificateService;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.ZonedDateTime;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Schema(
  description = "Entity representation per country in audit results."
)
public class AuditEntry {

  private CertificateService certificateService;

  /**
   * The constructor of the AuditEntry.
   *
   * @param country                             the uploader country.
   * @param uploadedTime                        the created time
   * @param uploaderThumbprint                  the thumbprint of the authentication certificate
   * @param uploaderOperatorSignature           the operator signature of the authentication certificate
   * @param uploaderSigningThumbprint           the thumbprint of the signing certificate
   * @param signingCertificateOperatorSignature the operator signature of the signing certificate
   * @param amount                              the amount of the uploaded diagnosis
   * @param batchSignature                      the batch signature
   */
  public AuditEntry(String country, ZonedDateTime uploadedTime, String uploaderThumbprint,
                    String uploaderOperatorSignature, String uploaderSigningThumbprint,
                    String signingCertificateOperatorSignature, long amount, String batchSignature) {
    this.country = country;
    this.uploadedTime = uploadedTime;
    this.uploaderThumbprint = uploaderThumbprint;
    this.uploaderOperatorSignature = uploaderOperatorSignature;
    this.uploaderSigningThumbprint = uploaderSigningThumbprint;
    this.signingCertificateOperatorSignature = signingCertificateOperatorSignature;
    this.amount = amount;
    this.batchSignature = batchSignature;

    Optional<CertificateEntity> authenticationCertificate = certificateService.getCertificate(
      uploaderThumbprint,
      country,
      CertificateEntity.CertificateType.AUTHENTICATION);

    if (authenticationCertificate.isPresent()) {
      CertificateEntity certificateEntity = authenticationCertificate.get();
      this.uploaderOperatorSignature = certificateEntity.getSignature();
    }

    Optional<CertificateEntity> signingCertificate = certificateService.getCertificate(
      uploaderSigningThumbprint,
      country,
      CertificateEntity.CertificateType.SIGNING);

    if (signingCertificate.isPresent()) {
      CertificateEntity certificateEntity = signingCertificate.get();
      this.signingCertificateOperatorSignature = certificateEntity.getSignature();
    }

  }

  @Schema(example = "DE")
  private String country;

  @Schema(example = "2020-07-31T11:24:43.086Z")
  private ZonedDateTime uploadedTime;

  @Schema(example = "69c697c045b4cdaa441a28af0ec1cc4128153b9ddc796b66bfa04b02ea3e103e")
  private String uploaderThumbprint;

  private String uploaderOperatorSignature;

  @Schema(example = "69c697c045b4cdaa441a28af0ec1bb4128153b9ddc796b66bfa04b02ea3e103e")
  private String uploaderSigningThumbprint;

  private String signingCertificateOperatorSignature;

  @Schema(example = "3")
  private long amount;

  @Schema(example = "exampleBatchSignature")
  private String batchSignature;

}
