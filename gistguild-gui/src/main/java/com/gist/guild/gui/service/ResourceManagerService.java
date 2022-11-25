package com.gist.guild.gui.service;

import com.gist.guild.commons.message.entity.Participant;

import java.util.concurrent.Future;

public interface ResourceManagerService {
    Future<Participant> findParticipantByTelegramId(Long participant_id);
    Future<Participant> addOrUpdateParticipant(Participant participant);
    Participant getParticipantByMail(String call_data);
}
