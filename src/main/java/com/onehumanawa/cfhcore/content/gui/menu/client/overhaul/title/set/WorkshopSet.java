package com.onehumanawa.cfhcore.content.gui.menu.client.overhaul.title.set;

import com.onehumanawa.cfhcore.content.gui.core.stage.node.BlocksNode;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.BurstNode;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.FxNode;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.ItemNode;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.ModelNode;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.MotesNode;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.PivotNode;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.SkyNode;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.SmokeNode;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.StageNode;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.TerrainNode;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.SmokeNode.Kind;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.menu.CFHGuiMenu;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

final class WorkshopSet extends TitleSet {
   private static final float WHEEL_X = -3.6F;
   private static final float WHEEL_Y = 0.5F;
   private static final float WHEEL_Z = -7.0F;
   private static final float BELT_X = -3.6F;
   private static final float BELT_Z = -3.0F;
   private static final int BELT_CELLS = 10;
   private static final int PRESS_CELL = 6;
   private static final float PRESS_Y = 2.0F;
   private static final float SHORT_ROD_OUT = 0.3F;
   private static final float STROKE_S = 2.86F;
   private static final float COG_X = 4.0F;
   private static final float COG_Y = 1.15F;
   private static final float COG_Z = -4.6F;
   private static final float WHEEL_SPEED = -36.0F;
   private static final float ENGINE_SPEED = 72.0F;
   private static final float LINE_SPEED = 48.0F;
   private static final float LINE_Y = 4.5F;
   private static final float LINE_Z = -5.0F;
   private static final int LINE_FROM = -9;
   private static final int LINE_TO = -1;
   private static final int MILL_X = 4;
   private static final int MILL_Y = 9;
   private static final int MILL_Z = -8;
   private static final int SAIL = 4;
   private boolean create;
   private Plot plot;
   private float beltTop;

   public int pool() {
      return 754967216;
   }

   public int dust() {
      return 1156111560;
   }

   protected void dress() {
      this.create = mod("create") && Pick.has("create:andesite_casing") && ModelNode.available(createModel("belt/middle"), true);
      this.beltTop = this.create ? 0.8125F : 0.5625F;
      this.stage.add((new SkyNode(140, 29L)).colors(-15985640, -14273473, -15063503, -15787998, -16315632).stars(0.55F));
      this.plot = new Plot(this.stage, -14, -3, -19, 15, 12, 4);
      this.floor();
      this.walls();
      this.race();
      this.corner();
      this.bench();
      this.housings();
      this.plot.done(this.stage);
      this.surroundings(this.plot, 84, 90, 40, 0.0F, (x, z) -> yard(x, z));
      this.wheel();
      this.belt();
      this.press();
      this.boiler();
      this.mixer();
      this.line();
      this.mill();
      this.crusher();
      this.windmill();
      this.rig();
      this.air();
   }

   private BlockState pick(String createName, String vanilla) {
      return this.create ? Pick.of(createName, vanilla) : Pick.of(vanilla);
   }

   private void floor() {
      BlockState base = this.pick("create:polished_cut_andesite", "polished_andesite");
      BlockState lane = this.pick("create:cut_andesite_bricks", "andesite");
      this.plot.fill(-14, -3, -19, 15, -2, 4, (BlockState)Pick.of("stone"));
      this.plot.scatter(-10, -1, -8, 10, -1, 3, this.rnd, 0.1F, base, this.pick("create:small_andesite_bricks", "stone_bricks"));
      this.plot.fill(-10, -1, 2, 10, -1, 2, (BlockState)lane);
      this.plot.fill(-10, -1, -3, 10, -1, -3, (BlockState)lane);
      this.plot.scatter(-14, -1, -19, 15, -1, -9, this.rnd, 0.35F, Pick.of("gravel"), Pick.of("coarse_dirt"), Pick.of("grass_block"));
      this.plot.scatter(-14, -1, -8, -11, -1, 4, this.rnd, 0.35F, Pick.of("gravel"), Pick.of("coarse_dirt"));
      this.plot.scatter(11, -1, -8, 15, -1, 4, this.rnd, 0.35F, Pick.of("gravel"), Pick.of("coarse_dirt"));
   }

   private static TerrainNode.Column yard(int x, int z) {
      int h = Math.floorMod(x * 73856093 ^ z * 19349663, 100);
      if (h < 22) {
         return new TerrainNode.Column(1, "gravel", -1, "dirt", 0, 0, false, (String)null, (String)null, 0);
      } else {
         return h < 45 ? new TerrainNode.Column(1, "coarse_dirt", -1, "dirt", 0, 0, false, (String)null, (String)null, 0) : new TerrainNode.Column(1, "grass_block_top", -10515899, "dirt", 0, 0, false, (String)null, (String)null, 0);
      }
   }

