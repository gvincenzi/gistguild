package com.gist.guild.commons.message.entity;

import lombok.Data;

@Data
public class Order extends Document {
    private Long amount;
    private Long quantity;
    private String address;
    private String productName;
    private String customerMail;
    private String customerTelegramUserId;
}
