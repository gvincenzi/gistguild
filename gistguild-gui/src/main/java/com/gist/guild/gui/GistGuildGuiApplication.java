package com.gist.guild.gui;

import com.gist.guild.gui.binding.MQBinding;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EnableBinding(MQBinding.class)
@EnableFeignClients
@EnableJpaRepositories
@SpringBootApplication
public class GistGuildGuiApplication {
    public static void main(String[] args) {
        SpringApplication.run(GistGuildGuiApplication.class, args);
    }
}
