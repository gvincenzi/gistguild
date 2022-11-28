package com.gist.guild.gui.bot;

import com.gist.guild.commons.message.entity.Participant;
import org.telegram.telegrambots.meta.api.objects.User;

import java.util.regex.Pattern;

public class BotUtils {
    private static Pattern pattern = Pattern.compile("-?\\d+(\\.\\d+)?");

    public static Participant createParticipant(User from, String mail) {
        Participant participant = new Participant();
        participant.setMail(mail);
        participant.setTelegramUserId(from.getId());

        return participant;
    }

    public static boolean isNumeric(String text) {
        if (text == null) {
            return false;
        }
        return pattern.matcher(text).matches();
    }
}
