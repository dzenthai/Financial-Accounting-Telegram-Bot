package com.dzenthai.financial.accounting.entity;

import com.dzenthai.financial.accounting.entity.enums.IncomeCategory;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;


@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "incomes")
public class Income {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected Long id;

    @ManyToOne(targetEntity = Account.class, cascade = {
            CascadeType.MERGE, CascadeType.REFRESH})
    @JoinColumn(name = "account_id")
    private Account account;

    @Column(nullable = false)
    @DecimalMin(value = "0.1", message = "Ошибка: сумма не может быть меньше нуля.")
    @DecimalMax(value = "1000000000", message = "Ошибка: сумма слишком большая. Максимально допустимая сумма - 1,000,000,000.")
    private BigDecimal amount;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private IncomeCategory category;

    @Column(nullable = false)
    private String note;

    @Temporal(TemporalType.TIMESTAMP)
    @DateTimeFormat(pattern = "dd/MM/yyyy")
    private LocalDateTime datetime;
}
