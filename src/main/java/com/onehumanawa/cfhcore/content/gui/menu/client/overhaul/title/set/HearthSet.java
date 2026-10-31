package com.onehumanawa.cfhcore.content.gui.menu.client.overhaul.title.set;

import com.onehumanawa.cfhcore.content.gui.core.stage.node.EntityNode;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.FxNode;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.MotesNode;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.SkyNode;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.SmokeNode;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.TerrainNode;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.SmokeNode.Kind;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.CatVariant;
import net.minecraft.world.level.block.state.BlockState;

final class HearthSet extends TitleSet {
   private static final float FIRE_X = -3.5F;
   private static final float FIRE_Y = 0.45F;
   private static final float FIRE_Z = -4.9F;
   private static final int STACK_TOP = 8;
   private Plot plot;

   public int pool() {
      return 822073768;
   }

   public int dust() {
      return 1224726696;
   }

   protected void dress() {
      this.stage.add((new SkyNode(260, 11L)).colors(-15326918, -15063488, -15458506, -16051418, -16447473).sun(-38.0F, 24.0F, -4667152, 0.55F, 2.2F, 26.0F).stars(0.9F));
      this.plot = new Plot(this.stage, -28, -2, -35, 29, 10, 6);
      this.ground();
      this.cabin();
      this.hearth();
      this.room();
      this.outside();
      this.plot.done(this.stage);
      TerrainNode.Column snow = new TerrainNode.Column(1, "snow", -1, "dirt", 0, 0, false, (String)null, (String)null, 0);
      this.surroundings(this.plot, 120, 130, 60, 0.125F, (x, z) -> snow);
      this.props();
      this.rig();
      this.air();
   }

   private void ground() {
      BlockState snow = Pick.of("snow_block");
      this.plot.fill(-28, -2, -35, 29, -1, 6, (BlockState)snow);
      this.plot.fill(-9, -2, -7, 9, -1, 4, (BlockState)Pick.of("cobblestone"));
      this.plot.scatter(-7, -1, -5, 7, -1, 4, this.rnd, 0.12F, Pick.of("spruce_planks"), Pick.of("stripped_spruce_wood"));
      this.plot.fill(-2, 0, -1, 2, 0, 1, (BlockState)Pick.of("red_carpet"));
      this.plot.fill(-1, 0, 0, 1, 0, 0, (BlockState)Pick.of("brown_carpet"));
      this.plot.put(0, 0, 1, (BlockState)Pick.of("dustydecorations:cowhide_rug[facing=south]", "brown_carpet"));
   }

   private void cabin() {
      BlockState logX = Pick.of("spruce_log[axis=x]");
      BlockState logZ = Pick.of("spruce_log[axis=z]");
      BlockState post = Pick.of("spruce_log[axis=y]");
      BlockState stripped = Pick.of("stripped_spruce_log[axis=x]");

      for(int y = 0; y <= 4; ++y) {
         this.plot.fill(-7, y, -6, 7, y, -6, (BlockState)(y % 2 == 0 ? logX : stripped));
         this.plot.fill(-8, y, -5, -8, y, 3, (BlockState)logZ);
         this.plot.fill(8, y, -5, 8, y, 3, (BlockState)logZ);
      }

      for(int x : new int[]{-8, 8}) {
         this.plot.fill(x, 0, -6, x, 5, -6, (BlockState)post);
      }

      BlockState drift = Pick.of("snow[layers=2]");
      this.plot.fill(-7, 5, -6, 7, 5, -6, (BlockState)drift);
      this.plot.fill(-8, 5, -5, -8, 5, 3, (BlockState)drift);
      this.plot.fill(8, 5, -5, 8, 5, 3, (BlockState)drift);
      BlockState pane = Pick.of("connectedglass:borderless_glass_pane[east=true,west=true]", "glass_pane[east=true,west=true]");
      this.plot.fill(3, 1, -6, 4, 2, -6, (BlockState)pane);
      this.plot.fill(2, 0, -6, 5, 0, -6, (BlockState)Pick.of("stripped_spruce_log[axis=x]"));
      this.plot.fill(2, 3, -6, 5, 3, -6, (BlockState)Pick.of("stripped_spruce_log[axis=x]"));
      this.plot.fill(2, 1, -6, 2, 2, -6, (BlockState)Pick.of("stripped_spruce_log[axis=y]"));
      this.plot.fill(5, 1, -6, 5, 2, -6, (BlockState)Pick.of("stripped_spruce_log[axis=y]"));
      this.plot.fill(3, 0, -5, 4, 0, -5, (BlockState)Pick.of("spruce_trapdoor[half=top,facing=south,open=false]"));
      this.plot.put(2, 1, -5, new String[]{"another_furniture:red_curtain[facing=south,vertical=down,horizontal=single,open=true]"});
      this.plot.put(2, 2, -5, new String[]{"another_furniture:red_curtain[facing=south,vertical=up,horizontal=single,open=true]"});
      this.plot.put(5, 1, -5, new String[]{"another_furniture:red_curtain[facing=south,vertical=down,horizontal=single,open=true]"});
      this.plot.put(5, 2, -5, new String[]{"another_furniture:red_curtain[facing=south,vertical=up,horizontal=single,open=true]"});
      this.plot.fill(-8, 1, -2, -8, 2, -1, (BlockState)Pick.of("connectedglass:borderless_glass_pane[north=true,south=true]", "glass_pane[north=true,south=true]"));
      BlockState beam = Pick.of("stripped_dark_oak_log[axis=x]");
      this.plot.fill(-7, 4, -3, 7, 4, -3, (BlockState)beam);
      this.plot.fill(-7, 4, 1, 7, 4, 1, (BlockState)beam);
   }

