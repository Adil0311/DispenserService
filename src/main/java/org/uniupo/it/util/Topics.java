package org.uniupo.it.util;

public class Topics {
    public static final String RESPONSE_CONSUMABLES_AVAILABILITY_TOPIC = "macchina/%s/transaction/consumableAvailabilityResponse";
    public static final String CONSUMABLES_AVAILABILITY_TOPIC = "macchina/%s/dispenser/consumableAvailability";
    public static final String DISPENSE_TOPIC = "macchina/%s/dispenser/dispense";
    public static final String DISPENSE_COMPLETED_TOPIC = "macchina/%s/dispenser/dispenseCompleted";
    public static final String ASSISTANCE_CHECK_FAULTS_TOPIC = "management/faults/response";
}
