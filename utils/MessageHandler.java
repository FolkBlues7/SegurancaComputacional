import javax.crypto.SecretKey;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;

/**
 * Runnable que processa uma única conexão TCP recebida por um P2PNode.
 * Ele verifica o HMAC, decifra a mensagem e executa a lógica de busca/repasse.
 */
public class MessageHandler implements Runnable {

    private final Socket socket;
    private final NodeInfo myNodeInfo; // Informações do nó QUE ESTÁ EXECUTANDO este handler
    private final SecretKey aesKey;
    private final SecretKey macKey;
    private final String logPrefix;

    public MessageHandler(Socket socket, NodeInfo myNodeInfo, SecretKey aesKey, SecretKey macKey) {
        this.socket = socket;
        this.myNodeInfo = myNodeInfo;
        this.aesKey = aesKey;
        this.macKey = macKey;
        this.logPrefix = "[" + myNodeInfo.getName() + " LOG]: ";
    }

    @Override
    public void run() {
        String remoteAddress = socket.getRemoteSocketAddress().toString();
        
        // try-with-resources para fechar o socket e o stream de entrada
        try (socket;
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            // 1. Ler a mensagem
            SecureMessage receivedMessage = (SecureMessage) in.readObject();

            String plainText;
            
            // 2. VERIFICAÇÃO DE SEGURANÇA (HMAC)
            try {
                plainText = SecureMessage.unpack(receivedMessage, aesKey, macKey);
                
                // LOG (Requisito da Tarefa)
                System.out.println(logPrefix + "[RECEBIDO de " + remoteAddress + "]: " + plainText);
                
            } catch (SecurityException e) {
                // REQUISITO DE SEGURANÇA (Testes 1 e 2): Falha no HMAC
                System.err.println(logPrefix + "Mensagem DESCARTADA de " + remoteAddress + "! (Falha de HMAC)");
                return; // Encerra o processamento
            }
            
            // 3. Processar a mensagem (se o HMAC foi válido)
            String[] parts = plainText.split(":", 3);
            if (parts.length < 3 || !parts[0].equals("SEARCH")) {
                System.err.println(logPrefix + "Mensagem malformada descartada: " + plainText);
                return;
            }

            String originatorName = parts[1];
            String fileName = parts[2];

            // 4. Lógica de Roteamento P2P
            
            // 4a. O arquivo foi encontrado?
            if (myNodeInfo.hasFile(fileName)) {
                System.out.println(logPrefix + "ARQUIVO ENCONTRADO! '" + fileName + "' está neste nó.");
                // Em um sistema real, enviaríamos uma mensagem "FOUND" de volta.
                // Aqui, apenas paramos o repasse.
                return;
            }

            // 4b. A mensagem deu a volta no anel? (Arquivo não existe)
            if (originatorName.equals(myNodeInfo.getName())) {
                System.out.println(logPrefix + "BUSCA FALHOU. '" + fileName + "' não foi encontrado no anel (mensagem deu a volta).");
                return;
            }

            // 4c. Não está aqui e não deu a volta? REPASSAR.
            System.out.println(logPrefix + "'" + fileName + "' não está aqui. Repassando para o sucessor (Porta " + myNodeInfo.getSuccessorPort() + ")...");
            
            // Encaminha a *mesma mensagem de texto original* para o sucessor
            // Usando a chave MAC correta (pois este nó é legítimo)
            P2PNode.sendToSuccessor(plainText, myNodeInfo, aesKey, macKey);

        } catch (SocketException | java.io.EOFException e) {
            System.out.println(logPrefix + "Conexão fechada por " + remoteAddress);
        } catch (IOException | ClassNotFoundException e) {
            System.err.println(logPrefix + "Erro ao ler mensagem de " + remoteAddress + ": " + e.getMessage());
        } catch (Exception e) {
            System.err.println(logPrefix + "Erro inesperado no handler: " + e.getMessage());
            e.printStackTrace();
        }
    }
}