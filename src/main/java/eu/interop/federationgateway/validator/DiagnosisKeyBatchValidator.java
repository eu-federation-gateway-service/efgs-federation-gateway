package eu.interop.federationgateway.validator;

import eu.interop.federationgateway.model.EfgsProto;
import java.util.List;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DiagnosisKeyBatchValidator implements
  ConstraintValidator<DiagnosisKeyBatchConstraint, EfgsProto.DiagnosisKeyBatch> {

  private static final String VALIDATION_FAILED_MESSAGE = "Validation of Diagnosis key failed: ";

  @Override
  public boolean isValid(EfgsProto.DiagnosisKeyBatch diagnosisKeyBatch, ConstraintValidatorContext context) {

    List<EfgsProto.DiagnosisKey> diagnosisKeys = diagnosisKeyBatch.getKeysList();
    for (EfgsProto.DiagnosisKey diagnosisKey : diagnosisKeys) {
      if (diagnosisKey.getReportType() == EfgsProto.ReportType.UNKNOWN) {
        log.error(VALIDATION_FAILED_MESSAGE + "Invalid Report-Type.");
        return false;
      } else if (diagnosisKey.getRollingStartIntervalNumber() == 0) {
        log.error(VALIDATION_FAILED_MESSAGE + "Invalid rolling start interval number.");
        return false;
      } else if (diagnosisKey.getKeyData() == null || diagnosisKey.getKeyData().isEmpty()) {
        log.error(VALIDATION_FAILED_MESSAGE + "The keydata is empty or null.");
        return false;
      } else if (diagnosisKey.getRollingPeriod() == 0) {
        log.error(VALIDATION_FAILED_MESSAGE + "Invalid rolling period.");
        return false;
      } else if (diagnosisKey.getTransmissionRiskLevel() == 0 || diagnosisKey.getTransmissionRiskLevel() > 8) {
        log.error(VALIDATION_FAILED_MESSAGE + "Invalid transmission risk level.");
        return false;
      }
    }

    log.info("Successfull validation of diagnosis keys");
    return true;
  }
}
