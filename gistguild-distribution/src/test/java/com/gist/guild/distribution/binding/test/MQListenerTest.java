package com.gist.guild.distribution.binding.test;

import com.gist.guild.commons.message.DistributionEventType;
import com.gist.guild.commons.message.DistributionMessage;
import com.gist.guild.commons.message.entity.Document;
import com.gist.guild.commons.message.entity.Participant;
import com.gist.guild.commons.message.entity.GistGuildItem;
import com.gist.guild.distribution.binding.MQListener;
import com.gist.guild.distribution.delivery.service.DistributionConcurrenceService;
import com.gist.guild.distribution.spike.controller.ControllerResponseCache;
import lombok.extern.java.Log;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Log
@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class MQListenerTest {
    @Autowired
    MQListener mqListener;

    @MockBean
    @Qualifier("distributionChannel")
    MessageChannel distributionChannel;

    @Test
    public void processEntryResponseTest1(){
        DistributionMessage<List<GistGuildItem>> msg = new DistributionMessage<>();
        msg.setType(DistributionEventType.ENTRY_RESPONSE);
        msg.setCorrelationID(UUID.randomUUID());
        msg.setInstanceName("test-instance");
        List<GistGuildItem> items = new ArrayList<>();
        items.add(getItem());
        msg.setContent(items);

        //Correlation ID added by Controller request
        DistributionConcurrenceService.getCorrelationIDs().add(msg.getCorrelationID());

        mqListener.processEntryResponse(msg);

        Assert.assertFalse(DistributionConcurrenceService.getCorrelationIDs().contains(msg.getCorrelationID()));
    }

    @Test
    public void processEntryResponseTest2(){
        DistributionMessage<List<GistGuildItem>> msg = new DistributionMessage<>();
        msg.setType(DistributionEventType.INTEGRITY_VERIFICATION);
        msg.setCorrelationID(UUID.randomUUID());
        msg.setInstanceName("test-instance");
        List<GistGuildItem> items = new ArrayList<>();
        items.add(getItem());
        msg.setContent(items);

        //Correlation ID added by Controller request
        DistributionConcurrenceService.getCorrelationIDs().add(msg.getCorrelationID());

        mqListener.processEntryResponse(msg);

        Assert.assertFalse(DistributionConcurrenceService.getCorrelationIDs().contains(msg.getCorrelationID()));
        Assert.assertNotNull(ControllerResponseCache.getFromCache(msg.getCorrelationID()));

        //Correlation ID remved from cache by Controller request
        Assert.assertNotNull(ControllerResponseCache.removeFromCache(msg.getCorrelationID()));
        Assert.assertNull(ControllerResponseCache.getFromCache(msg.getCorrelationID()));
    }

    @Test
    public void processEntryResponseTest3(){
        DistributionMessage<List<GistGuildItem>> msg = new DistributionMessage<>();
        msg.setType(DistributionEventType.CORRUPTION_DETECTED);
        msg.setCorrelationID(UUID.randomUUID());
        msg.setInstanceName("test-instance");
        List<GistGuildItem> items = new ArrayList<>();
        items.add(getItem());
        msg.setContent(items);

        //Correlation ID added by Controller request
        DistributionConcurrenceService.getCorrelationIDs().add(msg.getCorrelationID());

        mqListener.processEntryResponse(msg);

        GistGuildItem gistGuildItem = msg.getContent().iterator().next();
        Assert.assertTrue(gistGuildItem.getIsCorruptionDetected());
        Assert.assertFalse(DistributionConcurrenceService.getCorrelationIDs().contains(msg.getCorrelationID()));
    }

    private GistGuildItem getItem() {
        GistGuildItem item = new GistGuildItem();
        item.setIsCorruptionDetected(Boolean.FALSE);
        item.setId(UUID.randomUUID().toString());
        item.setTimestamp(Instant.now());
        item.setNodeInstanceName("test-instance");
        item.setPreviousId(UUID.randomUUID().toString());
        item.setNonce(new Random().nextInt());
        Participant owner = new Participant();
        owner.setNickname("test@test.com");
        item.setOwner(owner);
        Document document = new Document();
        document.setDescription("Test document");
        item.setDocument(document);
        return item;
    }

}
