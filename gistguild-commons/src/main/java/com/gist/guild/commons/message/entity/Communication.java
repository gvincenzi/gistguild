package com.gist.guild.commons.message.entity;

import lombok.Data;

@Data
public class Communication extends Document {
    String message;
    Participant recipient;
}
