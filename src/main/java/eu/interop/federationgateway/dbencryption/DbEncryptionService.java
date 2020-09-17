package eu.interop.federationgateway.dbencryption;

import eu.interop.federationgateway.config.EfgsProperties;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.util.Base64;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.security.crypto.encrypt.AesBytesEncryptor;
import org.springframework.stereotype.Service;

@Service
public class DbEncryptionService {

  private final EfgsProperties efgsProperties;

  private final Cipher cipher;

  private final Key key;

  private final Charset charset = StandardCharsets.UTF_8;

  /**
   * Constructor for DbEncryptionService.
   * Initializes Cipher with ciphersuite configured in application properties.
   */
  public DbEncryptionService(EfgsProperties efgsProperties) {
    this.efgsProperties = efgsProperties;

    cipher = AesBytesEncryptor.CipherAlgorithm.CBC.createCipher();
    key = new SecretKeySpec(efgsProperties.getDbencryption().getPassword().getBytes(), "AES");
  }

  /**
   * Decrypts a given AES-256 encrypted and base64 encoded String.
   *
   * @param encrypted the encrypted string
   * @return decrypted string
   */
  public String decryptString(String encrypted)
    throws InvalidKeyException, BadPaddingException, IllegalBlockSizeException, InvalidAlgorithmParameterException {
    return new String(decrypt(Base64.getDecoder().decode(encrypted)), charset);
  }

  /**
   * Encrypts and base 64 encodes a String.
   *
   * @param plain the plain string
   * @return encrypted string
   */
  public String encryptString(String plain)
    throws InvalidKeyException, BadPaddingException, IllegalBlockSizeException, InvalidAlgorithmParameterException {
    return Base64.getEncoder().encodeToString(encrypt(plain.getBytes(charset)));
  }

  /**
   * Decrypts a given AES-256 encrypted and base64 encoded String.
   *
   * @param encrypted the encrypted string
   * @return decrypted integer
   */
  public Integer decryptInteger(String encrypted)
    throws InvalidKeyException, BadPaddingException, IllegalBlockSizeException, InvalidAlgorithmParameterException {
    return Integer.valueOf(decryptString(encrypted));
  }

  /**
   * Encrypts and base 64 encodes an Integer.
   *
   * @param plain the plain Integer
   * @return encrypted string
   */
  public String encryptInteger(Integer plain)
    throws InvalidKeyException, BadPaddingException, IllegalBlockSizeException, InvalidAlgorithmParameterException {
    return encryptString(plain.toString());
  }

  public byte[] decryptByteArray(String encrypted)
    throws InvalidKeyException, BadPaddingException, IllegalBlockSizeException, InvalidAlgorithmParameterException {
    return decrypt(Base64.getDecoder().decode(encrypted));
  }

  /**
   * Encrypts and base 64 encodes an ByteArray.
   *
   * @param plain the plain ByteArray.
   * @return encrypted string
   */
  public String encryptByteArray(byte[] plain)
    throws InvalidKeyException, BadPaddingException, IllegalBlockSizeException, InvalidAlgorithmParameterException {
    return Base64.getEncoder().encodeToString(encrypt(plain));
  }

  private byte[] decrypt(byte[] encrypted)
    throws InvalidKeyException, BadPaddingException, IllegalBlockSizeException, InvalidAlgorithmParameterException {
    cipher.init(Cipher.DECRYPT_MODE, key, getInitializationVector());
    return cipher.doFinal(encrypted);
  }

  private byte[] encrypt(byte[] plain)
    throws InvalidKeyException, BadPaddingException, IllegalBlockSizeException, InvalidAlgorithmParameterException {
    cipher.init(Cipher.ENCRYPT_MODE, key, getInitializationVector());
    return cipher.doFinal(plain);
  }

  private IvParameterSpec getInitializationVector() {
    return new IvParameterSpec(
      efgsProperties.getDbencryption().getInitVector().getBytes(charset));
  }
}
