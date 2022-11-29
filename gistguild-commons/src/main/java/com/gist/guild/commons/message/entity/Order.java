package com.gist.guild.commons.message.entity;

import lombok.Data;

@Data
public class Order extends Document {
    private Long amount;
    private Long quantity;
    private String address;
    private String productName;
    private String productId;
    private String productUrl;
    private String customerMail;
    private Long customerTelegramUserId;
    private transient Boolean paid;

    @Override
    public String toString() {
        return "\nID : " + externalShortId +
                (quantity!=null ? "\nQuantità : " + quantity : "" ) +
                (address!=null ? "\nIndirizzo di spedizione : " + address : "" ) +
                (amount!=null ? String.format("\nImporto totale : %s €",amount) : "" ) +
                "\n\n**** Dettagli del prodotto ****\n" + productName;
    }
}
