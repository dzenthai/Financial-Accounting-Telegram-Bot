package com.dzenthai.financial.accounting.service;

import com.dzenthai.financial.accounting.entity.Account;
import com.dzenthai.financial.accounting.entity.Income;
import com.dzenthai.financial.accounting.entity.User;
import com.dzenthai.financial.accounting.entity.enums.Action;
import com.dzenthai.financial.accounting.entity.enums.IncomeCategory;
import com.dzenthai.financial.accounting.repository.IncomeRepo;
import com.dzenthai.financial.accounting.service.builder.MessageBuilder;
import com.dzenthai.financial.accounting.service.factory.IncomeKeyboardFactory;
import com.dzenthai.financial.accounting.service.manager.IncomeManager;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.interfaces.BotApiObject;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Slf4j
@Service
public class IncomeService {

    @PersistenceContext
    private EntityManager entityManager;
    private final IncomeRepo incomeRepo;
    private final IncomeKeyboardFactory incomeKeyboardFactory;
    private final MessageBuilder messageBuilder;
    private final AccountService accountService;
    private final UserService userService;
    private final IncomeManager incomeManager;

    public IncomeService(
            IncomeRepo incomeRepo,
            @Lazy IncomeKeyboardFactory incomeKeyboardFactory,
            MessageBuilder messageBuilder,
            @Lazy AccountService accountService,
            UserService userService,
            @Lazy IncomeManager incomeManager
    ) {
        this.incomeRepo = incomeRepo;
        this.incomeKeyboardFactory = incomeKeyboardFactory;
        this.messageBuilder = messageBuilder;
        this.accountService = accountService;
        this.userService = userService;
        this.incomeManager = incomeManager;
    }

    public BotApiMethod<?> incomeMenu(BotApiObject botApiObject) {
        Long chatId;
        if (botApiObject instanceof CallbackQuery callbackQuery) {
            chatId = callbackQuery.getMessage().getChatId();
            userService.updateCurrentIncomeId(chatId, null, Action.FREE);
            return messageBuilder.buildMessage(
                    """
                            Меню доходов. ✨
                            
                            Нажмите на один из ваших доходов, чтобы посмотреть подробности.
                            
                            Выберите интересующий вас пункт меню:
                            """,
                    callbackQuery,
                    incomeKeyboardFactory.incomeMenuKeyboard(callbackQuery));
        }
        if (botApiObject instanceof Message message) {
            chatId = message.getChatId();
            userService.updateCurrentIncomeId(chatId, null, Action.FREE);
            return messageBuilder.buildMessage("""
                            Ваша доход успешно сохранена!
                            
                            Нажмите на один из ваших доходов, чтобы посмотреть подробности.
                            
                            Выберите интересующий вас пункт меню:
                            """,
                    message,
                    incomeKeyboardFactory.incomeMenuKeyboard(message));
        }
        return null;
    }

    public BotApiMethod<?> saveAmountIncome(Message message) {
        Long chatId = message.getChatId();
        String messageText = message.getText();
        try {
            BigDecimal amount = new BigDecimal(messageText);
            User user = userService.findUserByBotApiObject(message);
            Account account = accountService.getAccountById(user.getCurrentAccountId());
            Income income = Income.builder()
                    .account(account)
                    .amount(amount)
                    .build();

            incomeRepo.save(income);
            userService.updateCurrentIncomeId(chatId, income.getId(), Action.INCOME_ADD_CATEGORY);
            return incomeManager.askForIncomeCategoryToAdd(message);

        } catch (NumberFormatException e) {
            return messageBuilder.buildMessage(
                    "Неверный формат числа. Пожалуйста, введите сумму в корректном формате.",
                    message,
                    null);
        }
    }


    @Transactional
    public BotApiMethod<?> saveIncomeCategory(CallbackQuery callbackQuery, IncomeCategory incomeCategory) {
        User user = userService.findUserByBotApiObject(callbackQuery);
        Income income = getIncomeById(user.getCurrentIncomeId());
        updateIncomeCategory(income, incomeCategory);
        updateIncomeNote(income, "-");
        updateIncomeDate(income, LocalDate.now());
        return incomeManager.askForIncomeNoteToAdd(callbackQuery);
    }

    @Transactional
    public BotApiMethod<?> saveNoteIncome(Message message) {
        User user = userService.findUserByBotApiObject(message);
        Income income = getIncomeById(user.getCurrentIncomeId());
        updateIncomeNote(income, message.getText());
        userService.updateCurrentIncomeId(user.getId(), null, Action.FREE);
        return incomeMenu(message);
    }

    public BotApiMethod<?> getIncome(CallbackQuery callbackQuery, String incomeId) {
        Income income = getIncomeById(Long.parseLong(incomeId));
        String category = income.getCategory().getDisplayName();
        String accountName = income.getAccount().getName();
        LocalDate date = income.getDate();
        String note = income.getNote();
        String amount = income.getAmount().toString();
        return messageBuilder.buildMessage("""
                        Доход.
                        
                        Категория: %s
                        Счет: %s
                        Примечание: %s
                        Дата: %s
                        
                        💰 Сумма: %s
                        """.formatted(category, accountName, note, date, amount),
                callbackQuery,
                incomeKeyboardFactory.editAndDeleteIncomeKeyboard(incomeId));
    }

    public BotApiMethod<?> editIncome(CallbackQuery callbackQuery, String incomeId) {
        return messageBuilder.buildMessage("""
                        Выберите опцию, которую хотите отредактировать.
                        """,
                callbackQuery,
                incomeKeyboardFactory.editIncomeKeyboard(incomeId));
    }

