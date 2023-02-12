package com.gist.guild.gui.bot.factory.impl;

import com.gist.guild.commons.message.entity.Order;
import com.gist.guild.commons.message.entity.Participant;
import com.gist.guild.commons.message.entity.Product;
import com.gist.guild.commons.message.entity.RechargeCredit;
import com.gist.guild.gui.bot.GistGuildBot;
import com.gist.guild.gui.bot.action.entity.Action;
import com.gist.guild.gui.bot.action.entity.ActionType;
import com.gist.guild.gui.bot.configuration.MessageProperties;
import com.gist.guild.gui.bot.factory.CallbackDataKey;
import com.gist.guild.gui.bot.factory.ItemFactory;
import com.gist.guild.gui.service.ResourceManagerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
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

    @Value("${gistguild.bot.stripe.active}")
    private Boolean stripeActive;

    @Autowired
    private MessageProperties messageProperties;

    @Autowired
    private GistGuildBot gistGuildBot;

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
                        return message(update.getChatId(), messageProperties.getMessage1());
                    } else {

                        product.setUrl(update.getText());
                        product = resourceManagerService.updateProduct(product).get();
                        return message(update.getChatId(), String.format(messageProperties.getMessage2(), product.getName()));

                    }
                } catch (InterruptedException | ExecutionException e) {
                    log.error(e.getMessage());
                }
            } else if (actionInProgress != null && ActionType.USER_SEARCH.equals(actionInProgress.getActionType())) {
                resourceManagerService.deleteActionInProgress(actionInProgress);
                Participant participantByTelegramId = null;
                try {
                    participantByTelegramId = resourceManagerService.findParticipantByTelegramId(Long.valueOf(update.getText())).get();
                } catch (ExecutionException e) {
                    if (NoSuchElementException.class == e.getCause().getClass()) {
                        return message(update.getChatId(), messageProperties.getMessage3());
                    } else {
                        log.error(e.getMessage());
                    }
                } catch (InterruptedException e) {
                    log.error(e.getMessage());
                }

                if(!participantByTelegramId.getActive()){
                    return message(update.getChatId(), messageProperties.getMessage3());
                }

                Action action = new Action();
                action.setActionType(ActionType.USER_MANAGEMENT);
                action.setTelegramUserIdToManage(participantByTelegramId.getTelegramUserId());
                action.setTelegramUserId(participant.getTelegramUserId());
                resourceManagerService.saveAction(action);
                return userManagementMenu(update.getChatId(), participantByTelegramId);
            } else if (actionInProgress != null && ActionType.PRODUCT_OWNER_MESSAGE.equals(actionInProgress.getActionType())) {
                resourceManagerService.deleteActionInProgress(actionInProgress);
                try {
                    Product product = resourceManagerService.getProduct(actionInProgress.getSelectedProductId()).get();
                    gistGuildBot.sendMessage(product.getOwnerTelegramUserId(),String.format(messageProperties.getMessage32(),participant.getNickname(),product.getName(),update.getText()));
                    return message(update.getChatId(), messageProperties.getMessage33());
                } catch (InterruptedException | ExecutionException e) {
                    log.error(e.getMessage());
                }
            }

        }

        message = message(update.getChatId(), messageProperties.getWelcome());

        List<InlineKeyboardButton> rowInline1 = new ArrayList<>();
        List<InlineKeyboardButton> rowInline2 = new ArrayList<>();
        List<InlineKeyboardButton> rowInline3 = new ArrayList<>();
        List<InlineKeyboardButton> rowInline4 = new ArrayList<>();
        List<InlineKeyboardButton> rowInline5 = new ArrayList<>();
        if (participant == null) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(messageProperties.getMenuItem1());
            button.setCallbackData(CallbackDataKey.REGISTRATION.name());
            rowInline1.add(button);
        } else {
            InlineKeyboardButton button1 = new InlineKeyboardButton();
            button1.setText(messageProperties.getMenuItem2());
            button1.setCallbackData(CallbackDataKey.CATALOG.name());
            rowInline1.add(button1);
            InlineKeyboardButton button7 = new InlineKeyboardButton();
            button7.setText(messageProperties.getMenuItem21());
            button7.setCallbackData(CallbackDataKey.SEARCH_PRODUCT.name());
            rowInline1.add(button7);
            InlineKeyboardButton button2 = new InlineKeyboardButton();
            button2.setText(messageProperties.getMenuItem3());
            button2.setCallbackData(CallbackDataKey.ORDER_LIST.name());
            rowInline2.add(button2);
            InlineKeyboardButton button20 = new InlineKeyboardButton();
            button20.setText(messageProperties.getMenuItem20());
            button20.setCallbackData(CallbackDataKey.ORDER_PAID_LIST.name());
            rowInline2.add(button20);
            InlineKeyboardButton button3 = new InlineKeyboardButton();
            button3.setText(messageProperties.getMenuItem4());
            button3.setCallbackData(CallbackDataKey.CREDIT.name());
            rowInline3.add(button3);
            InlineKeyboardButton button6 = new InlineKeyboardButton();
            button6.setText(messageProperties.getMenuItem5());
            button6.setCallbackData(CallbackDataKey.ADD_CREDIT.name());
            if(stripeActive) rowInline3.add(button6);
            InlineKeyboardButton button4 = new InlineKeyboardButton();
            button4.setText(String.format(messageProperties.getMenuItem6(), participant.getTelegramUserId()));
            button4.setCallbackData(CallbackDataKey.CANCELLATION.name());
            rowInline4.add(button4);
            InlineKeyboardButton button5 = new InlineKeyboardButton();
            button5.setText(messageProperties.getMenuItem7());
            button5.setCallbackData(CallbackDataKey.USER_MANAGEMENT.name());
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
        message.enableHtml(Boolean.TRUE);
        return message;
    }

    @Override
    public SendMessage message(Long chat_id, String text) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chat_id);
        sendMessage.setText(text);
        sendMessage.enableHtml(Boolean.TRUE);
        return sendMessage;
    }

    @Override
    public SendMessage productUrlManagement(Long chat_id) {
        return message(chat_id, messageProperties.getMessage4());
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
                message = message(chat_id, String.format(messageProperties.getMessage5(), participantToManage.getNickname(), credit.getNewCredit()));
            } else {
                log.error(e.getMessage());
            }
        } catch (InterruptedException e) {
            log.error(e.getMessage());
        }
        message = message(chat_id, String.format(messageProperties.getMessage6(), participantToManage.getNickname(), credit.getNewCredit()));
        List<InlineKeyboardButton> rowInline1 = new ArrayList<>();
        List<InlineKeyboardButton> rowInline2 = new ArrayList<>();
        InlineKeyboardButton button1 = new InlineKeyboardButton();
        button1.setText(messageProperties.getMenuItem5());
        button1.setCallbackData(CallbackDataKey.USER_MANAGEMENT.name()+CallbackDataKey.DELIMITER+CallbackDataKey.ADD_CREDIT.name());
        rowInline1.add(button1);
        InlineKeyboardButton button2 = new InlineKeyboardButton();
        button2.setText(String.format(messageProperties.getMenuItem6(), participantToManage.getTelegramUserId()));
        button2.setCallbackData(CallbackDataKey.USER_MANAGEMENT.name()+CallbackDataKey.DELIMITER+CallbackDataKey.CANCELLATION);
        rowInline1.add(button2);
        InlineKeyboardButton button3 = new InlineKeyboardButton();
        button3.setText(messageProperties.getMenuItem8());
        button3.setCallbackData(CallbackDataKey.USER_MANAGEMENT.name()+CallbackDataKey.DELIMITER+CallbackDataKey.END.name());
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
    public SendMessage userCredit(Long chat_id) {
        SendMessage message;
        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        message = message(chat_id, messageProperties.getMessage7());

        List<InlineKeyboardButton> rowInline1 = new ArrayList<>();
        List<InlineKeyboardButton> rowInline2 = new ArrayList<>();

        InlineKeyboardButton button1 = new InlineKeyboardButton();
        button1.setText("5 €");
        button1.setCallbackData(CallbackDataKey.ADD_CREDIT.name()+CallbackDataKey.DELIMITER+"5");
        rowInline1.add(button1);

        InlineKeyboardButton button2 = new InlineKeyboardButton();
        button2.setText("10 €");
        button2.setCallbackData(CallbackDataKey.ADD_CREDIT.name()+CallbackDataKey.DELIMITER+"10");
        rowInline1.add(button2);

        InlineKeyboardButton button3 = new InlineKeyboardButton();
        button3.setText("20 €");
        button3.setCallbackData(CallbackDataKey.ADD_CREDIT.name()+CallbackDataKey.DELIMITER+"20");
        rowInline2.add(button3);

        InlineKeyboardButton button4 = new InlineKeyboardButton();
        button4.setText("50 €");
        button4.setCallbackData(CallbackDataKey.ADD_CREDIT.name()+CallbackDataKey.DELIMITER+"50");
        rowInline2.add(button4);

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
        message = message(chat_id,  messageProperties.getMessage7());

        List<InlineKeyboardButton> rowInline1 = new ArrayList<>();
        List<InlineKeyboardButton> rowInline2 = new ArrayList<>();

        InlineKeyboardButton button1 = new InlineKeyboardButton();
        button1.setText("5 €");
        button1.setCallbackData(CallbackDataKey.USER_MANAGEMENT.name()+CallbackDataKey.DELIMITER+CallbackDataKey.ADD_CREDIT.name()+CallbackDataKey.DELIMITER+"5");
        rowInline1.add(button1);

        InlineKeyboardButton button2 = new InlineKeyboardButton();
        button2.setText("10 €");
        button2.setCallbackData(CallbackDataKey.USER_MANAGEMENT.name()+CallbackDataKey.DELIMITER+CallbackDataKey.ADD_CREDIT.name()+CallbackDataKey.DELIMITER+"10");
        rowInline1.add(button2);

        InlineKeyboardButton button3 = new InlineKeyboardButton();
        button3.setText("20 €");
        button3.setCallbackData(CallbackDataKey.USER_MANAGEMENT.name()+CallbackDataKey.DELIMITER+CallbackDataKey.ADD_CREDIT.name()+CallbackDataKey.DELIMITER+"20");
        rowInline2.add(button3);

        InlineKeyboardButton button4 = new InlineKeyboardButton();
        button4.setText("50 €");
        button4.setCallbackData(CallbackDataKey.USER_MANAGEMENT.name()+CallbackDataKey.DELIMITER+CallbackDataKey.ADD_CREDIT.name()+CallbackDataKey.DELIMITER+"50");
        rowInline2.add(button4);

        rowsInline.add(rowInline1);
        rowsInline.add(rowInline2);

        // Add it to the message
        markupInline.setKeyboard(rowsInline);
        message.setReplyMarkup(markupInline);
        return message;
    }

    @Override
    public SendMessage selectProductQuantity(Long chat_id) {
        return message(chat_id, messageProperties.getMessage8());
    }

    @Override
    public SendMessage selectAddress(Long chat_id) {
        return message(chat_id, messageProperties.getMessage9());
    }

    @Override
    public SendMessage orderDetailsMessageBuilder(Long chat_id, Order order) {
        SendMessage message = message(chat_id, messageProperties.toString(order));

        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        List<InlineKeyboardButton> rowInline1 = new ArrayList<>();
        List<InlineKeyboardButton> rowInline2 = new ArrayList<>();
        List<InlineKeyboardButton> rowInline3 = new ArrayList<>();
        List<InlineKeyboardButton> rowInline4 = new ArrayList<>();
        List<InlineKeyboardButton> rowInline5 = new ArrayList<>();

        InlineKeyboardButton button1 = new InlineKeyboardButton();
        button1.setText(String.format(messageProperties.getMenuItem9(),order.getAmount()));
        button1.setCallbackData(CallbackDataKey.PAYMENT.name()+CallbackDataKey.DELIMITER + order.getExternalShortId());
        rowInline1.add(button1);

        InlineKeyboardButton button2 = new InlineKeyboardButton();
        button2.setText(messageProperties.getMenuItem10());
        button2.setCallbackData(CallbackDataKey.ORDER_DELETE.name() + CallbackDataKey.DELIMITER + order.getExternalShortId());
        rowInline2.add(button2);

        if(!StringUtils.isEmpty(order.getProductUrl())){
            InlineKeyboardButton button3 = new InlineKeyboardButton();
            button3.setText(messageProperties.getMenuItem11());
            button3.setUrl(order.getProductUrl());
            rowInline3.add(button3);
        }

        InlineKeyboardButton button4 = new InlineKeyboardButton();
        button4.setText(messageProperties.getMenuItem3());
        button4.setCallbackData(CallbackDataKey.ORDER_LIST.name());
        rowInline4.add(button4);
        InlineKeyboardButton button5 = new InlineKeyboardButton();
        button5.setText(messageProperties.getMenuItem20());
        button5.setCallbackData(CallbackDataKey.ORDER_PAID_LIST.name());
        rowInline4.add(button5);

        InlineKeyboardButton button3 = new InlineKeyboardButton();
        button3.setText(messageProperties.getMenuItem13());
        button3.setCallbackData(CallbackDataKey.WELCOME.name());
        rowInline5.add(button3);

        // Set the keyboard to the markup
        if(order.getPaymentId() == null){
            rowsInline.add(rowInline1);
            rowsInline.add(rowInline2);
        } else {
            if(!StringUtils.isEmpty(order.getProductUrl())){
                rowsInline.add(rowInline3);
            }
        }

        rowsInline.add(rowInline4);
        rowsInline.add(rowInline5);

        // Add it to the message
        markupInline.setKeyboard(rowsInline);
        message.setReplyMarkup(markupInline);
        message.enableHtml(Boolean.TRUE);
        return message;
    }

    @Override
    public SendMessage sendMessageToProductOwner(Long chat_id) {
        return message(chat_id, messageProperties.getMessage31());
    }
}
