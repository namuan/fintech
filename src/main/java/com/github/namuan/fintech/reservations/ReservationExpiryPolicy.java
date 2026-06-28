package com.github.namuan.fintech.reservations;
import java.time.Duration;
public record ReservationExpiryPolicy(Duration ttl) {}
