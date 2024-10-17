package com.dzenthai.financial.accounting.repository;

import com.dzenthai.financial.accounting.entity.Account;
import com.dzenthai.financial.accounting.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


@Repository
public interface ExpenseRepo extends JpaRepository<Expense, Long> {

    Optional<List<Expense>> findByAccountAndDatetimeAfter(Account account, LocalDateTime date);

    Optional<List<Expense>> findByAccount(Account account);

    Optional<Expense> findExpenseById(Long id);

}
