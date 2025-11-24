package utils;

import java.io.*;

/**
 * Utilitário para converter Objetos Java em arrays de bytes e vice-versa.
 * Essencial para preparar o SensorData para a criptografia AES.
 */
public class SerializationUtils {

    /**
     * Serializa um objeto (ex: SensorData) para byte[]
     */
    public static byte[] serialize(Object object) throws IOException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        ObjectOutputStream objectStream = new ObjectOutputStream(byteStream);
        objectStream.writeObject(object);
        objectStream.flush();
        return byteStream.toByteArray();
    }

    /**
     * Desserializa um byte[] de volta para Objeto (ex: SensorData)
     */
    public static Object deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
        ByteArrayInputStream byteStream = new ByteArrayInputStream(bytes);
        ObjectInputStream objectStream = new ObjectInputStream(byteStream);
        return objectStream.readObject();
    }
}
