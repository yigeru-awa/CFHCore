package com.onehumanawa.cfhcore.foundation.developertool.web;

import com.onehumanawa.cfhcore.foundation.developertool.config.DeveloperToolConfig;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/** In-memory ring buffer of console output lines, shared by the GUI and HTTP API. */
public final class DevConsoleLog {
    private static final Deque<String> LINES = new ArrayDeque<>();

    private DevConsoleLog() {
    }

    public static synchronized void log(String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        for (String line : text.split("\\R", -1)) {
            if (!line.isEmpty()) {
                LINES.addLast(line);
            }
        }
        int max = DeveloperToolConfig.get().maxLogLines;
        while (LINES.size() > max) {
            LINES.removeFirst();
        }
    }

    public static synchronized void clear() {
        LINES.clear();
    }

    public static synchronized List<String> lines() {
        return new ArrayList<>(LINES);
    }
}
