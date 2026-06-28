package com.github.namuan.fintech.integration;
import java.time.Instant; import java.util.Map;
public record ProviderResponseRecord(String requestId, int statusCode, Instant receivedAt, Map<String,String> headers, String body) { public ProviderResponseRecord { headers = headers == null ? Map.of() : Map.copyOf(headers); } }