   private void hearth() {
      BlockState stone = Pick.of("stone_bricks");
      BlockState mossy = Pick.of("mossy_stone_bricks");
      BlockState cracked = Pick.of("cracked_stone_bricks");
      BlockState cobble = Pick.of("cobblestone");
      this.plot.fill(-5, 0, -5, -2, 0, -4, (BlockState)cobble);
      this.plot.scatter(-5, 1, -5, -2, 5, -4, this.rnd, 0.22F, stone, mossy, cracked);
      this.plot.scatter(-5, 0, -6, -2, 5, -6, this.rnd, 0.2F, stone, cracked);
      this.plot.scatter(-4, 6, -6, -3, 8, -5, this.rnd, 0.25F, stone, mossy, cracked);
      this.plot.put(-4, 9, -5, new String[]{"adorn:stone_brick_chimney[connected=false]", "stone_brick_wall[up=true]"});
      this.plot.put(-3, 9, -6, new String[]{"adorn:stone_brick_chimney[connected=false]", "stone_brick_wall[up=true]"});
      this.plot.fill(-4, 0, -5, -3, 1, -4, (BlockState)Pick.of("air"));
      this.plot.fill(-4, 0, -6, -3, 1, -6, (BlockState)Pick.of("nether_bricks"));
      this.plot.fill(-4, -1, -5, -3, -1, -4, (BlockState)Pick.of("nether_bricks"));
      this.plot.fill(-4, 2, -5, -3, 2, -5, (BlockState)Pick.of("nether_bricks"));
      this.plot.put(-4, 0, -5, (BlockState)Pick.of("campfire[lit=true,facing=east,signal_fire=false]"));
      this.plot.put(-3, 0, -5, (BlockState)Pick.of("campfire[lit=true,facing=west,signal_fire=false]"));
      this.plot.fill(-5, 2, -4, -2, 2, -4, (BlockState)Pick.of("stone_brick_stairs[facing=north,half=top,shape=straight]"));
      this.plot.fill(-5, -1, -3, -2, -1, -3, (BlockState)Pick.of("polished_andesite"));
      this.plot.put(-5, 2, -3, (BlockState)Pick.of("spruce_stairs[facing=north,half=top,shape=straight]"));
      this.plot.put(-2, 2, -3, (BlockState)Pick.of("spruce_stairs[facing=north,half=top,shape=straight]"));
      this.plot.fill(-4, 2, -3, -3, 2, -3, (BlockState)Pick.of("spruce_slab[type=top]"));
      this.plot.put(-5, 3, -3, (BlockState)Pick.of("candle[candles=3,lit=true]"));
      this.plot.put(-4, 3, -3, new String[]{"mcwholidays:garland[facing=south]", "potted_spruce_sapling"});
      this.plot.put(-3, 3, -3, new String[]{"dustydecorations:jars[amount=3]", "potted_fern"});
      this.plot.put(-2, 3, -3, (BlockState)Pick.of("red_candle[candles=2,lit=true]"));
      this.plot.put(-4, 2, -2, new String[]{"mcwholidays:red_stocking[facing=south]", "air"});
      this.plot.put(-3, 2, -2, new String[]{"mcwholidays:green_stocking[facing=south]", "air"});
      this.plot.put(-6, 0, -5, new String[]{"mcwholidays:pile_of_spruce_wood[facing=east]", "spruce_log[axis=z]"});
      this.plot.put(-6, 0, -4, new String[]{"mcwholidays:pile_of_spruce_wood[facing=east]", "spruce_log[axis=z]"});
      if (!Pick.has("mcwholidays:pile_of_spruce_wood")) {
         this.plot.put(-6, 1, -5, (BlockState)Pick.of("spruce_log[axis=z]"));
      }

   }

