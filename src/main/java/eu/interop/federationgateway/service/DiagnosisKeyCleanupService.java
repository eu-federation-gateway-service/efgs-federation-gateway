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
import eu.interop.federationgateway.utils.EfgsMdc;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class DiagnosisKeyCleanupService {

  private final DiagnosisKeyBatchService diagnosisKeyBatchService;

  private final DiagnosisKeyEntityService diagnosisKeyEntityService;

  private final EfgsProperties efgsProperties;

  /**
   * Cleanup task to delete all DiagnosisKeys and DiagnosisKeyBatches which are older then configured.
   */
  @Scheduled(cron = "0 0 0 * * *")
  @SchedulerLock(name = "DiagnosisKeyCleanupService_cleanupDiagnosisKeys", lockAtLeastFor = "PT0S",
    lockAtMostFor = "${efgs.download-settings.locklimit}")
  public void cleanupDiagnosisKeys() {
    ZonedDateTime deleteTimestamp = LocalDate.ofInstant(ZonedDateTime.now(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC)
      .atStartOfDay(ZoneOffset.UTC)
      .minusDays(efgsProperties.getDownloadSettings().getMaxAgeInDays());

    log.info("Starting DiagnosisKey and DiagnosisKeyBatch cleanup");

    final int deletedDiagnosisKeys = diagnosisKeyEntityService.deleteAllBefore(deleteTimestamp);
    final int deletedDiagnosisKeyBatches = diagnosisKeyBatchService.deleteAllBefore(deleteTimestamp);

    EfgsMdc.put("deletedDiagnosisKeys", deletedDiagnosisKeys);
    EfgsMdc.put("deletedDiagnosisKeyBatches", deletedDiagnosisKeyBatches);
    log.info("DiagnosisKey and DiagnosisKeyBatch cleanup finished.");
  }

}
