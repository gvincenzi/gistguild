package com.gist.guild.commons.message.entity;

import lombok.Data;

@Data
public class Participant extends Document {
    private String mail;
    private Long telegramUserId;
    private Boolean active = Boolean.TRUE;
    private Boolean administrator = Boolean.FALSE;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Participant)) return false;

        Participant that = (Participant) o;

        return getMail().equals(that.getMail());
    }

    @Override
    public int hashCode() {
        return getMail().hashCode();
    }
}
