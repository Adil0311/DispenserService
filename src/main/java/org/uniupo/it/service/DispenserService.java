package org.uniupo.it.service;

import com.google.gson.Gson;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.uniupo.it.dao.DrinkDaoImpl;
import org.uniupo.it.model.*;
import org.uniupo.it.util.Topics;

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
        mqttClient.subscribe(String.format(Topics.CONSUMABLES_AVAILABILITY_TOPIC, machineId), this::consumableAvailabilityHandler);
        mqttClient.subscribe(String.format(Topics.DISPENSE_TOPIC, machineId), this::startDispenseHandler);
    }

    private void startDispenseHandler(String topic, MqttMessage mqttMessage) {
        System.out.println("Start dispense handler");
        Selection s = parseMessage(mqttMessage, Selection.class);
        DrinkDaoImpl drinkDao = new DrinkDaoImpl();
        System.out.println(s.toString());
        drinkDao.dispenseDrink(s.getDrinkCode(), s.getSugarLevel());
        System.out.println("Drink dispensed");
        checkConsumablesAfterDispense();
        Selection dispensedSelection = new Selection(s.getDrinkCode(), s.getSugarLevel());
        String dispensedSelectionJson = gson.toJson(dispensedSelection);
        try {
            mqttClient.publish(String.format(Topics.DISPENSE_COMPLETED_TOPIC, machineId), new MqttMessage(dispensedSelectionJson.getBytes()));
        } catch (MqttException e) {
            throw new RuntimeException(e);
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
        System.out.println(jsonMessage);
        MqttMessage mqttMessage = new MqttMessage(jsonMessage.getBytes());
        mqttMessage.setQos(1);
        mqttClient.publish(Topics.ASSISTANCE_CHECK_FAULTS_TOPIC, mqttMessage);
    }

    private List<FaultMessage> getFaultMessages(List<Fault> faults) {
        return faults.stream().map(fault -> new FaultMessage(machineId, fault.getDescription(), Integer.parseInt(instituteId), fault.getTimestamp(), fault.getIdFault(), fault.getFaultType())).toList();

    }

    private void consumableAvailabilityHandler(String topic, MqttMessage mqttMessage) {
        System.out.println("Consumable availability handler");
        Selection s = parseMessage(mqttMessage, Selection.class);
        DrinkDaoImpl drinkDao = new DrinkDaoImpl();
        System.out.println(s.toString());
        DrinkAvailabilityResult drinkAvailabilityResult = drinkDao.checkDrinkAvailability(s.getDrinkCode(), s.getSugarLevel());
        System.out.println(drinkAvailabilityResult);
        try {
            String str = String.format(Topics.RESPONSE_CONSUMABLES_AVAILABILITY_TOPIC, machineId);
            System.out.println(str);
            mqttClient.publish(str, new MqttMessage(gson.toJson(drinkAvailabilityResult).getBytes()));
        } catch (MqttException e) {
            throw new RuntimeException(e);
        }
    }

    private <T> T parseMessage(MqttMessage message, Class<T> classType) {
        String jsonMessage = new String(message.getPayload());
        return gson.fromJson(jsonMessage, classType);
    }


}
