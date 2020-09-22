package eu.interop.federationgateway.dbencryption;

import javax.persistence.AttributeConverter;

public class DbEncryptionStringConverter implements AttributeConverter<String, String> {

  @Override
  public String convertToDatabaseColumn(String s) {
    try {
      return DbEncryptionService.getInstance().encryptString(s);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public String convertToEntityAttribute(String s) {
    try {
      return DbEncryptionService.getInstance().decryptString(s);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

}
