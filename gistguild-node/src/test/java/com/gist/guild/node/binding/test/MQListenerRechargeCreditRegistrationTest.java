package com.gist.guild.node.binding.test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.gist.guild.commons.exception.GistGuildGenericException;
import com.gist.guild.commons.message.DistributionEventType;
import com.gist.guild.commons.message.DistributionMessage;
import com.gist.guild.commons.message.entity.Document;
import com.gist.guild.commons.message.entity.DocumentProposition;
import com.gist.guild.commons.message.entity.RechargeCreditType;
import com.gist.guild.node.core.document.*;
import com.gist.guild.node.core.service.NodeService;
import com.gist.guild.node.core.service.NodeUtils;
import lombok.extern.java.Log;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.messaging.Message;
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
public class MQListenerRechargeCreditRegistrationTest extends MQListenerTest{
    @MockBean
    NodeService<com.gist.guild.commons.message.entity.Participant, Participant> participantNodeService;

    @MockBean
    NodeService<com.gist.guild.commons.message.entity.Product, Product> productNodeService;

    @Autowired
    NodeService<com.gist.guild.commons.message.entity.RechargeCredit, RechargeCredit> rechargeCreditNodeService;

    @MockBean
    NodeService<com.gist.guild.commons.message.entity.Order, Order> orderNodeService;

    @MockBean
    NodeService<com.gist.guild.commons.message.entity.Payment, Payment> paymentNodeService;

    private static final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    protected static DocumentProposition getNewDocument(String json) throws JsonProcessingException {
        log.info(json);
        DocumentProposition documentProposition = mapper.readValue(json, DocumentProposition.class);
        log.info(mapper.writeValueAsString(documentProposition));

        return documentProposition;
    }

    private DocumentProposition getDocumentPropositionRechargeCreditRegistration() throws JsonProcessingException {
        String json = "{\n" +
                "    \"documentPropositionType\" : \"RECHARGE_USER_CREDIT\",\n" +
                "    \"documentClass\" : \"RechargeCredit\",\n" +
                "    \"document\" : {\n" +
                "      \"rechargeUserCreditType\":\"ADMIN\",\n" +
                "      \"newCredit\":\"10\",\n" +
                "      \"oldCredit\":\"5\",\n" +
                "      \"customerNickname\":\"test\",\n" +
                "      \"customerTelegramUserId\":\"478956\"\n" +
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
        msg.setContent(getDocumentPropositionRechargeCreditRegistration());

        RechargeCredit rechargeCredit = new RechargeCredit();
        rechargeCredit.setPreviousId("GENESIS");
        rechargeCredit.setNodeInstanceName(instanceName);
        rechargeCredit.setRechargeUserCreditType(RechargeCreditType.ADMIN);
        rechargeCredit.setNewCredit(10L);
        rechargeCredit.setOldCredit(5L);
        rechargeCredit.setCustomerNickname("test");
        rechargeCredit.setCustomerTelegramUserId(478956L);

        Random random = new Random(rechargeCredit.getTimestamp().toEpochMilli());
        int nonce = random.nextInt();
        rechargeCredit.setNonce(nonce);
        rechargeCredit.setId(rechargeCreditNodeService.calculateHash(rechargeCredit));
        while (!NodeUtils.isHashResolved(rechargeCredit, difficultLevel)) {
            nonce = random.nextInt();
            rechargeCredit.setNonce(nonce);
            rechargeCredit.setId(rechargeCreditNodeService.calculateHash(rechargeCredit));
        }

        List<Document> items = new ArrayList<>();
        items.add(rechargeCredit);
        DistributionMessage<List<?>> responseMessage = new DistributionMessage<>();
        responseMessage.setCorrelationID(msg.getCorrelationID());
        responseMessage.setInstanceName(instanceName);
        responseMessage.setType(DistributionEventType.ENTRY_RESPONSE);
        responseMessage.setDocumentClass(RechargeCredit.class);
        responseMessage.setContent(items);

        Message<DistributionMessage<List<?>>> responseMsg = MessageBuilder.withPayload(responseMessage).build();

        Mockito.when(rechargeCreditRepository.save(ArgumentMatchers.any(RechargeCredit.class))).thenReturn(rechargeCredit);
        Mockito.when(responseChannel.send(responseMsg)).thenReturn(Boolean.TRUE);
        mqListener.processDocumentProposition(msg);
    }
}
