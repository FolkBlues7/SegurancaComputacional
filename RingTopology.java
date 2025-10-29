import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Classe de configuração que armazena o "mapa" da rede P2P.
 * Define as portas e responsabilidades de cada nó.
 */
public class RingTopology {

    public static final int BASE_PORT = 6000;
    public static final int TOTAL_NODES = 6;
    
    // Mapa estático para que todas as classes possam consultá-lo
    private static final Map<Integer, NodeInfo> ring = new ConcurrentHashMap<>();


    static {
        for (int i = 0; i < TOTAL_NODES; i++) {
            ring.put(i, new NodeInfo(i, BASE_PORT));
        }
    }


    public static NodeInfo getNodeInfo(int id) {
        if (id < 0 || id >= TOTAL_NODES) {
            throw new IllegalArgumentException("ID do nó deve estar entre 0 e " + (TOTAL_NODES - 1));
        }
        return ring.get(id);
    }

    //apenas teste
    public static void main(String[] args) {
        System.out.println("Topologia do Anel P2P (6 Nós):");
        for (int i = 0; i < TOTAL_NODES; i++) {
            System.out.println(RingTopology.getNodeInfo(i));
        }
    }
}