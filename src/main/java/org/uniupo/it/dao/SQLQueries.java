package org.uniupo.it.dao;

public final class SQLQueries {
    private SQLQueries() {
    }

    public static String getSchemaName(String instituteId, String machineId) {
        return String.format("machine_%s_%s",
                instituteId.toLowerCase().replace("-", "_"),
                machineId.toLowerCase().replace("-", "_"));
    }


    public static final class Recipe {
        private static final String SELECT_CONSUMABLES = """
                 SELECT r."consumableName", r."consumableQuantity", c.quantity\s
                 FROM %s."Recipe" r\s
                 JOIN %s."consumable" c ON r."consumableName" = c.name\s
                 WHERE r."drinkCode" = ?
                \s""";

        public static String getSelectConsumables(String instituteId, String machineId) {
            String schemaName = getSchemaName(instituteId, machineId);
            return String.format(SELECT_CONSUMABLES, schemaName, schemaName);
        }

        private static final String CHECK_BASE_CONSUMABLES = """
                 SELECT name, quantity\s
                 FROM %s.consumable\s
                 WHERE name IN ('CUP', 'SPOON')
                \s""";

        public static String getCheckBaseConsumables(String instituteId, String machineId) {
            return String.format(CHECK_BASE_CONSUMABLES, getSchemaName(instituteId, machineId));
        }


        private static final String GET_RECIPE_CONSUMABLES = """
            SELECT "consumableName", "consumableQuantity"
            FROM %s."Recipe"
            WHERE "drinkCode" = ?
        """;

        public static String getGetRecipeConsumables(String instituteId, String machineId) {
            return String.format(GET_RECIPE_CONSUMABLES, getSchemaName(instituteId, machineId));
        }
    }

    public static final class Consumable {
        private static final String CHECK_SUGAR = """
                 SELECT quantity\s
                 FROM %s.consumable\s
                 WHERE name = 'SUGAR'
                \s""";

        public static String getCheckSugar(String instituteId, String machineId) {
            return String.format(CHECK_SUGAR, getSchemaName(instituteId, machineId));
        }


        private static final String UPDATE_CONSUMABLE = """
            UPDATE %s.consumable
            SET quantity = quantity - ?
            WHERE name = ?
        """;

        public static String getUpdateConsumable(String instituteId, String machineId) {
            return String.format(UPDATE_CONSUMABLE, getSchemaName(instituteId, machineId));
        }

        private static final String CHECK_CONSUMABLES = """
                SELECT name, quantity, "maxQuantity"
                FROM %s.consumable
                WHERE quantity = 0;""";

        public static String getCheckConsumables(String instituteId, String machineId) {
            return String.format(CHECK_CONSUMABLES, getSchemaName(instituteId, machineId));
        }

        private static final String INSERT_FAULTS = """
                INSERT INTO %s."Fault" (description, id_fault, timestamp, fault_type)\s
                VALUES (?, ?, ?, ?)""";

        public static String getInsertFaults(String instituteId, String machineId) {
            return String.format(INSERT_FAULTS, getSchemaName(instituteId, machineId));
        }


        private static final String GET_FAULTS = """
                SELECT description, id_fault, timestamp, fault_type, risolto
                FROM %s."Fault" WHERE risolto IS FALSE;""";

        public static String getGetFaults(String instituteId, String machineId) {
            return String.format(GET_FAULTS, getSchemaName(instituteId, machineId));
        }

        private static final String GET_CONSUMABLES = """
                SELECT name, quantity, "maxQuantity"
                FROM %s.consumable;""";

        public static String getGetConsumables(String instituteId, String machineId) {
            return String.format(GET_CONSUMABLES, getSchemaName(instituteId, machineId));
        }
    }

}