   private void walls() {
      BlockState brick = Pick.of("bricks");
      BlockState upper = Pick.of("stone_bricks");
      BlockState frame = this.pick("create:industrial_iron_block", "iron_block");
      BlockState dark = Pick.of("deepslate_bricks");
      BlockState window = this.pick("create:framed_glass", "glass");

      for(int y = 0; y <= 5; ++y) {
         this.plot.fill(-10, y, -8, 10, y, -8, (BlockState)(y < 3 ? brick : upper));
         this.plot.fill(-4, y, -8, 4, y, -8, (BlockState)dark);
         this.plot.fill(-10, y, -7, -10, y, 3, (BlockState)brick);
         this.plot.fill(10, y, -7, 10, y, 3, (BlockState)brick);
      }

      for(int x : new int[]{-8, -7, -6, 6, 7, 8}) {
         this.plot.fill(x, 1, -8, x, 4, -8, (BlockState)window);
      }

      for(int x : new int[]{-10, -9, -5, 5, 9, 10}) {
         this.plot.fill(x, 0, -8, x, 5, -8, (BlockState)frame);
      }

      this.plot.fill(-10, 5, -8, 10, 5, -8, (BlockState)frame);
      this.plot.fill(-10, 0, -7, -10, 5, -7, (BlockState)frame);
      this.plot.fill(10, 0, -7, 10, 5, -7, (BlockState)frame);
      this.plot.fillModel(4, 6, -8, 4, 8, -8, this.pick("create:metal_girder[x=false,z=false]", "spruce_fence"));
      this.plot.model(4, 9, -8, (BlockState)this.pick("create:windmill_bearing[facing=south]", "stripped_spruce_wood"));
   }

   private void race() {
      BlockState water = Pick.of("water");
      BlockState rim = this.pick("create:cut_andesite_bricks", "stone_bricks");
      this.plot.fill(-8, -2, -8, -1, -2, -5, (BlockState)Pick.of("stone_bricks"));
      this.plot.fill(-7, -1, -7, -2, -1, -6, (BlockState)water);
      this.plot.fill(-8, -1, -7, -8, -1, -5, (BlockState)rim);
      this.plot.fill(-1, -1, -7, -1, -1, -5, (BlockState)rim);
      this.plot.fill(-8, -1, -5, -1, -1, -5, (BlockState)rim);
      BlockState post = this.pick("create:andesite_pillar[axis=y]", "stripped_spruce_log[axis=y]");
      this.plot.fill(-2, 0, -7, -2, 3, -7, (BlockState)post);
      this.plot.put(-3, 3, -7, (BlockState)water);
      this.plot.put(-3, 3, -6, (BlockState)Pick.of("spruce_trapdoor[half=bottom,facing=north,open=true]"));
      this.plot.put(-4, 3, -7, (BlockState)Pick.of("spruce_slab[type=bottom]"));
      this.plot.fill(-3, 0, -7, -3, 2, -7, (BlockState)Pick.of("water[level=8]"));
      this.plot.model(-3, 4, -7, (BlockState)this.pick("create:fluid_pipe[north=false,south=false,east=false,west=false,up=true,down=true]", "lightning_rod[facing=up]"));
      this.plot.model(-3, 5, -7, (BlockState)this.pick("create:fluid_pipe[north=true,south=false,east=false,west=false,up=false,down=true]", "lightning_rod[facing=up]"));
   }

   private void corner() {
      if (this.create) {
         this.plot.model(-8, 0, -5, new String[]{"create:millstone"});
         this.plot.model(-8, 1, -5, new String[]{"create:chute[facing=down,shape=windowed]", "air"});
         this.plot.model(-10, 0, -1, new String[]{"create:encased_fan[facing=east]"});
         this.plot.put(-9, 0, -1, (BlockState)Pick.of("campfire[lit=true,facing=east]"));
         this.plot.model(-8, 0, -1, new String[]{"create:depot"});
      } else {
         this.plot.put(-8, 0, -5, (BlockState)Pick.of("grindstone[face=floor,facing=east]"));
         this.plot.put(-9, 0, -1, (BlockState)Pick.of("smoker[facing=east,lit=true]"));
         this.plot.put(-8, 0, -1, (BlockState)Pick.of("campfire[lit=true,facing=east]"));
      }

      this.plot.put(-9, 0, -4, new String[]{"dustydecorations:burlap_sack[amount=3]", "adorn:crate", "barrel[facing=up,open=false]"});
      this.plot.put(-9, 0, -3, new String[]{"adorn:crate", "barrel[facing=up,open=false]"});
      this.plot.put(-9, 1, -3, new String[]{"dustydecorations:burlap_sack[amount=2]", "air"});
   }

