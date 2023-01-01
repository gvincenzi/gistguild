package com.gist.guild.commons.message.entity;

import lombok.Data;

@Data
public class Participant extends Document {
    private Long telegramUserId;
    private String nickname;
    private Boolean active = Boolean.TRUE;
    private Boolean administrator = Boolean.FALSE;
    private transient Long credit;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Participant)) return false;

        Participant that = (Participant) o;

        return getTelegramUserId().equals(that.getTelegramUserId());
    }

    @Override
    public int hashCode() {
        return getTelegramUserId().hashCode();
    }
}
