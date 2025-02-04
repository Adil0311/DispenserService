package org.uniupo.it.service;

import com.google.gson.Gson;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.uniupo.it.dao.DrinkDaoImpl;
import org.uniupo.it.model.*;
import org.uniupo.it.util.Topics;

import java.awt.*;
import java.util.List;

public class DispenserService {
    final private String machineId;
    final private MqttClient mqttClient;
    final private String instituteId;
    final private Gson gson;

    public DispenserService(String machineId, String instituteId, MqttClient mqttClient) throws MqttException {
        this.machineId = machineId;
        this.mqttClient = mqttClient;
        this.instituteId = instituteId;
        this.gson = new Gson();
        subscribeToTopics();

    }

    private void subscribeToTopics() throws MqttException {
        mqttClient.subscribe(String.format(Topics.CONSUMABLES_AVAILABILITY_TOPIC,instituteId, machineId), this::consumableAvailabilityHandler);
        mqttClient.subscribe(String.format(Topics.DISPENSE_TOPIC,instituteId, machineId), this::startDispenseHandler);
        mqttClient.subscribe(String.format((Topics.CONSUMABLES_REQUEST_TOPIC), instituteId, machineId), this::consumableRequestHandler);
        mqttClient.subscribe(String.format(Topics.KILL_SERVICE_TOPIC, instituteId, machineId), this::killServiceHandler);
    }

    private void killServiceHandler(String topic, MqttMessage message) {
        System.out.println("Service killed hello darkness my old friend :(");
        new Thread(()->{
            try {
                Thread.sleep(1000);
                if(mqttClient.isConnected()) {
                    mqttClient.disconnect();
                }
                mqttClient.close();
                System.exit(0);
            } catch (Exception e) {
                System.err.println("Error during shutdown: "+e.getMessage());
                Runtime.getRuntime().halt(1);
            }
        }).start();
    }

    private void consumableRequestHandler(String s, MqttMessage message) {
        System.out.println("Consumable request handler");
        DrinkDaoImpl drinkDao = new DrinkDaoImpl();
        List<Consumable> consumables = drinkDao.getConsumables();
        System.out.println(consumables.toString());
        try {
            String consumablesJson = gson.toJson(consumables);
            mqttClient.publish(String.format(Topics.CONSUMABLES_RESPONSE_TOPIC, instituteId, machineId), new MqttMessage(consumablesJson.getBytes()));
        } catch (MqttException e) {
            throw new RuntimeException(e);
        }

    }

    private void startDispenseHandler(String topic, MqttMessage mqttMessage) {
        System.out.println("Start dispense handler");
        try {
            Selection s = gson.fromJson(new String(mqttMessage.getPayload()), Selection.class);
            DrinkDaoImpl drinkDao = new DrinkDaoImpl();

            publishDisplayMessage("Preparazione bevanda in corso...");
            Thread.sleep(1000);
            for(int i = 0; i <= 5; i++) {
                StringBuilder progressBar = new StringBuilder("[");
                for(int j = 0; j < 5; j++) {
                    if(j < i) progressBar.append("■");
                    else progressBar.append("□");
                }
                progressBar.append("]");
                publishDisplayMessage(progressBar.toString());
                Thread.sleep(1000);
            }

            drinkDao.dispenseDrink(s.getDrinkCode(), s.getSugarLevel());
            checkConsumablesAfterDispense();
            publishDisplayMessage("Erogazione completata!");

            String dispensedSelectionJson = gson.toJson(new Selection(s.getDrinkCode(), s.getSugarLevel()));
            mqttClient.publish(String.format(Topics.DISPENSE_COMPLETED_TOPIC, instituteId, machineId),
                    new MqttMessage(dispensedSelectionJson.getBytes()));

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void publishDisplayMessage(String message) {
        try {
            DisplayMessageFormat displayMessage = new DisplayMessageFormat(false, message);
            String messageJson = gson.toJson(displayMessage);
            mqttClient.publish(
                    String.format(Topics.DISPLAY_TOPIC_UPDATE, instituteId, machineId),
                    new MqttMessage(messageJson.getBytes())
            );
        } catch (MqttException e) {
            System.err.println("Error publishing display message: " + e.getMessage());
        }
    }

    public void checkConsumablesAfterDispense() {
        try {
            DrinkDaoImpl drinkDao = new DrinkDaoImpl();
            List<Fault> newFaults = drinkDao.checkConsumablesAfterDispense();
            List<Fault> unresolvedFaults = drinkDao.getUnresolvedConsumables();

            if (!unresolvedFaults.isEmpty()) {
                for (Fault existingFault : unresolvedFaults) {
                    if (existingFault.getFaultType() == FaultType.CONSUMABILE_TERMINATO) {
                        newFaults.removeIf(newFault -> newFault.getFaultType() == FaultType.CONSUMABILE_TERMINATO && newFault.getDescription().equals(existingFault.getDescription()));
                    }
                }
            }
            if (!newFaults.isEmpty()) {
                System.out.println("New faults found");
                publishFaults(getFaultMessages(newFaults));
                drinkDao.insertMissingConsumables(newFaults);
            }
        } catch (MqttException e) {
            throw new RuntimeException(e);
        }
    }


    private void publishFaults(List<FaultMessage> faults) throws MqttException {
        String jsonMessage = gson.toJson(faults);
        MqttMessage mqttMessage = new MqttMessage(jsonMessage.getBytes());
        mqttMessage.setQos(1);
        System.out.println("Sent faults: "+jsonMessage+" on "+Topics.ASSISTANCE_CHECK_CONSUMABLES_TOPIC);
        mqttClient.publish(Topics.ASSISTANCE_CHECK_CONSUMABLES_TOPIC, mqttMessage);
    }

    private List<FaultMessage> getFaultMessages(List<Fault> faults) {
        return faults.stream().map(fault -> new FaultMessage(machineId, fault.getDescription(), Integer.parseInt(instituteId), fault.getTimestamp(), fault.getIdFault(), fault.getFaultType())).toList();

    }

    private void consumableAvailabilityHandler(String topic, MqttMessage mqttMessage) {
        System.out.println("Consumable availability handler");
        Selection s = gson.fromJson(new String(mqttMessage.getPayload()), Selection.class);
        DrinkDaoImpl drinkDao = new DrinkDaoImpl();
        System.out.println(s.toString());
        DrinkAvailabilityResult drinkAvailabilityResult = drinkDao.checkDrinkAvailability(s.getDrinkCode(), s.getSugarLevel());
        System.out.println(drinkAvailabilityResult.toString());
        try {

            mqttClient.publish(String.format(Topics.RESPONSE_CONSUMABLES_AVAILABILITY_TOPIC,instituteId, machineId), new MqttMessage(gson.toJson(drinkAvailabilityResult).getBytes()));
        } catch (MqttException e) {
            throw new RuntimeException(e);
        }
    }



}
