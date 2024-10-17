package com.dzenthai.financial.accounting.service.factory;

import com.dzenthai.financial.accounting.data.AccountData;
import com.dzenthai.financial.accounting.data.IncomeData;
import com.dzenthai.financial.accounting.entity.Account;
import com.dzenthai.financial.accounting.entity.Income;
import com.dzenthai.financial.accounting.entity.User;
import com.dzenthai.financial.accounting.entity.enums.IncomeCategory;
import com.dzenthai.financial.accounting.service.AccountService;
import com.dzenthai.financial.accounting.service.IncomeService;
import com.dzenthai.financial.accounting.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.interfaces.BotApiObject;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


@Slf4j
@Component
public class IncomeKeyboardFactory {

    private final KeyboardFactory keyboardFactory;
    private final UserService userService;
    private final AccountService accountService;
    private final IncomeService incomeService;

    public IncomeKeyboardFactory(
            KeyboardFactory keyboardFactory,
            UserService userService,
            AccountService accountService,
            IncomeService incomeService
    ) {
        this.keyboardFactory = keyboardFactory;
        this.userService = userService;
        this.accountService = accountService;
        this.incomeService = incomeService;
    }

    public InlineKeyboardMarkup incomeMenuKeyboard(BotApiObject botApiObject) {
        List<String> text = new ArrayList<>();
        List<Integer> config = new ArrayList<>();
        List<String> data = new ArrayList<>();

        User user = userService.findUserByBotApiObject(botApiObject);
        Account account = accountService.getAccountById(Objects.requireNonNull(user).getCurrentAccountId());
        List<Income> incomes = incomeService.getAllIncomesByAccountAndDateAfter(account, account.getCompareDate());

        for (Income income : incomes) {
            text.add(income.getCategory().getDisplayName() + income.getAmount());
            config.add(1);
            data.add(IncomeData.INCOME_GET_.name() + income.getId());
        }

        if (!incomes.isEmpty()) {
            text.add("Посмотреть отчет");
            data.add(IncomeData.INCOME_REPORT_.name() + account.getId());

            config.add(1);
        }

        text.add("✨ Добавить");
        data.add(IncomeData.INCOME_ADD_AMOUNT.name());

        text.add("↩️ Назад");
        data.add(AccountData.ACCOUNT_GET_.name() + account.getId());

        config.add(2);

        return keyboardFactory.createInlineKeyboard(text, config, data);
    }

    public InlineKeyboardMarkup getAllIncomeCategoryKeyboard() {
        List<String> text = IncomeCategory.getDisplayNames();
        List<Integer> config = new ArrayList<>();
        List<String> data = new ArrayList<>();

        List<String> incomeCategories = IncomeCategory.getNames();

        for (String incomeCategory : incomeCategories) {
            config.add(1);
            data.add(IncomeData.INCOME_ADD_CATEGORY_.name() + incomeCategory);
        }

        return keyboardFactory.createInlineKeyboard(text, config, data);
    }

    public InlineKeyboardMarkup getAllIncomeCategoryKeyboard(String incomeId) {
        List<String> text = IncomeCategory.getDisplayNames();
        List<Integer> config = new ArrayList<>();
        List<String> data = new ArrayList<>();

        List<String> incomeCategories = IncomeCategory.getNames();

        for (String incomeCategory : incomeCategories) {
            config.add(1);
            data.add("INCOME_EDIT_CATEGORY_" + incomeCategory + "_" + incomeId);
        }

        return keyboardFactory.createInlineKeyboard(text, config, data);
    }

    public InlineKeyboardMarkup editAndDeleteIncomeKeyboard(String incomeId) {
        return keyboardFactory.createInlineKeyboard(
                List.of("🛠️ Изменить", "🗑️ Удалить", "↩️ Назад"),
                List.of(2, 1),
                List.of(
                        IncomeData.INCOME_EDIT_ + incomeId,
                        IncomeData.INCOME_DELETE_ + incomeId,
                        IncomeData.INCOME_BACK.name()
                )
        );
    }

    public InlineKeyboardMarkup editIncomeKeyboard(String incomeId) {
        List<String> text = new ArrayList<>();
        List<Integer> config = new ArrayList<>();
        List<String> data = new ArrayList<>();

        text.add("💰 Сумма");
        config.add(1);
        data.add(IncomeData.INCOME_EDIT_AMOUNT_.name() + incomeId);

        text.add("🚀 Категория доходов");
        config.add(1);
        data.add(IncomeData.INCOME_EDIT_CATEGORY_.name() + incomeId);

        text.add("📌 Описание");
        config.add(1);
        data.add(IncomeData.INCOME_EDIT_NOTE_.name() + incomeId);

        text.add("↩️ Назад");
        config.add(1);
        data.add(IncomeData.INCOME_GET_.name() + incomeId);

        return keyboardFactory.createInlineKeyboard(text, config, data);
    }

    public InlineKeyboardMarkup confirmDeleteIncomeKeyboard(String incomeId) {
        return keyboardFactory.createInlineKeyboard(
                List.of("✅ Да", "❌ Нет"),
                List.of(2),
                List.of(
                        IncomeData.INCOME_DELETE_YES_.name() + incomeId,
                        IncomeData.INCOME_DELETE_NO_.name() + incomeId
                )
        );
    }

    public InlineKeyboardMarkup backToMainMenu(String text) {
        List<Integer> config = new ArrayList<>();
        List<String> data = new ArrayList<>();

        config.add(1);
        data.add(IncomeData.INCOME_BACK.name());

        return keyboardFactory.createInlineKeyboard(List.of(text), config, data);
    }
}
