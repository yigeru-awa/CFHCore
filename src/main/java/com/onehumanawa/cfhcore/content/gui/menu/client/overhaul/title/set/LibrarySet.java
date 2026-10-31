package com.onehumanawa.cfhcore.content.gui.menu.client.overhaul.title.set;

import com.onehumanawa.cfhcore.content.gui.core.stage.node.BlocksNode;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.FxNode;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.ItemNode;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.MotesNode;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.PivotNode;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;

final class LibrarySet extends TitleSet {
   private static final int TABLE_X = -3;
   private static final int TABLE_Z = -4;
   private Plot plot;
   private static final int BACK = -40;
   private static final int CEILING = 10;

   public int pool() {
      return 721409696;
   }

   public int dust() {
      return 1088866559;
   }

   protected void dress() {
      this.plot = new Plot(this.stage, -12, -2, -44, 12, 11, 14);
      this.room();
      this.hall();
      this.enchanting();
      this.desk();
      this.shelves();
      this.plot.done(this.stage);
      this.magic();
      this.candles();
      this.rig();
   }

   private void room() {
      this.plot.fill(-12, -2, -44, 12, -2, 14, (BlockState)Pick.of("deepslate"));
      this.plot.scatter(-8, -1, -40, 8, -1, 13, this.rnd, 0.14F, Pick.of("dark_oak_planks"), Pick.of("stripped_dark_oak_wood"));
      BlockState panel = Pick.of("dark_oak_planks");
      BlockState post = Pick.of("dark_oak_log[axis=y]");
      BlockState base = Pick.of("polished_deepslate");

      for(int y = 0; y < 10; ++y) {
         this.plot.fill(-8, y, -39, -8, y, 13, (BlockState)(y == 0 ? base : panel));
         this.plot.fill(8, y, -39, 8, y, 13, (BlockState)(y == 0 ? base : panel));
         this.plot.fill(-8, y, 14, 8, y, 14, (BlockState)(y == 0 ? base : panel));
      }

      for(int z = -37; z <= 13; z += 4) {
         this.plot.fill(-8, 0, z, -8, 9, z, (BlockState)post);
         this.plot.fill(8, 0, z, 8, 9, z, (BlockState)post);
      }

      this.plot.fill(-8, 10, -40, 8, 10, 14, (BlockState)panel);

      for(int z = -37; z <= 13; z += 4) {
         this.plot.fill(-7, 9, z, 7, 9, z, (BlockState)Pick.of("dark_oak_log[axis=x]"));
      }

      BlockState shelf = Pick.of("bookshelf");

      for(int z = 2; z <= 12; ++z) {
         if (Math.floorMod(z + 3, 4) != 0) {
            this.plot.fill(-7, 0, z, -7, 3, z, (BlockState)shelf);
            this.plot.fill(7, 0, z, 7, 3, z, (BlockState)shelf);
         }
      }

      this.plot.fill(-3, 0, -2, 3, 0, 2, (BlockState)Pick.of("black_carpet"));
      this.plot.fill(-2, 0, -1, 2, 0, 1, (BlockState)Pick.of("purple_carpet"));
   }

   private void hall() {
      BlockState shelf = Pick.of("bookshelf");
      BlockState panel = Pick.of("dark_oak_planks");

      for(int z = -39; z <= -8; ++z) {
         if (Math.floorMod(z - -37, 4) != 0) {
            this.plot.fill(-7, 0, z, -7, 3, z, (BlockState)shelf);
            this.plot.fill(7, 0, z, 7, 3, z, (BlockState)shelf);
            this.plot.fill(-7, 5, z, -7, 8, z, (BlockState)shelf);
            this.plot.fill(7, 5, z, 7, 8, z, (BlockState)shelf);
         }
      }

      this.plot.fill(-7, 4, -39, -6, 4, -8, (BlockState)Pick.of("dark_oak_slab[type=top]"));
      this.plot.fill(6, 4, -39, 7, 4, -8, (BlockState)Pick.of("dark_oak_slab[type=top]"));
      this.plot.fill(-6, 5, -39, -6, 5, -8, (BlockState)Pick.of("dark_oak_fence"));
      this.plot.fill(6, 5, -39, 6, 5, -8, (BlockState)Pick.of("dark_oak_fence"));
      this.plot.fill(-2, 0, -38, 2, 0, -7, (BlockState)Pick.of("black_carpet"));
      this.plot.fill(-1, 0, -38, 1, 0, -7, (BlockState)Pick.of("purple_carpet"));

      for(int y = 0; y < 10; ++y) {
         this.plot.fill(-8, y, -40, 8, y, -40, (BlockState)(y == 0 ? Pick.of("polished_deepslate") : panel));
      }

      this.plot.fill(-3, 2, -40, 3, 8, -40, (BlockState)Pick.of("purple_stained_glass_pane[east=true,west=true]"));
      this.plot.fill(-1, 3, -40, 1, 7, -40, (BlockState)Pick.of("magenta_stained_glass_pane[east=true,west=true]"));
      this.plot.fill(-4, 1, -40, 4, 1, -40, (BlockState)Pick.of("dark_oak_log[axis=x]"));
      this.plot.fill(-4, 9, -40, 4, 9, -40, (BlockState)Pick.of("dark_oak_log[axis=x]"));

      for(int z = -14; z >= -34; z -= 8) {
         this.plot.put(-4, 0, z, (BlockState)Pick.of("lectern[facing=east,has_book=true,powered=false]"));
         this.plot.put(4, 0, z - 4, (BlockState)Pick.of("lectern[facing=west,has_book=true,powered=false]"));
         this.hang(-3, 6, z - 2, 3);
         this.hang(3, 7, z - 6, 2);
      }

   }

