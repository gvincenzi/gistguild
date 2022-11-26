package com.gist.guild.node.binding;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.gist.guild.commons.exception.GistGuildGenericException;
import com.gist.guild.commons.message.DistributionEventType;
import com.gist.guild.commons.message.DistributionMessage;
import com.gist.guild.commons.message.DocumentRepositoryMethodParameter;
import com.gist.guild.commons.message.entity.Document;
import com.gist.guild.commons.message.entity.DocumentProposition;
import com.gist.guild.node.core.configuration.StartupConfig;
import com.gist.guild.node.core.document.Order;
import com.gist.guild.node.core.document.Participant;
import com.gist.guild.node.core.document.Product;
import com.gist.guild.node.core.repository.OrderRepository;
import com.gist.guild.node.core.repository.ParticipantRepository;
import com.gist.guild.node.core.repository.ProductRepository;
import com.gist.guild.node.core.service.NodeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@EnableBinding(MQBinding.class)
public class MQListener {
    @Autowired
    NodeService<com.gist.guild.commons.message.entity.Participant, Participant> participantNodeService;

    @Autowired
    NodeService<com.gist.guild.commons.message.entity.Product, Product> productNodeService;

    @Autowired
    NodeService<com.gist.guild.commons.message.entity.Order, Order> orderNodeService;

    @Autowired
    ParticipantRepository participantRepository;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    MessageChannel responseChannel;

    @Value("${spring.application.name}")
    private String instanceName;

    private static final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @StreamListener(target = "requestChannel")
    public void processDocumentProposition(DistributionMessage<DocumentProposition> msg) {
        log.info(String.format("START >> Message received in Request Channel with Correlation ID [%s]", msg.getCorrelationID()));
        List<Document> items = new ArrayList<>();
        Class documentClass = null;
        if (DistributionEventType.ENTRY_PROPOSITION.equals(msg.getType()) && msg.getContent() != null && StartupConfig.getStartupProcessed()) {
            try {
                switch (msg.getContent().getDocumentPropositionType()) {
                    case USER_REGISTRATION:
                        Participant participant = participantNodeService.add(mapper.readValue(mapper.writeValueAsString(msg.getContent().getDocument()), com.gist.guild.commons.message.entity.Participant.class));
                        documentClass = com.gist.guild.commons.message.entity.Participant.class;
                        items.add(participant);
                        log.info(String.format("New participant with ID [%s] correctly validated and ingested", participant.getId()));
                        sendResponseMessage(msg, items, documentClass);
                        break;
                    case PRODUCT_REGISTRATION:
                        Product product = productNodeService.add(mapper.readValue(mapper.writeValueAsString(msg.getContent().getDocument()), com.gist.guild.commons.message.entity.Product.class));
                        documentClass = com.gist.guild.commons.message.entity.Product.class;
                        items.add(product);
                        log.info(String.format("New product with ID [%s] correctly validated and ingested", product.getId()));
                        sendResponseMessage(msg, items, documentClass);
                        break;
                    case ORDER_REGISTRATION:
                        Order order = orderNodeService.add(mapper.readValue(mapper.writeValueAsString(msg.getContent().getDocument()), com.gist.guild.commons.message.entity.Order.class));
                        documentClass = com.gist.guild.commons.message.entity.Order.class;
                        items.add(order);
                        log.info(String.format("New order with ID [%s] correctly validated and ingested", order.getId()));
                        sendResponseMessage(msg, items, documentClass);
                        break;
                }
            } catch (GistGuildGenericException | JsonProcessingException e) {
                log.error(e.getMessage());
            }
        } else if (DistributionEventType.INTEGRITY_VERIFICATION.equals(msg.getType())) {
            processIntegrityRequest(msg);
        } else if (DistributionEventType.GET_DOCUMENT.equals(msg.getType())) {
            try {
                processGetDocumentRequest(msg);
            } catch (NoSuchMethodException e) {
                log.error(e.getMessage());
            } catch (InvocationTargetException | IllegalAccessException e) {
                log.error(e.getMessage());
            }
        }
        log.info(String.format("END >> Message received in Request Channel with Correlation ID [%s]", msg.getCorrelationID()));
    }

    private void sendResponseMessage(DistributionMessage<DocumentProposition> msg, List<Document> items, Class documentClass) {
        DistributionMessage<List<?>> responseMessage = new DistributionMessage<>();
        responseMessage.setCorrelationID(msg.getCorrelationID());
        responseMessage.setInstanceName(instanceName);
        responseMessage.setType(DistributionEventType.ENTRY_RESPONSE);
        responseMessage.setDocumentClass(documentClass);
        responseMessage.setContent(items);
        Message<DistributionMessage<List<?>>> responseMsg = MessageBuilder.withPayload(responseMessage).build();
        responseChannel.send(responseMsg);
    }

