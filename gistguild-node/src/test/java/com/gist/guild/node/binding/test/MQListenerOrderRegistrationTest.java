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
import com.gist.guild.node.core.document.*;
import com.gist.guild.node.core.repository.OrderRepository;
import com.gist.guild.node.core.service.NodeBusinessService;
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
public class MQListenerOrderRegistrationTest extends MQListenerTest{
    @MockBean
    NodeService<com.gist.guild.commons.message.entity.Participant, Participant> participantNodeService;

    @MockBean
    NodeService<com.gist.guild.commons.message.entity.Product, Product> productNodeService;

    @MockBean
    NodeService<com.gist.guild.commons.message.entity.RechargeCredit, RechargeCredit> rechargeCreditNodeService;

    @MockBean
    NodeService<com.gist.guild.commons.message.entity.Payment, Payment> paymentNodeService;

    @Autowired
    NodeService<com.gist.guild.commons.message.entity.Order, Order> orderNodeService;

    protected static DocumentProposition getNewDocument(String json) throws JsonProcessingException {
        log.info(json);
        DocumentProposition documentProposition = mapper.readValue(json, DocumentProposition.class);
        log.info(mapper.writeValueAsString(documentProposition));

        return documentProposition;
    }

    private DocumentProposition getDocumentPropositionOrderRegistration() throws JsonProcessingException {
        String json = "{\n" +
                "    \"documentPropositionType\" : \"ORDER_REGISTRATION\",\n" +
                "    \"documentClass\" : \"Order\",\n" +
                "    \"document\" : {\n" +
                "      \"amount\":\"15\",\n" +
                "      \"quantity\":\"3\",\n" +
                "      \"productName\":\"test\",\n" +
                "      \"productId\":\"100\",\n" +
                "      \"customerNickname\":\"test\",\n" +
                "      \"customerTelegramUserId\":\"478956\"\n" +
                "      }\n" +
                "    }\n" +
                "}";
        DocumentProposition proposition = getNewDocument(json);
        return proposition;
    }

    private DocumentProposition getDocumentPropositionOrderCancellation() throws JsonProcessingException {
        String json = "{\n" +
                "    \"documentPropositionType\" : \"ORDER_REGISTRATION\",\n" +
                "    \"documentClass\" : \"Order\",\n" +
                "    \"document\" : {\n" +
                "      \"amount\":\"15\",\n" +
                "      \"quantity\":\"3\",\n" +
                "      \"productName\":\"test\",\n" +
                "      \"productId\":\"100\",\n" +
                "      \"customerNickname\":\"test\",\n" +
                "      \"customerTelegramUserId\":\"478956\",\n" +
                "      \"deleted\":\"true\"\n" +
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
        msg.setContent(getDocumentPropositionOrderRegistration());

        Order order = new Order();
        order.setPreviousId("GENESIS");
        order.setNodeInstanceName(instanceName);
        order.setProductName("test");
        order.setProductId("1010");
        order.setAmount(15L);
        order.setQuantity(3L);
        order.setCustomerNickname("test");
        order.setCustomerTelegramUserId(478956L);

        Random random = new Random(order.getTimestamp().toEpochMilli());
        int nonce = random.nextInt();
        order.setNonce(nonce);
        order.setId(orderNodeService.calculateHash(order));
        while (!NodeUtils.isHashResolved(order, difficultLevel)) {
            nonce = random.nextInt();
            order.setNonce(nonce);
            order.setId(orderNodeService.calculateHash(order));
        }

        List<Document> items = new ArrayList<>();
        items.add(order);
        DistributionMessage<List<?>> responseMessage = new DistributionMessage<>();
        responseMessage.setCorrelationID(msg.getCorrelationID());
        responseMessage.setInstanceName(instanceName);
        responseMessage.setType(DistributionEventType.ENTRY_RESPONSE);
        responseMessage.setDocumentClass(Order.class);
        responseMessage.setContent(items);

        Message<DistributionMessage<List<?>>> responseMsg = MessageBuilder.withPayload(responseMessage).build();

        Mockito.when(orderRepository.save(ArgumentMatchers.any(Order.class))).thenReturn(order);
        Mockito.when(responseChannel.send(responseMsg)).thenReturn(Boolean.TRUE);
        mqListener.processDocumentProposition(msg);
    }

    @Test
    public void processDocumentPropositionTest2() throws JsonProcessingException, GistGuildGenericException {
        DistributionMessage<DocumentProposition> msg = new DistributionMessage<>();
        msg.setType(DistributionEventType.ENTRY_PROPOSITION);
        msg.setCorrelationID(UUID.randomUUID());
        msg.setContent(getDocumentPropositionOrderCancellation());

        Order order = new Order();
        order.setPreviousId("GENESIS");
        order.setNodeInstanceName(instanceName);
        order.setProductName("test");
        order.setProductId("1010");
        order.setAmount(15L);
        order.setQuantity(3L);
        order.setCustomerNickname("test");
        order.setCustomerTelegramUserId(478956L);

        Random random = new Random(order.getTimestamp().toEpochMilli());
        int nonce = random.nextInt();
        order.setNonce(nonce);
        order.setId(orderNodeService.calculateHash(order));
        while (!NodeUtils.isHashResolved(order, difficultLevel)) {
            nonce = random.nextInt();
            order.setNonce(nonce);
            order.setId(orderNodeService.calculateHash(order));
        }

        order.setDeleted(Boolean.TRUE);

        List<Document> items = new ArrayList<>();
        items.add(order);
        DistributionMessage<List<?>> responseMessage = new DistributionMessage<>();
        responseMessage.setCorrelationID(msg.getCorrelationID());
        responseMessage.setInstanceName(instanceName);
        responseMessage.setType(DistributionEventType.ENTRY_RESPONSE);
        responseMessage.setDocumentClass(Participant.class);
        responseMessage.setContent(items);

        Message<DistributionMessage<List<?>>> responseMsg = MessageBuilder.withPayload(responseMessage).build();

        Mockito.when(orderRepository.save(ArgumentMatchers.any(Order.class))).thenReturn(order);
        Mockito.when(responseChannel.send(responseMsg)).thenReturn(Boolean.TRUE);
        mqListener.processDocumentProposition(msg);
    }
}
