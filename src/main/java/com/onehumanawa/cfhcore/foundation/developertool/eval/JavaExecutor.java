package com.onehumanawa.cfhcore.foundation.developertool.eval;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import jdk.jshell.JShell;
import jdk.jshell.Snippet;
import jdk.jshell.SnippetEvent;

/**
 * Executes arbitrary Java snippets through the JDK's built-in JShell, using the
 * LOCAL execution engine so snippets run inside this JVM and can see every
 * already-loaded class (Minecraft, NeoForge, CFHCore, other mods).
 *
 * Only intended for the singleplayer developer console. Never call from a
 * dedicated server or a public multiplayer session.
 */
public final class JavaExecutor {
    private static final JavaExecutor INSTANCE = new JavaExecutor();

    private final JShell shell;
    private final Object lock = new Object();

    private JavaExecutor() {
        PrintStream sink = new PrintStream(new ByteArrayOutputStream());
        // JShell's local execution engine captures the current thread's context
        // class loader when built. It must be the game/mod loader so snippets can
        // see Minecraft, NeoForge and other mod classes at runtime.
        Thread thread = Thread.currentThread();
        ClassLoader oldLoader = thread.getContextClassLoader();
        // The class loader that defined JavaExecutor is the mod loader, which can
        // see Minecraft/NeoForge/other mods. Force it as the context loader so the
        // local execution engine captures it for snippet compilation + execution.
        thread.setContextClassLoader(JavaExecutor.class.getClassLoader());
        try {
            this.shell = JShell.builder()
                    .executionEngine("local")
                    .out(sink)
                    .err(sink)
                    .build();
        } finally {
            thread.setContextClassLoader(oldLoader);
        }
    }

    public static JavaExecutor get() {
        return INSTANCE;
    }

    /** Runs {@code code} on the calling thread (the caller must be on the game thread). */
    public String execute(String code) {
        if (code == null || code.isBlank()) {
            return "";
        }
        synchronized (this.lock) {
            PrintStream oldOut = System.out;
            PrintStream oldErr = System.err;
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            PrintStream capture = new PrintStream(buffer, true, StandardCharsets.UTF_8);
            StringBuilder result = new StringBuilder();
            Thread thread = Thread.currentThread();
            ClassLoader oldLoader = thread.getContextClassLoader();
            try {
                System.setOut(capture);
                System.setErr(capture);
                thread.setContextClassLoader(JavaExecutor.class.getClassLoader());
                // Do NOT inject imports: JShell's compile-time classpath cannot see
                // Minecraft classes, so `import net.minecraft...` would REJECT every
                // snippet. Plain code + reflection (or DevHelpers) work reliably.
                List<SnippetEvent> events = this.shell.eval(code);
                boolean rejected = false;
                for (SnippetEvent event : events) {
                    if (event.exception() != null) {
                        result.append("异常: ").append(describe(event.exception())).append('\n');
                    } else if (event.status() == Snippet.Status.REJECTED) {
                        rejected = true;
                    } else if (event.value() != null && !"null".equals(event.value())) {
                        result.append(event.value()).append('\n');
                    }
                }
                if (rejected && result.length() == 0) {
                    result.append("代码被拒绝（语法错误）。若引用了游戏类仍失败，请改用反射或 DevHelpers 帮手函数。\n");
                }
            } catch (Throwable t) {
                result.append(describe(t)).append('\n');
            } finally {
                thread.setContextClassLoader(oldLoader);
                System.setOut(oldOut);
                System.setErr(oldErr);
                capture.flush();
            }
            String printed = buffer.toString(StandardCharsets.UTF_8);
            if (!printed.isEmpty()) {
                result.append(printed);
            }
            String text = result.toString();
            return text.isEmpty() ? "(执行完毕，无输出)" : text;
        }
    }

    private static String describe(Object o) {
        if (o == null) {
            return "";
        }
        String s = o.toString();
        return s.length() > 4000 ? s.substring(0, 4000) + "…" : s;
    }
}
