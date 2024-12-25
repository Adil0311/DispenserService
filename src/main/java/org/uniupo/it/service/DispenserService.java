package org.uniupo.it.service;

import com.google.gson.Gson;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.uniupo.it.dao.DrinkDaoImpl;
import org.uniupo.it.model.DrinkAvailabilityResult;
import org.uniupo.it.model.Selection;
import org.uniupo.it.util.Topics;

public class DispenserService {
    final private String machineId;
    final private MqttClient mqttClient;
    final private Gson gson;

    public DispenserService(String machineId, MqttClient mqttClient) throws MqttException {
        this.machineId = machineId;
        this.mqttClient = mqttClient;
        this.gson = new Gson();
        subscribeToTopics();

    }

    private void subscribeToTopics() throws MqttException {
        mqttClient.subscribe(String.format(Topics.CONSUMABLES_AVAILABILITY_TOPIC, machineId), this::consumableAvailabilityHandler);
        mqttClient.subscribe(String.format(Topics.DISPENSE_TOPIC, machineId), this::startDispenseHandler);
    }

    private void startDispenseHandler(String topic, MqttMessage mqttMessage)  {
        System.out.println("Start dispense handler");
        Selection s = parseMessage(mqttMessage,Selection.class);
        DrinkDaoImpl drinkDao = new DrinkDaoImpl();
        System.out.println(s.toString());
        drinkDao.dispenseDrink(s.getDrinkCode(), s.getSugarLevel());
        System.out.println("Drink dispensed");
        try {
            mqttClient.publish(String.format(Topics.DISPENSE_COMPLETED_TOPIC, machineId), new MqttMessage("Dispense completed".getBytes()));
        } catch (MqttException e) {
            throw new RuntimeException(e);
        }


    }

    private void consumableAvailabilityHandler(String topic, MqttMessage mqttMessage) {
        System.out.println("Consumable availability handler");
        Selection s = parseMessage(mqttMessage,Selection.class);
        DrinkDaoImpl drinkDao = new DrinkDaoImpl();
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

    private <T> T parseMessage(MqttMessage message,Class<T> classType){
        String jsonMessage = new String(message.getPayload());
        return gson.fromJson(jsonMessage,classType);
    }


}
