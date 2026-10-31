package com.onehumanawa.cfhcore.content.gui.core.stage.test;

import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.core.screen.CFHGuiScreen;
import com.onehumanawa.cfhcore.content.gui.core.stage.Stage;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageWidget;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.BlockNode;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.EntityNode;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.FxNode;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.ItemNode;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.ModelNode;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.PlayerNode;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.StageNode;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.TextNode;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.VoxelNode;
import com.onehumanawa.cfhcore.content.gui.core.stage.scene.Anchor;
import com.onehumanawa.cfhcore.content.gui.core.stage.scene.Scene;
import com.onehumanawa.cfhcore.content.gui.core.stage.scene.SceneLoader;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiToasts;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.Nullable;

public final class StageTestScreen extends CFHGuiScreen {
   public static final UUID NOTCH = UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5");
   private static final ItemStack[] ITEMS;
   private final Variant variant;
   private @Nullable Stage stage;
   private @Nullable BlockNode chest;
   private @Nullable StageWidget widget;
   private int itemIndex;
   private boolean signGlow;
   private double savedMotion = Double.NaN;

   public StageTestScreen(@Nullable Screen parent, Variant variant) {
      super(Component.translatable("cfhgui.stage.test.title"), parent);
      this.variant = variant;
   }

   protected void build() {
      if (this.stage == null) {
         this.createScene();
      }

      Rect r = this.contentRect();
      this.widget = (StageWidget)this.add(new StageWidget(r.x(), r.y(), r.w(), r.h(), this.stage, Component.translatable("cfhgui.stage.test.title")));
      if (this.variant == StageTestScreen.Variant.OPEN) {
         this.setInitialFocus(this.widget);
      }

   }

