package org.uniupo.it.dao;

import org.uniupo.it.model.DrinkAvailabilityResult;
import org.uniupo.it.model.Fault;

import java.util.List;

public interface DrinkDao {
    DrinkAvailabilityResult checkDrinkAvailability(String drinkCode, int sugarQuantity);
    void dispenseDrink(String drinkCode, int sugarQuantity);
    List<Fault> checkConsumablesAfterDispense();
    void insertMissingConsumables(List<Fault> faults);
    List<Fault> getUnresolvedConsumables();
}
