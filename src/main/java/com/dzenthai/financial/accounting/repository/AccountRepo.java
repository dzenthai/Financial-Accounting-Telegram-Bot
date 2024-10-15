package com.dzenthai.financial.accounting.repository;

import com.dzenthai.financial.accounting.entity.Account;
import com.dzenthai.financial.accounting.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface AccountRepo extends JpaRepository<Account, Long> {

    Optional<Account> findAccountById(Long id);

    Optional<Account> findAccountByNameAndUser(String name, User user);

    Optional<List<Account>> findAccountsByUser(User user);

    void deleteAccountByIdAndUser(Long id, User user);
}
