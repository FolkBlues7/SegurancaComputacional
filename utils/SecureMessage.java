import javax.crypto.SecretKey;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.security.Security;


public class SecureMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private final byte[] iv;              
    private final byte[] encryptedData;   
    private final byte[] mac;             

    private SecureMessage(byte[] iv, byte[] encryptedData, byte[] mac) {
        this.iv = iv;
        this.encryptedData = encryptedData;
        this.mac = mac;
    }

    // Getters para os campos, necessários para o processo de verificação
    public byte[] getIv() {
        return iv;
    }

    public byte[] getEncryptedData() {
        return encryptedData;
    }

    public byte[] getMac() {
        return mac;
    }


    public static SecureMessage create(String plainText, SecretKey aesKey, SecretKey macKey) throws Exception {
        // 1. Gera IV
        byte[] iv = SecurityUtils.generateIv();

        // 2. Cifra o texto
        byte[] encryptedData = SecurityUtils.encrypt(plainText, aesKey, iv);

        // 3. Calcula o HMAC
        // O HMAC deve proteger tanto o IV quanto os dados cifrados.
        byte[] dataToSign = concatBytes(iv, encryptedData);
        byte[] mac = SecurityUtils.calcularHmacSha256(macKey, dataToSign);

        // 4. Retorna o pacote
        return new SecureMessage(iv, encryptedData, mac);
    }

    public static String unpack(SecureMessage message, SecretKey aesKey, SecretKey macKey) throws Exception {

        byte[] iv = message.getIv();
        byte[] encryptedData = message.getEncryptedData();
        byte[] receivedMac = message.getMac();

        // 1. Prepara os dados para verificação do HMAC
        byte[] dataToVerify = concatBytes(iv, encryptedData);

        // 2. Verifica o HMAC
        if (!SecurityUtils.checarHmac(macKey, dataToVerify, receivedMac)) {
            
            
            throw new SecurityException("Falha na verificação do HMAC! Mensagem descartada.");
        }

       
        return SecurityUtils.decrypt(encryptedData, aesKey, iv);
    }

    /**
     * Concatena dois arrays de bytes. (iv + encryptedData)
     */
    private static byte[] concatBytes(byte[] a, byte[] b) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        os.write(a);
        os.write(b);
        return os.toByteArray();
    }
}