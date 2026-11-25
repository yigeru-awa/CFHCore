package com.onehumanawa.cfhcore.foundation.developertool;

import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.platform.CFHGuiPlatform;
import com.onehumanawa.cfhcore.foundation.developertool.config.DeveloperToolConfig;
import com.onehumanawa.cfhcore.foundation.developertool.web.DevConsoleLog;
import com.onehumanawa.cfhcore.foundation.developertool.web.DevWebServer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * Developer-only tooling. A localhost HTTP endpoint that executes posted Java
 * snippets. It starts automatically when the client joins a singleplayer world
 * and is never active on a multiplayer server or dedicated server.
 */
public final class DeveloperTool {
    private static boolean initialised;

    public static synchronized void init() {
        if (!initialised) {
            initialised = true;
            DeveloperToolConfig.file();
            ensureDoc();
            CFHGui.LOGGER.info("[DevTool] initialised");
        }
    }

    /** Called on ClientPlayerNetworkEvent.LoggingIn: starts the web server in singleplayer. */
    public static void onJoinWorld() {
        init();
        Minecraft mc = Minecraft.getInstance();
        DeveloperToolConfig config = DeveloperToolConfig.get();
        boolean single = mc.level != null && mc.isSingleplayer();
        if (single && config.webServerEnabled) {
            DevWebServer.start();
            if (mc.player != null) {
                mc.player.sendSystemMessage(Component.literal(
                        "[DevTool] WebServer 已启动 http://localhost:" + config.webPort
                                + " token=" + config.accessToken));
            }
            CFHGui.LOGGER.info("[DevTool] WebServer auto-started on join (port {})", config.webPort);
        } else {
            DevWebServer.stop();
            if (!single) {
                CFHGui.LOGGER.info("[DevTool] WebServer not started (not singleplayer)");
            }
        }
    }

    /** Called every client tick: keeps the server running only while in singleplayer. */
    public static void onClientTick() {
        Minecraft mc = Minecraft.getInstance();
        boolean single = mc.level != null && mc.isSingleplayer();
        DeveloperToolConfig config = DeveloperToolConfig.get();
        if (single && config.webServerEnabled && !DevWebServer.isRunning()) {
            onJoinWorld();
        } else if (!single && DevWebServer.isRunning()) {
            DevWebServer.stop();
        }
    }

    /** Called on ClientPlayerNetworkEvent.LoggingOut: stops the web server. */
    public static void onLeaveWorld() {
        DevWebServer.stop();
        DevConsoleLog.clear();
    }

    /** Writes DeveloperTool.md into config/cfhgui/ on first launch if missing. */
    private static void ensureDoc() {
        try {
            Path dir = CFHGuiPlatform.get().configDir().resolve("cfhgui");
            Files.createDirectories(dir);
            Path doc = dir.resolve("DeveloperTool.md");
            if (!Files.exists(doc)) {
                Files.writeString(doc, DOC, StandardCharsets.UTF_8);
                CFHGui.LOGGER.info("[DevTool] Generated {}", doc);
            }
        } catch (Exception e) {
            CFHGui.LOGGER.warn("[DevTool] Could not write DeveloperTool.md: {}", e.toString());
        }
    }

    private static final String DOC = """
            # Developer Tool — 开发者 WebServer

            CFHCore 内置的单人游戏开发者 WebServer。进入单人世界后自动启动，
            无需打开任何界面或输入命令。联机/服务器中不会启动。

            ## 访问方式

            服务监听 `http://localhost:<端口>`（默认 3324），仅绑定 127.0.0.1。
            进入单人世界时聊天栏会提示当前端口和 AccessToken。

            执行 Java 代码：
            ```bash
            curl -X POST http://localhost:3324/eval \\
              -H "Authorization: Bearer <AccessToken>" \\
              -d 'Minecraft.getInstance().player.sendSystemMessage(net.minecraft.network.chat.Component.literal("hello"))'
            ```

            也支持 `X-Access-Token` 请求头。返回执行结果（标准输出 + 返回值 + 异常）。

            ### 直接执行游戏内命令（run_command）

            增加请求头 `run_command: true`，body 直接作为游戏内命令执行（以当前玩家身份、
            在集成服务器上执行），无需写 Java：
            ```bash
            curl -X POST http://localhost:3324/eval \\
              -H "Authorization: Bearer <AccessToken>" \\
              -H "run_command: true" \\
              -d 'summon minecraft:villager ~ ~ ~'
            ```
            也支持 `/summon ...` 形式（开头的 `/` 会被自动去掉）。

            ### DevHelpers 帮手函数

            在 Java snippet 里可直接调用（无需 import）：
            - `DevHelpers.runCommand("summon minecraft:villager 100 64 100")` — 执行命令
            - `DevHelpers.spawnAtPlayer("minecraft:villager")` — 在玩家处生成实体
            - `DevHelpers.give("minecraft:diamond", 64)` — 给予物品
            - `DevHelpers.say("消息")` — 发送聊天消息
            - `DevHelpers.teleport(x, y, z)` / `setTime(...)` / `setWeather(...)` / `setGamemode(...)`
            - `DevHelpers.playerInfo()` — 玩家信息
            - `DevHelpers.listCommands("筛选")` — 列出命令
            - `DevHelpers.inspect("net.minecraft...")` — 反射查看类方法
            - `DevHelpers.help()` — 列出全部帮手函数

            注意：直接在 snippet 里写 `Minecraft.getInstance()` 这类短类名通常可用
            （已内置常用 import）；若失败，可用 `DevHelpers` 或纯反射。

            ## 配置

            配置文件：`config/cfhgui/developertool-client.json`
            ```json
            {
              "webServerEnabled": true,
              "webPort": 3324,
              "accessToken": "<自动生成>",
              "maxLogLines": 200
            }
            ```

            - `webServerEnabled` — 是否允许启用 WebServer
            - `webPort` — 端口，可修改
            - `accessToken` — 访问令牌，可手动更换（需重启世界生效）
            - `maxLogLines` — 日志缓冲行数

            ## 安全提示

            - 仅监听 localhost，外部机器无法访问
            - AccessToken 仅作为轻量防护，勿泄露
            - 代码在当前 JVM 执行，可访问所有已加载类（含 Minecraft/NeoForge/其他模组），
              请仅在可信单人环境中使用
            - 本工具不会在联机世界或专用服务器中启动
            """;

    private DeveloperTool() {
    }
}
