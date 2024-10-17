package com.dzenthai.financial.accounting.service;

import com.dzenthai.financial.accounting.entity.Account;
import com.dzenthai.financial.accounting.entity.Expense;
import com.dzenthai.financial.accounting.entity.Limit;
import com.dzenthai.financial.accounting.entity.User;
import com.dzenthai.financial.accounting.entity.enums.Action;
import com.dzenthai.financial.accounting.entity.enums.ExpenseCategory;
import com.dzenthai.financial.accounting.repository.ExpenseRepo;
import com.dzenthai.financial.accounting.service.builder.MessageBuilder;
import com.dzenthai.financial.accounting.service.factory.ExpenseKeyboardFactory;
import com.dzenthai.financial.accounting.service.manager.ExpenseManager;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Slf4j
@Service
public class ExpenseService {

    @PersistenceContext
    private EntityManager entityManager;
    private final ExpenseRepo expenseRepo;
    private final ExpenseKeyboardFactory expenseKeyboardFactory;
    private final MessageBuilder messageBuilder;
    private final UserService userService;
    private final AccountService accountService;
    private final LimitService limitService;
    private final ExpenseManager expenseManager;

    public ExpenseService(
            ExpenseRepo expenseRepo,
            @Lazy ExpenseKeyboardFactory expenseKeyboardFactory,
            MessageBuilder messageBuilder,
            UserService userService,
            @Lazy AccountService accountService,
            @Lazy LimitService limitService,
            @Lazy ExpenseManager expenseManager) {
        this.expenseRepo = expenseRepo;
        this.expenseKeyboardFactory = expenseKeyboardFactory;
        this.messageBuilder = messageBuilder;
        this.userService = userService;
        this.accountService = accountService;
        this.limitService = limitService;
        this.expenseManager = expenseManager;
    }

    public BotApiMethod<?> expenseMenu(BotApiObject botApiObject) {
        Long chatId;
        if (botApiObject instanceof CallbackQuery callbackQuery) {
            chatId = callbackQuery.getMessage().getChatId();
            userService.updateCurrentExpenseId(chatId, null, Action.FREE);
            return messageBuilder.buildMessage("""
                            Меню расходов. 💸
                            
                            Нажмите на один из ваших расходов, чтобы посмотреть подробности.
                            
                            Выберите интересующий вас пункт меню:
                            """,
                    callbackQuery,
                    expenseKeyboardFactory.expenseMenuKeyboard(callbackQuery));
        }
        if (botApiObject instanceof Message message) {
            chatId = message.getChatId();
            userService.updateCurrentExpenseId(chatId, null, Action.FREE);
            return messageBuilder.buildMessage("""
                            Ваша трата успешно сохранена!
                            
                            Нажмите на один из ваших расходов, чтобы посмотреть подробности.
                            
                            Выберите интересующий вас пункт меню:
                            """,
                    message,
                    expenseKeyboardFactory.expenseMenuKeyboard(message));
        }
        return null;
    }

    @Transactional
    public BotApiMethod<?> saveAmountExpense(Message message) {
        Long chatId = message.getChatId();
        String messageText = message.getText();
        try {
            BigDecimal amount = new BigDecimal(messageText);
            User user = userService.findUserByBotApiObject(message);
            Account account = accountService.getAccountById(user.getCurrentAccountId());
            Expense expense = Expense.builder()
                    .account(account)
                    .amount(amount)
                    .build();

            expenseRepo.save(expense);

            userService.updateCurrentExpenseId(chatId, expense.getId(), Action.EXPENSE_ADD_CATEGORY);

            if (limitService.hasLimitExceeded(account, expense)) {
                updateLimitExceeded(expense, true);
            }

            return expenseManager.askForExpenseCategoryToAdd(message);

        } catch (NumberFormatException e) {
            return messageBuilder.buildMessage(
                    "Неверный формат числа. Пожалуйста, введите сумму в корректном формате.",
                    message,
                    null);
        }
    }

    @Transactional
    public BotApiMethod<?> saveExpenseCategory(CallbackQuery callbackQuery, ExpenseCategory expenseCategory) {
        User user = userService.findUserByBotApiObject(callbackQuery);
        Expense expense = getExpenseById(user.getCurrentExpenseId());
        updateExpenseCategory(expense, expenseCategory);
        updateExpenseNote(expense, "-");
        return expenseManager.askForExpenseNoteToAdd(callbackQuery);
    }

    @Transactional
    public BotApiMethod<?> saveNoteExpense(Message message) {
        User user = userService.findUserByBotApiObject(message);
        Expense expense = getExpenseById(user.getCurrentExpenseId());
        updateExpenseNote(expense, message.getText());
        updateExpenseDate(expense, LocalDateTime.now());
        userService.updateCurrentExpenseId(user.getChatId(), expense.getId(), Action.FREE);
        return expenseMenu(message);
    }

