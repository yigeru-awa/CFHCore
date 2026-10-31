package com.onehumanawa.cfhcore.content.gui.core.stage;

import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.platform.ModInfo;
import com.onehumanawa.cfhcore.content.gui.core.platform.CFHGuiPlatform;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.transformer.meta.MixinMerged;

public final class StageTrouble {
   private static final boolean DEBUG = Boolean.getBoolean("cfhgui.stage.debug");
   private static final Set<String> SAID = ConcurrentHashMap.newKeySet();
   private static final String[] OURS = new String[]{"java.", "javax.", "jdk.", "sun.", "com.sun.", "net.minecraft.", "com.mojang.", "com.google.", "it.unimi.", "org.joml.", "org.lwjgl.", "org.slf4j.", "org.apache.", "net.neoforged.", "net.minecraftforge.", "cpw.mods.", "net.fabricmc.", "org.spongepowered.", "com.llamalad7.", "com.onehumanawa.cfhcore.content.gui.", "dev.emi.emi."};
   private static final String[] MERGED = new String[]{"handler$", "redirect$", "modify$", "localvar$", "constant$", "args$", "wrapOperation$", "wrapWithCondition$", "modifyExpressionValue$", "modifyReturnValue$", "modifyReceiver$", "wrapMethod$"};

   private StageTrouble() {
   }

   public static void once(String what, String outcome, Throwable cause) {
      Throwable root = root(cause);
      if (SAID.add(what + "|" + outcome + "|" + root.getClass().getName())) {
         String from = culprit(cause);
         String message = "[CFHGui] scene: " + what + " failed" + (from != null ? " in " + from : "") + "; " + outcome + " (" + String.valueOf(root) + ")";
         if (DEBUG) {
            CFHGui.LOGGER.warn(message, cause);
         } else {
            CFHGui.LOGGER.warn(message);
         }

      }
   }

   public static boolean debug() {
      return DEBUG;
   }

   public static Throwable root(Throwable t) {
      Throwable r = t;

      for(int i = 0; i < 16 && r.getCause() != null && r.getCause() != r; ++i) {
         r = r.getCause();
      }

      return r;
   }

   public static @Nullable String culprit(Throwable t) {
      List<Throwable> chain = new ArrayList();

      for(Throwable c = t; c != null && !chain.contains(c) && chain.size() < 16; c = c.getCause()) {
         chain.add(0, c);
      }

      for(Throwable c : chain) {
         for(StackTraceElement f : c.getStackTrace()) {
            if (!ours(f.getClassName())) {
               return named(f);
            }

            String merged = mergedFrom(f);
            if (merged != null) {
               return "a mixin of " + merged;
            }
         }
      }

      return null;
   }

   private static boolean ours(String className) {
      for(String p : OURS) {
         if (className.startsWith(p)) {
            return true;
         }
      }

      return false;
   }

   private static @Nullable String mergedFrom(StackTraceElement f) {
      String method = f.getMethodName();
      boolean merged = false;

      for(String p : MERGED) {
         if (method.startsWith(p)) {
            merged = true;
            break;
         }
      }

      if (!merged) {
         return null;
      } else {
         try {
            Class<?> owner = Class.forName(f.getClassName(), false, StageTrouble.class.getClassLoader());

            for(Method m : owner.getDeclaredMethods()) {
               if (m.getName().equals(method)) {
                  MixinMerged info = (MixinMerged)m.getAnnotation(MixinMerged.class);
                  if (info != null) {
                     return ours(info.mixin()) ? null : info.mixin();
                  }
               }
            }
         } catch (RuntimeException | LinkageError | ReflectiveOperationException var9) {
         }

         String var10000 = f.getClassName();
         return var10000 + "." + method;
      }
   }

   private static String named(StackTraceElement f) {
      String module = f.getModuleName();
      if (module != null && !module.isBlank()) {
         try {
            Optional<ModInfo> info = CFHGuiPlatform.get().modInfo(module);
            if (info.isPresent()) {
               String var10000 = ((ModInfo)info.get()).name();
               return var10000 + " (" + module + ")";
            }
         } catch (RuntimeException var3) {
         }

         return module;
      } else {
         return f.getClassName();
      }
   }
}
