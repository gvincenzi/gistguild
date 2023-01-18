package com.gist.guild.gui.bot;

import com.gist.guild.commons.message.entity.Order;
import com.gist.guild.commons.message.entity.Participant;
import com.gist.guild.gui.bot.configuration.MessageProperties;
import com.gist.guild.gui.bot.factory.CallbackDataKey;
import com.gist.guild.gui.bot.factory.ItemFactory;
import com.gist.guild.gui.service.ResourceManagerService;
import net.bytebuddy.utility.RandomString;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

public class BotUtils {
    private static Pattern pattern = Pattern.compile("-?\\d+(\\.\\d+)?");

    public static Participant createParticipant(User from) {
        Participant participant = new Participant();
        participant.setNickname(from.getUserName() != null ? from.getUserName() : from.getFirstName() != null ? from.getFirstName() : from.getLastName() != null ? from.getLastName() : RandomString.make());
        participant.setTelegramUserId(from.getId());

        return participant;
    }

    public static boolean isNumeric(String text) {
        if (text == null) {
            return false;
        }
        return pattern.matcher(text).matches();
    }

    public static BotApiMethod getOrderList(BotApiMethod message, Long user_id, Long chat_id, ResourceManagerService resourceManagerService, ItemFactory itemFactory, MessageProperties messageProperties, Boolean paid) {
        try {
            List<Order> orders = paid ? resourceManagerService.getPaidOrders(user_id).get() : resourceManagerService.getOrders(user_id).get();
            if (orders.isEmpty()) {
                message = paid ? itemFactory.message(chat_id, messageProperties.getMessage29()) : itemFactory.message(chat_id, messageProperties.getMessage27());
            } else {
                InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
                List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
                Collections.sort(orders);
                for (Order order : orders) {
                    List<InlineKeyboardButton> rowInline = new ArrayList<>();
                    InlineKeyboardButton button = new InlineKeyboardButton();
                    button.setText(String.format(messageProperties.getMenuItem19(), order.getProductName(), order.getExternalShortId()));
                    button.setCallbackData(CallbackDataKey.ORDER_DETAILS.name() + CallbackDataKey.DELIMITER + order.getExternalShortId());
                    rowInline.add(button);
                    rowsInline.add(rowInline);
                }

                List<InlineKeyboardButton> rowInline = new ArrayList<>();
                InlineKeyboardButton button2 = new InlineKeyboardButton();
                button2.setText(messageProperties.getMenuItem13());
                button2.setCallbackData(CallbackDataKey.WELCOME.name());
                rowInline.add(button2);
                rowsInline.add(rowInline);

                markupInline.setKeyboard(rowsInline);
                message = itemFactory.message(chat_id, paid ? messageProperties.getMessage30() : messageProperties.getMessage28());

                ((SendMessage) message).setReplyMarkup(markupInline);
            }
        } catch (InterruptedException | ExecutionException e) {
        }
        return message;
    }

}
