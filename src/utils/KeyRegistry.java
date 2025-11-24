package utils;

import java.io.*;
import java.security.PublicKey;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simula um servidor de chaves públicas confiável.
 * Modificado para funcionar com MULTI-PROCESSOS usando arquivos.
 */
public class KeyRegistry {

    // Cache em memória
    private static final ConcurrentHashMap<String, PublicKey> publicKeys = new ConcurrentHashMap<>();

    /**
     * Registra a chave na memória E salva em arquivo para outros processos lerem.
     */
    public static void registerKey(String nodeId, PublicKey key) {
        // 1. Salva na memória local
        publicKeys.put(nodeId, key);
        
        // 2. Salva em arquivo físico (ex: "EDGE.key")
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(nodeId + ".key"))) {
            out.writeObject(key);
            System.out.println("[KeyRegistry] Chave pública salva em arquivo: " + nodeId + ".key");
        } catch (IOException e) {
            System.err.println("[KeyRegistry] Erro ao salvar chave em arquivo: " + e.getMessage());
        }
    }

    /**
     * Tenta pegar da memória. Se não achar, tenta ler do arquivo.
     */
    public static PublicKey getPublicKey(String nodeId) {
        // 1. Tenta memória
        if (publicKeys.containsKey(nodeId)) {
            return publicKeys.get(nodeId);
        }

        // 2. Tenta ler do arquivo (caso esteja em outro processo)
        File file = new File(nodeId + ".key");
        if (file.exists()) {
            try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
                PublicKey key = (PublicKey) in.readObject();
                // Atualiza cache local
                publicKeys.put(nodeId, key);
                return key;
            } catch (Exception e) {
                System.err.println("[KeyRegistry] Erro ao ler chave do arquivo: " + e.getMessage());
            }
        }
        
        // Retorna null se ainda não existe (o sensor vai continuar tentando)
        return null;
    }
}