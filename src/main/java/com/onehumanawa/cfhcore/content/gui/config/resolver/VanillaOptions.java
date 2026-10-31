package com.onehumanawa.cfhcore.content.gui.config.resolver;

import com.onehumanawa.cfhcore.content.gui.config.option.Binding;
import com.onehumanawa.cfhcore.content.gui.config.option.Choice;
import com.onehumanawa.cfhcore.content.gui.config.option.NumberRange;
import com.onehumanawa.cfhcore.content.gui.config.option.OptionBinding;
import com.onehumanawa.cfhcore.content.gui.config.option.OptionType;
import com.onehumanawa.cfhcore.content.gui.config.option.OptionValues;
import com.onehumanawa.cfhcore.content.gui.config.ui.ApplyQueue;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.DoubleFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.NarratorStatus;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.OptionEnum;
import org.jetbrains.annotations.Nullable;

public final class VanillaOptions {
   private static final Map<String, Supplier<Binding>> DEFS = new LinkedHashMap();
   private static final Map<String, OptionBinding> CACHE = new LinkedHashMap();
   public static final Set<String> COVERED_BY_SODIUM = Set.of("renderDistance", "simulationDistance", "gamma", "guiScale", "fullscreen", "enableVsync", "maxFps", "bobView", "attackIndicator", "showAutosaveIndicator", "graphicsMode", "renderClouds", "particles", "ao", "biomeBlendRadius", "entityShadows", "entityDistanceScaling", "mipmapLevels");
   public static final Set<String> ADVANCED = Set.of("simulationDistance", "biomeBlendRadius", "entityDistanceScaling", "mipmapLevels", "prioritizeChunkUpdates", "screenEffectScale", "fovEffectScale", "darknessEffectScale", "damageTiltStrength", "glintSpeed", "glintStrength", "showAutosaveIndicator", "menuBackgroundBlurriness", "rawMouseInput", "discrete_mouse_scroll", "mouseWheelSensitivity", "touchscreen", "allowServerListing", "telemetryOptInExtra", "chatDelay", "chatLineSpacing", "hideMatchedNames", "onlyShowSecureChat", "autoSuggestions", "reducedDebugInfo", "panoramaScrollSpeed", "darkMojangStudiosBackground", "hideSplashTexts", "notificationDisplayTime");
   private static @Nullable Integer pendingGuiScale;

   private static Options opts() {
      return Minecraft.getInstance().options;
   }

   public static Optional<OptionBinding> get(String key) {
      OptionBinding c = (OptionBinding)CACHE.get(key);
      if (c != null) {
         return Optional.of(c);
      } else {
         Supplier<Binding> s = (Supplier)DEFS.get(key);
         if (s == null) {
            return Optional.empty();
         } else {
            Binding b = (Binding)s.get();
            CACHE.put(key, b);
            return Optional.of(b);
         }
      }
   }

   public static List<OptionBinding> all(String... keys) {
      List<OptionBinding> out = new ArrayList();

      for(String k : keys) {
         Optional<OptionBinding> var10000 = get(k);
         Objects.requireNonNull(out);
         var10000.ifPresent(out::add);
      }

      return out;
   }

   public static Set<String> keys() {
      return DEFS.keySet();
   }

   private static <T> void apply(OptionInstance<T> inst, T value) {
      inst.set(value);
      opts().save();
   }

   private static void applyGuiScale() {
      Integer chosen = pendingGuiScale;
      pendingGuiScale = null;
      if (chosen != null) {
         apply(opts().guiScale(), chosen);
      }

   }

   private static @Nullable Component tip(String key, String caption) {
      if (I18n.exists(caption + ".tooltip")) {
         return Component.translatable(caption + ".tooltip");
      } else {
         String own = "cfhgui_config.opt." + key + ".tip";
         return I18n.exists(own) ? Component.translatable(own) : null;
      }
   }

