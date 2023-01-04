package com.gist.guild.node.binding.test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.gist.guild.commons.exception.GistGuildGenericException;
import com.gist.guild.commons.message.DistributionEventType;
import com.gist.guild.commons.message.DistributionMessage;
import com.gist.guild.commons.message.entity.Document;
import com.gist.guild.commons.message.entity.DocumentProposition;
import com.gist.guild.node.binding.MQListener;
import com.gist.guild.node.core.document.Participant;
import com.gist.guild.node.core.repository.ParticipantRepository;
import com.gist.guild.node.core.service.NodeService;
import com.gist.guild.node.core.service.NodeUtils;
import lombok.extern.java.Log;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Log
@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class MQListenerUserRegistrationTest {
    @Autowired
    MQListener mqListener;

    @MockBean
    ParticipantRepository participantRepository;

    @MockBean
    @Qualifier("responseChannel")
    MessageChannel responseChannel;

    @Autowired
    NodeService<com.gist.guild.commons.message.entity.Participant, Participant> participantNodeService;

    @Value("${spring.application.name}")
    private String instanceName;

    @Value("${gistguild.difficult.level}")
    private Integer difficultLevel;

    private static final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    protected static DocumentProposition getNewDocument(String json) throws JsonProcessingException {
        log.info(json);
        DocumentProposition documentProposition = mapper.readValue(json, DocumentProposition.class);
        log.info(mapper.writeValueAsString(documentProposition));

        return documentProposition;
    }

    private DocumentProposition getDocumentPropositionUserRegistration() throws JsonProcessingException {
        String json = "{\n" +
                "    \"documentPropositionType\" : \"USER_REGISTRATION\",\n" +
                "    \"documentClass\" : \"Participant\",\n" +
                "    \"document\" : {\n" +
                "      \"nickname\":\"test\",\n" +
                "      \"telegramUserId\":\"478956\"\n" +
                "      }\n" +
                "    }\n" +
                "}";
        DocumentProposition proposition = getNewDocument(json);
        return proposition;
    }

    private DocumentProposition getDocumentPropositionUserCancellation() throws JsonProcessingException {
        String json = "{\n" +
                "    \"documentPropositionType\" : \"USER_REGISTRATION\",\n" +
                "    \"documentClass\" : \"Participant\",\n" +
                "    \"document\" : {\n" +
                "      \"nickname\":\"test\",\n" +
                "      \"telegramUserId\":\"478956\",\n" +
                "      \"active\":\"false\"\n" +
                "      }\n" +
                "    }\n" +
                "}";
        DocumentProposition proposition = getNewDocument(json);
        return proposition;
    }

    @Test
    public void processDocumentPropositionTest1() throws JsonProcessingException, GistGuildGenericException {
        DistributionMessage<DocumentProposition> msg = new DistributionMessage<>();
        msg.setType(DistributionEventType.ENTRY_PROPOSITION);
        msg.setCorrelationID(UUID.randomUUID());
        msg.setContent(getDocumentPropositionUserRegistration());

        Participant participant = new Participant();
        participant.setPreviousId("GENESIS");
        participant.setNodeInstanceName(instanceName);
        participant.setNickname("test");
        participant.setTelegramUserId(478956L);

        Random random = new Random(participant.getTimestamp().toEpochMilli());
        int nonce = random.nextInt();
        participant.setNonce(nonce);
        participant.setId(participantNodeService.calculateHash(participant));
        while (!NodeUtils.isHashResolved(participant, difficultLevel)) {
            nonce = random.nextInt();
            participant.setNonce(nonce);
            participant.setId(participantNodeService.calculateHash(participant));
        }

        List<Document> items = new ArrayList<>();
        items.add(participant);
        DistributionMessage<List<?>> responseMessage = new DistributionMessage<>();
        responseMessage.setCorrelationID(msg.getCorrelationID());
        responseMessage.setInstanceName(instanceName);
        responseMessage.setType(DistributionEventType.ENTRY_RESPONSE);
        responseMessage.setDocumentClass(Participant.class);
        responseMessage.setContent(items);

        Message<DistributionMessage<List<?>>> responseMsg = MessageBuilder.withPayload(responseMessage).build();

        Mockito.when(participantRepository.save(ArgumentMatchers.any(Participant.class))).thenReturn(participant);
        Mockito.when(responseChannel.send(responseMsg)).thenReturn(Boolean.TRUE);
        mqListener.processDocumentProposition(msg);
    }

    @Test
    public void processDocumentPropositionTest2() throws JsonProcessingException, GistGuildGenericException {
        DistributionMessage<DocumentProposition> msg = new DistributionMessage<>();
        msg.setType(DistributionEventType.ENTRY_PROPOSITION);
        msg.setCorrelationID(UUID.randomUUID());
        msg.setContent(getDocumentPropositionUserCancellation());

        Participant participant = new Participant();
        participant.setPreviousId("GENESIS");
        participant.setNodeInstanceName(instanceName);
        participant.setNickname("test");
        participant.setTelegramUserId(478956L);

        Random random = new Random(participant.getTimestamp().toEpochMilli());
        int nonce = random.nextInt();
        participant.setNonce(nonce);
        participant.setId(participantNodeService.calculateHash(participant));
        while (!NodeUtils.isHashResolved(participant, difficultLevel)) {
            nonce = random.nextInt();
            participant.setNonce(nonce);
            participant.setId(participantNodeService.calculateHash(participant));
        }

        participant.setActive(Boolean.FALSE);

        List<Document> items = new ArrayList<>();
        items.add(participant);
        DistributionMessage<List<?>> responseMessage = new DistributionMessage<>();
        responseMessage.setCorrelationID(msg.getCorrelationID());
        responseMessage.setInstanceName(instanceName);
        responseMessage.setType(DistributionEventType.ENTRY_RESPONSE);
        responseMessage.setDocumentClass(Participant.class);
        responseMessage.setContent(items);

        Message<DistributionMessage<List<?>>> responseMsg = MessageBuilder.withPayload(responseMessage).build();

        Mockito.when(participantRepository.save(ArgumentMatchers.any(Participant.class))).thenReturn(participant);
        Mockito.when(responseChannel.send(responseMsg)).thenReturn(Boolean.TRUE);
        mqListener.processDocumentProposition(msg);
    }
}
