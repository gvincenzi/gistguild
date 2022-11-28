package com.gist.guild.commons.message.entity;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class Payment extends Document {
    private BigDecimal amount;
    private String customerMail;
    private Long customerTelegramUserId;
    private String orderId;
}
