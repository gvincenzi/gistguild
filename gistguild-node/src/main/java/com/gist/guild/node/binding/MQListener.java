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
import com.gist.guild.commons.message.entity.Communication;
import com.gist.guild.commons.message.entity.Document;
import com.gist.guild.commons.message.entity.DocumentProposition;
import com.gist.guild.node.core.configuration.MessageProperties;
import com.gist.guild.node.core.configuration.StartupConfig;
import com.gist.guild.node.core.document.*;
import com.gist.guild.node.core.repository.*;
import com.gist.guild.node.core.service.DistributionProcessor;
import com.gist.guild.node.core.service.EntryPropositionProcessor;
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
import java.util.UUID;
import java.util.concurrent.DelayQueue;

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

    @Autowired
    MessageProperties messageProperties;

    @Autowired
    EntryPropositionProcessor entryPropositionProcessor;

    @Autowired
    DistributionProcessor distributionProcessor;

    @Value("${spring.application.name}")
    private String instanceName;

    private static final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @StreamListener(target = "distributionChannel")
    public void processDistribution(DistributionMessage<List<?>> msg) {
        log.debug(String.format("START >> Message received in Distribution Channel with Correlation ID [%s]", msg.getCorrelationID()));

        distributionProcessor.add(msg);

        log.debug(String.format("END >> Message received in Distribution Channel with Correlation ID [%s]", msg.getCorrelationID()));
    }

    @StreamListener(target = "requestChannel")
    public void processDocumentProposition(DistributionMessage<DocumentProposition<?>> msg) {
        waitingForDistributionProcess();

        log.debug(String.format("START >> Message received in Request Channel with Correlation ID [%s]", msg.getCorrelationID()));
        if (DistributionEventType.ENTRY_PROPOSITION.equals(msg.getType()) && msg.getContent() != null && StartupConfig.getStartupProcessed()) {
            entryPropositionProcessor.add(msg);
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
        log.debug(String.format("END >> Message received in Request Channel with Correlation ID [%s]", msg.getCorrelationID()));
    }

    private void processGetDocumentRequest(DistributionMessage<DocumentProposition<?>> msg) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
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

    private void processIntegrityRequest(DistributionMessage<DocumentProposition<?>> msg) {
        // SEND A MESSAGE FOR EACH DOCUMENT TYPE
        try {
            // PARTICIPANT DOCUMENT
            List<Participant> items = participantNodeService.findAll();
            Boolean validation = participantNodeService.validate(items);
            if (!validation) {
                participantNodeService.corruptionDetected(msg, Participant.class, responseChannel);
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
                productNodeService.corruptionDetected(msg, Product.class, responseChannel);
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
                orderNodeService.corruptionDetected(msg, Order.class, responseChannel);
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
                rechargeCreditNodeService.corruptionDetected(msg, RechargeCredit.class, responseChannel);
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
                paymentNodeService.corruptionDetected(msg, Payment.class, responseChannel);
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

    private void waitingForDistributionProcess() {
        try {
            while(!distributionProcessor.isEmpty()) {
                log.info("Waiting for all distribution message processing done");
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            log.error(e.getMessage());
        }
    }
}
