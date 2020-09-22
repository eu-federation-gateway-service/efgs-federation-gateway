package eu.interop.federationgateway.dbencryption;

import javax.persistence.AttributeConverter;

public class DbEncryptionIntConverter implements AttributeConverter<Integer, String> {

  @Override
  public String convertToDatabaseColumn(Integer s) {
    try {
      return DbEncryptionService.getInstance().encryptInteger(s);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Integer convertToEntityAttribute(String s) {
    try {
      return DbEncryptionService.getInstance().decryptInteger(s);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

}
