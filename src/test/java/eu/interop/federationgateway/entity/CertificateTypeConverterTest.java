package eu.interop.federationgateway.entity;

import org.junit.jupiter.api.Test;

import static eu.interop.federationgateway.entity.CertificateEntity.CertificateType.*;
import static org.junit.jupiter.api.Assertions.*;

public class CertificateTypeConverterTest {

  private CertificateTypeConverter converter = new CertificateTypeConverter();

  @Test
  public void testConvertToDatabaseColumn() {
    assertNull(converter.convertToDatabaseColumn(null));
    assertEquals("AUTHENTICATION", converter.convertToDatabaseColumn(AUTHENTICATION));
    assertEquals("SIGNING", converter.convertToDatabaseColumn(SIGNING));
    assertEquals("CALLBACK", converter.convertToDatabaseColumn(CALLBACK));
  }

  @Test
  public void testConvertToEntityAttribute() {
    assertNull(converter.convertToEntityAttribute(null));
    assertEquals(AUTHENTICATION, converter.convertToEntityAttribute("AUTHENTICATION"));
    assertEquals(SIGNING, converter.convertToEntityAttribute("SIGNING"));
    assertEquals(CALLBACK, converter.convertToEntityAttribute("CALLBACK"));
    assertThrows(IllegalArgumentException.class, () ->
      converter.convertToEntityAttribute("X"));
  }

}