    private void processGetDocumentRequest(DistributionMessage<DocumentProposition> msg) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        //Populate DocumentRepositoryMethodParameters arrays
        Class<?>[] arrayParamType = new Class<?>[msg.getParams().size()];
        Object[] arrayParamValue = new Object[msg.getParams().size()];
        int index = 0;
        for (DocumentRepositoryMethodParameter param : msg.getParams()) {
            arrayParamType[index] = param.getType();
            arrayParamValue[index++] = param.getValue();
        }

        List<?> items = null;

        if (msg.getDocumentClass().getSimpleName().equalsIgnoreCase(Participant.class.getSimpleName())) {
            // PARTICIPANT DOCUMENT
            Method repositoryMethod = ParticipantRepository.class.getMethod(msg.getDocumentRepositoryMethod(), arrayParamType);
            items = (List<Participant>) repositoryMethod.invoke(participantRepository, arrayParamValue);
        } else if (msg.getDocumentClass().getSimpleName().equalsIgnoreCase(Product.class.getSimpleName())) {
            // PRODUCT DOCUMENT
            Method repositoryMethod = ProductRepository.class.getMethod(msg.getDocumentRepositoryMethod(), arrayParamType);
            items = (List<Product>) repositoryMethod.invoke(productRepository, arrayParamValue);
        }

