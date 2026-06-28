package com.github.namuan.fintech.reservations;
import java.util.UUID;
public record ReservationId(String value) { public static ReservationId random() { return new ReservationId(UUID.randomUUID().toString()); } }
