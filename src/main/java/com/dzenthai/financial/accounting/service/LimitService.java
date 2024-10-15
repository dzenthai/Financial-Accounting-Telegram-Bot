package com.dzenthai.financial.accounting.service;

import com.dzenthai.financial.accounting.entity.Account;
import com.dzenthai.financial.accounting.entity.Expense;
import com.dzenthai.financial.accounting.entity.Limit;
import com.dzenthai.financial.accounting.repository.LimitRepo;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;


@Service
public class LimitService {

    private final LimitRepo limitRepo;

    private final ExpenseService expenseService;

    public LimitService(LimitRepo limitRepo, ExpenseService expenseService) {
        this.limitRepo = limitRepo;
        this.expenseService = expenseService;
    }

    public Limit getLimitByAccount(Account account) {
        return limitRepo.findLimitByAccount(account).orElse(null);
    }

    @Transactional
    public void deleteLimit(Limit limit) {
        limitRepo.delete(limit);
    }

    @Transactional
    public void addLimit(Limit limit) {
        limitRepo.save(limit);
    }

    public boolean hasLimitExceeded(Account account, Expense expense) {
        Limit limit = getLimitByAccount(account);
        if (limit != null) {
            BigDecimal expenses =
                   expenseService.getAllExpensesByAccount(account)
                            .stream()
                            .map(Expense::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add)
                            .add(expense.getAmount());
            return expenses.compareTo(limit.getLimitAmount()) > 0;
        }
        return false;
    }
}
