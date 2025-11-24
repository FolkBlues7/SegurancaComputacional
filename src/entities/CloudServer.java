package entities;
//ip : 192.168.0.16
import javax.crypto.SecretKey;
import utils.CryptoManager;
import utils.HybridMessage;
import utils.KeyRegistry;
import utils.SerializationUtils;

import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.util.Base64;
import java.util.List; // Import necessário
import java.util.concurrent.CopyOnWriteArrayList; // Import necessário para thread-safety

public class CloudServer {

    private static final int TCP_PORT = 9001;
    private PrivateKey myPrivateKey;

    // --- "Banco de Dados" em memória ---
    // Public e Static para ser acessível pelo ClientApp
    public static final List<SensorData> DATABASE = new CopyOnWriteArrayList<>();

    public void start() throws Exception {
        // 1. Identidade da Nuvem
        KeyPair keyPair = CryptoManager.generateRSAKeyPair();
        this.myPrivateKey = keyPair.getPrivate();
        KeyRegistry.registerKey("CLOUD", keyPair.getPublic());

        System.out.println("Cloud Server (Datacenter) iniciado na porta TCP " + TCP_PORT);

        try (ServerSocket serverSocket = new ServerSocket(TCP_PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                // Thread para cada conexão TCP
                new Thread(() -> handleClient(clientSocket)).start();
            }
        }
    }

    private void handleClient(Socket socket) {
        try (ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
            
            // A. Ler Objeto
            HybridMessage msg = (HybridMessage) in.readObject();

            // B. Decifrar (Híbrido)
            byte[] sessionKeyBytes = CryptoManager.decryptRSA(msg.getEncryptedKey(), myPrivateKey);
            SecretKey sessionKey = CryptoManager.rebuildAESKey(sessionKeyBytes);

            String payloadBase64 = CryptoManager.decryptAES(msg.getEncryptedPayload(), sessionKey, msg.getIv());
            byte[] dataBytes = Base64.getDecoder().decode(payloadBase64);
            
            // Desserializa os bytes de volta para o objeto SensorData
            SensorData data = (SensorData) SerializationUtils.deserialize(dataBytes);

            // --- C. Armazenamento ---
            // Adiciona o dado recebido na lista em memória
            DATABASE.add(data);

            // Log simples para não poluir muito o console da simulação
            System.out.println("[CLOUD] Dado recebido e persistido. Total de registros: " + DATABASE.size());
            
            // Se quiser ver o relatório completo chegando, descomente a linha abaixo:
            // System.out.println(data.toFullReport());

        } catch (Exception e) {
            System.err.println("Erro na Cloud: " + e.getMessage());
        }
    }
    
    public static void main(String[] args) {
        try {
            // 1. Inicia o Servidor Cloud em uma thread
            new Thread(() -> {
                try {
                    new CloudServer().start();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();

            // 2. Inicia a Aplicação Cliente (Menu) no mesmo console
            // Pequeno delay para garantir que o servidor subiu
            Thread.sleep(1000);
            new Thread(new ClientApp()).start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
