package com.gist.guild.commons.message.entity;

import lombok.Data;

@Data
public class RechargeCredit extends Document {
    private Long oldCredit;
    private Long newCredit;
    private RechargeCreditType rechargeUserCreditType;
    private String customerMail;
    private Long customerTelegramUserId;
}