    @Transactional
    public BotApiMethod<?> editAmountIncome(Message message) {
        User user = userService.findUserByBotApiObject(message);
        String inputText = message.getText();
        BigDecimal amount;

        try {
            amount = new BigDecimal(inputText);
            if (amount.compareTo(BigDecimal.ZERO) < 0) {
                return messageBuilder.buildMessage(
                        "Ошибка: сумма не может быть меньше нуля.",
                        message,
                        null);
            }
            if (amount.compareTo(new BigDecimal("1000000000")) > 0) {
                return messageBuilder.buildMessage(
                        "Ошибка: сумма слишком большая. Максимально допустимая сумма - 1,000,000,000.",
                        message,
                        null);
            }
        } catch (NumberFormatException e) {
            return messageBuilder.buildMessage(
                    "Неверный формат числа. Пожалуйста, введите сумму в корректном формате.",
                    message,
                    null);
        }
        Income income = getIncomeById(user.getCurrentIncomeId());
        updateIncomeAmount(income, amount);
        userService.updateCurrentIncomeId(user.getChatId(), null, Action.FREE);
        return incomeMenu(message);
    }



    @Transactional
    public BotApiMethod<?> editIncomeCategory(CallbackQuery callbackQuery, IncomeCategory incomeCategory, String incomeId) {
        User user = userService.findUserByBotApiObject(callbackQuery);
        Income income = getIncomeById(Long.valueOf(incomeId));
        updateIncomeCategory(income, incomeCategory);
        userService.updateCurrentExpenseId(user.getChatId(), null, Action.FREE);
        return incomeMenu(callbackQuery);
    }

    @Transactional
    public BotApiMethod<?> editNoteIncome(Message message) {
        Long chatId = message.getChatId();
        User user = userService.findUserByBotApiObject(message);
        Income income = getIncomeById(user.getCurrentIncomeId());
        updateIncomeNote(income, message.getText());
        userService.updateCurrentIncomeId(chatId, null, Action.FREE);
        return incomeMenu(message);
    }

    public BotApiMethod<?> deleteIncome(CallbackQuery callbackQuery, String incomeId) {
        Income income = getIncomeById(Long.valueOf(incomeId));
        incomeRepo.delete(income);
        return messageBuilder.buildMessage(
                "Доход успешно удалена!",
                callbackQuery,
                incomeKeyboardFactory.backToMainMenu("↩️ Назад в меню доходов"));
    }

    public BotApiMethod<?> getIncomeReport(CallbackQuery callbackQuery, String incomeId) {

        Account account = accountService.getAccountById(Long.valueOf(incomeId));

        List<Income> incomes = getAllIncomesByAccountAndDateAfter(account, LocalDate.now().minusMonths(1));

        Map<IncomeCategory, BigDecimal> incomeMap = incomes.stream()
                .collect(Collectors.groupingBy(
                        Income::getCategory,
                        Collectors.reducing(BigDecimal.ZERO, Income::getAmount, BigDecimal::add)
                ));

        BigDecimal totalAmount = incomes.stream()
                .map(Income::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        StringBuilder report = new StringBuilder("Отчет по доходам:\n");

        incomeMap.forEach((category, amount) -> {
            BigDecimal percentage = amount.divide(totalAmount, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);

            report.append(String.format("""
                            
                            Категория: %s
                            Сумма: %s
                            Процент: %.2f%%
                            """,
                    category.getDisplayName(), amount, percentage));
        });

        return messageBuilder.buildMessage(
                report.toString(),
                callbackQuery,
                incomeKeyboardFactory.backToMainMenu("Назад"));
    }

    public List<Income> getAllIncomesByAccountAndDateAfter(Account account, LocalDate date) {
        entityManager.clear();
        return incomeRepo.findByAccountAndDateAfter(account, date).orElse(Collections.emptyList());
    }

    private Income getIncomeById(Long incomeId) {
        return incomeRepo.findById(incomeId).orElse(null);
    }

    @Transactional
    public void updateIncomeAmount(Income income, BigDecimal newAmount) {
        if (newAmount != null) {
            String updateQuery = "UPDATE Income i SET i.amount = :newAmount WHERE i.id = :incomeId";
            entityManager.createQuery(updateQuery)
                    .setParameter("newAmount", newAmount)
                    .setParameter("incomeId", income.getId())
                    .executeUpdate();
        }
    }

    @Transactional
    public void updateIncomeCategory(Income income, IncomeCategory newCategory) {
        if (newCategory != null) {
            String updateQuery = "UPDATE Income i SET i.category = :newCategory WHERE i.id = :incomeId";
            entityManager.createQuery(updateQuery)
                    .setParameter("newCategory", newCategory)
                    .setParameter("incomeId", income.getId())
                    .executeUpdate();
        }
    }

    @Transactional
    public void updateIncomeNote(Income income, String newNote) {
        if (newNote != null) {
            String updateQuery = "UPDATE Income i SET i.note = :newNote WHERE i.id = :incomeId";
            entityManager.createQuery(updateQuery)
                    .setParameter("newNote", newNote)
                    .setParameter("incomeId", income.getId())
                    .executeUpdate();
        }
    }

    @Transactional
    public void updateIncomeDate(Income income, LocalDate newDate) {
        if (newDate != null) {
            String updateQuery = "UPDATE Income i SET i.date = :newDate WHERE i.id = :incomeId";
            entityManager.createQuery(updateQuery)
                    .setParameter("newDate", newDate)
                    .setParameter("incomeId", income.getId())
                    .executeUpdate();
        }
    }
}
