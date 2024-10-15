package com.dzenthai.financial.accounting.bot.message;

import com.dzenthai.financial.accounting.bot.TelegramBot;
import com.dzenthai.financial.accounting.service.AccountService;
import com.dzenthai.financial.accounting.service.ExpenseService;
import com.dzenthai.financial.accounting.service.IncomeService;
import com.dzenthai.financial.accounting.service.UserService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.interfaces.BotApiObject;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Message;


@Component
public class MessageHandler extends AbstractHandler {

    private final UserService userService;

    private final AccountService accountService;

    private final ExpenseService expenseService;
    private final IncomeService incomeService;

    public MessageHandler(
            UserService userService,
            AccountService accountService,
            ExpenseService expenseService,
            IncomeService incomeService) {
        this.userService = userService;
        this.accountService = accountService;
        this.expenseService = expenseService;
        this.incomeService = incomeService;
    }

    @Override
    public BotApiMethod<?> answer(BotApiObject botApiObject, TelegramBot telegramBot) {

        Message message = (Message) botApiObject;
        var user = userService.findUserByBotApiObject(botApiObject);
        return switch (user.getAction()) {
            case FREE -> accountService.accountMenu(message);
            case ACCOUNT_ADD -> accountService.saveAccount(message);
            case EXPENSE_ADD_AMOUNT -> expenseService.saveAmountExpense(message);
            case EXPENSE_ADD_NOTE -> expenseService.saveNoteExpense(message);
            case EXPENSE_EDIT_AMOUNT -> expenseService.editAmountExpense(message);
            case EXPENSE_EDIT_NOTE -> expenseService.editNoteExpense(message);
            case EXPENSE_ADD_LIMIT -> expenseService.saveExpenseLimit(message);
            case INCOME_ADD_AMOUNT -> incomeService.saveAmountIncome(message);
            case INCOME_ADD_NOTE -> incomeService.saveNoteIncome(message);
            case INCOME_EDIT_AMOUNT -> incomeService.editAmountIncome(message);
            case INCOME_EDIT_NOTE -> incomeService.editNoteIncome(message);
            default -> null;
        };
    }
}
