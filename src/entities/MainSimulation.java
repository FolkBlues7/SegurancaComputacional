package entities;

public class MainSimulation {

    public static void main(String[] args) {
        System.out.println(">>> INICIANDO SIMULAÇÃO DE CIDADES INTELIGENTES <<<");
        
        // 1. Iniciar CLOUD (Datacenter)
        new Thread(() -> {
            try {
                new CloudServer().start();
            } catch (Exception e) { e.printStackTrace(); }
        }).start();

        // Pausa para garantir que Cloud subiu
        sleep(1000);

        // 2. Iniciar EDGE (Servidor de Borda)
        new Thread(() -> {
            try {
                new EdgeServer().start();
            } catch (Exception e) { e.printStackTrace(); }
        }).start();

        // Pausa para garantir que Edge subiu e registou as chaves
        sleep(1000);

        System.out.println(">>> SERVIDORES PRONTOS. INICIANDO SENSORES... <<<");

        // 3. Iniciar 4 Sensores Legítimos
        for (int i = 1; i <= 4; i++) {
            String sensorName = "Sensor0" + i;
            // Conectam ao localhost:9000 (Edge UDP)
            IoTDevice sensor = new IoTDevice(sensorName, "localhost", 9000, false);
            new Thread(sensor).start();
        }

        // 4. Iniciar 1 Sensor Atacante (Tentando injetar dados)
        IoTDevice attacker = new IoTDevice("ATACANTE_X", "localhost", 9000, true);
        new Thread(attacker).start();

        // 5. Iniciar Aplicação Cliente (Interface do Utilizador)
        // Roda na thread principal ou separada
        new Thread(new ClientApp()).start();
    }

    private static void sleep(int millis) {
        try { Thread.sleep(millis); } catch (Exception e) {}
    }
}