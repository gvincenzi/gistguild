package com.gist.guild.gui.bot;

import com.gist.guild.gui.bot.configuration.MessageProperties;
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
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
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

    @Autowired
    MessageProperties messageProperties;

    @Override
    public void onUpdateReceived(Update update) {
        Message loading = null;
        Long chat_id = null;
        BotApiMethod message = null;

        if(update.hasPreCheckoutQuery()){
            /* CHECK PAYLOAD */
            message = new AnswerPreCheckoutQuery();
            ((AnswerPreCheckoutQuery)message).setOk(true);
            ((AnswerPreCheckoutQuery)message).setPreCheckoutQueryId(update.getPreCheckoutQuery().getId());
        } else if (update.hasCallbackQuery()) {
            chat_id = update.getCallbackQuery().getMessage().getChatId();
            loading = loadingMessage(chat_id);
            message = callbackProcessor.process(update, message);
        } else if (update.hasMessage()) {
            chat_id = update.getMessage().getChatId();
            loading = loadingMessage(chat_id);
            message = messageProcessor.process(update, message);
        }

        try {
            if(message != null) execute(message);

            if(message != null && message instanceof AnswerPreCheckoutQuery){
                messageProcessor.process(update, message);
            }

            if(loading != null && chat_id != null) {
                DeleteMessage deleteMessage = new DeleteMessage();
                deleteMessage.setChatId(chat_id);
                deleteMessage.setMessageId(loading.getMessageId());
                execute(deleteMessage); // Call method to delete a message
            }
        } catch (TelegramApiException e) {
            log.error(e.getMessage());
        }
    }

    public Message loadingMessage(Long chat_id){
        return sendMessage(chat_id,messageProperties.getLoadingMessage());
    }

    public Message sendMessage(Long chat_id, String text){
        try {
            return execute(itemFactory.message(chat_id, text)); // Call method to send the message
        } catch (TelegramApiException e) {
            log.error(e.getMessage());
        }
        return null;
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
