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
import com.gist.guild.node.core.repository.PaymentRepository;
import com.gist.guild.node.core.repository.ProductRepository;
import com.gist.guild.node.core.repository.RechargeCreditRepository;
import com.gist.guild.node.core.service.NodeBusinessService;
import com.gist.guild.node.core.service.NodeService;
import com.gist.guild.node.core.service.NodeUtils;
import com.gist.guild.node.core.service.impl.ParticipantServiceImpl;
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
public class MQListenerPaymentRegistrationTest extends MQListenerTest{
    @MockBean
    NodeService<com.gist.guild.commons.message.entity.Participant, Participant> participantNodeService;

    @MockBean
    NodeService<com.gist.guild.commons.message.entity.Product, Product> productNodeService;

    @MockBean
    NodeService<com.gist.guild.commons.message.entity.RechargeCredit, RechargeCredit> rechargeCreditNodeService;

    @MockBean
    NodeService<com.gist.guild.commons.message.entity.Order, Order> orderNodeService;

    @Autowired
    NodeService<com.gist.guild.commons.message.entity.Payment, Payment> paymentNodeService;

    private static final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    protected static DocumentProposition getNewDocument(String json) throws JsonProcessingException {
        log.info(json);
        DocumentProposition documentProposition = mapper.readValue(json, DocumentProposition.class);
        log.info(mapper.writeValueAsString(documentProposition));

        return documentProposition;
    }

    private DocumentProposition getDocumentPropositionPaymentRegistration() throws JsonProcessingException {
        String json = "{\n" +
                "    \"documentPropositionType\" : \"ORDER_PAYMENT_CONFIRMATION\",\n" +
                "    \"documentClass\" : \"Payment\",\n" +
                "    \"document\" : {\n" +
                "      \"amount\":\"15\",\n" +
                "      \"orderId\":\"3000\",\n" +
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
        msg.setContent(getDocumentPropositionPaymentRegistration());

        Payment payment = new Payment();
        payment.setPreviousId("GENESIS");
        payment.setNodeInstanceName(instanceName);
        payment.setOrderId("1010");
        payment.setAmount(15L);
        payment.setCustomerNickname("test");
        payment.setCustomerTelegramUserId(478956L);

        Random random = new Random(payment.getTimestamp().toEpochMilli());
        int nonce = random.nextInt();
        payment.setNonce(nonce);
        payment.setId(paymentNodeService.calculateHash(payment));
        while (!NodeUtils.isHashResolved(payment, difficultLevel)) {
            nonce = random.nextInt();
            payment.setNonce(nonce);
            payment.setId(paymentNodeService.calculateHash(payment));
        }

        List<Document> items = new ArrayList<>();
        items.add(payment);
        DistributionMessage<List<?>> responseMessage = new DistributionMessage<>();
        responseMessage.setCorrelationID(msg.getCorrelationID());
        responseMessage.setInstanceName(instanceName);
        responseMessage.setType(DistributionEventType.ENTRY_RESPONSE);
        responseMessage.setDocumentClass(Payment.class);
        responseMessage.setContent(items);

        Message<DistributionMessage<List<?>>> responseMsg = MessageBuilder.withPayload(responseMessage).build();

        Mockito.when(paymentRepository.save(ArgumentMatchers.any(Payment.class))).thenReturn(payment);
        Mockito.when(responseChannel.send(responseMsg)).thenReturn(Boolean.TRUE);
        mqListener.processDocumentProposition(msg);
    }
}
