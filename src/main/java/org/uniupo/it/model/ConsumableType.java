package org.uniupo.it.model;

public enum ConsumableType {
    MILK,       // Latte per bevande come macchiato
    CHOCOLATE,  // Cioccolata per bevande al cioccolato
    SUGAR,      // Zucchero per dolcificare le bevande
    CUP,        // Bicchiere necessario per ogni erogazione
    SPOON,      // Cucchiaino necessario per mescolare
    TEA,        // Tè
    COFFEE;     // Caffè

    public String toDatabaseValue() {
        return this.name();
    }
}