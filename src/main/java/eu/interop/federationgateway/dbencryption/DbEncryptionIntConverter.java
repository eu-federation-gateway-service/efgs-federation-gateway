package eu.interop.federationgateway.dbencryption;

import javax.persistence.AttributeConverter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DbEncryptionIntConverter implements AttributeConverter<Integer, String> {

  private final DbEncryptionService dbEncryptionService;

  @Override
  public String convertToDatabaseColumn(Integer s) {
    try {
      return dbEncryptionService.encryptInteger(s);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Integer convertToEntityAttribute(String s) {
    try {
      return dbEncryptionService.decryptInteger(s);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

}
