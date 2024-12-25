package org.uniupo.it.dao;

import org.uniupo.it.model.DrinkAvailabilityResult;

public interface DrinkDao {
    DrinkAvailabilityResult checkDrinkAvailability(String drinkCode, int sugarQuantity);
    void dispenseDrink(String drinkCode, int sugarQuantity);
}
