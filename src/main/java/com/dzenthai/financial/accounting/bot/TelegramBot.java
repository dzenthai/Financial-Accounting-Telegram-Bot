package com.dzenthai.financial.accounting.bot;

import com.dzenthai.financial.accounting.bot.dispatcher.TelegramUpdateDispatcher;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;


@Slf4j
@Component
public final class TelegramBot extends TelegramLongPollingBot {

    private final TelegramUpdateDispatcher telegramUpdateDispatcher;

    public TelegramBot(@Value("${bot.token}") String botToken,
                       TelegramUpdateDispatcher telegramUpdateDispatcher
    ) {
        super(botToken);
        this.telegramUpdateDispatcher = telegramUpdateDispatcher;
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            var method = telegramUpdateDispatcher.distribute(update, this);
            if (method != null) {
                sendApiMethod(method);
            }

        } catch (ConstraintViolationException e) {
            handleValidationException(update, e);
        } catch (Exception e) {
            log.error("TelegramBot | Error while dispatching update ", e);
            sendExceptionMessage(update);
        }
    }

    private void handleValidationException(Update update, ConstraintViolationException e) {
        StringBuilder errorMessage = new StringBuilder("Ошибка: ");
        for (ConstraintViolation<?> violation : e.getConstraintViolations()) {
            errorMessage.append(violation.getMessage()).append("\n");
        }

        SendMessage message = SendMessage.builder()
                .chatId(update.getMessage().getChatId())
                .text(errorMessage.toString())
                .build();

        try {
            sendApiMethod(message);
        } catch (TelegramApiException ex) {
            log.error("TelegramBot | Telegram api error ", ex);
            throw new RuntimeException(ex);
        }
    }

    private void sendExceptionMessage(Update update) {
        SendMessage message = SendMessage.builder()
                .chatId(update.getMessage().getChatId())
                .text("Произошла ошибка, попробуйте позже!")
                .build();
        try {
            sendApiMethod(message);
        } catch (TelegramApiException e) {
            log.error("TelegramBot | Telegram api error ", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getBotUsername() {
        return "/";
    }
}
