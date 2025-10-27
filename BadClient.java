import javax.crypto.SecretKey;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * CLIENTE DE TESTE DE SEGURANÇA (Tarefa 2).
 *
 * Tenta fazer um "LOOKUP" no DirectoryServer usando
 * cifragem correta (AES), mas uma CHAVE HMAC ERRADA.
 *
 * O servidor deve detectar o HMAC inválido e descartar a mensagem.
 * O cliente deve, portanto, receber um "timeout" (nenhuma resposta).
 */
public class BadClient {

    private static final String DIR_SERVER_ADDRESS = "localhost";
    private static final int DIR_SERVER_PORT = 13000;
    private static final int BUFFER_SIZE = 4096;
    private static final int RESPONSE_TIMEOUT = 5000; // 5 segundos

    private final SecretKey aesKey;       // Chave AES (correta)
    private final SecretKey wrongMacKey;  // Chave HMAC (ERRADA)
    private final DatagramSocket socket;

    public BadClient() throws Exception {
        // Usa a chave AES correta (para cifragem)
        this.aesKey = SecurityUtils.getAesKey();
        
        // USA A CHAVE HMAC ERRADA para enviar a mensagem
        this.wrongMacKey = SecurityUtils.getWrongMacKey();
        
        // Abre o socket e define o timeout
        this.socket = new DatagramSocket();
        this.socket.setSoTimeout(RESPONSE_TIMEOUT);
    }

    /**
     * Tenta enviar um único LOOKUP malicioso.
     */
    public void testBadLookup() {
        System.out.println("[CLIENTE RUIM] Tentando 'LOOKUP:soma' com chave HMAC errada...");

        try (socket) { // O try-with-resources fechará o socket no final
            
            // 1. Monta a mensagem
            String plainText = "LOOKUP:soma";

            // 2. *** AQUI ESTÁ A FALHA ***
            // Cria a mensagem segura usando a chave HMAC errada
            SecureMessage secureMsg = SecureMessage.create(plainText, aesKey, wrongMacKey);

            // 3. Serializa para bytes
            byte[] sendData = UdpMessageUtils.serialize(secureMsg);

            // 4. Envia o pacote
            InetAddress dirAddress = InetAddress.getByName(DIR_SERVER_ADDRESS);
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, dirAddress, DIR_SERVER_PORT);
            
            System.out.println("Enviando pacote malicioso para o DirectoryServer...");
            socket.send(sendPacket);

            // 5. Espera pela resposta
            byte[] receiveBuffer = new byte[BUFFER_SIZE];
            DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);

            // Bloqueia até receber ou dar timeout
            socket.receive(receivePacket);

            // 6. Se chegarmos aqui, o TESTE FALHOU
            // O servidor respondeu, o que não deveria.
            System.err.println("[!!! TESTE FALHOU !!!] O servidor respondeu a uma mensagem com HMAC inválido.");
            
            // Tenta ler a resposta de qualquer maneira
            try {
                byte[] data = Arrays.copyOf(receivePacket.getData(), receivePacket.getLength());
                SecureMessage r = UdpMessageUtils.deserialize(data);
                // (Usaria a chave correta para tentar ler, mas não devemos chegar aqui)
                String response = SecureMessage.unpack(r, aesKey, SecurityUtils.getMacKey()); 
                System.err.println("Resposta inesperada: " + response);
            } catch (Exception e) {
                System.err.println("Não foi possível nem desempacotar a resposta inesperada.");
            }

        } catch (SocketTimeoutException e) {
            // 7. *** SUCESSO DO TESTE ***
            // O servidor não respondeu dentro do tempo limite (5 segundos).
            // Isso indica que ele descartou nossa mensagem como esperado.
            System.out.println("[TESTE BEM SUCEDIDO] O servidor não respondeu (timeout). Mensagem descartada como esperado.");
        } catch (IOException e) {
            System.err.println("Erro de I/O: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Erro inesperado no cliente ruim: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        try {
            BadClient badClient = new BadClient();
            badClient.testBadLookup();
        } catch (Exception e) {
            System.err.println("Falha ao inicializar cliente ruim: " + e.getMessage());
        }
    }
}