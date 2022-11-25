package com.gist.guild.distribution.binding;

import com.gist.guild.commons.message.DistributionEventType;
import com.gist.guild.commons.message.DistributionMessage;
import com.gist.guild.commons.message.entity.Document;
import com.gist.guild.distribution.delivery.service.DistributionConcurrenceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@EnableBinding(MQBinding.class)
public class MQListener {
    @Autowired
    MessageChannel distributionChannel;

    @StreamListener(target = "responseChannel")
    public void processEntryResponse(DistributionMessage<List<?>> msg) {
        log.info(String.format("START >> Message received in Response Channel with Correlation ID [%s]",msg.getCorrelationID()));
        if(DistributionEventType.ENTRY_RESPONSE.equals(msg.getType()) && msg.getContent() != null){
            log.info(String.format("Correlation ID [%s] processed",msg.getCorrelationID()));
            DistributionConcurrenceService.getCorrelationIDs().remove(msg.getCorrelationID());
            Message<DistributionMessage<List<?>>> message = MessageBuilder.withPayload(msg).build();
            distributionChannel.send(message);
        } else if(DistributionEventType.INTEGRITY_VERIFICATION.equals(msg.getType()) && msg.getContent() != null){
            log.info(String.format("Correlation ID [%s] processed",msg.getCorrelationID()));
            DistributionConcurrenceService.getCorrelationIDs().remove(msg.getCorrelationID());
            Message<DistributionMessage<List<?>>> message = MessageBuilder.withPayload(msg).build();
            distributionChannel.send(message);
        } else if(DistributionEventType.CORRUPTION_DETECTED.equals(msg.getType())){
            log.info(String.format("Correlation ID [%s] processed",msg.getCorrelationID()));
            DistributionConcurrenceService.getCorrelationIDs().remove(msg.getCorrelationID());

            List<Document> documents = new ArrayList<>();
            documents.add(Document.getItemCorruption());
            msg.setContent(documents);
            Message<DistributionMessage<List<?>>> message = MessageBuilder.withPayload(msg).build();
            distributionChannel.send(message);
        } else if(DistributionEventType.GET_DOCUMENT.equals(msg.getType())){
            log.info(String.format("Correlation ID [%s] processed",msg.getCorrelationID()));
            DistributionConcurrenceService.getCorrelationIDs().remove(msg.getCorrelationID());
            Message<DistributionMessage<List<?>>> message = MessageBuilder.withPayload(msg).build();
            distributionChannel.send(message);
        }
        log.info(String.format("END >> Message received in Response Channel with Correlation ID [%s]",msg.getCorrelationID()));
    }
}
