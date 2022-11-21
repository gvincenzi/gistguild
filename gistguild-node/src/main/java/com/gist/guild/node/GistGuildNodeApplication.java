package com.gist.guild.node;

import com.gist.guild.node.binding.MQBinding;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

//@EnableEurekaClient
@EnableBinding(MQBinding.class)
@EnableMongoRepositories
@EnableFeignClients
@SpringBootApplication
public class GistGuildNodeApplication {
    public static void main(String[] args) {
        SpringApplication.run(GistGuildNodeApplication.class, args);
    }
}
