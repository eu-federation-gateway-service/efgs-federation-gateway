package eu.interop.federationgateway.validator;

import eu.interop.federationgateway.model.EfgsProto;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DiagnosisKeyBatchValidator implements
  ConstraintValidator<DiagnosisKeyBatchConstraint, EfgsProto.DiagnosisKeyBatch> {

  private static final String VALIDATION_FAILED_MESSAGE = "Validation of diagnosis key failed: ";
  private static final int ROLLING_START_INTERVAL_LENGTH = 600;
  private static final int TRL_DEFAULT_VALUE = 0x7fffffff; // this value will be used if a correct TransmissionRiskLevel cannot be provided

  @Override
  public boolean isValid(EfgsProto.DiagnosisKeyBatch diagnosisKeyBatch, ConstraintValidatorContext context) {

    /*
     see
       - https://github.com/google/exposure-notifications-server/blob/5a77e6982b16c7282c77dea39772491cc7b7dc8b/internal/publish/model/exposure_model.go#L338
       - https://github.com/google/exposure-notifications-server/blob/5a77e6982b16c7282c77dea39772491cc7b7dc8b/tools/export-analyzer/main.go#L123
     */

    long minimumRollingStart = Instant
      .now()
      .truncatedTo(ChronoUnit.DAYS)
      .minus(15, ChronoUnit.DAYS)
      .getEpochSecond() / ROLLING_START_INTERVAL_LENGTH;

    long maximumRollingStart = Instant
      .now()
      .getEpochSecond() / ROLLING_START_INTERVAL_LENGTH;
    maximumRollingStart += 1;

    List<EfgsProto.DiagnosisKey> diagnosisKeys = diagnosisKeyBatch.getKeysList();
    for (EfgsProto.DiagnosisKey diagnosisKey : diagnosisKeys) {
      if (diagnosisKey.getKeyData() == null || diagnosisKey.getKeyData().isEmpty()) {
        return fail("The keydata is empty or null.", context);

      } else if (diagnosisKey.getKeyData().size() != 16) {
        return fail("The keydata is not 16 bytes.", context);

      } else if (diagnosisKey.getRollingStartIntervalNumber() < minimumRollingStart
        || diagnosisKey.getRollingStartIntervalNumber() > maximumRollingStart) {
        return fail("Invalid rolling start interval number.", context);
      } else if (diagnosisKey.getRollingPeriod() < 1 || diagnosisKey.getRollingPeriod() > 144) {
        return fail("Invalid rolling period.", context);

      } else if ((diagnosisKey.getTransmissionRiskLevel() < 0 || diagnosisKey.getTransmissionRiskLevel() > 8)
        && diagnosisKey.getTransmissionRiskLevel() != TRL_DEFAULT_VALUE) {
        return fail("Invalid transmission risk level.", context);

      } /*
        This checks needs further investigation because of multiple
        usage of DSOS during the exchange and epidemiological harmonization.

        else if (diagnosisKey.getDaysSinceOnsetOfSymptoms() < -14 || diagnosisKey.getDaysSinceOnsetOfSymptoms() > 14) {
        return fail("Invalid days since onset of symptoms.", context);

      }*/
    }

    log.info("Successful validation of diagnosis keys");
    return true;
  }

  private boolean fail(String reason, ConstraintValidatorContext context) {
    context.buildConstraintViolationWithTemplate(VALIDATION_FAILED_MESSAGE + reason)
      .addConstraintViolation();

    return false;
  }
}
