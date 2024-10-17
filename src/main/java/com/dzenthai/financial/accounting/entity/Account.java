package com.dzenthai.financial.accounting.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;


@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "accounts")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected Long id;

    @Column(unique = true, nullable = false)
    @NotBlank(message = "Название аккаунта не может быть пустым.")
    private String name;

    @OneToMany(mappedBy = "account", cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE}, fetch = FetchType.LAZY)
    private List<Expense> expenses;

    @OneToMany(mappedBy = "account", cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE}, fetch = FetchType.LAZY)
    private List<Income> incomes;

    @OneToOne(mappedBy = "account", cascade = CascadeType.REMOVE)
    private Limit account_limit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = true)
    private LocalDateTime compareDate;
}
