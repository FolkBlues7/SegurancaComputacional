import javax.crypto.SecretKey;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;

/**
 * Classe principal de um Nó P2P (Tarefa 3).
 *
 * Cada instância desta classe é um nó no anel.
 * Ela faz duas coisas simultaneamente:
 * 1. Roda um Servidor (em uma thread) para ouvir mensagens do antecessor.
 * 2. Roda um leitor de Input (na thread main) para iniciar buscas.
 */
public class P2PNode {

    private final NodeInfo myNodeInfo;
    private final SecretKey aesKey;
    private final SecretKey macKey;
    private final SecretKey wrongMacKey; // Para o Teste de Segurança 1
    private final String logPrefix;
    private final String prompt;

    public P2PNode(int nodeId) throws NoSuchAlgorithmException {
        this.myNodeInfo = RingTopology.getNodeInfo(nodeId);
        this.aesKey = SecurityUtils.getAesKey();
        this.macKey = SecurityUtils.getMacKey();
        this.wrongMacKey = SecurityUtils.getWrongMacKey(); // Chave para simular falha
        this.logPrefix = "[" + myNodeInfo.getName() + " LOG]: ";
        this.prompt = "[" + myNodeInfo.getName() + " CONSOLE]: ";

        System.out.println("--- Nó " + myNodeInfo.getName() + " INICIADO ---");
        System.out.println(myNodeInfo);
        System.out.println("-------------------------");
    }

    /**
     * Inicia as duas funções principais do nó.
     */
    public void start() {
        // 1. Inicia o servidor (para ouvir o antecessor) em uma nova thread
        new Thread(this::startServer).start();
        
        // 2. Inicia o loop de input do usuário (na thread main)
        startUserInput();
    }

    /**
     * (Roda na Thread do Servidor)
     * Fica em loop, aceitando conexões (do antecessor)
     * e despachando-as para MessageHandlers.
     */
    private void startServer() {
        // try-with-resources
        try (ServerSocket serverSocket = new ServerSocket(myNodeInfo.getPort())) {
            System.out.println(logPrefix + "Servidor ouvindo na porta " + myNodeInfo.getPort());
            
            while (true) {
                Socket clientSocket = serverSocket.accept(); // Bloqueia até uma conexão chegar
                // Entrega para um novo MessageHandler em uma nova thread
                new Thread(new MessageHandler(clientSocket, myNodeInfo, aesKey, macKey)).start();
            }
        } catch (IOException e) {
            System.err.println(logPrefix + "Erro fatal no servidor " + myNodeInfo.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * (Roda na Thread Main)
     * Fica em loop, lendo a entrada do usuário no console.
     */
    private void startUserInput() {
        Scanner scanner = new Scanner(System.in);
        
        while (true) {
            System.out.println("\nComandos: 'buscar [arquivoXX]', 'buscafalha [arquivoXX]', 'exit'");
            System.out.print(prompt);
            String line = scanner.nextLine();
            String[] parts = line.split("\\s+", 2); // Divide em "comando" e "resto"
            String command = parts[0].toLowerCase();
            
            if (parts.length < 1) continue;

            try {
                switch (command) {
                    case "buscar":
                    case "buscafalha":
                        if (parts.length < 2) {
                            System.out.println("Erro na entrada de dados. Tente outra vez!");
                            continue;
                        }
                        String fileName = parts[1].trim();
                        
                        // Monta a mensagem de busca
                        String plainText = "SEARCH:" + myNodeInfo.getName() + ":" + fileName;

                        // TESTE DE SEGURANÇA 1: "buscafalha" usa a chave HMAC errada
                        SecretKey keyToUse = (command.equals("buscafalha")) ? wrongMacKey : macKey;
                        
                        if(command.equals("buscafalha")) {
                             System.out.println(logPrefix + "INICIANDO BUSCA COM FALHA (HMAC ERRADO) para: " + fileName);
                        } else {
                             System.out.println(logPrefix + "Iniciando busca por: " + fileName);
                        }
                        
                        // Envia para o sucessor
                        sendToSuccessor(plainText, myNodeInfo, aesKey, keyToUse);
                        break;
                    
                    case "exit":
                        System.out.println(logPrefix + "Encerrando nó...");
                        scanner.close();
                        System.exit(0); // Força o encerramento (mata a thread do servidor)
                        return;

                    default:
                        // REQUISITO: Erro de entrada
                        System.out.println("Erro na entrada de dados. Tente outra vez!");
                }
            } catch (Exception e) {
                System.err.println(logPrefix + "Erro ao enviar mensagem: " + e.getMessage());
            }
        }
    }

    /**
     * Método auxiliar (CLIENTE TCP) para enviar uma mensagem ao SUCESSOR.
     * Usado tanto para iniciar uma busca quanto para repassar uma.
     */
    public static void sendToSuccessor(String plainText, NodeInfo myNodeInfo, SecretKey aesKey, SecretKey macKey) throws Exception {
        String logPrefix = "[" + myNodeInfo.getName() + " LOG]: ";
        
        // try-with-resources: Abre uma *nova* conexão com o sucessor,
        // envia a mensagem e fecha tudo.
        try (Socket socket = new Socket("localhost", myNodeInfo.getSuccessorPort());
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {
            
            // Cria e envia a mensagem segura
            SecureMessage message = SecureMessage.create(plainText, aesKey, macKey);
            out.writeObject(message);
            out.flush();
            
            // LOG (Requisito da Tarefa)
            System.out.println(logPrefix + "[ENVIADO para Porta " + myNodeInfo.getSuccessorPort() + "]: " + plainText);
            
        }
    }

    // --- Ponto de Entrada ---
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Erro: Você precisa especificar o ID do nó (0 a 5).");
            System.err.println("Uso: java P2PNode [id]");
            // --- ADAPTAÇÃO PARA ECLIPSE ---
            System.err.println("\nOU (se estiver no Eclipse/sem args), digite o ID abaixo:");
            Scanner scanner = new Scanner(System.in);
            System.out.print("Digite o ID do Nó (0-5): ");
            String id = scanner.nextLine();
            args = new String[]{id};
            // --- Fim da Adaptação ---
        }

        try {
            int nodeId = Integer.parseInt(args[0]);
            if (nodeId < 0 || nodeId >= RingTopology.TOTAL_NODES) {
                System.err.println("ID inválido. Deve ser entre 0 e 5.");
                return;
            }
            
            // Cria e inicia o nó
            P2PNode node = new P2PNode(nodeId);
            node.start();

        } catch (NumberFormatException e) {
            System.err.println("ID inválido. Deve ser um número (0-5).");
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Falha ao inicializar chaves de criptografia: " + e.getMessage());
        }
    }
}