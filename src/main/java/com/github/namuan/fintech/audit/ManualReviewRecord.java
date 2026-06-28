package com.github.namuan.fintech.audit;
import java.time.Instant;
public record ManualReviewRecord(String id, String reviewerId, String outcome, Instant reviewedAt) {}
