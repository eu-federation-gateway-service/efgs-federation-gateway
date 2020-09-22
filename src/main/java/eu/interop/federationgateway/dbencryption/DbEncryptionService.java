package eu.interop.federationgateway.dbencryption;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.util.Base64;
import java.util.Random;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.encrypt.AesBytesEncryptor;

@Slf4j
public class DbEncryptionService {

  private static final String PASSWORD_PROPERTY_NAME = "efgs_dbencryption_password";
  private static final Charset charset = StandardCharsets.UTF_8;
  private static DbEncryptionService instance;
  private final Cipher cipher;
  private final Key key;

  /**
   * Constructor for DbEncryptionService.
   * Initializes Cipher with ciphersuite configured in application properties.
   */
  private DbEncryptionService() {
    cipher = AesBytesEncryptor.CipherAlgorithm.CBC.createCipher();

    String dbEncryptionPassword = System.getenv().get(PASSWORD_PROPERTY_NAME);

    if (dbEncryptionPassword != null) {
      int passwordLength = dbEncryptionPassword.length();
      if (passwordLength != 16 && passwordLength != 24 && passwordLength != 32) {
        throw new ValidationException(
          "Invalid Application Configuration: Database password must be a string with length of 16, 24 or 32");
      }

      key = new SecretKeySpec(dbEncryptionPassword.getBytes(), "AES");
    } else {
      log.info("No DB encryption password found. Generating random password.");
      Random random = new Random();
      byte[] password = new byte[16];
      random.nextBytes(password);

      key = new SecretKeySpec(password, "AES");
    }
  }

  /**
   * Returns an instance of Singleton-DbEncryptionService.
   */
  public static DbEncryptionService getInstance() {
    if (DbEncryptionService.instance == null) {
      DbEncryptionService.instance = new DbEncryptionService();
    }

    return instance;
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
    return new IvParameterSpec("WnU2IQhlAAN@bK~L".getBytes(charset));
  }
}
