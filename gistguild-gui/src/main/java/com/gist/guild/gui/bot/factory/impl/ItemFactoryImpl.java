package com.gist.guild.gui.bot.factory.impl;

import com.gist.guild.commons.message.entity.Participant;
import com.gist.guild.commons.message.entity.Product;
import com.gist.guild.commons.message.entity.RechargeCredit;
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
import java.util.NoSuchElementException;
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
            } else if (actionInProgress != null && ActionType.USER_SEARCH.equals(actionInProgress.getActionType())) {
                resourceManagerService.deleteActionInProgress(actionInProgress);
                Participant participantByMail = null;
                try {
                    participantByMail = resourceManagerService.findParticipantByMail(update.getText()).get();
                } catch (ExecutionException e) {
                    if (NoSuchElementException.class == e.getCause().getClass()) {
                        return message(update.getChatId(), "Nessun iscritto con questa mail\nClicca su /start per tornare al menu principale.");
                    } else {
                        log.error(e.getMessage());
                    }
                } catch (InterruptedException e) {
                    log.error(e.getMessage());
                }

                if(!participantByMail.getActive()){
                    return message(update.getChatId(), "Nessun iscritto con questa mail\nClicca su /start per tornare al menu principale.");
                }

                Action action = new Action();
                action.setActionType(ActionType.USER_MANAGEMENT);
                action.setTelegramUserIdToManage(participantByMail.getTelegramUserId());
                action.setTelegramUserId(participant.getTelegramUserId());
                resourceManagerService.saveAction(action);
                return userManagementMenu(update.getChatId(), participantByMail);
            }

        }
        //TODO DIfferent actions

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
            InlineKeyboardButton button2 = new InlineKeyboardButton();
            button2.setText("I tuoi ordini");
            button2.setCallbackData("listaOrdini");
            rowInline2.add(button2);
            InlineKeyboardButton button3 = new InlineKeyboardButton();
            button3.setText("Credito residuo");
            button3.setCallbackData("creditoResiduo");
            rowInline2.add(button3);
//            rowInline3.add(new InlineKeyboardButton().setText("Ricarica credito").setCallbackData("ricaricaCredito"));
            InlineKeyboardButton button4 = new InlineKeyboardButton();
            button4.setText("Cancellazione");
            button4.setCallbackData("cancellazione");
            rowInline4.add(button4);
            InlineKeyboardButton button5 = new InlineKeyboardButton();
            button5.setText("Gestione iscritti");
            button5.setCallbackData("usermng");
            rowInline5.add(button5);
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

    @Override
    public SendMessage userManagementMenu(Long chat_id, Participant participantToManage) {
        SendMessage message;
        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        RechargeCredit credit = null;
        try {
            credit = resourceManagerService.getCredit(participantToManage.getTelegramUserId()).get();
        } catch (ExecutionException e) {
            if (NoSuchElementException.class == e.getCause().getClass()) {
                message = message(chat_id, String.format("Utente : %s\nNon ha credito residuo", participantToManage.getMail(), credit.getNewCredit()));
            } else {
                log.error(e.getMessage());
            }
        } catch (InterruptedException e) {
            log.error(e.getMessage());
        }
        message = message(chat_id, String.format("Utente : %s\nCredito residuo : %s €", participantToManage.getMail(), credit.getNewCredit()));
        List<InlineKeyboardButton> rowInline1 = new ArrayList<>();
        List<InlineKeyboardButton> rowInline2 = new ArrayList<>();
        InlineKeyboardButton button1 = new InlineKeyboardButton();
        button1.setText("Ricarica credito");
        button1.setCallbackData("usermng#ricaricaCredito");
        rowInline1.add(button1);
        InlineKeyboardButton button2 = new InlineKeyboardButton();
        button2.setText("Cancellazione");
        button2.setCallbackData("usermng#cancellazione");
        rowInline1.add(button2);
        InlineKeyboardButton button3 = new InlineKeyboardButton();
        button3.setText("Modifica terminata");
        button3.setCallbackData("usermng#end");
        rowInline2.add(button3);

        // Set the keyboard to the markup
        rowsInline.add(rowInline1);
        rowsInline.add(rowInline2);

        // Add it to the message
        markupInline.setKeyboard(rowsInline);
        message.setReplyMarkup(markupInline);
        return message;
    }

    @Override
    public SendMessage userManagementCredit(Long chat_id) {
        SendMessage message;
        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        message = message(chat_id, "Ricarica il credito dell'utente scegliendo tra le seguenti opzioni:");

        List<InlineKeyboardButton> rowInline1 = new ArrayList<>();
        List<InlineKeyboardButton> rowInline2 = new ArrayList<>();

        InlineKeyboardButton button1 = new InlineKeyboardButton();
        button1.setText("5 €");
        button1.setCallbackData("usermng#ricaricaCredito#5");
        rowInline1.add(button1);

        InlineKeyboardButton button2 = new InlineKeyboardButton();
        button2.setText("10 €");
        button2.setCallbackData("usermng#ricaricaCredito#10");
        rowInline1.add(button2);

        InlineKeyboardButton button3 = new InlineKeyboardButton();
        button3.setText("20 €");
        button3.setCallbackData("usermng#ricaricaCredito#20");
        rowInline2.add(button3);

        InlineKeyboardButton button4 = new InlineKeyboardButton();
        button4.setText("50 €");
        button4.setCallbackData("usermng#ricaricaCredito#50");
        rowInline2.add(button4);

        rowsInline.add(rowInline1);
        rowsInline.add(rowInline2);

        // Add it to the message
        markupInline.setKeyboard(rowsInline);
        message.setReplyMarkup(markupInline);
        return message;
    }
}