   private void enchanting() {
      this.plot.fill(-5, -1, -6, -1, -1, -3, (BlockState)Pick.of("polished_deepslate"));
      this.plot.put(-3, 0, -4, (BlockState)Pick.of("enchanting_table"));
      BlockState shelf = Pick.of("bookshelf");
      this.plot.fill(-5, 0, -6, -1, 1, -6, (BlockState)shelf);
      this.plot.fill(-5, 0, -5, -5, 1, -3, (BlockState)shelf);
      this.plot.put(-1, 0, -5, (BlockState)shelf);
      this.plot.put(-5, 2, -6, (BlockState)Pick.of("amethyst_cluster[facing=up]"));
      this.plot.put(-4, 2, -6, (BlockState)Pick.of("purple_candle[candles=3,lit=true]"));
      this.plot.put(-3, 2, -6, new String[]{"dustydecorations:book_stack[facing=south]", "large_amethyst_bud[facing=up]"});
      this.plot.put(-2, 2, -6, (BlockState)Pick.of("amethyst_cluster[facing=up]"));
      this.plot.put(-1, 2, -6, (BlockState)Pick.of("white_candle[candles=2,lit=true]"));
      this.plot.put(-5, 2, -4, (BlockState)Pick.of("candle[candles=4,lit=true]"));
      this.plot.put(-5, 2, -3, (BlockState)Pick.of("medium_amethyst_bud[facing=up]"));
      this.plot.put(-1, 1, -5, (BlockState)Pick.of("purple_candle[candles=1,lit=true]"));
      this.plot.put(-4, 0, -3, (BlockState)Pick.of("small_amethyst_bud[facing=up]"));
      this.plot.put(-2, 0, -5, (BlockState)Pick.of("amethyst_cluster[facing=up]"));
   }

   private void desk() {
      this.plot.fill(3, 0, -4, 4, 0, -4, (BlockState)Pick.of("adorn:dark_oak_table", "another_furniture:dark_oak_table[facing=north,leg_1=true,leg_2=true,leg_3=true,leg_4=true]", "dark_oak_fence"));
      if (!Pick.has("adorn:dark_oak_table") && !Pick.has("another_furniture:dark_oak_table")) {
         this.plot.fill(3, 0, -4, 4, 0, -4, (BlockState)Pick.of("dark_oak_slab[type=top]"));
      }

      this.plot.put(3, 1, -4, (BlockState)Pick.of("brewing_stand[has_bottle_0=true,has_bottle_1=false,has_bottle_2=true]"));
      this.plot.put(4, 1, -4, new String[]{"dustydecorations:ink_and_quill[facing=south]", "white_candle[candles=3,lit=true]"});
      this.plot.put(2, 0, -4, (BlockState)Pick.of("lectern[facing=east,has_book=true,powered=false]"));
      this.plot.put(3, 0, -3, new String[]{"another_furniture:dark_oak_chair[facing=north,tucked=false,variant=1]", "adorn:dark_oak_chair[facing=north,half=lower]", "dark_oak_stairs[facing=south,half=bottom,shape=straight]"});
      this.plot.put(5, 0, -3, new String[]{"dustydecorations:vintage_globe[facing=west]", "dustydecorations:globe[facing=west]", "flower_pot"});
      this.plot.put(4, 0, -2, (BlockState)Pick.of("water_cauldron[level=2]"));
      this.plot.put(2, 1, -5, new String[]{"dustydecorations:scattered_papers[facing=south]", "air"});
      this.plot.put(5, 1, -4, (BlockState)Pick.of("candle[candles=2,lit=true]"));
   }

