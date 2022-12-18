package com.gist.guild.gui.binding;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.gist.guild.commons.message.DistributionEventType;
import com.gist.guild.commons.message.DistributionMessage;
import com.gist.guild.commons.message.entity.*;
import com.gist.guild.gui.service.GuiConcurrenceService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;

import java.util.List;

@Data
@Slf4j
@EnableBinding(MQBinding.class)
public class MQListener {
    private static final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    @Autowired
    private DocumentAsyncService<Participant> participantAsyncService;

    @Autowired
    private DocumentAsyncService<Product> productAsyncService;

    @Autowired
    private DocumentAsyncService<Order> orderAsyncService;

    @Autowired
    private DocumentAsyncService<RechargeCredit> rechargeCreditAsyncService;

    @Autowired
    private DocumentAsyncService<Payment> paymentAsyncService;

    @StreamListener(target = "distributionChannel")
    public void processDistribution(DistributionMessage<List<?>> msg) {
        log.info(String.format("START >> Message received in Distribution Channel with Correlation ID [%s]", msg.getCorrelationID()));
        if (DistributionEventType.ENTRY_RESPONSE.equals(msg.getType()) && msg.getContent() != null && GuiConcurrenceService.getCorrelationIDs().contains(msg.getCorrelationID())) {
            log.info(String.format("Correlation ID [%s] processed", msg.getCorrelationID()));
            GuiConcurrenceService.getCorrelationIDs().remove(msg.getCorrelationID());
            putInCache(msg);
        } else if (DistributionEventType.CORRUPTION_DETECTED.equals(msg.getType()) && msg.getContent() != null) {
            //FIXNME How ?
        } else if (DistributionEventType.GET_DOCUMENT.equals(msg.getType())) {
            log.info(String.format("Correlation ID [%s] processed", msg.getCorrelationID()));
            GuiConcurrenceService.getCorrelationIDs().remove(msg.getCorrelationID());
            putInCache(msg);
        } else if (DistributionEventType.BUSINESS_EXCEPTION.equals(msg.getType()) && msg.getContent() != null) {
            log.info(String.format("Correlation ID [%s] processed with exception", msg.getCorrelationID()));
            GuiConcurrenceService.getCorrelationIDs().remove(msg.getCorrelationID());
            putInCache(msg);
        }
        log.info(String.format("END >> Message received in Distribution Channel with Correlation ID [%s]", msg.getCorrelationID()));
    }

    private void putInCache(DistributionMessage<List<?>> msg) {
        try {
            if (Participant.class.getSimpleName().equalsIgnoreCase(msg.getDocumentClass().getSimpleName())) {
                if (msg.getContent() == null || msg.getContent().isEmpty()) {
                    participantAsyncService.putInCache(msg.getCorrelationID(), null, msg.getExceptions());
                } else for (Object item : msg.getContent()) {
                    // PARTICIPANT DOCUMENT
                    Participant participant = mapper.readValue(mapper.writeValueAsString(item), Participant.class);
                    participantAsyncService.putInCache(msg.getCorrelationID(), participant, msg.getExceptions());
                }
            } else if (Product.class.getSimpleName().equalsIgnoreCase(msg.getDocumentClass().getSimpleName())) {
                if (msg.getContent() == null || msg.getContent().isEmpty()) {
                    productAsyncService.putInCache(msg.getCorrelationID(), null, msg.getExceptions());
                } else for (Object item : msg.getContent()) {
                    // PRODUCT DOCUMENT
                    Product product = mapper.readValue(mapper.writeValueAsString(item), Product.class);
                    productAsyncService.putInCache(msg.getCorrelationID(), product, msg.getExceptions());
                }
            } else if (Order.class.getSimpleName().equalsIgnoreCase(msg.getDocumentClass().getSimpleName())) {
                if (msg.getContent() == null || msg.getContent().isEmpty()) {
                    orderAsyncService.putInCache(msg.getCorrelationID(), null, msg.getExceptions());
                } else for (Object item : msg.getContent()) {
                    // ORDER DOCUMENT
                    Order order = mapper.readValue(mapper.writeValueAsString(item), Order.class);
                    orderAsyncService.putInCache(msg.getCorrelationID(), order, msg.getExceptions());
                }
            } else if (RechargeCredit.class.getSimpleName().equalsIgnoreCase(msg.getDocumentClass().getSimpleName())) {
                if (msg.getContent() == null || msg.getContent().isEmpty()) {
                    rechargeCreditAsyncService.putInCache(msg.getCorrelationID(), null, msg.getExceptions());
                } else for (Object item : msg.getContent()) {
                    // RECHARGE_CREDIT DOCUMENT
                    RechargeCredit rechargeCredit = mapper.readValue(mapper.writeValueAsString(item), RechargeCredit.class);
                    rechargeCreditAsyncService.putInCache(msg.getCorrelationID(), rechargeCredit, msg.getExceptions());
                }
            } else if (Payment.class.getSimpleName().equalsIgnoreCase(msg.getDocumentClass().getSimpleName())) {
                if (msg.getContent() == null || msg.getContent().isEmpty()) {
                    paymentAsyncService.putInCache(msg.getCorrelationID(), null, msg.getExceptions());
                } else for (Object item : msg.getContent()) {
                    // PAYMENT DOCUMENT
                    Payment payment = mapper.readValue(mapper.writeValueAsString(item), Payment.class);
                    paymentAsyncService.putInCache(msg.getCorrelationID(), payment, msg.getExceptions());
                }
            }
        } catch (JsonProcessingException e) {
        }
    }
}
