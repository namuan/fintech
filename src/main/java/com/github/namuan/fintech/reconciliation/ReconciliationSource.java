package com.github.namuan.fintech.reconciliation;
import java.util.List;
@FunctionalInterface public interface ReconciliationSource { List<ReconciliationRecord> records(); }
