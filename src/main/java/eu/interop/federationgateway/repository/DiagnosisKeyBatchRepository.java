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

package eu.interop.federationgateway.repository;

import eu.interop.federationgateway.entity.DiagnosisKeyBatchEntity;
import java.time.ZonedDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Transactional(isolation = Isolation.REPEATABLE_READ)
public interface DiagnosisKeyBatchRepository extends JpaRepository<DiagnosisKeyBatchEntity, Long> {

  @Modifying
  @Query("DELETE FROM DiagnosisKeyBatchEntity d WHERE d.createdAt < :before")
  int deleteByCreatedAtBefore(@Param("before") ZonedDateTime before);

  Optional<DiagnosisKeyBatchEntity> findByBatchName(String name);

  Optional<DiagnosisKeyBatchEntity> findByBatchLink(String name);

  Optional<DiagnosisKeyBatchEntity> findTopByOrderByCreatedAtDesc();

  Optional<DiagnosisKeyBatchEntity> findFirstByCreatedAtIsBetweenOrderByCreatedAtAsc(
    ZonedDateTime begin, ZonedDateTime end);
}
