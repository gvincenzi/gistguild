package com.gist.guild.gui.bot.processor;

import com.gist.guild.gui.bot.configuration.MessageProperties;
import com.gist.guild.gui.bot.factory.ItemFactory;
import com.gist.guild.gui.service.ResourceManagerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;

public abstract class UpdateProcessor {
    protected static final int CURRENCY_DIVISOR = 100;
    protected static final String START_TOKEN = "/start";
    protected static final String RESET_TOKEN = "/reset";
    protected static final long ZERO = 0L;
    protected static final String PLUS = "+";
    protected static final String MINUS = "-";
    protected static final String EMPTY_STRING = "";

    @Autowired
    ResourceManagerService resourceManagerService;

    @Autowired
    ItemFactory itemFactory;

    @Autowired
    MessageProperties messageProperties;

    public abstract BotApiMethod process(Update update, BotApiMethod message);
}
