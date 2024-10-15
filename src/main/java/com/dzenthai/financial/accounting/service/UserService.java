package com.dzenthai.financial.accounting.service;

import com.dzenthai.financial.accounting.entity.User;
import com.dzenthai.financial.accounting.entity.enums.Action;
import com.dzenthai.financial.accounting.repository.UserRepo;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.interfaces.BotApiObject;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;


@Service
public class UserService {

    private final UserRepo userRepo;

    @PersistenceContext
    private EntityManager entityManager;

    public UserService(UserRepo userRepo) {
        this.userRepo = userRepo;
    }

    public User findUserByBotApiObject(BotApiObject botApiObject) {
        Long chatId = null;
        if (botApiObject instanceof CallbackQuery callbackQuery) {
            chatId = callbackQuery.getMessage().getChatId();
        } else if (botApiObject instanceof Message message) {
            chatId = message.getChatId();
        }
        if (chatId != null) {
            return userRepo.findUserByChatId(chatId).orElse(null);
        }
        return null;
    }

    @Transactional
    public void updateUserAction(Long chatId, Action action) {
        String updateQuery = "UPDATE User u SET u.action = :newAction WHERE u.chatId = :chatId";
        entityManager.createQuery(updateQuery)
                .setParameter("newAction", action)
                .setParameter("chatId", chatId)
                .executeUpdate();
    }

    @Transactional
    public void updateCurrentAccountId(Long chatId, Long currentAccountId, Action action) {
        String updateQuery = "UPDATE User u SET u.currentAccountId = :currentAccountId, u.action = :newAction WHERE u.chatId = :chatId";
        entityManager.createQuery(updateQuery)
                .setParameter("currentAccountId", currentAccountId)
                .setParameter("newAction", action)
                .setParameter("chatId", chatId)
                .executeUpdate();

        entityManager.flush();
    }

    @Transactional
    public void updateCurrentExpenseId(Long chatId, Long currentExpenseId, Action action) {
        String updateQuery = "UPDATE User u SET u.currentExpenseId = :currentExpenseId, u.action = :newAction WHERE u.chatId = :chatId";
        entityManager.createQuery(updateQuery)
                .setParameter("currentExpenseId", currentExpenseId)
                .setParameter("newAction", action)
                .setParameter("chatId", chatId)
                .executeUpdate();

        entityManager.flush();
    }

    @Transactional
    public void updateCurrentIncomeId(Long chatId, Long currentIncomeId, Action action) {
        String updateQuery = "UPDATE User u SET u.currentIncomeId = :currentIncomeId, u.action = :newAction WHERE u.chatId = :chatId";
        entityManager.createQuery(updateQuery)
                .setParameter("currentIncomeId", currentIncomeId)
                .setParameter("newAction", action)
                .setParameter("chatId", chatId)
                .executeUpdate();

        entityManager.flush();
    }
}