   private void bench() {
      this.plot.fill(1, 0, -7, 3, 0, -7, (BlockState)Pick.of("adorn:spruce_kitchen_counter[facing=south]", "spruce_planks"));
      this.plot.put(1, 1, -7, (BlockState)Pick.of("anvil[facing=east]"));
      this.plot.put(2, 1, -7, new String[]{"dustydecorations:pots_and_pans[amount=2]", "crafting_table"});
      this.plot.put(3, 1, -7, (BlockState)Pick.of("lantern[hanging=false]"));
      this.plot.put(8, 0, -2, new String[]{"adorn:crate", "dustydecorations:burlap_sack[amount=3]", "barrel[facing=up,open=false]"});
      this.plot.put(8, 0, -1, new String[]{"dustydecorations:burlap_sack[amount=2]", "barrel[facing=north,open=false]"});
      this.plot.put(9, 0, -2, new String[]{"adorn:crate", "barrel[facing=up,open=false]"});
      this.plot.put(9, 1, -2, new String[]{"dustydecorations:coiled_rope[facing=west]", "air"});
   }

   private void housings() {
      if (this.create) {
         this.plot.model(0, 0, -5, new String[]{"create:blaze_burner[blaze=kindled]"});
         this.plot.model(0, 1, -5, new String[]{"create:basin"});
         this.plot.model(0, 3, -5, new String[]{"create:mechanical_mixer"});
         this.plot.model(-1, 4, -5, new String[]{"create:gearbox[axis=z]"});
         this.plot.model(-7, 4, -5, new String[]{"create:gearbox[axis=z]"});
         this.plot.model(-3, 4, -5, new String[]{"create:speedometer[facing=up,axis_along_first=true]"});
      } else {
         this.plot.put(0, 0, -5, (BlockState)Pick.of("water_cauldron[level=3]"));
         this.plot.put(1, 0, -5, (BlockState)Pick.of("brewing_stand"));
         this.plot.put(7, 0, -7, (BlockState)Pick.of("blast_furnace[facing=south,lit=true]"));
         this.plot.fill(7, 1, -7, 7, 2, -7, (BlockState)Pick.of("waxed_copper_block"));
         this.plot.put(7, 3, -7, (BlockState)Pick.of("lightning_rod[facing=up]"));
         this.plot.fill(8, 2, -7, 9, 2, -7, (BlockState)Pick.of("lightning_rod[facing=east]"));
         this.plot.put(6, 0, -7, (BlockState)Pick.of("waxed_copper_bulb[lit=true]"));
      }

   }

   private static ResourceLocation createModel(String path) {
      return ResourceLocation.fromNamespaceAndPath("create", "block/" + path);
   }

   private ModelNode partial(String path) {
      return quiet((ModelNode)this.stage.add((new ModelNode(createModel(path))).gameModel(true)));
   }

   private ModelNode block(String state) {
      return quiet((ModelNode)this.stage.add(ModelNode.of(Pick.of(state))));
   }

   private ModelNode standIn(String model) {
      return quiet((ModelNode)this.stage.add(new ModelNode(model.contains(":") ? ResourceLocation.parse(model) : CFHGuiMenu.id(model))));
   }

   private static ModelNode quiet(ModelNode m) {
      m.pickable(false).hoverFeel(0.0F, 1.0F);
      return m;
   }

   private void cogwheel(boolean large, float x, float y, float z, char axis, float speed, float phase) {
      if (this.create) {
         this.axle(this.block(large ? "create:large_cogwheel[axis=y]" : "create:cogwheel[axis=y]"), x, y, z, axis, speed, phase);
      } else {
         this.axle(this.shaft(1), x, y, z, axis, speed, phase);
         PivotNode pivot = this.joint((StageNode)null, x, y, z);
         switch (axis) {
            case 'x' -> pivot.rotate(0.0F, 0.0F, -90.0F);
            case 'z' -> pivot.rotate(0.0F, 90.0F, 0.0F);
            default -> pivot.rotate(0.0F, 0.0F, 0.0F);
         }

         PivotNode turned = this.joint(pivot, 0.0F, 0.0F, 0.0F);
         turned.rotate(phase, 0.0F, 0.0F);
         BlocksNode gear = BlockGear.build(this.stage.level(), large, true);
         this.stage.add(gear);
         gear.scale(BlockGear.scale(large));
         gear.pickable(false).hoverFeel(0.0F, 1.0F);
         gear.attachTo(turned);
         gear.spin(speed * motion());
      }
   }

