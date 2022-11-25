package com.gist.guild.gui.bot;

import com.gist.guild.commons.message.entity.Participant;
import org.telegram.telegrambots.meta.api.objects.User;

public class BotUtils {
    public static Participant createParticipant(User from, String mail) {
        Participant participant = new Participant();
        participant.setMail(mail);
        participant.setTelegramUserId(from.getId());

        return participant;
    }
}
