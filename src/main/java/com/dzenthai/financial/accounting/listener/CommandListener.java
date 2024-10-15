package com.dzenthai.financial.accounting.listener;

import com.dzenthai.financial.accounting.bot.TelegramBot;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Message;


public interface CommandListener {

     BotApiMethod<?> onCommand(Message message, TelegramBot telegramBot);
}
