package com.github.namuan.fintech.lineage;
import java.util.Map;
public record ReplayContext(SourceSnapshotId snapshotId, Map<String, InputVersion> inputVersions) { public ReplayContext { inputVersions = inputVersions == null ? Map.of() : Map.copyOf(inputVersions); } }
