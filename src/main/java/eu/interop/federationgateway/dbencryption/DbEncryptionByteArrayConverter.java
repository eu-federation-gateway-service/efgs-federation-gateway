package eu.interop.federationgateway.dbencryption;

import javax.persistence.AttributeConverter;

public class DbEncryptionByteArrayConverter implements AttributeConverter<byte[], String> {

  @Override
  public String convertToDatabaseColumn(byte[] s) {
    try {
      return DbEncryptionService.getInstance().encryptByteArray(s);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public byte[] convertToEntityAttribute(String s) {
    try {
      return DbEncryptionService.getInstance().decryptByteArray(s);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

}
