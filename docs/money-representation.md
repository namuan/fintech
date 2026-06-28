# Money representation

## What problem this package solves

Before you can move or record money, you have to represent it. Getting the representation wrong means every layer above inherits the error — silent precision loss, accidental cross‑currency arithmetic, or misinterpreted scale at an API boundary.

The `money` and `currency` packages provide the primitives that make those mistakes unrepresentable or explicit.

## Package layout

```
com.github.namuan.fintech
  currency
    AssetId            – sealed interface (permits FiatCurrency, CryptoAsset)
    FiatCurrency       – ISO 4217 code + scale + display name
    CryptoAsset        – network + optional contract address + symbol + scale
    CurrencyUnit       – non-sealed extension point for custom assets
  money
    Amount             – sealed interface (permits ScaledAmount, DecimalAmount)
    ScaledAmount       – BigInteger mantissa + int scale (minor-unit integer model)
    DecimalAmount      – BigDecimal wrapper for arbitrary-precision computation
    Money              – Amount + AssetId; arithmetic forbidden across assets
    MoneyParser        – construct Money from string decimal or mantissa+scale
    MoneyFormatter     – display-friendly and JSON-safe output helpers
```

## What it deliberately does not solve

- The library does **not** enforce a single canonical representation. `ScaledAmount` and `DecimalAmount` coexist because storage and computation have different needs (see Domain tradeoffs).
- It does **not** encode a universal currency database. `FiatCurrency` reads from `java.util.Currency` but callers can construct one directly with custom metadata. Validate against a controlled set at your boundary.
- It does **not** stop you from using `double` in quant/risk code. `ApproximateAmount` in the `quant` package explicitly marks approximate calculations so they are not accidentally booked into the ledger.

## Safe defaults

- `Money.plus` / `Money.minus` reject cross‑asset arithmetic at runtime.
- `Posting` requires a non‑negative amount; the debit/credit side carries the sign, not a negative number.
- `ScaledAmount` stores the scale explicitly; no assumption that every currency has 2 decimal digits.
- `CryptoAsset` identity requires a network identifier. A USDC token on Ethereum is not the same as a USDC token on Polygon.
- `MoneyJson` serialises amount as a string (`"12.34"`) to avoid JSON‑number precision loss in default parsers.

## Dangerous tradeoffs

| Tradeoff | Risk | Mitigation |
|---|---|---|
| Using `DecimalAmount` as the canonical store | `BigDecimal` is slower and heap‑heavier than `long` | Use `ScaledAmount` for high‑throughput paths; convert at boundaries |
| Assuming `FiatCurrency.of("XYZ")` always works | `Currency.getInstance` throws for unknown codes | Validate against your controlled set first |
| Treating pegged/wrapped crypto as equivalent to the underlying | `USDC` ≠ `USD`; a de‑peg or bridge exploit breaks the assumption | Model them as distinct `CryptoAsset` records |
| Parsing bare JSON numbers for money | Most JSON parsers map to `double`, reintroducing precision loss | Always serialise as string or mantissa+scale; use `MoneyJson` |

## Example usage

```java
// Fiat
FiatCurrency usd = new FiatCurrency("USD", 2, "US Dollar");
Money balance = Money.decimal("123.45", usd);

// Crypto
CryptoAsset usdcEth = new CryptoAsset(
    "ethereum",
    Optional.of("0xA0b86991c6218b36c1d19D4a2e9Eb0cE3606eB48"),
    "USDC", 6, "USD Coin (Ethereum)"
);
Money cryptoBalance = Money.minorUnits(1_000_000, usdcEth); // 1.000000 USDC

// Arithmetic is asset-safe
Money sum = balance.plus(balance);                    // OK
Money cross = balance.plus(Money.decimal("1.00", eur));// throws

// Serialisation
MoneyJson json = MoneyJson.from(balance);
// { "amount": "123.45", "asset": "USD", "scale": 2 }
```

## Read next

- [Ledger](ledger.md) — how money moves between accounts
- [Domain tradeoffs](domain-tradeoffs.md) — when to use ScaledAmount vs DecimalAmount vs ApproximateAmount