   private void shelves() {
      BlockState shelf = Pick.of("bookshelf");
      String full = "chiseled_bookshelf[facing=south,slot_0_occupied=true,slot_1_occupied=true,slot_2_occupied=true,slot_3_occupied=false,slot_4_occupied=true,slot_5_occupied=true]";
      String some = "chiseled_bookshelf[facing=south,slot_0_occupied=false,slot_1_occupied=true,slot_2_occupied=true,slot_3_occupied=true,slot_4_occupied=false,slot_5_occupied=true]";
      this.plot.fill(3, 0, -6, 7, 3, -6, (BlockState)shelf);
      this.plot.put(4, 1, -6, (BlockState)Pick.of("chiseled_bookshelf[facing=south,slot_0_occupied=true,slot_1_occupied=true,slot_2_occupied=true,slot_3_occupied=false,slot_4_occupied=true,slot_5_occupied=true]"));
      this.plot.put(6, 2, -6, (BlockState)Pick.of("chiseled_bookshelf[facing=south,slot_0_occupied=false,slot_1_occupied=true,slot_2_occupied=true,slot_3_occupied=true,slot_4_occupied=false,slot_5_occupied=true]"));
      this.plot.put(3, 3, -6, (BlockState)Pick.of("chiseled_bookshelf[facing=south,slot_0_occupied=true,slot_1_occupied=true,slot_2_occupied=true,slot_3_occupied=false,slot_4_occupied=true,slot_5_occupied=true]"));
      this.plot.fill(5, 0, -5, 5, 3, -5, (BlockState)Pick.of("ladder[facing=south]"));
      this.plot.fill(-7, 0, -6, -6, 3, -6, (BlockState)shelf);
      this.plot.put(-7, 2, -6, (BlockState)Pick.of("chiseled_bookshelf[facing=south,slot_0_occupied=false,slot_1_occupied=true,slot_2_occupied=true,slot_3_occupied=true,slot_4_occupied=false,slot_5_occupied=true]"));
      this.plot.fill(-7, 0, -5, -7, 2, -2, (BlockState)shelf);
      this.plot.put(-7, 1, -3, (BlockState)Pick.of("chiseled_bookshelf[facing=south,slot_0_occupied=true,slot_1_occupied=true,slot_2_occupied=true,slot_3_occupied=false,slot_4_occupied=true,slot_5_occupied=true]"));
      this.plot.put(-7, 3, -4, (BlockState)Pick.of("skeleton_skull[rotation=4]"));
      this.plot.put(-7, 3, -2, new String[]{"dustydecorations:books[facing=east]", "candle[candles=1,lit=true]"});
      this.plot.put(7, 4, -6, new String[]{"dustydecorations:book_stack[facing=south]", "amethyst_cluster[facing=up]"});
      this.plot.put(4, 4, -6, (BlockState)Pick.of("amethyst_cluster[facing=up]"));
      this.plot.fill(-1, 0, -6, 1, 0, -6, (BlockState)Pick.of("adorn:dark_oak_drawer[facing=south]", "another_furniture:dark_oak_drawer[facing=south,open=false]", "dark_oak_planks"));
      this.plot.put(-1, 1, -6, new String[]{"dustydecorations:book_stack[facing=south]", "air"});
      this.plot.put(1, 1, -6, new String[]{"dustydecorations:jars[amount=3]", "air"});
      this.hang(-4, 2, -5, 2);
      this.hang(4, 1, -5, 3);
      this.hang(6, 3, 1, 1);
   }

   private void hang(int x, int y, int z, int chain) {
      this.plot.put(x, y, z, Pick.of("soul_lantern[hanging=true]"));

      for(int i = 1; y + i <= Math.max(y + chain, 9); ++i) {
         if (this.plot.empty(x, y + i, z)) {
            this.plot.put(x, y + i, z, Pick.of("chain[axis=y]"));
         }
      }

   }

   private void magic() {
      PivotNode ring = (PivotNode)this.stage.add(new PivotNode());
      ring.at(-3.0F, 1.55F, -4.0F);
      ring.spin(Theme.current().motion() > 0.0F ? 22.0F : 0.0F);

      for(int i = 0; i < 3; ++i) {
         float a = (float)i / 3.0F * ((float)Math.PI * 2F);
         ItemNode book = (ItemNode)this.stage.add(new ItemNode(new ItemStack(Items.ENCHANTED_BOOK)));
         book.size(0.42F).bob(0.05F).pickable(false);
         book.at(Mth.sin(a) * 0.95F, -0.2F + (float)i * 0.08F, Mth.cos(a) * 0.95F).yaw(a * (180F / (float)Math.PI) + 90.0F).attachTo(ring);
         book.tint(-3622680);
      }

      ((FxNode)this.stage.add(FxNode.sparkles(46, 3.4F, 2.4F, 3.2F, 0.05F, -3561217, 17L))).at(-3.0F, 1.2F, -4.4F);
      ((MotesNode)this.stage.add((new MotesNode(36, 4.0F, 3.0F, 3.5F, 0.03F, -1331138305, 18L)).rise(1.8F).sway(1.4F))).at(-3.0F, 0.1F, -4.0F);
      ((FxNode)this.stage.add(FxNode.glow(2.8F, 1184915711).fade(1.7F))).at(-3.0F, 0.012F, -3.6F);
      ((MotesNode)this.stage.add((new MotesNode(30, 8.0F, 3.0F, 5.0F, 0.025F, 1627379616, 19L)).rise(0.5F))).at(2.0F, 0.3F, -4.0F);
   }

