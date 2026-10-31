package com.onehumanawa.cfhcore.content.gui.config.page;

import com.onehumanawa.cfhcore.content.gui.config.option.AdvancedBinding;
import com.onehumanawa.cfhcore.content.gui.config.option.OptionBinding;
import com.onehumanawa.cfhcore.content.gui.config.resolver.VanillaOptions;
import com.onehumanawa.cfhcore.content.gui.config.ui.OptionPageBase;
import com.onehumanawa.cfhcore.content.gui.config.ui.Section;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.platform.CFHGuiPlatform;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.network.chat.Component;

public final class VideoPage extends OptionPageBase {
   public static final String VANILLA_TAB = "display";
   private static final String[] DISPLAY = new String[]{"fullscreen", "enableVsync", "maxFps", "guiScale", "gamma", "menuBackgroundBlurriness"};
   private static final String[] GRAPHICS = new String[]{"renderDistance", "simulationDistance", "graphicsMode", "renderClouds", "particles", "ao", "biomeBlendRadius", "entityDistanceScaling", "entityShadows", "mipmapLevels"};
   private static final String[] PERFORMANCE = new String[]{"prioritizeChunkUpdates"};
   private static final String[] EFFECTS = new String[]{"fov", "screenEffectScale", "fovEffectScale", "darknessEffectScale", "damageTiltStrength", "glintSpeed", "glintStrength", "hideLightningFlashes", "bobView", "attackIndicator", "showAutosaveIndicator"};
   private static final String[] ADVANCED_WORDS = new String[]{"chunk_update", "chunk_build", "thread", "cull", "occlusion", "defer", "no_error", "buffer", "sorting", "translucen", "entity_distance", "biome_blend", "mipmap", "persistent", "async", "memory", "cpu", "gpu", "light_update", "render_ahead", "prevent_shaders", "linear_flat", "fast_random", "steady_debug", "use_fog_occlusion", "block_face", "compact_vertex", "always_defer", "allocator", "leaves_quality"};

   public VideoPage() {
      super("video", Component.translatable("cfhgui_config.page.video"), Icon.MONITOR);
   }

   public static boolean sodium() {
      return false;
   }

   public static boolean distantHorizons() {
      return false;
   }

   protected boolean pills(String tabKey) {
      return false;
   }

   protected List<Section> sections() {
      List<Section> out = new ArrayList();
      boolean sodium = sodium();
      out.add(vanilla("display", DISPLAY, sodium).tab("display", VideoTopics.title("display")));
      out.add(vanilla("world", GRAPHICS, sodium).tab("graphics", VideoTopics.title("graphics")));
      out.add(vanilla("performance", PERFORMANCE, sodium).tab("performance", VideoTopics.title("performance")));
      out.add(vanilla("effects", EFFECTS, sodium).tab("effects", VideoTopics.title("effects")));
      return out;
   }

   static boolean advancedPage(String pageTitle) {
      String t = pageTitle.toLowerCase(Locale.ROOT);
      return t.contains("advanced") || t.contains("performance");
   }

   static boolean advancedOption(String idPath, String label) {
      String var10000 = idPath.toLowerCase(Locale.ROOT);
      String hay = var10000 + " " + label.toLowerCase(Locale.ROOT).replace(' ', '_');

      for(String w : ADVANCED_WORDS) {
         if (hay.contains(w)) {
            return true;
         }
      }

      return false;
   }

   private static String idPath(String bindingId) {
      int i = bindingId.lastIndexOf(58);
      return i < 0 ? bindingId : bindingId.substring(i + 1);
   }

   private static Section vanilla(String id, String[] keys, boolean sodium) {
      Section s = Section.of(id, Component.translatable("cfhgui_config.video.vanilla"));

      for(String k : keys) {
         if (!sodium || !VanillaOptions.COVERED_BY_SODIUM.contains(k)) {
            Optional<OptionBinding> var10000 = VanillaOptions.get(k);
            Objects.requireNonNull(s);
            var10000.ifPresent(s::add);
         }
      }

      return s;
   }

   public static List<OptionBinding> presetBindings() {
      List<OptionBinding> out = new ArrayList();
      out.addAll(VanillaOptions.all(DISPLAY));
      out.addAll(VanillaOptions.all(GRAPHICS));
      out.addAll(VanillaOptions.all(PERFORMANCE));
      out.addAll(VanillaOptions.all(EFFECTS));
      return out;
   }
}
