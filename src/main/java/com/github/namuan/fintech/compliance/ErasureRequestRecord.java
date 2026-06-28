package com.github.namuan.fintech.compliance;
import java.time.Instant;
public record ErasureRequestRecord(String id, PiiReference subject, Instant requestedAt, String legalBasis) {}
