import javax.crypto.SecretKey;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Classe principal do Servidor DNS.
 * Gerencia o estado compartilhado (mapa DNS e lista de clientes)
 * e aceita novas conexões de clientes.
 */
public class Server {

    private static final int PORT = 12345;

    // 1. Estado Compartilhado (Thread-Safe)

    // Mapa DNS: "servidor1" -> "192.168.0.10"
    // Usamos ConcurrentHashMap para permitir leituras e escritas
    // seguras a partir de múltiplas threads (ClientHandlers).
    private final Map<String, String> dnsMap;

    // Lista de Handlers de Clientes Requisitantes
    // Usamos CopyOnWriteArrayList para "broadcasts" seguros.
    // É otimizado para cenários onde lemos (iteramos) a lista
    // com muito mais frequência do que escrevemos (adicionamos/removemos).
    private final List<ClientHandler> requesterHandlers;

    // Chaves de criptografia carregadas uma vez
    private final SecretKey aesKey;
    private final SecretKey macKey;

    public Server() throws NoSuchAlgorithmException {
        // Inicializa as coleções thread-safe
        this.dnsMap = new ConcurrentHashMap<>();
        this.requesterHandlers = new CopyOnWriteArrayList<>();

        // Carrega as chaves
        this.aesKey = SecurityUtils.getAesKey();
        this.macKey = SecurityUtils.getMacKey();

        // Popula o mapa DNS inicial (Requisito da Tarefa 1)
        initializeDnsMap();
    }

    private void initializeDnsMap() {
        dnsMap.put("servidor1", "192.168.0.10");
        dnsMap.put("servidor2", "192.168.0.20");
        dnsMap.put("servidor3", "192.168.0.30");
        dnsMap.put("servidor4", "192.168.0.40");
        dnsMap.put("servidor5", "192.168.0.50");
        dnsMap.put("servidor6", "192.168.0.60");
        dnsMap.put("servidor7", "192.168.0.70");
        dnsMap.put("servidor8", "192.168.0.80");
        dnsMap.put("servidor9", "192.168.0.90");
        dnsMap.put("servidor10", "192.168.0.100");
    }

    /**
     * Inicia o loop principal do servidor.
     */
    public void start() {
        // try-with-resources garante que o ServerSocket será fechado
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Servidor Mini-DNS iniciado na porta " + PORT);
            System.out.println("Aguardando conexões...");

            while (true) {
                // Bloqueia até um cliente se conectar
                Socket clientSocket = serverSocket.accept();
                System.out.println("Novo cliente conectado: " + clientSocket.getRemoteSocketAddress());

                // Cria um novo handler para este cliente
                ClientHandler handler = new ClientHandler(clientSocket, this, aesKey, macKey);

                // Inicia o handler em uma nova Thread
                new Thread(handler).start();
            }
        } catch (IOException e) {
            System.err.println("Erro crítico no servidor: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // --- Métodos de acesso ao Estado Compartilhado ---

    /**
     * Retorna o mapa DNS.
     */
    public Map<String, String> getDnsMap() {
        return dnsMap;
    }

    /**
     * Adiciona um cliente à lista de "escuta" de atualizações.
     * Chamado pelo ClientHandler quando ele recebe "REGISTER_REQUESTER".
     */
    public void addRequester(ClientHandler handler) {
        requesterHandlers.add(handler);
        System.out.println("Handler " + handler.getClientId() + " registrado para updates.");
    }

    /**
     * Remove um cliente da lista (quando ele desconecta).
     */
    public void removeRequester(ClientHandler handler) {
        requesterHandlers.remove(handler);
        System.out.println("Handler " + handler.getClientId() + " removido dos updates.");
    }

    /**
     * Atualiza um registro DNS e notifica (broadcast) todos os clientes.
     * Este é o "Binding Dinâmico".
     */
    public void updateAndBroadcast(String name, String newIp) {
        // 1. Atualiza o mapa
        dnsMap.put(name, newIp);
        System.out.println("[BINDING DINÂMICO] Atualizado: " + name + " -> " + newIp);

        // 2. Prepara a mensagem de notificação
        String updateMessage = "PUSH_UPDATE:" + name + ":" + newIp;

        // 3. Faz o broadcast (envia para todos os requisitantes)
        System.out.println("Enviando PUSH_UPDATE para " + requesterHandlers.size() + " clientes...");
        for (ClientHandler handler : requesterHandlers) {
            try {
                // Pede para o handler do cliente enviar a mensagem
                handler.sendMessage(updateMessage);
            } catch (Exception e) {
                System.err.println("Erro ao enviar update para " + handler.getClientId() + ": " + e.getMessage());
                // (O handler.run() cuidará da remoção se a conexão caiu)
            }
        }
    }

    // --- Ponto de Entrada ---
    public static void main(String[] args) {
        try {
            Server server = new Server();
            server.start();
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Falha ao inicializar chaves de criptografia: " + e.getMessage());
        }
    }
}