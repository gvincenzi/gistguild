package com.gist.guild.gui.service.impl;

import com.gist.guild.commons.message.DistributionMessage;
import com.gist.guild.commons.message.DocumentPropositionType;
import com.gist.guild.commons.message.DocumentRepositoryMethodParameter;
import com.gist.guild.commons.message.entity.DocumentProposition;
import com.gist.guild.commons.message.entity.Participant;
import com.gist.guild.gui.binding.DocumentAsyncService;
import com.gist.guild.gui.bot.action.entity.Action;
import com.gist.guild.gui.bot.action.repository.ActionRepository;
import com.gist.guild.gui.client.DocumentClient;
import com.gist.guild.gui.service.GuiConcurrenceService;
import com.gist.guild.gui.service.ResourceManagerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Future;

@Slf4j
@Service
public class ResourceManagerServiceImpl implements ResourceManagerService {
    @Autowired
    DocumentClient documentClient;

    @Autowired
    DocumentAsyncService documentAsyncService;

    @Autowired
    private ActionRepository actionRepository;

    @Override
    public Future<Participant> findParticipantByTelegramId(Long participant_id) {
        List<DocumentRepositoryMethodParameter<?>> params = new ArrayList<>(1);
        params.add(new DocumentRepositoryMethodParameter<Long>(Long.class,participant_id));
        ResponseEntity<DistributionMessage<Void>> distributionMessageResponseEntity = documentClient.documentByClass(Participant.class.getSimpleName(), "findByTelegramUserIdAndActiveTrue", params);
        return documentAsyncService.getUniqueResult(distributionMessageResponseEntity.getBody().getCorrelationID());
    }

    @Override
    public Future<Participant> addOrUpdateParticipant(Participant participant) {
        DocumentProposition documentProposition = new DocumentProposition();
        documentProposition.setDocumentPropositionType(DocumentPropositionType.USER_REGISTRATION);
        documentProposition.setDocumentClass(Participant.class.getSimpleName());
        documentProposition.setDocument(participant);
        ResponseEntity<DistributionMessage<DocumentProposition>> distributionMessageResponseEntity = documentClient.itemProposition(documentProposition);
        GuiConcurrenceService.getCorrelationIDs().add(distributionMessageResponseEntity.getBody().getCorrelationID());
        return documentAsyncService.getUniqueResult(distributionMessageResponseEntity.getBody().getCorrelationID());
    }

    @Override
    public Action getActionInProgress(Long telegramUserId){
        Optional<Action> actionOptional = actionRepository.findByTelegramUserIdAndInProgressTrue(telegramUserId);
        if(actionOptional.isPresent()){
            return actionOptional.get();
        } else {
            return null;
        }
    }
}
