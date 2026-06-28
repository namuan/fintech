package com.github.namuan.fintech.lineage;
import java.time.Instant;
public record InputVersion(String source, String version, Instant observedAt) {}
