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

import eu.interop.federationgateway.entity.DiagnosisKeyUploadBatchEntity;
import eu.interop.federationgateway.repository.DiagnosisKeyUploadRepository;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class DiagnosisKeyUploadService {

  private final DiagnosisKeyUploadRepository diagnosisKeyUploadRepository;

  /**
   * Save a diagnosis kew upload batch entry in the db.
   * @param batchName The batch name.
   * @param numberOfKeys The number of keys.
   * @param country The country.
   * @return The saved entry.
   */
  public DiagnosisKeyUploadBatchEntity save(String batchName,int numberOfKeys,String country) {
    DiagnosisKeyUploadBatchEntity diagnosisKeyUploadBatchEntity = new DiagnosisKeyUploadBatchEntity();
    diagnosisKeyUploadBatchEntity.setCountry(country);
    diagnosisKeyUploadBatchEntity.setBatchName(batchName);
    diagnosisKeyUploadBatchEntity.setNumberOfKeys(numberOfKeys);
    diagnosisKeyUploadBatchEntity.setCreatedAt(ZonedDateTime.now(ZoneOffset.UTC));
    return diagnosisKeyUploadRepository.save(diagnosisKeyUploadBatchEntity);
  }
}
