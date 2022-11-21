package com.gist.guild.node.core.repository;

import com.gist.guild.node.core.document.Participant;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ParticipantRepository extends DocumentRepository<Participant>, MongoRepository<Participant, String> {
    List<Participant> findByActiveTrue();
    List<Participant> findByAdministratorTrue();
    Participant findByMail(String mail);
    Optional<Participant> findByTelegramUserId(Integer telegramUserId);
    Optional<Participant> findByTelegramUserIdAndActiveTrue(Integer id);
}