   private ModelNode shaft(int length) {
      ModelNode m = this.create ? this.block("create:shaft[axis=y]") : this.standIn("minecraft:block/stripped_spruce_log");
      if (!this.create) {
         m.scale(0.28F, 1.0F, 0.28F);
      }

      return m.repeat(length, 0.0F, 1.0F, 0.0F);
   }

   private PivotNode axle(ModelNode model, float x, float y, float z, char axis, float speed, float phase) {
      PivotNode pivot = this.joint((StageNode)null, x, y, z);
      switch (axis) {
         case 'x' -> pivot.rotate(0.0F, 0.0F, -90.0F);
         case 'z' -> pivot.rotate(0.0F, 90.0F, 0.0F);
         default -> pivot.rotate(0.0F, 0.0F, 0.0F);
      }

      model.at(0.0F, -0.5F, 0.0F).attachTo(pivot);
      model.spinAngle(phase);
      model.spin(speed * motion());
      return pivot;
   }

   private PivotNode joint(@Nullable StageNode parent, float x, float y, float z) {
      PivotNode p = (PivotNode)this.stage.add(new PivotNode());
      p.at(x, y, z);
      if (parent != null) {
         p.attachTo(parent);
      }

      return p;
   }

   private static float motion() {
      return Theme.current().motion() > 0.0F ? 1.0F : 0.0F;
   }

   private static float ease(float t) {
      float u = Mth.clamp(t, 0.0F, 1.0F);
      return u * u * (3.0F - 2.0F * u);
   }

   private Item item(String createId, Item vanilla) {
      if (!this.create) {
         return vanilla;
      } else {
         Item it = (Item)BuiltInRegistries.ITEM.get(ResourceLocation.parse(createId));
         return it == Items.AIR ? vanilla : it;
      }
   }

   private void wheel() {
      if (this.create && ModelNode.available(createModel("water_wheel/wheel"), true)) {
         this.axle(this.partial("water_wheel/wheel"), -3.6F, 0.5F, -7.0F, 'z', -36.0F, 0.0F);
      } else {
         BlocksNode wheel = new BlocksNode(this.stage.level(), 7, 1, 7);
         BlockState rim = Pick.of("spruce_planks");
         BlockState spoke = Pick.of("stripped_spruce_log[axis=x]");
         BlockState spokeZ = Pick.of("stripped_spruce_log[axis=z]");
         BlockState hub = Pick.of("spruce_log[axis=y]");
         BlockState paddle = Pick.of("spruce_slab[type=bottom]");

         for(int i = 0; i < 7; ++i) {
            for(int k = 0; k < 7; ++k) {
               int dx = i - 3;
               int dz = k - 3;
               int r2 = dx * dx + dz * dz;
               if (r2 >= 7 && r2 <= 10) {
                  wheel.put(i, 0, k, rim);
               } else if (r2 >= 11 && r2 <= 13 && (dx == 0 || dz == 0 || Math.abs(dx) == Math.abs(dz))) {
                  wheel.put(i, 0, k, paddle);
               }
            }
         }

         for(int d = -2; d <= 2; ++d) {
            wheel.put(3 + d, 0, 3, spoke);
            wheel.put(3, 0, 3 + d, spokeZ);
         }

         wheel.put(3, 0, 3, hub);
         wheel.done().softLit(true);
         this.stage.add(wheel);
         wheel.meshOffset(-3.5F, -0.5F, -3.5F);
         PivotNode pivot = this.joint((StageNode)null, -3.6F, 0.5F, -7.0F);
         pivot.rotate(0.0F, 90.0F, 0.0F).scale(0.3F);
         wheel.at(0.0F, 0.0F, 0.0F).attachTo(pivot);
         wheel.spin(-36.0F * motion());
      }

      this.axle(this.shaft(Math.round(4.0F) - 1), -3.6F, 0.5F, -6.0F, 'z', -36.0F, 0.0F);
   }

