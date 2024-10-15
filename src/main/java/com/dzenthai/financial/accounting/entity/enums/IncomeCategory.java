package com.dzenthai.financial.accounting.entity.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;


@Getter
public enum IncomeCategory {
    SALARY("Зарплата 💼 "),
    SALARY2("Зарплата 2 🏢 "),
    FREELANCE("Фриланс 💻 "),
    BONUSES("Бонусы 🎁 "),
    COMMISSION("Комиссия 📊 "),
    INVESTMENT("Инвестиции 📈 "),
    RENTAL("Аренда 🏠 "),
    INTEREST("Проценты 💰 "),
    SIDEHUSTLE("Подработка 🛠️ "),
    PENSION("Пенсия 👴 "),
    WELFARE("Социальная помощь 🏥 "),
    ANNUITIES("Аннуитеты 📑 "),
    OTHER("Другое ❓ ");

    private final String displayName;

    IncomeCategory(String displayName) {
        this.displayName = displayName;
    }

    public static List<String> getDisplayNames() {
        return Arrays.stream(values())
                .map(IncomeCategory::getDisplayName)
                .toList();
    }

    public static List<String> getNames() {
        return Arrays.stream(values())
                .map(IncomeCategory::name)
                .toList();

    }
}
