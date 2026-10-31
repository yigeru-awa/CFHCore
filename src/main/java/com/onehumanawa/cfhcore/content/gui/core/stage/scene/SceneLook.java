package com.onehumanawa.cfhcore.content.gui.core.stage.scene;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.onehumanawa.cfhcore.content.gui.core.stage.Stage;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageFinish;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageLighting;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.SkyNode;
import net.minecraft.util.GsonHelper;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

public final class SceneLook {
   private final @Nullable JsonObject sky;
   private final @Nullable JsonObject rig;
   private final @Nullable JsonObject finish;
   private final @Nullable JsonArray lamps;

   private SceneLook(@Nullable JsonObject sky, @Nullable JsonObject rig, @Nullable JsonArray lamps, @Nullable JsonObject finish) {
      this.sky = sky;
      this.rig = rig;
      this.lamps = lamps;
      this.finish = finish;
   }

   static @Nullable SceneLook parse(JsonObject json) {
      JsonObject sky = object(json, "sky");
      JsonObject rig = object(json, "rig");
      JsonObject finish = object(json, "finish");
      JsonArray lamps = json.has("lamps") && json.get("lamps").isJsonArray() ? json.getAsJsonArray("lamps") : null;
      return sky == null && rig == null && finish == null && lamps == null ? null : new SceneLook(sky, rig, lamps, finish);
   }

   public boolean hasSky() {
      return this.sky != null;
   }

   void addSky(Stage stage) {
      if (this.sky != null) {
         SkyNode node = new SkyNode(GsonHelper.getAsInt(this.sky, "starCount", 260), GsonHelper.getAsLong(this.sky, "seed", 11L));
         if (this.sky.has("colors") && this.sky.get("colors").isJsonArray() && this.sky.getAsJsonArray("colors").size() >= 5) {
            JsonArray c = this.sky.getAsJsonArray("colors");
            node.colors(color(c.get(0)), color(c.get(1)), color(c.get(2)), color(c.get(3)), color(c.get(4)));
         }

         JsonObject sun = object(this.sky, "sun");
         if (sun != null) {
            node.sun(GsonHelper.getAsFloat(sun, "yaw", 0.0F), GsonHelper.getAsFloat(sun, "elevation", 5.0F), color(sun.get("color"), -14192), GsonHelper.getAsFloat(sun, "glow", 0.6F), GsonHelper.getAsFloat(sun, "disc", 4.0F), GsonHelper.getAsFloat(sun, "reach", 40.0F));
         }

         node.stars(GsonHelper.getAsFloat(this.sky, "stars", 0.0F));
         stage.add(node);
      }
   }

   public void applyLight(Stage stage) {
      StageLighting light = stage.lighting();
      if (this.rig != null) {
         Vector3f key = vec(this.rig.get("key"));
         if (key != null) {
            light.key(key.x, key.y, key.z);
         }

         Vector3f keyColor = vec(this.rig.get("keyColor"));
         if (keyColor != null) {
            light.keyColor(keyColor.x, keyColor.y, keyColor.z);
         }

         JsonObject fill = object(this.rig, "fill");
         if (fill != null) {
            Vector3f dir = vec(fill.get("dir"));
            Vector3f c = vec(fill.get("color"));
            light.fill(dir != null ? dir.x : -1.0F, dir != null ? dir.y : 0.25F, dir != null ? dir.z : 0.2F, c != null ? c.x : 0.11F, c != null ? c.y : 0.14F, c != null ? c.z : 0.2F);
         }

         Vector3f ambient = vec(this.rig.get("ambient"));
         if (ambient != null) {
            light.ambientColor(ambient.x, ambient.y, ambient.z);
         }

         JsonObject rim = object(this.rig, "rim");
         if (rim != null) {
            Vector3f c = vec(rim.get("color"));
            light.rim(c != null ? c.x : 1.0F, c != null ? c.y : 0.9F, c != null ? c.z : 0.76F, GsonHelper.getAsFloat(rim, "strength", 0.2F));
         }

         if (this.rig.has("wrap")) {
            light.wrap(GsonHelper.getAsFloat(this.rig, "wrap", 0.45F));
         }
      }

      if (this.lamps != null) {
         light.clearLamps();

         for(JsonElement e : this.lamps) {
            if (e.isJsonObject()) {
               JsonObject l = e.getAsJsonObject();
               Vector3f at = vec(l.get("at"));
               Vector3f c = vec(l.get("color"));
               if (at != null && c != null) {
                  int slot = light.lamp(at.x, at.y, at.z, c.x, c.y, c.z, GsonHelper.getAsFloat(l, "reach", 6.0F));
                  if (slot >= 0 && l.has("flicker")) {
                     light.lampFlicker(slot, GsonHelper.getAsFloat(l, "flicker", 0.0F));
                  }
               }
            }
         }
      }

      if (this.finish != null) {
         StageFinish f = stage.finish() != null ? stage.finish() : StageFinish.cinematic();
         if (this.finish.has("bloom")) {
            f.bloom(GsonHelper.getAsFloat(this.finish, "bloom", f.bloom));
         }

         if (this.finish.has("vignette")) {
            f.vignette(GsonHelper.getAsFloat(this.finish, "vignette", f.vignette));
         }

         if (this.finish.has("warmth")) {
            f.warmth(GsonHelper.getAsFloat(this.finish, "warmth", f.warmth));
         }

         if (this.finish.has("contrast")) {
            f.contrast(GsonHelper.getAsFloat(this.finish, "contrast", f.contrast));
         }

         if (this.finish.has("saturation")) {
            f.saturation(GsonHelper.getAsFloat(this.finish, "saturation", f.saturation));
         }

         if (this.finish.has("grain")) {
            f.grain(GsonHelper.getAsFloat(this.finish, "grain", f.grain));
         }

         if (this.finish.has("threshold")) {
            f.threshold(GsonHelper.getAsFloat(this.finish, "threshold", f.threshold));
         }

         if (this.finish.has("spread")) {
            f.spread(GsonHelper.getAsFloat(this.finish, "spread", f.spread));
         }

         Vector3f shadows = vec(this.finish.get("shadows"));
         Vector3f highlights = vec(this.finish.get("highlights"));
         if (shadows != null) {
            f.shadows(shadows.x, shadows.y, shadows.z);
         }

         if (highlights != null) {
            f.highlights(highlights.x, highlights.y, highlights.z);
         }

         stage.finish(f);
      }

   }

   private static @Nullable JsonObject object(JsonObject o, String key) {
      return o.has(key) && o.get(key).isJsonObject() ? o.getAsJsonObject(key) : null;
   }

   private static @Nullable Vector3f vec(@Nullable JsonElement el) {
      if (el != null && el.isJsonArray() && el.getAsJsonArray().size() >= 3) {
         JsonArray a = el.getAsJsonArray();
         return new Vector3f(a.get(0).getAsFloat(), a.get(1).getAsFloat(), a.get(2).getAsFloat());
      } else {
         return null;
      }
   }

   private static int color(@Nullable JsonElement el) {
      return color(el, -16777216);
   }

   private static int color(@Nullable JsonElement el, int fallback) {
      return SceneLoader.color(el, fallback);
   }
}
