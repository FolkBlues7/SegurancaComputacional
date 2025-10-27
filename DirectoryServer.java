import javax.crypto.SecretKey;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Servidor de Diretório (Tarefa 2).
 * Roda em UDP, gerencia um catálogo de serviços e usa Round Robin
 * para balanceamento de carga.
 */
public class DirectoryServer {

    private static final int PORT = 13000;
    private static final int BUFFER_SIZE = 4096; // Tamanho do buffer para pacotes (inclui overhead de criptografia)

    // Chaves de segurança
    private final SecretKey aesKey;
    private final SecretKey macKey;

    // Estado Thread-Safe
    // "soma" -> [ (ServidorA, IP, Porta), (ServidorB, IP, Porta) ]
    private final Map<String, CopyOnWriteArrayList<ServiceProviderInfo>> directory;
    
    // "soma" -> 2 (próximo índice a ser usado)
    private final Map<String, AtomicInteger> roundRobinCounters;

    // Componentes de Rede e Concorrência
    private DatagramSocket socket;
    private final ExecutorService threadPool;

    public DirectoryServer() throws NoSuchAlgorithmException {
        this.aesKey = SecurityUtils.getAesKey();
        this.macKey = SecurityUtils.getMacKey();
        
        this.directory = new ConcurrentHashMap<>();
        this.roundRobinCounters = new ConcurrentHashMap<>();
        
        // Pool de threads para processar pacotes concorrentemente
        this.threadPool = Executors.newCachedThreadPool();
    }

    public void start() {
        // try-with-resources para garantir que o socket feche
        try (DatagramSocket serverSocket = new DatagramSocket(PORT)) {
            this.socket = serverSocket;
            System.out.println("Servidor de Diretório (UDP) iniciado na porta " + PORT);
            System.out.println("Aguardando registros e consultas...");

            while (true) {
                // 1. Prepara para receber um pacote
                byte[] receiveBuffer = new byte[BUFFER_SIZE];
                DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);

                // 2. Aguarda (bloqueia) até um pacote chegar
                socket.receive(receivePacket);

                // 3. Captura os dados e o endereço de retorno
                // Copiamos os dados para um array do tamanho exato
                byte[] data = Arrays.copyOf(receivePacket.getData(), receivePacket.getLength());
                InetAddress returnAddress = receivePacket.getAddress();
                int returnPort = receivePacket.getPort();
                
                // 4. Entrega o processamento para uma Thread do Pool
                // A thread principal volta imediatamente a esperar por pacotes
                threadPool.submit(() -> processPacket(data, returnAddress, returnPort));
            }
        } catch (IOException e) {
            System.err.println("Erro crítico no Servidor de Diretório: " + e.getMessage());
            e.printStackTrace();
        } finally {
            threadPool.shutdown();
        }
    }

    /**
     * Processa um pacote recebido (executado em uma thread do pool).
     */
    private void processPacket(byte[] data, InetAddress returnAddress, int returnPort) {
        String plainText;
        String clientId = returnAddress.getHostAddress() + ":" + returnPort;

        try {
            // 1. Desserializa o pacote de bytes para SecureMessage
            SecureMessage secureMessage = UdpMessageUtils.deserialize(data);
            
            // 2. VERIFICAÇÃO DE SEGURANÇA (HMAC) e Decifragem
            // Se o HMAC for inválido, unpack() lança SecurityException
            plainText = SecureMessage.unpack(secureMessage, aesKey, macKey);
            
            System.out.println("SERVIDOR: Mensagem válida recebida de " + clientId + ": " + plainText);

        } catch (SecurityException e) {
            // REQUISITO DE SEGURANÇA: Falha no HMAC
            System.err.println("SERVIDOR: Mensagem DESCARTADA de " + clientId + "! (Falha de HMAC)");
            return; // Encerra o processamento deste pacote
        } catch (Exception e) {
            System.err.println("SERVIDOR: Erro ao desempacotar mensagem de " + clientId + ": " + e.getMessage());
            return; // Pacote malformado ou erro de decifragem
        }

        // 3. Processa o comando (se a segurança foi validada)
        String[] parts = plainText.split(":", 5); // Limite de 5 partes
        String command = parts[0];

        try {
            switch (command) {
                case "REGISTER":
                    // Formato: "REGISTER:nomeServidor:portaServiço:serviço1,serviço2"
                    // Ex: "REGISTER:ServidorSomaAlfa:9001:soma,multiplicacao"
                    if (parts.length < 4) return;
                    
                    String serverName = parts[1];
                    int servicePort = Integer.parseInt(parts[2]);
                    String[] services = parts[3].split(",");
                    String serverAddress = returnAddress.getHostAddress(); // IP de quem enviou

                    ServiceProviderInfo providerInfo = new ServiceProviderInfo(serverName, serverAddress, servicePort);

                    for (String service : services) {
                        // Garante que a lista exista
                        directory.putIfAbsent(service.trim(), new CopyOnWriteArrayList<>());
                        // Adiciona o provedor à lista (thread-safe)
                        directory.get(service.trim()).add(providerInfo);
                        System.out.println("Serviço registrado: '" + service.trim() + "' em " + providerInfo);
                    }
                    // Envia confirmação
                    sendMessage("REGISTER_OK:Serviços registrados com sucesso.", returnAddress, returnPort);
                    break;

                case "LOOKUP":
                    // Formato: "LOOKUP:nomeServiço"
                    // Ex: "LOOKUP:soma"
                    if (parts.length < 2) return;
                    
                    String serviceName = parts[1];
                    List<ServiceProviderInfo> providers = directory.get(serviceName);

                    if (providers == null || providers.isEmpty()) {
                        sendMessage("LOOKUP_RESPONSE:NOT_FOUND", returnAddress, returnPort);
                    } else {
                        // REQUISITO: Balanceamento de Carga (Round Robin)
                        // Garante que o contador exista
                        roundRobinCounters.putIfAbsent(serviceName, new AtomicInteger(0));
                        
                        // Pega o índice atual, incrementa e usa o módulo (thread-safe)
                        int index = roundRobinCounters.get(serviceName).getAndIncrement() % providers.size();
                        
                        ServiceProviderInfo selectedProvider = providers.get(index);
                        
                        // Envia a resposta com o servidor escolhido
                        sendMessage("LOOKUP_RESPONSE:" + selectedProvider.toString(), returnAddress, returnPort);
                    }
                    break;
                
                default:
                    sendMessage("ERROR:Comando desconhecido", returnAddress, returnPort);
            }
        } catch (Exception e) {
            System.err.println("Erro ao processar comando '" + command + "': " + e.getMessage());
        }
    }

    /**
     * Método auxiliar para enviar uma resposta (UDP).
     * Cifra, assina, serializa e envia o pacote.
     */
    private void sendMessage(String plainText, InetAddress returnAddress, int returnPort) {
        try {
            // 1. Cria a mensagem segura
            SecureMessage secureResponse = SecureMessage.create(plainText, aesKey, macKey);
            
            // 2. Serializa para bytes
            byte[] sendData = UdpMessageUtils.serialize(secureResponse);

            // 3. Cria e envia o DatagramPacket
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, returnAddress, returnPort);
            socket.send(sendPacket);
            
        } catch (Exception e) {
            System.err.println("Falha ao enviar resposta para " + returnAddress + ":" + returnPort + ": " + e.getMessage());
        }
    }

    // --- Ponto de Entrada ---
    public static void main(String[] args) {
        try {
            DirectoryServer server = new DirectoryServer();
            server.start();
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Falha ao inicializar chaves de criptografia: " + e.getMessage());
        }
    }
}