package com.gist.guild.distribution.domain.service.test;

import com.gist.guild.distribution.delivery.service.DistributionConcurrenceService;
import com.gist.guild.distribution.exception.DistributionException;
import lombok.extern.java.Log;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.UUID;

@Log
@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class DistributionConcurrenceServiceTest {
    @Autowired
    DistributionConcurrenceService distributionConcurrenceService;

    @Test
    public void waitingForLastCorrelationIDProcessingTest() throws DistributionException {
        UUID test = UUID.randomUUID();
        DistributionConcurrenceService.getCorrelationIDs().add(test);
        DistributionConcurrenceService.setLastBlockingCorrelationID(test);

        new Thread(){
            public void run(){
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                DistributionConcurrenceService.getCorrelationIDs().remove(test);
            }
        }.start();

        long timeInMillis = System.currentTimeMillis();
        distributionConcurrenceService.waitingForLastCorrelationIDProcessing();
        Assert.assertTrue("The waiting time is too long",System.currentTimeMillis()-timeInMillis < 4000);
    }
}
