package com.gist.guild.node.core.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gist.guild.commons.exception.GistGuildGenericException;
import com.gist.guild.node.core.document.Participant;
import com.gist.guild.node.core.repository.ParticipantRepository;
import com.gist.guild.node.core.service.NodeService;
import com.gist.guild.node.core.service.NodeUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@Slf4j
@Service
public class ParticipantServiceImpl implements NodeService<com.gist.guild.commons.message.entity.Participant,Participant> {
    private static final String GENESIS = "GENESIS";

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    private ParticipantRepository participantRepository;

    @Value("${gistguild.difficult.level}")
    private Integer difficultLevel;

    @Value("${spring.application.name}")
    private String instanceName;

    protected Participant getNewItem(com.gist.guild.commons.message.entity.Participant document, Participant previous) throws GistGuildGenericException {
        if (document == null) {
            throw new GistGuildGenericException("Document are mandatory");
        }
        Participant participant = new Participant();
        participant.setPreviousId(previous != null ? previous.getId() : GENESIS);
        participant.setNodeInstanceName(instanceName);
        participant.setActive(document.getActive());
        participant.setAdministrator(document.getAdministrator());
        participant.setMail(document.getMail());
        participant.setTelegramUserId(document.getTelegramUserId());

        Random random = new Random(participant.getTimestamp().toEpochMilli());
        int nonce = random.nextInt();
        participant.setNonce(nonce);
        participant.setId(NodeUtils.calculateHash(participant));
        while (!NodeUtils.isHashResolved(participant, difficultLevel)) {
            nonce = random.nextInt();
            participant.setNonce(nonce);
            participant.setId(NodeUtils.calculateHash(participant));
        }

        return participant;
    }

    @Override
    public Boolean updateLocal(com.gist.guild.commons.message.entity.Participant document) throws GistGuildGenericException {
        if (participantRepository.findByIsCorruptionDetectedTrue().size() == 0 && !participantRepository.existsById(document.getId())) {
                Participant participant = new Participant();
                participant.setId(document.getId());
                participant.setTimestamp(document.getTimestamp());
                participant.setPreviousId(document.getPreviousId());
                participant.setNodeInstanceName(document.getNodeInstanceName());
                participant.setActive(document.getActive());
                participant.setAdministrator(document.getAdministrator());
                participant.setMail(document.getMail());
                participant.setTelegramUserId(document.getTelegramUserId());
                participant.setNonce(document.getNonce());
                participantRepository.save(participant);
                return validate(participantRepository.findAllByOrderByTimestampAsc());
        } else if(participantRepository.findByIsCorruptionDetectedTrue().size() == 0 && participantRepository.existsById(document.getId())){
            Participant participant = participantRepository.findById(document.getId()).get();
            participant.setActive(document.getActive());
            participant.setAdministrator(document.getAdministrator());
            participantRepository.save(participant);
            return validate(participantRepository.findAllByOrderByTimestampAsc());
        }

        return Boolean.TRUE;
    }

    @Override
    public List<Participant> findAll() {
        return participantRepository.findAllByOrderByTimestampAsc();
    }

    @Override
    public Boolean validate(List<Participant> rdItems) throws GistGuildGenericException {
        if (rdItems == null) {
            throw new GistGuildGenericException("Iterable items collection is mandatory");
        }
        Participant currentItem;
        Participant previousItem;

        Collections.sort(rdItems);

        Boolean result = true;
        for (int i = 0; i < rdItems.size(); i++) {
            previousItem = i > 0 ? rdItems.get(i - 1) : null;
            currentItem = rdItems.get(i);
            if (!currentItem.getId().equals(NodeUtils.calculateHash(currentItem))) {
                result = false;
            }
            if (previousItem != null && !previousItem.getId().equals(currentItem.getPreviousId())) {
                result = false;
            }
            if (previousItem == null && !GENESIS.equals(currentItem.getPreviousId())) {
                result = false;
            }
            if (!NodeUtils.isHashResolved(currentItem, difficultLevel)) {
                result = false;
            }
        }

        return result;
    }

    @Override
    public Participant add(com.gist.guild.commons.message.entity.Participant document) throws GistGuildGenericException {
        if (document == null) {
            throw new GistGuildGenericException("Document is mandatory");
        }
        if (participantRepository.findByIsCorruptionDetectedTrue().size() > 0) {
            throw new GistGuildGenericException("Gist Guild registry is corrupted");
        }

        List<Participant> participants = participantRepository.findByMail(document.getMail());
        participants.addAll(participantRepository.findByTelegramUserId(document.getTelegramUserId()));
        if(participants.size() > 0) {
            Participant participant = participants.iterator().next();
            participant.setMail(document.getMail());
            participant.setActive(document.getActive());
            participant.setAdministrator(document.getAdministrator());
            return participantRepository.save(participant);
        } else {
            Participant previous = participantRepository.findTopByOrderByTimestampDesc();
            Participant newItem = getNewItem(document, previous);
            return participantRepository.save(newItem);
        }
    }

    @Override
    public void init(List<com.gist.guild.commons.message.entity.Participant> content) throws GistGuildGenericException {
        Collections.sort(content);
        List<Participant> participants = participantRepository.findAll(Sort.by("timestamp"));
        for (int i = 0; i < content.size(); i++) {
            if (i < participants.size()) {
                if (!participants.get(i).getId().equals(content.get(i).getId())) {
                    throw new GistGuildGenericException("Guild registry has been corrupted");
                }
            } else {
                updateLocal(content.get(i));
            }
        }
    }
}
