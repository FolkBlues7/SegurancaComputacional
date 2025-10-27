import javax.crypto.SecretKey;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;

/**
 * Cliente Registrador (Tarefa 1).
 *
 * Simula um serviço (Binding Dinâmico) que se conecta ao servidor
 * apenas para atualizar o endereço IP de alguns nomes.
 *
 * Processo:
 * 1. Conecta ao servidor.
 * 2. Envia 3 mensagens "UPDATE".
 * 3. Fecha a conexão.
 */
public class RegistradorClient {

    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 12345;

    private final SecretKey aesKey;
    private final SecretKey macKey;
    private ObjectOutputStream out;

    public RegistradorClient() throws NoSuchAlgorithmException {
        // Usa as mesmas chaves legítimas do servidor
        this.aesKey = SecurityUtils.getAesKey();
        this.macKey = SecurityUtils.getMacKey();
    }

    public void start() {
        // try-with-resources garante o fechamento do socket e streams
        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
             ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) { // ois é necessário para o ObjectOutputStream funcionar corretamente

            this.out = oos; // Armazena o stream de saída

            System.out.println("Cliente Registrador conectado a " + SERVER_ADDRESS + ":" + SERVER_PORT);

            // Lista de atualizações a serem enviadas
            String[] updates = {
                    "UPDATE:servidor1:10.10.10.1",
                    "UPDATE:servidor4:40.40.40.4",
                    "UPDATE:servidor9:90.90.90.9"
            };

            for (String updateMsg : updates) {
                try {
                    sendMessage(updateMsg);
                    System.out.println("Enviando atualização: " + updateMsg);

                    // Pequena pausa para fins de demonstração
                    Thread.sleep(500);

                } catch (Exception e) {
                    System.err.println("Erro ao enviar mensagem '" + updateMsg + "': " + e.getMessage());
                }
            }

            System.out.println("Atualizações enviadas. Fechando conexão.");

        } catch (Exception e) {
            System.err.println("Erro no Cliente Registrador: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Método auxiliar para criar e enviar uma SecureMessage.
     */
    private synchronized void sendMessage(String plainText) throws Exception {
        if (out == null) {
            System.err.println("Stream de saída não está pronto.");
            return;
        }
        SecureMessage message = SecureMessage.create(plainText, aesKey, macKey);
        out.writeObject(message);
        out.flush();
    }

    public static void main(String[] args) {
        try {
            RegistradorClient registrador = new RegistradorClient();
            registrador.start();
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Falha ao inicializar chaves de criptografia: " + e.getMessage());
        }
    }
}