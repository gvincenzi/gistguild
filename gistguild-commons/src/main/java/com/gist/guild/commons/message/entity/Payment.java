package com.gist.guild.commons.message.entity;

import lombok.Data;

@Data
public class Payment extends Document {
    private Long amount;
    private String customerMail;
    private Long customerTelegramUserId;
    private String orderId;
}
