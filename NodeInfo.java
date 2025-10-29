import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * Classe de dados (POJO) para armazenar as informações
 * de configuração de um único nó P2P.
 */
public class NodeInfo implements Serializable {

    private static final long serialVersionUID = 3L;

    private final int id;             // 0, 1, 2, 3, 4, 5
    private final String name;        // "P0", "P1", ...
    private final int port;           // Porta de escuta (ex: 6000)
    private final int successorPort;  // Porta do próximo nó (ex: 6001)
    private final Set<String> files;  // Arquivos que este nó armazena

    public NodeInfo(int id, int basePort) {
        this.id = id;
        this.name = "P" + id;
        this.port = basePort + id; 
        
        
        int successorId = (id == 5) ? 0 : id + 1;
        this.successorPort = basePort + successorId;

        
        this.files = new HashSet<>();
        int startFile = (id * 10) + 1;  // P0 -> 1, P1 -> 11
        int endFile = (id * 10) + 10; // P0 -> 10, P1 -> 20
        for (int i = startFile; i <= endFile; i++) {
            files.add("arquivo" + i);
        }
    }

    // Getters
    public int getId() { return id; }
    public String getName() { return name; }
    public int getPort() { return port; }
    public int getSuccessorPort() { return successorPort; }
    public Set<String> getFiles() { return files; }
    
    
    public boolean hasFile(String fileName) {
        return files.contains(fileName);
    }

    @Override
    public String toString() {
        return String.format("%s [Porta: %d, Sucessor: %d, Arquivos: %d-%d]",
                name, port, successorPort, (id * 10) + 1, (id * 10) + 10);
    }
}