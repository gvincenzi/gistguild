package com.gist.guild.distribution.delivery.service.valence.test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.gist.guild.commons.message.DistributionEventType;
import com.gist.guild.commons.message.DistributionMessage;
import com.gist.guild.commons.message.DocumentRepositoryMethodParameter;
import com.gist.guild.commons.message.entity.DocumentProposition;
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

import java.util.ArrayList;
import java.util.List;

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
    @Qualifier("responseChannel")
    SubscribableChannel responseChannel;

    @MockBean
    DistributionConcurrenceService distributionConcurrenceService;

    private DocumentProposition getDocumentProposition() throws JsonProcessingException {
        String json = "{\n" +
                "    \"documentPropositionType\" : \"USER_REGISTRATION\",\n" +
                "    \"documentClass\" : \"Participant\",\n" +
                "    \"document\" : {\n" +
                "      \"nickname\":\"test\",\n" +
                "      \"telegramUserId\":\"478956\"\n" +
                "      }\n" +
                "    }\n" +
                "}";
        DocumentProposition proposition = mapper.readValue(json, DocumentProposition.class);
        return proposition;
    }

    private DocumentRepositoryMethodParameter getParam() throws JsonProcessingException {
        String json = "{\"type\":\"java.lang.String\",\"value\":\"test@test.it\"}";
        return mapper.readValue(json, DocumentRepositoryMethodParameter.class);
    }

    @Test
    public void propose() throws JsonProcessingException, DistributionException, ClassNotFoundException {
        DocumentProposition proposition = getDocumentProposition();
        Mockito.when(requestChannel.send(Mockito.any(Message.class))).thenReturn(Boolean.TRUE);

        DistributionMessage<DocumentProposition> proposed = deliveryValenceService.propose(proposition);
        AssertionErrors.assertNotNull("Correlation ID is null", proposed.getCorrelationID());
        AssertionErrors.assertEquals("DistributionType is not coherent", DistributionEventType.ENTRY_PROPOSITION,proposed.getType());
        AssertionErrors.assertEquals("DocumentProposition is not equal", proposition, proposed.getContent());

    }

    @Test
    public void sendIntegrityVerificationRequest() throws DistributionException {
        Mockito.when(requestChannel.send(Mockito.any(Message.class))).thenReturn(Boolean.TRUE);

        DistributionMessage<Void> proposed = deliveryValenceService.sendIntegrityVerificationRequest();
        AssertionErrors.assertNotNull("Correlation ID is null", proposed.getCorrelationID());
        AssertionErrors.assertEquals("DistributionType is not coherent", DistributionEventType.INTEGRITY_VERIFICATION,proposed.getType());

    }

    @Test
    public void getDocumentClass() throws JsonProcessingException, DistributionException, ClassNotFoundException {
        String method = "findByTelegramUserId";
        List<DocumentRepositoryMethodParameter> params = new ArrayList<>(1);
        params.add(getParam());
        Mockito.when(requestChannel.send(Mockito.any(Message.class))).thenReturn(Boolean.TRUE);

        DistributionMessage<Void> proposed = deliveryValenceService.sendDocumentClassRequest(Participant.class.getSimpleName(),method,params);
        AssertionErrors.assertNotNull("Correlation ID is null", proposed.getCorrelationID());
        AssertionErrors.assertEquals("DistributionType is not coherent", DistributionEventType.GET_DOCUMENT,proposed.getType());

    }
}
