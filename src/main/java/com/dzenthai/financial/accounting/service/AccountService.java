package com.dzenthai.financial.accounting.service;

import com.dzenthai.financial.accounting.entity.Account;
import com.dzenthai.financial.accounting.entity.Expense;
import com.dzenthai.financial.accounting.entity.Income;
import com.dzenthai.financial.accounting.entity.User;
import com.dzenthai.financial.accounting.entity.enums.Action;
import com.dzenthai.financial.accounting.repository.AccountRepo;
import com.dzenthai.financial.accounting.service.builder.MessageBuilder;
import com.dzenthai.financial.accounting.service.factory.AccountKeyboardFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.interfaces.BotApiObject;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;


@Service
public class AccountService {

    @PersistenceContext
    private EntityManager entityManager;
    private final AccountKeyboardFactory accountKeyboardFactory;
    private final MessageBuilder messageBuilder;
    private final AccountRepo accountRepo;
    private final UserService userService;
    private final ExpenseService expenseService;
    private final IncomeService incomeService;

    public AccountService(
            AccountKeyboardFactory accountKeyboardFactory,
            MessageBuilder messageBuilder,
            AccountRepo accountRepo,
            UserService userService,
            ExpenseService expenseService,
            IncomeService incomeService
    ) {
        this.accountKeyboardFactory = accountKeyboardFactory;
        this.messageBuilder = messageBuilder;
        this.accountRepo = accountRepo;
        this.userService = userService;
        this.expenseService = expenseService;
        this.incomeService = incomeService;
    }

    public BotApiMethod<?> accountMenu(BotApiObject botApiObject) {
        User user = userService.findUserByBotApiObject(botApiObject);
        List<Account> account = getAllAccountsByUser(user);
        if (account.isEmpty()) {
            return messageBuilder.buildMessage("""
                            Меню счетов. 🏛️
                            
                            Список счетов пуст.
                            
                            Начните управлять финансами, создав свой первый счет!
                            
                            Нажмите на "Создать счет" чтобы начать вести учет.
                            """
                    ,
                    botApiObject,
                    accountKeyboardFactory.accountMenuKeyboard(botApiObject));
        }
        return messageBuilder.buildMessage("""
                        Меню счетов. 🏛️
                        
                        Выберите нужный счет для управления или создайте новый.
                        
                        Список ваших счетов:
                        """,
                botApiObject,
                accountKeyboardFactory.accountMenuKeyboard(botApiObject));
    }

    @Transactional
    public BotApiMethod<?> saveAccount(Message message) {
        Long chatId = message.getChatId();
        String name = message.getText();
        User user = userService.findUserByBotApiObject(message);
        Account account = getAccountByNameAndUser(name, user);
        if (account != null) {
            userService.updateUserAction(chatId, Action.ACCOUNT_ADD);
            return messageBuilder.buildMessage("""
                            Счет с названием "%s" уже существует.
                            
                            Пожалуйста, выберите другое имя для нового счета.
                            
                            Введите название счета
                            """.formatted(name),
                    message,
                    accountKeyboardFactory.backToAccountMenu());
        } else {
            account = Account.builder()
                    .name(name)
                    .user(user)
                    .compareDate(LocalDateTime.now().minusMonths(1))
                    .build();
            accountRepo.save(account);
            userService.updateUserAction(chatId, Action.FREE);
            return messageBuilder.buildMessage("""
                            Счет с названием - %s успешно сохранен!
                            
                            Нажмите на "Перейти к счету" или "Меню счетов",
                            чтобы перейти к вашем новому счету,
                            или вернуться в меню счетов.
                            
                            Выберите действие:
                            """.formatted(name),
                    message,
                    accountKeyboardFactory.backToAccountMenuAfterSaveAccountKeyboard(account.getId()));
        }
    }

