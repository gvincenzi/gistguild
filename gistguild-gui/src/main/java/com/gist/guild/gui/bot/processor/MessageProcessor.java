package com.gist.guild.gui.bot.processor;

import com.gist.guild.commons.message.entity.*;
import com.gist.guild.gui.bot.BotUtils;
import com.gist.guild.gui.bot.action.entity.Action;
import com.gist.guild.gui.bot.action.entity.ActionType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.concurrent.ExecutionException;

@Slf4j
@Service
public class MessageProcessor extends UpdateProcessor {

    @Value("${gistguild.bot.entryFreeCredit.active}")
    private Boolean entryFreeCredit;

    @Value("${gistguild.bot.entryFreeCredit.amount}")
    private Long entryFreeCreditAmount;

    @Override
    public BotApiMethod process(Update update, BotApiMethod message) {
        Long user_id = update.getMessage().getFrom().getId();
        Long chat_id = update.getMessage().getChatId();

        Action actionInProgress = resourceManagerService.getActionInProgress(user_id);

        if (update.getMessage().getText() != null && update.getMessage().getText().equalsIgnoreCase("/start")) {
            message = itemFactory.welcomeMessage(update.getMessage(), user_id);
        } else if (update.getMessage().getText() != null && update.getMessage().getText().contains("@") && actionInProgress == null) {
            Participant participant = null;
            try {
                participant = resourceManagerService.addOrUpdateParticipant(BotUtils.createParticipant(update.getMessage().getFrom(), update.getMessage().getText())).get();
                message = itemFactory.message(chat_id, String.format("Nuovo utente iscritto correttamente : una mail di conferma è stata inviata all'indirizzo %s.\nClicca su /start per iniziare.", participant.getMail()));
                if (entryFreeCredit) {
                    RechargeCredit rechargeCredit = new RechargeCredit();
                    rechargeCredit.setCustomerMail(participant.getMail());
                    rechargeCredit.setCustomerTelegramUserId(participant.getTelegramUserId());
                    rechargeCredit.setNewCredit(entryFreeCreditAmount);
                    rechargeCredit.setOldCredit(0L);
                    rechargeCredit.setRechargeUserCreditType(RechargeCreditType.TELEGRAM);
                    resourceManagerService.addCredit(rechargeCredit).get();
                    message = itemFactory.message(chat_id, String.format("Nuovo utente iscritto correttamente : una mail di conferma è stata inviata all'indirizzo specificato.\nRiceverai anche un credito di %s € in regalo da utilizzare da subito per gli acquisti di prodotti dal catalogo.\nClicca su /start per iniziare.", entryFreeCreditAmount));
                } else {
                    message = itemFactory.message(chat_id, "Nuovo utente iscritto correttamente : una mail di conferma è stata inviata all'indirizzo specificato.\nClicca su /start per iniziare.");
                }
            } catch (InterruptedException | ExecutionException e) {
                log.error(e.getMessage());
            }
        } else if (update.getMessage().getText() != null && !update.getMessage().getText().contains("@") && actionInProgress != null && !(ActionType.SELECT_PRODUCT.equals(actionInProgress.getActionType()) || ActionType.SELECT_ADDRESS.equals(actionInProgress.getActionType()))) {
            message = itemFactory.welcomeMessage(update.getMessage(), user_id);
        } else if (update.getMessage().getText().contains("@") && ActionType.USER_SEARCH.equals(actionInProgress.getActionType())) {
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
                    order.setCustomerMail(participant.getMail());
                    order.setCustomerTelegramUserId(participant.getTelegramUserId());
                    order.setProductId(product.getId());
                    order.setProductName(product.getName());
                    order.setQuantity(Long.parseLong(update.getMessage().getText()));
                    resourceManagerService.addOrUpdateOrder(order).get();
                    message = BotUtils.getOrderList(message, user_id, chat_id, resourceManagerService, itemFactory);
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
                order.setCustomerMail(participant.getMail());
                order.setCustomerTelegramUserId(participant.getTelegramUserId());
                order.setProductId(product.getId());
                order.setProductName(product.getName());
                order.setQuantity(actionInProgress.getQuantity());
                order.setAddress(update.getMessage().getText());
                resourceManagerService.addOrUpdateOrder(order).get();
                message = BotUtils.getOrderList(message, user_id, chat_id, resourceManagerService, itemFactory);
            } catch (InterruptedException | ExecutionException e) {
                log.error(e.getMessage());
            }
        }
        return message;
    }
}