   private void room() {
      this.plot.fill(-7, 0, -5, -6, 2, -5, (BlockState)Pick.of("bookshelf"));
      this.plot.put(-7, 3, -5, new String[]{"dustydecorations:book_stack[facing=south]", "lantern[hanging=false]"});
      this.plot.put(-6, 3, -5, new String[]{"dustydecorations:books[facing=south]", "air"});
      this.plot.fill(-1, 0, -5, 1, 0, -5, (BlockState)Pick.of("adorn:spruce_drawer[facing=south]", "another_furniture:spruce_drawer[facing=south,open=false]", "barrel[facing=up,open=false]"));
      this.plot.put(-1, 1, -5, new String[]{"dustydecorations:honey_jar[amount=2]", "flower_pot"});
      this.plot.put(0, 1, -5, new String[]{"dustydecorations:book_stack[facing=south]", "potted_fern"});
      this.plot.put(1, 1, -5, new String[]{"dustydecorations:jars[amount=2]", "potted_red_tulip"});
      this.plot.put(0, 2, -6, new String[]{"dustydecorations:winter_wreath[facing=south]", "air"});
      this.plot.put(3, 0, -2, new String[]{"adorn:red_sofa[facing=west]", "another_furniture:red_sofa[facing=west,type=single]", "spruce_stairs[facing=east,half=bottom,shape=straight]"});
      if (!Pick.has("adorn:red_sofa") && !Pick.has("another_furniture:red_sofa")) {
         this.plot.put(3, 0, -3, (BlockState)Pick.of("spruce_trapdoor[facing=north,half=bottom,open=true]"));
         this.plot.put(3, 0, -1, (BlockState)Pick.of("spruce_trapdoor[facing=south,half=bottom,open=true]"));
      }

      this.plot.put(3, 0, -4, new String[]{"adorn:spruce_table", "another_furniture:spruce_table[facing=north,leg_1=true,leg_2=true,leg_3=true,leg_4=true]", "spruce_fence"});
      if (!Pick.has("adorn:spruce_table") && !Pick.has("another_furniture:spruce_table")) {
         this.plot.put(3, 1, -4, (BlockState)Pick.of("spruce_pressure_plate"));
      }

      this.plot.put(3, 1, -4, new String[]{"adorn:candlelit_lantern[hanging=false]", "lantern[hanging=false]"});
      this.plot.put(4, 0, -3, new String[]{"dustydecorations:wicker_basket[facing=west]", "barrel[facing=up,open=false]"});
      this.plot.put(6, 2, -5, new String[]{"adorn:spruce_shelf[facing=south]", "another_furniture:spruce_shelf[facing=south,horizontal=single]", "spruce_trapdoor[facing=south,half=top,open=false]"});
      this.plot.put(7, 2, -5, new String[]{"adorn:spruce_shelf[facing=south]", "another_furniture:spruce_shelf[facing=south,horizontal=single]", "spruce_trapdoor[facing=south,half=top,open=false]"});
      this.plot.put(6, 3, -5, new String[]{"dustydecorations:jars[amount=4]", "flower_pot"});
      this.plot.put(7, 3, -5, new String[]{"dustydecorations:books[facing=south]", "potted_fern"});
      this.plot.put(6, 0, -5, (BlockState)Pick.of("chiseled_bookshelf[facing=south,slot_0_occupied=true,slot_1_occupied=true,slot_2_occupied=false,slot_3_occupied=true,slot_4_occupied=true,slot_5_occupied=true]"));
      this.plot.put(7, 0, -5, (BlockState)Pick.of("barrel[facing=up,open=false]"));
      this.plot.put(7, 1, -5, new String[]{"dustydecorations:pots_and_pans[amount=3]", "flower_pot"});
      this.plot.fill(-7, 0, -1, -7, 0, 1, (BlockState)Pick.of("adorn:spruce_bench[facing=east]", "spruce_stairs[facing=west,half=bottom,shape=straight]"));
      this.plot.put(-7, 0, -3, new String[]{"dustydecorations:wicker_basket[facing=east]", "barrel[facing=up,open=false]"});
      this.plot.put(-6, 3, 1, (BlockState)Pick.of("lantern[hanging=true]"));
   }

