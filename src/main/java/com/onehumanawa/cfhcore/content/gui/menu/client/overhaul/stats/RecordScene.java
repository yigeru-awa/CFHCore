package com.onehumanawa.cfhcore.content.gui.menu.client.overhaul.stats;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Fonts;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.core.stage.Stage;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageFinish;
import com.onehumanawa.cfhcore.content.gui.core.stage.anim.IdleMotion;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.BlockNode;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.BlocksNode;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.EntityNode;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.FxNode;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.HitNode;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.ItemNode;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.MotesNode;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.PivotNode;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.PlayerNode;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.StageNode;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.menu.client.overhaul.pause.Diorama;
import com.onehumanawa.cfhcore.content.gui.menu.client.overhaul.pause.LivePlayer;
import com.onehumanawa.cfhcore.content.gui.menu.client.stats.StatFmt;
import com.onehumanawa.cfhcore.content.gui.menu.client.stats.StatNames;
import com.onehumanawa.cfhcore.content.gui.menu.client.stats.StatsHost;
import com.onehumanawa.cfhcore.content.gui.menu.client.stats.StatsTab;
import com.onehumanawa.cfhcore.content.gui.menu.client.stats.data.StatKind;
import com.onehumanawa.cfhcore.content.gui.menu.client.stats.data.StatsFeed;
import com.onehumanawa.cfhcore.content.gui.menu.client.stats.data.Summary;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

final class RecordScene {
   private static final float FOV = 26.0F;
   private static final float AIR = 1.08F;
   static final float ROUND = 25.0F;
   static final float OVER = 20.0F;
   private static final int GRASS = 9551193;
   private static final int FOLIAGE = 7842607;
   private static final int WATER = 4159204;
   private final Stage stage;
   private final StatsHost host;
   private final PivotNode root;
   private final Anim pop;
   private final @Nullable BlocksNode land;
   private final float feet;
   private final float seat;
   private final float boxSide;
   private final float boxBack;
   private final float boxFront;
   private final float boxBottom;
   private final float boxTop;
   private final List<StageNode> made;
   private final List<Thing> things;
   private @Nullable StageNode figure;
   private String figureKey;
   private String thingsKey;
   private Summary summary;
   private final Vector3f point;
   private final Vector3f ndc;

   RecordScene(Screen owner, StatsHost host, Summary first) {
      this.pop = new Anim(0.0F, 620, Ease.OUT_BACK);
      this.made = new ArrayList();
      this.things = new ArrayList();
      this.figureKey = "";
      this.thingsKey = "";
      this.point = new Vector3f();
      this.ndc = new Vector3f();
      Minecraft mc = Minecraft.getInstance();
      Theme theme = Theme.current();
      this.host = host;
      this.summary = first;
      Stage s = (new Stage()).bind(owner);
      this.stage = s;
      s.background(0);
      s.soft(true);
      s.finish(StageFinish.glow());
      s.resolutionScale(mc.getWindow().getWidth() <= 2048 ? 2.0F : 1.5F);
      s.camera().idle(IdleMotion.sway(26000.0F, 5.0F, 0.4F, 0.03F));
      s.lighting().sun(0.55F, 0.85F, 0.45F).ambient(0.62F).key(6.0F, 8.0F, 9.0F).keyColor(0.74F, 0.66F, 0.54F).ambientColor(0.42F, 0.42F, 0.46F).fill(-1.0F, 0.3F, 0.3F, 0.14F, 0.18F, 0.26F).rim(1.0F, 0.9F, 0.72F, 0.2F);
      this.root = (PivotNode)s.add(new PivotNode());
      int warm = Colors.lerp(-5688, theme.accent(), theme.isVanilla() ? 0.0F : 0.3F);
      BlocksNode ground = null;
      float standing = 0.0F;
      float bottom = -3.0F;
      float top = 2.0F;
      if (mc.player != null && mc.level != null) {
         Diorama.Piece piece = Diorama.cut(s, mc.player, mc.level, 25.0F, LivePlayer.groundY(mc.player));
         if (piece != null) {
            ground = piece.land();
            standing = piece.feet();
            bottom = piece.bottom();
            top = piece.top();
         }
      } else {
         ground = meadow(s);
      }

      this.land = ground;
      this.feet = standing;
      this.seat = mc.player != null && mc.level != null ? LivePlayer.seat(mc.player) : 0.0F;
      if (ground != null) {
         ((BlocksNode)s.add(ground)).at(-4.5F, -3.0F, -5.5F).attachTo(this.root);
         this.boxSide = 4.5F;
         this.boxBack = -5.5F;
         this.boxFront = 4.0F;
         this.boxBottom = bottom;
         this.boxTop = Math.max(top, standing + this.seat + 4.2F);
         ((FxNode)s.add(FxNode.glow(5.6F, Colors.withAlpha(warm, 48)).fade(1.9F))).at(0.0F, -3.6F, -1.0F).attachTo(this.root);
      } else {
         this.boxSide = 3.8F;
         this.boxBack = -2.2F;
         this.boxFront = 3.6F;
         this.boxBottom = -0.2F;
         this.boxTop = standing + this.seat + 4.2F;
         ((FxNode)s.add(FxNode.glow(3.6F, Colors.withAlpha(warm, 88)).fade(1.7F))).at(0.0F, 0.01F, 0.4F).attachTo(this.root);
      }

      ((FxNode)s.add(FxNode.glow(0.62F, 2013265920).fade(1.5F))).at(0.0F, standing + 0.02F, 0.0F).attachTo(this.root);
      ((MotesNode)s.add(new MotesNode(24, 7.0F, 5.0F, 7.0F, 0.03F, 1224730562, 12L))).at(0.0F, 2.2F, -1.0F).attachTo(this.root);
      this.pop.snap(0.0F);
      this.pop.set(1.0F);
      this.root.onFrame((ctx) -> {
         boolean moving = Theme.current().motion() > 0.0F;
         this.root.scale(moving ? Math.max(0.001F, this.pop.get()) : 1.0F);
         this.root.rotate(moving ? 6.0F * Mth.sin(ctx.seconds() * 0.35F) : 0.0F, 0.0F, 0.0F);
      });
      this.show(first);
   }

