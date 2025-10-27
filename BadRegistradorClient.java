import javax.crypto.SecretKey;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;

/**
 * CLIENTE DE TESTE DE SEGURANÇA 2 (Registrador Malicioso).
 *
 * Tenta enviar um "UPDATE" (Binding Dinâmico) para o servidor
 * assinando a mensagem com a CHAVE HMAC ERRADA.
 *
 * O servidor deve detectar o HMAC inválido, descartar a mensagem
 * e NÃO deve fazer o broadcast da atualização falsa.
 */
public class BadRegistradorClient {

    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 12345;

    private final SecretKey aesKey;       // Chave AES (correta)
    private final SecretKey wrongMacKey;  // Chave HMAC (ERRADA)
    private ObjectOutputStream out;

    public BadRegistradorClient() throws NoSuchAlgorithmException {
        
        this.aesKey = SecurityUtils.getAesKey();
        
        
        this.wrongMacKey = SecurityUtils.getWrongMacKey();
    }

    public void start() {
        // try-with-resources
        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
             ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream())) {

            this.out = oos;
            System.out.println("[CLIENTE RUIM REG] Conectado ao servidor.");


            String badUpdateMessage = "UPDATE:servidor1:66.66.66.66";

            System.out.println("[CLIENTE RUIM REG] Enviando atualização falsa (com MAC errado): " + badUpdateMessage);
            
            try {
                sendMessage(badUpdateMessage);
                System.out.println("[CLIENTE RUIM REG] Mensagem enviada.");
                System.out.println("[CLIENTE RUIM REG] O servidor deve descartar esta mensagem.");
                
            } catch (Exception e) {
                System.err.println("Erro ao enviar mensagem: " + e.getMessage());
            }

            // Pausa para observação dos logs
            Thread.sleep(1000);

        } catch (Exception e) {
            System.err.println("Erro no Cliente Registrador Ruim: " + e.getMessage());
        } finally {
            System.out.println("[CLIENTE RUIM REG] Desconectado.");
        }
    }


    private synchronized void sendMessage(String plainText) throws Exception {
        if (out == null) return;
        
        // AQUI ESTÁ A FALHA: Usa a 'wrongMacKey'
        SecureMessage message = SecureMessage.create(plainText, aesKey, wrongMacKey);
        
        out.writeObject(message);
        out.flush();
    }

    public static void main(String[] args) {
        try {
            BadRegistradorClient badRegistrador = new BadRegistradorClient();
            badRegistrador.start();
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Falha ao inicializar chaves: " + e.getMessage());
        }
    }
}