package com.gist.guild.distribution.binding.test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.gist.guild.commons.message.DistributionEventType;
import com.gist.guild.commons.message.DistributionMessage;
import com.gist.guild.commons.message.entity.Document;
import com.gist.guild.commons.message.entity.DocumentProposition;
import com.gist.guild.commons.message.entity.Participant;
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

    private static final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    public void processEntryResponseTest1(){
        DistributionMessage<List<?>> msg = new DistributionMessage<>();
        msg.setType(DistributionEventType.ENTRY_RESPONSE);
        msg.setCorrelationID(UUID.randomUUID());
        msg.setInstanceName("test-instance");
        List<Document> items = new ArrayList<>();
        items.add(getItem());
        msg.setContent(items);

        //Correlation ID added by Controller request
        DistributionConcurrenceService.getCorrelationIDs().add(msg.getCorrelationID());

        mqListener.processEntryResponse(msg);

        Assert.assertFalse(DistributionConcurrenceService.getCorrelationIDs().contains(msg.getCorrelationID()));
    }

    @Test
    public void processEntryResponseTest2(){
        DistributionMessage<List<?>> msg = new DistributionMessage<>();
        msg.setType(DistributionEventType.INTEGRITY_VERIFICATION);
        msg.setCorrelationID(UUID.randomUUID());
        msg.setInstanceName("test-instance");
        List<Document> items = new ArrayList<>();
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
    public void processEntryResponseTest3() throws JsonProcessingException {
        DistributionMessage<List<?>> msg = new DistributionMessage<>();
        msg.setType(DistributionEventType.CORRUPTION_DETECTED);
        msg.setCorrelationID(UUID.randomUUID());
        msg.setInstanceName("test-instance");
        List<Document> items = new ArrayList<>();
        items.add(getItem());
        msg.setContent(items);

        //Correlation ID added by Controller request
        DistributionConcurrenceService.getCorrelationIDs().add(msg.getCorrelationID());

        mqListener.processEntryResponse(msg);

        Document document = mapper.readValue(mapper.writeValueAsString(msg.getContent().iterator().next()), Document.class) ;
        Assert.assertTrue(document.getIsCorruptionDetected());
        Assert.assertFalse(DistributionConcurrenceService.getCorrelationIDs().contains(msg.getCorrelationID()));
    }

    @Test
    public void processEntryResponseTest4() throws JsonProcessingException {
        DistributionMessage<List<?>> msg = new DistributionMessage<>();
        msg.setType(DistributionEventType.GET_DOCUMENT);
        msg.setCorrelationID(UUID.randomUUID());
        msg.setInstanceName("test-instance");
        List<Document> items = new ArrayList<>();
        items.add(getItem());
        msg.setContent(items);

        //Correlation ID added by Controller request
        DistributionConcurrenceService.getCorrelationIDs().add(msg.getCorrelationID());

        mqListener.processEntryResponse(msg);

        Document document = mapper.readValue(mapper.writeValueAsString(msg.getContent().iterator().next()), Document.class) ;
        Assert.assertFalse(DistributionConcurrenceService.getCorrelationIDs().contains(msg.getCorrelationID()));
    }

    private Document getItem() {
        Document item = new Document();
        item.setIsCorruptionDetected(Boolean.FALSE);
        item.setId(UUID.randomUUID().toString());
        item.setTimestamp(Instant.now());
        item.setNodeInstanceName("test-instance");
        item.setPreviousId(UUID.randomUUID().toString());
        item.setNonce(new Random().nextInt());
        Participant owner = new Participant();
        owner.setMail("test@test.com");
        DocumentProposition documentProposition = new DocumentProposition();
        documentProposition.setDescription("Test documentProposition");
        return item;
    }

}
