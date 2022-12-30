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
    @Value("${gistguild.bot.stripe}")
    private String stripe;

    @Override
    public BotApiMethod process(Update update, BotApiMethod message) {
        Long user_id;// Set variables
        String call_data = update.getCallbackQuery().getData();
        user_id = update.getCallbackQuery().getFrom().getId();
        Long chat_id = update.getCallbackQuery().getMessage().getChatId();

        if (call_data.equals(CallbackDataKey.REGISTRATION.name())) {
            message = itemFactory.message(chat_id, "Per iscriversi al sistema basta scrivere un messaggio in questa chat con solo la propria email.\nSarete iscritti al sistema con i dati del vostro account Telegram e con la mail che avrete indicato");
        } else if (call_data.equals(CallbackDataKey.CANCELLATION)) {
            try {
                Participant participant = resourceManagerService.findParticipantByTelegramId(user_id).get();
                participant.setActive(Boolean.FALSE);
                resourceManagerService.addOrUpdateParticipant(participant).get();
                message = itemFactory.message(chat_id, "Utente rimosso correttamente");
            } catch (InterruptedException | ExecutionException e) {
                log.error(e.getMessage());
            }
        } else if (call_data.equals(CallbackDataKey.CREDIT.name())) {
            try {
                RechargeCredit rechargeCredit = resourceManagerService.getCredit(user_id).get();
                message = itemFactory.message(chat_id, String.format("Il tuo credito residuo : %s €", rechargeCredit.getNewCredit()));
            } catch (ExecutionException e) {
                if (NoSuchElementException.class == e.getCause().getClass()) {
                    message = itemFactory.message(chat_id, "Non hai credito residuo");
                } else {
                    log.error(e.getMessage());
                }
            } catch (InterruptedException e) {
                log.error(e.getMessage());
            }
        } else if (call_data.startsWith(CallbackDataKey.ORDER_LIST.name())) {
            message = BotUtils.getOrderList(message, user_id, chat_id, resourceManagerService, itemFactory);
        } else if (call_data.startsWith(CallbackDataKey.CATALOG.name())) {
            try {
                Participant participant = resourceManagerService.findParticipantByTelegramId(user_id).get();
                List<Product> products = resourceManagerService.getProducts(participant.getAdministrator()).get();
                if (products.isEmpty()) {
                    message = itemFactory.message(chat_id, "Non ci sono elementi nel catalogo");
                } else {
                    InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
                    List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
                    Collections.sort(products);
                    for (Product product : products) {
                        List<InlineKeyboardButton> rowInline = new ArrayList<>();
                        InlineKeyboardButton button = new InlineKeyboardButton();
                        button.setText((participant.getAdministrator() ? (product.getActive() ? "+" : "-") : "") + product.getName() + (product.getAvailableQuantity() != null ? String.format(" (disponibilità: %d)", product.getAvailableQuantity().intValue()) : ""));
                        button.setCallbackData(CallbackDataKey.PRODUCT_DETAILS.name() + CallbackDataKey.DELIMITER + product.getExternalShortId());
                        rowInline.add(button);
                        rowsInline.add(rowInline);
                    }

                    markupInline.setKeyboard(rowsInline);
                    message = itemFactory.message(chat_id, "Qui di seguito la lista dei documenti in catalogo, selezionane uno per ordinarlo :\n");

                    ((SendMessage) message).setReplyMarkup(markupInline);
                }
            } catch (InterruptedException e) {
                log.error(e.getMessage());
            } catch (ExecutionException e) {
                log.error(e.getMessage());
            }
        } else if (call_data.startsWith(CallbackDataKey.PRODUCT_DETAILS.name() + CallbackDataKey.DELIMITER)) {
            try {
                String[] split = call_data.split(CallbackDataKey.DELIMITER);
                Long productExternalShortId = Long.parseLong(split[1]);
                Product product = resourceManagerService.getProduct(productExternalShortId).get();
                message = itemFactory.message(chat_id, product.toString());
                InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
                List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
                List<InlineKeyboardButton> rowInline = new ArrayList<>();
                List<InlineKeyboardButton> rowInline1 = new ArrayList<>();
                List<InlineKeyboardButton> rowInline2 = new ArrayList<>();
                InlineKeyboardButton button = new InlineKeyboardButton();
                button.setText("Ordina questo prodotto");
                button.setCallbackData(CallbackDataKey.PRODUCT_SELECT.name() + CallbackDataKey.DELIMITER + product.getExternalShortId());
                rowInline.add(button);
                InlineKeyboardButton button2 = new InlineKeyboardButton();
                button2.setText("Torna alla lista dei prodotti");
                button2.setCallbackData(CallbackDataKey.CATALOG.name());
                rowInline.add(button2);

                InlineKeyboardButton button3 = new InlineKeyboardButton();
                button3.setText("Modifica URL");
                button3.setCallbackData(CallbackDataKey.ADMIN_CATALOG_MANAGEMENT.name() + CallbackDataKey.DELIMITER + CallbackDataKey.URL.name() + CallbackDataKey.DELIMITER + product.getExternalShortId());
                rowInline1.add(button3);
                InlineKeyboardButton button4 = new InlineKeyboardButton();
                button4.setText(product.getActive() ? "Disattiva dal catalogo" : "Attiva nel catalogo");
                button4.setCallbackData(CallbackDataKey.ADMIN_CATALOG_MANAGEMENT.name() + CallbackDataKey.DELIMITER + CallbackDataKey.ACTIVATION.name() + product.getExternalShortId());
                rowInline1.add(button4);

                InlineKeyboardButton button5 = new InlineKeyboardButton();
                button5.setText("Torna al menù principale");
                button5.setCallbackData(CallbackDataKey.WELCOME.name());
                rowInline2.add(button5);
                // Set the keyboard to the markup
                rowsInline.add(rowInline);
                if (resourceManagerService.findParticipantByTelegramId(user_id).get().getAdministrator()) {
                    rowsInline.add(rowInline1);
                }
                rowsInline.add(rowInline2);
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
                message = itemFactory.message(chat_id, String.format("Modifica del prodotto [%s] terminata.\nClicca su /start per tornare al menu principale.", product.getName()));
            } catch (InterruptedException e) {
                log.error(e.getMessage());
            } catch (ExecutionException e) {
                log.error(e.getMessage());
            }
        } else if (call_data.startsWith(CallbackDataKey.PRODUCT_SELECT.name()+CallbackDataKey.DELIMITER)) {
            try {
                Participant participant = resourceManagerService.findParticipantByTelegramId(user_id).get();
                String[] split = call_data.split(CallbackDataKey.DELIMITER);
                Long productExternalShortId = Long.parseLong(split[1]);
                Product product = resourceManagerService.getProduct(productExternalShortId).get();

                if (product.getAvailableQuantity() == null && !product.getDelivery()) {
                    Order order = new Order();
                    order.setCustomerMail(participant.getMail());
                    order.setCustomerTelegramUserId(participant.getTelegramUserId());
                    order.setProductId(product.getId());
                    order.setProductName(product.getName());
                    order.setProductUrl(product.getUrl());
                    order.setProductPassword(product.getPassword());
                    order.setAmount(product.getPrice());
                    try {
                        order = resourceManagerService.addOrUpdateOrder(order);
                        message = itemFactory.message(chat_id,String.format("Ordine ID[%d] effettuato con successo",order.getExternalShortId()));
                    } catch (GistGuildGenericException e) {
                        message = itemFactory.message(chat_id,String.format("Errore nella registrazione dell'ordine [%s]",e.getMessage()));
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
        } else if (call_data.startsWith(CallbackDataKey.ORDER_DETAILS.name() + CallbackDataKey.DELIMITER)) {
            String[] split = call_data.split(CallbackDataKey.DELIMITER);
            Long orderExternalShortId = Long.parseLong(split[1]);
            Order order = resourceManagerService.getOrderProcessed(orderExternalShortId);
            message = itemFactory.orderDetailsMessageBuilder(chat_id, order);
        }  else if (call_data.startsWith(CallbackDataKey.PAYMENT.name()+CallbackDataKey.DELIMITER)) {
            String[] split = call_data.split(CallbackDataKey.DELIMITER);
            Long orderExternalShortId = Long.parseLong(split[1]);
            try{
                Participant participant = resourceManagerService.findParticipantByTelegramId(user_id).get();
                try {
                    resourceManagerService.payOrder(orderExternalShortId, participant.getMail(), participant.getTelegramUserId());
                    message = itemFactory.message(chat_id,"Pagamento effettuato con successo");
                } catch (GistGuildGenericException e) {
                    message = itemFactory.message(chat_id,String.format("Errore nel pagamento [%s]",e.getMessage()));
                }
            } catch (InterruptedException | ExecutionException e) {
                log.error(e.getMessage());
            }
        } else if (call_data.startsWith(CallbackDataKey.ORDER_DELETE.name() + CallbackDataKey.DELIMITER)) {
            String[] split = call_data.split(CallbackDataKey.DELIMITER);
            Long orderExternalShortId = Long.parseLong(split[1]);
            try {
                Order order = resourceManagerService.getOrderProcessed(orderExternalShortId);
                order.setDeleted(Boolean.TRUE);
                order = resourceManagerService.addOrUpdateOrder(order);
                message = itemFactory.message(chat_id,String.format("Ordine ID[%d] annullato con successo",order.getExternalShortId()));
            } catch (GistGuildGenericException e) {
                message = itemFactory.message(chat_id,String.format("Errore nella registrazione dell'ordine [%s]",e.getMessage()));
            }
        } else if (call_data.equalsIgnoreCase(CallbackDataKey.ADD_CREDIT.name())) {
            message = itemFactory.userCredit(chat_id);
        } else if(call_data.startsWith(CallbackDataKey.ADD_CREDIT.name()+CallbackDataKey.DELIMITER)) {
            String[] split = call_data.split(CallbackDataKey.DELIMITER);
            StringBuilder payload = new StringBuilder();
            payload.append(user_id);
            payload.append(split[1]);
            LabeledPrice price = new LabeledPrice();
            price.setLabel("Ricarica credito");
            price.setAmount(Integer.parseInt(split[1])*100);

            message = new SendInvoice();
            ((SendInvoice) message).setProviderToken(stripe);
            List<LabeledPrice> prices = new ArrayList<>();
            prices.add(price);
            ((SendInvoice) message).setPrices(prices);
            ((SendInvoice) message).setTitle("GIST Guild - Credito");
            ((SendInvoice) message).setDescription("Ricarica del conto prepagato");
            ((SendInvoice) message).setCurrency("EUR");
            ((SendInvoice) message).setChatId(chat_id);
            ((SendInvoice) message).setPayload(payload.toString());
            ((SendInvoice) message).setStartParameter("pay");

        } else if (call_data.equalsIgnoreCase(CallbackDataKey.USER_MANAGEMENT.name())) {
            Action action = new Action();
            action.setActionType(ActionType.USER_SEARCH);
            action.setTelegramUserId(user_id);
            resourceManagerService.saveAction(action);
            message = itemFactory.message(chat_id, "Scrivi la mail dell'utente che vuoi gestire");
        } else if (call_data.equalsIgnoreCase(CallbackDataKey.USER_MANAGEMENT.name()+CallbackDataKey.DELIMITER+CallbackDataKey.END.name())) {
            Action actionInProgress = resourceManagerService.getActionInProgress(user_id);
            if (actionInProgress != null && actionInProgress.getTelegramUserIdToManage() != null) {
                resourceManagerService.deleteActionInProgress(actionInProgress);
                message = itemFactory.message(chat_id, "Modifica terminata.\nClicca su /start per tornare al menu principale.");
            }
        } else if (call_data.equalsIgnoreCase(CallbackDataKey.USER_MANAGEMENT.name()+CallbackDataKey.DELIMITER+CallbackDataKey.CANCELLATION.name())) {
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
                message = itemFactory.message(chat_id, "Utente rimosso correttamente\nClicca su /start per tornare al menu principale.");
            }
        } else if (call_data.equalsIgnoreCase(CallbackDataKey.USER_MANAGEMENT.name()+CallbackDataKey.DELIMITER+CallbackDataKey.ADD_CREDIT.name())) {
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
        } else if (call_data.startsWith(CallbackDataKey.USER_MANAGEMENT.name()+CallbackDataKey.DELIMITER+CallbackDataKey.ADD_CREDIT.name()+CallbackDataKey.DELIMITER)) {
            String[] split = call_data.split(CallbackDataKey.DELIMITER);
            Long credit = Long.parseLong(split[2]);
            Action actionInProgress = resourceManagerService.getActionInProgress(user_id);
            if (actionInProgress != null && actionInProgress.getTelegramUserIdToManage() != null && ActionType.USER_CREDIT.equals(actionInProgress.getActionType())) {
                resourceManagerService.deleteActionInProgress(actionInProgress);

                Participant participantToRecharge = null;
                try {
                    RechargeCredit rechargeCreditLast = resourceManagerService.getCredit(user_id).get();
                    participantToRecharge = resourceManagerService.findParticipantByTelegramId(actionInProgress.getTelegramUserIdToManage()).get();
                    RechargeCredit rechargeCredit = new RechargeCredit();
                    rechargeCredit.setCustomerMail(participantToRecharge.getMail());
                    rechargeCredit.setCustomerTelegramUserId(participantToRecharge.getTelegramUserId());
                    rechargeCredit.setNewCredit(rechargeCreditLast.getNewCredit() + credit);
                    rechargeCredit.setOldCredit(rechargeCreditLast.getNewCredit());
                    rechargeCredit.setRechargeUserCreditType(RechargeCreditType.TELEGRAM);
                    resourceManagerService.addCredit(rechargeCredit).get();
                    message = itemFactory.message(chat_id, "Credito aggiornato correttamente\nClicca su /start per tornare al menu principale.");
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