   private static Binding base(String key, String caption, OptionType type) {
      return Binding.of("optionsTxt:" + key, type, (Component)Component.translatable(caption)).tooltip(tip(key, caption)).searchWords("vanilla " + key).advanced(ADVANCED.contains(key));
   }

   private static void bool(String key, String caption, Function<Options, OptionInstance<Boolean>> f, boolean def) {
      DEFS.put(key, () -> base(key, caption, OptionType.BOOLEAN).getter(() -> ((OptionInstance)f.apply(opts())).get()).setter((v) -> apply((OptionInstance)f.apply(opts()), OptionValues.asBoolean(v, def))).def(def));
   }

   private static void intRange(String key, String caption, Function<Options, OptionInstance<Integer>> f, int min, int max, int def, DoubleFunction<Component> fmt, @Nullable Runnable after) {
      intRange(key, caption, f, min, max, 1, def, fmt, after);
   }

   private static void intRange(String key, String caption, Function<Options, OptionInstance<Integer>> f, int min, int max, int step, int def, DoubleFunction<Component> fmt, @Nullable Runnable after) {
      DEFS.put(key, () -> base(key, caption, OptionType.INT).getter(() -> ((OptionInstance)f.apply(opts())).get()).setter((v) -> {
            apply((OptionInstance)f.apply(opts()), OptionValues.asInt(v, def));
            if (after != null) {
               after.run();
            }

         }).def(def).range(NumberRange.of((double)min, (double)max, (double)step)).format((v) -> (Component)fmt.apply(OptionValues.asDouble(v, (double)def))));
   }

   private static void dbl(String key, String caption, Function<Options, OptionInstance<Double>> f, double min, double max, double step, double def, DoubleFunction<Component> fmt) {
      DEFS.put(key, () -> base(key, caption, OptionType.DOUBLE).getter(() -> ((OptionInstance)f.apply(opts())).get()).setter((v) -> apply((OptionInstance)f.apply(opts()), OptionValues.asDouble(v, def))).def(def).range(NumberRange.of(min, max, step)).format((v) -> (Component)fmt.apply(OptionValues.asDouble(v, def))));
   }

   private static void unit(String key, String caption, Function<Options, OptionInstance<Double>> f, double def, DoubleFunction<Component> fmt) {
      dbl(key, caption, f, (double)0.0F, (double)1.0F, 0.01, def, fmt);
   }

   private static void choice(String key, String caption, Function<Options, OptionInstance<?>> f, String def) {
      DEFS.put(key, () -> {
         OptionInstance inst = (OptionInstance)f.apply(opts());
         List<Object> values = enumValues(inst);
         List<Choice> choices = new ArrayList();

         for(Object v : values) {
            choices.add(new Choice(choiceId(v), choiceLabel(v)));
         }

         return base(key, caption, OptionType.CHOICE).choices(choices).getter(() -> choiceId(((OptionInstance)f.apply(opts())).get())).setter((vx) -> {
            String id = OptionValues.asString(vx);
            OptionInstance live = (OptionInstance)f.apply(opts());

            for(Object o : enumValues(live)) {
               if (choiceId(o).equals(id)) {
                  apply(live, o);
                  return;
               }
            }

         }).def(def);
      });
   }

   private static List<Object> enumValues(OptionInstance<?> inst) {
      Object vs = inst.values();
      List<Object> out = new ArrayList();
      if (vs instanceof OptionInstance.Enum<?> e) {
         out.addAll(e.values());
      } else if (vs instanceof OptionInstance.AltEnum<?> e) {
         out.addAll(e.values());
      } else if (vs instanceof OptionInstance.LazyEnum<?> e) {
         out.addAll((Collection)e.values().get());
      } else if (vs instanceof OptionInstance.ClampingLazyMaxIntRange r) {
         for(int i = r.minInclusive(); i <= Math.min(r.maxInclusive(), 64); ++i) {
            out.add(i);
         }
      }

      return out;
   }

   private static String choiceId(Object v) {
      if (v instanceof Enum<?> e) {
         return e.name();
      } else {
         return String.valueOf(v);
      }
   }

