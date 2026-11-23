package com.onehumanawa.cfhcore.content.gui.menu.client.overhaul.title.set;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.onehumanawa.cfhcore.content.gui.core.stage.builds.BuildCache;
import com.onehumanawa.cfhcore.content.gui.core.stage.builds.BuildSpec;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.FxNode;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

final class FileSet extends TitleSet {
   private int pool = 788523464;
   private int dust = 1442834370;

   public int pool() {
      return this.pool;
   }

   public int dust() {
      return this.dust;
   }

   protected void dress() {
      JsonObject title = this.scene.section("title");
      if (title != null) {
         this.pool = color(title.get("pool"), this.pool);
         this.dust = color(title.get("dust"), this.dust);
      }

      this.scene.applyLook(this.stage);
      BuildSpec build = this.scene.build();
      boolean here = build != null && build.available() && BuildCache.present(build) && BuildCache.problem(build) == null;
      if (here && title != null && title.has("blocks") && title.get("blocks").isJsonArray()) {
         this.blocks(title.getAsJsonArray("blocks"));
      }

      if (!here) {
         this.floor();
      }

   }

   private void blocks(JsonArray list) {
      List<int[]> boxes = new ArrayList();
      List<String[]> names = new ArrayList();
      int x0 = Integer.MAX_VALUE;
      int y0 = Integer.MAX_VALUE;
      int z0 = Integer.MAX_VALUE;
      int x1 = Integer.MIN_VALUE;
      int y1 = Integer.MIN_VALUE;
      int z1 = Integer.MIN_VALUE;

      for(JsonElement e : list) {
         if (e.isJsonObject()) {
            JsonObject o = e.getAsJsonObject();
            int[] a = ints(o.has("at") ? o.get("at") : o.get("from"));
            int[] b = ints(o.has("at") ? o.get("at") : o.get("to"));
            if (a != null && b != null) {
               String[] block;
               if (o.has("block") && o.get("block").isJsonArray()) {
                  JsonArray arr = o.getAsJsonArray("block");
                  block = new String[arr.size()];

                  for(int i = 0; i < block.length; ++i) {
                     block[i] = arr.get(i).getAsString();
                  }
               } else {
                  block = new String[]{o.has("block") ? o.get("block").getAsString() : "minecraft:air"};
               }

               int[] box = new int[]{Math.min(a[0], b[0]), Math.min(a[1], b[1]), Math.min(a[2], b[2]), Math.max(a[0], b[0]), Math.max(a[1], b[1]), Math.max(a[2], b[2])};
               boxes.add(box);
               names.add(block);
               x0 = Math.min(x0, box[0]);
               y0 = Math.min(y0, box[1]);
               z0 = Math.min(z0, box[2]);
               x1 = Math.max(x1, box[3]);
               y1 = Math.max(y1, box[4]);
               z1 = Math.max(z1, box[5]);
            }
         }
      }

      if (!boxes.isEmpty()) {
         Plot plot = new Plot(this.stage, x0, y0, z0, x1, y1, z1);

         for(int i = 0; i < boxes.size(); ++i) {
            int[] b = (int[])boxes.get(i);
            plot.fill(b[0], b[1], b[2], b[3], b[4], b[5], (String[])names.get(i));
         }

         plot.done(this.stage);
      }
   }

   private static @Nullable int[] ints(@Nullable JsonElement el) {
      if (el != null && el.isJsonArray() && el.getAsJsonArray().size() >= 3) {
         JsonArray a = el.getAsJsonArray();
         return new int[]{Math.round(a.get(0).getAsFloat()), Math.round(a.get(1).getAsFloat()), Math.round(a.get(2).getAsFloat())};
      } else {
         return null;
      }
   }

   private void floor() {
      Plot plot = new Plot(this.stage, -3, -1, -2, 3, -1, 2);
      plot.fill(-3, -1, -2, 3, -1, 2, (BlockState)Pick.of("polished_deepslate"));
      plot.fill(-2, -1, -1, 2, -1, 1, (BlockState)Pick.of("deepcfhgui_tiles"));
      plot.done(this.stage);
      ((FxNode)this.stage.add(FxNode.glow(16.0F, -13948377).fade(2.6F))).at(0.0F, -0.02F, 0.5F);
   }

   private static int color(@Nullable JsonElement el, int fallback) {
      if (el != null && !el.isJsonNull()) {
         try {
            String s = el.getAsString().trim();
            if (s.startsWith("#")) {
               s = s.substring(1);
            }

            if (s.length() == 6) {
               return -16777216 | (int)Long.parseLong(s, 16);
            }

            if (s.length() == 8) {
               return (int)Long.parseLong(s, 16);
            }
         } catch (RuntimeException var3) {
         }

         return fallback;
      } else {
         return fallback;
      }
   }
}
