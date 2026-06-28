package com.github.namuan.fintech.serialization;

import com.github.namuan.fintech.money.Money;

public record MoneyJson(String amount, String asset, Integer scale) {
    public static MoneyJson from(Money money) {
        return new MoneyJson(money.decimalValue().toPlainString(), money.asset().code(), money.amount().scale());
    }
}
