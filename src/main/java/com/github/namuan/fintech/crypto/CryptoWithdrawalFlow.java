package com.github.namuan.fintech.crypto;
import com.github.namuan.fintech.reservations.Reservation;
public record CryptoWithdrawalFlow(String id, Reservation reservation, WalletAddress destination, NetworkFeeEstimate feeEstimate, OnChainTransaction transaction) {}
