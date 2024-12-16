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
    final private String baseTopic;
    final private Gson gson;

    public DispenserService(String machineId, MqttClient mqttClient) throws MqttException {
        this.machineId = machineId;
        this.mqttClient = mqttClient;
        this.baseTopic = "macchina/" + machineId + "/dispenser";
        this.gson = new Gson();
        this.mqttClient.subscribe(baseTopic + "/consumableAvailability", this::consumableAvailabilityHandler);

    }

    private void consumableAvailabilityHandler(String topic, MqttMessage mqttMessage) {
        String jsonMessage = new String(mqttMessage.getPayload());
        Selection s = gson.fromJson(jsonMessage, Selection.class);
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


}
