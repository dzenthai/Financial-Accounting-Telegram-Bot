package com.dzenthai.financial.accounting.service.manager;

import com.dzenthai.financial.accounting.bot.TelegramBot;
import com.dzenthai.financial.accounting.entity.User;
import com.dzenthai.financial.accounting.entity.enums.Action;
import com.dzenthai.financial.accounting.listener.CallbackQueryListener;
import com.dzenthai.financial.accounting.service.builder.MessageBuilder;
import com.dzenthai.financial.accounting.service.AccountService;
import com.dzenthai.financial.accounting.service.UserService;
import com.dzenthai.financial.accounting.service.factory.AccountKeyboardFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;


@Component
public class AccountManager implements CallbackQueryListener {

    private final AccountService accountService;

    private final UserService userService;

    private final MessageBuilder messageBuilder;

    private final AccountKeyboardFactory accountKeyboardFactory;

    public AccountManager(
            AccountService accountService,
            UserService userService,
            MessageBuilder messageBuilder,
            AccountKeyboardFactory accountKeyboardFactory
    ) {
        this.accountService = accountService;
        this.userService = userService;
        this.messageBuilder = messageBuilder;
        this.accountKeyboardFactory = accountKeyboardFactory;
    }

    public BotApiMethod<?> askForNameToAdd(CallbackQuery callbackQuery) {
        Long chatId = callbackQuery.getMessage().getChatId();
        userService.updateUserAction(chatId, Action.ACCOUNT_ADD);
        return messageBuilder.buildMessage(
                "Введите название счета",
                callbackQuery,
                null);
    }

    public BotApiMethod<?> askForAccountToDelete(CallbackQuery callbackQuery, String accountId) {
        return messageBuilder.buildMessage(
                "Вы уверены что хотите удалить счет?",
                callbackQuery,
                accountKeyboardFactory.deleteAccountKeyboard(accountId));
    }

    @Override
    public BotApiMethod<?> onCallbackQuery(CallbackQuery callbackQuery, String[] words, TelegramBot telegramBot) {
        if (words.length == 4) {
            if (words[1].equals("DELETE")) {
                if (words[2].equals("YES")) {
                    String accountId = words[3];
                    return accountService.deleteAccount(callbackQuery, accountId);
                }
                if (words[2].equals("NO")) {
                    String accountId = words[3];
                    return accountService.getAccount(callbackQuery, accountId);
                }
            }
        }
        if (words.length == 3) {
            if (words[1].equals("GET")) {
                String accountId = words[2];
                User user = userService.findUserByBotApiObject(callbackQuery);
                userService.updateCurrentAccountId(user.getChatId(), Long.valueOf(accountId), Action.FREE);
                return accountService.getAccount(callbackQuery, accountId);
            }
            if (words[1].equals("DELETE")) {
                String accountId = words[2];
                return askForAccountToDelete(callbackQuery, accountId);
            }
        }
        if (words.length == 2) {
            if (words[1].equals("ADD")) {
                return askForNameToAdd(callbackQuery);
            }
        }
        return accountService.accountMenu(callbackQuery);
    }
}
