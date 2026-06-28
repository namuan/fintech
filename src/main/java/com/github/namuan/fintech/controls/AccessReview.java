package com.github.namuan.fintech.controls;
import java.time.Instant; import java.util.List;
public record AccessReview(String id, Instant reviewedAt, String reviewerId, List<Grant> grants) { public AccessReview { grants = List.copyOf(grants); } }
