package com.onehumanawa.cfhcore.foundation.developertool.eval;

import com.mojang.brigadier.tree.CommandNode;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Developer helper functions exposed to the WebServer. Every method is compiled
 * with real Minecraft types (type-safe) and runs on the calling thread, which
 * the WebServer guarantees is the game client thread.
 *
 * Snippets can call these by fully-qualified name or simple reflection; HTTP
 * clients can trigger {@link #runCommand(String)} directly via the
 * {@code run_command: true} header.
 */
public final class DevHelpers {

    /** Executes a vanilla/mod command on the integrated server, sourced at the player. */
    public static String runCommand(String command) {
        Minecraft mc = Minecraft.getInstance();
        MinecraftServer server = mc.getSingleplayerServer();
        if (server == null) {
            return "错误: 当前不在单人世界（无集成服务器）";
        }
        if (mc.player == null) {
            return "错误: 没有本地玩家";
        }
        ServerPlayer sender = server.getPlayerList().getPlayer(mc.player.getUUID());
        CommandSourceStack src = server.createCommandSourceStack().withPermission(4);
        if (sender != null) {
            src = src.withEntity(sender)
                    .withPosition(sender.position())
                    .withRotation(sender.getRotationVector())
                    .withLevel(sender.serverLevel());
        } else {
            src = src.withPosition(mc.player.position());
        }
        String cmd = command.startsWith("/") ? command.substring(1) : command;
        final CommandSourceStack source = src;
        if (server.isSameThread()) {
            doRun(server, source, cmd);
            return "已执行 /" + cmd;
        }
        // Must run on the server thread (entity spawning etc. are thread-sensitive).
        final String[] result = new String[1];
        final Throwable[] error = new Throwable[1];
        server.execute(() -> {
            try {
                doRun(server, source, cmd);
                result[0] = "已执行 /" + cmd;
            } catch (Throwable t) {
                error[0] = t;
            }
        });
        for (int i = 0; i < 200 && result[0] == null && error[0] == null; i++) {
            try {
                Thread.sleep(25);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        if (error[0] != null) {
            return "执行异常: " + error[0];
        }
        return result[0] == null ? "执行超时" : result[0];
    }

    private static void doRun(MinecraftServer server, CommandSourceStack src, String cmd) {
        server.getCommands().performPrefixedCommand(src, cmd);
    }

    /** Sends a chat message to the local player. */
    public static String say(String message) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return "错误: 没有玩家";
        }
        mc.player.sendSystemMessage(Component.literal(message));
        return "已发送聊天消息: " + message;
    }

    /** Summons an entity at explicit coordinates. */
    public static String spawn(String id, double x, double y, double z) {
        return runCommand("summon " + id + " " + x + " " + y + " " + z);
    }

    /** Summons an entity 2 blocks in front of the player (safe offset, no clipping). */
    public static String spawnAtPlayer(String id) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return "错误: 没有玩家";
        }
        net.minecraft.world.phys.Vec3 look = mc.player.getLookAngle();
        double x = mc.player.getX() + look.x * 2.0;
        double y = mc.player.getY();
        double z = mc.player.getZ() + look.z * 2.0;
        return runCommand("summon " + id + " " + x + " " + y + " " + z);
    }

    /** Gives an item stack to the player. */
    public static String give(String item, int count) {
        return runCommand("give @s " + item + " " + Math.max(1, count));
    }

    /** Teleports the player to coordinates. */
    public static String teleport(double x, double y, double z) {
        return runCommand("tp @s " + x + " " + y + " " + z);
    }

    /** Sets the world time (e.g. day, night, 1000). */
    public static String setTime(String value) {
        return runCommand("time set " + value);
    }

    /** Sets the weather (clear, rain, thunder). */
    public static String setWeather(String value) {
        return runCommand("weather " + value);
    }

    /** Changes the player's gamemode (survival, creative, spectator, adventure). */
    public static String setGamemode(String mode) {
        return runCommand("gamemode " + mode);
    }

    /** Returns a short status string about the local player/world. */
    public static String playerInfo() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return "错误: 不在世界中";
        }
        return "玩家=" + mc.player.getName().getString()
                + " UUID=" + mc.player.getUUID()
                + " 坐标=" + mc.player.getX() + "," + mc.player.getY() + "," + mc.player.getZ()
                + " 维度=" + mc.level.dimension().location()
                + " 单人=" + mc.isSingleplayer();
    }

    /** Lists root commands, optionally filtered by a substring. */
    public static String listCommands(String filter) {
        Minecraft mc = Minecraft.getInstance();
        MinecraftServer server = mc.getSingleplayerServer();
        if (server == null) {
            return "错误: 不在单人世界";
        }
        String f = filter == null ? "" : filter.toLowerCase(Locale.ROOT);
        List<String> names = new ArrayList<>();
        try {
            // Commands -> CommandDispatcher -> root -> children (reflective to stay
            // compatible across MC builds without hard-coding the accessor name).
            Object commands = server.getCommands();
            java.lang.reflect.Method dispatcherMethod = null;
            for (java.lang.reflect.Method m : commands.getClass().getMethods()) {
                if ((m.getName().equals("getDispatcher") || m.getName().equals("getCommandDispatcher"))
                        && m.getParameterCount() == 0) {
                    dispatcherMethod = m;
                    break;
                }
            }
            if (dispatcherMethod == null) {
                return "无法找到命令分发器访问器";
            }
            Object dispatcher = dispatcherMethod.invoke(commands);
            Object root = dispatcher.getClass().getMethod("getRoot").invoke(dispatcher);
            @SuppressWarnings("unchecked")
            Iterable<CommandNode<CommandSourceStack>> children =
                    (Iterable<CommandNode<CommandSourceStack>>) root.getClass().getMethod("getChildren").invoke(root);
            for (CommandNode<CommandSourceStack> node : children) {
                if (f.isEmpty() || node.getName().toLowerCase(Locale.ROOT).contains(f)) {
                    names.add(node.getName());
                }
            }
        } catch (Throwable t) {
            return "列出命令失败: " + t;
        }
        Collections.sort(names);
        return "匹配命令(" + names.size() + "): " + String.join(", ", names);
    }

    /** Reflectively prints the public methods of a class — handy for API discovery. */
    public static String inspect(String className) {
        try {
            Class<?> c = Class.forName(className, false, DevHelpers.class.getClassLoader());
            StringBuilder sb = new StringBuilder("类 " + className + " 的方法:\n");
            for (Method m : c.getMethods()) {
                sb.append("  ").append(m.getReturnType().getSimpleName()).append(' ')
                        .append(m.getName()).append('(');
                Class<?>[] ps = m.getParameterTypes();
                for (int i = 0; i < ps.length; i++) {
                    if (i > 0) {
                        sb.append(", ");
                    }
                    sb.append(ps[i].getSimpleName());
                }
                sb.append(")\n");
            }
            return sb.toString();
        } catch (Throwable t) {
            return "无法加载类 " + className + ": " + t;
        }
    }

    /** Lists the available helper functions. */
    public static String help() {
        return String.join("\n",
                "DevHelpers 帮手函数:",
                "  runCommand(String)       执行游戏内命令，source 为当前玩家",
                "  say(String)              发送聊天消息",
                "  spawn(id,x,y,z)          在指定坐标生成实体",
                "  spawnAtPlayer(id)        在玩家位置生成实体",
                "  give(item,count)         给予物品",
                "  teleport(x,y,z)          传送玩家",
                "  setTime / setWeather / setGamemode(String)",
                "  playerInfo()             玩家信息",
                "  listCommands(filter)     列出命令",
                "  inspect(className)       反射打印类方法",
                "HTTP: 请求头 run_command: true 时，body 直接作为命令执行");
    }

    private DevHelpers() {
    }
}
