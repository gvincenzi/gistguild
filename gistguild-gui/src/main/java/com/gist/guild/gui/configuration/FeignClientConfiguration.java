package com.gist.guild.gui.configuration;

import feign.Logger;
import feign.auth.BasicAuthRequestInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class FeignClientConfiguration {
    @Value("${gistguild.spike.username}")
    private String username;
    @Value("${gistguild.spike.password}")
    private String password;

    @Bean
    public BasicAuthRequestInterceptor basicAuthRequestInterceptor() {
        return new BasicAuthRequestInterceptor(username, password);
    }

    @Bean
    public Logger.Level configureLogLevel(){
        return  Logger.Level.BASIC;
    }
}