        DistributionMessage<List<?>> responseMessage = new DistributionMessage<>();
        responseMessage.setCorrelationID(msg.getCorrelationID());
        responseMessage.setInstanceName(instanceName);
        responseMessage.setType(DistributionEventType.GET_DOCUMENT);
        responseMessage.setDocumentClass(msg.getDocumentClass());
        responseMessage.setContent(items);
        Message<DistributionMessage<List<?>>> responseMsg = MessageBuilder.withPayload(responseMessage).build();
        responseChannel.send(responseMsg);
    }

    private void processIntegrityRequest(DistributionMessage<DocumentProposition> msg) {
        // SEND A MESSAGE FOR EACH DOCUMENT TYPE
        try {
            // PARTICIPANT DOCUMENT
            List<Participant> items = participantNodeService.findAll();
            Boolean validation = participantNodeService.validate(items);
            if (!validation) {
                corruptionDetected(msg);
            }
            DistributionMessage<List<Participant>> responseMessage = new DistributionMessage<>();
            responseMessage.setCorrelationID(msg.getCorrelationID());
            responseMessage.setInstanceName(instanceName);
            responseMessage.setType(DistributionEventType.INTEGRITY_VERIFICATION);
            responseMessage.setDocumentClass(com.gist.guild.commons.message.entity.Participant.class);
            responseMessage.setValid(validation);
            responseMessage.setContent(items);
            Message<DistributionMessage<List<Participant>>> responseMsg = MessageBuilder.withPayload(responseMessage).build();
            responseChannel.send(responseMsg);

            // PRODUCT DOCUMENT
            List<Product> productList = productNodeService.findAll();
            Boolean validationProduct = productNodeService.validate(productList);
            if (!validationProduct) {
                corruptionDetected(msg);
            }
            DistributionMessage<List<Product>> responseProductMessage = new DistributionMessage<>();
            responseProductMessage.setCorrelationID(msg.getCorrelationID());
            responseProductMessage.setInstanceName(instanceName);
            responseProductMessage.setType(DistributionEventType.INTEGRITY_VERIFICATION);
            responseProductMessage.setDocumentClass(com.gist.guild.commons.message.entity.Product.class);
            responseProductMessage.setValid(validationProduct);
            responseProductMessage.setContent(productList);
            Message<DistributionMessage<List<Product>>> responseProductMsg = MessageBuilder.withPayload(responseProductMessage).build();
            responseChannel.send(responseProductMsg);

            // ORDER DOCUMENT
            List<Order> orderList = orderNodeService.findAll();
            Boolean validationOrder = orderNodeService.validate(orderList);
            if (!validationOrder) {
                corruptionDetected(msg);
            }
            DistributionMessage<List<Order>> responseOrderMessage = new DistributionMessage<>();
            responseOrderMessage.setCorrelationID(msg.getCorrelationID());
            responseOrderMessage.setInstanceName(instanceName);
            responseOrderMessage.setType(DistributionEventType.INTEGRITY_VERIFICATION);
            responseOrderMessage.setDocumentClass(com.gist.guild.commons.message.entity.Order.class);
            responseOrderMessage.setValid(validationOrder);
            responseOrderMessage.setContent(orderList);
            Message<DistributionMessage<List<Order>>> responseOrderMsg = MessageBuilder.withPayload(responseOrderMessage).build();
            responseChannel.send(responseOrderMsg);
        } catch (GistGuildGenericException e) {
            log.error(e.getMessage());
        }
    }

    @StreamListener(target = "distributionChannel")
    public void processDistribution(DistributionMessage<List<?>> msg) {
        if (DistributionEventType.ENTRY_RESPONSE.equals(msg.getType()) && msg.getContent() != null && !instanceName.equals(msg.getInstanceName()) && StartupConfig.getStartupProcessed()) {
            log.info(String.format("START >> Message received in Distribution Channel with Correlation ID [%s]", msg.getCorrelationID()));
            try {
                if (Participant.class.getSimpleName().equalsIgnoreCase(msg.getDocumentClass().getSimpleName())) {
                    for (Object item : msg.getContent()) {
                        // PARTICIPANT DOCUMENT
                        if (participantNodeService.updateLocal(mapper.readValue(mapper.writeValueAsString(item), com.gist.guild.commons.message.entity.Participant.class))) {
                            log.info(String.format("New participant with ID [%s] correctly validated and ingested", ((com.gist.guild.commons.message.entity.Participant) item).getId()));
                        } else {
                            corruptionDetected(msg);
                        }
                    }
                }
                if (Product.class.getSimpleName().equalsIgnoreCase(msg.getDocumentClass().getSimpleName())) {
                    for (Object item : msg.getContent()) {
                        // PRODUCT DOCUMENT
                        if (productNodeService.updateLocal(mapper.readValue(mapper.writeValueAsString(item), com.gist.guild.commons.message.entity.Product.class))) {
                            log.info(String.format("New product with ID [%s] correctly validated and ingested", ((com.gist.guild.commons.message.entity.Product) item).getId()));
                        } else {
                            corruptionDetected(msg);
                        }
                    }
                }
                if (Order.class.getSimpleName().equalsIgnoreCase(msg.getDocumentClass().getSimpleName())) {
                    for (Object item : msg.getContent()) {
                        // ORDER DOCUMENT
                        if (orderNodeService.updateLocal(mapper.readValue(mapper.writeValueAsString(item), com.gist.guild.commons.message.entity.Order.class))) {
                            log.info(String.format("New order with ID [%s] correctly validated and ingested", ((com.gist.guild.commons.message.entity.Order) item).getId()));
                        } else {
                            corruptionDetected(msg);
                        }
                    }
                }
            } catch (GistGuildGenericException | JsonProcessingException e) {
                log.error(e.getMessage());
            }
        } else if (DistributionEventType.INTEGRITY_VERIFICATION.equals(msg.getType()) && msg.getContent() != null && !instanceName.equals(msg.getInstanceName())) {
            // A MESSAGE RECEIVED FOR EACH DOCUMENT TYPE
            try {
                if (Participant.class.getSimpleName().equalsIgnoreCase(msg.getDocumentClass().getSimpleName())) {
                    List<com.gist.guild.commons.message.entity.Participant> participants = new ArrayList(msg.getContent().size());
                    for (Object document : msg.getContent()) {
                        participants.add(mapper.readValue(mapper.writeValueAsString(document), com.gist.guild.commons.message.entity.Participant.class));
                    }
                    participantNodeService.init(participants);
                    StartupConfig.startupParticipantProcessed = Boolean.TRUE;
                } else if (Product.class.getSimpleName().equalsIgnoreCase(msg.getDocumentClass().getSimpleName())) {
                    List<com.gist.guild.commons.message.entity.Product> products = new ArrayList(msg.getContent().size());
                    for (Object document : msg.getContent()) {
                        products.add(mapper.readValue(mapper.writeValueAsString(document), com.gist.guild.commons.message.entity.Product.class));
                    }
                    productNodeService.init(products);
                    StartupConfig.startupProductProcessed = Boolean.TRUE;
                } else if (Order.class.getSimpleName().equalsIgnoreCase(msg.getDocumentClass().getSimpleName())) {
                    List<com.gist.guild.commons.message.entity.Order> orders = new ArrayList(msg.getContent().size());
                    for (Object document : msg.getContent()) {
                        orders.add(mapper.readValue(mapper.writeValueAsString(document), com.gist.guild.commons.message.entity.Order.class));
                    }
                    orderNodeService.init(orders);
                    StartupConfig.startupOrderProcessed = Boolean.TRUE;
                }

                if (Boolean.TRUE.equals(StartupConfig.getStartupProcessed())) {
                    log.info("Startup process for this node has been correctly terminated");
                }

                log.info("Integrity verification correctly validated and ingested");
            } catch (GistGuildGenericException | JsonProcessingException e) {
                log.error(e.getMessage());
                corruptionDetected(msg);
                log.error("Integrity verification failed");
            }
        } else if (DistributionEventType.CORRUPTION_DETECTED.equals(msg.getType()) && msg.getContent() != null && StartupConfig.getStartupProcessed()) {
            //FIXNME How ?
        }
        log.info(String.format("END >> Message received in Distribution Channel with Correlation ID [%s]", msg.getCorrelationID()));
    }

    private void corruptionDetected(DistributionMessage<?> msg) {
        log.error(String.format("Corruption detected : send message with Correlation ID [%s]", msg.getCorrelationID()));
        DistributionMessage<List<Document>> responseMessage = new DistributionMessage<>();
        responseMessage.setCorrelationID(msg.getCorrelationID());
        responseMessage.setInstanceName(instanceName);
        responseMessage.setType(DistributionEventType.CORRUPTION_DETECTED);
        Message<DistributionMessage<List<Document>>> responseMsg = MessageBuilder.withPayload(responseMessage).build();
        responseChannel.send(responseMsg);
    }
}
