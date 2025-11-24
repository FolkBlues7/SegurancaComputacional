package entities;

import javax.crypto.SecretKey;
import utils.CryptoManager;
import utils.HybridMessage;
import utils.KeyRegistry;
import utils.SerializationUtils;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.security.PublicKey;
import java.util.Random;
import java.util.Scanner;

public class IoTDevice implements Runnable {

    private final String deviceId;
    private final String targetHost;
    private final int targetPort;
    private final boolean isAttacker;

    public IoTDevice(String deviceId, String targetHost, int targetPort, boolean isAttacker) {
        this.deviceId = deviceId;
        this.targetHost = targetHost;
        this.targetPort = targetPort;
        this.isAttacker = isAttacker;
    }

    @Override
    public void run() {
        try (DatagramSocket socket = new DatagramSocket()) {
            Random random = new Random();
            System.out.println("Sensor " + deviceId + " iniciado (Duração: 5 min).");
            if(isAttacker) System.out.println("!!! MODO ATACANTE ATIVADO !!!");

            long startTime = System.currentTimeMillis();
            long duration = 5 * 60 * 1000; // 5 Minutos

            while (System.currentTimeMillis() - startTime < duration) {
                // 1. Gerar Dados
                SensorData data = generateRandomData(random);

                // 2. Buscar chave do EDGE
                PublicKey edgePublicKey = KeyRegistry.getPublicKey("EDGE");
                if (edgePublicKey == null) {
                    System.err.println("Aguardando Edge Server ficar online...");
                    Thread.sleep(2000);
                    continue;
                }

                // 3. Criptografia
                SecretKey sessionKey = CryptoManager.generateAESKey();
                byte[] iv = CryptoManager.generateIv();

                byte[] dataBytes = SerializationUtils.serialize(data);
                // Truque para serialização simples
                String dataAsBase64 = java.util.Base64.getEncoder().encodeToString(dataBytes);
                
                byte[] encryptedPayload = CryptoManager.encryptAES(dataAsBase64, sessionKey, iv);
                
                // Se for atacante, poderíamos corromper algo aqui, 
                // mas vamos manter o fluxo enviando dados com credenciais válidas 
                // para simular injeção de dados falsos.
                
                byte[] encryptedSessionKey = CryptoManager.encryptRSA(sessionKey.getEncoded(), edgePublicKey);

                HybridMessage message = new HybridMessage(deviceId, encryptedSessionKey, iv, encryptedPayload);

                // 4. Envio
                byte[] msgBytes = SerializationUtils.serialize(message);
                DatagramPacket packet = new DatagramPacket(
                        msgBytes, msgBytes.length, InetAddress.getByName(targetHost), targetPort
                );
                socket.send(packet);

                System.out.println("[" + deviceId + "] Enviado: Temp=" + String.format("%.1f", data.getTemperature()) + " CO2=" + String.format("%.1f", data.getCo2()));

                Thread.sleep(2000 + random.nextInt(1000));
            }
            System.out.println(">>> Sensor " + deviceId + " finalizou.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private SensorData generateRandomData(Random r) {
        double temp = 20 + r.nextDouble() * 15; 
        double co2 = 300 + r.nextDouble() * 500;
        return new SensorData(deviceId, co2, 0, 0, 0, 0, 0, temp, 50, 5, 60);
    }

    // --- MÉTODO MAIN INTERATIVO ---
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        
        System.out.println("=== CONFIGURAÇÃO DO SENSOR ===");
        System.out.print("Digite o ID do Sensor (ex: Sensor01): ");
        String id = sc.nextLine();
        
        System.out.print("Este dispositivo é um Atacante? (s/n): ");
        String resp = sc.nextLine();
        boolean attacker = resp.equalsIgnoreCase("s");

        // Inicia o sensor
        IoTDevice device = new IoTDevice(id, "localhost", 9000, attacker);
        device.run();
        
        sc.close();
    }
}