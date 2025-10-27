import javax.crypto.SecretKey;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * Servidor de Serviço (Tarefa 2).
 *
 * Este programa NÃO é um servidor que fica escutando (como o Server da Tarefa 1).
 * Seu único trabalho é enviar uma (1) mensagem UDP para o DirectoryServer
 * para registrar os serviços que ele oferece.
 *
 * É um programa "execute e termine".
 */
public class ServiceServer {

    private static final String DIR_SERVER_ADDRESS = "localhost";
    private static final int DIR_SERVER_PORT = 13000;
    private static final int BUFFER_SIZE = 4096;
    private static final int RESPONSE_TIMEOUT = 5000; // 5 segundos

    private final String myServerName; // Ex: "ServiceAlpha"
    private final int myServicePort;   // Ex: 9001 (porta onde este serviço *estaria* rodando)
    private final String myServices;   // Ex: "soma,subtracao"

    private final SecretKey aesKey;
    private final SecretKey macKey;

    public ServiceServer(String serverName, int servicePort, String services) throws NoSuchAlgorithmException {
        this.myServerName = serverName;
        this.myServicePort = servicePort;
        this.myServices = services;

        // Carrega as chaves de segurança (as mesmas do DirectoryServer)
        this.aesKey = SecurityUtils.getAesKey();
        this.macKey = SecurityUtils.getMacKey();
    }

    /**
     * Envia a mensagem de registro (UDP) para o DirectoryServer.
     */
    public void register() {
        // try-with-resources: O socket é aberto e fechado automaticamente.
        // Abre um socket em uma porta aleatória (para enviar e receber a resposta)
        try (DatagramSocket socket = new DatagramSocket()) {
            
            // Define um timeout para a resposta
            socket.setSoTimeout(RESPONSE_TIMEOUT);

            // 1. Monta a mensagem de registro
            String registerMessage = String.format("REGISTER:%s:%d:%s",
                    myServerName, myServicePort, myServices);

            System.out.println("'" + myServerName + "' registrando serviços: " + myServices + " na porta " + myServicePort);
            
            // 2. Cria a mensagem segura
            SecureMessage secureMsg = SecureMessage.create(registerMessage, aesKey, macKey);

            // 3. Serializa para bytes (para UDP)
            byte[] sendData = UdpMessageUtils.serialize(secureMsg);

            // 4. Envia o pacote
            InetAddress dirAddress = InetAddress.getByName(DIR_SERVER_ADDRESS);
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, dirAddress, DIR_SERVER_PORT);
            socket.send(sendPacket);
            System.out.println("Mensagem de registro enviada para o DirectoryServer.");

            // 5. Espera pela confirmação ("REGISTER_OK")
            byte[] receiveBuffer = new byte[BUFFER_SIZE];
            DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
            
            socket.receive(receivePacket); // Bloqueia até a resposta chegar (ou dar timeout)

            // 6. Desempacota a resposta
            byte[] responseData = Arrays.copyOf(receivePacket.getData(), receivePacket.getLength());
            SecureMessage secureResponse = UdpMessageUtils.deserialize(responseData);
            
            // unpack() checa o HMAC e decifra
            String plainResponse = SecureMessage.unpack(secureResponse, aesKey, macKey);

            System.out.println("[DirectoryServer]: " + plainResponse);
            System.out.println("'" + myServerName + "' registrado com sucesso.");

        } catch (SocketTimeoutException e) {
            System.err.println("Erro: O DirectoryServer não respondeu (timeout).");
        } catch (SecurityException e) {
            System.err.println("Erro: Resposta inválida do servidor (falha de HMAC).");
        } catch (IOException e) {
            System.err.println("Erro de I/O: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Erro inesperado ao registrar: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Erro: Especifique qual servidor registrar.");
            System.err.println("Uso: java ServiceServer [1|2]");
            System.err.println("  1 = ServiceAlpha (soma, subtracao, multiplicacao)");
            System.err.println("  2 = ServiceBravo (soma, divisao)");
            return;
        }

        try {
            String serverId = args[0];
            ServiceServer server;

            if ("1".equals(serverId)) {
                // Servidor 1: Oferece 3 serviços, incluindo "soma"
                server = new ServiceServer(
                        "ServiceAlpha",
                        9001, // Porta "fictícia" de serviço
                        "soma,subtracao,multiplicacao"
                );
            } else if ("2".equals(serverId)) {
                // Servidor 2: Oferece 2 serviços, incluindo "soma"
                server = new ServiceServer(
                        "ServiceBravo",
                        9002, // Porta "fictícia" de serviço
                        "soma,divisao"
                );
            } else {
                System.err.println("ID inválido. Use '1' ou '2'.");
                return;
            }

            // Executa o registro
            server.register();

        } catch (NoSuchAlgorithmException e) {
            System.err.println("Falha ao inicializar chaves de criptografia: " + e.getMessage());
        }
    }
}