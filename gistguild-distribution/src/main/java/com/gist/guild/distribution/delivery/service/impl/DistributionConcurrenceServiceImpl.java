package com.gist.guild.distribution.delivery.service.impl;

import com.gist.guild.distribution.delivery.service.DistributionConcurrenceService;
import com.gist.guild.distribution.exception.DistributionException;
import lombok.extern.java.Log;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class DistributionConcurrenceServiceImpl extends DistributionConcurrenceService {
    @Override
    public synchronized void waitingForLastCorrelationIDProcessing() throws DistributionException {
        int numberOfTry = 0;
        while(getCorrelationIDs().contains(getLastBlockingCorrelationID())){
            log.info(String.format("Waiting last blocking correlationID process end - CorrelationID [%s]",getLastBlockingCorrelationID().toString()));
            if(numberOfTry == 3){
                throw new DistributionException(String.format("Timeout while last blocking correlationID process end waiting  - CorrelationID [%s]",getLastBlockingCorrelationID().toString()));
            }
            try {
                numberOfTry++;
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                log.error(e.getMessage());
            }
        }
        log.info("No blocking correlationID in progress");
    }
}
