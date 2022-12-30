package com.gist.guild.gui.bot;

import com.gist.guild.gui.bot.factory.ItemFactory;
import com.gist.guild.gui.bot.processor.UpdateProcessor;
import com.gist.guild.gui.service.ResourceManagerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerPreCheckoutQuery;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

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

    @Autowired
    UpdateProcessor callbackProcessor;

    @Autowired
    UpdateProcessor messageProcessor;

    @Override
    public void onUpdateReceived(Update update) {
        BotApiMethod message = null;

        if(update.hasPreCheckoutQuery()){
            /* CHECK PAYLOAD */
            message = new AnswerPreCheckoutQuery();
            ((AnswerPreCheckoutQuery)message).setOk(true);
            ((AnswerPreCheckoutQuery)message).setPreCheckoutQueryId(update.getPreCheckoutQuery().getId());
        } else if (update.hasCallbackQuery()) {
            message = callbackProcessor.process(update, message);
        } else if (update.hasMessage()) {
            message = messageProcessor.process(update, message);
        }

        try {
            execute(message); // Call method to send the message

            if(message instanceof AnswerPreCheckoutQuery){
                messageProcessor.process(update, message);
            }
        } catch (TelegramApiException e) {
            log.error(e.getMessage());
        }
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
