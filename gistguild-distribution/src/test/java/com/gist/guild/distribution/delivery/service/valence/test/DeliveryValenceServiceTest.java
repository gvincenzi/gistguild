package com.gist.guild.distribution.delivery.service.valence.test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.gist.guild.commons.message.DistributionEventType;
import com.gist.guild.commons.message.DistributionMessage;
import com.gist.guild.commons.message.entity.Document;
import com.gist.guild.commons.message.entity.Participant;
import com.gist.guild.distribution.delivery.service.DistributionConcurrenceService;
import com.gist.guild.distribution.domain.service.valence.DeliveryValenceService;
import com.gist.guild.distribution.exception.DistributionException;
import lombok.extern.java.Log;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.AssertionErrors;

@Log
@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class DeliveryValenceServiceTest {
    private static final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Autowired
    DeliveryValenceService deliveryValenceService;

    @MockBean
    @Qualifier("requestChannel")
    MessageChannel requestChannel;

    @MockBean
    @Qualifier("requestIntegrityChannel")
    MessageChannel requestIntegrityChannel;

    @MockBean
    @Qualifier("responseChannel")
    SubscribableChannel responseChannel;

    @MockBean
    DistributionConcurrenceService distributionConcurrenceService;

    protected static Document getNewDocument(String json) throws JsonProcessingException {
        log.info(json);
        Document document = mapper.readValue(json, Document.class);
        log.info(mapper.writeValueAsString(document));

        return document;
    }

    private Document getEntryProposition() throws JsonProcessingException {
        String json = "{\"description\":\"Test document\",\"countryName\":\"Italy\","
                + "\"countryPopulation\":60591668,\"male\":29665645,\"female\":30921362}";
        Document proposition = getNewDocument(json);
        Participant owner = new Participant();
        owner.setNickname("test@test.com");
        proposition.setOwner(owner);
        return proposition;
    }

    @Test
    public void addNewEntry() throws JsonProcessingException, DistributionException {
        Document proposition = getEntryProposition();
        Mockito.when(requestChannel.send(Mockito.any(Message.class))).thenReturn(Boolean.TRUE);

        DistributionMessage<Document> proposed = deliveryValenceService.propose(proposition);
        AssertionErrors.assertNotNull("Correlation ID is null", proposed.getCorrelationID());
        AssertionErrors.assertEquals("DistributionType is not coherent", DistributionEventType.ENTRY_PROPOSITION,proposed.getType());
        AssertionErrors.assertEquals("EntryProposition is not equal", proposition, proposed.getContent());

    }

    @Test
    public void sendIntegrityVerificationRequest() throws DistributionException {
        Mockito.when(requestChannel.send(Mockito.any(Message.class))).thenReturn(Boolean.TRUE);

        DistributionMessage<Void> proposed = deliveryValenceService.sendIntegrityVerificationRequest();
        AssertionErrors.assertNotNull("Correlation ID is null", proposed.getCorrelationID());
        AssertionErrors.assertEquals("DistributionType is not coherent", DistributionEventType.INTEGRITY_VERIFICATION,proposed.getType());

    }
}
