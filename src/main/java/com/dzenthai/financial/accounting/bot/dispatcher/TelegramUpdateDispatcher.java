package com.dzenthai.financial.accounting.bot.dispatcher;

import com.dzenthai.financial.accounting.bot.TelegramBot;
import com.dzenthai.financial.accounting.bot.message.CallbackQueryHandler;
import com.dzenthai.financial.accounting.bot.message.CommandHandler;
import com.dzenthai.financial.accounting.bot.message.MessageHandler;
import com.dzenthai.financial.accounting.entity.User;
import com.dzenthai.financial.accounting.entity.enums.Action;
import com.dzenthai.financial.accounting.repository.UserRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.time.LocalDate;
import java.util.Objects;


@Slf4j
@Service
public class TelegramUpdateDispatcher {

    private final MessageHandler messageHandler;

    private final CommandHandler commandHandler;

    private final CallbackQueryHandler callbackQueryHandler;

    private final UserRepo userRepo;

    public TelegramUpdateDispatcher(
            MessageHandler messageHandler,
            CommandHandler commandHandler,
            CallbackQueryHandler callbackQueryHandler,
            UserRepo userRepo) {
        this.messageHandler = messageHandler;
        this.commandHandler = commandHandler;
        this.callbackQueryHandler = callbackQueryHandler;
        this.userRepo = userRepo;
    }

    public BotApiMethod<?> distribute(Update update, TelegramBot telegramBot) {
        log.debug("TelegramUpdateDispatcher | Update: {}", update);
        if (update.hasCallbackQuery()) {
            checkUser(update.getCallbackQuery().getMessage().getChatId());
            User user = userRepo.findUserByChatId(update.getCallbackQuery().getMessage().getChatId()).orElse(null);
            Objects.requireNonNull(user).setAction(Action.FREE);
            userRepo.save(user);
            log.debug("TelegramUpdateDispatcher | CallbackQuery: {}", update.getCallbackQuery());
            CallbackQuery callbackQuery = update.getCallbackQuery();
            return callbackQueryHandler.answer(callbackQuery, telegramBot);
        }
        if (update.hasMessage()) {
            checkUser(update.getMessage().getChatId());
            log.debug("TelegramUpdateDispatcher | Message: {}", update.getMessage());
            Message message = update.getMessage();
            if (message.hasText()) {
                checkUser(message.getChatId());
                log.debug("TelegramUpdateDispatcher | Text: {}", message.getText());
                if (message.getText().charAt(0) == '/') {
                    checkUser(message.getChatId());
                    log.debug("TelegramUpdateDispatcher | Command: {}", message.getText());
                    return commandHandler.answer(message, telegramBot);
                }
            }
            return messageHandler.answer(message, telegramBot);
        }
        log.warn("TelegramUpdateDispatcher | Unsupported update type: {}", update);
        return null;
    }

    private void checkUser(Long chatId) {
        if(!userRepo.existsByChatId(chatId)) {
            User user = User.builder()
                    .chatId(chatId)
                    .action(Action.FREE)
                    .compareDate(LocalDate.now().minusMonths(1))
                    .build();
            userRepo.save(user);
        }
    }
}
