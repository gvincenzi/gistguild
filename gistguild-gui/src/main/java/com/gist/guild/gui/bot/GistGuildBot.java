package com.gist.guild.gui.bot;

import com.gist.guild.commons.message.entity.*;
import com.gist.guild.gui.bot.action.entity.Action;
import com.gist.guild.gui.bot.action.entity.ActionType;
import com.gist.guild.gui.bot.factory.ItemFactory;
import com.gist.guild.gui.service.ResourceManagerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutionException;

@Slf4j
@Component
public class GistGuildBot extends TelegramLongPollingBot {
    @Value("${gistguild.bot.username}")
    private String botUsername;

    @Value("${gistguild.bot.token}")
    private String botToken;

    @Value("${gistguild.bot.entryFreeCredit.active}")
    private Boolean entryFreeCredit;

    @Value("${gistguild.bot.entryFreeCredit.amount}")
    private Long entryFreeCreditAmount;

    @Autowired
    ResourceManagerService resourceManagerService;

    @Autowired
    ItemFactory itemFactory;

    @Override
    public void onUpdateReceived(Update update) {
        BotApiMethod message = null;
        Long user_id = null;

        if (update.hasCallbackQuery()) {
            // Set variables
            String call_data = update.getCallbackQuery().getData();
            user_id = update.getCallbackQuery().getFrom().getId();
            Long chat_id = update.getCallbackQuery().getMessage().getChatId();

            if (call_data.equals("iscrizione")) {
                message = itemFactory.message(chat_id, "Per iscriversi al sistema basta scrivere un messaggio in questa chat con solo la propria email.\nSarete iscritti al sistema con i dati del vostro account Telegram e con la mail che avrete indicato");
            } else if (call_data.equals("cancellazione")) {
                try {
                    Participant participant = resourceManagerService.findParticipantByTelegramId(user_id).get();
                    participant.setActive(Boolean.FALSE);
                    resourceManagerService.addOrUpdateParticipant(participant).get();
                    message = itemFactory.message(chat_id, "Utente rimosso correttamente");
                } catch (InterruptedException | ExecutionException e) {
                    log.error(e.getMessage());
                }
            } else if (call_data.equals("creditoResiduo")) {
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
            } else if (call_data.startsWith("listaOrdini")) {
                message = getOrderList(message, user_id, chat_id);
            } else if (call_data.startsWith("catalogo")) {
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
                            button.setCallbackData("detailProduct#" + product.getExternalShortId());
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
            } else if (call_data.startsWith("detailProduct#")) {
                try {
                    String[] split = call_data.split("#");
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
                    button.setCallbackData("selectProduct#" + product.getExternalShortId());
                    rowInline.add(button);
                    InlineKeyboardButton button2 = new InlineKeyboardButton();
                    button2.setText("Torna alla lista dei prodotti");
                    button2.setCallbackData("catalogo");
                    rowInline.add(button2);

                    InlineKeyboardButton button3 = new InlineKeyboardButton();
                    button3.setText("Modifica URL");
                    button3.setCallbackData("catalogmng#url#" + product.getExternalShortId());
                    rowInline1.add(button3);
                    InlineKeyboardButton button4 = new InlineKeyboardButton();
                    button4.setText(product.getActive() ? "Disattiva dal catalogo" : "Attiva nel catalogo");
                    button4.setCallbackData("catalogmng#active#" + product.getExternalShortId());
                    rowInline1.add(button4);

                    InlineKeyboardButton button5 = new InlineKeyboardButton();
                    button5.setText("Torna al menù principale");
                    button5.setCallbackData("welcomeMenu");
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
            } else if (call_data.startsWith("catalogmng#url#")) {
                String[] split = call_data.split("#");
                Long productExternalShortId = Long.parseLong(split[2]);
                Action action = new Action();
                action.setActionType(ActionType.PRODUCT_URL);
                action.setProductIdToManage(productExternalShortId);
                action.setTelegramUserId(user_id);
                resourceManagerService.saveAction(action);
                message = itemFactory.productUrlManagement(chat_id);
            } else if (call_data.startsWith("catalogmng#active#")) {
                try {
                    String[] split = call_data.split("#");
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
            } else if (call_data.startsWith("selectProduct#")) {
                try {
                    Participant participant = resourceManagerService.findParticipantByTelegramId(user_id).get();
                    String[] split = call_data.split("#");
                    Long productExternalShortId = Long.parseLong(split[1]);
                    Product product = resourceManagerService.getProduct(productExternalShortId).get();

                    if (product.getAvailableQuantity() == null && !product.getDelivery()) {
                        Order order = new Order();
                        order.setCustomerMail(participant.getMail());
                        order.setCustomerTelegramUserId(participant.getTelegramUserId());
                        order.setProductId(product.getId());
                        order.setProductName(product.getName());
                        resourceManagerService.addOrUpdateOrder(order).get();
                        message = getOrderList(message, user_id, chat_id);
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
                } catch (InterruptedException e) {
                    log.error(e.getMessage());
                } catch (ExecutionException e) {
                    log.error(e.getMessage());
                }
            } else if (call_data.equalsIgnoreCase("usermng")) {
                Action action = new Action();
                action.setActionType(ActionType.USER_SEARCH);
                action.setTelegramUserId(user_id);
                resourceManagerService.saveAction(action);
                message = itemFactory.message(chat_id, "Scrivi la mail dell'utente che vuoi gestire");
            } else if (call_data.equalsIgnoreCase("usermng#end")) {
                Action actionInProgress = getActionInProgress(user_id);
                if (actionInProgress != null && actionInProgress.getTelegramUserIdToManage() != null) {
                    resourceManagerService.deleteActionInProgress(actionInProgress);
                    message = itemFactory.message(chat_id, "Modifica terminata.\nClicca su /start per tornare al menu principale.");
                }
            } else if (call_data.equalsIgnoreCase("usermng#cancellazione")) {
                Action actionInProgress = getActionInProgress(user_id);
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
            } else if (call_data.equalsIgnoreCase("usermng#ricaricaCredito")) {
                Action actionInProgress = getActionInProgress(user_id);
                if (actionInProgress != null && actionInProgress.getTelegramUserIdToManage() != null) {
                    Action action = new Action();
                    action.setActionType(ActionType.USER_CREDIT);
                    action.setTelegramUserIdToManage(actionInProgress.getTelegramUserIdToManage());
                    action.setTelegramUserId(user_id);
                    resourceManagerService.deleteActionInProgress(actionInProgress);
                    resourceManagerService.saveAction(action);
                    message = itemFactory.userManagementCredit(chat_id);
                }
            } else if (call_data.startsWith("usermng#ricaricaCredito#")) {
                String[] split = call_data.split("#");
                Long credit = Long.parseLong(split[2]);
                Action actionInProgress = getActionInProgress(user_id);
                if (actionInProgress != null && actionInProgress.getTelegramUserIdToManage() != null && ActionType.USER_CREDIT.equals(actionInProgress.getActionType())) {
                    resourceManagerService.deleteActionInProgress(actionInProgress);

                    Participant participantToRecharge = null;
                    try {
                        RechargeCredit rechargeCreditLast = resourceManagerService.getCredit(user_id).get();
                        participantToRecharge = resourceManagerService.findParticipantByTelegramId(actionInProgress.getTelegramUserIdToManage()).get();
                        RechargeCredit rechargeCredit = new RechargeCredit();
                        rechargeCredit.setCustomerMail(participantToRecharge.getMail());
                        rechargeCredit.setCustomerTelegramUserId(participantToRecharge.getTelegramUserId());
                        rechargeCredit.setNewCredit(credit);
                        rechargeCredit.setOldCredit(rechargeCreditLast.getOldCredit());
                        rechargeCredit.setRechargeUserCreditType(RechargeCreditType.TELEGRAM);
                        resourceManagerService.addCredit(rechargeCredit).get();
                        message = itemFactory.message(chat_id, "Credito aggiornato correttamente\nClicca su /start per tornare al menu principale.");
                    } catch (InterruptedException | ExecutionException e) {
                        log.error(e.getMessage());
                    }

                }
            } else if (call_data.startsWith("welcomeMenu")) {
                message = itemFactory.welcomeMessage(update.getCallbackQuery().getMessage(), user_id);
            }
        } else if (update.hasMessage()) {
            user_id = update.getMessage().getFrom().getId();
            Long chat_id = update.getMessage().getChatId();

            Action actionInProgress = getActionInProgress(user_id);

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
                        message = getOrderList(message, user_id, chat_id);
                    }
                } catch (InterruptedException | ExecutionException e) {
                    log.error(e.getMessage());
                }
            } else if (update.getMessage().getText() != null && !BotUtils.isNumeric(update.getMessage().getText()) && actionInProgress !=null && ActionType.SELECT_ADDRESS.equals(actionInProgress.getActionType())) {
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
                    message = getOrderList(message, user_id, chat_id);
                } catch (InterruptedException | ExecutionException e) {
                    log.error(e.getMessage());
                }
            }
        }

