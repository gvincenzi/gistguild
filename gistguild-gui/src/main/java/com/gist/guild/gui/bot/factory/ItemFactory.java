package com.gist.guild.gui.bot.factory;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;

public interface ItemFactory {
    SendMessage welcomeMessage(Message update, Long user_id);
    SendMessage message(Long chat_id, String text);
}
