package com.onehumanawa.cfhcore.content.gui.core.client.settings;

import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.screen.Reskin;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiToggle;
import java.util.function.Consumer;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public final class LayoutStyleRows {
   private LayoutStyleRows() {
   }

   public static Component styleLabel() {
      return Component.translatable("cfh.settings.section.look");
   }

   public static AbstractWidget styleSegmented(int x, int y, int width, @Nullable Consumer<Boolean> onChange) {
      return new CFHGuiToggle(x, y, width, styleLabel(), CFHGui.config().reskinContainers, (v) -> {
         applyContainers(v);
         if (onChange != null) {
            onChange.accept(v);
         }

      });
   }

   public static Component containersLabel() {
      return Component.translatable("cfhgui.setup.containers");
   }

   public static CFHGuiToggle containersToggle(int x, int y, int width, @Nullable Consumer<Boolean> onChange) {
      return new CFHGuiToggle(x, y, width, containersLabel(), CFHGui.config().reskinContainers, (v) -> {
         applyContainers(v);
         if (onChange != null) {
            onChange.accept(v);
         }

      });
   }

   public static void applyContainers(boolean on) {
      CFHGui.configFile().update((c) -> c.reskinContainers = on);
      Theme.reload();
      Reskin.invalidate();
   }
}