   private void candles() {
      float[][] at = new float[][]{{-2.6F, 2.3F, -3.6F}, {-3.7F, 2.0F, -2.4F}, {-4.4F, 2.5F, -5.2F}, {2.8F, 2.4F, -4.4F}, {3.9F, 2.1F, -2.8F}, {4.6F, 2.6F, -5.6F}, {-5.0F, 2.9F, -1.5F}, {5.2F, 2.95F, -1.2F}, {-2.9F, 3.4F, -6.0F}, {-1.8F, 4.2F, -11.0F}, {2.2F, 4.8F, -15.0F}, {-2.6F, 5.4F, -20.0F}, {1.4F, 4.6F, -24.0F}, {-1.2F, 5.8F, -29.0F}, {2.6F, 5.2F, -33.0F}, {0.2F, 6.2F, -36.0F}};
      String[] kinds = new String[]{"candle[candles=1,lit=true]", "white_candle[candles=1,lit=true]", "candle[candles=1,lit=true]"};

      for(int i = 0; i < at.length; ++i) {
         BlocksNode one = new BlocksNode(this.stage.level(), 1, 1, 1);
         one.put(0, 0, 0, Pick.of(kinds[i % kinds.length]));
         one.done().softLit(true);
         this.stage.add(one);
         PivotNode drift = (PivotNode)this.stage.add(new PivotNode());
         float x = at[i][0];
         float y = at[i][1];
         float z = at[i][2];
         float phase = (float)i * 1.3F;
         drift.at(x, y, z);
         one.at(-0.5F, 0.0F, -0.5F).attachTo(drift);
         drift.onFrame((ctx) -> {
            float t = Theme.current().motion() > 0.0F ? ctx.seconds() : 0.0F;
            drift.at(x + Mth.sin(t * 0.31F + phase) * 0.05F, y + Mth.sin(t * 0.7F + phase) * 0.07F, z + Mth.cos(t * 0.27F + phase) * 0.05F);
         });
      }

   }

   private void rig() {
      this.light.key(2.0F, 4.4F, 6.4F).keyColor(0.35F, 0.32F, 0.41F).fill(-1.0F, 0.4F, 0.2F, 0.05F, 0.11F, 0.14F).ambientColor(0.085F, 0.07F, 0.11F).rim(0.78F, 0.58F, 1.0F, 0.26F).wrap(0.5F);
      int table = this.light.lamp(-3.0F, 1.3F, -3.4F, 0.95F, 0.45F, 1.45F, 6.0F);
      this.light.lampFlicker(table, 0.12F);
      int desk = this.light.lamp(3.6F, 1.6F, -3.4F, 1.1F, 0.72F, 0.36F, 4.2F);
      this.light.lampFlicker(desk, 0.22F);
      int floating = this.light.lamp(-2.6F, 2.4F, -2.6F, 0.8F, 0.55F, 0.28F, 4.5F);
      this.light.lampFlicker(floating, 0.2F);
      this.light.lamp(-4.0F, 2.3F, -4.6F, 0.25F, 0.7F, 0.8F, 4.0F);
      this.light.lamp(4.0F, 1.3F, -4.6F, 0.25F, 0.7F, 0.8F, 4.0F);
      this.light.lamp(-5.0F, 2.4F, -5.0F, 0.6F, 0.3F, 0.9F, 4.0F);
      int front = this.light.lamp(0.0F, 2.6F, 2.6F, 0.7F, 0.52F, 0.34F, 7.5F);
      this.light.lampFlicker(front, 0.08F);
      int near = this.light.lamp(-2.5F, 2.6F, -16.0F, 0.85F, 0.6F, 0.34F, 9.0F);
      this.light.lampFlicker(near, 0.15F);
      int far = this.light.lamp(2.5F, 2.6F, -28.0F, 0.8F, 0.56F, 0.32F, 9.0F);
      this.light.lampFlicker(far, 0.15F);
      this.light.lamp(0.0F, 5.0F, -37.5F, 0.55F, 0.45F, 0.95F, 16.0F);
      this.finish.bloom(0.6F).vignette(0.46F).warmth(0.004F).contrast(1.08F).saturation(1.14F).grain(1.5F).shadows(0.006F, -0.004F, 0.022F).highlights(0.02F, 0.012F, -0.006F);
   }
}