   private void outside() {
      int[][] trees = new int[][]{{-2, -11, 6}, {3, -13, 7}, {7, -10, 5}, {-8, -14, 7}, {10, -16, 8}, {-12, -9, 6}, {12, -8, 5}, {0, -18, 8}, {-5, -19, 7}, {6, -20, 8}, {-15, -2, 7}, {-19, -8, 8}, {-23, -1, 6}, {-17, -17, 7}, {-24, -13, 8}, {-21, -24, 7}, {-12, -27, 8}, {-3, -30, 7}, {5, -27, 6}, {13, -25, 8}, {18, -20, 7}, {23, -11, 8}, {16, -4, 6}, {21, 0, 7}, {25, -28, 7}, {-26, -30, 6}, {9, -33, 8}};

      for(int[] t : trees) {
         this.spruce(t[0], t[1], t[2]);
      }

      BlockState layer = Pick.of("snow[layers=1]");
      BlockState deep = Pick.of("snow[layers=3]");

      for(int x = -28; x <= 29; ++x) {
         for(int z = -35; z <= 6; ++z) {
            if ((x < -8 || x > 8 || z < -6) && this.plot.empty(x, 0, z)) {
               this.plot.put(x, 0, z, (BlockState)(this.rnd.nextFloat() < 0.3F ? deep : layer));
            }
         }
      }

      BlockState fence = Pick.of("spruce_fence[east=true,west=true]");

      for(int x = -6; x <= 9; ++x) {
         if (this.plot.at(x, 0, -9).getBlock() != fence.getBlock()) {
            this.plot.put(x, 0, -9, (BlockState)fence);
         }
      }

   }

   private void spruce(int x, int z, int height) {
      BlockState log = Pick.of("spruce_log[axis=y]");
      BlockState leaves = Pick.of("spruce_leaves[persistent=true]");
      BlockState snow = Pick.of("snow[layers=1]");

      for(int y = 0; y < height; ++y) {
         this.plot.put(x, y, z, log);
      }

      for(int y = 2; y <= height + 1; ++y) {
         int r = Math.max(0, (height + 1 - y) / 2 - ((y - height) % 2 == 0 ? 0 : 1));

         for(int dx = -r; dx <= r; ++dx) {
            for(int dz = -r; dz <= r; ++dz) {
               if (Math.abs(dx) + Math.abs(dz) <= r + (r > 1 ? 1 : 0) && (dx != 0 || dz != 0 || y >= height) && this.plot.empty(x + dx, y, z + dz)) {
                  this.plot.put(x + dx, y, z + dz, leaves);
               }
            }
         }
      }

      for(int dx = -3; dx <= 3; ++dx) {
         for(int dz = -3; dz <= 3; ++dz) {
            for(int y = height + 2; y >= 1; --y) {
               if (this.plot.at(x + dx, y, z + dz).getBlock() == leaves.getBlock()) {
                  if (this.plot.empty(x + dx, y + 1, z + dz)) {
                     this.plot.put(x + dx, y + 1, z + dz, snow);
                  }
                  break;
               }
            }
         }
      }

   }

