package eu.interop.federationgateway.dbencryption;

import eu.interop.federationgateway.entity.DiagnosisKeyPayload;
import javax.persistence.AttributeConverter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DbEncryptionReportTypeConverter implements AttributeConverter<DiagnosisKeyPayload.ReportType, String> {

  private final DbEncryptionService dbEncryptionService;

  @Override
  public String convertToDatabaseColumn(DiagnosisKeyPayload.ReportType s) {
    try {
      return dbEncryptionService.encryptString(s.name());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public DiagnosisKeyPayload.ReportType convertToEntityAttribute(String s) {
    try {
      return DiagnosisKeyPayload.ReportType.valueOf(dbEncryptionService.decryptString(s));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

}
