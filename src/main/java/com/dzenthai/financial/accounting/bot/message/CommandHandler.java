package com.dzenthai.financial.accounting.bot.message;

import com.dzenthai.financial.accounting.bot.TelegramBot;
import com.dzenthai.financial.accounting.service.manager.MainMenuManager;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.interfaces.BotApiObject;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Message;


@Component
public class CommandHandler extends AbstractHandler {

    private final MainMenuManager mainMenuManager;

    public CommandHandler(MainMenuManager mainMenuManager) {
        this.mainMenuManager = mainMenuManager;
    }

    @Override
    public BotApiMethod<?> answer(BotApiObject botApiObject, TelegramBot telegramBot) {
        Message command = (Message) botApiObject;
        if (command.getText().equals("/start")) {
            return mainMenuManager.onCommand(command, telegramBot);
        }
        return null;
    }
}
