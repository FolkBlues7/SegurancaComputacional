package entities;

import java.util.Scanner;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ClientApp implements Runnable {

    @Override
    public void run() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("\n=== APLICAÇÃO CLIENTE (Cidades Inteligentes) INICIADA ===");
        System.out.println("Aguardando dados chegarem à nuvem...");

        try { Thread.sleep(5000); } catch (InterruptedException e) {} // Espera inicial

        while (true) {
            System.out.println("\n--- MENU DE CONSULTA ---");
            System.out.println("1. Relatório Geral (Últimos 10 registos)");
            System.out.println("2. Alertas de Temperatura (> 35°C)");
            System.out.println("3. Alertas de Poluição (CO2 > 600)");
            System.out.println("4. Monitorizar Sensor Específico");
            System.out.println("5. Sair");
            System.out.print("Escolha uma opção: ");

            String input = scanner.nextLine();

            switch (input) {
                case "1":
                    printLastRecords();
                    break;
                case "2":
                    printHighTempAlerts();
                    break;
                case "3":
                    printPollutionAlerts();
                    break;
                case "4":
                    System.out.print("Digite o ID do Sensor (ex: Sensor01): ");
                    String id = scanner.nextLine();
                    printSensorHistory(id);
                    break;
                case "5":
                    System.out.println("Encerrando Cliente...");
                    return;
                default:
                    System.out.println("Opção inválida.");
            }
            
            // Pequena pausa para não misturar com logs do servidor no console
            try { Thread.sleep(1000); } catch (Exception e) {}
        }
    }

    private void printLastRecords() {
        System.out.println("\n--- ÚLTIMOS REGISTOS NA NUVEM ---");
        List<SensorData> db = CloudServer.DATABASE;
        if (db.isEmpty()) {
            System.out.println("(Nenhum dado disponível ainda)");
            return;
        }
        // Pega os últimos 10
        db.stream()
          .sorted(Comparator.comparingLong(SensorData::getTimestamp).reversed())
          .limit(10)
          .forEach(System.out::println);
    }

    private void printHighTempAlerts() {
        System.out.println("\n--- ALERTAS DE ALTA TEMPERATURA (> 35°C) ---");
        CloudServer.DATABASE.stream()
            .filter(d -> d.getTemperature() > 35.0)
            .forEach(d -> System.out.println("[ALERTA] " + d));
    }

    private void printPollutionAlerts() {
        System.out.println("\n--- ALERTAS DE POLUIÇÃO (CO2 > 600) ---");
        CloudServer.DATABASE.stream()
            .filter(d -> d.getCo2() > 600.0)
            .forEach(d -> System.out.println("[ALERTA] " + d));
    }

    private void printSensorHistory(String sensorId) {
        System.out.println("\n--- HISTÓRICO DO SENSOR: " + sensorId + " ---");
        List<SensorData> history = CloudServer.DATABASE.stream()
            .filter(d -> d.getSensorId().equalsIgnoreCase(sensorId))
            .collect(Collectors.toList());
            
        if (history.isEmpty()) {
            System.out.println("Nenhum dado encontrado para este ID.");
        } else {
            history.forEach(System.out::println);
        }
    }
}