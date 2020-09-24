package eu.interop.federationgateway.dbencryption;

import eu.interop.federationgateway.entity.DiagnosisKeyPayload;
import javax.persistence.AttributeConverter;
import javax.persistence.PersistenceException;

public class DbEncryptionReportTypeConverter implements AttributeConverter<DiagnosisKeyPayload.ReportType, String> {

  @Override
  public String convertToDatabaseColumn(DiagnosisKeyPayload.ReportType s) {
    try {
      return DbEncryptionService.getInstance().encryptString(s.name());
    } catch (Exception e) {
      throw new PersistenceException(e);
    }
  }

  @Override
  public DiagnosisKeyPayload.ReportType convertToEntityAttribute(String s) {
    try {
      return DiagnosisKeyPayload.ReportType.valueOf(DbEncryptionService.getInstance().decryptString(s));
    } catch (Exception e) {
      throw new PersistenceException(e);
    }
  }

}
