package com.dzenthai.financial.accounting.service.factory;

import com.dzenthai.financial.accounting.data.AccountData;
import com.dzenthai.financial.accounting.data.ExpenseData;
import com.dzenthai.financial.accounting.entity.Account;
import com.dzenthai.financial.accounting.entity.Expense;
import com.dzenthai.financial.accounting.entity.Limit;
import com.dzenthai.financial.accounting.entity.User;
import com.dzenthai.financial.accounting.entity.enums.ExpenseCategory;
import com.dzenthai.financial.accounting.service.AccountService;
import com.dzenthai.financial.accounting.service.ExpenseService;
import com.dzenthai.financial.accounting.service.LimitService;
import com.dzenthai.financial.accounting.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.interfaces.BotApiObject;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


@Slf4j
@Component
public class ExpenseKeyboardFactory {

    private final KeyboardFactory keyboardFactory;

    private final UserService userService;

    private final AccountService accountService;

    private final ExpenseService expenseService;

    private final LimitService limitService;

    public ExpenseKeyboardFactory(
            KeyboardFactory keyboardFactory,
            UserService userService,
            AccountService accountService,
            ExpenseService expenseService,
            LimitService limitService
    ) {
        this.keyboardFactory = keyboardFactory;
        this.userService = userService;
        this.accountService = accountService;
        this.expenseService = expenseService;
        this.limitService = limitService;
    }

    public InlineKeyboardMarkup expenseMenuKeyboard(BotApiObject botApiObject) {

        List<String> text = new ArrayList<>();
        List<Integer> config = new ArrayList<>();
        List<String> data = new ArrayList<>();

        User user = userService.findUserByBotApiObject(botApiObject);

        Account account = accountService.getAccountById(Objects.requireNonNull(user).getCurrentAccountId());

        List<Expense> expenses = expenseService.getAllExpensesByAccountAndDateAfter(account, account.getCompareDate());

        Limit limit = limitService.getLimitByAccount(account);

        for (Expense expense : expenses) {
            text.add(expense.getCategory().getDisplayName() + expense.getAmount());
            config.add(1);
            data.add(ExpenseData.EXPENSE_GET_.name() + expense.getId());
        }

        if (!expenses.isEmpty()) {

            text.add("Посмотреть отчет");
            data.add(ExpenseData.EXPENSE_REPORT_.name() + account.getId());

            text.add("Меню лимитов");
            data.add(ExpenseData.EXPENSE_LIMIT.name());

            config.add(2);
        }

        text.add("💸 Добавить");
        data.add(ExpenseData.EXPENSE_ADD_AMOUNT.name());

        text.add("↩️ Назад");
        data.add(AccountData.ACCOUNT_GET_.name() + account.getId());

        config.add(2);

        if (expenses.isEmpty() && limit != null) {
            text.add("🗑️ Удалить лимит");
            config.add(1);
            data.add(ExpenseData.EXPENSE_LIMIT_DELETE_.name() + account.getId());
        }

        return keyboardFactory.createInlineKeyboard(text, config, data);

    }

    public InlineKeyboardMarkup getAllExpenseCategoryKeyboard() {
        List<String> text = ExpenseCategory.getDisplayNames();
        List<Integer> config = new ArrayList<>();
        List<String> data = new ArrayList<>();

        List<String> expenseCategories = ExpenseCategory.getNames();

        for (String expenseCategory : expenseCategories) {
            config.add(1);
            data.add(ExpenseData.EXPENSE_ADD_CATEGORY_.name() + expenseCategory);
        }

        return keyboardFactory.createInlineKeyboard(text, config, data);
    }

    public InlineKeyboardMarkup getAllExpenseCategoryKeyboard(String expenseId) {
        List<String> text = ExpenseCategory.getDisplayNames();
        List<Integer> config = new ArrayList<>();
        List<String> data = new ArrayList<>();

        List<String> expenseCategories = ExpenseCategory.getNames();

        for (String expenseCategory : expenseCategories) {
            config.add(1);
            data.add("EXPENSE_EDIT_CATEGORY_" + expenseCategory + "_" + expenseId);
        }

        return keyboardFactory.createInlineKeyboard(text, config, data);
    }

    public InlineKeyboardMarkup editAndDeleteExpenseKeyboard(String expenseId) {
        return keyboardFactory.createInlineKeyboard(
                List.of("🛠️ Изменить", "🗑️ Удалить", "↩️ Назад"),
                List.of(2, 1),
                List.of(
                        ExpenseData.EXPENSE_EDIT_ + expenseId,
                        ExpenseData.EXPENSE_DELETE_ + expenseId,
                        ExpenseData.EXPENSE_BACK.name()

                )
        );
    }

