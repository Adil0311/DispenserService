package org.uniupo.it.dao;

import org.uniupo.it.model.ConsumableType;
import org.uniupo.it.model.DrinkAvailabilityResult;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class DrinkDaoImpl implements DrinkDao {
    @Override
    public DrinkAvailabilityResult checkDrinkAvailability(String drinkCode, int sugarQuantity) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            DrinkAvailabilityResult baseConsumablesResult = checkBaseConsumables(conn);
            if (!baseConsumablesResult.isAvailable()) {
                return baseConsumablesResult;
            }

            DrinkAvailabilityResult sugarResult = checkSugar(conn, sugarQuantity);
            if (!sugarResult.isAvailable()) {
                return sugarResult;
            }

            return checkRecipeConsumables(conn, drinkCode);

        } catch (SQLException e) {
            throw new RuntimeException("Error checking drink availability", e);
        }
    }

    private DrinkAvailabilityResult checkBaseConsumables(Connection conn) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
                """
                SELECT name, quantity 
                FROM machine.consumable 
                WHERE name IN ('CUP', 'SPOON')
                """)) {

            ResultSet rs = stmt.executeQuery();
            Map<ConsumableType, Integer> baseConsumables = new HashMap<>();

            while (rs.next()) {
                ConsumableType consumableName = ConsumableType.valueOf(rs.getString("name"));
                int quantity = rs.getInt("quantity");
                baseConsumables.put(consumableName, quantity);
            }

            if (!baseConsumables.containsKey(ConsumableType.CUP) || baseConsumables.get(ConsumableType.CUP) < 1) {
                return new DrinkAvailabilityResult(false, ConsumableType.CUP, null);
            }

            if (!baseConsumables.containsKey(ConsumableType.SPOON) || baseConsumables.get(ConsumableType.SPOON) < 1) {
                return new DrinkAvailabilityResult(false, ConsumableType.SPOON, null);
            }

            System.out.println(baseConsumables);

            return new DrinkAvailabilityResult(true);
        }
    }

    private DrinkAvailabilityResult checkSugar(Connection conn, int sugarQuantity) throws SQLException {
        if (sugarQuantity == 0) {
            return new DrinkAvailabilityResult(true);
        }

        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT quantity FROM machine.consumable WHERE name = 'SUGAR'")) {

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                int availableSugar = rs.getInt("quantity");
                System.out.println(availableSugar);
                if (sugarQuantity > availableSugar) {
                    return new DrinkAvailabilityResult(false, ConsumableType.SUGAR, availableSugar);
                }
                return new DrinkAvailabilityResult(true);
            }
            return new DrinkAvailabilityResult(false, ConsumableType.SUGAR, 0);
        }
    }

    private DrinkAvailabilityResult checkRecipeConsumables(Connection conn, String drinkCode) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
                """
                SELECT r.consumableName, r.consumableQuantity, c.quantity 
                FROM machine."Recipe" r 
                JOIN machine.consumable c ON r.consumableName = c.name 
                WHERE r."drinkCode" = ?
                """)) {

            stmt.setString(1, drinkCode);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                ConsumableType consumableName = ConsumableType.valueOf(rs.getString("consumableName"));
                int requiredQuantity = rs.getInt("consumableQuantity");
                int availableQuantity = rs.getInt("quantity");
                System.out.println(consumableName + " " + requiredQuantity + " " + availableQuantity);

                if (availableQuantity < requiredQuantity) {
                    return new DrinkAvailabilityResult(false, consumableName, null);
                }
            }

            return new DrinkAvailabilityResult(true);
        }
    }
}