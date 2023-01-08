package com.gist.guild.node.core.service.impl;

import com.gist.guild.commons.exception.GistGuildGenericException;
import com.gist.guild.node.core.document.Participant;
import com.gist.guild.node.core.repository.ParticipantRepository;
import com.gist.guild.node.core.service.NodeService;
import com.gist.guild.node.core.service.NodeUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;
import java.util.UUID;

@Data
@Slf4j
@Service
public class ParticipantServiceImpl extends NodeService<com.gist.guild.commons.message.entity.Participant,Participant> {
    @Autowired
    private ParticipantRepository repository;

    @Autowired
    private InMemoryUserDetailsManager userDetailsService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${gistguild.admin.password}")
    private String password;

    protected static final String ADMIN = "ADMIN";

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
        participant.setAdministrator(previous != null ? document.getAdministrator() : Boolean.TRUE);
        if(previous == null){
            participant.setAdminPasswordEncoded(getDefaultAdminPassword(participant));
        }

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
        Participant participant = null;
        if (repository.findByIsCorruptionDetectedTrue().size() == 0 && !repository.existsById(document.getId())) {
                participant = new Participant();
                participant.setId(document.getId());
                participant.setTimestamp(document.getTimestamp());
                participant.setPreviousId(document.getPreviousId());
                participant.setNodeInstanceName(document.getNodeInstanceName());
                participant.setActive(document.getActive());
                participant.setAdministrator(document.getAdministrator());
                participant.setNickname(document.getNickname());
                participant.setAdminPasswordEncoded(document.getAdminPasswordEncoded());
                participant.setTelegramUserId(document.getTelegramUserId());
                participant.setNonce(document.getNonce());
                participant.setExternalShortId(document.getExternalShortId());
                repository.save(participant);
        } else if(repository.findByIsCorruptionDetectedTrue().size() == 0 && repository.existsById(document.getId())){
            participant = repository.findById(document.getId()).get();
            participant.setActive(document.getActive());
            participant.setAdministrator(document.getAdministrator());
            participant.setAdminPasswordEncoded(document.getAdminPasswordEncoded());
            repository.save(participant);
        }

        if(document.getAdministrator() && !userDetailsService.userExists(document.getTelegramUserId().toString())){
            UserDetails admin = User.withUsername(document.getTelegramUserId().toString())
                    .password(participant.getAdminPasswordEncoded())
                    .roles(ADMIN)
                    .build();
            userDetailsService.createUser(admin);
        } else if(!document.getAdministrator() && userDetailsService.userExists(document.getTelegramUserId().toString())){
            userDetailsService.deleteUser(document.getTelegramUserId().toString());
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
        Participant newParticipant = null;
        if(participants.size() > 0) {
            Participant participant = participants.iterator().next();
            participant.setActive(document.getActive());
            participant.setAdministrator(GENESIS.equals(participant.getPreviousId()) ? Boolean.TRUE : document.getAdministrator());

            if(!participant.getAdministrator() && document.getAdministrator()){
                participant.setAdminPasswordEncoded(getDefaultAdminPassword(participant));
            }

            newParticipant = repository.save(participant);
        } else {
            Participant previous = repository.findTopByOrderByTimestampDesc();
            Participant newItem = getNewItem(document, previous);
            newParticipant = repository.save(newItem);
        }

        if(newParticipant.getAdministrator() && !userDetailsService.userExists(newParticipant.getTelegramUserId().toString())){
            UserDetails admin = User.withUsername(newParticipant.getTelegramUserId().toString())
                    .password(newParticipant.getAdminPasswordEncoded())
                    .roles(ADMIN)
                    .build();
            userDetailsService.createUser(admin);
        } else if(!newParticipant.getAdministrator() && userDetailsService.userExists(newParticipant.getTelegramUserId().toString())){
            userDetailsService.deleteUser(newParticipant.getTelegramUserId().toString());
        }

        return newParticipant;
    }

    private String getDefaultAdminPassword(Participant participant) {
        String pass = UUID.randomUUID().toString();
        return passwordEncoder.encode(pass);
    }

}
