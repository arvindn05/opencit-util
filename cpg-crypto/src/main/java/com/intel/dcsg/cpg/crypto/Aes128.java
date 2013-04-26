/*
 * Copyright (C) 2011-2012 Intel Corporation
 * All rights reserved.
 */
package com.intel.dcsg.cpg.crypto;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;

/**
 * Note:  this class is not compatible with the Aes128 class in Mt Wilson.
 * The Mt Wilson Aes128 class uses AES/CBC/PKCS5Padding.
 * This class uses AES/OFB8/NoPadding in order to be able to encrypt large
 * streams of data and also small amounts of data w/o changing modes.
 * 
 * This class ALWAYS prepends the 16-byte IV to the output (IV is always 16 bytes for AES128).
 * 
 * The key must be stored separately, of course.  See PasswordKeyEnvelope for a convenient
 * implementation of password-based encryption of the key.
 * 
 * If you want to generate a new random key, call the generateKey() method.
 * SecretKey skey = Aes128.generateKey();
 * 
 * To simplify usage, this class wraps all cryptographic exceptions with a single CryptographyException class.
 * The original exceptions are available via the getCause() method of CryptographyException.
 * 
 * The encryptString and decryptString methods use the UTF-8 character set and Base64 encoding.
 * 
 * TODO create a password constructor that automatically uses password-based key-derivation to
 * create the secret key;   possibly modify the Password object to support that and pass in 
 * a Password object,  since it already has built-in support for keeping track of the salt.
 * 
 * If you need to generate a new secret key from a password, try something like this:
 * SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
 * KeySpec spec = new PBEKeySpec("password".toCharArray(), salt_byteArray, ITERATIONS(65536), KEYLEN_BITS(128));
 * SecretKey tmp = factory.generateSecret (spec);
 * SecretKey secret = new SecretKeySpec(tmp.getEncoded(), "AES");
 * 
 * @author jbuhacoff
 */
public class Aes128 {
    private static final String ALGORITHM = "AES/OFB8/NoPadding";
    private static int KEY_LENGTH = 128;
    private static int BLOCK_SIZE = 16; // KEY_LENGTH / 8
    private SecretKey secretKey;
    private Cipher cipher;
    
    public Aes128(byte[] secretKeyAes128) throws CryptographyException {
        try {
            cipher = Cipher.getInstance(ALGORITHM);
            secretKey = new SecretKeySpec(secretKeyAes128, "AES");
        }
        catch(NoSuchAlgorithmException e) {
            throw new CryptographyException(e);
        }
        catch(NoSuchPaddingException e) {
            throw new CryptographyException(e);
        }
    }

    public Aes128(SecretKey secretKeyAes128) throws CryptographyException {
        try {
            cipher = Cipher.getInstance(ALGORITHM);
            secretKey = secretKeyAes128;
        }
        catch(NoSuchAlgorithmException e) {
            throw new CryptographyException(e);
        }
        catch(NoSuchPaddingException e) {
            throw new CryptographyException(e);
        }
    }
    
    public String getAlgorithm() { return ALGORITHM; }
    
    /**
     * Note:  the cipher algorithm and block size are NOT written to the output.  You must separately track
     * the algorithm name and block size, along with your secret key, in order to reliably decrypt the output later.
     * 
     * @param in the plaintext source ; after the input stream is copied the input stream will be closed by this method
     * @param out the ciphertext destination ; after the input stream is copied the output stream will be closed by this method
     * @throws CryptographyException 
     */
    public void encryptStream(InputStream in, OutputStream out) throws CryptographyException, IOException {
        try {
            byte[] iv = generateIV();
            AlgorithmParameterSpec paramSpec = new IvParameterSpec(iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, paramSpec); // throws InvalidAlgorithmParameterException
            CipherOutputStream cipherOut = new CipherOutputStream(out, cipher);
            IOUtils.write(iv, out);
            IOUtils.copyLarge(in, cipherOut); // throws IOException
            out.close(); // throws IOException,  calls doFinal() on the cipher, so we do this before closing the input stream just in case in.close() throws an exception
            in.close(); // throws IOException
        }
        catch(InvalidAlgorithmParameterException e) {
            throw new CryptographyException(e);
        }
        catch(InvalidKeyException e) {
            throw new CryptographyException(e);
        }
    }
    
