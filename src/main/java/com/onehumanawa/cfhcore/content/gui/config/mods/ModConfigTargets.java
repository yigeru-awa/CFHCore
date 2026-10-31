package com.onehumanawa.cfhcore.content.gui.config.mods;

import com.onehumanawa.cfhcore.content.gui.config.ConfigPlatform;
import com.onehumanawa.cfhcore.content.gui.config.CFHGuiConfig;
import com.onehumanawa.cfhcore.content.gui.config.doc.ConfigFiles;
import com.onehumanawa.cfhcore.content.gui.config.doc.Documents;
import com.onehumanawa.cfhcore.content.gui.config.doc.FileDocument;
import com.onehumanawa.cfhcore.content.gui.config.editor.FileEditorScreen;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.platform.CFHGuiPlatform;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class ModConfigTargets {
   public static List<Target> forMod(String modId) {
      return find(modId).targets();
   }

   public static Found find(String modId) {
      List<Target> out = new ArrayList();
      List<Path> files = new ArrayList();
      Minecraft mc = Minecraft.getInstance();
      Optional<Function<Screen, Screen>> own = CFHGuiPlatform.get().otherModConfigScreen(modId);
      own.ifPresent((f) -> out.add(new Target(Component.translatable("cfhgui_config.mods.own_screen"), Icon.EXTERNAL, () -> {
            Screen parent = mc.screen;

            try {
               Screen s = (Screen)f.apply(parent);
               if (s != null) {
                  mc.setScreen(s);
               }
            } catch (Exception e) {
               CFHGuiConfig.LOGGER.warn("[CFHGui Config] {}'s config screen failed: {}", modId, e.toString());
            }

         })));
      Set<String> covered = new HashSet();

      for(FileDocument doc : ConfigPlatform.get().nativeConfigs(modId)) {
         covered.add(key(doc.path()));
         files.add(doc.path());
         out.add(new Target(Component.literal(doc.title().getString()), Icon.SLIDERS, () -> mc.setScreen(new FileEditorScreen(mc.screen, doc))));
      }

      for(Path p : ConfigFiles.forMod(modId)) {
         if (!covered.contains(key(p))) {
            files.add(p);
            out.add(new Target(Component.literal(p.getFileName().toString()), Icon.CODE, () -> Documents.open(p, modId).ifPresent((doc) -> mc.setScreen(new FileEditorScreen(mc.screen, doc)))));
         }
      }

      return new Found(out, files);
   }

   public static long lastModified(List<Path> files) {
      long newest = 0L;

      for(Path p : files) {
         try {
            newest = Math.max(newest, Files.getLastModifiedTime(p).toMillis());
         } catch (RuntimeException | IOException var6) {
         }
      }

      return newest;
   }

   private static String key(Path p) {
      try {
         return p.toAbsolutePath().normalize().toString();
      } catch (Exception var2) {
         return String.valueOf(p);
      }
   }

   private ModConfigTargets() {
   }

   public static record Target(Component label, Icon icon, Runnable open) {
   }

   public static record Found(List<Target> targets, List<Path> files) {
   }
}
