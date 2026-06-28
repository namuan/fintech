package com.github.namuan.fintech.controls;
import java.util.Set;
public record Role(String name, Set<Permission> permissions) { public Role { permissions = permissions == null ? Set.of() : Set.copyOf(permissions); } }
