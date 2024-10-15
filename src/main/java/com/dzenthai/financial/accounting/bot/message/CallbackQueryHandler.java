package com.dzenthai.financial.accounting.bot.message;

import com.dzenthai.financial.accounting.bot.TelegramBot;
import com.dzenthai.financial.accounting.service.manager.AccountManager;
import com.dzenthai.financial.accounting.service.manager.ExpenseManager;
import com.dzenthai.financial.accounting.service.manager.IncomeManager;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.interfaces.BotApiObject;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;


@Component
public class CallbackQueryHandler extends AbstractHandler {

    private final AccountManager accountManager;

    private final ExpenseManager expenseManager;

    private final IncomeManager incomeManager;

    public CallbackQueryHandler(
            AccountManager accountManager,
            ExpenseManager expenseManager,
            IncomeManager incomeManager
    ) {
        this.accountManager = accountManager;
        this.expenseManager = expenseManager;
        this.incomeManager = incomeManager;
    }

    @Override
    public BotApiMethod<?> answer(BotApiObject botApiObject, TelegramBot telegramBot) {
        CallbackQuery callbackQuery = (CallbackQuery) botApiObject;
        String[] words = callbackQuery.getData().split("_");
        return switch (words[0]) {
            case "ACCOUNT" -> accountManager.onCallbackQuery(callbackQuery, words, telegramBot);
            case "EXPENSE" -> expenseManager.onCallbackQuery(callbackQuery, words, telegramBot);
            case "INCOME" -> incomeManager.onCallbackQuery(callbackQuery, words, telegramBot);
            default -> null;
        };
    }
}
