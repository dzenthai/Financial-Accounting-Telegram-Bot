package com.dzenthai.financial.accounting.service.factory;

import com.dzenthai.financial.accounting.data.AccountData;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.util.List;


@Component
public class MainMenuKeyboardFactory {

    private final KeyboardFactory keyboardFactory;

    public MainMenuKeyboardFactory(KeyboardFactory keyboardFactory) {
        this.keyboardFactory = keyboardFactory;
    }

    public InlineKeyboardMarkup mainMenuKeyboard() {
        return keyboardFactory.createInlineKeyboard(
                List.of("💼 Мой профиль"),
                List.of(1),
                List.of(AccountData.ACCOUNT.name())
        );
    }
}