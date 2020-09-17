package eu.interop.federationgateway.dbencryption;

import javax.persistence.AttributeConverter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DbEncryptionByteArrayConverter implements AttributeConverter<byte[], String> {

  private final DbEncryptionService dbEncryptionService;

  @Override
  public String convertToDatabaseColumn(byte[] s) {
    try {
      return dbEncryptionService.encryptByteArray(s);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public byte[] convertToEntityAttribute(String s) {
    try {
      return dbEncryptionService.decryptByteArray(s);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

}
