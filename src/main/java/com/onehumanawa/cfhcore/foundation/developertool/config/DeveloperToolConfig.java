package com.onehumanawa.cfhcore.foundation.developertool.config;

import com.onehumanawa.cfhcore.content.gui.core.config.JsonConfig;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Developer console settings. Only used in singleplayer, never on a server.
 */
public final class DeveloperToolConfig {
    public boolean webServerEnabled = true;
    public int webPort = 3324;
    public String accessToken = "";
    public int maxLogLines = 200;

    private static JsonConfig<DeveloperToolConfig> file;
    private static volatile int version;

    public DeveloperToolConfig sanitize() {
        this.webPort = Math.max(1, Math.min(65535, this.webPort));
        if (this.accessToken == null || this.accessToken.isBlank()) {
            this.accessToken = UUID.randomUUID().toString().replace("-", "");
        }
        this.maxLogLines = Math.max(20, Math.min(2000, this.maxLogLines));
        return this;
    }

    public DeveloperToolConfig copy() {
        return JsonConfig.GSON.fromJson(JsonConfig.GSON.toJson(this), DeveloperToolConfig.class).sanitize();
    }

    public static synchronized JsonConfig<DeveloperToolConfig> file() {
        if (file == null) {
            file = JsonConfig.of("developertool-client", DeveloperToolConfig.class, DeveloperToolConfig::new);
            file.get().sanitize();
            file.save();
            version++;
        }
        return file;
    }

    public static DeveloperToolConfig get() {
        return file().get().sanitize();
    }

    public static synchronized void reload() {
        file().load();
        file().get().sanitize();
        version++;
    }

    public static synchronized void edit(Consumer<DeveloperToolConfig> change) {
        file().update(c -> {
            change.accept(c);
            c.sanitize();
        });
        version++;
    }

    public static int version() {
        file();
        return version;
    }

    private DeveloperToolConfig() {
    }
}
