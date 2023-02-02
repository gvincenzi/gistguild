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
        log.info(String.format("START >> Message received in Response Channel with Correlation ID [%s] Type [%s]",msg.getCorrelationID(), msg.getType().name()));
        if(DistributionEventType.CORRUPTION_DETECTED.equals(msg.getType())){
            log.debug(String.format("Correlation ID [%s] Type [%s] processed",msg.getCorrelationID(), msg.getType().name()));
            DistributionConcurrenceService.getCorrelationIDs().remove(msg.getCorrelationID());

            List<Document> documents = new ArrayList<>();
            documents.add(Document.getItemCorruption());
            msg.setContent(documents);
            Message<DistributionMessage<List<?>>> message = MessageBuilder.withPayload(msg).build();
            distributionChannel.send(message);
        } else {
            process(msg);
        }
        log.info(String.format("Message sent in Distribution Channel with Correlation ID [%s]",msg.getCorrelationID()));
        log.info(String.format("END >> Message received in Response Channel with Correlation ID [%s]",msg.getCorrelationID()));
    }

    private void process(DistributionMessage<List<?>> msg) {
        log.debug(String.format("Correlation ID [%s] Type [%s] processed", msg.getCorrelationID(), msg.getType().name()));
        DistributionConcurrenceService.getCorrelationIDs().remove(msg.getCorrelationID());
        Message<DistributionMessage<List<?>>> message = MessageBuilder.withPayload(msg).build();
        distributionChannel.send(message);
    }
}
