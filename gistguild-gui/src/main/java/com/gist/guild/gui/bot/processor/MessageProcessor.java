package com.gist.guild.gui.bot.processor;

import com.gist.guild.commons.exception.GistGuildGenericException;
import com.gist.guild.commons.message.entity.*;
import com.gist.guild.gui.bot.BotUtils;
import com.gist.guild.gui.bot.action.entity.Action;
import com.gist.guild.gui.bot.action.entity.ActionType;
import com.gist.guild.gui.bot.factory.CallbackDataKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.AnswerPreCheckoutQuery;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutionException;

@Slf4j
@Service
public class MessageProcessor extends UpdateProcessor {
    @Override
    public BotApiMethod process(Update update, BotApiMethod message) {
        if(message instanceof AnswerPreCheckoutQuery){
            try {
                RechargeCredit rechargeCreditLast = resourceManagerService.getCredit(update.getPreCheckoutQuery().getFrom().getId()).get();
                Participant participant = resourceManagerService.findParticipantByTelegramId(update.getPreCheckoutQuery().getFrom().getId()).get();
                RechargeCredit rechargeCredit = new RechargeCredit();
                rechargeCredit.setCustomerNickname(participant.getNickname());
                rechargeCredit.setCustomerTelegramUserId(participant.getTelegramUserId());
                rechargeCredit.setNewCredit(rechargeCreditLast.getNewCredit() + update.getPreCheckoutQuery().getTotalAmount()/ CURRENCY_DIVISOR);
                rechargeCredit.setOldCredit(rechargeCreditLast.getNewCredit());
                rechargeCredit.setRechargeUserCreditType(RechargeCreditType.INVOICE);
                resourceManagerService.addCredit(rechargeCredit);
                return message;
            } catch (InterruptedException | ExecutionException e) {
                log.error(e.getMessage());
            }
        }

        Long user_id = update.getMessage().getFrom().getId();
        Long chat_id = update.getMessage().getChatId();

        Action actionInProgress = resourceManagerService.getActionInProgress(user_id);

        if (update.getMessage().getText() != null && update.getMessage().getText().equalsIgnoreCase(START_TOKEN)) {
            message = itemFactory.welcomeMessage(update.getMessage(), user_id);
        } else if (update.getMessage().getText() != null && update.getMessage().getText().equalsIgnoreCase(RESET_TOKEN)) {
            message = itemFactory.resetMessage(update.getMessage(), user_id);
        } else if (update.getMessage().getText() != null && actionInProgress != null && !(ActionType.SELECT_PRODUCT.equals(actionInProgress.getActionType()) || ActionType.SEARCH_PRODUCT.equals(actionInProgress.getActionType()) || ActionType.SELECT_ADDRESS.equals(actionInProgress.getActionType()))) {
            message = itemFactory.welcomeMessage(update.getMessage(), user_id);
        } else if (actionInProgress != null && ActionType.USER_SEARCH.equals(actionInProgress.getActionType())) {
            message = itemFactory.welcomeMessage(update.getMessage(), user_id);
        } else if (update.getMessage().getText() != null && BotUtils.isNumeric(update.getMessage().getText()) && actionInProgress != null && ActionType.SELECT_PRODUCT.equals(actionInProgress.getActionType())) {
            resourceManagerService.deleteActionInProgress(actionInProgress);
            try {
                Participant participant = resourceManagerService.findParticipantByTelegramId(user_id).get();
                Product product = resourceManagerService.getProduct(actionInProgress.getSelectedProductId()).get();
                if (product.getDelivery()) {
                    Action action = new Action();
                    action.setActionType(ActionType.SELECT_ADDRESS);
                    action.setTelegramUserId(user_id);
                    action.setSelectedProductId(actionInProgress.getSelectedProductId());
                    action.setQuantity(Long.parseLong(update.getMessage().getText()));
                    resourceManagerService.saveAction(action);
                    message = itemFactory.selectAddress(chat_id);
                } else {
                    Order order = new Order();
                    order.setCustomerNickname(participant.getNickname());
                    order.setCustomerTelegramUserId(participant.getTelegramUserId());
                    order.setProductId(product.getId());
                    order.setProductName(product.getName());
                    order.setProductOwnerTelegramUserId(product.getOwnerTelegramUserId());
                    order.setProductUrl(product.getUrl());
                    order.setProductPassword(product.getPassword());
                    order.setQuantity(Long.parseLong(update.getMessage().getText()));
                    order.setAmount(order.getQuantity() != null ? product.getPrice() * order.getQuantity() : product.getPrice());
                    try {
                        order = resourceManagerService.addOrUpdateOrder(order);
                        message = itemFactory.message(chat_id, String.format(messageProperties.getMessage20(), order.getExternalShortId()));
                    } catch (GistGuildGenericException e) {
                        message = itemFactory.message(chat_id,String.format(messageProperties.getError1(),e.getMessage()));
                    }
                }
            } catch (InterruptedException | ExecutionException e) {
                log.error(e.getMessage());
            }
        } else if (update.getMessage().getText() != null && !BotUtils.isNumeric(update.getMessage().getText()) && actionInProgress != null && ActionType.SELECT_ADDRESS.equals(actionInProgress.getActionType())) {
            resourceManagerService.deleteActionInProgress(actionInProgress);
            try {
                Participant participant = resourceManagerService.findParticipantByTelegramId(user_id).get();
                Product product = resourceManagerService.getProduct(actionInProgress.getSelectedProductId()).get();
                Order order = new Order();
                order.setCustomerNickname(participant.getNickname());
                order.setCustomerTelegramUserId(participant.getTelegramUserId());
                order.setProductId(product.getId());
                order.setProductName(product.getName());
                order.setProductOwnerTelegramUserId(product.getOwnerTelegramUserId());
                order.setProductUrl(product.getUrl());
                order.setProductPassword(product.getPassword());
                order.setQuantity(actionInProgress.getQuantity());
                order.setAddress(update.getMessage().getText());
                order.setAmount(order.getQuantity() != null ? product.getPrice() * order.getQuantity() : product.getPrice());
                try {
                    order = resourceManagerService.addOrUpdateOrder(order);
                    message = itemFactory.message(chat_id, String.format(messageProperties.getMessage20(), order.getExternalShortId()));
                } catch (GistGuildGenericException e) {
                    message = itemFactory.message(chat_id, String.format(messageProperties.getError1(), e.getMessage()));
                }
            } catch (InterruptedException | ExecutionException e) {
                log.error(e.getMessage());
            }
        } else if (update.getMessage().getText() != null && !BotUtils.isNumeric(update.getMessage().getText()) && actionInProgress != null && ActionType.SEARCH_PRODUCT.equals(actionInProgress.getActionType())) {
                resourceManagerService.deleteActionInProgress(actionInProgress);
            try {
                List<Product> products = resourceManagerService.getProductsByTags(update.getMessage().getText()).get();
                if (products.isEmpty()) {
                    message = itemFactory.message(chat_id, messageProperties.getMessage16());
                } else {
                    InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
                    List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
                    Collections.sort(products);
                    for (Product product : products) {
                        List<InlineKeyboardButton> rowInline = new ArrayList<>();
                        InlineKeyboardButton button = new InlineKeyboardButton();
                        button.setText((user_id.equals(product.getOwnerTelegramUserId()) ? (product.getActive() ? PLUS : MINUS) : EMPTY_STRING) + product.getName() + (product.getAvailableQuantity() != null ? String.format(messageProperties.getMessage17(), product.getAvailableQuantity().intValue()) : EMPTY_STRING));
                        button.setCallbackData(CallbackDataKey.PRODUCT_DETAILS.name() + CallbackDataKey.DELIMITER + product.getExternalShortId());
                        rowInline.add(button);
                        rowsInline.add(rowInline);
                    }

                    markupInline.setKeyboard(rowsInline);
                    message = itemFactory.message(chat_id, messageProperties.getMessage35());

                    ((SendMessage) message).setReplyMarkup(markupInline);
                }
            } catch (InterruptedException e) {
                log.error(e.getMessage());
            } catch (ExecutionException e) {
                log.error(e.getMessage());
            }

        } else if (update.getMessage() != null && update.getMessage().hasSuccessfulPayment()){
            try {
                RechargeCredit rechargeCredit = resourceManagerService.getCredit(user_id).get();
                message = itemFactory.message(chat_id, String.format(messageProperties.getMessage14(), rechargeCredit.getNewCredit()));
            } catch (ExecutionException e) {
                if (NoSuchElementException.class == e.getCause().getClass()) {
                    message = itemFactory.message(chat_id, messageProperties.getMessage15());
                } else {
                    log.error(e.getMessage());
                }
            } catch (InterruptedException e) {
                log.error(e.getMessage());
            }
        }
        return message;
    }
}
