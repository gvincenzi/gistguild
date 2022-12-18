package com.gist.guild.gui.bot;

import com.gist.guild.commons.message.entity.Order;
import com.gist.guild.commons.message.entity.Participant;
import com.gist.guild.gui.bot.factory.CallbackDataKey;
import com.gist.guild.gui.bot.factory.ItemFactory;
import com.gist.guild.gui.service.ResourceManagerService;
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

    public static BotApiMethod getOrderList(BotApiMethod message, Long user_id, Long chat_id, ResourceManagerService resourceManagerService, ItemFactory itemFactory) {
        try {
            List<Order> orders = resourceManagerService.getOrders(user_id).get();
            if (orders.isEmpty()) {
                message = itemFactory.message(chat_id, "Non hai ordini in corso");
            } else {
                InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
                List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
                Collections.sort(orders);
                for (Order order : orders) {
                    List<InlineKeyboardButton> rowInline = new ArrayList<>();
                    InlineKeyboardButton button = new InlineKeyboardButton();
                    button.setText("ID#" + order.getExternalShortId() + " : " + order.getProductName());
                    button.setCallbackData(CallbackDataKey.ORDER_DETAILS.name() + CallbackDataKey.DELIMITER + order.getExternalShortId());
                    rowInline.add(button);
                    rowsInline.add(rowInline);
                }

                List<InlineKeyboardButton> rowInline = new ArrayList<>();
                InlineKeyboardButton button2 = new InlineKeyboardButton();
                button2.setText("Torna al menù principale");
                button2.setCallbackData(CallbackDataKey.WELCOME.name());
                rowInline.add(button2);
                rowsInline.add(rowInline);

                markupInline.setKeyboard(rowsInline);
                message = itemFactory.message(chat_id, "Qui di seguito la lista dei tuoi ordini in corso, per accedere ai dettagli cliccare sull'ordine:\n");

                ((SendMessage) message).setReplyMarkup(markupInline);
            }
        } catch (InterruptedException | ExecutionException e) {
        }
        return message;
    }

}
