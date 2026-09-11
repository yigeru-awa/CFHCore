package com.onehumanawa.cfhcore.foundation.registry;

import java.util.HashMap;
import java.util.Map;

public final class LangCollector {

    private final Map<String, LangEntry> entries = new HashMap<>();

    public void add(String key, String en, String zh) {
        entries.put(key, new LangEntry(en, zh));
    }

    public Map<String, LangEntry> getEntries() {
        return entries;
    }

    public record LangEntry(String en, String zh) {}

    public Map<String, Map<String, String>> generateAll() {
        Map<String, Map<String, String>> result = new HashMap<>();
        Map<String, String> enMap = new HashMap<>();
        Map<String, String> zhMap = new HashMap<>();

        for (Map.Entry<String, LangEntry> entry : entries.entrySet()) {
            enMap.put(entry.getKey(), entry.getValue().en());
            zhMap.put(entry.getKey(), entry.getValue().zh());
        }

        result.put("en_us", enMap);
        result.put("zh_cn", zhMap);
        return result;
    }
}
