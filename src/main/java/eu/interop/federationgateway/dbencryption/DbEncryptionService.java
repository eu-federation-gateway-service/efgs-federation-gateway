/*-
 * ---license-start
 * EU-Federation-Gateway-Service / efgs-federation-gateway
 * ---
 * Copyright (C) 2020 - 2021 T-Systems International GmbH and all other contributors
 * ---
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ---license-end
 */

package eu.interop.federationgateway.dbencryption;

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
import javax.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.encrypt.AesBytesEncryptor;

@Slf4j
public class DbEncryptionService {

  private static final String PASSWORD_PROPERTY_NAME = "efgs_dbencryption_password";
  private static final Charset CHARSET = StandardCharsets.UTF_8;
  private static DbEncryptionService instance;
  private final Cipher cipher;
  private final Key key;

  /**
   * Constructor for DbEncryptionService.
   * Initializes Cipher with ciphersuite configured in application properties.
   */
  private DbEncryptionService() {
    cipher = AesBytesEncryptor.CipherAlgorithm.CBC.createCipher();

    String dbEncryptionPassword = System.getenv().containsKey(PASSWORD_PROPERTY_NAME)
      ? System.getenv(PASSWORD_PROPERTY_NAME)
      : System.getProperty(PASSWORD_PROPERTY_NAME);

    if (dbEncryptionPassword != null) {
      int passwordLength = dbEncryptionPassword.length();
      if (passwordLength != 16 && passwordLength != 24 && passwordLength != 32) {
        throw new ValidationException(
          "Invalid Application Configuration: Database password must be a string with length of 16, 24 or 32");
      }

      key = new SecretKeySpec(dbEncryptionPassword.getBytes(), "AES");
    } else {
      throw new ValidationException("DB encryption password must be set!");
    }
  }

  /**
   * Returns an instance of Singleton-DbEncryptionService.
   * @return The DbEncryptionService instance
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
   * @throws InvalidKeyException if the given key is inappropriate for initializing this cipher
   * @throws BadPaddingException if this cipher is in decryption mode, and (un)padding has been requested, 
   *     but the decrypted data is not bounded by the appropriate padding bytes
   * @throws IllegalBlockSizeException if this cipher is a block cipher, 
   *     no padding has been requested (only in encryption mode), and the total input length 
   *     of the data processed by this cipher is not a multiple of block size; 
   * @throws InvalidAlgorithmParameterException if the given algorithm parameters are inappropriate for this cipher
   */
  public String decryptString(String encrypted)
    throws InvalidKeyException, BadPaddingException, IllegalBlockSizeException, InvalidAlgorithmParameterException {
    return new String(decrypt(Base64.getDecoder().decode(encrypted)), CHARSET);
  }

  /**
   * Encrypts and base 64 encodes a String.
   *
   * @param plain the plain string
   * @return encrypted string
   * @throws InvalidKeyException if the given key is inappropriate for initializing this cipher
   * @throws BadPaddingException if this cipher is in decryption mode, and (un)padding has been requested, 
   *     but the decrypted data is not bounded by the appropriate padding bytes
   * @throws IllegalBlockSizeException if this cipher is a block cipher, 
   *     no padding has been requested (only in encryption mode), and the total input length 
   *     of the data processed by this cipher is not a multiple of block size; 
   * @throws InvalidAlgorithmParameterException if the given algorithm parameters are inappropriate for this cipher
   */
  public String encryptString(String plain)
    throws InvalidKeyException, BadPaddingException, IllegalBlockSizeException, InvalidAlgorithmParameterException {
    return Base64.getEncoder().encodeToString(encrypt(plain.getBytes(CHARSET)));
  }

  /**
   * Decrypts a given AES-256 encrypted and base64 encoded String.
   *
   * @param encrypted the encrypted string
   * @return decrypted integer
   * @throws InvalidKeyException if the given key is inappropriate for initializing this cipher
   * @throws BadPaddingException if this cipher is in decryption mode, and (un)padding has been requested, 
   *     but the decrypted data is not bounded by the appropriate padding bytes
   * @throws IllegalBlockSizeException if this cipher is a block cipher, 
   *     no padding has been requested (only in encryption mode), and the total input length 
   *     of the data processed by this cipher is not a multiple of block size; 
   * @throws InvalidAlgorithmParameterException if the given algorithm parameters are inappropriate for this cipher
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
   * @throws InvalidKeyException if the given key is inappropriate for initializing this cipher
   * @throws BadPaddingException if this cipher is in decryption mode, and (un)padding has been requested, 
   *     but the decrypted data is not bounded by the appropriate padding bytes
   * @throws IllegalBlockSizeException if this cipher is a block cipher, 
   *     no padding has been requested (only in encryption mode), and the total input length 
   *     of the data processed by this cipher is not a multiple of block size; 
   * @throws InvalidAlgorithmParameterException if the given algorithm parameters are inappropriate for this cipher
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
   * @throws InvalidKeyException if the given key is inappropriate for initializing this cipher
   * @throws BadPaddingException if this cipher is in decryption mode, and (un)padding has been requested, 
   *     but the decrypted data is not bounded by the appropriate padding bytes
   * @throws IllegalBlockSizeException if this cipher is a block cipher, 
   *     no padding has been requested (only in encryption mode), and the total input length 
   *     of the data processed by this cipher is not a multiple of block size; 
   * @throws InvalidAlgorithmParameterException if the given algorithm parameters are inappropriate for this cipher
   */
  public String encryptByteArray(byte[] plain)
    throws InvalidKeyException, BadPaddingException, IllegalBlockSizeException, InvalidAlgorithmParameterException {
    return Base64.getEncoder().encodeToString(encrypt(plain));
  }

  private byte[] decrypt(byte[] encrypted)
    throws InvalidKeyException, BadPaddingException, IllegalBlockSizeException, InvalidAlgorithmParameterException {
    synchronized (cipher) {
      cipher.init(Cipher.DECRYPT_MODE, key, getInitializationVector());
      return cipher.doFinal(encrypted);
    }
  }

  private byte[] encrypt(byte[] plain)
    throws InvalidKeyException, BadPaddingException, IllegalBlockSizeException, InvalidAlgorithmParameterException {
    synchronized (cipher) {
      cipher.init(Cipher.ENCRYPT_MODE, key, getInitializationVector());
      return cipher.doFinal(plain);
    }

  }

  private IvParameterSpec getInitializationVector() {
    return new IvParameterSpec("WnU2IQhlAAN@bK~L".getBytes(CHARSET));
  }
}
