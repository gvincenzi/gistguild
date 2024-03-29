package com.gist.guild.gui.bot.processor;

import com.gist.guild.commons.exception.GistGuildGenericException;
import com.gist.guild.commons.message.entity.*;
import com.gist.guild.gui.bot.BotUtils;
import com.gist.guild.gui.bot.action.entity.Action;
import com.gist.guild.gui.bot.action.entity.ActionType;
import com.gist.guild.gui.bot.factory.CallbackDataKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.invoices.SendInvoice;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.payments.LabeledPrice;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutionException;

@Slf4j
@Service
public class CallbackProcessor extends UpdateProcessor {
    @Value("${gistguild.bot.stripe.token}")
    private String stripeToken;

    @Value("${gistguild.bot.entryFreeCredit.amount}")
    private Long entryFreeCreditAmount;

    @Override
    public BotApiMethod process(Update update, BotApiMethod message) {
        Long user_id = update.getCallbackQuery().getFrom().getId();
        String call_data = update.getCallbackQuery().getData();
        Long chat_id = update.getCallbackQuery().getMessage().getChatId();

        if (call_data.equals(CallbackDataKey.REGISTRATION.name())) {
            Participant participant = null;
            try {
                participant = resourceManagerService.addOrUpdateParticipant(BotUtils.createParticipant(update.getCallbackQuery().getFrom())).get();
                message = itemFactory.message(chat_id, String.format(messageProperties.getMessage10(), participant.getNickname()));
                RechargeCredit rechargeCredit = new RechargeCredit();
                rechargeCredit.setCustomerNickname(participant.getNickname());
                rechargeCredit.setCustomerTelegramUserId(participant.getTelegramUserId());
                rechargeCredit.setNewCredit(entryFreeCreditAmount);
                rechargeCredit.setOldCredit(ZERO);
                rechargeCredit.setRechargeUserCreditType(RechargeCreditType.FREE);
                resourceManagerService.addCredit(rechargeCredit).get();
                if (entryFreeCreditAmount > 0) {
                    message = itemFactory.message(chat_id, String.format(messageProperties.getMessage11(), participant.getNickname(), entryFreeCreditAmount));
                } else {
                    message = itemFactory.message(chat_id,  String.format(messageProperties.getMessage10(), participant.getNickname()));
                }
            } catch (InterruptedException | ExecutionException e) {
                log.error(e.getMessage());
            }
        } else if (call_data.equals(CallbackDataKey.CANCELLATION.name())) {
            try {
                Participant participant = resourceManagerService.findParticipantByTelegramId(user_id).get();
                participant.setActive(Boolean.FALSE);
                resourceManagerService.addOrUpdateParticipant(participant).get();
                message = itemFactory.message(chat_id, messageProperties.getMessage13());
            } catch (InterruptedException | ExecutionException e) {
                log.error(e.getMessage());
            }
        } else if (call_data.equals(CallbackDataKey.CREDIT.name())) {
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
        } else if (call_data.startsWith(CallbackDataKey.ORDER_LIST.name())) {
            message = BotUtils.getOrderList(message, user_id, chat_id, resourceManagerService, itemFactory, messageProperties, Boolean.FALSE);
        } else if (call_data.startsWith(CallbackDataKey.ORDER_PAID_LIST.name())) {
            message = BotUtils.getOrderList(message, user_id, chat_id, resourceManagerService, itemFactory, messageProperties, Boolean.TRUE);
        } else if (call_data.startsWith(CallbackDataKey.CATALOG.name())) {
            try {
                List<Product> products = resourceManagerService.getProducts().get();
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
                    message = itemFactory.message(chat_id, messageProperties.getMessage18());

                    ((SendMessage) message).setReplyMarkup(markupInline);
                }
            } catch (InterruptedException e) {
                log.error(e.getMessage());
            } catch (ExecutionException e) {
                log.error(e.getMessage());
            }
        } else if (call_data.equalsIgnoreCase(CallbackDataKey.SEARCH_PRODUCT.name())) {
            Action action = new Action();
            action.setActionType(ActionType.SEARCH_PRODUCT);
            action.setTelegramUserId(user_id);
            resourceManagerService.saveAction(action);
            message = itemFactory.message(chat_id, messageProperties.getMessage34());
        } else if (call_data.startsWith(CallbackDataKey.PRODUCT_DETAILS.name() + CallbackDataKey.DELIMITER)) {
            try {
                String[] split = call_data.split(CallbackDataKey.DELIMITER);
                Long productExternalShortId = Long.parseLong(split[1]);
                Product product = resourceManagerService.getProduct(productExternalShortId).get();
                message = itemFactory.message(chat_id, messageProperties.toString(product));
                InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
                List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
                List<InlineKeyboardButton> rowInline = new ArrayList<>();
                List<InlineKeyboardButton> rowInline1 = new ArrayList<>();
                List<InlineKeyboardButton> rowInline2 = new ArrayList<>();
                List<InlineKeyboardButton> rowInline3 = new ArrayList<>();
                InlineKeyboardButton button = new InlineKeyboardButton();
                button.setText(messageProperties.getMenuItem14());
                button.setCallbackData(CallbackDataKey.PRODUCT_SELECT.name() + CallbackDataKey.DELIMITER + product.getExternalShortId());
                rowInline.add(button);
                InlineKeyboardButton button2 = new InlineKeyboardButton();
                button2.setText(messageProperties.getMenuItem15());
                button2.setCallbackData(CallbackDataKey.CATALOG.name());
                rowInline.add(button2);

                InlineKeyboardButton button3 = new InlineKeyboardButton();
                button3.setText(messageProperties.getMenuItem16());
                button3.setCallbackData(CallbackDataKey.ADMIN_CATALOG_MANAGEMENT.name() + CallbackDataKey.DELIMITER + CallbackDataKey.URL.name() + CallbackDataKey.DELIMITER + product.getExternalShortId());
                rowInline1.add(button3);
                InlineKeyboardButton button4 = new InlineKeyboardButton();
                button4.setText(product.getActive() ? messageProperties.getMenuItem17() : messageProperties.getMenuItem18());
                button4.setCallbackData(CallbackDataKey.ADMIN_CATALOG_MANAGEMENT.name() + CallbackDataKey.DELIMITER + CallbackDataKey.ACTIVATION.name() + CallbackDataKey.DELIMITER + product.getExternalShortId());
                rowInline1.add(button4);

                InlineKeyboardButton buttonMessageToProductOwner = new InlineKeyboardButton();
                buttonMessageToProductOwner.setText(messageProperties.getMenuItem12());
                buttonMessageToProductOwner.setCallbackData(CallbackDataKey.PRODUCT_OWNER_MESSAGE.name() + CallbackDataKey.DELIMITER + product.getExternalShortId());
                rowInline2.add(buttonMessageToProductOwner);

                InlineKeyboardButton button5 = new InlineKeyboardButton();
                button5.setText(messageProperties.getMenuItem13());
                button5.setCallbackData(CallbackDataKey.WELCOME.name());
                rowInline3.add(button5);
                // Set the keyboard to the markup
                rowsInline.add(rowInline);
                if (resourceManagerService.findParticipantByTelegramId(user_id).get().getAdministrator()) {
                    rowsInline.add(rowInline1);
                }
                rowsInline.add(rowInline2);
                rowsInline.add(rowInline3);
                // Add it to the message
                markupInline.setKeyboard(rowsInline);
                ((SendMessage) message).setReplyMarkup(markupInline);
            } catch (InterruptedException e) {
                log.error(e.getMessage());
            } catch (ExecutionException e) {
                log.error(e.getMessage());
            }
        } else if (call_data.startsWith(CallbackDataKey.ADMIN_CATALOG_MANAGEMENT.name() + CallbackDataKey.DELIMITER + CallbackDataKey.URL.name() + CallbackDataKey.DELIMITER)) {
            String[] split = call_data.split(CallbackDataKey.DELIMITER);
            Long productExternalShortId = Long.parseLong(split[2]);
            Action action = new Action();
            action.setActionType(ActionType.PRODUCT_URL);
            action.setProductIdToManage(productExternalShortId);
            action.setTelegramUserId(user_id);
            resourceManagerService.saveAction(action);
            message = itemFactory.productUrlManagement(chat_id);
        } else if (call_data.startsWith(CallbackDataKey.ADMIN_CATALOG_MANAGEMENT.name() + CallbackDataKey.DELIMITER + CallbackDataKey.ACTIVATION.name() + CallbackDataKey.DELIMITER)) {
            try {
                String[] split = call_data.split(CallbackDataKey.DELIMITER);
                Long productExternalShortId = Long.parseLong(split[2]);
                Product product = resourceManagerService.getProduct(productExternalShortId).get();
                product.setActive(!product.getActive());
                product = resourceManagerService.updateProduct(product).get();
                message = itemFactory.message(chat_id, String.format(messageProperties.getMessage19(), product.getName()));
            } catch (InterruptedException e) {
                log.error(e.getMessage());
            } catch (ExecutionException e) {
                log.error(e.getMessage());
            }
        } else if (call_data.startsWith(CallbackDataKey.PRODUCT_SELECT.name() + CallbackDataKey.DELIMITER)) {
            try {
                Participant participant = resourceManagerService.findParticipantByTelegramId(user_id).get();
                String[] split = call_data.split(CallbackDataKey.DELIMITER);
                Long productExternalShortId = Long.parseLong(split[1]);
                Product product = resourceManagerService.getProduct(productExternalShortId).get();

                if (product.getAvailableQuantity() == null && !product.getDelivery()) {
                    Order order = new Order();
                    order.setCustomerNickname(participant.getNickname());
                    order.setCustomerTelegramUserId(participant.getTelegramUserId());
                    order.setProductId(product.getId());
                    order.setProductName(product.getName());
                    order.setProductOwnerTelegramUserId(product.getOwnerTelegramUserId());
                    order.setProductUrl(product.getUrl());
                    order.setProductPassword(product.getPassword());
                    order.setAmount(product.getPrice());
                    try {
                        order = resourceManagerService.addOrUpdateOrder(order);
                        message = itemFactory.message(chat_id, String.format(messageProperties.getMessage20(), order.getExternalShortId()));
                    } catch (GistGuildGenericException e) {
                        message = itemFactory.message(chat_id, String.format(messageProperties.getError1(), e.getMessage()));
                    }
                } else if (product.getAvailableQuantity() != null) {
                    Action action = new Action();
                    action.setActionType(ActionType.SELECT_PRODUCT);
                    action.setTelegramUserId(user_id);
                    action.setSelectedProductId(product.getExternalShortId());
                    resourceManagerService.saveAction(action);
                    message = itemFactory.selectProductQuantity(chat_id);
                } else if (product.getAvailableQuantity() == null && product.getDelivery()) {
                    Action action = new Action();
                    action.setActionType(ActionType.SELECT_ADDRESS);
                    action.setTelegramUserId(user_id);
                    action.setSelectedProductId(product.getExternalShortId());
                    resourceManagerService.saveAction(action);
                    message = itemFactory.selectAddress(chat_id);
                }
            } catch (InterruptedException | ExecutionException e) {
                log.error(e.getMessage());
            }
        } else if (call_data.startsWith(CallbackDataKey.PRODUCT_OWNER_MESSAGE.name() + CallbackDataKey.DELIMITER)) {
            String[] split = call_data.split(CallbackDataKey.DELIMITER);
            Long productExternalShortId = Long.parseLong(split[1]);

            Action action = new Action();
            action.setActionType(ActionType.PRODUCT_OWNER_MESSAGE);
            action.setTelegramUserId(user_id);
            action.setSelectedProductId(productExternalShortId);
            resourceManagerService.saveAction(action);
            message = itemFactory.sendMessageToProductOwner(chat_id);
        } else if (call_data.startsWith(CallbackDataKey.ORDER_DETAILS.name() + CallbackDataKey.DELIMITER)) {
            String[] split = call_data.split(CallbackDataKey.DELIMITER);
            Long orderExternalShortId = Long.parseLong(split[1]);
            try{
                Order order = resourceManagerService.getOrder(orderExternalShortId).get();
                message = itemFactory.orderDetailsMessageBuilder(chat_id, order);
            } catch (InterruptedException | ExecutionException e) {
                log.error(e.getMessage());
            }
        } else if (call_data.startsWith(CallbackDataKey.PAYMENT.name() + CallbackDataKey.DELIMITER)) {
            String[] split = call_data.split(CallbackDataKey.DELIMITER);
            Long orderExternalShortId = Long.parseLong(split[1]);
            try {
                Participant participant = resourceManagerService.findParticipantByTelegramId(user_id).get();
                try {
                    resourceManagerService.payOrder(orderExternalShortId, participant.getNickname(), participant.getTelegramUserId());
                    message = itemFactory.message(chat_id, messageProperties.getMessage22());
                } catch (GistGuildGenericException e) {
                    message = itemFactory.message(chat_id, String.format(messageProperties.getError2(), e.getMessage()));
                }
            } catch (InterruptedException | ExecutionException e) {
                log.error(e.getMessage());
            }
        } else if (call_data.startsWith(CallbackDataKey.ORDER_DELETE.name() + CallbackDataKey.DELIMITER)) {
            String[] split = call_data.split(CallbackDataKey.DELIMITER);
            Long orderExternalShortId = Long.parseLong(split[1]);
            try {
                Order order = resourceManagerService.getOrder(orderExternalShortId).get();
                order.setDeleted(Boolean.TRUE);
                order = resourceManagerService.addOrUpdateOrder(order);
                message = itemFactory.message(chat_id, String.format(messageProperties.getMessage21(), order.getExternalShortId()));
            } catch (GistGuildGenericException e) {
                message = itemFactory.message(chat_id, String.format(messageProperties.getError1(), e.getMessage()));
            } catch (InterruptedException | ExecutionException e) {
                log.error(e.getMessage());
            }
        } else if (call_data.equalsIgnoreCase(CallbackDataKey.ADD_CREDIT.name())) {
            message = itemFactory.userCredit(chat_id);
        } else if (call_data.startsWith(CallbackDataKey.ADD_CREDIT.name() + CallbackDataKey.DELIMITER)) {
            String[] split = call_data.split(CallbackDataKey.DELIMITER);
            StringBuilder payload = new StringBuilder();
            payload.append(user_id);
            payload.append(split[1]);
            LabeledPrice price = new LabeledPrice();
            price.setLabel(messageProperties.getMenuItem5());
            price.setAmount(Integer.parseInt(split[1]) * CURRENCY_DIVISOR);

            message = new SendInvoice();
            ((SendInvoice) message).setProviderToken(stripeToken);
            List<LabeledPrice> prices = new ArrayList<>();
            prices.add(price);
            ((SendInvoice) message).setPrices(prices);
            ((SendInvoice) message).setTitle(messageProperties.getInvoiceTitle());
            ((SendInvoice) message).setDescription(messageProperties.getInvoiceDescription());
            ((SendInvoice) message).setCurrency(messageProperties.getInvoiceCurrency());
            ((SendInvoice) message).setChatId(chat_id);
            ((SendInvoice) message).setPayload(payload.toString());
            ((SendInvoice) message).setStartParameter(messageProperties.getInvoiceStartParameter());

        } else if (call_data.equalsIgnoreCase(CallbackDataKey.USER_MANAGEMENT.name())) {
            Action actionInProgress = resourceManagerService.getActionInProgress(user_id);
            if (actionInProgress != null) {
                resourceManagerService.deleteActionInProgress(actionInProgress);
            }
            Action action = new Action();
            action.setActionType(ActionType.USER_SEARCH);
            action.setTelegramUserId(user_id);
            resourceManagerService.saveAction(action);
            message = itemFactory.message(chat_id, messageProperties.getMessage23());
        } else if (call_data.equalsIgnoreCase(CallbackDataKey.USER_MANAGEMENT.name() + CallbackDataKey.DELIMITER + CallbackDataKey.END.name())) {
            Action actionInProgress = resourceManagerService.getActionInProgress(user_id);
            if (actionInProgress != null && actionInProgress.getTelegramUserIdToManage() != null) {
                resourceManagerService.deleteActionInProgress(actionInProgress);
                message = itemFactory.message(chat_id, messageProperties.getMessage24());
            }
        } else if (call_data.equalsIgnoreCase(CallbackDataKey.USER_MANAGEMENT.name() + CallbackDataKey.DELIMITER + CallbackDataKey.CANCELLATION.name())) {
            Action actionInProgress = resourceManagerService.getActionInProgress(user_id);
            if (actionInProgress != null && actionInProgress.getTelegramUserIdToManage() != null) {
                Participant participantToDelete = null;
                try {
                    participantToDelete = resourceManagerService.findParticipantByTelegramId(actionInProgress.getTelegramUserIdToManage()).get();
                    participantToDelete.setActive(Boolean.FALSE);
                    resourceManagerService.addOrUpdateParticipant(participantToDelete).get();
                } catch (InterruptedException | ExecutionException e) {
                    log.error(e.getMessage());
                }

                resourceManagerService.deleteActionInProgress(actionInProgress);
                message = itemFactory.message(chat_id, messageProperties.getMessage25());
            }
        } else if (call_data.equalsIgnoreCase(CallbackDataKey.USER_MANAGEMENT.name() + CallbackDataKey.DELIMITER + CallbackDataKey.ADD_CREDIT.name())) {
            Action actionInProgress = resourceManagerService.getActionInProgress(user_id);
            if (actionInProgress != null && actionInProgress.getTelegramUserIdToManage() != null) {
                Action action = new Action();
                action.setActionType(ActionType.USER_CREDIT);
                action.setTelegramUserIdToManage(actionInProgress.getTelegramUserIdToManage());
                action.setTelegramUserId(user_id);
                resourceManagerService.deleteActionInProgress(actionInProgress);
                resourceManagerService.saveAction(action);
                message = itemFactory.userManagementCredit(chat_id);
            }
        } else if (call_data.startsWith(CallbackDataKey.USER_MANAGEMENT.name() + CallbackDataKey.DELIMITER + CallbackDataKey.ADD_CREDIT.name() + CallbackDataKey.DELIMITER)) {
            String[] split = call_data.split(CallbackDataKey.DELIMITER);
            Long credit = Long.parseLong(split[2]);
            Action actionInProgress = resourceManagerService.getActionInProgress(user_id);
            if (actionInProgress != null && actionInProgress.getTelegramUserIdToManage() != null && ActionType.USER_CREDIT.equals(actionInProgress.getActionType())) {
                resourceManagerService.deleteActionInProgress(actionInProgress);

                Participant participantToRecharge = null;
                try {
                    RechargeCredit rechargeCreditLast = resourceManagerService.getCredit(actionInProgress.getTelegramUserIdToManage()).get();
                    participantToRecharge = resourceManagerService.findParticipantByTelegramId(actionInProgress.getTelegramUserIdToManage()).get();
                    RechargeCredit rechargeCredit = new RechargeCredit();
                    rechargeCredit.setCustomerNickname(participantToRecharge.getNickname());
                    rechargeCredit.setCustomerTelegramUserId(participantToRecharge.getTelegramUserId());
                    rechargeCredit.setNewCredit(rechargeCreditLast.getNewCredit() + credit);
                    rechargeCredit.setOldCredit(rechargeCreditLast.getNewCredit());
                    rechargeCredit.setRechargeUserCreditType(RechargeCreditType.ADMIN);
                    resourceManagerService.addCredit(rechargeCredit).get();
                    message = itemFactory.message(chat_id, messageProperties.getMessage26());
                } catch (InterruptedException | ExecutionException e) {
                    log.error(e.getMessage());
                }

            }
        } else if (call_data.startsWith(CallbackDataKey.WELCOME.name())) {
            message = itemFactory.welcomeMessage(update.getCallbackQuery().getMessage(), user_id);
        }
        return message;
    }
}
