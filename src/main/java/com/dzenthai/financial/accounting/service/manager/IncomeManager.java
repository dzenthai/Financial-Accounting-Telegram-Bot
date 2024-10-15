package com.dzenthai.financial.accounting.service.manager;

import com.dzenthai.financial.accounting.bot.TelegramBot;
import com.dzenthai.financial.accounting.entity.User;
import com.dzenthai.financial.accounting.entity.enums.Action;
import com.dzenthai.financial.accounting.entity.enums.IncomeCategory;
import com.dzenthai.financial.accounting.listener.CallbackQueryListener;
import com.dzenthai.financial.accounting.service.IncomeService;
import com.dzenthai.financial.accounting.service.UserService;
import com.dzenthai.financial.accounting.service.builder.MessageBuilder;
import com.dzenthai.financial.accounting.service.factory.IncomeKeyboardFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;


@Component
public class IncomeManager implements CallbackQueryListener {

    private final IncomeService incomeService;

    private final MessageBuilder messageBuilder;

    private final IncomeKeyboardFactory incomeKeyboardFactory;

    private final UserService userService;

    public IncomeManager(
            IncomeService incomeService,
            MessageBuilder messageBuilder,
            IncomeKeyboardFactory incomeKeyboardFactory,
            UserService userService
    ) {
        this.incomeService = incomeService;
        this.messageBuilder = messageBuilder;
        this.incomeKeyboardFactory = incomeKeyboardFactory;
        this.userService = userService;
    }

    private BotApiMethod<?> askForIncomeAmountToAdd(CallbackQuery callbackQuery) {
        User user = userService.findUserByBotApiObject(callbackQuery);
        userService.updateUserAction(user.getChatId(), Action.INCOME_ADD_AMOUNT);
        return messageBuilder.buildMessage(
                "Введите сумму",
                callbackQuery,
                incomeKeyboardFactory.backToMainMenu("✋ Отмена"));
    }

    public BotApiMethod<?> askForIncomeCategoryToAdd(Message message) {
        User user = userService.findUserByBotApiObject(message);
        userService.updateUserAction(user.getId(), Action.INCOME_ADD_CATEGORY);
        return messageBuilder.buildMessage(
                "Выберите категорию доходов",
                message,
                incomeKeyboardFactory.getAllIncomeCategoryKeyboard());
    }

    public BotApiMethod<?> askForIncomeNoteToAdd(CallbackQuery callbackQuery) {
        User user = userService.findUserByBotApiObject(callbackQuery);
        userService.updateUserAction(user.getChatId(), Action.INCOME_ADD_NOTE);
        return messageBuilder.buildMessage(
                "Введите описание (примечание)",
                callbackQuery,
                incomeKeyboardFactory.backToMainMenu("⏩ Пропустить"));
    }

    private BotApiMethod<?> askForIncomeAmountToEdit(CallbackQuery callbackQuery, String incomeId) {
        Long chatId = callbackQuery.getMessage().getChatId();
        userService.updateCurrentIncomeId(chatId, Long.valueOf(incomeId), Action.INCOME_EDIT_AMOUNT);
        return messageBuilder.buildMessage(
                "Введите новую сумму",
                callbackQuery,
                incomeKeyboardFactory.backToMainMenu("✋ Отмена"));
    }

    private BotApiMethod<?> askForIncomeCategoryToEdit(CallbackQuery callbackQuery, String incomeId) {
        Long chatId = callbackQuery.getMessage().getChatId();
        userService.updateCurrentIncomeId(chatId, Long.valueOf(incomeId), Action.INCOME_EDIT_CATEGORY);
        return messageBuilder.buildMessage(
                "Выберите новую категорию доходов",
                callbackQuery,
                incomeKeyboardFactory.getAllIncomeCategoryKeyboard(incomeId));
    }

    private BotApiMethod<?> askForIncomeNoteToEdit(CallbackQuery callbackQuery, String incomeId) {
        Long chatId = callbackQuery.getMessage().getChatId();
        userService.updateCurrentIncomeId(chatId, Long.valueOf(incomeId), Action.INCOME_EDIT_NOTE);
        return messageBuilder.buildMessage(
                "Введите новое описание (примечание)",
                callbackQuery,
                incomeKeyboardFactory.backToMainMenu("✋ Отмена"));
    }

    private BotApiMethod<?> askForIncomeToDelete(CallbackQuery callbackQuery, String incomeId) {
        return messageBuilder.buildMessage(
                "Вы уверены, что хотите удалить этот доход?",
                callbackQuery,
                incomeKeyboardFactory.confirmDeleteIncomeKeyboard(incomeId));
    }


    @Override
    public BotApiMethod<?> onCallbackQuery(CallbackQuery callbackQuery, String[] words, TelegramBot telegramBot) {
        if (words.length == 5) {
            if (words[1].equals("EDIT")) {
                if (words[2].equals("CATEGORY")) {
                    IncomeCategory category = IncomeCategory.valueOf(words[3]);
                    String incomeId = words[4];
                    return incomeService.editIncomeCategory(callbackQuery, category, incomeId);
                }
            }
        }
        if (words.length == 4) {
            if (words[1].equals("ADD")) {
                if (words[2].equals("CATEGORY")) {
                    IncomeCategory incomeCategory = IncomeCategory.valueOf(words[3]);
                    return incomeService.saveIncomeCategory(callbackQuery, incomeCategory);
                }
            }
            if (words[1].equals("EDIT")) {
                switch (words[2]) {
                    case "AMOUNT" -> {
                        String incomeId = words[3];
                        return askForIncomeAmountToEdit(callbackQuery, incomeId);
                    }
                    case "CATEGORY" -> {
                        String incomeId = words[3];
                        return askForIncomeCategoryToEdit(callbackQuery, incomeId);
                    }
                    case "NOTE" -> {
                        String incomeId = words[3];
                        return askForIncomeNoteToEdit(callbackQuery, incomeId);
                    }
                }
            }
            if (words[1].equals("DELETE")) {
                if (words[2].equals("YES")) {
                    String incomeId = words[3];
                    return incomeService.deleteIncome(callbackQuery, incomeId);
                }
                if (words[2].equals("NO")) {
                    String incomeId = words[3];
                    return incomeService.getIncome(callbackQuery, incomeId);
                }
            }
        }
        if (words.length == 3) {
            if (words[1].equals("ADD")) {
                if (words[2].equals("AMOUNT")) {
                    return askForIncomeAmountToAdd(callbackQuery);
                }
            }
            if (words[1].equals("REPORT")) {
                String incomeId = words[2];
                return incomeService.getIncomeReport(callbackQuery, incomeId);
            }
            if (words[1].equals("GET")) {
                String incomeId = words[2];
                return incomeService.getIncome(callbackQuery, incomeId);
            }
            if (words[1].equals("EDIT")) {
                String incomeId = words[2];
                return incomeService.editIncome(callbackQuery, incomeId);
            }
            if (words[1].equals("DELETE")) {
                String incomeId = words[2];
                return askForIncomeToDelete(callbackQuery, incomeId);
            }
        }
        if (words.length == 2) {
            if (words[1].equals("BACK")) {
                return incomeService.incomeMenu(callbackQuery);
            }
        }
        if (words.length == 1) {
            return incomeService.incomeMenu(callbackQuery);
        }
        return null;
    }
}
