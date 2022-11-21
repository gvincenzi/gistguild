package com.gist.guild.node.core.configuration;

import com.gist.guild.node.spike.client.SpikeClient;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

@Log
@Configuration
public class StartupConfig {
    public static volatile Boolean startupProcessed = Boolean.FALSE;

    @Autowired
    SpikeClient spikeClient;

    @Value("${gistguild.startup}")
    private Boolean requiredStartup;

    @EventListener(ApplicationReadyEvent.class)
    void getStartup() {
        if(requiredStartup){
            log.info("Startup node required");
            spikeClient.integrityVerification();
            log.info("Integrity verification request sent");
        } else {
            log.info("Startup node not required");
            startupProcessed = Boolean.TRUE;
        }
    }
}
