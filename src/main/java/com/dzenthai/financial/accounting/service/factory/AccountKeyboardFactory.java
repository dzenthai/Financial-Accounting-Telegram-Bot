package com.dzenthai.financial.accounting.service.factory;

import com.dzenthai.financial.accounting.data.AccountData;
import com.dzenthai.financial.accounting.data.ExpenseData;
import com.dzenthai.financial.accounting.data.IncomeData;
import com.dzenthai.financial.accounting.entity.Account;
import com.dzenthai.financial.accounting.entity.User;
import com.dzenthai.financial.accounting.service.AccountService;
import com.dzenthai.financial.accounting.service.UserService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.interfaces.BotApiObject;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.util.ArrayList;
import java.util.List;


@Component
public class AccountKeyboardFactory {

    private final KeyboardFactory keyboardFactory;

    private final AccountService accountService;

    private final UserService userService;

    public AccountKeyboardFactory(
            KeyboardFactory keyboardFactory,
            @Lazy AccountService accountService,
            UserService userService
    ) {
        this.keyboardFactory = keyboardFactory;
        this.accountService = accountService;
        this.userService = userService;
    }

    public InlineKeyboardMarkup accountMenuKeyboard(BotApiObject botApiObject) {

        List<String> text = new ArrayList<>();
        List<Integer> config = new ArrayList<>();
        List<String> data = new ArrayList<>();

        User user = userService.findUserByBotApiObject(botApiObject);

        List<Account> accounts = accountService.getAllAccountsByUser(user);

        for (Account account : accounts) {
            text.add(account.getName());
            data.add(AccountData.ACCOUNT_GET_.name() + account.getId());
        }

        configureButtonRows(config, accounts);

        text.add("🌟 Создать счет");
        data.add(AccountData.ACCOUNT_ADD.name());
        config.add(1);

        return keyboardFactory.createInlineKeyboard(text, config, data);
    }

    public InlineKeyboardMarkup accountOperationKeyboard(String accountId) {

        List<String> text = new ArrayList<>();
        List<Integer> config = new ArrayList<>();
        List<String> data = new ArrayList<>();

        text.add("✨ Доходы");
        data.add(IncomeData.INCOME.name());

        text.add("💸 Расходы");
        data.add(ExpenseData.EXPENSE.name());

        config.add(2);

        text.add("🗑️ Удалить счет");
        data.add(AccountData.ACCOUNT_DELETE_.name() + accountId);

        text.add("🕒 Дата и время");
        data.add(AccountData.ACCOUNT_DATETIME_.name() + accountId);

        config.add(2);

        text.add("↩️ Назад");
        data.add(AccountData.ACCOUNT.name());
        config.add(1);

        return keyboardFactory.createInlineKeyboard(text, config, data);
    }

    public InlineKeyboardMarkup deleteAccountKeyboard(String accountId) {
        return keyboardFactory.createInlineKeyboard(
                List.of("✅ Да", "❌ Нет"),
                List.of(2),
                List.of(AccountData.ACCOUNT_DELETE_YES_.name() + accountId,
                        AccountData.ACCOUNT_DELETE_NO_.name() + accountId)
        );
    }

    public InlineKeyboardMarkup editAccountDatetimeKeyboard(String accountId) {
        return keyboardFactory.createInlineKeyboard(
                List.of("📍 День", "📍 Неделя", "📍 Месяц", "📍 Полгода", "📍 Год", "↩️ Назад"),
                List.of(2, 3, 1),
                List.of(
                        AccountData.ACCOUNT_DATETIME_DAY_.name() + accountId,
                        AccountData.ACCOUNT_DATETIME_WEEK_.name() + accountId,
                        AccountData.ACCOUNT_DATETIME_MONTH_.name() + accountId,
                        AccountData.ACCOUNT_DATETIME_SIXMONTH_.name() + accountId,
                        AccountData.ACCOUNT_DATETIME_YEAR_.name() + accountId,
                        AccountData.ACCOUNT_GET_.name() + accountId)
        );
    }


    public InlineKeyboardMarkup backToAccountMenuAfterSaveAccountKeyboard(Long id) {
        Account account = accountService.getAccountById(id);

        List<String> text = List.of("👤 Перейти к счету", "🏛️ Меню счетов");
        List<String> data = List.of(AccountData.ACCOUNT_GET_.name() + account.getId(), AccountData.ACCOUNT.name());
        List<Integer> config = List.of(2);

        return keyboardFactory.createInlineKeyboard(text, config, data);
    }

    public InlineKeyboardMarkup backToAccount(String accountId) {
        return keyboardFactory.createInlineKeyboard(
                List.of("👤 Перейти к счету"),
                List.of(1),
                List.of(AccountData.ACCOUNT_GET_.name() + accountId)
        );
    }

    public InlineKeyboardMarkup backToAccountMenu() {
        return keyboardFactory.createInlineKeyboard(
                List.of("🏛️ Меню счетов"),
                List.of(1),
                List.of(AccountData.ACCOUNT.name())
        );
    }

    private void configureButtonRows(List<Integer> config, List<?> objects) {
        for (int i = 0; i < objects.size(); i += 2) {
            config.add((i + 1 < objects.size()) ? 2 : 1);
        }
    }
}

