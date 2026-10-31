package com.onehumanawa.cfhcore.content.gui.config.mods;

import java.util.Map;
import java.util.Set;

public record ModFacts(Set<String> declaredLibraries, Set<String> nested, Map<String, Set<String>> requires) {
   public static final ModFacts NONE = new ModFacts(Set.of(), Set.of(), Map.of());
}
