package eu.interop.federationgateway.service;

import eu.interop.federationgateway.config.EfgsProperties;
import eu.interop.federationgateway.utils.EfgsMDC;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
  public void cleanupDiagnosisKeys() {
    ZonedDateTime deleteTimestamp = LocalDate.now()
      .atStartOfDay(ZoneOffset.UTC)
      .minusDays(efgsProperties.getDownloadSettings().getMaxAgeInDays());

    log.info("Starting DiagnosisKey and DiagnosisKeyBatch cleanup");

    final int deletedDiagnosisKeys = diagnosisKeyEntityService.deleteAllBefore(deleteTimestamp);
    final int deletedDiagnosisKeyBatches = diagnosisKeyBatchService.deleteAllBefore(deleteTimestamp);

    EfgsMDC.put("deletedDiagnosisKeys", deletedDiagnosisKeys);
    EfgsMDC.put("deletedDiagnosisKeyBatches", deletedDiagnosisKeyBatches);
    log.info("DiagnosisKey and DiagnosisKeyBatch cleanup finished.");
  }

}
