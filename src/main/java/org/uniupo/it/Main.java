package org.uniupo.it;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.uniupo.it.mqttConfig.MqttOptions;
import org.uniupo.it.service.DispenserService;

import java.util.UUID;

public class Main {
    public static void main(String[] args) {

        if (args.length != 2) {
            System.out.println("Parametri non validi");
        }

        String mqttUrl = "ssl://localhost:8883";
        String instituteId = args[0];
        String machineId = args[1];


        try {
            MqttClient mqttClient = new MqttClient(mqttUrl, UUID.randomUUID() + " " + machineId);
            MqttConnectOptions mqttOptions = new MqttOptions().getOptions();
            mqttClient.connect(mqttOptions);
            new DispenserService(machineId, instituteId, mqttClient);
        } catch (Exception e) {
            e.printStackTrace();
        }


    }
}