   private void belt() {
      PivotNode frame = this.joint((StageNode)null, -3.6F, 0.0F, -3.0F);
      frame.rotate(90.0F, 0.0F, 0.0F);
      if (this.create) {
         belting(this.partial("belt/start"), false).at(0.0F, 0.0F, 0.0F).attachTo(frame);
         belting(this.partial("belt/start_bottom"), true).at(0.0F, 0.0F, 0.0F).attachTo(frame);
         belting(this.partial("belt/middle"), false).repeat(8, 0.0F, 0.0F, 1.0F).at(0.0F, 0.0F, 1.0F).attachTo(frame);
         belting(this.partial("belt/middle_bottom"), true).repeat(8, 0.0F, 0.0F, 1.0F).at(0.0F, 0.0F, 1.0F).attachTo(frame);
         belting(this.partial("belt/end"), false).at(0.0F, 0.0F, 9.0F).attachTo(frame);
         belting(this.partial("belt/end_bottom"), true).at(0.0F, 0.0F, 9.0F).attachTo(frame);

         for(int end : new int[]{0, 9}) {
            PivotNode axis = this.joint(frame, 0.0F, 0.5F, (float)end);
            axis.rotate(0.0F, 0.0F, 90.0F);
            ModelNode pulley = this.partial("belt_pulley");
            pulley.at(0.0F, -0.5F, 0.0F).attachTo(axis);
            pulley.spin(-36.0F * motion());
         }

         this.block("create:depot").at(6.4F, 0.0F, -3.0F);
      } else {
         this.standIn("minecraft:block/dark_oak_slab").repeat(10, 0.0F, 0.0F, 1.0F).at(0.0F, 0.0F, 0.0F).attachTo(frame);
         this.standIn("minecraft:block/black_carpet").repeat(10, 0.0F, 0.0F, 1.0F).at(0.0F, 0.5F, 0.0F).attachTo(frame);
      }

      this.cargo();
   }

   private static ModelNode belting(ModelNode piece, boolean bottom) {
      ResourceLocation strip = createTexture("belt_scroll");
      return piece.scroll(createTexture(bottom ? "belt_offset" : "belt"), strip, -0.34965035F, bottom ? 0.5F : 0.0F);
   }

   private static ResourceLocation createTexture(String name) {
      return ResourceLocation.fromNamespaceAndPath("create", "block/" + name);
   }

   private void cargo() {
      ItemStack before = new ItemStack(Items.IRON_INGOT);
      ItemStack after = new ItemStack(this.item("create:iron_sheet", Items.HEAVY_WEIGHTED_PRESSURE_PLATE));
      PivotNode[] carriers = new PivotNode[10];
      ItemNode[] items = new ItemNode[10];

      for(int i = 0; i < 10; ++i) {
         carriers[i] = this.joint((StageNode)null, 0.0F, 0.0F, 0.0F);
         carriers[i].yaw(this.rnd.nextFloat() * 50.0F - 25.0F);
         items[i] = (ItemNode)this.stage.add(new ItemNode(before));
         items[i].size(0.46F).pickable(false);
         items[i].tint(-3617584);
         items[i].at(0.0F, 0.0F, 0.5F).rotate(0.0F, -90.0F, 0.0F).attachTo(carriers[i]);
      }

      float top = this.beltTop + 0.03F;
      PivotNode clock = this.joint((StageNode)null, 0.0F, 0.0F, 0.0F);
      clock.onFrame((ctx) -> {
         float t = ctx.seconds() * motion();

         for(int i = 0; i < 10; ++i) {
            float s = Mth.positiveModulo(6.5F + (t - 0.3F) / 2.86F + (float)i, 10.0F) - 0.5F;
            float shown = ease((s + 0.45F) / 0.5F) * ease((9.45F - s) / 0.5F);
            carriers[i].at(-3.6F + s, top, -3.0F);
            items[i].size(0.46F * Math.max(0.01F, shown));
            ItemStack now = s >= 5.999F ? after : before;
            if (items[i].stack() != now) {
               items[i].stack(now);
            }
         }

      });
   }

