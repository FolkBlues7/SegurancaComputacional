import javax.crypto.SecretKey;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;

/**
 * CLIENTE DE TESTE DE SEGURANÇA 2 (Atacante "P7").
 *
 * Simula um processo fora da rede P2P que tenta injetar
 * uma mensagem de busca (com HMAC errado) em um nó legítimo.
 */
public class BadExternalClient {

    // Vamos atacar o Nó P0 na porta 6000
    private static final String TARGET_ADDRESS = "localhost";
    private static final int TARGET_PORT = 6000; // Porta do P0

    private final SecretKey aesKey;
    private final SecretKey wrongMacKey; // Chave HMAC errada

    public BadExternalClient() throws NoSuchAlgorithmException {
        this.aesKey = SecurityUtils.getAesKey();
        this.wrongMacKey = SecurityUtils.getWrongMacKey();
    }

    public void attack() {
        System.out.println("[ATACANTE P7] Conectando ao nó " + TARGET_ADDRESS + ":" + TARGET_PORT);
        
        // try-with-resources
        try (Socket socket = new Socket(TARGET_ADDRESS, TARGET_PORT);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {
            
            // Tenta forjar uma busca pelo "arquivo1"
            String plainText = "SEARCH:P7:arquivo1"; // "P7" é o originador
            
            System.out.println("[ATACANTE P7] Enviando mensagem (com HMAC errado): " + plainText);
            
            // *** AQUI ESTÁ A FALHA ***
            SecureMessage message = SecureMessage.create(plainText, aesKey, wrongMacKey);
            out.writeObject(message);
            out.flush();
            
            System.out.println("[ATACANTE P7] Mensagem enviada. O nó P0 deve descartá-la.");
            System.out.println("[ATACANTE P7] Encerrando.");

        } catch (Exception e) {
            System.err.println("[ATACANTE P7] Erro: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        try {
            BadExternalClient attacker = new BadExternalClient();
            attacker.attack();
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Falha ao inicializar chaves: " + e.getMessage());
        }
    }
}