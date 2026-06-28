package com.github.namuan.fintech.controls;
import java.time.Instant;
public record Grant(String actorId, Role role, String grantedBy, Instant grantedAt) {}
