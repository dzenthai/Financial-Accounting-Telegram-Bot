package com.dzenthai.financial.accounting.service.builder;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.interfaces.BotApiObject;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;


@Component
public class MessageBuilder {

    public BotApiMethod<?> buildMessage(String text, BotApiObject object, InlineKeyboardMarkup keyboardMarkup) {
        if (object instanceof CallbackQuery callbackQuery) {
            EditMessageText editMessageText = EditMessageText.builder()
                    .chatId(callbackQuery.getMessage().getChatId())
                    .messageId(callbackQuery.getMessage().getMessageId())
                    .text(text)
                    .build();
            if (keyboardMarkup != null) {
                editMessageText.setReplyMarkup(keyboardMarkup);
            }
            return editMessageText;
        }
        if (object instanceof Message message) {
            SendMessage sendMessage = SendMessage.builder()
                    .chatId(message.getChatId())
                    .text(text)
                    .build();
            if (keyboardMarkup != null) {
                sendMessage.setReplyMarkup(keyboardMarkup);
            }
            return sendMessage;
        }
        return null;
    }
}
