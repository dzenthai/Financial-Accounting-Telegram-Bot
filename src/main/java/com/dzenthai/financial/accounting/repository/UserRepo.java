package com.dzenthai.financial.accounting.repository;

import com.dzenthai.financial.accounting.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface UserRepo extends JpaRepository<User, Long> {

    Optional<User> findUserByChatId(Long chatId);

    boolean existsByChatId(Long chatId);

}
