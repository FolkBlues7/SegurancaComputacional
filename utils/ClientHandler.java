import javax.crypto.SecretKey;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;


public class ClientHandler implements Runnable {

    private final Socket socket;
    private final Server server; // Referência ao servidor (para acesso ao estado)
    private final SecretKey aesKey;
    private final SecretKey macKey;

    private ObjectOutputStream out; // Stream de saída (declarado no escopo da classe)
    private ObjectInputStream in;   // Stream de entrada

    // Flag para saber se este cliente quer receber updates
    private boolean isRequester = false;
    private final String clientId; // Apenas para logging

    public ClientHandler(Socket socket, Server server, SecretKey aesKey, SecretKey macKey) {
        this.socket = socket;
        this.server = server;
        this.aesKey = aesKey;
        this.macKey = macKey;
        this.clientId = socket.getRemoteSocketAddress().toString();
    }

    public String getClientId() {
        return clientId;
    }

    @Override
    public void run() {
        // try-with-resources para garantir que os streams e o socket sejam fechados
        try (socket; // O socket será fechado no final
             ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) {

            this.out = oos;
            this.in = ois;

            // Loop principal de escuta do cliente
            while (socket.isConnected()) {
                // 1. Ler a mensagem segura
                SecureMessage receivedMessage = (SecureMessage) in.readObject();

                String plainText = null;

                // 2. Verificar o HMAC e Decifrar
                try {
                    // unpack() faz a verificação do HMAC
                    try {
						plainText = SecureMessage.unpack(receivedMessage, aesKey, macKey);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

                } catch (SecurityException e) {
                    // REQUISITO DE SEGURANÇA: Falha no HMAC
                    System.err.println("SERVIDOR: Mensagem DESCARTADA de " + clientId + "! (Falha de HMAC)");
                    // Apenas continua o loop, descartando a mensagem.
                    continue;
                }

                // 3. Processar o comando (se o HMAC foi válido)
                System.out.println("SERVIDOR: Mensagem válida recebida de " + clientId + ": " + plainText);
                processCommand(plainText);
            }

        } catch (EOFException | SocketException e) {
            System.out.println("Cliente " + clientId + " desconectado.");
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Erro no handler " + clientId + ": " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
            // 4. Limpeza (Cleanup)
            // Se este cliente era um requisitante, remova-o da lista de broadcast
            if (isRequester) {
                server.removeRequester(this);
            }
        }
    }

    /**
     * Processa o comando recebido do cliente (já decifrado).
     */
    private void processCommand(String plainText) throws Exception {
        String[] parts = plainText.split(":", 3); // Limita a 3 partes
        String command = parts[0];

        switch (command) {
            case "REGISTER_REQUESTER":
                this.isRequester = true;
                server.addRequester(this);
                // Envia confirmação de volta
                sendMessage("REGISTERED_OK:Você agora recebe atualizações.");
                break;

            case "LOOKUP":
                if (parts.length < 2) return; // Ignora comando malformado
                String name = parts[1];
                String ip = server.getDnsMap().getOrDefault(name, "NOT_FOUND");
                sendMessage("LOOKUP_RESPONSE:" + name + ":" + ip);
                break;

            case "UPDATE":
                if (parts.length < 3) return; // Ignora comando malformado
                String nameToUpdate = parts[1];
                String newIp = parts[2];
                // Chama o método do servidor para atualizar e fazer broadcast
                server.updateAndBroadcast(nameToUpdate, newIp);
                break;

            default:
                System.err.println("Comando desconhecido: " + command);
                sendMessage("ERROR:Comando desconhecido.");
        }
    }

    /**
     * Cifra, assina e envia uma mensagem para este cliente.
     * Este método é 'synchronized' para evitar que a thread do Server
     * (no broadcast) e a thread deste Handler (numa resposta de LOOKUP)
     * tentem escrever no 'out' ao mesmo tempo.
     */
    public synchronized void sendMessage(String plainText) throws Exception {
        if (out != null && socket.isConnected()) {
            SecureMessage response = SecureMessage.create(plainText, aesKey, macKey);
            out.writeObject(response);
            out.flush();
        } else {
            System.err.println("Tentativa de enviar mensagem para cliente desconectado " + clientId);
        }
    }
}