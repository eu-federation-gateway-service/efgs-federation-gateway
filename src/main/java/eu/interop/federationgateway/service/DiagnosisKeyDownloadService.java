/*-
 * ---license-start
 * EU-Federation-Gateway-Service / efgs-federation-gateway
 * ---
 * Copyright (C) 2020 - 2022 T-Systems International GmbH and all other contributors
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

import eu.interop.federationgateway.entity.DiagnosisKeyDownloadEntity;
import eu.interop.federationgateway.repository.DiagnosisKeyDownloadRepository;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class DiagnosisKeyDownloadService {

  private final DiagnosisKeyDownloadRepository diagnosisKeyDownloadRepository;

  /**
   * Save a diagnosis kew download entry in the db.
   * @param batchId The batch id.
   * @param country The country.
   * @return The saved entry.
   */
  public DiagnosisKeyDownloadEntity save(Long batchId, String country) {
    DiagnosisKeyDownloadEntity diagnosisKeyDownloadEntity = new DiagnosisKeyDownloadEntity();
    diagnosisKeyDownloadEntity.setDownloadBatchId(batchId);
    diagnosisKeyDownloadEntity.setCountry(country);
    diagnosisKeyDownloadEntity.setRequestedAt(ZonedDateTime.now(ZoneOffset.UTC));

    return diagnosisKeyDownloadRepository.save(diagnosisKeyDownloadEntity);
  }
}