   private void props() {
      EntityNode cat = (EntityNode)this.stage.add(new EntityNode(this.stage.level(), EntityType.CAT));
      Entity var3 = cat.entity();
      if (var3 instanceof Cat c) {
         c.setVariant(BuiltInRegistries.CAT_VARIANT.getHolderOrThrow(CatVariant.RED));
         c.setLying(true);
      }

      cat.lookAtCursor(false).pickable(false);
      cat.at(-2.8F, 0.0F, -2.5F).yaw(-150.0F);
      cat.tint(-2056080);
   }

   private void rig() {
      this.light.key(1.8F, 4.2F, 6.5F).keyColor(0.44F, 0.37F, 0.29F).fill(1.0F, 0.35F, -0.4F, 0.07F, 0.1F, 0.19F).ambientColor(0.1F, 0.085F, 0.08F).rim(1.0F, 0.62F, 0.32F, 0.22F).wrap(0.5F);
      int fire = this.light.lamp(-3.5F, 0.79999995F, -4.0F, 2.1F, 0.95F, 0.36F, 8.5F);
      this.light.lampFlicker(fire, 0.42F);
      int embers = this.light.lamp(-3.5F, 0.15F, -3.4F, 0.9F, 0.32F, 0.08F, 3.6F);
      this.light.lampFlicker(embers, 0.6F);
      int lantern = this.light.lamp(3.0F, 1.5F, -3.6F, 0.95F, 0.66F, 0.34F, 4.8F);
      this.light.lampFlicker(lantern, 0.08F);
      int hanging = this.light.lamp(-6.0F, 3.3F, 1.0F, 0.7F, 0.5F, 0.28F, 4.2F);
      this.light.lampFlicker(hanging, 0.06F);
      int candles = this.light.lamp(-3.5F, 3.4F, -2.6F, 0.5F, 0.33F, 0.16F, 3.0F);
      this.light.lampFlicker(candles, 0.25F);
      this.light.lamp(3.5F, 2.2F, -8.5F, 0.22F, 0.32F, 0.58F, 7.5F);
      this.light.lamp(0.0F, 1.2F, -5.0F, 0.45F, 0.3F, 0.15F, 3.2F);
      this.finish.bloom(0.55F).vignette(0.42F).warmth(0.018F).contrast(1.08F).saturation(1.1F).grain(1.4F).shadows(-0.004F, 0.002F, 0.018F).highlights(0.02F, 0.008F, -0.01F);
   }

   private void air() {
      ((MotesNode)this.stage.add((new MotesNode(26, 1.3F, 2.2F, 0.9F, 0.035F, -520119748, 31L)).rise(5.5F).sway(1.6F))).at(-3.5F, 0.2F, -4.0F);
      ((SmokeNode)this.stage.add((new SmokeNode(Kind.COSY, 14, 7L)).rise(1.6F, 0.9F).spread(0.35F).wind(0.0F, -0.25F).size(0.4F, 1.0F).colors(-1328502648, 1080055888))).at(-3.5F, 0.75F, -4.9F);
      ((SmokeNode)this.stage.add((new SmokeNode(Kind.COSY, 26, 8L)).rise(7.0F, 0.7F).spread(0.9F).wind(0.45F, 0.12F).size(0.9F, 3.2F).colors(-1600546142, 5265512))).at(-3.5F, 9.7F, -5.5F);
      MotesNode snow = (MotesNode)this.stage.add((new MotesNode(260, 30.0F, 12.0F, 16.0F, 0.06F, -386730241, 44L)).rise(-7.0F).sway(3.0F).additive(false));
      snow.at(0.0F, 0.0F, -15.0F);
      ((FxNode)this.stage.add(FxNode.glow(3.4F, 1191152186).fade(1.6F))).at(-2.9F, 0.012F, -2.6F);
      ((FxNode)this.stage.add(FxNode.glow(2.2F, 714060031).fade(1.8F))).at(3.5F, 0.012F, -4.6F);
   }
}