   private void createScene() {
      if (this.variant == StageTestScreen.Variant.MOTION0 && Double.isNaN(this.savedMotion)) {
         this.savedMotion = CFHGui.config().motion;
         CFHGui.config().motion = (double)0.0F;
         Theme.reload();
      }

      this.stage = (new Stage()).bind(this);
      Scene scene = SceneLoader.load(CFHGui.id("stage_test"));
      scene.apply(this.stage);
      Anchor chestAt = scene.anchorOr("chest", 0.0F, 1.0F, 1.5F, 0.0F);
      this.chest = (BlockNode)this.stage.add((BlockNode)chestAt.place(new BlockNode(this.stage.level(), (BlockState)Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.SOUTH))));
      this.chest.tooltip(Component.translatable("cfhgui.stage.test.chest_tip")).named((Component)Component.translatable("cfhgui.stage.test.chest"));
      this.chest.onClick(() -> this.chest.toggle());
      Anchor signAt = scene.anchorOr("sign", 0.0F, 1.0F, 3.3F, 0.0F);
      BlockNode sign = (BlockNode)this.stage.add((BlockNode)signAt.place(new BlockNode(this.stage.level(), (BlockState)Blocks.OAK_SIGN.defaultBlockState().setValue(BlockStateProperties.ROTATION_16, 0))));
      sign.signText(Component.translatable("cfhgui.stage.test.sign1"), Component.translatable("cfhgui.stage.test.sign2"));
      sign.bounds(-0.4F, 0.0F, -0.15F, 0.4F, 1.1F, 0.15F).hoverFeel(0.08F, 1.06F).named((Component)Component.translatable("cfhgui.stage.test.sign"));
      sign.onClick(() -> {
         this.signGlow = !this.signGlow;
         sign.signText(this.signGlow ? DyeColor.ORANGE : DyeColor.BLACK, this.signGlow, Component.translatable("cfhgui.stage.test.sign1"), Component.translatable("cfhgui.stage.test.sign2"));
      });
      Anchor a = scene.anchorOr("player_a", -2.4F, 1.0F, 1.2F, 18.0F);
      PlayerNode me = (PlayerNode)this.stage.add((PlayerNode)a.place(PlayerNode.local()));
      me.pose(PlayerNode.Pose.WAVE).pickable(true).tooltip(Component.translatable("cfhgui.stage.test.player_tip"));
      this.stage.add((TextNode)a.offset(0.0F, 2.25F, 0.0F).place((new TextNode(Component.literal(me.name().getString()))).seeThrough(true)));
      Anchor bAt = scene.anchorOr("player_b", 2.4F, 1.0F, 1.2F, -18.0F);
      PlayerNode notch = (PlayerNode)this.stage.add((PlayerNode)bAt.place(new PlayerNode(NOTCH, "Notch")));
      notch.pose(PlayerNode.Pose.SIT).pickable(true).tooltip(Component.translatable("cfhgui.stage.test.player_tip"));
      this.stage.add((TextNode)bAt.offset(0.0F, 1.75F, 0.0F).place((new TextNode(Component.literal("Notch"))).seeThrough(true)));
      Anchor itemAt = scene.anchorOr("item", -1.6F, 1.15F, -1.2F, 0.0F);
      ItemNode item = (ItemNode)this.stage.add((ItemNode)itemAt.place(new ItemNode(ITEMS[0])));
      item.size(1.5F).spin(40.0F).bob(0.05F).pickable(true).tooltip(Component.translatable("cfhgui.stage.test.item_tip"));
      item.onClick(() -> item.stack(ITEMS[++this.itemIndex % ITEMS.length]));
      this.stage.add((FxNode)itemAt.offset(0.0F, 0.5F, 0.0F).place(FxNode.sparkles(18, 1.4F, 1.4F, 1.4F, 0.12F, Theme.current().accent(), 3L)));
      Anchor cogAt = scene.anchorOr("cog", 1.6F, 1.55F, -1.2F, 0.0F);
      ModelNode cog = (ModelNode)this.stage.add((ModelNode)cogAt.place(new ModelNode(CFHGui.id("stage/cog"))));
      cog.spin(18.0F).pickable(true).tooltip(Component.translatable("cfhgui.stage.test.cog_tip")).named((Component)Component.translatable("cfhgui.stage.test.cog"));
      cog.onHover((h) -> {
         if (h) {
            cog.boostSpin(6.0F);
         }

      });
      cog.onClick(() -> CFHGuiToasts.show(Component.translatable("cfhgui.stage.test.cog"), Component.translatable("cfhgui.stage.test.cog_clicked"), Icon.INFO));
      Anchor wolfAt = scene.anchorOr("wolf", 3.0F, 1.0F, -2.6F, -30.0F);
      EntityNode wolf = (EntityNode)this.stage.add((EntityNode)wolfAt.place(new EntityNode(this.stage.level(), EntityType.WOLF)));
      wolf.tamed(true).sitting(true).pickable(true).tooltip(Component.translatable("cfhgui.stage.test.wolf_tip"));
      Anchor planetAt = scene.anchorOr("planet", -3.6F, 4.2F, -3.2F, 0.0F);
      VoxelNode planet = (VoxelNode)this.stage.add((VoxelNode)planetAt.place(VoxelNode.planet(this.stage.level(), 4, 20260929L)));
      planet.scale(0.42F).spin(6.0F).pickable(true).tooltip(Component.translatable("cfhgui.stage.test.planet_tip"));
      this.stage.add(((FxNode)planetAt.place(FxNode.clouds(5, 7.0F, 7.0F, 0.9F, 0.9F, 11L, 0.12F))).scale(0.42F));
      Anchor islandAt = scene.anchorOr("island", 4.3F, 3.2F, -4.0F, 25.0F);
      VoxelNode island = (VoxelNode)this.stage.add((VoxelNode)islandAt.place(VoxelNode.heightfield(this.stage.level(), 8, 8, 4242L)));
      island.centered();
      island.scale(0.4F).spin(-4.0F).pickable(true).tooltip(Component.translatable("cfhgui.stage.test.island_tip"));
      if (this.variant == StageTestScreen.Variant.OPEN) {
         this.chest.open(true);
         this.stage.focus(this.chest);
      }

   }

   protected void renderContent(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      if (this.stage != null) {
         Rect r = this.contentRect();
         String info = String.format("stage %.2f ms (last %.2f)  fbo %dx%d  nodes %d  motion %.1f  intro %d%%", this.stage.frameMs(), this.stage.lastFrameMs(), this.stage.targetWidth(), this.stage.targetHeight(), this.stage.nodes().size(), Theme.current().motion(), Math.round(this.stage.camera().timeline().progress() * 100.0F));
         int plateW = CFHGuiDraw.width(info) + 8;
         g.fill(r.x() + 2, r.y() + r.h() - 15, r.x() + 2 + plateW, r.y() + r.h() - 2, -1610612736);
         CFHGuiDraw.text(g, info, r.x() + 6, r.y() + r.h() - 12, this.palette().textMuted());
         Component hint = Component.translatable("cfhgui.stage.test.hint");
         int hintW = CFHGuiDraw.width(hint) + 8;
         g.fill(r.x() + r.w() - 2 - hintW, r.y() + 2, r.x() + r.w() - 2, r.y() + 15, -1610612736);
         CFHGuiDraw.textRight(g, hint, r.x() + r.w() - 6, r.y() + 5, this.palette().textMuted());
         StageNode hovered = this.stage.hovered();
         if (hovered != null) {
            CFHGuiDraw.text(g, hovered.name(), r.x() + 6, r.y() + 4, this.palette().text());
         }

      }
   }

   public void removed() {
      super.removed();
      if (this.stage != null) {
         this.stage.close();
         this.stage = null;
      }

      if (!Double.isNaN(this.savedMotion)) {
         CFHGui.config().motion = this.savedMotion;
         this.savedMotion = Double.NaN;
         Theme.reload();
      }

   }

   static {
      ITEMS = new ItemStack[]{new ItemStack(Items.DIAMOND_PICKAXE), new ItemStack(Items.ENCHANTED_GOLDEN_APPLE), new ItemStack(Items.COMPASS), new ItemStack(Items.OAK_SAPLING)};
   }

   public static enum Variant {
      DEFAULT,
      OPEN,
      MOTION0;
   }
}
