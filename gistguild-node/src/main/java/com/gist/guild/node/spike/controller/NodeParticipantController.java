package com.gist.guild.node.spike.controller;

import com.gist.guild.commons.exception.GistGuildGenericException;
import com.gist.guild.commons.message.DistributionEventType;
import com.gist.guild.commons.message.DistributionMessage;
import com.gist.guild.commons.message.DocumentPropositionType;
import com.gist.guild.commons.message.DocumentRepositoryMethodParameter;
import com.gist.guild.commons.message.entity.Communication;
import com.gist.guild.commons.message.entity.DocumentProposition;
import com.gist.guild.commons.message.entity.RechargeCreditType;
import com.gist.guild.node.binding.CorrelationIdCache;
import com.gist.guild.node.core.configuration.MessageProperties;
import com.gist.guild.node.core.configuration.StartupConfig;
import com.gist.guild.node.core.document.*;
import com.gist.guild.node.core.repository.ParticipantRepository;
import com.gist.guild.node.core.repository.RechargeCreditRepository;
import com.gist.guild.node.core.service.NodeService;
import com.gist.guild.node.spike.client.SpikeClient;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.annotation.Output;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.security.Principal;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Log
@Controller
public class NodeParticipantController {
    @Value("${spring.application.name}")
    private String instanceName;

    @Autowired
    ParticipantRepository participantRepository;

    @Autowired
    RechargeCreditRepository rechargeCreditRepository;

    @Autowired
    NodeService<com.gist.guild.commons.message.entity.Participant,Participant> nodeService;

    @Autowired
    SpikeClient spikeClient;

    @Autowired
    CorrelationIdCache correlationIdCache;

    @Autowired
    MessageChannel responseChannel;

    @Autowired
    private MessageProperties messageProperties;

    @GetMapping("/participant")
    public String welcome(Model model) throws GistGuildGenericException {
        List<Participant> items = participantRepository.findAll();
        Iterator<Participant> participantIterator = items.iterator();
        while(participantIterator.hasNext()){
            Participant next = participantIterator.next();
            List<RechargeCredit> rechargeCredits = rechargeCreditRepository.findTopByCustomerTelegramUserIdOrderByTimestampDesc(next.getTelegramUserId());
            if (rechargeCredits.size() > 0) {
                next.setCredit(rechargeCredits.iterator().next().getNewCredit());
            } else {
                next.setCredit(0L);
            }
        }

        model.addAttribute("instanceName", instanceName);
        model.addAttribute("startup", StartupConfig.getStartupProcessed());
        Collections.sort(items);
        Collections.reverse(items);
        model.addAttribute("items", items);
        model.addAttribute("newCommunication", new Communication());
        model.addAttribute("newParticipant", new com.gist.guild.commons.message.entity.Participant());

        return "participant"; //view
    }

    @GetMapping("/participant/{id}")
    public String prepareModifyParticipant(Model model, @PathVariable String id) throws GistGuildGenericException {
        List<Participant> items = participantRepository.findAll();
        com.gist.guild.commons.message.entity.Participant toModify = new com.gist.guild.commons.message.entity.Participant();
        model.addAttribute("instanceName", instanceName);
        Iterator<Participant> participantIterator = items.iterator();

        while(participantIterator.hasNext()){
            Participant next = participantIterator.next();
            if(next.getId().equals(id)){
                toModify = next;
                List<RechargeCredit> rechargeCredits = rechargeCreditRepository.findTopByCustomerTelegramUserIdOrderByTimestampDesc(next.getTelegramUserId());
                if (rechargeCredits.size() > 0) {
                    toModify.setCredit(rechargeCredits.iterator().next().getNewCredit());
                } else {
                    toModify.setCredit(0L);
                }
                break;
            }
        }

        model.addAttribute("startup", StartupConfig.getStartupProcessed());
        Collections.sort(items);
        Collections.reverse(items);
        model.addAttribute("items", items);
        model.addAttribute("newCommunication", new Communication());
        model.addAttribute("newParticipant", toModify);
        return "participant"; //view
    }