        try {
            execute(message); // Call method to send the message
        } catch (TelegramApiException e) {
            log.error(e.getMessage());
        }
    }

    private BotApiMethod getOrderList(BotApiMethod message, Long user_id, Long chat_id) {
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
                    button.setText("ID#"+order.getExternalShortId()+" : "+order.getProductName());
                    button.setCallbackData("orderDetails#" + order.getExternalShortId());
                    rowInline.add(button);
                    rowsInline.add(rowInline);
                }

                List<InlineKeyboardButton> rowInline = new ArrayList<>();
                InlineKeyboardButton button2 = new InlineKeyboardButton();
                button2.setText("Torna al menù principale");
                button2.setCallbackData("welcomeMenu");
                rowInline.add(button2);
                rowsInline.add(rowInline);

                markupInline.setKeyboard(rowsInline);
                message = itemFactory.message(chat_id, "Qui di seguito la lista dei tuoi ordini in corso, per accedere ai dettagli cliccare sull'ordine:\n");

                ((SendMessage) message).setReplyMarkup(markupInline);
            }
        } catch (InterruptedException | ExecutionException e) {
            log.error(e.getMessage());
        }
        return message;
    }

    private Action getActionInProgress(Long user_id) {
        return resourceManagerService.getActionInProgress(user_id);
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }
}
