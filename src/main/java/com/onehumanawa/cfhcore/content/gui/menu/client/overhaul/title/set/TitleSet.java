package com.onehumanawa.cfhcore.content.gui.menu.client.overhaul.title.set;

import com.onehumanawa.cfhcore.content.gui.core.platform.CFHGuiPlatform;
import com.onehumanawa.cfhcore.content.gui.core.stage.Stage;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageFinish;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageLighting;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.TerrainNode;
import com.onehumanawa.cfhcore.content.gui.core.stage.scene.Scene;
import java.util.Random;
import java.util.function.BiFunction;

public abstract class TitleSet {
   protected Stage stage;
   protected Scene scene;
   protected StageLighting light;
   protected StageFinish finish;
   protected Random rnd;

   public final void build(Stage stage, Scene scene, long seed) {
      this.stage = stage;
      this.scene = scene;
      this.light = stage.lighting();
      this.finish = stage.finish() != null ? stage.finish() : StageFinish.cinematic();
      stage.finish(this.finish);
      this.rnd = new Random(seed);
      this.light.clearLamps();
      this.dress();
   }

   protected abstract void dress();

   public abstract int pool();

   public int dust() {
      return 1442834370;
   }

   protected static boolean mod(String id) {
      return CFHGuiPlatform.get().isModLoaded(id);
   }

   protected TerrainNode surroundings(Plot plot, int side, int back, int front, float top, BiFunction<Integer, Integer, TerrainNode.Column> column) {
      int cell = 2;
      int x0 = plot.minX() - side / 2 * 2;
      int z0 = plot.minZ() - back / 2 * 2;
      int w = (plot.maxX() - x0 + 1 + side) / 2;
      int d = (plot.maxZ() - z0 + 1 + front) / 2;
      TerrainNode land = (new TerrainNode(w, d)).still();
      int ground = 1;

      for(int k = 0; k < d; ++k) {
         for(int i = 0; i < w; ++i) {
            int x = x0 + i * 2;
            int z = z0 + k * 2;
            if (plot.covers(x, z) && plot.covers(x + 2 - 1, z + 2 - 1)) {
               land.hole(i, k);
            } else {
               TerrainNode.Column c = (TerrainNode.Column)column.apply(x, z);
               if (c != null) {
                  land.put(i, k, c);
                  ground = c.ground();
               }
            }
         }
      }

      land.edgeFade(0.68F, 3.0F);
      land.scale(2.0F);
      ((TerrainNode)this.stage.add(land)).at((float)x0 - 0.5F + (float)(w / 2 * 2), top - (float)(ground * 2), (float)z0 - 0.5F + (float)(d / 2 * 2));
      return land;
   }
}
