package com.onehumanawa.cfhcore.content.gui.config.api;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.core.screen.SidebarScreen;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class DeveloperSections {
   private static final Map<String, Section> SECTIONS = new LinkedHashMap();

   public static synchronized void register(Section section) {
      SECTIONS.put(section.id(), section);
   }

   public static synchronized List<Section> sections() {
      List<Section> out = new ArrayList(SECTIONS.values());
      out.sort(Comparator.comparingInt(Section::order));
      return out;
   }

   private DeveloperSections() {
   }

   public interface Panel {
      void build(SidebarScreen var1, Rect var2);

      default void render(SidebarScreen screen, GuiGraphics g, Rect area, int mouseX, int mouseY, float partialTick) {
      }

      default void tick() {
      }

      default void hidden() {
      }
   }

   public static record Section(String id, Component title, Icon icon, int order, Panel panel) {
   }
}
