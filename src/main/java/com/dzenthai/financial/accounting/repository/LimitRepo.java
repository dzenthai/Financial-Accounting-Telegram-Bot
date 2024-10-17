package com.dzenthai.financial.accounting.repository;

import com.dzenthai.financial.accounting.entity.Account;
import com.dzenthai.financial.accounting.entity.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface LimitRepo extends JpaRepository<Limit, Long> {

    Optional<Limit> findLimitByAccount(Account account);

}
