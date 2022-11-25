package com.gist.guild.gui.binding;

import org.springframework.cloud.stream.annotation.Input;
import org.springframework.cloud.stream.annotation.Output;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;

public interface MQBinding {
    @Input("distributionChannel")
    SubscribableChannel distributionChannel();
}
