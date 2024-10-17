package com.dzenthai.financial.accounting.repository;

import com.dzenthai.financial.accounting.entity.Account;
import com.dzenthai.financial.accounting.entity.Income;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


@Repository
public interface IncomeRepo extends JpaRepository<Income, Long> {

    Optional<List<Income>> findByAccountAndDatetimeAfter(Account account, LocalDateTime date);

}
