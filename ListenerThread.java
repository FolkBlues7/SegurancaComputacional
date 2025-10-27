import javax.crypto.SecretKey;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.SocketException;

/**
 * Thread de escuta para o Cliente Requisitante.
 * Fica em loop lendo mensagens do servidor (respostas de LOOKUP ou PUSH_UPDATE)
 * e as imprime no console.
 */
public class ListenerThread implements Runnable {

    private final ObjectInputStream in;
    private final SecretKey aesKey;
    private final SecretKey macKey;

    public ListenerThread(ObjectInputStream in, SecretKey aesKey, SecretKey macKey) {
        this.in = in;
        this.aesKey = aesKey;
        this.macKey = macKey;
    }

    @Override
    public void run() {
        try {
            // Loop de escuta
            while (true) {
                // 1. Bloqueia até receber uma mensagem
                SecureMessage receivedMessage = (SecureMessage) in.readObject();

                String plainText;

                // 2. Tenta desempacotar (verificar HMAC e decifrar)
                try {
                    plainText = SecureMessage.unpack(receivedMessage, aesKey, macKey);

                    // 3. Imprime a mensagem do servidor
                    // Usamos \r (carriage return) e [CONSOLE] para não bagunçar
                    // a linha de input do usuário no console principal.
                    System.out.println("\r[Servidor]: " + plainText);
                    System.out.print("[CONSOLE] Digite um nome (ou 'exit'): "); // Mostra o prompt de novo

                } catch (SecurityException e) {
                    // Isso não deve acontecer se o servidor for legítimo
                    System.err.println("\r[Alerta]: Recebida mensagem inválida do servidor! (Falha de HMAC)");
                    System.out.print("[CONSOLE] Digite um nome (ou 'exit'): ");
                } catch (Exception e) {
                    System.err.println("\r[Erro]: Falha ao decifrar mensagem: " + e.getMessage());
                    System.out.print("[CONSOLE] Digite um nome (ou 'exit'): ");
                }
            }
        } catch (SocketException | EOFException e) {
            // Servidor caiu ou fechou a conexão
            System.out.println("\r[Info]: Conexão com o servidor perdida.");
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Erro na thread de escuta: " + e.getMessage());
            e.printStackTrace();
        }
    }
}