package com.onehumanawa.cfhcore.content.gui.menu.client.lookat;

import com.onehumanawa.cfhcore.content.gui.core.module.Modules;
import com.onehumanawa.cfhcore.content.gui.core.platform.CFHGuiPlatform;
import com.onehumanawa.cfhcore.content.gui.menu.CFHGuiMenu;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public final class LookAtSettings {
   private static final LookAtConfig DEFAULTS = new LookAtConfig();

   private LookAtSettings() {
   }

   static void init() {
      try {
         if (Modules.isLoaded("cfhgui_config") || CFHGuiPlatform.get().isModLoaded("cfhgui_config")) {
            LookAtSettingsTab.register();
         }
      } catch (Throwable t) {
         CFHGuiMenu.LOGGER.warn("[CFHGui Menu] the look-at plate's tab could not be added to CFHGui Config: {}", t.toString());
      }

   }

   public static List<Group> groups() {
      List<Group> out = new ArrayList();
      BooleanSupplier on = () -> LookAtConfig.get().enabled;
      List<Setting> plate = new ArrayList();
      Setting enabled = bool("enabled", (c) -> c.enabled, (c, v) -> c.enabled = v);
      enabled.structural = true;
      String other = StandBack.owner() == null ? null : StandBack.ownerName();
      if (other != null) {
         enabled.label = Component.translatable("cfhgui_lookat.settings.left_to", new Object[]{Component.translatable("cfhgui_lookat.settings.enabled"), other});
         enabled.tooltip = Component.translatable("cfhgui_lookat.settings.left_to.tip", new Object[]{other});
         enabled.enabled = () -> !LookAtConfig.get().yieldToMods;
      }

      plate.add(enabled);
      Setting leave = bool("yieldToMods", (c) -> c.yieldToMods, (c, v) -> c.yieldToMods = v);
      leave.structural = true;
      plate.add(leave);
      plate.add(when(bool("portrait", (c) -> c.portrait, (c, v) -> c.portrait = v), on));
      out.add(new Group("plate", plate));
      List<Setting> lines = new ArrayList();
      lines.add(when(bool("name", (c) -> c.name, (c, v) -> c.name = v), on));
      lines.add(when(bool("kind", (c) -> c.kind, (c, v) -> c.kind = v), on));
      lines.add(when(bool("health", (c) -> c.health, (c, v) -> c.health = v), on));
      lines.add(when(bool("armor", (c) -> c.armor, (c, v) -> c.armor = v), on));
      lines.add(when(bool("baby", (c) -> c.baby, (c, v) -> c.baby = v), on));
      lines.add(when(bool("owner", (c) -> c.owner, (c, v) -> c.owner = v), on));
      lines.add(when(bool("villager", (c) -> c.villager, (c, v) -> c.villager = v), on));
      lines.add(when(bool("mount", (c) -> c.mount, (c, v) -> c.mount = v), on));
      lines.add(when(bool("effects", (c) -> c.effects, (c, v) -> c.effects = v), on));
      out.add(new Group("lines", lines));
      return out;
   }

   private static Setting bool(String id, Function<LookAtConfig, Boolean> get, BiConsumer<LookAtConfig, Boolean> set) {
      return new Setting(id, get, set);
   }

   private static Setting when(Setting s, BooleanSupplier enabled) {
      s.enabled = enabled;
      return s;
   }

   public static final class Setting {
      public final String id;
      private final Function<LookAtConfig, Boolean> get;
      private final BiConsumer<LookAtConfig, Boolean> set;
      private BooleanSupplier enabled = () -> true;
      public boolean structural;
      private @Nullable Component label;
      private @Nullable Component tooltip;

      private Setting(String id, Function<LookAtConfig, Boolean> get, BiConsumer<LookAtConfig, Boolean> set) {
         this.id = id;
         this.get = get;
         this.set = set;
      }

      public Component label() {
         return (Component)(this.label != null ? this.label : Component.translatable("cfhgui_lookat.settings." + this.id));
      }

      public Component tooltip() {
         return (Component)(this.tooltip != null ? this.tooltip : Component.translatable("cfhgui_lookat.settings." + this.id + ".tip"));
      }

      public boolean get() {
         return (Boolean)this.get.apply(LookAtConfig.get());
      }

      public boolean def() {
         return (Boolean)this.get.apply(LookAtSettings.DEFAULTS);
      }

      public void set(boolean value) {
         this.set.accept(LookAtConfig.get(), value);
         LookAtConfig.save();
      }

      public boolean enabled() {
         return this.enabled.getAsBoolean();
      }

      public BooleanSupplier enabledIf() {
         return this.enabled;
      }
   }

   public static record Group(String id, List<Setting> settings) {
      public Component title() {
         return Component.translatable("cfhgui_lookat.settings.group." + this.id);
      }

      public Component pageTitle() {
         return Component.translatable("cfhgui_lookat.settings.group." + this.id + ".page");
      }

      public Component about() {
         return Component.translatable("cfhgui_lookat.settings.group." + this.id + ".about");
      }
   }
}
