package com.gist.guild.gui.binding;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.gist.guild.commons.message.DistributionEventType;
import com.gist.guild.commons.message.DistributionMessage;
import com.gist.guild.commons.message.entity.Document;
import com.gist.guild.commons.message.entity.Participant;
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
    private DocumentAsyncService<Participant> documentAsyncService;

    @StreamListener(target = "distributionChannel")
    public void processDistribution(DistributionMessage<List<?>> msg) {
        log.info(String.format("START >> Message received in Distribution Channel with Correlation ID [%s]", msg.getCorrelationID()));
        if (DistributionEventType.ENTRY_RESPONSE.equals(msg.getType()) && msg.getContent() != null && GuiConcurrenceService.getCorrelationIDs().contains(msg.getCorrelationID())) {
            log.info(String.format("Correlation ID [%s] processed",msg.getCorrelationID()));
            GuiConcurrenceService.getCorrelationIDs().remove(msg.getCorrelationID());
            try {
                for (Object item : msg.getContent()) {
                    // PARTICIPANT DOCUMENT
                    if (msg.getDocumentClass().getSimpleName().equalsIgnoreCase(Participant.class.getSimpleName())) {
                        Participant participant = mapper.readValue(mapper.writeValueAsString(item), Participant.class);
                        log.info(String.format("Participant [%s] created or updated",participant.getMail()));
                        documentAsyncService.putInCache(msg.getCorrelationID(), participant);
                    }
                }
            } catch (JsonProcessingException e) {
                // IT IS NOT A PARTICIPANT DOCUMENT
            }
        } else if (DistributionEventType.CORRUPTION_DETECTED.equals(msg.getType()) && msg.getContent() != null) {
            //FIXNME How ?
        } else if (DistributionEventType.GET_DOCUMENT.equals(msg.getType())){
            log.info(String.format("Correlation ID [%s] processed",msg.getCorrelationID()));
            GuiConcurrenceService.getCorrelationIDs().remove(msg.getCorrelationID());
            if(msg.getContent() == null || msg.getContent().isEmpty()){
                documentAsyncService.putInCache(msg.getCorrelationID(), null);
            } else {
                try {
                    for (Object item : msg.getContent()) {
                        // PARTICIPANT DOCUMENT
                        if (msg.getDocumentClass().getSimpleName().equalsIgnoreCase(Participant.class.getSimpleName())) {
                            Participant participant = mapper.readValue(mapper.writeValueAsString(item), Participant.class);
                            log.info(String.format("Participant [%s] created or updated", participant.getMail()));
                            documentAsyncService.putInCache(msg.getCorrelationID(), participant);
                        }
                    }
                } catch (JsonProcessingException e) {
                    // IT IS NOT A PARTICIPANT DOCUMENT
                }
            }
        }
        log.info(String.format("END >> Message received in Distribution Channel with Correlation ID [%s]", msg.getCorrelationID()));
    }
}
