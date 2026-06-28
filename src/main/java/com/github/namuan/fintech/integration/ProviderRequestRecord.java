package com.github.namuan.fintech.integration;
import java.time.Instant; import java.util.Map;
public record ProviderRequestRecord(String id, ExternalProvider provider, String operation, Instant sentAt, Map<String,String> headers, String body) { public ProviderRequestRecord { headers = headers == null ? Map.of() : Map.copyOf(headers); } }
