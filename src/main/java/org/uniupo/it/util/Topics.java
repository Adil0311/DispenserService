package org.uniupo.it.util;

public class Topics {
    private final static String BASE_TOPIC = "istituto/%s/macchina/%s";
    public static final String DISPLAY_TOPIC_UPDATE = BASE_TOPIC+"/frontend/screen/update";
    public static final String RESPONSE_CONSUMABLES_AVAILABILITY_TOPIC = BASE_TOPIC+"/transaction/consumableAvailabilityResponse";
    public static final String CONSUMABLES_AVAILABILITY_TOPIC = BASE_TOPIC+"/dispenser/consumableAvailability";
    public static final String DISPENSE_TOPIC = BASE_TOPIC+"/dispenser/dispense";
    public static final String DISPENSE_COMPLETED_TOPIC = BASE_TOPIC+"/dispenser/dispenseCompleted";

    public static final String ASSISTANCE_CHECK_CONSUMABLES_TOPIC = "management/faults/newFault";

    public static final String CONSUMABLES_REQUEST_TOPIC = "machines/%s/%s/consumables/request";
    public static final String CONSUMABLES_RESPONSE_TOPIC = "machines/%s/%s/consumables/response";
}
