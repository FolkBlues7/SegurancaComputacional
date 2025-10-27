import javax.crypto.SecretKey;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;

/**
 * CLIENTE DE TESTE DE SEGURANÇA 1 (Requisitante Malicioso).
 *
 * Tenta fazer um LOOKUP (e se registrar), mas assina as mensagens
 * com a CHAVE HMAC ERRADA.
 *
 * O servidor deve detectar a falha no HMAC e "descartar a mensagem".
 */
public class BadRequitanteClient {

    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 12345;

    private final SecretKey aesKey;       
    private final SecretKey wrongMacKey;  
    private final SecretKey correctMacKey; 
    private ObjectOutputStream out;

    public BadRequitanteClient() throws NoSuchAlgorithmException {
        
        this.aesKey = SecurityUtils.getAesKey();
        
        
        this.correctMacKey = SecurityUtils.getMacKey();
        
       
        this.wrongMacKey = SecurityUtils.getWrongMacKey();
    }

    public void start() {
        // try-with-resources
        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
             ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) {

            this.out = oos;
            System.out.println("[CLIENTE RUIM] Conectado ao servidor.");

            ListenerThread listener = new ListenerThread(ois, aesKey, correctMacKey);
            new Thread(listener).start();

            
            System.out.println("[CLIENTE RUIM] Enviando 'REGISTER_REQUESTER' (com MAC errado)...");
            sendMessage("REGISTER_REQUESTER");
            System.out.println("[CLIENTE RUIM] Servidor deve descartar esta mensagem.");
            
            Thread.sleep(500); // Pausa para ver o log do servidor

           
            System.out.println("[CLIENTE RUIM] Enviando 'LOOKUP:servidor1' (com MAC errado)...");
            sendMessage("LOOKUP:servidor1");
            System.out.println("[CLIENTE RUIM] Servidor deve descartar. Nenhuma resposta 'LOOKUP_RESPONSE' é esperada.");


            Scanner scanner = new Scanner(System.in);
            System.out.print("[CONSOLE RUIM] Pressione Enter para sair (ou digite 'exit').");
            scanner.nextLine();

        } catch (Exception e) {
            System.err.println("Erro no cliente ruim: " + e.getMessage());
        } finally {
            System.out.println("[CLIENTE RUIM] Desconectado.");
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
            BadRequitanteClient badClient = new BadRequitanteClient();
            badClient.start();
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Falha ao inicializar chaves: " + e.getMessage());
        }
    }
}