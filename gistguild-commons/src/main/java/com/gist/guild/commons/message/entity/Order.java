package com.gist.guild.commons.message.entity;

import lombok.Data;

import java.text.NumberFormat;

@Data
public class Order extends Document {
    private Long amount;
    private Long quantity;
    private String address;
    private String productName;
    private String productId;
    private String customerMail;
    private Long customerTelegramUserId;

    @Override
    public String toString() {
        return "\nID : " + externalShortId +
                (quantity!=null ? "\nQuantità : " + quantity : "" ) +
                (address!=null ? "\nIndirizzo di spedizione : " + address : "" ) +
                (amount!=null ? "\nImporto totale : " + NumberFormat.getCurrencyInstance().format(amount) : "" ) +
                "\n\n**** Dettagli del prodotto ****\n" + productName;
                //(paid ? (StringUtils.isNotEmpty(this.getProduct().getUrl()) ? "\n\nURL : " + this.getProduct().getUrl() : StringUtils.EMPTY) : StringUtils.EMPTY) +
                //(paid ? (StringUtils.isNotEmpty(this.getProduct().getPassword()) ? "\n\n**Password : " + this.getProduct().getPassword() : StringUtils.EMPTY) : "\n\n**Quest'ordine non è ancora stato pagato**");
    }
}
