package com.gist.guild.gui.bot;

import com.gist.guild.commons.message.entity.Participant;
import com.gist.guild.gui.bot.action.entity.Action;
import com.gist.guild.gui.bot.factory.ItemFactory;
import com.gist.guild.gui.service.ResourceManagerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@Slf4j
@Component
public class GistGuildBot extends TelegramLongPollingBot {
    @Value("${gistguild.bot.username}")
    private String botUsername;

    @Value("${gistguild.bot.token}")
    private String botToken;

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
                message = itemFactory.message(chat_id, "Per iscriversi al sistema basta scrivere un messaggio in questa chat con solo la propria email.\nInMediArt GasSMan vi iscriverà al sistema con i dati del vostro account Telegram e con la mail che avrete indicato");
            } else if (call_data.equals("cancellazione")) {
                try {
                    Participant participant = resourceManagerService.findParticipantByTelegramId(user_id).get();
                    participant.setActive(Boolean.FALSE);
                    resourceManagerService.addOrUpdateParticipant(participant).get();
                    message = itemFactory.message(chat_id, "Utente rimosso correttamente");
                } catch (InterruptedException e) {
                    log.error(e.getMessage());
                } catch (ExecutionException e) {
                    log.error(e.getMessage());
                }
            }
        } else if (update.hasMessage()) {
            user_id = update.getMessage().getFrom().getId();
            Long chat_id = update.getMessage().getChatId();

            Action actionInProgress = getActionInProgress(user_id);

            if (update.getMessage().getText() != null && update.getMessage().getText().equalsIgnoreCase("/start")) {
                message = itemFactory.welcomeMessage(update.getMessage(), user_id);
            } else if (update.getMessage().getText() != null && update.getMessage().getText().contains("@") && actionInProgress ==null) {
                Participant participant = null;
                try {
                    participant = resourceManagerService.addOrUpdateParticipant(BotUtils.createParticipant(update.getMessage().getFrom(), update.getMessage().getText())).get();
                    message = itemFactory.message(chat_id, String.format("Nuovo utente iscritto correttamente : una mail di conferma è stata inviata all'indirizzo %s.\nClicca su /start per iniziare.",participant.getMail()));
                    /*if(entryFreeCredit){
                        resourceManagerService.addCredit(user_id, BigDecimal.valueOf(entryFreeCreditAmount*100));
                        message = itemFactory.message(chat_id, String.format("Nuovo utente iscritto correttamente : una mail di conferma è stata inviata all'indirizzo specificato.\nRiceverai anche un credito di %s € in regalo da utilizzare da subito per gli acquisti di prodotti dal catalogo.\nClicca su /start per iniziare.", entryFreeCreditAmount));
                    } else{
                        message = itemFactory.message(chat_id, "Nuovo utente iscritto correttamente : una mail di conferma è stata inviata all'indirizzo specificato.\nClicca su /start per iniziare.");
                    }*/
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
