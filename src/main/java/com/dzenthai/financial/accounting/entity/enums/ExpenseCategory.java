package com.dzenthai.financial.accounting.entity.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;


@Getter
public enum ExpenseCategory {
    ENTERTAINMENT("Развлечения 🎉 "),
    FOOD("Еда 🍕 "),
    HOUSING("Жилище 🏠 "),
    TRANSPORT("Транспорт 🚗 "),
    CLOTHING("Одежда 👔👗 "),
    UTILITIES("Коммунальные услуги 💡 "),
    MEDICATIONS("Медикаменты 💊 "),
    EDUCATION("Образование 📚 "),
    INSURANCE("Страховка 🛡️ "),
    OTHER("Другое ❓ ");

    private final String displayName;

    ExpenseCategory(String displayName) {
        this.displayName = displayName;
    }

    public static List<String> getDisplayNames() {
        return Arrays.stream(values())
                .map(ExpenseCategory::getDisplayName)
                .toList();
    }

    public static List<String> getNames() {
        return Arrays.stream(values())
                .map(ExpenseCategory::name)
                .toList();
    }
}

