package com.gist.guild.node.binding;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.gist.guild.commons.exception.GistGuildGenericException;
import com.gist.guild.commons.exception.GistGuildInsufficientCreditException;
import com.gist.guild.commons.exception.GistGuildInsufficientQuantityException;
import com.gist.guild.commons.message.DistributionEventType;
import com.gist.guild.commons.message.DistributionMessage;
import com.gist.guild.commons.message.DocumentRepositoryMethodParameter;
import com.gist.guild.commons.message.entity.Document;
import com.gist.guild.commons.message.entity.DocumentProposition;
import com.gist.guild.node.core.configuration.StartupConfig;
import com.gist.guild.node.core.document.*;
import com.gist.guild.node.core.repository.*;
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
    NodeService<com.gist.guild.commons.message.entity.RechargeCredit, RechargeCredit> rechargeCreditNodeService;

    @Autowired
    NodeService<com.gist.guild.commons.message.entity.Payment, Payment> paymentNodeService;

    @Autowired
    ParticipantRepository participantRepository;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    RechargeCreditRepository rechargeCreditRepository;

    @Autowired
    PaymentRepository paymentRepository;

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
                        Order order = null;
                        documentClass = com.gist.guild.commons.message.entity.Order.class;
                        try {
                            order = orderNodeService.add(mapper.readValue(mapper.writeValueAsString(msg.getContent().getDocument()), com.gist.guild.commons.message.entity.Order.class));
                        } catch (GistGuildInsufficientQuantityException e) {
                            log.error(e.getMessage());
                            sendResponseExceptionMessage(msg, items, documentClass, e);
                            break;
                        }
                        items.add(order);
                        log.info(String.format("New order with ID [%s] correctly validated and ingested", order.getId()));
                        sendResponseMessage(msg, items, documentClass);
                        break;
                    case RECHARGE_USER_CREDIT:
                        RechargeCredit rechargeCredit = rechargeCreditNodeService.add(mapper.readValue(mapper.writeValueAsString(msg.getContent().getDocument()), com.gist.guild.commons.message.entity.RechargeCredit.class));
                        documentClass = com.gist.guild.commons.message.entity.RechargeCredit.class;
                        items.add(rechargeCredit);
                        log.info(String.format("New rechargeCredit with ID [%s] correctly validated and ingested", rechargeCredit.getId()));
                        sendResponseMessage(msg, items, documentClass);
                        break;
                    case ORDER_PAYMENT_CONFIRMATION:
                        Payment payment = null;
                        documentClass = com.gist.guild.commons.message.entity.Payment.class;
                        try {
                            payment = paymentNodeService.add(mapper.readValue(mapper.writeValueAsString(msg.getContent().getDocument()), com.gist.guild.commons.message.entity.Payment.class));
                        } catch (GistGuildInsufficientCreditException e) {
                            log.error(e.getMessage());
                            sendResponseExceptionMessage(msg, items, documentClass, e);
                            break;
                        }
                        items.add(payment);
                        log.info(String.format("New payment with ID [%s] correctly validated and ingested", payment.getId()));
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

    private void sendResponseExceptionMessage(DistributionMessage<DocumentProposition> msg, List<Document> items, Class documentClass, GistGuildGenericException e) {
        DistributionMessage<List<?>> responseMessage = new DistributionMessage<>();
        responseMessage.setCorrelationID(msg.getCorrelationID());
        responseMessage.setInstanceName(instanceName);
        responseMessage.setType(DistributionEventType.BUSINESS_EXCEPTION);
        responseMessage.setDocumentClass(documentClass);
        responseMessage.setContent(items);

        List<GistGuildGenericException> exceptions = new ArrayList<>(1);
        exceptions.add(e);
        responseMessage.setExceptions(exceptions);
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
        } else if (msg.getDocumentClass().getSimpleName().equalsIgnoreCase(Order.class.getSimpleName())) {
            // ORDER DOCUMENT
            Method repositoryMethod = OrderRepository.class.getMethod(msg.getDocumentRepositoryMethod(), arrayParamType);
            items = (List<Order>) repositoryMethod.invoke(orderRepository, arrayParamValue);
        } else if (msg.getDocumentClass().getSimpleName().equalsIgnoreCase(RechargeCredit.class.getSimpleName())) {
            // RECHARGE_CREDIT DOCUMENT
            Method repositoryMethod = RechargeCreditRepository.class.getMethod(msg.getDocumentRepositoryMethod(), arrayParamType);
            items = (List<RechargeCredit>) repositoryMethod.invoke(rechargeCreditRepository, arrayParamValue);
        } else if (msg.getDocumentClass().getSimpleName().equalsIgnoreCase(Payment.class.getSimpleName())) {
            // PAYMENT DOCUMENT
            Method repositoryMethod = PaymentRepository.class.getMethod(msg.getDocumentRepositoryMethod(), arrayParamType);
            items = (List<Payment>) repositoryMethod.invoke(paymentRepository, arrayParamValue);
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
                corruptionDetected(msg, Participant.class);
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
                corruptionDetected(msg, Product.class);
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
                corruptionDetected(msg, Order.class);
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

            // RECHARGE_CREDIT DOCUMENT
            List<RechargeCredit> rechargeCreditList = rechargeCreditNodeService.findAll();
            Boolean validationRechargeCredit = rechargeCreditNodeService.validate(rechargeCreditList);
            if (!validationRechargeCredit) {
                corruptionDetected(msg, RechargeCredit.class);
            }
            DistributionMessage<List<RechargeCredit>> responseRechargeCreditMessage = new DistributionMessage<>();
            responseRechargeCreditMessage.setCorrelationID(msg.getCorrelationID());
            responseRechargeCreditMessage.setInstanceName(instanceName);
            responseRechargeCreditMessage.setType(DistributionEventType.INTEGRITY_VERIFICATION);
            responseRechargeCreditMessage.setDocumentClass(com.gist.guild.commons.message.entity.RechargeCredit.class);
            responseRechargeCreditMessage.setValid(validationOrder);
            responseRechargeCreditMessage.setContent(rechargeCreditList);
            Message<DistributionMessage<List<RechargeCredit>>> responseRechargeCreditMsg = MessageBuilder.withPayload(responseRechargeCreditMessage).build();
            responseChannel.send(responseRechargeCreditMsg);

            // PAYMENT DOCUMENT
            List<Payment> paymentList = paymentNodeService.findAll();
            Boolean validationPayment = paymentNodeService.validate(paymentList);
            if (!validationPayment) {
                corruptionDetected(msg, Payment.class);
            }
            DistributionMessage<List<Payment>> responsePaymentMessage = new DistributionMessage<>();
            responsePaymentMessage.setCorrelationID(msg.getCorrelationID());
            responsePaymentMessage.setInstanceName(instanceName);
            responsePaymentMessage.setType(DistributionEventType.INTEGRITY_VERIFICATION);
            responsePaymentMessage.setDocumentClass(com.gist.guild.commons.message.entity.Payment.class);
            responsePaymentMessage.setValid(validationPayment);
            responsePaymentMessage.setContent(paymentList);
            Message<DistributionMessage<List<Payment>>> responsePaymentMsg = MessageBuilder.withPayload(responsePaymentMessage).build();
            responseChannel.send(responsePaymentMsg);
        } catch (GistGuildGenericException e) {
            log.error(e.getMessage());
        }
    }

    @StreamListener(target = "distributionChannel")
    public void processDistribution(DistributionMessage<List<?>> msg) {
        log.info(String.format("START >> Message received in Distribution Channel with Correlation ID [%s]", msg.getCorrelationID()));
        if (DistributionEventType.ENTRY_RESPONSE.equals(msg.getType()) && msg.getContent() != null && !instanceName.equals(msg.getInstanceName()) && StartupConfig.getStartupProcessed()) {
            try {
                if (Participant.class.getSimpleName().equalsIgnoreCase(msg.getDocumentClass().getSimpleName())) {
                    for (Object item : msg.getContent()) {
                        // PARTICIPANT DOCUMENT
                        if (participantNodeService.updateLocal(mapper.readValue(mapper.writeValueAsString(item), com.gist.guild.commons.message.entity.Participant.class))) {
                            log.info(String.format("New participant with ID [%s] correctly validated and ingested", ((com.gist.guild.commons.message.entity.Participant) item).getId()));
                        } else {
                            corruptionDetected(msg, Participant.class);
                        }
                    }
                }
                if (Product.class.getSimpleName().equalsIgnoreCase(msg.getDocumentClass().getSimpleName())) {
                    for (Object item : msg.getContent()) {
                        // PRODUCT DOCUMENT
                        if (productNodeService.updateLocal(mapper.readValue(mapper.writeValueAsString(item), com.gist.guild.commons.message.entity.Product.class))) {
                            log.info(String.format("New product with ID [%s] correctly validated and ingested", ((com.gist.guild.commons.message.entity.Product) item).getId()));
                        } else {
                            corruptionDetected(msg, Product.class);
                        }
                    }
                }
                if (Order.class.getSimpleName().equalsIgnoreCase(msg.getDocumentClass().getSimpleName())) {
                    for (Object item : msg.getContent()) {
                        // ORDER DOCUMENT
                        if (orderNodeService.updateLocal(mapper.readValue(mapper.writeValueAsString(item), com.gist.guild.commons.message.entity.Order.class))) {
                            log.info(String.format("New order with ID [%s] correctly validated and ingested", ((com.gist.guild.commons.message.entity.Order) item).getId()));
                        } else {
                            corruptionDetected(msg, Order.class);
                        }
                    }
                }
                if (RechargeCredit.class.getSimpleName().equalsIgnoreCase(msg.getDocumentClass().getSimpleName())) {
                    for (Object item : msg.getContent()) {
                        // RECAHRGE_CREDIT DOCUMENT
                        if (rechargeCreditNodeService.updateLocal(mapper.readValue(mapper.writeValueAsString(item), com.gist.guild.commons.message.entity.RechargeCredit.class))) {
                            log.info(String.format("New rechargeCredit with ID [%s] correctly validated and ingested", ((com.gist.guild.commons.message.entity.RechargeCredit) item).getId()));
                        } else {
                            corruptionDetected(msg, RechargeCredit.class);
                        }
                    }
                }
                if (Payment.class.getSimpleName().equalsIgnoreCase(msg.getDocumentClass().getSimpleName())) {
                    for (Object item : msg.getContent()) {
                        // PAYMENT DOCUMENT
                        if (paymentNodeService.updateLocal(mapper.readValue(mapper.writeValueAsString(item), com.gist.guild.commons.message.entity.Payment.class))) {
                            log.info(String.format("New payment with ID [%s] correctly validated and ingested", ((com.gist.guild.commons.message.entity.Payment) item).getId()));
                        } else {
                            corruptionDetected(msg, Payment.class);
                        }
                    }
                }
            } catch (GistGuildGenericException | JsonProcessingException e) {
                log.error(e.getMessage());
            }
        } else if (DistributionEventType.INTEGRITY_VERIFICATION.equals(msg.getType()) && msg.getContent() != null && !instanceName.equals(msg.getInstanceName())) {
            // A MESSAGE RECEIVED FOR EACH DOCUMENT TYPE
            Class classProcessing = null;
            try {
                if (Participant.class.getSimpleName().equalsIgnoreCase(msg.getDocumentClass().getSimpleName())) {
                    classProcessing = Participant.class;
                    List<com.gist.guild.commons.message.entity.Participant> participants = new ArrayList(msg.getContent().size());
                    for (Object document : msg.getContent()) {
                        participants.add(mapper.readValue(mapper.writeValueAsString(document), com.gist.guild.commons.message.entity.Participant.class));
                    }
                    participantNodeService.init(participants);
                    StartupConfig.startupParticipantProcessed = Boolean.TRUE;
                } else if (Product.class.getSimpleName().equalsIgnoreCase(msg.getDocumentClass().getSimpleName())) {
                    classProcessing = Product.class;
                    List<com.gist.guild.commons.message.entity.Product> products = new ArrayList(msg.getContent().size());
                    for (Object document : msg.getContent()) {
                        products.add(mapper.readValue(mapper.writeValueAsString(document), com.gist.guild.commons.message.entity.Product.class));
                    }
                    productNodeService.init(products);
                    StartupConfig.startupProductProcessed = Boolean.TRUE;
                } else if (Order.class.getSimpleName().equalsIgnoreCase(msg.getDocumentClass().getSimpleName())) {
                    classProcessing = Order.class;
                    List<com.gist.guild.commons.message.entity.Order> orders = new ArrayList(msg.getContent().size());
                    for (Object document : msg.getContent()) {
                        orders.add(mapper.readValue(mapper.writeValueAsString(document), com.gist.guild.commons.message.entity.Order.class));
                    }
                    orderNodeService.init(orders);
                    StartupConfig.startupOrderProcessed = Boolean.TRUE;
                } else if (RechargeCredit.class.getSimpleName().equalsIgnoreCase(msg.getDocumentClass().getSimpleName())) {
                    classProcessing = RechargeCredit.class;
                    List<com.gist.guild.commons.message.entity.RechargeCredit> rechargeCredits = new ArrayList(msg.getContent().size());
                    for (Object document : msg.getContent()) {
                        rechargeCredits.add(mapper.readValue(mapper.writeValueAsString(document), com.gist.guild.commons.message.entity.RechargeCredit.class));
                    }
                    rechargeCreditNodeService.init(rechargeCredits);
                    StartupConfig.startupRechargeCreditProcessed = Boolean.TRUE;
                } else if (Payment.class.getSimpleName().equalsIgnoreCase(msg.getDocumentClass().getSimpleName())) {
                    classProcessing = Payment.class;
                    List<com.gist.guild.commons.message.entity.Payment> payments = new ArrayList(msg.getContent().size());
                    for (Object document : msg.getContent()) {
                        payments.add(mapper.readValue(mapper.writeValueAsString(document), com.gist.guild.commons.message.entity.Payment.class));
                    }
                    paymentNodeService.init(payments);
                    StartupConfig.startupPaymentProcessed = Boolean.TRUE;
                }

                if (Boolean.TRUE.equals(StartupConfig.getStartupProcessed())) {
                    log.info("Startup process for this node has been correctly terminated");
                }

                log.info("Integrity verification correctly validated and ingested");
            } catch (GistGuildGenericException | JsonProcessingException e) {
                log.error(e.getMessage());
                corruptionDetected(msg, classProcessing);
                log.error("Integrity verification failed");
            }
        } else if (DistributionEventType.CORRUPTION_DETECTED.equals(msg.getType()) && msg.getContent() != null && StartupConfig.getStartupProcessed()) {
            log.warn(String.format("Corruption detected by instance [%s]", msg.getInstanceName()));
            try {
                Document itemCorruption = mapper.readValue(mapper.writeValueAsString(msg.getContent().iterator().next()), com.gist.guild.commons.message.entity.Document.class);
                Participant participant = new Participant();
                participant.setId(itemCorruption.getId());
                participant.setTimestamp(itemCorruption.getTimestamp());
                participant.setNodeInstanceName(itemCorruption.getNodeInstanceName());
                participant.setIsCorruptionDetected(itemCorruption.getIsCorruptionDetected());

                participantNodeService.add(participant);
            } catch (GistGuildGenericException | JsonProcessingException e) {
                log.error(e.getMessage());
            }
        }
        log.info(String.format("END >> Message received in Distribution Channel with Correlation ID [%s]", msg.getCorrelationID()));
    }

    private void corruptionDetected(DistributionMessage<?> msg, Class classDetected) {
        log.error(String.format("Corruption detected on document [%s]: send message with Correlation ID [%s]", classDetected.getSimpleName(), msg.getCorrelationID()));
        DistributionMessage<List<Document>> responseMessage = new DistributionMessage<>();
        responseMessage.setCorrelationID(msg.getCorrelationID());
        responseMessage.setInstanceName(instanceName);
        responseMessage.setType(DistributionEventType.CORRUPTION_DETECTED);
        Message<DistributionMessage<List<Document>>> responseMsg = MessageBuilder.withPayload(responseMessage).build();
        responseChannel.send(responseMsg);
    }
}
