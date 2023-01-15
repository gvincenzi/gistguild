package com.gist.guild.node.core.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.gist.guild.commons.exception.GistGuildGenericException;
import com.gist.guild.commons.message.DistributionEventType;
import com.gist.guild.commons.message.DistributionMessage;
import com.gist.guild.commons.message.entity.Document;
import com.gist.guild.commons.message.entity.DocumentProposition;
import com.gist.guild.node.binding.CorrelationIdCache;
import com.gist.guild.node.core.configuration.MessageProperties;
import com.gist.guild.node.core.configuration.StartupConfig;
import com.gist.guild.node.core.document.*;
import com.gist.guild.node.core.service.DistributionProcessor;
import com.gist.guild.node.core.service.NodeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.MessageChannel;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.DelayQueue;

@Slf4j
@Service
public class DistributionProcessorImpl implements DistributionProcessor {
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

    @Value("${spring.application.name}")
    private String instanceName;

    @Autowired
    CorrelationIdCache correlationIdCache;

    private static final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private DelayQueue<DistributionMessage<List<?>>> messageDelayQueue = new DelayQueue<DistributionMessage<List<?>>>();

    public void add(DistributionMessage<List<?>> msg){
        log.info(String.format("DistributionMessage [%s] in queue",msg.getCorrelationID()));
        messageDelayQueue.add(msg);
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
        DistributionMessage<List<?>> msg = messageDelayQueue.take();
        log.info(String.format("DistributionMessage [%s] START processing",msg.getCorrelationID()));

        if (DistributionEventType.ENTRY_RESPONSE.equals(msg.getType()) && msg.getContent() != null && !instanceName.equals(msg.getInstanceName()) && StartupConfig.getStartupProcessed()) {
            try {
                if (Participant.class.getSimpleName().equalsIgnoreCase(msg.getDocumentClass().getSimpleName())) {
                    for (Object item : msg.getContent()) {
                        // PARTICIPANT DOCUMENT
                        if (participantNodeService.updateLocal(mapper.readValue(mapper.writeValueAsString(item), com.gist.guild.commons.message.entity.Participant.class))) {
                            log.info("New participant correctly validated and ingested");
                        } else {
                            participantNodeService.corruptionDetected(msg, Participant.class, responseChannel);
                        }
                    }
                }
                if (Product.class.getSimpleName().equalsIgnoreCase(msg.getDocumentClass().getSimpleName())) {
                    for (Object item : msg.getContent()) {
                        // PRODUCT DOCUMENT
                        if (productNodeService.updateLocal(mapper.readValue(mapper.writeValueAsString(item), com.gist.guild.commons.message.entity.Product.class))) {
                            log.info("Product correctly updated in local registry");
                        } else {
                            productNodeService.corruptionDetected(msg, Product.class, responseChannel);
                        }
                    }
                }
                if (Order.class.getSimpleName().equalsIgnoreCase(msg.getDocumentClass().getSimpleName())) {
                    for (Object item : msg.getContent()) {
                        // ORDER DOCUMENT
                        if (orderNodeService.updateLocal(mapper.readValue(mapper.writeValueAsString(item), com.gist.guild.commons.message.entity.Order.class))) {
                            log.info("Order correctly updated in local registry");
                        } else {
                            orderNodeService.corruptionDetected(msg, Order.class, responseChannel);
                        }
                    }
                }
                if (RechargeCredit.class.getSimpleName().equalsIgnoreCase(msg.getDocumentClass().getSimpleName())) {
                    for (Object item : msg.getContent()) {
                        // RECAHRGE_CREDIT DOCUMENT
                        if (rechargeCreditNodeService.updateLocal(mapper.readValue(mapper.writeValueAsString(item), com.gist.guild.commons.message.entity.RechargeCredit.class))) {
                            log.info("RechargeCredit correctly updated in local registry");
                        } else {
                            rechargeCreditNodeService.corruptionDetected(msg, RechargeCredit.class, responseChannel);
                        }
                    }
                }
                if (Payment.class.getSimpleName().equalsIgnoreCase(msg.getDocumentClass().getSimpleName())) {
                    for (Object item : msg.getContent()) {
                        // PAYMENT DOCUMENT
                        if (paymentNodeService.updateLocal(mapper.readValue(mapper.writeValueAsString(item), com.gist.guild.commons.message.entity.Payment.class))) {
                            log.info("Payment correctly updated in local registry");
                        } else {
                            paymentNodeService.corruptionDetected(msg, Payment.class, responseChannel);
                        }
                    }
                }
            } catch (GistGuildGenericException | JsonProcessingException e) {
                log.error(e.getMessage());
            }
        } else if (DistributionEventType.INTEGRITY_VERIFICATION.equals(msg.getType()) && msg.getContent() != null && !instanceName.equals(msg.getInstanceName())) {
            // A MESSAGE RECEIVED FOR EACH DOCUMENT TYPE
            Class classProcessing = null;
            NodeService nodeProcessing = null;
            try {
                if (Participant.class.getSimpleName().equalsIgnoreCase(msg.getDocumentClass().getSimpleName())) {
                    classProcessing = Participant.class;
                    nodeProcessing = participantNodeService;
                    List<com.gist.guild.commons.message.entity.Participant> participants = new ArrayList(msg.getContent().size());
                    for (Object document : msg.getContent()) {
                        participants.add(mapper.readValue(mapper.writeValueAsString(document), com.gist.guild.commons.message.entity.Participant.class));
                    }
                    participantNodeService.init(participants);
                    StartupConfig.startupParticipantProcessed = Boolean.TRUE;
                } else if (Product.class.getSimpleName().equalsIgnoreCase(msg.getDocumentClass().getSimpleName())) {
                    classProcessing = Product.class;
                    nodeProcessing = productNodeService;
                    List<com.gist.guild.commons.message.entity.Product> products = new ArrayList(msg.getContent().size());
                    for (Object document : msg.getContent()) {
                        products.add(mapper.readValue(mapper.writeValueAsString(document), com.gist.guild.commons.message.entity.Product.class));
                    }
                    productNodeService.init(products);
                    StartupConfig.startupProductProcessed = Boolean.TRUE;
                } else if (Order.class.getSimpleName().equalsIgnoreCase(msg.getDocumentClass().getSimpleName())) {
                    classProcessing = Order.class;
                    nodeProcessing = orderNodeService;
                    List<com.gist.guild.commons.message.entity.Order> orders = new ArrayList(msg.getContent().size());
                    for (Object document : msg.getContent()) {
                        orders.add(mapper.readValue(mapper.writeValueAsString(document), com.gist.guild.commons.message.entity.Order.class));
                    }
                    orderNodeService.init(orders);
                    StartupConfig.startupOrderProcessed = Boolean.TRUE;
                } else if (RechargeCredit.class.getSimpleName().equalsIgnoreCase(msg.getDocumentClass().getSimpleName())) {
                    classProcessing = RechargeCredit.class;
                    nodeProcessing = rechargeCreditNodeService;
                    List<com.gist.guild.commons.message.entity.RechargeCredit> rechargeCredits = new ArrayList(msg.getContent().size());
                    for (Object document : msg.getContent()) {
                        rechargeCredits.add(mapper.readValue(mapper.writeValueAsString(document), com.gist.guild.commons.message.entity.RechargeCredit.class));
                    }
                    rechargeCreditNodeService.init(rechargeCredits);
                    StartupConfig.startupRechargeCreditProcessed = Boolean.TRUE;
                } else if (Payment.class.getSimpleName().equalsIgnoreCase(msg.getDocumentClass().getSimpleName())) {
                    classProcessing = Payment.class;
                    nodeProcessing = paymentNodeService;
                    List<com.gist.guild.commons.message.entity.Payment> payments = new ArrayList(msg.getContent().size());
                    for (Object document : msg.getContent()) {
                        payments.add(mapper.readValue(mapper.writeValueAsString(document), com.gist.guild.commons.message.entity.Payment.class));
                    }
                    paymentNodeService.init(payments);
                    StartupConfig.startupPaymentProcessed = Boolean.TRUE;
                }

                log.info(String.format("Integrity verification for [%s] correctly validated and ingested",classProcessing.getSimpleName()));

                if (Boolean.TRUE.equals(StartupConfig.getStartupProcessed())) {
                    log.info("Startup process for this node has been correctly terminated");
                }
            } catch (GistGuildGenericException | JsonProcessingException e) {
                log.error(e.getMessage());
                nodeProcessing.corruptionDetected(msg, classProcessing, responseChannel);
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

        //PUT IN CACHE FOR ADMINISTRATION GUI REQUESTS
        correlationIdCache.putInCache(msg.getCorrelationID(), msg.getExceptions());

        log.info(String.format("DistributionMessage [%s] END processing",msg.getCorrelationID()));
    }
}
