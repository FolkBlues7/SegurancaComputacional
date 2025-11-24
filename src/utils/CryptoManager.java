package utils;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.nio.charset.StandardCharsets;

public class CryptoManager {

    private static final String RSA_ALGO = "RSA";
    private static final String AES_ALGO = "AES/CBC/PKCS5Padding";
    private static final int AES_KEY_SIZE = 128;
    private static final int RSA_KEY_SIZE = 2048;

    // --- GERAÇÃO DE CHAVES ---

    /**
     * Gera um par de chaves RSA (Pública e Privada).
     * Chamado uma vez por cada nó (Sensor, Edge, Cloud) ao iniciar.
     */
    public static KeyPair generateRSAKeyPair() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance(RSA_ALGO);
        keyGen.initialize(RSA_KEY_SIZE);
        return keyGen.generateKeyPair();
    }

    /**
     * Gera uma chave AES temporária (Sessão).
     * Chamado a cada nova mensagem enviada.
     */
    public static SecretKey generateAESKey() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(AES_KEY_SIZE);
        return keyGen.generateKey();
    }
    
    public static byte[] generateIv() {
        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);
        return iv;
    }

    // --- CRIPTOGRAFIA RSA (Para a Chave AES) ---

    /**
     * Cifra a chave AES usando a Chave PÚBLICA do destinatário.
     */
    public static byte[] encryptRSA(byte[] data, PublicKey publicKey) throws Exception {
        Cipher cipher = Cipher.getInstance(RSA_ALGO);
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        return cipher.doFinal(data);
    }

    /**
     * Decifra a chave AES usando a Chave PRIVADA do próprio nó.
     */
    public static byte[] decryptRSA(byte[] data, PrivateKey privateKey) throws Exception {
        Cipher cipher = Cipher.getInstance(RSA_ALGO);
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        return cipher.doFinal(data);
    }

    // --- CRIPTOGRAFIA AES (Para os Dados) ---

    /**
     * Cifra o texto (JSON/String) com a chave de sessão AES.
     */
    public static byte[] encryptAES(String plainText, SecretKey key, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance(AES_ALGO);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec);
        return cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Decifra os dados com a chave de sessão AES.
     */
    public static String decryptAES(byte[] cipherText, SecretKey key, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance(AES_ALGO);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        cipher.init(Cipher.DECRYPT_MODE, key, ivSpec);
        byte[] original = cipher.doFinal(cipherText);
        return new String(original, StandardCharsets.UTF_8);
    }

    // Utilitário para converter bytes da chave AES decifrada de volta para SecretKey
    public static SecretKey rebuildAESKey(byte[] keyBytes) {
        return new SecretKeySpec(keyBytes, "AES");
    }
}
