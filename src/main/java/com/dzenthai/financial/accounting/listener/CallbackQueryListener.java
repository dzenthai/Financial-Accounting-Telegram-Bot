package com.dzenthai.financial.accounting.listener;

import com.dzenthai.financial.accounting.bot.TelegramBot;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;


public interface CallbackQueryListener {

    BotApiMethod<?> onCallbackQuery(CallbackQuery callbackQuery, String[] words, TelegramBot telegramBot);
}