   private void press() {
      float px = 2.4F;
      ModelNode head;
      ModelNode shortHead;
      float top;
      if (this.create) {
         top = 2.0F;
         this.block("create:mechanical_press[facing=south]").at(2.4F, 2.0F, -3.0F);
         head = this.partial("mechanical_press/head");
         shortHead = null;
         this.axle(this.shaft(2), 2.4F, 2.5F, -4.0F, 'z', 48.0F, 0.0F);
         this.block("create:gearbox[axis=x]").at(2.4F, 2.0F, -5.0F);
         this.block("create:metal_girder[x=false,z=false]").repeat(2, 0.0F, 1.0F, 0.0F).at(2.4F, 0.0F, -5.0F);
      } else {
         top = this.beltTop + 1.0F;
         BlocksNode body = new BlocksNode(this.stage.level(), 1, 1, 1);
         body.put(0, 0, 0, Pick.of("piston[facing=down,extended=true]"));
         body.done().softLit(true);
         ((BlocksNode)this.stage.add(body)).at(1.9000001F, top, -3.5F);
         head = this.standIn("minecraft:block/piston_head");
         shortHead = this.standIn("minecraft:block/piston_head_short");
      }

      float travel = top - this.beltTop;
      PivotNode stroke = this.joint((StageNode)null, 2.4F, top, -3.0F);
      head.at(0.0F, 0.0F, 0.0F).attachTo(stroke);
      if (!this.create) {
         head.rotate(0.0F, -90.0F, 0.0F).at(0.0F, 0.5F, 0.5F);
         shortHead.rotate(0.0F, -90.0F, 0.0F).at(0.0F, 0.5F, 0.5F).attachTo(stroke);
      }

      BurstNode sparks = (BurstNode)this.stage.add(new BurstNode(22, 0.5F, 0.045F, -14224, 5L));
      sparks.at(2.4F, this.beltTop + 0.05F, -2.9F);
      int flash = this.light.lamp(2.4F, this.beltTop + 0.3F, -2.4F, 1.4F, 1.0F, 0.55F, 3.5F);
      this.light.lampStrength(flash, 0.0F);
      boolean[] struck = new boolean[]{false};
      stroke.onFrame((ctx) -> {
         float t = motion() > 0.0F ? ctx.seconds() % 2.86F : 0.0F;
         float down = t < 0.3F ? (float)Math.pow((double)(t / 0.3F), (double)3.0F) : (t < 0.45F ? 1.0F : (t < 1.15F ? 1.0F - ease((t - 0.45F) / 0.7F) : 0.0F));
         stroke.at(2.4F, top - travel * down, -3.0F);
         if (shortHead != null) {
            boolean near = travel * down < 0.3F;
            shortHead.visible(near);
            head.visible(!near);
         }

         if (t >= 0.3F && t < 0.45F) {
            if (!struck[0]) {
               struck[0] = true;
               sparks.fire();
            }
         } else if (t > 0.45F) {
            struck[0] = false;
         }

         this.light.lampStrength(flash, t >= 0.3F && t < 0.9F ? Math.max(0.0F, 1.0F - (t - 0.3F) / 0.6F) : 0.0F);
      });
   }

   private void boiler() {
      float ex = 6.0F;
      float ey = 1.15F;
      float ez = -6.6F;
      float base = 0.65F;
      this.cogwheel(true, 4.0F, 1.15F, -4.6F, 'z', 72.0F, 7.5F);
      ModelNode crankShaft = this.shaft(this.create ? 2 : 3);
      this.axle(crankShaft, 4.0F, 1.15F, this.create ? -6.6F : -7.5F, 'z', 72.0F, 0.0F);
      if (this.create) {
         this.block("create:blaze_burner[blaze=kindled]").at(7.0F, -0.35000002F, -6.6F);
         this.block("create:fluid_tank[bottom=true,top=false,shape=window]").at(7.0F, 0.65F, -6.6F);
         this.block("create:fluid_tank[bottom=false,top=false,shape=window]").at(7.0F, 1.65F, -6.6F);
         this.block("create:fluid_tank[bottom=false,top=true,shape=window]").at(7.0F, 2.65F, -6.6F);
         this.block("create:steam_whistle[facing=up,wall=false,size=medium,powered=false]").at(7.0F, 3.65F, -6.6F);
         this.blaze(7.0F, -0.35000002F, -6.6F);
         this.block("create:steam_engine[face=wall,facing=west]").at(6.0F, 0.65F, -6.6F);
         this.engine(6.0F, 1.15F, -6.6F, crankShaft);
      }
   }

   private void engine(float x, float y, float z, ModelNode shaft) {
      PivotNode frame = this.joint((StageNode)null, x, y, z);
      frame.rotate(-90.0F, 90.0F, 0.0F);
      PivotNode piston = this.joint(frame, 0.0F, 0.0F, 0.0F);
      this.partial("steam_engine/piston").at(0.0F, -0.5F, 0.0F).attachTo(piston);
      PivotNode swing = this.joint(frame, 0.0F, 0.75F, 0.0F);
      this.partial("steam_engine/linkage").at(0.0F, -0.25F, 0.0F).attachTo(swing);
      PivotNode crank = this.joint(frame, 0.0F, 2.0F, 0.0F);
      this.partial("steam_engine/shaft_connector").at(0.0F, -0.5F, 0.0F).attachTo(crank);
      frame.onFrame((ctx) -> {
         float angle = (float)Math.toRadians((double)(-shaft.spinAngle() - 90.0F));
         float sine = Mth.sin(angle);
         float sine2 = Mth.sin(angle - ((float)Math.PI / 2F));
         float out = (1.0F - sine) / 4.0F * 24.0F / 16.0F;
         piston.at(0.0F, out, 0.0F);
         swing.at(0.0F, 0.75F + out, 0.0F).rotate(0.0F, sine2 * 23.0F, 0.0F);
         crank.rotate(0.0F, (float)(-Math.toDegrees((double)(angle + ((float)Math.PI / 2F)))), 0.0F);
      });
   }

