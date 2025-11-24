package utils;

import java.io.Serializable;

/**
 * Representa uma mensagem protegida por Criptografia Híbrida.
 * Implementa Serializable para envio via TCP/UDP.
 */
public class HybridMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String senderId;        // Quem enviou (ex: "Sensor01")
    private final byte[] encryptedKey;    // Chave AES cifrada com RSA (para o destinatário abrir)
    private final byte[] iv;              // Vetor de Inicialização do AES
    private final byte[] encryptedPayload;// Os dados reais (SensorData) cifrados com AES

    public HybridMessage(String senderId, byte[] encryptedKey, byte[] iv, byte[] encryptedPayload) {
        this.senderId = senderId;
        this.encryptedKey = encryptedKey;
        this.iv = iv;
        this.encryptedPayload = encryptedPayload;
    }

    public String getSenderId() { return senderId; }
    public byte[] getEncryptedKey() { return encryptedKey; }
    public byte[] getIv() { return iv; }
    public byte[] getEncryptedPayload() { return encryptedPayload; }
}