   Stage stage() {
      return this.stage;
   }

   void close() {
      this.stage.close();
   }

   private static BlocksNode meadow(Stage s) {
      int w = 9;
      int l = 8;
      int deep = 3;
      BlocksNode node = new BlocksNode(s.level(), 9, 9, 8);
      node.fill(0, 0, 0, 8, 0, 7, Blocks.STONE);
      node.fill(0, 1, 0, 8, 1, 7, Blocks.DIRT);
      node.fill(0, 2, 0, 8, 2, 7, Blocks.GRASS_BLOCK);

      for(int y = 3; y < 6; ++y) {
         node.put(1, y, 1, Blocks.OAK_LOG);
      }

      for(int x = 0; x <= 2; ++x) {
         for(int z = 0; z <= 2; ++z) {
            if (x != 1 || z != 1) {
               node.put(x, 5, z, Blocks.OAK_LEAVES);
            }

            if (Math.abs(x - 1) + Math.abs(z - 1) <= 1) {
               node.put(x, 6, z, Blocks.OAK_LEAVES);
            }
         }
      }

      int[][] grass = new int[][]{{4, 0}, {6, 1}, {8, 3}, {3, 2}, {5, 3}, {8, 0}, {2, 4}, {6, 4}, {0, 3}};

      for(int[] at : grass) {
         node.put(at[0], 3, at[1], Blocks.SHORT_GRASS);
      }

      node.put(5, 3, 1, Blocks.POPPY);
      node.put(7, 3, 2, Blocks.DANDELION);
      node.put(3, 3, 0, Blocks.OXEYE_DAISY);
      s.level().tintRegion(node.origin(), 9551193, 7842607, 4159204);
      return node.done();
   }

   void frame(Rect full, Rect visible) {
      float aspectFull = (float)full.w() / (float)Math.max(1, full.h());
      float aspect = (float)Math.max(1, visible.w()) / (float)Math.max(1, visible.h());
      float round = (float)Math.toRadians((double)25.0F);
      float over = (float)Math.toRadians((double)20.0F);
      float ex = Mth.cos(over) * Mth.sin(round);
      float ey = Mth.sin(over);
      float ez = Mth.cos(over) * Mth.cos(round);
      float rx = Mth.cos(round);
      float rz = -Mth.sin(round);
      float ux = -ey * Mth.sin(round);
      float uy = Mth.cos(over);
      float uz = -ey * Mth.cos(round);
      float cx = 0.0F;
      float cy = (this.boxBottom + this.boxTop) / 2.0F;
      float cz = (this.boxBack + this.boxFront) / 2.0F;
      float across = 0.0F;
      float up = 0.0F;
      float near = 0.0F;

      for(int i = 0; i < 8; ++i) {
         float x = ((i & 1) == 0 ? -this.boxSide : this.boxSide) - 0.0F;
         float y = ((i & 2) == 0 ? this.boxBottom : this.boxTop) - cy;
         float z = ((i & 4) == 0 ? this.boxBack : this.boxFront) - cz;
         across = Math.max(across, Math.abs(x * rx + z * rz));
         up = Math.max(up, Math.abs(x * ux + y * uy + z * uz));
         near = Math.max(near, x * ex + y * ey + z * ez);
      }

      float tan = (float)Math.tan(Math.toRadians((double)13.0F));
      float dist = Math.max(up / tan, across / (tan * Math.max(0.2F, aspect))) * 1.08F + near * 0.5F;
      float where = ((float)visible.x() + (float)visible.w() / 2.0F - (float)full.x()) / (float)Math.max(1, full.w()) * 2.0F - 1.0F;
      float aside = where * dist * tan * aspectFull;
      float tx = 0.0F - aside * rx;
      float tz = cz - aside * rz;
      this.stage.camera().at(tx + dist * ex, cy + dist * ey, tz + dist * ez).lookAt(tx, cy, tz).fov(26.0F).clip(0.1F, dist + 60.0F);
   }

