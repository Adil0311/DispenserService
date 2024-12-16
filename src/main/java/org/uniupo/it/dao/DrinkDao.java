package org.uniupo.it.dao;

import org.uniupo.it.model.DrinkAvailabilityResult;

public interface DrinkDao {
    public DrinkAvailabilityResult checkDrinkAvailability(String drinkCode, int sugarQuantity);
}
