package com.gist.guild.node.core.repository;

import com.gist.guild.node.core.document.Participant;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ParticipantRepository extends DocumentRepository<Participant>, MongoRepository<Participant, String> {
    List<Participant> findByTelegramUserId(Long telegramUserId);
    List<Participant> findByTelegramUserIdAndActiveTrue(Long id);
}