   private void blaze(float x, float y, float z) {
      PivotNode bob = this.joint((StageNode)null, x, y, z);
      this.partial("blaze_burner/blaze/active").glow(0.8F).at(0.0F, 0.0F, 0.0F).attachTo(bob);
      bob.onFrame((ctx) -> {
         float t = ctx.seconds() * motion();
         bob.at(x, y + (Mth.sin(t * 2.4F) + 1.0F) / 32.0F, z).yaw(Mth.sin(t * 0.35F) * 25.0F);
      });
      ((FxNode)this.stage.add(FxNode.glow(1.4F, 1627363900).fade(1.4F))).at(x, Math.max(0.012F, y + 0.05F), z);
   }

   private void mixer() {
      if (this.create) {
         this.blaze(0.0F, 0.0F, -5.0F);
         this.axle(this.partial("cogwheel_shaftless"), 0.0F, 3.5F, -5.0F, 'y', -48.0F, 11.25F);
         this.cogwheel(false, -1.0F, 3.5F, -5.0F, 'y', 48.0F, 0.0F);
         PivotNode pole = this.joint((StageNode)null, 0.0F, 3.0F, -5.0F);
         this.partial("mechanical_mixer/pole").at(0.0F, 0.0F, 0.0F).attachTo(pole);
         PivotNode headAt = this.joint((StageNode)null, 0.0F, 3.0F, -5.0F);
         ModelNode head = this.partial("mechanical_mixer/head");
         head.at(0.0F, 0.0F, 0.0F).attachTo(headAt);
         float cycle = 9.0F;
         headAt.onFrame((ctx) -> {
            float t = motion() > 0.0F ? ctx.seconds() % 9.0F : 0.0F;
            float down = t < 1.0F ? ease(t) : (t < 6.5F ? 1.0F : (t < 7.5F ? 1.0F - ease(t - 6.5F) : 0.0F));
            float offset = 0.4375F + down;
            pole.at(0.0F, 3.0F - offset, -5.0F);
            headAt.at(0.0F, 3.0F - offset, -5.0F);
            head.spin(motion() * (60.0F + 300.0F * down));
         });
      }
   }

   private void line() {
      this.axle(this.shaft(9), -9.0F, 4.5F, -5.0F, 'x', 48.0F, 0.0F);
      if (this.create) {
         float dial = 0.359375F;
         PivotNode face = this.joint((StageNode)null, -3.0F, 4.5F, -5.0F);
         face.rotate(-90.0F, 0.0F, 0.0F);
         this.partial("gauge/speedometer/head").at(0.0F, -0.5F, 0.0F).attachTo(face);
         PivotNode needle = this.joint(face, 0.0F, -0.140625F, -0.140625F);
         this.partial("gauge/dial").at(0.0F, -0.359375F, 0.140625F).attachTo(needle);
         needle.onFrame((ctx) -> needle.rotate(0.0F, -90.0F * (0.45F + 0.03F * Mth.sin(ctx.seconds() * 7.0F) * motion()), 0.0F));
         this.axle(this.shaft(3), -7.0F, 1.5F, -5.0F, 'y', 48.0F, 0.0F);
         this.cogwheel(false, -7.0F, 0.5F, -5.0F, 'y', 48.0F, 0.0F);
      }
   }

   private void crusher() {
      if (this.create && ModelNode.available(Pick.of("create:crushing_wheel[axis=y]"))) {
         float y = 1.5F;
         float z = 1.0F;
         this.axle(this.block("create:crushing_wheel[axis=y]"), -7.0F, 1.5F, 1.0F, 'z', -30.0F, 0.0F);
         this.axle(this.block("create:crushing_wheel[axis=y]"), -5.0F, 1.5F, 1.0F, 'z', 30.0F, 11.0F);
         this.block("create:chute[facing=down,shape=windowed]").at(-6.0F, 2.6F, 1.0F);
      }
   }

   private void mill() {
      if (this.create) {
         this.axle(this.partial("millstone/inner"), -8.0F, 0.5F, -5.0F, 'y', -48.0F, 11.25F);
         PivotNode fan = this.joint((StageNode)null, -10.0F, 0.5F, -1.0F);
         fan.rotate(0.0F, 0.0F, 90.0F);
         PivotNode turning = this.joint(fan, 0.0F, 0.0F, 0.0F);
         turning.spin(240.0F * motion());
         this.partial("encased_fan/propeller").rotate(0.0F, -90.0F, 0.0F).at(0.0F, 0.0F, 0.5F).attachTo(turning);
         ((SmokeNode)this.stage.add((new SmokeNode(Kind.WISP, 14, 24L)).rise(1.6F, 0.2F).spread(0.25F).wind(1.1F, 0.0F).size(0.25F, 0.8F).colors(-2133274424, 9472128))).at(-9.0F, 0.6F, -1.0F);
      }
   }

