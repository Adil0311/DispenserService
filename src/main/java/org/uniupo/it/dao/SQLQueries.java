package org.uniupo.it.dao;

public final class SQLQueries {
    private SQLQueries() {
    }

    public static final class Recipe {
        public static final String SELECT_CONSUMABLES = """
                 SELECT r."consumableName", r."consumableQuantity", c.quantity\s
                 FROM machine."Recipe" r\s
                 JOIN machine.consumable c ON r."consumableName" = c.name\s
                 WHERE r."drinkCode" = ?
                \s""";

        public static final String CHECK_BASE_CONSUMABLES = """
                 SELECT name, quantity\s
                 FROM machine.consumable\s
                 WHERE name IN ('CUP', 'SPOON')
                \s""";

        public static final String GET_RECIPE_CONSUMABLES = """
            SELECT "consumableName", "consumableQuantity"
            FROM machine."Recipe"
            WHERE "drinkCode" = ?
        """;
    }

    public static final class Consumable {
        public static final String CHECK_SUGAR = """
                 SELECT quantity\s
                 FROM machine.consumable\s
                 WHERE name = 'SUGAR'
                \s""";
        public static final String UPDATE_CONSUMABLE = """
            UPDATE machine.consumable
            SET quantity = quantity - ?
            WHERE name = ?
        """;

        public static final String CHECK_CONSUMABLES = """
                SELECT name, quantity, "maxQuantity"
                FROM machine.consumable
                WHERE quantity = 0;""";

        public static final String INSERT_FAULTS = """
                INSERT INTO machine."Fault" (description, id_fault, timestamp, fault_type)\s
                VALUES (?, ?, ?, ?)""";

        public static final String GET_FAULTS = """
                SELECT description, id_fault, timestamp, fault_type, risolto
                FROM machine."Fault" WHERE risolto IS FALSE;""";

        public static final String GET_CONSUMABLES = """
                SELECT name, quantity, "maxQuantity"
                FROM machine.consumable;""";
    }

}
