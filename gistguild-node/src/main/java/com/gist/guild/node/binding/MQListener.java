package com.gist.guild.node.binding;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.gist.guild.commons.exception.GistGuildGenericException;
import com.gist.guild.commons.message.DistributionEventType;
import com.gist.guild.commons.message.DistributionMessage;
import com.gist.guild.commons.message.DocumentRepositoryMethodParameter;
import com.gist.guild.commons.message.entity.Document;
import com.gist.guild.commons.message.entity.DocumentProposition;
import com.gist.guild.node.core.configuration.StartupConfig;
import com.gist.guild.node.core.document.Participant;
import com.gist.guild.node.core.repository.ParticipantRepository;
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
    ParticipantRepository participantRepository;

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
        if (DistributionEventType.ENTRY_PROPOSITION.equals(msg.getType()) && msg.getContent() != null && StartupConfig.startupProcessed) {
            try {
                Participant participant = null;
                switch (msg.getContent().getDocumentPropositionType()) {
                    case USER_REGISTRATION:
                        participant = participantNodeService.add(mapper.readValue(mapper.writeValueAsString(msg.getContent().getDocument()), com.gist.guild.commons.message.entity.Participant.class));
                        documentClass = com.gist.guild.commons.message.entity.Participant.class;
                        items.add(participant);
                        log.info(String.format("New item with ID [%s] correctly validated and ingested", participant.getId()));
                        break;
                }
            } catch (GistGuildGenericException | JsonProcessingException e) {
                log.error(e.getMessage());
            }

            DistributionMessage<List<?>> responseMessage = new DistributionMessage<>();
            responseMessage.setCorrelationID(msg.getCorrelationID());
            responseMessage.setInstanceName(instanceName);
            responseMessage.setType(DistributionEventType.ENTRY_RESPONSE);
            responseMessage.setDocumentClass(documentClass);
            responseMessage.setContent(items);
            Message<DistributionMessage<List<?>>> responseMsg = MessageBuilder.withPayload(responseMessage).build();
            responseChannel.send(responseMsg);
        } else if (DistributionEventType.INTEGRITY_VERIFICATION.equals(msg.getType())) {
            processIntegrityRequest(msg);
        } else if (DistributionEventType.GET_DOCUMENT.equals(msg.getType())) {
            try {
                processGetDocumentRequest(msg);
            } catch (NoSuchMethodException e) {
                log.error(e.getMessage());
            } catch (InvocationTargetException e) {
                log.error(e.getMessage());
            } catch (IllegalAccessException e) {
                log.error(e.getMessage());
            }
        }
        log.info(String.format("END >> Message received in Request Channel with Correlation ID [%s]", msg.getCorrelationID()));
    }

    private void processGetDocumentRequest(DistributionMessage<DocumentProposition> msg) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        //Populate DocumentRepositoryMethodParameters arrays
        Class<?>[] arrayParamType = new Class<?>[msg.getParams().size()];
        Object[] arrayParamValue = new Object[msg.getParams().size()];
        int index = 0;
        for (DocumentRepositoryMethodParameter param: msg.getParams()) {
            arrayParamType[index] = param.getType();
            arrayParamValue[index++] = param.getValue();
        }

        List<?> items = null;

        // PARTICIPANT DOCUMENT
        if (msg.getDocumentClass().getSimpleName().equalsIgnoreCase(Participant.class.getSimpleName())) {
            Method repositoryMethod = ParticipantRepository.class.getDeclaredMethod(msg.getDocumentRepositoryMethod(),arrayParamType);
            items = (List<Participant>) repositoryMethod.invoke(participantRepository,arrayParamValue);
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
                corruptionDetected(msg);
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
        } catch (GistGuildGenericException e) {
            log.error(e.getMessage());
        }
    }

    @StreamListener(target = "distributionChannel")
    public void processDistribution(DistributionMessage<List<?>> msg) {
        log.info(String.format("START >> Message received in Distribution Channel with Correlation ID [%s]", msg.getCorrelationID()));
        if (DistributionEventType.ENTRY_RESPONSE.equals(msg.getType()) && msg.getContent() != null && !instanceName.equals(msg.getInstanceName()) && StartupConfig.startupProcessed) {
            try {
                try {
                    for (Object item : msg.getContent()) {
                        // PARTICIPANT DOCUMENT
                        if (msg.getDocumentClass().getSimpleName().equalsIgnoreCase(Participant.class.getSimpleName())) {
                            if (participantNodeService.updateLocal(mapper.readValue(mapper.writeValueAsString(item), com.gist.guild.commons.message.entity.Participant.class))) {
                                log.info(String.format("New item with ID [%s] correctly validated and ingested", ((com.gist.guild.commons.message.entity.Participant) item).getId()));
                            } else {
                                corruptionDetected(msg);
                            }
                        }
                    }
                } catch (JsonProcessingException e) {
                    // IT IS NOT A PARTICIPANT DOCUMENT
                }
            } catch (GistGuildGenericException e) {
                log.error(e.getMessage());
            }
        } else if (DistributionEventType.INTEGRITY_VERIFICATION.equals(msg.getType()) && msg.getContent() != null && !instanceName.equals(msg.getInstanceName())) {
            // A MESSAGE RECEIVED FOR EACH DOCUMENT TYPE
            try {

                try {
                    List<com.gist.guild.commons.message.entity.Participant> participants = new ArrayList(msg.getContent().size());
                    if (msg.getDocumentClass().getSimpleName().equalsIgnoreCase(Participant.class.getSimpleName())) {
                        for (Object document : msg.getContent()) {
                            // PARTICIPANT DOCUMENT
                            participants.add(mapper.readValue(mapper.writeValueAsString(document), com.gist.guild.commons.message.entity.Participant.class));
                        }
                        participantNodeService.init(participants);
                    }

                    // FIXME How to know if the message received for each document type is the last ? We need un boolean for each document type
                    if (Boolean.FALSE.equals(StartupConfig.startupProcessed)) {
                        log.info("Startup process for this node has been correctly terminated");
                        StartupConfig.startupProcessed = Boolean.TRUE;
                    }

                    log.info("Integrity verification correctly validated and ingested");
                } catch (JsonProcessingException e) {
                    log.error(e.getMessage());
                    log.error("Integrity verification failed");
                }
            } catch (GistGuildGenericException e) {
                log.error(e.getMessage());
                corruptionDetected(msg);
            }
        } else if (DistributionEventType.CORRUPTION_DETECTED.equals(msg.getType()) && msg.getContent() != null && StartupConfig.startupProcessed) {
            //FIXNME How ?
        }
        log.info(String.format("END >> Message received in Distribution Channel with Correlation ID [%s]", msg.getCorrelationID()));
    }

    private void corruptionDetected(DistributionMessage<?> msg) {
        log.error(String.format("Corruption detected : send message with Correlation ID [%s]", msg.getCorrelationID()));
        DistributionMessage<List<Document>> responseMessage = new DistributionMessage<>();
        responseMessage.setCorrelationID(msg.getCorrelationID());
        responseMessage.setInstanceName(instanceName);
        responseMessage.setType(DistributionEventType.CORRUPTION_DETECTED);
        Message<DistributionMessage<List<Document>>> responseMsg = MessageBuilder.withPayload(responseMessage).build();
        responseChannel.send(responseMsg);
    }
}
