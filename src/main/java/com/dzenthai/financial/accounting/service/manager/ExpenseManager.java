package com.dzenthai.financial.accounting.service.manager;

import com.dzenthai.financial.accounting.bot.TelegramBot;
import com.dzenthai.financial.accounting.entity.User;
import com.dzenthai.financial.accounting.entity.enums.Action;
import com.dzenthai.financial.accounting.entity.enums.ExpenseCategory;
import com.dzenthai.financial.accounting.listener.CallbackQueryListener;
import com.dzenthai.financial.accounting.service.ExpenseService;
import com.dzenthai.financial.accounting.service.UserService;
import com.dzenthai.financial.accounting.service.builder.MessageBuilder;
import com.dzenthai.financial.accounting.service.factory.ExpenseKeyboardFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;


@Component
public class ExpenseManager implements CallbackQueryListener {

    private final ExpenseService expenseService;

    private final UserService userService;

    private final MessageBuilder messageBuilder;

    private final ExpenseKeyboardFactory expenseKeyboardFactory;

    public ExpenseManager(
            ExpenseService expenseService,
            UserService userService,
            MessageBuilder messageBuilder,
            ExpenseKeyboardFactory expenseKeyboardFactory
    ) {
        this.expenseService = expenseService;
        this.userService = userService;
        this.messageBuilder = messageBuilder;
        this.expenseKeyboardFactory = expenseKeyboardFactory;
    }

    private BotApiMethod<?> askForExpenseAmountToAdd(CallbackQuery callbackQuery) {
        User user = userService.findUserByBotApiObject(callbackQuery);
        userService.updateUserAction(user.getChatId(), Action.EXPENSE_ADD_AMOUNT);
        return messageBuilder.buildMessage(
                "Введите сумму",
                callbackQuery,
                expenseKeyboardFactory.backToExpenseMenu("✋ Отмена"));
    }

    public BotApiMethod<?> askForExpenseCategoryToAdd(Message message) {
        User user = userService.findUserByBotApiObject(message);
        userService.updateUserAction(user.getChatId(), Action.EXPENSE_ADD_CATEGORY);
        return messageBuilder.buildMessage(
                "Выберите категорию трат",
                message,
                expenseKeyboardFactory.getAllExpenseCategoryKeyboard());
    }

    public BotApiMethod<?> askForExpenseNoteToAdd(CallbackQuery callbackQuery) {
        User user = userService.findUserByBotApiObject(callbackQuery);
        userService.updateUserAction(user.getChatId(), Action.EXPENSE_ADD_NOTE);
        return messageBuilder.buildMessage(
                "Введите описание (примечание)",
                callbackQuery,
                expenseKeyboardFactory.backToExpenseMenu("⏩ Пропустить"));
    }

    private BotApiMethod<?> askForExpenseAmountToEdit(CallbackQuery callbackQuery, String expenseId) {
        Long chatId = callbackQuery.getMessage().getChatId();
        userService.updateCurrentExpenseId(chatId, Long.valueOf(expenseId), Action.EXPENSE_EDIT_AMOUNT);
        return messageBuilder.buildMessage(
                "Введите новую сумму",
                callbackQuery,
                expenseKeyboardFactory.backToExpenseMenu("✋ Отмена"));
    }

    private BotApiMethod<?> askForExpenseCategoryToEdit(CallbackQuery callbackQuery, String expenseId) {
        Long chatId = callbackQuery.getMessage().getChatId();
        userService.updateCurrentExpenseId(chatId, Long.valueOf(expenseId), Action.EXPENSE_EDIT_CATEGORY);
        return messageBuilder.buildMessage(
                "Выберите новую категорию трат",
                callbackQuery,
                expenseKeyboardFactory.getAllExpenseCategoryKeyboard(expenseId));
    }

    private BotApiMethod<?> askForExpenseNoteToEdit(CallbackQuery callbackQuery, String expenseId) {
        Long chatId = callbackQuery.getMessage().getChatId();
        userService.updateCurrentExpenseId(chatId, Long.valueOf(expenseId), Action.EXPENSE_EDIT_NOTE);
        return messageBuilder.buildMessage(
                "Введите новое описание (примечание)",
                callbackQuery,
                expenseKeyboardFactory.backToExpenseMenu("✋ Отмена"));
    }

    private BotApiMethod<?> askForExpenseToDelete(CallbackQuery callbackQuery, String expenseId) {
        return messageBuilder.buildMessage(
                "Вы уверены что хотите удалить эту трату?",
                callbackQuery,
                expenseKeyboardFactory.deleteExpenseKeyboard(expenseId));
    }

