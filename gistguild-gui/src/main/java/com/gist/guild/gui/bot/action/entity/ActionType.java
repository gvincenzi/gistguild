package com.gist.guild.gui.bot.action.entity;

public enum ActionType {
    BUY("Acquisto"),
    BUY_WITH_DELIVERY("Acquisto con consegna"),
    USER_SEARCH("Ricerca Utente"),
    SELECT_PRODUCT("Selezione prodotto"),
    SELECT_ADDRESS("Selezione indirizzo"),
    USER_MANAGEMENT("Gestione Utente"),
    USER_CREDIT("Ricarica credito di un utente"),
    PRODUCT_URL("Modifica URL del prodotto");

    private String label;

    ActionType(String label){
        this.label = label;
    }

    public String getLabel() {
        return this.label;
    }
}
