package com.github.namuan.fintech.lineage;
import java.util.List;
public record LineageRecord(String id, SourceSnapshotId snapshotId, List<InputVersion> inputs, List<TransformationStep> transformations) { public LineageRecord { inputs = List.copyOf(inputs); transformations = List.copyOf(transformations); } }
