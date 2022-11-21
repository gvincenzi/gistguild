package com.gist.guild.distribution;

import com.gist.guild.distribution.binding.MQBinding;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.stream.annotation.EnableBinding;

//@EnableEurekaClient
@EnableBinding(MQBinding.class)
@SpringBootApplication
public class GistGuildDistributionApplication {
    public static void main(String[] args) {
        SpringApplication.run(GistGuildDistributionApplication.class, args);
    }
}
