package com.gist.guild.node.core.service.impl;

import com.gist.guild.commons.exception.GistGuildGenericException;
import com.gist.guild.node.core.document.Participant;
import com.gist.guild.node.core.repository.ParticipantRepository;
import com.gist.guild.node.core.service.NodeService;
import com.gist.guild.node.core.service.NodeUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;

@Data
@Slf4j
@Service
public class ParticipantServiceImpl extends NodeService<com.gist.guild.commons.message.entity.Participant,Participant> {
    @Autowired
    private ParticipantRepository repository;

    @Override
    public String calculateHash(Participant document) throws GistGuildGenericException {
        return NodeUtils.applySha256(
                document.getPreviousId() +
                        document.getTimestamp().toEpochMilli() +
                        document.getNonce() +
                        document.getNodeInstanceName() +
                        document.getTelegramUserId()
        );
    }

    protected Participant getNewItem(com.gist.guild.commons.message.entity.Participant document, Participant previous) throws GistGuildGenericException {
        if (document == null) {
            throw new GistGuildGenericException(messageProperties.getError1());
        }
        Participant participant = new Participant();
        participant.setPreviousId(previous != null ? previous.getId() : GENESIS);
        participant.setNodeInstanceName(instanceName);
        participant.setActive(document.getActive());
        participant.setAdministrator(document.getAdministrator());
        participant.setNickname(document.getNickname());
        participant.setTelegramUserId(document.getTelegramUserId());
        participant.setIsCorruptionDetected(document.getIsCorruptionDetected());

        Random random = new Random(participant.getTimestamp().toEpochMilli());
        int nonce = random.nextInt();
        participant.setNonce(nonce);
        participant.setId(calculateHash(participant));
        while (!NodeUtils.isHashResolved(participant, difficultLevel)) {
            nonce = random.nextInt();
            participant.setNonce(nonce);
            participant.setId(calculateHash(participant));
        }

        participant.setExternalShortId(repository.count());
        return participant;
    }

    @Override
    public Boolean updateLocal(com.gist.guild.commons.message.entity.Participant document) throws GistGuildGenericException {
        if (repository.findByIsCorruptionDetectedTrue().size() == 0 && !repository.existsById(document.getId())) {
                Participant participant = new Participant();
                participant.setId(document.getId());
                participant.setTimestamp(document.getTimestamp());
                participant.setPreviousId(document.getPreviousId());
                participant.setNodeInstanceName(document.getNodeInstanceName());
                participant.setActive(document.getActive());
                participant.setAdministrator(document.getAdministrator());
                participant.setNickname(document.getNickname());
                participant.setTelegramUserId(document.getTelegramUserId());
                participant.setNonce(document.getNonce());
                participant.setExternalShortId(document.getExternalShortId());
                repository.save(participant);
        } else if(repository.findByIsCorruptionDetectedTrue().size() == 0 && repository.existsById(document.getId())){
            Participant participant = repository.findById(document.getId()).get();
            participant.setActive(document.getActive());
            participant.setAdministrator(document.getAdministrator());
            repository.save(participant);
        }

        return validate(repository.findAllByOrderByTimestampAsc());
    }

    @Override
    public Participant add(com.gist.guild.commons.message.entity.Participant document) throws GistGuildGenericException {
        if (document == null) {
            throw new GistGuildGenericException(messageProperties.getError1());
        }
        if (repository.findByIsCorruptionDetectedTrue().size() > 0) {
            throw new GistGuildGenericException(messageProperties.getError2());
        }

        List<Participant> participants = repository.findByTelegramUserId(document.getTelegramUserId());
        if(participants.size() > 0) {
            Participant participant = participants.iterator().next();
            participant.setActive(document.getActive());
            participant.setAdministrator(document.getAdministrator());
            return repository.save(participant);
        } else {
            Participant previous = repository.findTopByOrderByTimestampDesc();
            Participant newItem = getNewItem(document, previous);
            return repository.save(newItem);
        }
    }

}
