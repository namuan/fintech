package com.github.namuan.fintech.money;

public final class MoneyFormatter {
    private MoneyFormatter() {}

    public static String decimalString(Money money) {
        return money.decimalValue().toPlainString();
    }

    public static String display(Money money) {
        return decimalString(money) + " " + money.asset().code();
    }
}
