package com.gist.guild.distribution.delivery.service.valence.impl;

import com.gist.guild.commons.message.DistributionEventType;
import com.gist.guild.commons.message.DistributionMessage;
import com.gist.guild.commons.message.entity.Document;
import com.gist.guild.distribution.delivery.service.DistributionConcurrenceService;
import com.gist.guild.distribution.domain.service.valence.DeliveryValenceService;
import com.gist.guild.distribution.exception.DistributionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
public class DeliveryValenceServiceImpl implements DeliveryValenceService {
    @Autowired
    MessageChannel requestChannel;

    @Autowired
    MessageChannel requestIntegrityChannel;

    @Autowired
    DistributionConcurrenceService distributionConcurrenceService;

    @Override
    public DistributionMessage<Document> propose(Document proposition) throws DistributionException {
        distributionConcurrenceService.waitingForLastCorrelationIDProcessing();

        DistributionMessage<Document> distributionMessage = new DistributionMessage<>();
        distributionMessage.setCorrelationID(UUID.randomUUID());
        distributionMessage.setType(DistributionEventType.ENTRY_PROPOSITION);
        distributionMessage.setContent(proposition);
        Message<DistributionMessage<Document>> msg = MessageBuilder.withPayload(distributionMessage).build();
        requestChannel.send(msg);
        DistributionConcurrenceService.setLastBlockingCorrelationID(distributionMessage.getCorrelationID());
        DistributionConcurrenceService.getCorrelationIDs().add(DistributionConcurrenceService.getLastBlockingCorrelationID());
        log.info(String.format("Correlation ID [%s] waiting for processing",distributionMessage.getCorrelationID().toString()));
        return distributionMessage;
    }

    private DistributionMessage<Void> getVoidDistributionMessage(DistributionEventType listEntriesRequest) throws DistributionException {
        distributionConcurrenceService.waitingForLastCorrelationIDProcessing();

        DistributionMessage<Void> distributionMessage = new DistributionMessage<>();
        distributionMessage.setCorrelationID(UUID.randomUUID());
        distributionMessage.setType(listEntriesRequest);
        Message<DistributionMessage<Void>> msg = MessageBuilder.withPayload(distributionMessage).build();
        requestIntegrityChannel.send(msg);
        DistributionConcurrenceService.setLastBlockingCorrelationID(distributionMessage.getCorrelationID());
        DistributionConcurrenceService.getCorrelationIDs().add(DistributionConcurrenceService.getLastBlockingCorrelationID());
        return distributionMessage;
    }

    @Override
    public DistributionMessage<Void> sendIntegrityVerificationRequest() throws DistributionException {
        return getVoidDistributionMessage(DistributionEventType.INTEGRITY_VERIFICATION);
    }
}
