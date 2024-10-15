package com.dzenthai.financial.accounting.bot.message;

import com.dzenthai.financial.accounting.bot.TelegramBot;
import org.telegram.telegrambots.meta.api.interfaces.BotApiObject;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;


public abstract class AbstractHandler {

    public abstract BotApiMethod<?> answer(BotApiObject botApiObject, TelegramBot telegramBot);
}
