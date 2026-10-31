package com.onehumanawa.cfhcore.content.gui.config.page;

import java.util.List;
import java.util.Locale;
import net.minecraft.network.chat.Component;

final class VideoTopics {
   static final String DISPLAY = "display";
   static final String GRAPHICS = "graphics";
   static final String PERFORMANCE = "performance";
   static final String EFFECTS = "effects";
   static final String EXTRAS = "extras";
   static final List<String> ALL = List.of("display", "graphics", "performance", "effects", "extras");
   private static final String[] EXTRAS_WORDS = new String[]{"overlay", "text_contrast", "show_fps", "coordinates", "debug_hud", "debug", "toast", "tooltip", "prevent_shaders", "hud"};
   private static final String[] PERFORMANCE_WORDS = new String[]{"chunk_update", "defer", "cull", "occlusion", "animate_only", "no_error", "persistent", "render_ahead", "cpu", "gpu", "thread", "memory", "prioritiz", "async", "fast_random", "linear_flat", "light_update", "reduce_resolution", "buffer", "translucen", "sorting", "performance", "lag"};
   private static final String[] EFFECTS_WORDS = new String[]{"animation", "animate", "particle", "bobbing", "bob_view", "attack_indicator", "autosave", "fov", "screen_effect", "damage", "tilt", "glint", "lightning", "darkness", "shake", "hurt", "instant_sneak", "panini"};
   private static final String[] DISPLAY_WORDS = new String[]{"fullscreen", "vsync", "v_sync", "fps", "frame", "gui_scale", "guiscale", "brightness", "gamma", "resolution", "monitor", "blur", "adaptive", "wayland", "window", "borderless"};
   private static final String[] GRAPHICS_WORDS = new String[]{"render_distance", "simulation", "graphics", "cloud", "weather", "leaves", "smooth_light", "ambient_occlusion", "biome", "entity_distance", "entity_shadow", "shadow", "vignette", "mipmap", "fog", "sky", "star", "sun", "moon", "beacon", "enchanting", "item_frame", "name_tag", "light", "detail", "color", "shader", "texture", "quality", "distance", "render"};

   private VideoTopics() {
   }

   static Component title(String topic) {
      return Component.translatable("cfhgui_config.video.topic." + topic);
   }

   static String topicOf(String idPath, String label, String pageName) {
      String var10000 = idPath.toLowerCase(Locale.ROOT);
      String hay = var10000 + " " + label.toLowerCase(Locale.ROOT).replace(' ', '_');
      if (any(hay, EXTRAS_WORDS)) {
         return "extras";
      } else if (any(hay, PERFORMANCE_WORDS)) {
         return "performance";
      } else if (any(hay, EFFECTS_WORDS)) {
         return "effects";
      } else if (any(hay, DISPLAY_WORDS)) {
         return "display";
      } else {
         return any(hay, GRAPHICS_WORDS) ? "graphics" : topicOfPage(pageName);
      }
   }

   static String topicOfPage(String pageName) {
      String p = pageName.toLowerCase(Locale.ROOT);
      if (!p.contains("general") && !p.contains("display") && !p.contains("video")) {
         if (!p.contains("quality") && !p.contains("detail") && !p.contains("render") && !p.contains("fog") && !p.contains("shader") && !p.contains("graphic")) {
            if (!p.contains("performance") && !p.contains("advanced")) {
               return !p.contains("animation") && !p.contains("particle") && !p.contains("effect") ? "extras" : "effects";
            } else {
               return "performance";
            }
         } else {
            return "graphics";
         }
      } else {
         return "display";
      }
   }

   private static boolean any(String hay, String[] words) {
      for(String w : words) {
         if (hay.contains(w)) {
            return true;
         }
      }

      return false;
   }
}
