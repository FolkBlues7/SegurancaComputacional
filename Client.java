import javax.crypto.SecretKey;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Scanner;

/**
 * Cliente (Tarefa 2).
 *
 * Este programa consulta o DirectoryServer (via UDP) para
 * descobrir o endereço de servidores que oferecem um serviço específico.
 */
public class Client {

    private static final String DIR_SERVER_ADDRESS = "localhost";
    private static final int DIR_SERVER_PORT = 13000;
    private static final int BUFFER_SIZE = 4096;
    private static final int RESPONSE_TIMEOUT = 5000; // 5 segundos

    private final SecretKey aesKey;
    private final SecretKey macKey;
    private final DatagramSocket socket; // O socket é reutilizado para todas as consultas

    public Client() throws Exception {
        this.aesKey = SecurityUtils.getAesKey();
        this.macKey = SecurityUtils.getMacKey();
        
        // Abre o socket UDP em uma porta aleatória
        this.socket = new DatagramSocket(); 
        
        // Define o timeout de resposta
        this.socket.setSoTimeout(RESPONSE_TIMEOUT); 
    }

    public void start() {
        System.out.println("Cliente de Descoberta de Serviço (UDP) iniciado.");
        System.out.println("Serviços de exemplo para consultar: 'soma', 'subtracao', 'multiplicacao', 'divisao'");

        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print("\nDigite o serviço que deseja buscar (ou 'exit'): ");
                String serviceName = scanner.nextLine();

                if ("exit".equalsIgnoreCase(serviceName) || serviceName.trim().isEmpty()) {
                    break;
                }

                // Envia a consulta e espera a resposta
                lookupService(serviceName);
            }
        } finally {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            System.out.println("Cliente encerrado.");
        }
    }

    /**
     * Envia um "LOOKUP" para o DirectoryServer e aguarda a resposta.
     */
    private void lookupService(String serviceName) {
        try {
            // 1. Monta e protege a mensagem
            String plainText = "LOOKUP:" + serviceName;
            SecureMessage secureMsg = SecureMessage.create(plainText, aesKey, macKey);

            // 2. Serializa para bytes (para UDP)
            byte[] sendData = UdpMessageUtils.serialize(secureMsg);

            // 3. Envia o pacote de consulta
            InetAddress dirAddress = InetAddress.getByName(DIR_SERVER_ADDRESS);
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, dirAddress, DIR_SERVER_PORT);
            
            System.out.println("Consultando: '" + serviceName + "'...");
            socket.send(sendPacket);

            // 4. Prepara para receber a resposta
            byte[] receiveBuffer = new byte[BUFFER_SIZE];
            DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);

            // 5. Aguarda a resposta (bloqueia até receber ou dar timeout)
            socket.receive(receivePacket);

            // 6. Desempacota a resposta
            byte[] responseData = Arrays.copyOf(receivePacket.getData(), receivePacket.getLength());
            SecureMessage secureResponse = UdpMessageUtils.deserialize(responseData);
            
            // unpack() checa o HMAC e decifra
            String plainResponse = SecureMessage.unpack(secureResponse, aesKey, macKey);

            System.out.println("[DirectoryServer]: " + plainResponse);

        } catch (SocketTimeoutException e) {
            // Isso acontece se o servidor não responder
            // (seja por estar offline ou por DESCARTAR nossa mensagem por falha de HMAC)
            System.err.println("Erro: O Servidor de Diretório não respondeu (timeout).");
        } catch (SecurityException e) {
            System.err.println("Erro: Resposta inválida do servidor (falha de HMAC).");
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Erro de comunicação: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Erro inesperado ao consultar: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        try {
            Client client = new Client();
            client.start();
        } catch (Exception e) {
            System.err.println("Falha ao inicializar cliente: " + e.getMessage());
        }
    }
}