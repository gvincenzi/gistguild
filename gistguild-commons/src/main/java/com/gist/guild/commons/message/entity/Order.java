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
    private String productPassword;
    private String customerNickname;
    private Long customerTelegramUserId;
    private Boolean deleted = Boolean.FALSE;
    private Boolean delivered = Boolean.FALSE;
    private transient Boolean paid;
}