    public BotApiMethod<?> getExpense(CallbackQuery callbackQuery, String expenseId) {
        Expense expense = getExpenseById(Long.parseLong(expenseId));
        String category = expense.getCategory().getDisplayName();
        String accountName = expense.getAccount().getName();
        LocalDateTime datetime = expense.getDatetime();
        String note = expense.getNote();
        String amount = expense.getAmount().toString();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy \nВремя: HH:mm:ss");
        String formattedDateTime = datetime.format(formatter);
        return messageBuilder.buildMessage("""
                        Расход.
                        
                        Категория: %s
                        Счет: %s
                        Описание: %s
                        Дата: %s
                        
                        💰 Сумма: %s
                        """.formatted(category, accountName, note, formattedDateTime, amount),
                callbackQuery,
                expenseKeyboardFactory.editAndDeleteExpenseKeyboard(expenseId));
    }

    public BotApiMethod<?> editExpense(CallbackQuery callbackQuery, String expenseId) {
        return messageBuilder.buildMessage("""
                        Выберите опцию, которую хотите отредактировать.
                        """,
                callbackQuery,
                expenseKeyboardFactory.editExpenseKeyboard(expenseId));
    }

    @Transactional
    public BotApiMethod<?> editAmountExpense(Message message) {
        User user = userService.findUserByBotApiObject(message);
        String messageText = message.getText();
        try {
            BigDecimal amount = new BigDecimal(messageText);

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

            Expense expense = getExpenseById(user.getCurrentExpenseId());
            updateExpenseAmount(expense, amount);
            userService.updateCurrentExpenseId(user.getChatId(), null, Action.FREE);
        } catch (NumberFormatException e) {
            return messageBuilder.buildMessage(
                    "Неверный формат числа. Пожалуйста, введите сумму в корректном формате.",
                    message,
                    null);
        }
        return expenseMenu(message);
    }


    @Transactional
    public BotApiMethod<?> editExpenseCategory(CallbackQuery callbackQuery, ExpenseCategory expenseCategory, String expenseId) {
        User user = userService.findUserByBotApiObject(callbackQuery);
        Expense expense = getExpenseById(Long.valueOf(expenseId));
        updateExpenseCategory(expense, expenseCategory);
        userService.updateCurrentExpenseId(user.getChatId(), null, Action.FREE);
        return expenseMenu(callbackQuery);
    }

    @Transactional
    public BotApiMethod<?> editNoteExpense(Message message) {
        User user = userService.findUserByBotApiObject(message);
        String note = message.getText();
        Expense expense = getExpenseById(user.getCurrentExpenseId());
        updateExpenseNote(expense, note);
        userService.updateCurrentExpenseId(user.getChatId(), null, Action.FREE);
        return expenseMenu(message);
    }

    public BotApiMethod<?> deleteExpense(CallbackQuery callbackQuery, String expenseId) {
        Expense expense = getExpenseById(Long.parseLong(expenseId));
        expenseRepo.delete(expense);
        return messageBuilder.buildMessage(
                "Трата успешно удалена!",
                callbackQuery,
                expenseKeyboardFactory.backToExpenseMenu("↩️ Назад в меню трат"));
    }

