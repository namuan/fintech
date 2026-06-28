package com.github.namuan.fintech.currency;

import java.util.Objects;
import java.util.Optional;

public record CryptoAsset(
        String network,
        Optional<String> contractAddress,
        String symbol,
        int scale,
        String displayName
) implements AssetId {
    public CryptoAsset {
        Objects.requireNonNull(network, "network");
        Objects.requireNonNull(contractAddress, "contractAddress");
        Objects.requireNonNull(symbol, "symbol");
        Objects.requireNonNull(displayName, "displayName");
        if (network.isBlank()) throw new IllegalArgumentException("network is required");
        if (symbol.isBlank()) throw new IllegalArgumentException("symbol is required");
        if (scale < 0) throw new IllegalArgumentException("scale must be non-negative");
    }

    @Override public String code() {
        return contractAddress.map(address -> network + ":" + address).orElse(network + ":native:" + symbol);
    }
}
