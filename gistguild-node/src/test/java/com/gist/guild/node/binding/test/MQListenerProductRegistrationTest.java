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
import com.gist.guild.node.core.document.Product;
import com.gist.guild.node.core.repository.ParticipantRepository;
import com.gist.guild.node.core.repository.ProductRepository;
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
public class MQListenerProductRegistrationTest {
    @Autowired
    MQListener mqListener;

    @MockBean
    ProductRepository productRepository;

    @MockBean
    @Qualifier("responseChannel")
    MessageChannel responseChannel;

    @Autowired
    NodeService<com.gist.guild.commons.message.entity.Product, Product> productNodeService;

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

    private DocumentProposition getDocumentPropositionProductRegistration() throws JsonProcessingException {
        String json = "{\n" +
                "    \"documentPropositionType\" : \"PRODUCT_REGISTRATION\",\n" +
                "    \"documentClass\" : \"Product\",\n" +
                "    \"document\" : {\n" +
                "      \"name\":\"test\",\n" +
                "      \"description\":\"test\",\n" +
                "      \"price\":\"10\",\n" +
                "      \"availableQuantity\":\"15\",\n" +
                "      \"ownerMail\":\"test\",\n" +
                "      \"ownerTelegramUserId\":\"478956\"\n" +
                "      }\n" +
                "    }\n" +
                "}";
        DocumentProposition proposition = getNewDocument(json);
        return proposition;
    }

    private DocumentProposition getDocumentPropositionProductCancellation() throws JsonProcessingException {
        String json = "{\n" +
                "    \"documentPropositionType\" : \"PRODUCT_REGISTRATION\",\n" +
                "    \"documentClass\" : \"Product\",\n" +
                "    \"document\" : {\n" +
                "      \"name\":\"test\",\n" +
                "      \"description\":\"test\",\n" +
                "      \"price\":\"10\",\n" +
                "      \"availableQuantity\":\"15\",\n" +
                "      \"ownerMail\":\"test\",\n" +
                "      \"ownerTelegramUserId\":\"478956\",\n" +
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
        msg.setContent(getDocumentPropositionProductRegistration());

        Product product = new Product();
        product.setPreviousId("GENESIS");
        product.setNodeInstanceName(instanceName);
        product.setName("test");
        product.setDescription("test");
        product.setAvailableQuantity(15L);
        product.setPrice(10L);
        product.setOwnerMail("test");
        product.setOwnerTelegramUserId(478956L);

        Random random = new Random(product.getTimestamp().toEpochMilli());
        int nonce = random.nextInt();
        product.setNonce(nonce);
        product.setId(productNodeService.calculateHash(product));
        while (!NodeUtils.isHashResolved(product, difficultLevel)) {
            nonce = random.nextInt();
            product.setNonce(nonce);
            product.setId(productNodeService.calculateHash(product));
        }

        List<Document> items = new ArrayList<>();
        items.add(product);
        DistributionMessage<List<?>> responseMessage = new DistributionMessage<>();
        responseMessage.setCorrelationID(msg.getCorrelationID());
        responseMessage.setInstanceName(instanceName);
        responseMessage.setType(DistributionEventType.ENTRY_RESPONSE);
        responseMessage.setDocumentClass(Product.class);
        responseMessage.setContent(items);

        Message<DistributionMessage<List<?>>> responseMsg = MessageBuilder.withPayload(responseMessage).build();

        Mockito.when(productRepository.save(ArgumentMatchers.any(Product.class))).thenReturn(product);
        Mockito.when(responseChannel.send(responseMsg)).thenReturn(Boolean.TRUE);
        mqListener.processDocumentProposition(msg);
    }

    @Test
    public void processDocumentPropositionTest2() throws JsonProcessingException, GistGuildGenericException {
        DistributionMessage<DocumentProposition> msg = new DistributionMessage<>();
        msg.setType(DistributionEventType.ENTRY_PROPOSITION);
        msg.setCorrelationID(UUID.randomUUID());
        msg.setContent(getDocumentPropositionProductCancellation());

        Product product = new Product();
        product.setPreviousId("GENESIS");
        product.setNodeInstanceName(instanceName);
        product.setName("test");
        product.setDescription("test");
        product.setAvailableQuantity(15L);
        product.setPrice(10L);
        product.setOwnerMail("test");
        product.setOwnerTelegramUserId(478956L);

        Random random = new Random(product.getTimestamp().toEpochMilli());
        int nonce = random.nextInt();
        product.setNonce(nonce);
        product.setId(productNodeService.calculateHash(product));
        while (!NodeUtils.isHashResolved(product, difficultLevel)) {
            nonce = random.nextInt();
            product.setNonce(nonce);
            product.setId(productNodeService.calculateHash(product));
        }

        product.setActive(Boolean.FALSE);

        List<Document> items = new ArrayList<>();
        items.add(product);
        DistributionMessage<List<?>> responseMessage = new DistributionMessage<>();
        responseMessage.setCorrelationID(msg.getCorrelationID());
        responseMessage.setInstanceName(instanceName);
        responseMessage.setType(DistributionEventType.ENTRY_RESPONSE);
        responseMessage.setDocumentClass(Participant.class);
        responseMessage.setContent(items);

        Message<DistributionMessage<List<?>>> responseMsg = MessageBuilder.withPayload(responseMessage).build();

        Mockito.when(productRepository.save(ArgumentMatchers.any(Product.class))).thenReturn(product);
        Mockito.when(responseChannel.send(responseMsg)).thenReturn(Boolean.TRUE);
        mqListener.processDocumentProposition(msg);
    }
}
