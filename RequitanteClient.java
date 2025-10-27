import javax.crypto.SecretKey;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;

/**
 * Cliente Requisitante (Ex: os 3 clientes da Tarefa 1).
 * 1. Conecta ao servidor.
 * 2. Inicia uma ListenerThread para receber respostas e PUSH_UPDATEs.
 * 3. Envia "REGISTER_REQUESTER" para se registrar para updates.
 * 4. Entra em um loop de console para o usuário fazer "LOOKUP".
 */
public class RequitanteClient {

    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 12345;

    private final SecretKey aesKey;
    private final SecretKey macKey;
    private ObjectOutputStream out;

    public RequitanteClient() throws NoSuchAlgorithmException {
        this.aesKey = SecurityUtils.getAesKey();
        this.macKey = SecurityUtils.getMacKey();
    }

    public void start() {
        // try-with-resources para garantir o fechamento do socket
        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
             ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) {

            this.out = oos; // Armazena o stream de saída

            System.out.println("Conectado ao servidor Mini-DNS em " + SERVER_ADDRESS + ":" + SERVER_PORT);

            // 1. Inicia a Thread de Escuta
            ListenerThread listener = new ListenerThread(ois, aesKey, macKey);
            new Thread(listener).start();

            // 2. Envia a mensagem de registro (para receber updates)
            sendMessage("REGISTER_REQUESTER");

            // 3. Inicia o loop de input do usuário (na thread principal)
            Scanner scanner = new Scanner(System.in);
            while (true) {
                System.out.print("[CONSOLE] Digite um nome (ou 'exit'): ");
                String input = scanner.nextLine();

                if ("exit".equalsIgnoreCase(input)) {
                    break; // Sai do loop (e o try-with-resources fecha o socket)
                }

                if (!input.trim().isEmpty()) {
                    // Envia um pedido de LOOKUP
                    sendMessage("LOOKUP:" + input);
                }
            }

        } catch (Exception e) {
            System.err.println("Erro no cliente: " + e.getMessage());
            e.printStackTrace();
        } finally {
            System.out.println("Cliente desconectado.");
        }
    }

    /**
     * Método auxiliar para criar e enviar uma SecureMessage.
     * Este método é 'synchronized' para o caso de querermos
     * usar múltiplas threads para enviar no futuro.
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
            RequitanteClient client = new RequitanteClient();
            client.start();
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Falha ao inicializar chaves de criptografia: " + e.getMessage());
        }
    }
}