    private BotApiMethod<?> askForExpenseLimitToDelete(CallbackQuery callbackQuery, String accountId) {
        return messageBuilder.buildMessage("Вы уверены что хотите удалить этот лимит?",
                callbackQuery,
                expenseKeyboardFactory.deleteLimitKeyboard(accountId));
    }

    private BotApiMethod<?> askForExpenseLimit(CallbackQuery callbackQuery, String accountId) {
        Long chatId = callbackQuery.getMessage().getChatId();
        userService.updateCurrentExpenseId(chatId, Long.valueOf(accountId), Action.EXPENSE_ADD_LIMIT);
        return messageBuilder.buildMessage(
                "Введите сумму лимита",
                callbackQuery,
                expenseKeyboardFactory.backToLimitMenu("✋ Отмена"));
    }

    @Override
    public BotApiMethod<?> onCallbackQuery(CallbackQuery callbackQuery, String[] words, TelegramBot telegramBot) {
        if (words.length == 5) {
            if (words[1].equals("EDIT")) {
                if (words[2].equals("CATEGORY")) {
                    ExpenseCategory category = ExpenseCategory.valueOf(words[3]);
                    String expenseId = words[4];
                    return expenseService.editExpenseCategory(callbackQuery, category, expenseId);
                }
            }
            if (words[1].equals("LIMIT")) {
                if (words[2].equals("DELETE")) {
                    if (words[3].equals("YES")) {
                        String accountId = words[4];
                        return expenseService.deleteLimit(callbackQuery, accountId);
                    }
                    if (words[3].equals("NO")) {
                        return expenseService.getLimitMenu(callbackQuery);
                    }
                }
            }
        }
        if (words.length == 4) {
            if (words[1].equals("ADD")) {
                if (words[2].equals("CATEGORY")) {
                    ExpenseCategory expenseCategory = ExpenseCategory.valueOf(words[3]);
                    return expenseService.saveExpenseCategory(callbackQuery, expenseCategory);
                }
            }
            if (words[1].equals("LIMIT")) {
                if (words[2].equals("ADD")) {
                    String accountId = words[3];
                    return askForExpenseLimit(callbackQuery, accountId);
                }
                if (words[2].equals("DELETE")) {
                    String accountId = words[3];
                    return askForExpenseLimitToDelete(callbackQuery, accountId);
                }
            }
            if (words[1].equals("EDIT")) {
                switch (words[2]) {
                    case "AMOUNT" -> {
                        String expenseId = words[3];
                        return askForExpenseAmountToEdit(callbackQuery, expenseId);
                    }
                    case "CATEGORY" -> {
                        String expenseId = words[3];
                        return askForExpenseCategoryToEdit(callbackQuery, expenseId);
                    }
                    case "NOTE" -> {
                        String expenseId = words[3];
                        return askForExpenseNoteToEdit(callbackQuery, expenseId);
                    }
                }
            }
            if (words[1].equals("DELETE")) {
                switch (words[2]) {
                    case "YES" -> {
                        String expenseId = words[3];
                        return expenseService.deleteExpense(callbackQuery, expenseId);
                    }
                    case "NO" -> {
                        String expenseId = words[3];
                        return expenseService.getExpense(callbackQuery, expenseId);
                    }
                    case "LIMIT" -> {
                        String accountId = words[3];
                        return expenseService.deleteLimit(callbackQuery, accountId);
                    }
                }
            }
        }
        if (words.length == 3) {
            if (words[1].equals("ADD")) {
                if (words[2].equals("AMOUNT")) {
                    return askForExpenseAmountToAdd(callbackQuery);
                }
            }
            if (words[1].equals("REPORT")) {
                String expenseId = words[2];
                return expenseService.getExpenseReport(callbackQuery, expenseId);
            }
            if (words[1].equals("GET")) {
                String expenseId = words[2];
                return expenseService.getExpense(callbackQuery, expenseId);
            }
            if (words[1].equals("EDIT")) {
                String expenseId = words[2];
                return expenseService.editExpense(callbackQuery, expenseId);
            }
            if (words[1].equals("DELETE")) {
                String expenseId = words[2];
                return askForExpenseToDelete(callbackQuery, expenseId);
            }
            if (words[1].equals("LIMIT")) {
                if (words[2].equals("EXCEEDED")) {
                    return expenseService.getAllLimitExceededExpenses(callbackQuery);
                }
            }
        }
        if (words.length == 2) {
            if (words[1].equals("BACK")) {
                return expenseService.expenseMenu(callbackQuery);
            }
            if (words[1].equals("LIMIT")) {
                return expenseService.getLimitMenu(callbackQuery);
            }
        }
        if (words.length == 1) {
            return expenseService.expenseMenu(callbackQuery);
        }
        return null;
    }
}
