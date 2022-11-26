package com.gist.guild.gui.bot.factory.impl;

import com.gist.guild.commons.message.entity.Participant;
import com.gist.guild.commons.message.entity.Product;
import com.gist.guild.gui.bot.action.entity.Action;
import com.gist.guild.gui.bot.action.entity.ActionType;
import com.gist.guild.gui.bot.factory.ItemFactory;
import com.gist.guild.gui.service.ResourceManagerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Slf4j
@Component
public class ItemFactoryImpl implements ItemFactory {
    @Autowired
    ResourceManagerService resourceManagerService;

    public SendMessage welcomeMessage(Message update, Long user_id) {
        SendMessage message;
        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        Participant participant = null;
        try {
            participant = resourceManagerService.findParticipantByTelegramId(user_id).get();
        } catch (InterruptedException | ExecutionException e) {
            log.error(e.getMessage());
        }

        if (participant != null) {
            Action actionInProgress = resourceManagerService.getActionInProgress(participant.getTelegramUserId());

            if (actionInProgress != null && ActionType.PRODUCT_URL.equals(actionInProgress.getActionType())) {
                try {
                    Product product = resourceManagerService.getProduct(actionInProgress.getProductIdToManage()).get();
                    resourceManagerService.deleteActionInProgress(actionInProgress);
                    if (product == null) {
                        return message(update.getChatId(), "Nessun prodotto con questo ID in catalogo\nClicca su /start per tornare al menu principale.");
                    } else {

                        product.setUrl(update.getText());
                        product = resourceManagerService.updateProduct(product).get();
                        return message(update.getChatId(), String.format("Modifica del prodotto [%s] terminata.\nClicca su /start per tornare al menu principale.", product.getName()));

                    }
                } catch (InterruptedException | ExecutionException e) {
                    log.error(e.getMessage());
                }
            }
            //TODO DIfferent actions
        }

        message = message(update.getChatId(), String.format("%s,\nScegli tra le seguenti opzioni:", participant == null ? "Benvenuto nel sistema GIST Guild" : "Bentornato nel sistema GIST Guild"));

        List<InlineKeyboardButton> rowInline1 = new ArrayList<>();
        List<InlineKeyboardButton> rowInline2 = new ArrayList<>();
        List<InlineKeyboardButton> rowInline3 = new ArrayList<>();
        List<InlineKeyboardButton> rowInline4 = new ArrayList<>();
        List<InlineKeyboardButton> rowInline5 = new ArrayList<>();
        if (participant == null) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText("Iscrizione");
            button.setCallbackData("iscrizione");
            rowInline1.add(button);
        } else {
            InlineKeyboardButton button1 = new InlineKeyboardButton();
            button1.setText("Catalogo");
            button1.setCallbackData("catalogo");
            rowInline1.add(button1);
//            rowInline2.add(new InlineKeyboardButton().setText("I tuoi ordini").setCallbackData("listaOrdini"));
//            rowInline3.add(new InlineKeyboardButton().setText("Credito residuo").setCallbackData("creditoResiduo"));
//            rowInline3.add(new InlineKeyboardButton().setText("Ricarica credito").setCallbackData("ricaricaCredito"));
            InlineKeyboardButton button4 = new InlineKeyboardButton();
            button4.setText("Cancellazione");
            button4.setCallbackData("cancellazione");
            rowInline4.add(button4);
//            rowInline5.add(new InlineKeyboardButton().setText("Gestione iscritti").setCallbackData("usermng"));
        }

        // Set the keyboard to the markup
        rowsInline.add(rowInline1);
        rowsInline.add(rowInline2);
        rowsInline.add(rowInline3);
        rowsInline.add(rowInline4);
        if (participant != null && participant.getAdministrator()) {
            rowsInline.add(rowInline5);
        }

        // Add it to the message
        markupInline.setKeyboard(rowsInline);
        message.setReplyMarkup(markupInline);
        return message;
    }

    @Override
    public SendMessage message(Long chat_id, String text) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chat_id);
        sendMessage.setText(text);
        return sendMessage;
    }

    @Override
    public SendMessage productUrlManagement(Long chat_id) {
        return message(chat_id, "Inviare un ulteriore messaggio indicando l'URL da associare al prodotto");
    }
}
