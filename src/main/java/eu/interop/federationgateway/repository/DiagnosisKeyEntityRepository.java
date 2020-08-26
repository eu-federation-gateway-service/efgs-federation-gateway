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

package eu.interop.federationgateway.repository;

import eu.interop.federationgateway.entity.DiagnosisKeyEntity;
import eu.interop.federationgateway.model.AuditEntry;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Transactional(isolation = Isolation.SERIALIZABLE)
public interface DiagnosisKeyEntityRepository extends JpaRepository<DiagnosisKeyEntity, Long> {

  @Modifying
  int deleteByCreatedAtBefore(ZonedDateTime before);

  List<DiagnosisKeyEntity> findByPayloadVisitedCountriesIsContaining(String country);

  List<DiagnosisKeyEntity> findAllByPayloadOrigin(String country);

  int countAllByUploader_BatchTag(String batchTag);

  List<DiagnosisKeyEntity> findByBatchTagIsNullAndUploader_BatchTag(String batchTag);

  @Query("SELECT new eu.interop.federationgateway.model.AuditEntry("
    + "min(uploader.country), min(createdAt), min(uploader.thumbprint), COUNT(*), min(uploader.batchSignature))"
    + "FROM DiagnosisKeyEntity WHERE batchTag = :batchTag GROUP BY uploader.batchTag")
  List<AuditEntry> findAllByBatchTag(@Param("batchTag") String batchTag);

  Optional<DiagnosisKeyEntity> findFirstByBatchTagIsNull();

  List<DiagnosisKeyEntity> findByBatchTagIsAndUploader_CountryIsNot(String batchTag, String country);

}
