package com.dzenthai.financial.accounting.listener;

import com.dzenthai.financial.accounting.bot.TelegramBot;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Message;


public interface MessageListener {

    public BotApiMethod<?> onMessage(Message message, TelegramBot telegramBot);
}
