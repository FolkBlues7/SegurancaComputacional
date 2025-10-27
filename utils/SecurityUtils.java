import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * Classe utilitária para criptografia (AES) e autenticação (HMAC-SHA256).
 * * AES (Advanced Encryption Standard) é usado para confidencialidade.
 * HMAC-SHA256 (Hash-based Message Authentication Code) é usado para integridade e autenticidade.
 */
public class SecurityUtils {

    private static final String ALGORITHM_AES = "AES/CBC/PKCS5Padding";
    private static final String ALGORITHM_HMAC = "HmacSHA256";
    private static final int AES_KEY_SIZE = 128; // 128 bits (16 bytes)
    private static final int IV_SIZE = 16;       // 16 bytes para AES/CBC

    // --- Chaves Pré-Compartilhadas ---
    // Em uma aplicação real, NÃO devem ser hardcoded!
    // Esta é a "senha" para a cifragem AES.
    private static final String AES_PASSWORD = "minha-senha-super-secreta-aes";

    // Esta é a "senha" para o HMAC[cite: 394].
    private static final String HMAC_PASSWORD = "minha-chave-secreta-hmac-diff";

    // Chave para simular um cliente "ruim"
    private static final String HMAC_PASSWORD_ERRADA = "chave-errada-do-atacante";

    // --- Métodos de Geração de Chave (Baseados nas senhas) ---

    /**
     * Gera uma SecretKey (AES ou HMAC) a partir de uma senha.
     * Usamos SHA-256 da senha para garantir o tamanho correto da chave.
     */
    private static SecretKey getKeyFromPassword(String password, String algorithm, int keySizeBits) throws NoSuchAlgorithmException {
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        byte[] keyBytes = sha.digest(password.getBytes(StandardCharsets.UTF_8));
        keyBytes = Arrays.copyOf(keyBytes, keySizeBits / 8); // Trunca para o tamanho desejado (16 bytes para AES-128, 32 para HMAC-SHA256)
        return new SecretKeySpec(keyBytes, algorithm);
    }

    public static SecretKey getAesKey() throws NoSuchAlgorithmException {
        return getKeyFromPassword(AES_PASSWORD, "AES", AES_KEY_SIZE);
    }

    public static SecretKey getMacKey() throws NoSuchAlgorithmException {
        // Para HmacSHA256, a chave é usada diretamente no SecretKeySpec
        return new SecretKeySpec(HMAC_PASSWORD.getBytes(StandardCharsets.UTF_8), ALGORITHM_HMAC);
    }

    public static SecretKey getWrongMacKey() throws NoSuchAlgorithmException {
        // Para simular o teste de falha
        return new SecretKeySpec(HMAC_PASSWORD_ERRADA.getBytes(StandardCharsets.UTF_8), ALGORITHM_HMAC);
    }

    // --- Métodos de Cifragem Simétrica (AES) ---

    /**
     * Gera um Vetor de Inicialização (IV) aleatório.
     * O IV é necessário para o modo CBC e deve ser único para cada cifragem.
     */
    public static byte[] generateIv() {
        byte[] iv = new byte[IV_SIZE];
        new SecureRandom().nextBytes(iv);
        return iv;
    }

    /**
     * Cifra uma mensagem (String) usando AES.
     * Retorna o texto cifrado (byte[]).
     */
    public static byte[] encrypt(String plainText, SecretKey key, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHM_AES);
        IvParameterSpec ivParams = new IvParameterSpec(iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, ivParams);
        return cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Decifra um texto cifrado (byte[]) usando AES.
     * Retorna a mensagem original (String).
     */
    public static String decrypt(byte[] cipherText, SecretKey key, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHM_AES);
        IvParameterSpec ivParams = new IvParameterSpec(iv);
        cipher.init(Cipher.DECRYPT_MODE, key, ivParams);
        byte[] decryptedBytes = cipher.doFinal(cipherText);
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }

    // --- Métodos HMAC (CÓDIGO DO PROFESSOR - pratica-6.4-crc-sha-hmac.pdf) ---

    /**
     * Calcula HMAC-SHA256 dado uma chave e os dados.
     * [cite: 241, 242]
     */
    public static byte[] calcularHmacSha256(SecretKey key, byte[] bytesMensagem) throws Exception {
        Mac mac = Mac.getInstance(ALGORITHM_HMAC); //
        mac.init(key); // [cite: 248]
        return mac.doFinal(bytesMensagem); // [cite: 249]
    }

    /**
     * Verifica HMAC de forma segura (tempo constante).
     * [cite: 254, 258]
     */
    public static boolean checarHmac(SecretKey key, byte[] bytesMensagem, byte[] hmacRecebido) throws Exception {
        byte[] hmacCalculado = calcularHmacSha256(key, bytesMensagem); // [cite: 256, 259]
        return MessageDigest.isEqual(hmacCalculado, hmacRecebido); // [cite: 262]
    }

    /**
     * Converte array de bytes para string hex (minúscula).
     * [cite: 214]
     */
    public static String bytes2Hex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2); // [cite: 215]
        for (byte b : bytes) {
            sb.append(String.format("%02x", b & 0xFF)); // [cite: 218]
        }
        return sb.toString(); // [cite: 221]
    }
}