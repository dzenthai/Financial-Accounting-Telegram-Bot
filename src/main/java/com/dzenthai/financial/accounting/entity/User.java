package com.dzenthai.financial.accounting.entity;

import com.dzenthai.financial.accounting.entity.enums.Action;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;


@Getter
@Setter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected Long id;

    @Column(unique = true)
    private Long chatId;

    @Enumerated(EnumType.STRING)
    private Action action;

    @Column(nullable = true)
    private Long currentAccountId;

    @Column(nullable = true)
    private Long currentExpenseId;

    @Column(nullable = true)
    private Long currentIncomeId;

    @Column(nullable = true)
    private LocalDate compareDate;
}