   private void windmill() {
      PivotNode hub = this.joint((StageNode)null, 4.0F, 9.5F, -8.0F);
      hub.rotate(0.0F, 90.0F, 0.0F);
      PivotNode turning = this.joint(hub, 0.0F, 0.0F, 0.0F);
      turning.spin(14.0F * motion());
      if (this.create) {
         this.partial("bearing/top").at(0.0F, -0.5F, 0.0F).attachTo(turning);
      }

      int r = 4;
      BlocksNode sails = new BlocksNode(this.stage.level(), 9, 1, 9);
      BlockState sail = this.pick("create:white_sail[facing=up]", "white_carpet");
      BlockState frame = this.pick("create:sail_frame[facing=up]", "spruce_trapdoor[half=bottom,open=false]");

      for(int i = 1; i <= 4; ++i) {
         BlockState s = i == 1 ? frame : sail;
         sails.put(4 + i, 0, 4, s);
         sails.put(4 - i, 0, 4, s);
         sails.put(4, 0, 4 + i, s);
         sails.put(4, 0, 4 - i, s);
         if (i >= 2) {
            sails.put(4 + i, 0, 5, s);
            sails.put(4 - i, 0, 3, s);
            sails.put(3, 0, 4 + i, s);
            sails.put(5, 0, 4 - i, s);
         }
      }

      sails.done().softLit(true);
      this.stage.add(sails);
      sails.meshOffset(-4.5F, 0.0F, -4.5F);
      sails.at(0.0F, 0.5F, 0.0F).attachTo(turning);
   }

   private void rig() {
      this.light.key(2.2F, 4.4F, 6.2F).keyColor(0.46F, 0.4F, 0.31F).fill(-1.0F, 0.5F, -0.6F, 0.07F, 0.14F, 0.17F).ambientColor(0.11F, 0.125F, 0.135F).rim(0.7F, 0.85F, 1.0F, 0.2F).wrap(0.45F);
      int burner = this.light.lamp(7.0F, 0.4F, -5.8F, 1.8F, 0.8F, 0.3F, 6.0F);
      this.light.lampFlicker(burner, 0.3F);
      int mixing = this.light.lamp(0.0F, 0.9F, -4.2F, 1.5F, 0.7F, 0.3F, 4.5F);
      this.light.lampFlicker(mixing, 0.25F);
      this.light.lamp(-7.0F, 3.0F, -7.0F, 0.28F, 0.45F, 0.58F, 8.0F);
      this.light.lamp(7.0F, 3.0F, -7.0F, 0.28F, 0.45F, 0.58F, 8.0F);
      this.light.lamp(-2.3999999F, 0.3F, -5.8F, 0.2F, 0.36F, 0.44F, 3.6F);
      int bench = this.light.lamp(2.0F, 1.8F, -6.4F, 0.8F, 0.6F, 0.34F, 4.0F);
      this.light.lampFlicker(bench, 0.06F);
      int fire = this.light.lamp(-8.5F, 0.6F, -1.0F, 1.2F, 0.62F, 0.3F, 3.5F);
      this.light.lampFlicker(fire, 0.4F);
      this.finish.bloom(0.5F).vignette(0.4F).warmth(0.008F).contrast(1.1F).saturation(1.08F).grain(1.3F).shadows(-0.012F, 0.008F, 0.02F).highlights(0.024F, 0.01F, -0.012F);
   }

   private void air() {
      ((SmokeNode)this.stage.add((new SmokeNode(Kind.WISP, 16, 21L)).rise(2.4F, 0.8F).spread(0.4F).wind(-0.12F, 0.1F).size(0.3F, 1.1F).colors(-1863125770, 13160664))).at(7.0F, 4.75F, -6.6F);
      ((SmokeNode)this.stage.add((new SmokeNode(Kind.WISP, 14, 23L)).rise(1.4F, 0.35F).spread(0.7F).wind(0.1F, 0.05F).size(0.3F, 1.0F).colors(1356392688, 11585752))).at(-3.0F, -0.1F, -6.9F);
      ((MotesNode)this.stage.add((new MotesNode(40, 9.0F, 4.0F, 5.0F, 0.03F, 1891623167, 12L)).rise(0.6F))).at(0.0F, 0.2F, -5.5F);
   }
}
