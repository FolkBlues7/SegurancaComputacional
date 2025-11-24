package entities;

import java.io.Serializable;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Representa os dados ambientais coletados por uma Estação Inteligente.
 * Contém gases, partículas, clima e ruído.
 */
public class SensorData implements Serializable {

    private static final long serialVersionUID = 1L;

    // Identificação e Tempo
    private final String sensorId;
    private final long timestamp;

    // Gases (ppm ou ppb)
    private final double co2;
    private final double co;
    private final double no2;
    private final double so2;

    // Partículas (µg/m³)
    private final double pm25;
    private final double pm10;

    // Clima
    private final double temperature; // Celsius
    private final double humidity;    // %
    private final double uvIndex;     // Índice UV

    // Som
    private final double noiseLevel;  // dB

    public SensorData(String sensorId, double co2, double co, double no2, double so2,
                      double pm25, double pm10, double temperature, double humidity,
                      double uvIndex, double noiseLevel) {
        this.sensorId = sensorId;
        this.timestamp = System.currentTimeMillis(); // Marca a hora da criação
        this.co2 = co2;
        this.co = co;
        this.no2 = no2;
        this.so2 = so2;
        this.pm25 = pm25;
        this.pm10 = pm10;
        this.temperature = temperature;
        this.humidity = humidity;
        this.uvIndex = uvIndex;
        this.noiseLevel = noiseLevel;
    }

    // Getters principais (usados para análise de alertas no Edge/Cloud)
    public String getSensorId() { return sensorId; }
    public long getTimestamp() { return timestamp; }
    public double getCo2() { return co2; }
    public double getTemperature() { return temperature; }
    public double getHumidity() { return humidity; }
    public double getNoiseLevel() { return noiseLevel; }

    @Override
    public String toString() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
                                        .withZone(ZoneId.systemDefault());
        String time = formatter.format(Instant.ofEpochMilli(timestamp));

        return String.format(
            "[%s | ID: %s] Temp: %.1f°C, Umid: %.1f%%, CO2: %.1f, Ruído: %.1fdB, UV: %.1f",
            time, sensorId, temperature, humidity, co2, noiseLevel, uvIndex
        );
    }
    
    // Método para relatório completo
    public String toFullReport() {
        return String.format(
            "--- Relatório do Sensor %s ---\n" +
            "Tempo: %s\n" +
            "Gases: CO2=%.2f, CO=%.2f, NO2=%.2f, SO2=%.2f\n" +
            "Partículas: PM2.5=%.2f, PM10=%.2f\n" +
            "Clima: Temp=%.1f°C, Umid=%.1f%%, UV=%.1f\n" +
            "Som: %.1f dB\n" +
            "--------------------------------",
            sensorId, Instant.ofEpochMilli(timestamp),
            co2, co, no2, so2, pm25, pm10, temperature, humidity, uvIndex, noiseLevel
        );
    }
}