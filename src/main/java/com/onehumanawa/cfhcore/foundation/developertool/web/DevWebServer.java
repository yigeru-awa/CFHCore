package com.onehumanawa.cfhcore.foundation.developertool.web;

import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.foundation.developertool.config.DeveloperToolConfig;
import com.onehumanawa.cfhcore.foundation.developertool.eval.DevHelpers;
import com.onehumanawa.cfhcore.foundation.developertool.eval.JavaExecutor;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

/**
 * Tiny HTTP server bound to localhost only. Requires an access token on every
 * POST and executes the posted body as a Java snippet on the game client thread.
 */
public final class DevWebServer {
    private static HttpServer server;

    private DevWebServer() {
    }

    public static synchronized void start() {
        stop();
        DeveloperToolConfig config = DeveloperToolConfig.get();
        if (!config.webServerEnabled) {
            CFHGui.LOGGER.info("[DevTool] Web server disabled by config");
            return;
        }
        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", config.webPort), 0);
            server.createContext("/eval", DevWebServer::handleEval);
            server.setExecutor(Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "CFH-DevTool-Web");
                t.setDaemon(true);
                return t;
            }));
            server.start();
            CFHGui.LOGGER.info("[DevTool] Web server listening on http://localhost:{}  token={}",
                    config.webPort, config.accessToken);
        } catch (Exception e) {
            CFHGui.LOGGER.error("[DevTool] Failed to start web server", e);
            server = null;
        }
    }

    public static synchronized void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
            CFHGui.LOGGER.info("[DevTool] Web server stopped");
        }
    }

    public static boolean isRunning() {
        return server != null;
    }

    private static void handleEval(HttpExchange exchange) throws IOException {
        try {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                respond(exchange, 405, "Use POST");
                return;
            }
            String token = exchange.getRequestHeaders().getFirst("Authorization");
            if (token == null) {
                token = exchange.getRequestHeaders().getFirst("X-Access-Token");
            }
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            DeveloperToolConfig config = DeveloperToolConfig.get();
            if (token == null || !constantTimeEquals(token.trim(), config.accessToken)) {
                respond(exchange, 401, "Unauthorized: invalid access token");
                return;
            }
            String body = readAll(exchange.getRequestBody());
            final String code = body;
            String runCommand = exchange.getRequestHeaders().getFirst("run_command");
            final boolean isCommand = runCommand != null && Boolean.parseBoolean(runCommand.trim());
            final String[] result = new String[1];
            if (isCommand) {
                // Server-side commands (especially entity spawning) MUST run on the
                // integrated server thread, not the client render thread.
                net.minecraft.server.MinecraftServer server =
                        net.minecraft.client.Minecraft.getInstance().getSingleplayerServer();
                if (server == null) {
                    respond(exchange, 400, "错误: 当前不在单人世界（无集成服务器）");
                    return;
                }
                server.execute(() -> {
                    String out;
                    try {
                        out = DevHelpers.runCommand(code);
                    } catch (Throwable t) {
                        out = "异常: " + t;
                    }
                    result[0] = out;
                    DevConsoleLog.log("> [cmd] " + code);
                    DevConsoleLog.log(result[0]);
                });
            } else {
                // Java snippets run on the client game thread.
                net.minecraft.client.Minecraft.getInstance().execute(() -> {
                    String out;
                    try {
                        out = JavaExecutor.get().execute(code);
                    } catch (Throwable t) {
                        out = "异常: " + t;
                    }
                    result[0] = out;
                    DevConsoleLog.log("> " + code);
                    DevConsoleLog.log(result[0]);
                });
            }
            // Wait for the queued task (bounded wait to avoid hanging the HTTP thread).
            for (int i = 0; i < 200 && result[0] == null; i++) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            respond(exchange, 200, result[0] == null ? "执行超时" : result[0]);
        } catch (Throwable t) {
            CFHGui.LOGGER.error("[DevTool] Web request failed", t);
            respond(exchange, 500, "执行失败: " + t);
        } finally {
            exchange.close();
        }
    }

    private static String readAll(InputStream in) throws IOException {
        return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }

    private static void respond(HttpExchange exchange, int status, String text) throws IOException {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) {
            return false;
        }
        int diff = 0;
        for (int i = 0; i < a.length(); i++) {
            diff |= a.charAt(i) ^ b.charAt(i);
        }
        return diff == 0;
    }
}
