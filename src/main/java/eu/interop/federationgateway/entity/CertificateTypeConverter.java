package eu.interop.federationgateway.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.stream.Stream;

/**
 * A converter for achieving O/R mapping of the certificate.type column as enum.
 *
 * <p><b>Note:</b> the need of having an explicit converter (instead of an @Enumerated annotation)
 * occurred after the update of the libraries to spring cloud 2023.0.0.
 * After this update, the @Enumerated annotation seems to be ignored.
 */
@Converter(autoApply = true)
public class CertificateTypeConverter implements AttributeConverter<CertificateEntity.CertificateType, String> {

  @Override
  public String convertToDatabaseColumn(CertificateEntity.CertificateType certificateType) {
    if (certificateType == null) {
      return null;
    }
    return certificateType.toString();
  }

  @Override
  public CertificateEntity.CertificateType convertToEntityAttribute(String s) {
    if (s == null) {
      return null;
    }
    return Stream.of(CertificateEntity.CertificateType.values())
      .filter(t -> t.toString().equals(s))
      .findFirst()
      .orElseThrow(IllegalArgumentException::new);
  }
}
