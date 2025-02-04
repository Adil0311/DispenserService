package org.uniupo.it.dao;

import org.uniupo.it.model.*;

import java.sql.*;
import java.util.*;

public class DrinkDaoImpl implements DrinkDao {

    private final String instituteId;
    private final String machineId;

    public DrinkDaoImpl(String instituteId, String machineId) {
        this.instituteId = instituteId;
        this.machineId = machineId;
    }
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
            System.out.println("Error checking drink availability"+e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error checking drink availability", e);
        }
    }

    @Override
    public void dispenseDrink(String drinkCode, int sugarQuantity) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            System.out.println("Dispensing drink");
            updateBaseConsumables(conn);
            System.out.println("Base consumables updated");
            if (sugarQuantity > 0) {
                updateSugar(conn, sugarQuantity);
            }
            System.out.println("Sugar updated");
            updateRecipeConsumables(conn, drinkCode);
            System.out.println("Recipe consumables updated");
        } catch (SQLException e) {
            System.out.println("Error dispensing drink"+e.getMessage());
            throw new RuntimeException("Error dispensing drink", e);
        }

    }

    private void updateRecipeConsumables(Connection conn, String drinkCode) throws SQLException {
        System.out.println("Updating recipe consumables");
        try (PreparedStatement getRecipe = conn.prepareStatement(SQLQueries.Recipe.getGetRecipeConsumables(instituteId, machineId));
             PreparedStatement updateConsumable = conn.prepareStatement(SQLQueries.Consumable.getUpdateConsumable(instituteId, machineId))) {

            getRecipe.setString(1, drinkCode);
            ResultSet rs = getRecipe.executeQuery();

            while (rs.next()) {
                String consumableName = rs.getString("consumableName");
                int quantity = rs.getInt("consumableQuantity");

                updateConsumable.setInt(1, quantity);
                updateConsumable.setObject(2, ConsumableType.valueOf(consumableName), Types.OTHER);
                updateConsumable.executeUpdate();
            }
        }
    }

    private void updateSugar(Connection conn, int sugarQuantity) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(SQLQueries.Consumable.getUpdateConsumable(instituteId, machineId))) {
            stmt.setInt(1, sugarQuantity);
            stmt.setObject(2, ConsumableType.SUGAR, Types.OTHER);
            stmt.executeUpdate();
        }
    }

    private void updateBaseConsumables(Connection conn) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(SQLQueries.Consumable.getUpdateConsumable(instituteId, machineId))) {
            // Debug logs
            System.out.println("Starting base consumables update");

            // Aggiorna bicchiere
            stmt.setInt(1, 1);
            stmt.setObject(2, ConsumableType.CUP, Types.OTHER); // Usiamo setObject per l'ENUM
            System.out.println("Executing update for CUP");
            stmt.executeUpdate();

            // Aggiorna cucchiaio
            stmt.setInt(1, 1);
            stmt.setObject(2, ConsumableType.SPOON, Types.OTHER); // Usiamo setObject per l'ENUM
            System.out.println("Executing update for SPOON");
            stmt.executeUpdate();
        }
    }

    private DrinkAvailabilityResult checkBaseConsumables(Connection conn) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(SQLQueries.Recipe.getCheckBaseConsumables(instituteId, machineId))) {

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

        try (PreparedStatement stmt = conn.prepareStatement(SQLQueries.Consumable.getCheckSugar(instituteId, machineId))) {

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
        try (PreparedStatement stmt = conn.prepareStatement(SQLQueries.Recipe.getSelectConsumables(instituteId, machineId))) {

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

    @Override
    public List<Fault> checkConsumablesAfterDispense() {
        List<Fault> faults = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(SQLQueries.Consumable.getCheckConsumables(instituteId, machineId));
                 ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    faults.add(new Fault(
                            rs.getString("name")+ " is empty",
                            UUID.randomUUID(),
                            new java.sql.Timestamp(System.currentTimeMillis()),
                            FaultType.CONSUMABILE_TERMINATO
                    ));
                }
            }

            return faults;
        } catch (SQLException e) {
            System.out.println("Error during machine checkup"+e.getMessage());
            throw new RuntimeException("Error during machine checkup", e);
        }
    }

    @Override
    public void insertMissingConsumables(List<Fault> faults) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQLQueries.Consumable.getInsertFaults(instituteId, machineId))) {

            for (Fault fault : faults) {
                pstmt.setString(1, fault.getDescription());
                pstmt.setObject(2, fault.getIdFault());
                pstmt.setTimestamp(3, fault.getTimestamp());
                pstmt.setObject(4, fault.getFaultType().toString(), Types.OTHER);
                pstmt.addBatch();
            }

            pstmt.executeBatch();

        } catch (SQLException e) {
            System.out.println("Failed to insert faults"+e.getMessage());
            throw new RuntimeException("Failed to insert faults", e);
        }
    }

    @Override
    public List<Fault> getUnresolvedConsumables() {
        List<Fault> faults = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQLQueries.Consumable.getGetFaults(instituteId, machineId));
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                faults.add(new Fault(
                        rs.getString("description"),
                        rs.getObject("id_fault", UUID.class),
                        rs.getTimestamp("timestamp"),
                        FaultType.valueOf(rs.getString("fault_type")),
                        rs.getBoolean("risolto")
                ));
            }
        } catch (SQLException e) {
            System.out.println("Failed to fetch unresolved faults"+e.getMessage());
            throw new RuntimeException("Failed to fetch unresolved faults", e);
        }

        return faults;
    }

    @Override
    public List<Consumable> getConsumables() {
        List<Consumable> consumables = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQLQueries.Consumable.getGetConsumables(instituteId, machineId));
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                consumables.add(new Consumable(
                        ConsumableType.valueOf(rs.getString("name")),
                        rs.getInt("quantity"),
                        rs.getInt("maxQuantity")
                ));
            }
        } catch (SQLException e) {
            System.out.println("Failed to fetch consumables"+e.getMessage());
            throw new RuntimeException("Failed to fetch consumables", e);
        }

        return consumables;
    }
}