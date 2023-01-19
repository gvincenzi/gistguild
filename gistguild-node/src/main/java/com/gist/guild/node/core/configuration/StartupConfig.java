package com.gist.guild.node.core.configuration;

import com.gist.guild.node.spike.client.SpikeClient;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;

@Log
@EnableScheduling
@Configuration
public class StartupConfig {
    public static volatile Boolean startupParticipantProcessed = Boolean.FALSE;
    public static volatile Boolean startupProductProcessed = Boolean.FALSE;
    public static volatile Boolean startupOrderProcessed = Boolean.FALSE;
    public static volatile Boolean startupRechargeCreditProcessed = Boolean.FALSE;
    public static volatile Boolean startupPaymentProcessed = Boolean.FALSE;

    @Autowired
    ApplicationContext applicationContext;

    @Autowired
    SpikeClient spikeClient;

    @Value("${gistguild.startup}")
    private Boolean requiredStartup;

    @EventListener(ApplicationReadyEvent.class)
    void getStartup(ApplicationReadyEvent event) {
        if(event.getApplicationContext().equals(this.applicationContext) && requiredStartup){
            log.info("Startup node required");
            spikeClient.integrityVerification();
            log.info("Integrity verification request sent");
        } else if(event.getApplicationContext().equals(this.applicationContext) && !requiredStartup){
            log.info("Startup node not required");
            startupParticipantProcessed = Boolean.TRUE;
            startupProductProcessed = Boolean.TRUE;
            startupOrderProcessed = Boolean.TRUE;
            startupRechargeCreditProcessed = Boolean.TRUE;
            startupPaymentProcessed = Boolean.TRUE;
        }
    }

    public static Boolean getStartupProcessed(){
        return startupParticipantProcessed && startupProductProcessed && startupOrderProcessed && startupRechargeCreditProcessed && startupPaymentProcessed;
    }

    public static void reset(){
        if(getStartupProcessed()) {
            log.info("Startup node reset");
            startupParticipantProcessed = Boolean.FALSE;
            startupProductProcessed = Boolean.FALSE;
            startupOrderProcessed = Boolean.FALSE;
            startupRechargeCreditProcessed = Boolean.FALSE;
            startupPaymentProcessed = Boolean.FALSE;
        }
    }
}