   private static Component choiceLabel(Object v) {
      if (v instanceof OptionEnum oe) {
         return oe.getCaption();
      } else if (v instanceof NarratorStatus n) {
         return n.getName();
      } else if (v instanceof Boolean b) {
         return Component.translatable(b ? "options.on" : "options.off");
      } else if (v instanceof String s) {
         return s.isEmpty() ? Component.translatable("options.audioDevice.default") : Component.literal(s);
      } else {
         return Component.literal(String.valueOf(v));
      }
   }

   private static Component percent(double v) {
      return Component.literal(Math.round(v * (double)100.0F) + "%");
   }

   private static Component chunks(double v) {
      return Component.translatable("options.chunks", new Object[]{(int)Math.round(v)});
   }

   private static Component num(double v) {
      return Component.literal(Long.toString(Math.round(v)));
   }

   private static Component off(double v, Component on) {
      return (Component)(v <= (double)0.0F ? Component.translatable("options.off") : on);
   }

   private VanillaOptions() {
   }

   static {
      Minecraft mc = Minecraft.getInstance();
      intRange("renderDistance", "options.renderDistance", Options::renderDistance, 2, 32, 12, VanillaOptions::chunks, (Runnable)null);
      intRange("simulationDistance", "options.simulationDistance", Options::simulationDistance, 5, 32, 12, VanillaOptions::chunks, (Runnable)null);
      choice("graphicsMode", "options.graphics", Options::graphicsMode, "FANCY");
      intRange("fov", "options.fov", Options::fov, 30, 110, 70, (v) -> (Component)(v == (double)70.0F ? Component.translatable("options.fov.min") : (v == (double)110.0F ? Component.translatable("options.fov.max") : num(v))), (Runnable)null);
      DEFS.put("guiScale", () -> base("guiScale", "options.guiScale", OptionType.INT).getter(() -> pendingGuiScale != null ? pendingGuiScale : (Integer)opts().guiScale().get()).setter((v) -> {
            pendingGuiScale = OptionValues.asInt(v, 0);
            ApplyQueue.afterDrag("guiScale", 300, VanillaOptions::applyGuiScale);
         }).def(0).range(NumberRange.of((double)0.0F, (double)8.0F, (double)1.0F)).format((v) -> (Component)(OptionValues.asDouble(v, (double)0.0F) <= (double)0.0F ? Component.translatable("options.guiScale.auto") : num(OptionValues.asDouble(v, (double)0.0F)))));
      bool("fullscreen", "options.fullscreen", Options::fullscreen, false);
      bool("enableVsync", "options.vsync", Options::enableVsync, true);
      intRange("maxFps", "options.framerateLimit", Options::framerateLimit, 10, 260, 10, 120, (v) -> v >= (double)260.0F ? Component.translatable("options.framerateLimit.max") : Component.translatable("options.framerate", new Object[]{(int)Math.round(v)}), (Runnable)null);
      intRange("mipmapLevels", "options.mipmapLevels", Options::mipmapLevels, 0, 4, 4, (v) -> off(v, num(v)), () -> ApplyQueue.later("mipmap", 600, () -> {
            mc.updateMaxMipLevel((Integer)opts().mipmapLevels().get());
            mc.delayTextureReload();
         }));
      choice("particles", "options.particles", Options::particles, "ALL");
      dbl("entityDistanceScaling", "options.entityDistanceScaling", Options::entityDistanceScaling, (double)0.5F, (double)5.0F, (double)0.25F, (double)1.0F, VanillaOptions::percent);
      bool("entityShadows", "options.entityShadows", Options::entityShadows, true);
      intRange("biomeBlendRadius", "options.biomeBlendRadius", Options::biomeBlendRadius, 0, 7, 2, (v) -> off(v, Component.literal((int)(v * (double)2.0F + (double)1.0F) + "x" + (int)(v * (double)2.0F + (double)1.0F))), (Runnable)null);
      choice("prioritizeChunkUpdates", "options.prioritizeChunkUpdates", Options::prioritizeChunkUpdates, "NONE");
      unit("gamma", "options.gamma", Options::gamma, (double)0.5F, (v) -> (Component)(v <= (double)0.0F ? Component.translatable("options.gamma.min") : (v >= (double)1.0F ? Component.translatable("options.gamma.max") : percent(v))));
      choice("renderClouds", "options.renderClouds", Options::cloudStatus, "FANCY");
      bool("ao", "options.ao", Options::ambientOcclusion, true);
      unit("glintSpeed", "options.glintSpeed", Options::glintSpeed, (double)0.5F, VanillaOptions::percent);
      unit("glintStrength", "options.glintStrength", Options::glintStrength, (double)0.75F, VanillaOptions::percent);
      unit("screenEffectScale", "options.screenEffectScale", Options::screenEffectScale, (double)1.0F, VanillaOptions::percent);
      unit("fovEffectScale", "options.fovEffectScale", Options::fovEffectScale, (double)1.0F, VanillaOptions::percent);
      unit("darknessEffectScale", "options.darknessEffectScale", Options::darknessEffectScale, (double)1.0F, VanillaOptions::percent);
      unit("damageTiltStrength", "options.damageTiltStrength", Options::damageTiltStrength, (double)1.0F, VanillaOptions::percent);
      bool("hideLightningFlashes", "options.hideLightningFlashes", Options::hideLightningFlash, false);
      intRange("menuBackgroundBlurriness", "options.accessibility.menu_background_blurriness", Options::menuBackgroundBlurriness, 0, 10, 5, (v) -> off(v, num(v)), (Runnable)null);
      bool("bobView", "options.viewBobbing", Options::bobView, true);
      choice("attackIndicator", "options.attackIndicator", Options::attackIndicator, "CROSSHAIR");
      bool("showAutosaveIndicator", "options.autosaveIndicator", Options::showAutosaveIndicator, true);
      unit("mouseSensitivity", "options.sensitivity", Options::sensitivity, (double)0.5F, (v) -> (Component)(v <= (double)0.0F ? Component.translatable("options.sensitivity.min") : (v >= (double)1.0F ? Component.translatable("options.sensitivity.max") : percent(v * (double)2.0F))));
      bool("invertYMouse", "options.invertMouse", Options::invertYMouse, false);
      bool("rawMouseInput", "options.rawMouseInput", Options::rawMouseInput, true);
      bool("discrete_mouse_scroll", "options.discrete_mouse_scroll", Options::discreteMouseScroll, false);
      dbl("mouseWheelSensitivity", "options.mouseWheelSensitivity", Options::mouseWheelSensitivity, 0.01, (double)10.0F, 0.01, (double)1.0F, (v) -> Component.literal(String.format(Locale.ROOT, "%.2f", v)));
      bool("touchscreen", "options.touchscreen", Options::touchscreen, false);
      bool("toggleSprint", "cfhgui_config.opt.toggleSprint", Options::toggleSprint, false);
      bool("toggleCrouch", "cfhgui_config.opt.toggleCrouch", Options::toggleCrouch, false);
      bool("autoJump", "options.autoJump", Options::autoJump, false);
      bool("operatorItemsTab", "options.operatorItemsTab", Options::operatorItemsTab, false);

      for(SoundSource src : SoundSource.values()) {
         String key = "soundCategory_" + src.getName();
         unit(key, "soundCategory." + src.getName(), (o) -> o.getSoundSourceOptionInstance(src), (double)1.0F, (v) -> (Component)(v <= (double)0.0F ? Component.translatable("options.off") : percent(v)));
      }

      bool("showSubtitles", "options.showSubtitles", Options::showSubtitles, false);
      bool("directionalAudio", "options.directionalAudio", Options::directionalAudio, false);
      choice("soundDevice", "options.audioDevice", Options::soundDevice, "");
      choice("chatVisibility", "options.chat.visibility", Options::chatVisibility, "FULL");
      bool("chatColors", "options.chat.color", Options::chatColors, true);
      bool("chatLinks", "options.chat.links", Options::chatLinks, true);
      bool("chatLinksPrompt", "options.chat.links.prompt", Options::chatLinksPrompt, true);
      unit("chatOpacity", "options.chat.opacity", Options::chatOpacity, (double)1.0F, (v) -> percent(v * 0.9 + 0.1));
      unit("textBackgroundOpacity", "options.accessibility.text_background_opacity", Options::textBackgroundOpacity, (double)0.5F, VanillaOptions::percent);
      bool("backgroundForChatOnly", "options.accessibility.text_background", Options::backgroundForChatOnly, true);
      unit("chatScale", "options.chat.scale", Options::chatScale, (double)1.0F, VanillaOptions::percent);
      unit("chatLineSpacing", "options.chat.line_spacing", Options::chatLineSpacing, (double)0.0F, VanillaOptions::percent);
      dbl("chatDelay", "cfhgui_config.opt.chatDelay", Options::chatDelay, (double)0.0F, (double)6.0F, 0.1, (double)0.0F, (v) -> v <= (double)0.0F ? Component.translatable("options.chat.delay_none") : Component.translatable("options.chat.delay", new Object[]{String.format(Locale.ROOT, "%.1f", v)}));
      unit("chatWidth", "options.chat.width", Options::chatWidth, (double)1.0F, (v) -> Component.literal(Math.round(v * (double)280.0F + (double)40.0F) + "px"));
      unit("chatHeightFocused", "options.chat.height.focused", Options::chatHeightFocused, (double)1.0F, (v) -> Component.literal(Math.round(v * (double)160.0F + (double)20.0F) + "px"));
      unit("chatHeightUnfocused", "options.chat.height.unfocused", Options::chatHeightUnfocused, 0.44366196, (v) -> Component.literal(Math.round(v * (double)160.0F + (double)20.0F) + "px"));
      bool("autoSuggestions", "options.autoSuggestCommands", Options::autoSuggestions, true);
      bool("hideMatchedNames", "options.hideMatchedNames", Options::hideMatchedNames, true);
      bool("onlyShowSecureChat", "options.onlyShowSecureChat", Options::onlyShowSecureChat, false);
      bool("reducedDebugInfo", "options.reducedDebugInfo", Options::reducedDebugInfo, false);
      dbl("notificationDisplayTime", "options.notifications.display_time", Options::notificationDisplayTime, (double)0.5F, (double)10.0F, 0.1, (double)1.0F, (v) -> Component.translatable("options.multiplier", new Object[]{String.format(Locale.ROOT, "%.1f", v)}));
      choice("narrator", "options.narrator", Options::narrator, "OFF");
      bool("highContrast", "options.accessibility.high_contrast", Options::highContrast, false);
      bool("forceUnicodeFont", "options.forceUnicodeFont", Options::forceUnicodeFont, false);
      bool("japaneseGlyphVariants", "options.japaneseGlyphVariants", Options::japaneseGlyphVariants, false);
      choice("mainHand", "options.mainHand", Options::mainHand, "RIGHT");
      bool("darkMojangStudiosBackground", "options.darkMojangStudiosBackgroundColor", Options::darkMojangStudiosBackground, false);
      bool("hideSplashTexts", "options.hideSplashTexts", Options::hideSplashTexts, false);
      unit("panoramaScrollSpeed", "options.accessibility.panorama_speed", Options::panoramaSpeed, (double)1.0F, VanillaOptions::percent);
      bool("narratorHotkey", "options.accessibility.narrator_hotkey", Options::narratorHotkey, true);
      bool("realmsNotifications", "options.realmsNotifications", Options::realmsNotifications, true);
      bool("allowServerListing", "options.allowServerListing", Options::allowServerListing, true);
      bool("telemetryOptInExtra", "options.telemetry.button", Options::telemetryOptInExtra, false);
   }
}