    public InlineKeyboardMarkup editExpenseKeyboard(String expenseId) {

        List<String> text = new ArrayList<>();
        List<Integer> config = new ArrayList<>();
        List<String> data = new ArrayList<>();

        text.add("💰 Сумма");
        config.add(1);
        data.add(ExpenseData.EXPENSE_EDIT_AMOUNT_.name() + expenseId);

        text.add("🛒 Категория трат");
        config.add(1);
        data.add(ExpenseData.EXPENSE_EDIT_CATEGORY_.name() + expenseId);

        text.add("📌 Описание");
        config.add(1);
        data.add(ExpenseData.EXPENSE_EDIT_NOTE_.name() + expenseId);

        text.add("↩️ Назад");
        config.add(1);
        data.add(ExpenseData.EXPENSE_GET_.name() + expenseId);

        return keyboardFactory.createInlineKeyboard(text, config, data);
    }

    public InlineKeyboardMarkup deleteExpenseKeyboard(String expenseId) {
        return keyboardFactory.createInlineKeyboard(
                List.of("✅ Да", "❌ Нет"),
                List.of(2),
                List.of(ExpenseData.EXPENSE_DELETE_YES_.name() + expenseId,
                        ExpenseData.EXPENSE_DELETE_NO_.name() + expenseId)
        );
    }

    public InlineKeyboardMarkup getLimitMenuKeyboard(BotApiObject botApiObject) {

        List<String> text = new ArrayList<>();
        List<Integer> config = new ArrayList<>();
        List<String> data = new ArrayList<>();

        User user = userService.findUserByBotApiObject(botApiObject);
        Account account = accountService.getAccountById(Objects.requireNonNull(user).getCurrentAccountId());
        Limit limit = limitService.getLimitByAccount(account);

        List<Expense> expenses = expenseService.getAllExpensesByAccount(account).stream().filter(Expense::isLimitExceeded).toList();

        if (!expenses.isEmpty()) {
            text.add("🛫 Превышения лимита");
            data.add(ExpenseData.EXPENSE_LIMIT_EXCEEDED.name());
            config.add(1);
        }

        if (limit != null) {
            text.add("🗑️ Удалить лимит");
            data.add(ExpenseData.EXPENSE_LIMIT_DELETE_.name() + account.getId());
            config.add(1);

        } else {
            text.add("⚡ Установить лимит");
            data.add(ExpenseData.EXPENSE_LIMIT_ADD_.name() + account.getId());
            config.add(1);
        }

        text.add("↩️ Назад");
        config.add(1);
        data.add(ExpenseData.EXPENSE.name());

        return keyboardFactory.createInlineKeyboard(text, config, data);

    }

    public InlineKeyboardMarkup getAllLimitExceededExpensesKeyboard(List<Expense> expenses) {

        List<String> text = new ArrayList<>();
        List<Integer> config = new ArrayList<>();
        List<String> data = new ArrayList<>();

        for (Expense expense : expenses) {
            text.add(expense.getCategory().getDisplayName() + expense.getAmount());
            config.add(1);
            data.add(ExpenseData.EXPENSE_GET_.name() + expense.getId());
        }

        text.add("↩️ Назад");
        config.add(1);
        data.add(ExpenseData.EXPENSE_LIMIT.name());

        return keyboardFactory.createInlineKeyboard(text, config, data);

    }

    public InlineKeyboardMarkup backToExpenseMenu(String text) {
        List<Integer> config = new ArrayList<>();
        List<String> data = new ArrayList<>();

        config.add(1);
        data.add(ExpenseData.EXPENSE_BACK.name());

        return keyboardFactory.createInlineKeyboard(List.of(text), config, data);
    }

    public InlineKeyboardMarkup backToLimitMenu(String text) {
        List<Integer> config = new ArrayList<>();
        List<String> data = new ArrayList<>();

        config.add(1);
        data.add(ExpenseData.EXPENSE_LIMIT.name());

        return keyboardFactory.createInlineKeyboard(List.of(text), config, data);
    }

    public InlineKeyboardMarkup deleteLimitKeyboard(String accountId) {
        return keyboardFactory.createInlineKeyboard(
                List.of("✅ Да", "❌ Нет"),
                List.of(2),
                List.of(ExpenseData.EXPENSE_LIMIT_DELETE_YES_.name() + accountId,
                        ExpenseData.EXPENSE_LIMIT_DELETE_NO_.name() + accountId)
        );
    }
}
