package com.gist.guild.node.core.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.gist.guild.commons.exception.GistGuildGenericException;
import com.gist.guild.commons.exception.GistGuildInsufficientCreditException;
import com.gist.guild.commons.exception.GistGuildInsufficientQuantityException;
import com.gist.guild.commons.message.DistributionEventType;
import com.gist.guild.commons.message.DistributionMessage;
import com.gist.guild.commons.message.entity.Communication;
import com.gist.guild.commons.message.entity.Document;
import com.gist.guild.commons.message.entity.DocumentProposition;
import com.gist.guild.commons.message.entity.RechargeCreditType;
import com.gist.guild.node.core.configuration.MessageProperties;
import com.gist.guild.node.core.document.*;
import com.gist.guild.node.core.repository.ParticipantRepository;
import com.gist.guild.node.core.service.EntryPropositionProcessor;
import com.gist.guild.node.core.service.NodeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.DelayQueue;

@Slf4j
@Service
public class EntryPropositionProcessorImpl implements EntryPropositionProcessor {
    @Autowired
    MessageChannel responseChannel;

    @Autowired
    MessageProperties messageProperties;

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

    @Value("${spring.application.name}")
    private String instanceName;

    private static final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private DelayQueue<DistributionMessage<DocumentProposition<?>>> messageDelayQueue = new DelayQueue<DistributionMessage<DocumentProposition<?>>>();

    public void add(DistributionMessage<DocumentProposition<?>> msg){
        log.debug(String.format("DocumentProposition [%s] in queue",msg.getCorrelationID())); messageDelayQueue.add(msg);
    }

    @Override
    public Boolean isEmpty() {
        return messageDelayQueue.isEmpty();
    }

    @Scheduled(fixedDelay = 250)
    public void process() throws InterruptedException {
        if(messageDelayQueue.isEmpty()){
            return;
        }
        DistributionMessage<DocumentProposition<?>> msg = messageDelayQueue.take();
        log.debug(String.format("DocumentProposition [%s] START processing",msg.getCorrelationID()));
        try {
            List<Document> items = new ArrayList<>();
            Class documentClass = null;
            switch (msg.getContent().getDocumentPropositionType()) {
                case USER_REGISTRATION:
                    Participant participant = participantNodeService.add(mapper.readValue(mapper.writeValueAsString(msg.getContent().getDocument()), com.gist.guild.commons.message.entity.Participant.class));
                    documentClass = com.gist.guild.commons.message.entity.Participant.class;
                    items.add(participant);
                    log.info(String.format("Participant with ID [%s] correctly validated and ingested", participant.getId()));
                    sendNewParticipantCommunication(participant);
                    if(participant.getNewAdministratorTempPassword() != null && participant.getNewAdministratorTempPassword() != "") {
                        sendNewAdministratorCommunication(participant);
                    }
                    sendResponseMessage(msg, items, documentClass);
                    break;
                case PRODUCT_REGISTRATION:
                    Product product = productNodeService.add(mapper.readValue(mapper.writeValueAsString(msg.getContent().getDocument()), com.gist.guild.commons.message.entity.Product.class));
                    documentClass = com.gist.guild.commons.message.entity.Product.class;
                    items.add(product);
                    log.info(String.format("Product with ID [%s] correctly validated and ingested", product.getId()));
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
                    log.info(String.format("Order with ID [%s] correctly validated and ingested", order.getId()));
                    sendResponseMessage(msg, items, documentClass);
                    sendNewOrderCommunication(order);
                    break;
                case RECHARGE_USER_CREDIT:
                    RechargeCredit rechargeCredit = rechargeCreditNodeService.add(mapper.readValue(mapper.writeValueAsString(msg.getContent().getDocument()), com.gist.guild.commons.message.entity.RechargeCredit.class));
                    documentClass = com.gist.guild.commons.message.entity.RechargeCredit.class;
                    items.add(rechargeCredit);
                    log.info(String.format("RechargeCredit with ID [%s] correctly validated and ingested", rechargeCredit.getId()));
                    sendResponseMessage(msg, items, documentClass);
                    sendNewRechargeCreditCommunication(rechargeCredit);
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
                    log.info(String.format("Payment with ID [%s] correctly validated and ingested", payment.getId()));
                    sendResponseMessage(msg, items, documentClass);

                    List<RechargeCredit> rechargeCreditList = rechargeCreditNodeService.findAll();
                    Collections.reverse(rechargeCreditList);
                    items.clear();
                    items.add(rechargeCreditList.iterator().next());
                    documentClass = com.gist.guild.commons.message.entity.RechargeCredit.class;
                    sendResponseMessage(msg, items, documentClass);
                    break;
            }
        } catch (GistGuildGenericException | JsonProcessingException e) {
            log.error(e.getMessage());
        }

        log.debug(String.format("DocumentProposition [%s] END processing",msg.getCorrelationID()));
    }

