package com.gist.guild.node.core.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "gistguild.message")
public class MessageProperties {
    String error1;
    String error2;
    String error3;
    String error4;
    String error5;
    String error6;

    String adminPasswordMessage;
    String newOrderMessage;
    String newParticipantMessage;
    String adminMessage;
}