    @Transactional
    public BotApiMethod<?> deleteAccount(CallbackQuery callbackQuery, String accountId) {
        Long chatId = callbackQuery.getMessage().getChatId();
        User user = userService.findUserByBotApiObject(callbackQuery);
        Account account = getAccountById(Long.valueOf(accountId));
        if (account != null) {
            accountRepo.deleteAccountByIdAndUser(account.getId(), user);
            userService.updateUserAction(chatId, Action.FREE);
            return messageBuilder.buildMessage(
                    "Счет с названием - %s удален!".formatted(account.getName()),
                    callbackQuery,
                    accountKeyboardFactory.backToAccountMenu());
        }
        return null;
    }

    @Transactional
    public BotApiMethod<?> setAccountDatetime(CallbackQuery callbackQuery, String accountId, LocalDateTime datetime) {
        Account account = getAccountById(Long.valueOf(accountId));
        updateCompareDate(account, datetime);
        return messageBuilder.buildMessage(
                "Дата и время успешно установлены!",
                callbackQuery,
                accountKeyboardFactory.backToAccount(accountId));
    }

    public BotApiMethod<?> getAccount(CallbackQuery callbackQuery, String id) {
        Account account = getAccountById(Long.valueOf(id));
        BigDecimal totalIncomes = incomeService
                .getAllIncomesByAccountAndDateAfter(account, account.getCompareDate())
                .stream()
                .map(Income::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalExpenses = expenseService
                .getAllExpensesByAccountAndDateAfter(account, account.getCompareDate())
                .stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal balance = totalIncomes.subtract(totalExpenses);
        if (totalExpenses.compareTo(BigDecimal.ZERO) == 0 && totalIncomes.compareTo(BigDecimal.ZERO) == 0) {
            return messageBuilder.buildMessage("""
                            В данный момент ваш список доходов и расходов пуст.
                            
                            Добавьте первый доход или трату, чтобы начать учет.
                            
                            Выберите интересующий вас пункт меню:
                            """,
                    callbackQuery,
                    accountKeyboardFactory.accountOperationKeyboard(id)
            );
        }
        String datetime;
        LocalDate compareDate = account.getCompareDate().toLocalDate();
        LocalDate now = LocalDate.now();

        long daysBetween = ChronoUnit.DAYS.between(compareDate, now);
        long monthsBetween = ChronoUnit.MONTHS.between(compareDate, now);
        long yearsBetween = ChronoUnit.YEARS.between(compareDate, now);

        if (daysBetween == 1) {
            datetime = "За день";
        } else if (daysBetween <= 7) {
            datetime = "За неделю";
        } else if (monthsBetween == 1) {
            datetime = "За текущий месяц";
        } else if (monthsBetween == 6) {
            datetime = "За полгода";
        } else if (yearsBetween == 1) {
            datetime = "За год";
        } else {
            datetime = "За указанный период";
        }
        return messageBuilder.buildMessage("""
                        🕒 %s:
                        
                        ✨ Доходы: %s
                        💸 Расходы: %s
                        
                        💰 Баланс: %s
                        """.formatted(datetime, totalIncomes, totalExpenses, balance),
                callbackQuery,
                accountKeyboardFactory.accountOperationKeyboard(id)
        );
    }

    public Account getAccountById(Long id) {
        return accountRepo.findAccountById(id).orElse(null);
    }

    public Account getAccountByNameAndUser(String name, User user) {
        return accountRepo.findAccountByNameAndUser(name, user).orElse(null);
    }

    public List<Account> getAllAccountsByUser(User user) {
        return accountRepo.findAccountsByUser(user).orElse(null);
    }

    @Transactional
    public void updateCompareDate(Account account, LocalDateTime newDate) {
        String updateCompareDateQuery = "UPDATE Account a SET a.compareDate = :newDate WHERE a.id = :accountId";
        entityManager.createQuery(updateCompareDateQuery)
                .setParameter("newDate", newDate)
                .setParameter("accountId", account.getId())
                .executeUpdate();
    }

}