    /**
     * @param in the ciphertext source ; after the input stream is copied the input stream will be closed by this method
     * @param out the plaintext destination ; after the input stream is copied the output stream will be closed by this method
     * @throws CryptographyException 
     */
    public void decryptStream(InputStream in, OutputStream out) throws CryptographyException, IOException {
        try {
            // first read the IV
            byte[] iv = new byte[BLOCK_SIZE];
            int ivLength = in.read(iv);
            assert ivLength == iv.length; // otherwise we didn't have enough bytes in the input to even read an IV
            AlgorithmParameterSpec paramSpec = new IvParameterSpec(iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, paramSpec); // throws InvalidAlgorithmParameterException
            CipherInputStream cipherIn = new CipherInputStream(in, cipher);
            IOUtils.copyLarge(cipherIn, out); // throws IOException
            in.close(); // throws IOException,  calls doFinal() on the cipher
            out.close(); // throws IOException
        }
        catch(InvalidAlgorithmParameterException e) {
            throw new CryptographyException(e);
        }
        catch(InvalidKeyException e) {
            throw new CryptographyException(e);
        }
    }
    
    
    
    public byte[] encrypt(byte[] plaintext) throws CryptographyException {
        try {
            byte[] iv = generateIV();
            AlgorithmParameterSpec paramSpec = new IvParameterSpec(iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, paramSpec); // throws InvalidAlgorithmParameterException
            byte[] ciphertext = cipher.doFinal(plaintext);
            return concat(iv, ciphertext);
        }
        catch(InvalidAlgorithmParameterException e) {
            throw new CryptographyException(e);
        }
        catch(InvalidKeyException e) {
            throw new CryptographyException(e);
        }
        catch(IllegalBlockSizeException e) {
            throw new CryptographyException(e);
        }
        catch(BadPaddingException e) {
            throw new CryptographyException(e);
        }
    }
    
    public String encryptString(String plaintext) throws CryptographyException {
        try {
            return Base64.encodeBase64String(encrypt(plaintext.getBytes("UTF-8")));
        } catch (UnsupportedEncodingException e) {
            throw new CryptographyException(e);
        }
    }
        
    public byte[] decrypt(byte[] ciphertext) throws CryptographyException {
        try {
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(ciphertext, 0, BLOCK_SIZE));
            return cipher.doFinal(ciphertext, BLOCK_SIZE, ciphertext.length - BLOCK_SIZE); // skip the first 16 bytes (IV)
        }
        catch(InvalidKeyException e) {
            throw new CryptographyException(e);
        }
        catch(IllegalBlockSizeException e) {
            throw new CryptographyException(e);
        }
        catch(BadPaddingException e) {
            throw new CryptographyException(e);
        }
        catch(InvalidAlgorithmParameterException e) {
            throw new CryptographyException(e);
        }
    }

    public String decryptString(String ciphertext) throws CryptographyException {
        try {
            return new String(decrypt(Base64.decodeBase64(ciphertext)), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new CryptographyException(e);
        }
    }
    
    
    private byte[] concat(byte[] a, byte[] b) {
        byte[] result = new byte[a.length + b.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }
    
    private byte[] generateIV() {
        SecureRandom random = new SecureRandom();
        int blockSize = cipher.getBlockSize();
        assert blockSize > 0;
        byte[] iv = new byte[blockSize];
        random.nextBytes(iv);
        return iv;
    }
    
    public static SecretKey generateKey() throws CryptographyException {
        try {
            KeyGenerator kgen = KeyGenerator.getInstance("AES");
            kgen.init(KEY_LENGTH);
            SecretKey skey = kgen.generateKey();
            return skey;
        }
        catch(NoSuchAlgorithmException e) {
            throw new CryptographyException(e);
        }
    }
}
