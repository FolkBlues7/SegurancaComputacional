import java.io.Serializable;

/**
 * Classe de dados simples (POJO) que representa um provedor de serviço.
 * É o "cartão de visita" que o DirectoryServer armazena.
 * * Implementa Serializable para que possa ser empacotado
 * (indiretamente, dentro do SecureMessage) e enviado pela rede.
 */
public class ServiceProviderInfo implements Serializable {

    private static final long serialVersionUID = 2L;

    private final String serverName; // Ex: "ServiceAlpha"
    private final String address;    // Ex: "localhost"
    private final int port;          // Ex: 9001

    public ServiceProviderInfo(String serverName, String address, int port) {
        this.serverName = serverName;
        this.address = address;
        this.port = port;
    }

    public String getServerName() {
        return serverName;
    }

    public String getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    @Override
    public String toString() {
        // Formato da resposta para o cliente
        return serverName + ":" + address + ":" + port;
    }

    @Override
    public boolean equals(Object obj) {
        // Necessário para o CopyOnWriteArrayList.remove() funcionar
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ServiceProviderInfo that = (ServiceProviderInfo) obj;
        return port == that.port &&
               serverName.equals(that.serverName) &&
               address.equals(that.address);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(serverName, address, port);
    }
}