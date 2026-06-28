package com.github.namuan.fintech.controls;
public record ApprovalPolicy(int requiredApprovers, boolean requesterMayApprove) { public ApprovalPolicy { if(requiredApprovers < 1) throw new IllegalArgumentException("requiredApprovers must be positive"); } }