    void sendNewAdministratorCommunication(Participant participant) {
        Communication communication = new Communication();
        communication.setMessage(String.format(messageProperties.getAdminPasswordMessage(),participant.getTelegramUserId(),participant.getNewAdministratorTempPassword()));
        communication.setRecipientTelegramUserId(participant.getTelegramUserId());

        List<Communication> items = new ArrayList<>(1);
        items.add(communication);

        buildCommunicationResponseMessage(items);

        participant.setNewAdministratorTempPassword(null);
    }

    void sendNewRechargeCreditCommunication(RechargeCredit rechargeCredit) {
        if(
                !RechargeCreditType.INVOICE.equals(rechargeCredit.getRechargeUserCreditType())
                && !RechargeCreditType.ADMIN.equals(rechargeCredit.getRechargeUserCreditType())
        ){
            return;
        }

        //MESSAGE TO ADMINISTRATORS
        List<Participant> administrators = participantRepository.findByAdministratorTrue();
        List<Communication> items = new ArrayList<>();
        for (Participant administrator : administrators) {
            if (administrator.getTelegramUserId().equals(rechargeCredit.getCustomerTelegramUserId())) continue;

            String message = null;

            if(RechargeCreditType.INVOICE.equals(rechargeCredit.getRechargeUserCreditType())){
                message = messageProperties.getNewRechargeCreditInvoiceMessage();
            } else if(RechargeCreditType.ADMIN.equals(rechargeCredit.getRechargeUserCreditType())){
                message = messageProperties.getNewRechargeCreditAdminMessage();
            }

            Communication communication = new Communication();
            communication.setMessage(String.format(message,rechargeCredit.getCustomerNickname(),rechargeCredit.getNewCredit()-rechargeCredit.getOldCredit(),rechargeCredit.getNewCredit()));
            communication.setRecipientTelegramUserId(administrator.getTelegramUserId());
            items.add(communication);
        }

        //MESSAGE TO PARTICIPANT
        if(RechargeCreditType.ADMIN.equals(rechargeCredit.getRechargeUserCreditType())) {
            Communication communication = new Communication();
            communication.setMessage(String.format(messageProperties.getNewRechargeCreditParticipantMessage(), rechargeCredit.getNewCredit() - rechargeCredit.getOldCredit(), rechargeCredit.getNewCredit()));
            communication.setRecipientTelegramUserId(rechargeCredit.getCustomerTelegramUserId());
            items.add(communication);
        }

        buildCommunicationResponseMessage(items);
    }

    void sendNewOrderCommunication(Order order) {
        if(order.getCustomerTelegramUserId().equals(order.getProductOwnerTelegramUserId())) return;
        Communication communication = new Communication();
        communication.setMessage(String.format(messageProperties.getNewOrderMessage(),order.getCustomerNickname(),order.getProductName()));
        communication.setRecipientTelegramUserId(order.getProductOwnerTelegramUserId());

        List<Communication> items = new ArrayList<>(1);
        items.add(communication);

        buildCommunicationResponseMessage(items);
    }

    void sendNewParticipantCommunication(Participant participant) {
        Boolean isNewParticipant = (participant.getTimestamp().compareTo(participant.getLastUpdateTimestamp()) == 0);
        if(isNewParticipant) {
            List<Participant> administrators = participantRepository.findByAdministratorTrue();
            List<Communication> items = new ArrayList<>(administrators.size());
            for (Participant administrator : administrators) {
                if (!administrator.getTelegramUserId().equals(participant.getTelegramUserId())) {
                    Communication communication = new Communication();
                    communication.setMessage(String.format(messageProperties.getNewParticipantMessage(), participant.getNickname()));
                    communication.setRecipientTelegramUserId(administrator.getTelegramUserId());
                    items.add(communication);
                }
            }

            buildCommunicationResponseMessage(items);
        }
    }

    private void buildCommunicationResponseMessage(List<Communication> items) {
        DistributionMessage<List<?>> responseMessage = new DistributionMessage<>();
        responseMessage.setCorrelationID(UUID.randomUUID());
        responseMessage.setInstanceName(instanceName);
        responseMessage.setType(DistributionEventType.COMMUNICATION);
        responseMessage.setDocumentClass(Communication.class);
        responseMessage.setContent(items);
        Message<DistributionMessage<List<?>>> responseMsg = MessageBuilder.withPayload(responseMessage).build();
        responseChannel.send(responseMsg);
    }

    private void sendResponseMessage(DistributionMessage<DocumentProposition<?>> msg, List<Document> items, Class documentClass) {
        DistributionMessage<List<?>> responseMessage = new DistributionMessage<>();
        responseMessage.setCorrelationID(msg.getCorrelationID());
        responseMessage.setInstanceName(instanceName);
        responseMessage.setType(DistributionEventType.ENTRY_RESPONSE);
        responseMessage.setDocumentClass(documentClass);
        responseMessage.setContent(items);
        Message<DistributionMessage<List<?>>> responseMsg = MessageBuilder.withPayload(responseMessage).build();
        responseChannel.send(responseMsg);
    }

    private void sendResponseExceptionMessage(DistributionMessage<DocumentProposition<?>> msg, List<Document> items, Class documentClass, GistGuildGenericException e) {
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
}
