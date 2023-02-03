package com.gist.guild.node;

import com.gist.guild.node.binding.MQBinding;
import com.gist.guild.node.core.configuration.MessageProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

//@EnableEurekaClient
@EnableBinding(MQBinding.class)
@EnableMongoRepositories
@EnableFeignClients
@EnableConfigurationProperties(MessageProperties.class)
@EnableAsync
@SpringBootApplication
public class GistGuildNodeApplication {
    public static void main(String[] args) {
        SpringApplication.run(GistGuildNodeApplication.class, args);
    }
}
