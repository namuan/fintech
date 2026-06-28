package com.github.namuan.fintech.trading;
import java.util.List;
public record Execution(String id, String orderId, List<Fill> fills) { public Execution { fills = List.copyOf(fills); } }
