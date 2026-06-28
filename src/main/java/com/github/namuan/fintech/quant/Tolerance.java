package com.github.namuan.fintech.quant;
public record Tolerance(double absolute) { public boolean within(double expected, double actual){ return Math.abs(expected - actual) <= absolute; } }
