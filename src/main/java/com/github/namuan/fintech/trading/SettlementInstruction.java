package com.github.namuan.fintech.trading;
import com.github.namuan.fintech.money.Money; import java.time.LocalDate;
public record SettlementInstruction(String executionId, Money deliver, Money receive, LocalDate settlementDate) {}