    @PostMapping("/participant")
    public String newParticipant(@ModelAttribute com.gist.guild.commons.message.entity.Participant newParticipant, Model model) throws GistGuildGenericException, InterruptedException {
        if(newParticipant.getId() != null){
            List<RechargeCredit> rechargeCredits = rechargeCreditRepository.findTopByCustomerTelegramUserIdOrderByTimestampDesc(newParticipant.getTelegramUserId());
            if (rechargeCredits.size() > 0) {
                long actualCredit = rechargeCredits.iterator().next().getNewCredit();
                if(newParticipant.getCredit() != actualCredit){
                    RechargeCredit newRechargeCredit = new RechargeCredit();
                    newRechargeCredit.setOldCredit(actualCredit);
                    newRechargeCredit.setNewCredit(newParticipant.getCredit());
                    newRechargeCredit.setCustomerNickname(newParticipant.getNickname());
                    newRechargeCredit.setCustomerTelegramUserId(newParticipant.getTelegramUserId());
                    newRechargeCredit.setRechargeUserCreditType(RechargeCreditType.ADMIN);

                    DocumentProposition documentProposition = new DocumentProposition();
                    documentProposition.setDocumentPropositionType(DocumentPropositionType.RECHARGE_USER_CREDIT);
                    documentProposition.setDocumentClass(com.gist.guild.commons.message.entity.RechargeCredit.class.getSimpleName());

                    documentProposition.setDocument(newRechargeCredit);
                    ResponseEntity<DistributionMessage<DocumentProposition>> distributionMessageResponseEntity = spikeClient.itemProposition(documentProposition);
                    try {
                        correlationIdCache.getResult(distributionMessageResponseEntity.getBody().getCorrelationID()).get(10000, TimeUnit.MILLISECONDS);
                    } catch (ExecutionException e) {
                        log.severe(e.getMessage());
                    } catch (TimeoutException e) {
                        log.severe(e.getMessage());
                    }
                }
            }
        }

        DocumentProposition documentProposition = new DocumentProposition();
        documentProposition.setDocumentPropositionType(DocumentPropositionType.USER_REGISTRATION);
        documentProposition.setDocumentClass(com.gist.guild.commons.message.entity.Participant.class.getSimpleName());
        documentProposition.setDocument(newParticipant);
        ResponseEntity<DistributionMessage<DocumentProposition>> distributionMessageResponseEntity = spikeClient.itemProposition(documentProposition);

        try {
            correlationIdCache.getResult(distributionMessageResponseEntity.getBody().getCorrelationID()).get(10000, TimeUnit.MILLISECONDS);
        } catch (ExecutionException e) {
            log.severe(e.getMessage());
        } catch (TimeoutException e) {
            log.severe(e.getMessage());
        }


        List<Participant> items = participantRepository.findAll();
        Iterator<Participant> participantIterator = items.iterator();
        while(participantIterator.hasNext()){
            Participant next = participantIterator.next();
            List<RechargeCredit> rechargeCredits = rechargeCreditRepository.findTopByCustomerTelegramUserIdOrderByTimestampDesc(next.getTelegramUserId());
            if (rechargeCredits.size() > 0) {
                next.setCredit(rechargeCredits.iterator().next().getNewCredit());
            } else {
                next.setCredit(0L);
            }
        }
        model.addAttribute("instanceName", instanceName);
        model.addAttribute("startup", StartupConfig.getStartupProcessed());
        Collections.sort(items);
        Collections.reverse(items);
        model.addAttribute("items", items);
        model.addAttribute("newCommunication", new Communication());
        model.addAttribute("newParticipant", new com.gist.guild.commons.message.entity.Participant());

        return "participant"; //view
    }

    @GetMapping("/communication/{id}")
    public String prepareCommunicationParticipant(Model model, @PathVariable String id) throws GistGuildGenericException {
        List<Participant> items = participantRepository.findAll();
        model.addAttribute("instanceName", instanceName);
        Iterator<Participant> participantIterator = items.iterator();

        while(participantIterator.hasNext()){
            Participant next = participantIterator.next();
            if(next.getId().equals(id)){
                Communication communication = new Communication();
                communication.setRecipientTelegramUserId(next.getTelegramUserId());
                model.addAttribute("newCommunication", communication);
                break;
            }
        }

        model.addAttribute("startup", StartupConfig.getStartupProcessed());
        Collections.sort(items);
        Collections.reverse(items);
        model.addAttribute("items", items);
        model.addAttribute("newParticipant", new com.gist.guild.commons.message.entity.Participant());

        return "participant"; //view
    }

    @PostMapping("/communication")
    public String newCommunication(Principal principal, @ModelAttribute com.gist.guild.commons.message.entity.Communication newCommunication, Model model) throws GistGuildGenericException, InterruptedException {
        Participant administrator = participantRepository.findByTelegramUserId(Long.parseLong(principal.getName())).iterator().next();
        newCommunication.setMessage(String.format(messageProperties.getAdminMessage(),administrator.getNickname(),newCommunication.getMessage()));

        DistributionMessage<List<?>> responseMessage = new DistributionMessage<>();
        responseMessage.setCorrelationID(UUID.randomUUID());
        responseMessage.setInstanceName(instanceName);
        responseMessage.setType(DistributionEventType.COMMUNICATION);
        responseMessage.setDocumentClass(Communication.class);
        List<Communication> communications = new ArrayList<>(1);
        communications.add(newCommunication);
        responseMessage.setContent(communications);
        Message<DistributionMessage<List<?>>> responseMsg = MessageBuilder.withPayload(responseMessage).build();
        responseChannel.send(responseMsg);

        return welcome(model);
    }

}
