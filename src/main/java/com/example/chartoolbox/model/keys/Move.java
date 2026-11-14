package com.example.chartoolbox.model.keys;

/** One action "fix for me" moves: from its key (and modifier) to a free key, or to {@link Bind#UNBOUND} when none was free. */
public record Move(String id, String label, String ownerName, String fromKey, String fromModifier, String toKey) {}