   void show(Summary next) {
      this.summary = next;
      StatsFeed feed = this.host.feed();
      Minecraft mc = Minecraft.getInstance();
      boolean live = feed.subject() == null && mc.player != null && mc.level != null;
      String who = live ? "live" : feed.shownId().toString();
      if (!who.equals(this.figureKey)) {
         this.figureKey = who;
         if (this.figure != null) {
            this.stage.remove(this.figure);
         }

         if (live) {
            this.figure = new SelfFigure();
         } else {
            PlayerNode player = feed.subject() == null ? PlayerNode.local() : new PlayerNode(feed.shownId(), feed.shownName());
            player.lookAtCursor(false);
            player.pickable(false);
            player.hoverFeel(0.0F, 1.0F);
            this.figure = player;
         }

         this.stage.add(this.figure).at(0.0F, this.feet, 0.0F).attachTo(this.root);
      }

      ResourceLocation block = next.mostMined == null ? null : next.mostMined.id();
      ResourceLocation prey = next.prey == null ? null : next.prey.id();
      int stones = stones(next.distance);
      String key = String.valueOf(block) + "|" + String.valueOf(prey) + "|" + stones + "|" + (next.played > 0L) + "|" + (next.deaths > 0L) + "|" + live;
      if (!key.equals(this.thingsKey)) {
         this.thingsKey = key;

         for(StageNode n : this.made) {
            this.stage.remove(n);
         }

         this.made.clear();
         this.things.clear();
         if (block != null) {
            this.trophy(block);
         }

         if (prey != null) {
            this.prey(prey);
         }

         if (stones > 0) {
            this.road(stones);
         }

         if (next.played > 0L) {
            this.clock(live ? this.seat : 0.0F);
         }

         if (next.deaths > 0L) {
            this.skull();
         }
      }

      int i = 0;

      for(StageNode n : this.made) {
         if (n.pickable() && i < this.things.size()) {
            n.tooltip((Component)((Thing)this.things.get(i++)).tip().apply(next));
         }
      }

   }

   private <T extends StageNode> T put(T node) {
      this.made.add(node);
      this.stage.add(node).attachTo(this.root);
      return node;
   }

   private static Component line(String key, String value) {
      return Component.translatable("cfhgui.stats.line", new Object[]{Component.translatable(key), value});
   }

