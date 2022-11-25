package com.gist.guild.gui.service;

import com.gist.guild.commons.message.entity.Participant;
import com.gist.guild.gui.bot.action.entity.Action;

import java.util.concurrent.Future;

public interface ResourceManagerService {
    Future<Participant> findParticipantByTelegramId(Long participant_id);
    Future<Participant> addOrUpdateParticipant(Participant participant);
    Action getActionInProgress(Long telegramUserId);
}