    public BotApiMethod<?> getExpenseReport(CallbackQuery callbackQuery, String expenseId) {

        Account account = accountService.getAccountById(Long.valueOf(expenseId));

        List<Expense> expenses = getAllExpensesByAccountAndDateAfter(account, LocalDateTime.now().minusMonths(1));

        Map<ExpenseCategory, BigDecimal> expenseMap = expenses.stream()
                .collect(Collectors.groupingBy(
                        Expense::getCategory,
                        Collectors.reducing(BigDecimal.ZERO, Expense::getAmount, BigDecimal::add)
                ));

        BigDecimal totalAmount = expenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        StringBuilder report = new StringBuilder("Отчет по тратам:\n");

        expenseMap.forEach((category, amount) -> {
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
                expenseKeyboardFactory.backToExpenseMenu("↩️ Назад"));
    }

    @Transactional
    public BotApiMethod<?> saveExpenseLimit(Message message) {
        String messageText = message.getText();
        try {
            BigDecimal limitAmount = new BigDecimal(messageText);
            User user = userService.findUserByBotApiObject(message);
            Account account = accountService.getAccountById(user.getCurrentAccountId());
            Limit limit  = Limit.builder()
                    .limitAmount(limitAmount)
                    .account(account)
                    .datetime(LocalDateTime.now())
                    .build();
            limitService.addLimit(limit);
            userService.updateUserAction(user.getChatId(), Action.FREE);

            return messageBuilder.buildMessage(
                    "Лимит для аккаунта %s успешно установлен!".formatted(account.getName()),
                    message,
                    expenseKeyboardFactory.backToLimitMenu("↩️ Назад в меню лимитов"));

        } catch (NumberFormatException e) {
            return messageBuilder.buildMessage(
                    "Ошибка: введите корректное число для лимита.",
                    message,
                    null);
        }
    }

    @Transactional
    public BotApiMethod<?> deleteLimit(CallbackQuery callbackQuery, String accountId) {
        Account account = accountService.getAccountById(Long.valueOf(accountId));
        Limit limit = limitService.getLimitByAccount(account);
        if (limit != null) {
            String updateLimitExceeded = "UPDATE Expense e SET e.limitExceeded = false WHERE e.account = :account";
            entityManager.createQuery(updateLimitExceeded)
                    .setParameter("account", account)
                    .executeUpdate();
            limitService.deleteLimit(limit);
        }
        return messageBuilder.buildMessage(
                "Лимит успешно удален!",
                callbackQuery,
                expenseKeyboardFactory.backToLimitMenu("↩️ Назад в меню лимитов"));
    }

    public BotApiMethod<?> getLimitMenu(CallbackQuery callbackQuery) {
        User user = userService.findUserByBotApiObject(callbackQuery);
        Account account = accountService.getAccountById(user.getCurrentAccountId());
        Limit limit = limitService.getLimitByAccount(account);
        List<Expense> expenses = getAllExpensesByAccount(account);
        if (limit == null) {
            return messageBuilder.buildMessage("""
                            Добро пожаловать в меню лимитов!
                            
                            Здесь вы можете установить лимит для вашего счета,
                            а также просматривать списки расходов, которые превысили данный лимит.
                            
                            Нажмите на кнопку "Установить лимит", чтобы начать работу с лимитом.
                            """,
                    callbackQuery,
                    expenseKeyboardFactory.getLimitMenuKeyboard(callbackQuery));
        }
        BigDecimal totalExpenses = expenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal availableBalance =  limit.getLimitAmount().subtract(totalExpenses);

        String emoji = "🟢";

        if (availableBalance.compareTo(BigDecimal.ZERO) < 0) {
            emoji = "🔴";
            availableBalance = BigDecimal.valueOf(0);
        }
        return messageBuilder.buildMessage("""
                Меню лимитов
                
                💰 Сумма расходов: %s
                
                ⚡ Лимит: %s
                
                %s Остаток: %s
                
                Выберите интересующий вас пункт меню:
                """.formatted(totalExpenses, limit.getLimitAmount(), emoji, availableBalance),
                callbackQuery,
                expenseKeyboardFactory.getLimitMenuKeyboard(callbackQuery));
    }

    public BotApiMethod<?> getAllLimitExceededExpenses(CallbackQuery callbackQuery) {
        User user = userService.findUserByBotApiObject(callbackQuery);
        Account account = accountService.getAccountById(user.getCurrentAccountId());
        List<Expense> expenses = getAllExpensesByAccount(account).stream().filter(Expense::isLimitExceeded).toList();

        return messageBuilder.buildMessage(
                "Траты превысившие лимит:",
                callbackQuery,
                expenseKeyboardFactory.getAllLimitExceededExpensesKeyboard(expenses));
    }

    public Expense getExpenseById(Long id) {
        return expenseRepo.findExpenseById(id).orElse(null);
    }

    public List<Expense> getAllExpensesByAccount(Account account) {
        return expenseRepo.findByAccount(account).orElse(null);
    }

    public List<Expense> getAllExpensesByAccountAndDateAfter(Account account, LocalDateTime datetime) {
        entityManager.clear();
        return expenseRepo.findByAccountAndDatetimeAfter(account, datetime).orElse(Collections.emptyList());

    }

    @Transactional
    public void updateExpenseAmount(Expense expense, BigDecimal newAmount) {
        if (newAmount != null) {
            String updateQuery = "UPDATE Expense e SET e.amount = :newAmount WHERE e.id = :expenseId";
            entityManager.createQuery(updateQuery)
                    .setParameter("newAmount", newAmount)
                    .setParameter("expenseId", expense.getId())
                    .executeUpdate();
        }
    }

    @Transactional
    public void updateExpenseCategory(Expense expense, ExpenseCategory newCategory) {
        if (newCategory != null) {
            String updateQuery = "UPDATE Expense e SET e.category = :newCategory WHERE e.id = :expenseId";
            entityManager.createQuery(updateQuery)
                    .setParameter("newCategory", newCategory)
                    .setParameter("expenseId", expense.getId())
                    .executeUpdate();
        }
    }

    @Transactional
    public void updateExpenseNote(Expense expense, String newNote) {
        if (newNote != null) {
            String updateQuery = "UPDATE Expense e SET e.note = :newNote WHERE e.id = :expenseId";
            entityManager.createQuery(updateQuery)
                    .setParameter("newNote", newNote)
                    .setParameter("expenseId", expense.getId())
                    .executeUpdate();
        }
    }

    @Transactional
    public void updateExpenseDate(Expense expense, LocalDateTime newDate) {
        if (newDate != null) {
            String updateQuery = "UPDATE Expense e SET e.datetime = :newDate WHERE e.id = :expenseId";
            entityManager.createQuery(updateQuery)
                    .setParameter("newDate", newDate)
                    .setParameter("expenseId", expense.getId())
                    .executeUpdate();
        }
    }

    @Transactional
    public void updateLimitExceeded(Expense expense, boolean exceeded) {
        String updateLimitExceeded = "UPDATE Expense e SET e.limitExceeded = :isLimitExceed WHERE e.id = :expenseId";
        entityManager.createQuery(updateLimitExceeded)
                .setParameter("isLimitExceed", exceeded)
                .setParameter("expenseId", expense.getId())
                .executeUpdate();
    }
}
