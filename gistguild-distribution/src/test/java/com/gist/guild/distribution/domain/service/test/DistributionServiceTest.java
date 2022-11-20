package com.gist.guild.distribution.domain.service.test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.gist.guild.commons.message.DistributionEventType;
import com.gist.guild.commons.message.DistributionMessage;
import com.gist.guild.commons.message.entity.Document;
import com.gist.guild.commons.message.entity.Participant;
import com.gist.guild.distribution.domain.service.valence.DeliveryValenceService;
import com.gist.guild.distribution.exception.DistributionException;
import lombok.extern.java.Log;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.AssertionErrors;

import java.util.UUID;

@Log
@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class DistributionServiceTest {
    private static final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @MockBean
    DeliveryValenceService deliveryValenceService;

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
        DistributionMessage<Document> distributionMessage = new DistributionMessage<>();
        distributionMessage.setCorrelationID(UUID.randomUUID());
        distributionMessage.setType(DistributionEventType.ENTRY_PROPOSITION);
        distributionMessage.setContent(proposition);
        Mockito.when(deliveryValenceService.propose(proposition)).thenReturn(distributionMessage);

        DistributionMessage<Document> proposed = deliveryValenceService.propose(proposition);
        AssertionErrors.assertNotNull("Correlation ID is null", proposed.getCorrelationID());
        AssertionErrors.assertEquals("DistributionType is not coherent", DistributionEventType.ENTRY_PROPOSITION,proposed.getType());
        AssertionErrors.assertEquals("EntryProposition is not equal", proposition, proposed.getContent());
    }

    @Test
    public void verifyRegistryIntegrity() throws DistributionException {
        DistributionMessage<Void> distributionMessage = new DistributionMessage<>();
        distributionMessage.setCorrelationID(UUID.randomUUID());
        distributionMessage.setType(DistributionEventType.INTEGRITY_VERIFICATION);
        Mockito.when(deliveryValenceService.sendIntegrityVerificationRequest()).thenReturn(distributionMessage);
        DistributionMessage<Void> proposed = deliveryValenceService.sendIntegrityVerificationRequest();
        AssertionErrors.assertNotNull("Correlation ID is null", proposed.getCorrelationID());
        AssertionErrors.assertEquals("DistributionType is not coherent", DistributionEventType.INTEGRITY_VERIFICATION,proposed.getType());
    }
}
