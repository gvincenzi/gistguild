package com.gist.guild.commons.message.entity;

import lombok.Data;

@Data
public class Participant {
    String nickname;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Participant)) return false;

        Participant that = (Participant) o;

        return getNickname().equals(that.getNickname());
    }

    @Override
    public int hashCode() {
        return getNickname().hashCode();
    }
}
