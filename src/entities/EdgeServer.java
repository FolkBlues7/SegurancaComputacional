package entities;

import javax.crypto.SecretKey;

import utils.CryptoManager;
import utils.HybridMessage;
import utils.KeyRegistry;
import utils.SerializationUtils;

import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Socket;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;

public class EdgeServer {

    private static final int UDP_PORT = 9000;
    private static final String CLOUD_HOST = "localhost";
    private static final int CLOUD_PORT = 9001;

    private PrivateKey myPrivateKey;

    public void start() throws Exception {
        // 1. Configuração de Chaves (Identidade do Edge)
        KeyPair keyPair = CryptoManager.generateRSAKeyPair();
        this.myPrivateKey = keyPair.getPrivate();
        KeyRegistry.registerKey("EDGE", keyPair.getPublic()); // Publica para os sensores verem

        System.out.println("Edge Server iniciado na porta UDP " + UDP_PORT);

        try (DatagramSocket socket = new DatagramSocket(UDP_PORT)) {
            byte[] buffer = new byte[4096 * 2]; // Buffer grande para objeto serializado

            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                // Thread para processar cada pacote recebido (não bloquear o servidor)
                new Thread(() -> processPacket(packet)).start();
            }
        }
    }

    private void processPacket(DatagramPacket packet) {
        try {
            // A. Desserializar a mensagem Híbrida
            byte[] rawData = new byte[packet.getLength()];
            System.arraycopy(packet.getData(), 0, rawData, 0, packet.getLength());
            HybridMessage msg = (HybridMessage) SerializationUtils.deserialize(rawData);

            // B. Decifrar a Chave AES (usando minha RSA Privada)
            byte[] sessionKeyBytes = CryptoManager.decryptRSA(msg.getEncryptedKey(), myPrivateKey);
            SecretKey sessionKey = CryptoManager.rebuildAESKey(sessionKeyBytes);

            // C. Decifrar o Payload (usando a AES recuperada)
            String payloadBase64 = CryptoManager.decryptAES(msg.getEncryptedPayload(), sessionKey, msg.getIv());
            byte[] sensorDataBytes = Base64.getDecoder().decode(payloadBase64);
            SensorData data = (SensorData) SerializationUtils.deserialize(sensorDataBytes);

            System.out.println("[Edge] Recebido de " + msg.getSenderId() + ": " + data.getCo2() + "ppm CO2");

            // D. Análise de Borda (Alertas Simples)
            if (data.getTemperature() > 35.0) {
                System.out.println("!!! ALERTA DE BORDA: Alta Temperatura detectada no sensor " + msg.getSenderId());
            }
            if (data.getCo2() > 700.0) {
                System.out.println("!!! ALERTA DE BORDA: Níveis de CO2 elevados no sensor " + msg.getSenderId());
            }

            // E. Encaminhar para a Nuvem (TCP)
            forwardToCloud(data);

        } catch (Exception e) {
            System.err.println("Erro ao processar pacote no Edge: " + e.getMessage());
            // e.printStackTrace();
        }
    }

    private void forwardToCloud(SensorData data) {
        try {
            // 1. Obter chave da Nuvem
            PublicKey cloudKey = KeyRegistry.getPublicKey("CLOUD");
            if (cloudKey == null) return;

            // 2. Re-criptografia Híbrida (Nova chave AES para este trecho)
            SecretKey newSessionKey = CryptoManager.generateAESKey();
            byte[] iv = CryptoManager.generateIv();

            // Serializa e Cifra Payload
            byte[] dataBytes = SerializationUtils.serialize(data);
            String base64Data = Base64.getEncoder().encodeToString(dataBytes);
            byte[] encryptedPayload = CryptoManager.encryptAES(base64Data, newSessionKey, iv);

            // Cifra Chave AES
            byte[] encryptedKey = CryptoManager.encryptRSA(newSessionKey.getEncoded(), cloudKey);

            HybridMessage cloudMsg = new HybridMessage("EDGE_FORWARDER", encryptedKey, iv, encryptedPayload);

            // 3. Envio TCP
            try (Socket socket = new Socket(CLOUD_HOST, CLOUD_PORT);
                 ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {
                out.writeObject(cloudMsg);
            }

        } catch (Exception e) {
            System.err.println("Erro ao enviar para Cloud: " + e.getMessage());
        }
    }
    
    public static void main(String[] args) {
        try {
            System.out.println("Iniciando Edge Server...");
            // O método start() já bloqueia o processo ouvindo UDP, 
            // então podemos chamá-lo diretamente.
            new EdgeServer().start();
        } catch (Exception e) {
            System.err.println("Erro fatal no Edge: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
