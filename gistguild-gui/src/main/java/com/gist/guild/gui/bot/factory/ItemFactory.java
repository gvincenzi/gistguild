package com.gist.guild.gui.bot.factory;

import com.gist.guild.commons.message.entity.Order;
import com.gist.guild.commons.message.entity.Participant;
import com.gist.guild.commons.message.entity.Product;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;

public interface ItemFactory {
    SendMessage welcomeMessage(Message update, Long user_id);
    SendMessage message(Long chat_id, String text);
    SendMessage productUrlManagement(Long chat_id);
    SendMessage userManagementMenu(Long chat_id, Participant participantToManage);
    SendMessage userManagementCredit(Long chat_id);
    SendMessage selectProductQuantity(Long chat_id);
    SendMessage selectAddress(Long chat_id);
    SendMessage orderDetailsMessageBuilder(Long chat_id, Order order);
}
