package com.dzenthai.financial.accounting.service.manager;

import com.dzenthai.financial.accounting.bot.TelegramBot;
import com.dzenthai.financial.accounting.listener.CommandListener;
import com.dzenthai.financial.accounting.service.builder.MessageBuilder;
import com.dzenthai.financial.accounting.service.factory.MainMenuKeyboardFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Message;

@Component
public class MainMenuManager implements CommandListener {

    private final MessageBuilder messageBuilder;

    private final MainMenuKeyboardFactory mainMenuKeyboardFactory;

    public MainMenuManager(
            MessageBuilder messageBuilder,
            MainMenuKeyboardFactory mainMenuKeyboardFactory
    ) {
        this.messageBuilder = messageBuilder;
        this.mainMenuKeyboardFactory = mainMenuKeyboardFactory;
    }

    @Override
    public BotApiMethod<?> onCommand(Message message, TelegramBot telegramBot) {
        return mainMenu(message);
    }

    public BotApiMethod<?> mainMenu(Message message) {
        return messageBuilder.buildMessage("""
                        Привет %s"
                        
                        Добро пожаловать в систему учета финансов.
                        Здесь ты можешь управлять своими доходами, расходами и следить за состоянием своего счета.
                        
                        Нажмите на "Мой профиль", чтобы начать работу с вашим счетом.
                        """.formatted(message.getChat().getFirstName()),
                message,
                mainMenuKeyboardFactory.mainMenuKeyboard());
    }
}
