package com.github.namuan.fintech.money;

import com.github.namuan.fintech.currency.AssetId;
import java.math.BigDecimal;
import java.math.BigInteger;

public final class MoneyParser {
    private MoneyParser() {}

    public static Money decimalString(String amount, AssetId asset) {
        return new Money(new DecimalAmount(new BigDecimal(amount)), asset);
    }

    public static Money mantissaAndScale(String mantissa, int scale, AssetId asset) {
        return new Money(new ScaledAmount(new BigInteger(mantissa), scale), asset);
    }
}
