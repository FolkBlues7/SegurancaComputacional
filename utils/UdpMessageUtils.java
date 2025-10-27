import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Classe utilitária para serializar e desserializar
 * o objeto SecureMessage para ser enviado via DatagramPacket (UDP).
 */
public class UdpMessageUtils {

    /**
     * Converte um objeto (SecureMessage) em um array de bytes.
     */
    public static byte[] serialize(SecureMessage message) throws IOException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        ObjectOutputStream objStream = new ObjectOutputStream(byteStream);
        objStream.writeObject(message);
        objStream.flush();
        return byteStream.toByteArray();
    }

    /**
     * Converte um array de bytes (de um DatagramPacket) de volta
     * para um objeto SecureMessage.
     */
    public static SecureMessage deserialize(byte[] data) throws IOException, ClassNotFoundException {
        ByteArrayInputStream byteStream = new ByteArrayInputStream(data);
        ObjectInputStream objStream = new ObjectInputStream(byteStream);
        return (SecureMessage) objStream.readObject();
    }
}