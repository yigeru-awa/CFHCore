package com.onehumanawa.cfhcore.content.gui.core.stage.scene;

import com.onehumanawa.cfhcore.content.gui.core.stage.mesh.StageModels;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public final class SceneCommandHandler {
   public Component capture(String template, String scene, @Nullable BlockPos origin) {
      return SceneCapture.capture(template, scene, origin);
   }

   public Component list() {
      Path dir = SceneLoader.configDir();
      List<String> names = new ArrayList();
      if (Files.isDirectory(dir, new LinkOption[0])) {
         try {
            Stream<Path> s = Files.list(dir);

            try {
               s.filter((p) -> p.getFileName().toString().endsWith(".json")).forEach((p) -> names.add(p.getFileName().toString().replace(".json", "")));
            } catch (Throwable var7) {
               if (s != null) {
                  try {
                     s.close();
                  } catch (Throwable var6) {
                     var7.addSuppressed(var6);
                  }
               }

               throw var7;
            }

            if (s != null) {
               s.close();
            }
         } catch (Exception var8) {
         }
      }

      return names.isEmpty() ? Component.translatable("cfhgui.stage.scenes.none", new Object[]{dir.toString()}) : Component.translatable("cfhgui.stage.scenes.list", new Object[]{names.size(), String.join(", ", names)});
   }

   public Component reload() {
      SceneLoader.clear();
      StageModels.clear();
      return Component.translatable("cfhgui.stage.scenes.reloaded");
   }
}
