package eu.interop.federationgateway.utils;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.cert.X509CertificateHolder;

@Slf4j
public class CertificateUtils {

  /**
   * Calculates the SHA-256 thumbprint of X509Certificate.
   *
   * @param x509Certificate the certificate the thumbprint should be calculated for.
   * @return 32-byte SHA-256 hash as hex encoded string
   */
  public static String getCertThumbprint(X509Certificate x509Certificate) {
    try {
      return calculateHash(x509Certificate.getEncoded());
    } catch (Exception e) {
      log.error("Could not calculate thumbprint of certificate.");
      return null;
    }
  }

  /**
   * Calculates the SHA-256 thumbprint of X509CertificateHolder.
   *
   * @param x509CertificateHolder the certificate the thumbprint should be calculated for.
   * @return 32-byte SHA-256 hash as hex encoded string
   */
  public static String getCertThumbprint(X509CertificateHolder x509CertificateHolder) {
    try {
      return calculateHash(x509CertificateHolder.getEncoded());
    } catch (Exception e) {
      log.error("Could not calculate thumbprint of certificate.");
      return null;
    }
  }

  private static String calculateHash(byte[] data) throws NoSuchAlgorithmException {
    byte[] certHashBytes = MessageDigest.getInstance("SHA-256").digest(data);
    String hexString = new BigInteger(1, certHashBytes).toString(16);

    if (hexString.length() == 63) {
      hexString = "0" + hexString;
    }

    return hexString;
  }
}