   private float ground(int lx, int lz) {
      if (this.land == null) {
         return 0.0F;
      } else {
         int gx = lx + 4;
         int gz = lz + 5;

         int top;
         for(top = this.land.top(gx, gz); top >= 0 && this.land.at(gx, top, gz).getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO).isEmpty(); --top) {
         }

         return top < 0 ? Float.NaN : (float)(top - 3) + 1.0F;
      }
   }

   private @Nullable float[] spot(int[][] places) {
      for(int[] p : places) {
         float y = this.ground(p[0], p[1]);
         if (!Float.isNaN(y) && Math.abs(y - this.feet) <= 1.01F) {
            return new float[]{(float)p[0], y, (float)p[1]};
         }
      }

      return this.land == null ? new float[]{(float)places[0][0], 0.0F, (float)places[0][1]} : null;
   }

   private void trophy(ResourceLocation id) {
      float[] at = this.spot(new int[][]{{-3, 0}, {-2, 1}, {-3, 1}, {-2, 0}, {-3, -1}, {-2, -1}});
      if (at != null) {
         Block block = (Block)BuiltInRegistries.BLOCK.getOptional(id).orElse(null);
         if (block != null && block != Blocks.AIR) {
            BlockState state = block.defaultBlockState();
            StageNode node;
            if (!"minecraft".equals(id.getNamespace()) && (state.hasBlockEntity() || state.getRenderShape() != RenderShape.MODEL)) {
               Item item = block.asItem();
               if (item == Items.AIR) {
                  return;
               }

               node = (new ItemNode(new ItemStack(item))).size(1.0F).spin(14.0F).bob(0.03F);
               node.at(at[0], at[1] + 0.15F, at[2]);
            } else {
               node = (new BlockNode(this.stage.level(), state)).spin(14.0F);
               node.scale(0.8F).at(at[0], at[1] + 0.36F, at[2]);
            }

            ((FxNode)this.put(FxNode.glow(0.8F, 1493164720).fade(1.6F))).at(at[0], at[1] + 0.02F, at[2]);
            this.put(node).named(StatNames.block(id)).onClick(() -> this.host.openTab(StatsTab.BLOCKS));
            this.things.add(new Thing(at[0], at[1] + 1.5F, at[2], Component.translatable("cfhgui.stats.thing.mined"), (s) -> s.mostMined == null ? "" : StatFmt.count(s.mostMined.value()), (s) -> (Component)(s.mostMined == null ? Component.empty() : line("cfhgui.stats.o.most_mined", I18n.get("cfhgui.stats.thing_count", new Object[]{StatNames.block(s.mostMined.id()).getString(), StatFmt.count(s.mostMined.value())})))));
         }
      }
   }

   private void prey(ResourceLocation id) {
      EntityType<?> type = (EntityType)BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null);
      if (type != null && type != EntityType.PLAYER) {
         float[] at = this.spot(new int[][]{{3, -3}, {3, -2}, {2, -3}, {2, -2}, {3, -1}, {2, -1}, {3, 0}});
         if (at != null) {
            EntityNode node = (new EntityNode(this.stage.level(), type)).lookAtCursor(false);
            if (node.entity() == null) {
               node.dispose();
            } else {
               float h = Math.max(0.3F, node.boundsMax().y);
               float w = Math.max(0.3F, node.boundsMax().x * 2.0F);
               float scale = Math.min(1.0F, Math.min(1.9F / h, 1.6F / w));
               node.scale(scale).yaw(14.0F).at(at[0], at[1], at[2]);
               ((EntityNode)this.put(node)).onClick(() -> this.host.openTab(StatsTab.COMBAT));
               this.things.add(new Thing(at[0], at[1] + h * scale + 0.5F, at[2], Component.translatable("cfhgui.stats.thing.prey"), (s) -> s.prey == null ? "" : StatFmt.count(s.prey.value()), (s) -> (Component)(s.prey == null ? Component.empty() : line("cfhgui.stats.o.prey", I18n.get("cfhgui.stats.thing_count", new Object[]{StatNames.entity(s.prey.id()).getString(), StatFmt.count(s.prey.value())})))));
            }
         }
      }
   }

   private static int stones(long cm) {
      if (cm <= 0L) {
         return 0;
      } else {
         double km = (double)cm / (double)100000.0F;
         return km < 0.1 ? 1 : (km < (double)1.0F ? 2 : (km < (double)10.0F ? 3 : (km < (double)100.0F ? 4 : (km < (double)1000.0F ? 5 : 6))));
      }
   }

   private void road(int count) {
      int lx = -3;
      float y = Float.NaN;

      for(int column : new int[]{-3, -2, -4, -1, 0, 1, 2, 3, 4}) {
         float top = this.ground(column, 2);
         if (!Float.isNaN(top) && !(top - this.feet > 1.01F)) {
            if (this.feet - top <= 1.01F) {
               lx = column;
               y = top;
               break;
            }

            if (Float.isNaN(y) || top > y) {
               lx = column;
               y = top;
            }
         }
      }

      if (Float.isNaN(y)) {
         y = this.feet;
      }

      float z = 3.05F;

      for(int i = 0; i < count; ++i) {
         float size = Math.max(0.34F, 0.9F - (float)i * 0.12F);
         BlockNode stone = new BlockNode(this.stage.level(), Blocks.DIRT_PATH.defaultBlockState());
         stone.scale(size).at((float)lx, y - 0.05F - (float)i * 0.13F - size * 0.9375F, z);
         stone.hoverFeel(0.0F, 1.0F);
         this.put(stone);
         z += size * 0.5F + 0.16F + Math.max(0.34F, 0.9F - (float)(i + 1) * 0.12F) * 0.5F;
      }

      float length = z - 2.5F;
      HitNode hit = (new HitNode()).box(1.0F, 1.0F, length);
      hit.at((float)lx, y - 0.9F - (float)count * 0.13F, 2.5F + length / 2.0F);
      ((HitNode)this.put(hit)).named(Component.translatable("cfhgui.stats.o.distance")).onClick(() -> this.host.openTab(StatsTab.TRAVEL));
      this.things.add(new Thing((float)lx, y + 0.5F, 3.05F, Component.translatable("cfhgui.stats.thing.distance"), (s) -> StatFmt.of(StatKind.DISTANCE, s.distance, (ResourceLocation)null).text(), (s) -> line("cfhgui.stats.o.distance", StatFmt.of(StatKind.DISTANCE, s.distance, (ResourceLocation)null).text())));
   }

   private void clock(float raised) {
      ItemNode clock = (new ItemNode(new ItemStack(Items.CLOCK))).size(0.62F).spin(22.0F).bob(0.04F);
      clock.at(0.0F, this.feet + raised + 2.4F, 0.0F);
      ((ItemNode)this.put(clock)).named(Component.translatable("cfhgui.stats.o.played")).onClick(() -> this.host.openTab(StatsTab.GENERAL));
      this.things.add(new Thing(0.0F, this.feet + raised + 3.5F, 0.0F, Component.translatable("cfhgui.stats.thing.played"), (s) -> StatFmt.of(StatKind.TIME, s.played, (ResourceLocation)null).text(), (s) -> line("cfhgui.stats.o.played", StatFmt.of(StatKind.TIME, s.played, (ResourceLocation)null).text())));
   }

   private void skull() {
      float[] at = this.spot(new int[][]{{3, 1}, {2, 1}, {3, 2}, {2, 2}, {3, 0}});
      if (at != null) {
         BlockNode skull = new BlockNode(this.stage.level(), (BlockState)Blocks.SKELETON_SKULL.defaultBlockState().setValue(SkullBlock.ROTATION, 8));
         skull.scale(1.15F).yaw(20.0F).at(at[0], at[1], at[2]);
         skull.bounds(-0.3F, 0.0F, -0.3F, 0.3F, 0.55F, 0.3F);
         ((BlockNode)this.put(skull)).named(Component.translatable("cfhgui.stats.o.deaths")).onClick(() -> this.host.openTab(StatsTab.COMBAT));
         this.things.add(new Thing(at[0], at[1] + 1.05F, at[2], Component.translatable("cfhgui.stats.thing.deaths"), (s) -> StatFmt.count(s.deaths), (s) -> line("cfhgui.stats.o.deaths", StatFmt.count(s.deaths))));
      }
   }

   void labels(GuiGraphics g, Rect full, Rect visible, float alpha, boolean van) {
      if (visible.w() >= 170 && visible.h() >= 170 && !(alpha <= 0.02F) && !(this.pop.get() < 0.6F)) {
         boolean captions = visible.w() >= 250;
         int ink = Colors.scaleAlpha(van ? -1 : Theme.current().palette().text(), alpha);
         int soft = Colors.scaleAlpha(van ? -3618616 : Theme.current().palette().textMuted(), alpha);

         for(Thing t : this.things) {
            String value = (String)t.value().apply(this.summary);
            if (!value.isEmpty()) {
               this.point.set(t.x(), t.y(), t.z());
               this.root.model.transformPosition(this.point);
               if (this.stage.camera().project(this.point.x, this.point.y, this.point.z, this.ndc)) {
                  int px = full.x() + Math.round((this.ndc.x * 0.5F + 0.5F) * (float)full.w());
                  int py = full.y() + Math.round((1.0F - (this.ndc.y * 0.5F + 0.5F)) * (float)full.h());
                  if (px >= visible.x() + 4 && px <= visible.right() - 4 && py >= visible.y() + 12 && py <= visible.bottom() - 4) {
                     Component number = Fonts.heading(value);
                     int w = CFHGuiDraw.width(number);
                     int x = Mth.clamp(px - w / 2, visible.x() + 2, Math.max(visible.x() + 2, visible.right() - w - 2));
                     g.drawString(CFHGuiDraw.font(), number, x, py - 9, ink, true);
                     if (captions) {
                        int cw = CFHGuiDraw.width(t.caption());
                        int cx = Mth.clamp(px - cw / 2, visible.x() + 2, Math.max(visible.x() + 2, visible.right() - cw - 2));
                        g.drawString(CFHGuiDraw.font(), t.caption(), cx, py - 19, soft, true);
                     }
                  }
               }
            }
         }

      }
   }

   private static record Thing(float x, float y, float z, Component caption, Function<Summary, String> value, Function<Summary, Component> tip) {
   }
}
