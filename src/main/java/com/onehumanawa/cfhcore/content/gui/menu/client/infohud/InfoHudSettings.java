package com.onehumanawa.cfhcore.content.gui.menu.client.infohud;

import com.onehumanawa.cfhcore.content.gui.core.module.Modules;
import com.onehumanawa.cfhcore.content.gui.core.platform.CFHGuiPlatform;
import com.onehumanawa.cfhcore.content.gui.menu.CFHGuiMenu;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public final class InfoHudSettings {
   private static final InfoHudConfig DEFAULTS = new InfoHudConfig();

   private InfoHudSettings() {
   }

   static void init() {
      try {
         if (Modules.isLoaded("cfhgui_config") || CFHGuiPlatform.get().isModLoaded("cfhgui_config")) {
            InfoHudSettingsTab.register();
         }
      } catch (Throwable t) {
         CFHGuiMenu.LOGGER.warn("[CFHGui Menu] the info overlay's tab could not be added to CFHGui Config: {}", t.toString());
      }

   }

   public static List<Group> groups() {
      List<Group> out = new ArrayList();
      List<Setting> overlay = new ArrayList();
      Setting enabled = bool("enabled", (c) -> c.enabled, (c, v) -> c.enabled = v);
      overlay.add(enabled);
      Setting mode = choice("mode", (c) -> c.mode().name(), (c, v) -> c.mode = v, "FULL", "COMPACT");
      mode.structural = true;
      overlay.add(mode);
      overlay.add(choice("corner", (c) -> c.corner().name(), (c, v) -> c.corner = v, "TOP_LEFT", "TOP_RIGHT", "BOTTOM_LEFT", "BOTTOM_RIGHT"));
      Setting scale = new Setting("scale", InfoHudSettings.Type.DOUBLE, (c) -> c.scale, (c, v) -> c.scale = ((Number)v).doubleValue());
      scale.min = (double)0.5F;
      scale.max = (double)2.0F;
      scale.step = (double)0.25F;
      scale.format = (v) -> Component.translatable("cfhgui_infohud.settings.percent", new Object[]{Integer.toString((int)Math.round(((Number)v).doubleValue() * (double)100.0F))});
      overlay.add(scale);
      Setting opacity = new Setting("opacity", InfoHudSettings.Type.INT, (c) -> c.opacity, (c, v) -> c.opacity = ((Number)v).intValue());
      opacity.min = (double)0.0F;
      opacity.max = (double)100.0F;
      opacity.step = (double)5.0F;
      opacity.format = (v) -> Component.translatable("cfhgui_infohud.settings.percent", new Object[]{Integer.toString(((Number)v).intValue())});
      overlay.add(opacity);
      out.add(new Group("overlay", overlay));
      BooleanSupplier full = () -> InfoHudConfig.get().mode() == InfoHudConfig.Mode.FULL;
      List<Setting> place = new ArrayList();
      place.add(bool("position", (c) -> c.position, (c, v) -> c.position = v));
      place.add(when(bool("chunk", (c) -> c.chunk, (c, v) -> c.chunk = v), full));
      place.add(bool("facing", (c) -> c.facing, (c, v) -> c.facing = v));
      place.add(bool("biome", (c) -> c.biome, (c, v) -> c.biome = v));
      place.add(bool("dimension", (c) -> c.dimension, (c, v) -> c.dimension = v));
      place.add(when(bool("light", (c) -> c.light, (c, v) -> c.light = v), full));
      place.add(when(bool("time", (c) -> c.time, (c, v) -> c.time = v), full));
      place.add(when(bool("speed", (c) -> c.speed, (c, v) -> c.speed = v), full));
      out.add(new Group("place", place));
      List<Setting> game = new ArrayList();
      Setting fps = bool("fps", (c) -> c.fps, (c, v) -> c.fps = v);
      fps.structural = true;
      game.add(fps);
      game.add(when(bool("graph", (c) -> c.graph, (c, v) -> c.graph = v), () -> InfoHudConfig.get().fps && full.getAsBoolean()));
      game.add(bool("latency", (c) -> c.latency, (c, v) -> c.latency = v));
      game.add(bool("memory", (c) -> c.memory, (c, v) -> c.memory = v));
      boolean vram = videoMemoryKnown();
      game.add(when(bool("videoMemory", (c) -> c.videoMemory, (c, v) -> c.videoMemory = v), () -> vram));
      if (!vram) {
         Setting note = new Setting("videoMemory.note", InfoHudSettings.Type.NOTE, (c) -> "", (c, v) -> {
         });
         note.text = Component.translatable(VideoMemory.reasonKey());
         game.add(note);
      }

      out.add(new Group("game", game));
      return out;
   }

   private static boolean videoMemoryKnown() {
      try {
         return VideoMemory.known();
      } catch (Throwable var1) {
         return false;
      }
   }

   private static Setting bool(String id, Function<InfoHudConfig, Boolean> get, BiConsumer<InfoHudConfig, Boolean> set) {
      Type var10003 = InfoHudSettings.Type.BOOL;
      Objects.requireNonNull(get);
      return new Setting(id, var10003, get::apply, (c, v) -> set.accept(c, Boolean.TRUE.equals(v)));
   }

   private static Setting choice(String id, Function<InfoHudConfig, String> get, BiConsumer<InfoHudConfig, String> set, String... choices) {
      Type var10003 = InfoHudSettings.Type.CHOICE;
      Objects.requireNonNull(get);
      Setting s = new Setting(id, var10003, get::apply, (c, v) -> set.accept(c, String.valueOf(v)));
      s.choices = List.of(choices);
      return s;
   }

   private static Setting when(Setting s, BooleanSupplier enabled) {
      s.enabled = enabled;
      return s;
   }

   public static enum Type {
      BOOL,
      INT,
      DOUBLE,
      CHOICE,
      NOTE;
   }

   public static final class Setting {
      public final String id;
      public final Type type;
      private final Function<InfoHudConfig, Object> get;
      private final BiConsumer<InfoHudConfig, Object> set;
      public double min;
      public double max;
      public double step;
      public List<String> choices = List.of();
      public @Nullable Function<Object, Component> format;
      private BooleanSupplier enabled = () -> true;
      public boolean structural;
      private @Nullable Component text;

      private Setting(String id, Type type, Function<InfoHudConfig, Object> get, BiConsumer<InfoHudConfig, Object> set) {
         this.id = id;
         this.type = type;
         this.get = get;
         this.set = set;
      }

      public Component label() {
         return (Component)(this.text != null ? this.text : Component.translatable("cfhgui_infohud.settings." + this.id));
      }

      public Component tooltip() {
         return Component.translatable("cfhgui_infohud.settings." + this.id + ".tip");
      }

      public Component choiceLabel(String choice) {
         String var10000 = this.id;
         return Component.translatable("cfhgui_infohud.settings." + var10000 + "." + choice.toLowerCase(Locale.ROOT));
      }

      public Object get() {
         return this.get.apply(InfoHudConfig.get());
      }

      public Object def() {
         return this.get.apply(InfoHudSettings.DEFAULTS);
      }

      public void apply(Object value) {
         this.set.accept(InfoHudConfig.get(), value);
         InfoHudConfig.touch();
      }

      public void set(Object value) {
         this.set.accept(InfoHudConfig.get(), value);
         InfoHudConfig.save();
      }

      public void commit() {
         InfoHudConfig.save();
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
         return Component.translatable("cfhgui_infohud.settings.group." + this.id);
      }

      public Component pageTitle() {
         return Component.translatable("cfhgui_infohud.settings.group." + this.id + ".page");
      }

      public Component about() {
         return Component.translatable("cfhgui_infohud.settings.group." + this.id + ".about");
      }
   }
}
