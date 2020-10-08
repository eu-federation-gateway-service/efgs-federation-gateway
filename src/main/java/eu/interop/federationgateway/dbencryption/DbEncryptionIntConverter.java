package eu.interop.federationgateway.dbencryption;

import javax.persistence.AttributeConverter;
import javax.persistence.PersistenceException;

public class DbEncryptionIntConverter implements AttributeConverter<Integer, String> {

  @Override
  public String convertToDatabaseColumn(Integer s) {
    try {
      return DbEncryptionService.getInstance().encryptInteger(s);
    } catch (Exception e) {
      throw new PersistenceException(e);
    }
  }

  @Override
  public Integer convertToEntityAttribute(String s) {
    try {
      return DbEncryptionService.getInstance().decryptInteger(s);
    } catch (Exception e) {
      throw new PersistenceException(e);
    }
  }

}
