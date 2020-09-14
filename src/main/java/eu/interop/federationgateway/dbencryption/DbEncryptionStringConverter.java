package eu.interop.federationgateway.dbencryption;

import javax.persistence.AttributeConverter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DbEncryptionStringConverter implements AttributeConverter<String, String> {

  private final DbEncryptionService dbEncryptionService;

  @Override
  public String convertToDatabaseColumn(String s) {
    try {
      return dbEncryptionService.encryptString(s);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public String convertToEntityAttribute(String s) {
    try {
      return dbEncryptionService.decryptString(s);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

}
