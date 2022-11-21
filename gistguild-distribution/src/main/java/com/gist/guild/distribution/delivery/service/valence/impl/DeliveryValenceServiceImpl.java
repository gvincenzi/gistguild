package com.gist.guild.distribution.delivery.service.valence.impl;

import com.gist.guild.commons.message.DistributionEventType;
import com.gist.guild.commons.message.DistributionMessage;
import com.gist.guild.commons.message.DocumentRepositoryMethodParameter;
import com.gist.guild.commons.message.entity.DocumentProposition;
import com.gist.guild.distribution.delivery.service.DistributionConcurrenceService;
import com.gist.guild.distribution.domain.service.valence.DeliveryValenceService;
import com.gist.guild.distribution.exception.DistributionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class DeliveryValenceServiceImpl implements DeliveryValenceService {
    public static final String ENTITY_PACKAGE = "com.gist.guild.commons.message.entity.";

    @Autowired
    MessageChannel requestChannel;

    @Autowired
    DistributionConcurrenceService distributionConcurrenceService;

    @Override
    public DistributionMessage<DocumentProposition> propose(DocumentProposition proposition) throws DistributionException, ClassNotFoundException {
        distributionConcurrenceService.waitingForLastCorrelationIDProcessing();

        DistributionMessage<DocumentProposition> distributionMessage = new DistributionMessage<>();
        distributionMessage.setCorrelationID(UUID.randomUUID());
        distributionMessage.setType(DistributionEventType.ENTRY_PROPOSITION);
        distributionMessage.setDocumentClass(Class.forName(ENTITY_PACKAGE +proposition.getDocumentClass()));
        distributionMessage.setContent(proposition);
        Message<DistributionMessage<DocumentProposition>> msg = MessageBuilder.withPayload(distributionMessage).build();
        requestChannel.send(msg);
        DistributionConcurrenceService.setLastBlockingCorrelationID(distributionMessage.getCorrelationID());
        DistributionConcurrenceService.getCorrelationIDs().add(DistributionConcurrenceService.getLastBlockingCorrelationID());
        log.info(String.format("Correlation ID [%s] waiting for processing",distributionMessage.getCorrelationID().toString()));
        return distributionMessage;
    }

    private DistributionMessage<Void> getVoidDistributionMessage(DistributionEventType listEntriesRequest, Class documentClass, String documentRepositoryMethod, List<DocumentRepositoryMethodParameter> params) throws DistributionException {
        distributionConcurrenceService.waitingForLastCorrelationIDProcessing();

        DistributionMessage<Void> distributionMessage = new DistributionMessage<>();
        distributionMessage.setCorrelationID(UUID.randomUUID());
        distributionMessage.setType(listEntriesRequest);
        distributionMessage.setDocumentClass(documentClass);
        distributionMessage.setDocumentRepositoryMethod(documentRepositoryMethod);
        distributionMessage.setParams(params);
        Message<DistributionMessage<Void>> msg = MessageBuilder.withPayload(distributionMessage).build();
        requestChannel.send(msg);
        DistributionConcurrenceService.setLastBlockingCorrelationID(distributionMessage.getCorrelationID());
        DistributionConcurrenceService.getCorrelationIDs().add(DistributionConcurrenceService.getLastBlockingCorrelationID());
        return distributionMessage;
    }

    @Override
    public DistributionMessage<Void> sendIntegrityVerificationRequest() throws DistributionException {
        return getVoidDistributionMessage(DistributionEventType.INTEGRITY_VERIFICATION, null, null,null);
    }

    @Override
    public DistributionMessage<Void> sendDocumentClassRequest(String documentClass, String documentRepositoryMethod, List<DocumentRepositoryMethodParameter> params) throws DistributionException, ClassNotFoundException {
        return getVoidDistributionMessage(DistributionEventType.GET_DOCUMENT, Class.forName(ENTITY_PACKAGE + documentClass), documentRepositoryMethod, params);
    }
}
