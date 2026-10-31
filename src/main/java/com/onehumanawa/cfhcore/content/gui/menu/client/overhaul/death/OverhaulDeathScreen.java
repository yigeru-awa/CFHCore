package com.onehumanawa.cfhcore.content.gui.menu.client.overhaul.death;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Fonts;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.core.stage.Stage;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageFinish;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageWidget;
import com.onehumanawa.cfhcore.content.gui.core.stage.anim.IdleMotion;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.FxNode;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.MotesNode;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.PivotNode;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.menu.CFHGuiMenu;
import com.onehumanawa.cfhcore.content.gui.menu.client.death.DeathScreenBase;
import com.onehumanawa.cfhcore.content.gui.menu.client.pause.PauseActions;
import com.onehumanawa.cfhcore.content.gui.menu.mixin.DeathScreenAccessor;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

public final class OverhaulDeathScreen extends DeathScreenBase {
   private static final float FOV = 26.0F;
   private static final float AIR = 1.08F;
   private static final float ROUND = 25.0F;
   private static final float OVER = 20.0F;
   private static final int LEAVE_GAP = 8;
   private final Anim pop;
   private @Nullable Stage stage;
   private boolean sceneBroken;
   private Rect stageRect;
   private static final float BOX_SIDE = 1.3F;
   private static final float BOX_BACK = -1.3F;
   private static final float BOX_FRONT = 1.3F;
   private static final float BOX_BOTTOM = -0.3F;
   private static final float BOX_TOP = 1.3F;
   private int margin;
   private int colW;
   private int room;
   private int headTop;
   private float scale;

   public OverhaulDeathScreen(@Nullable Component cause, boolean hardcore, boolean preview) {
      super(cause, hardcore, preview);
      this.pop = new Anim(0.0F, 620, Ease.OUT_BACK);
      this.stageRect = new Rect(0, 0, 0, 0);
      this.scale = 2.0F;
   }

   public static @Nullable Screen replacing(@Nullable Screen vanilla) {
      if (vanilla instanceof DeathScreen && !(vanilla instanceof DeathScreenBase)) {
         DeathScreenAccessor opened = (DeathScreenAccessor)vanilla;
         return new OverhaulDeathScreen(opened.cfhgui$causeOfDeath(), opened.cfhgui$hardcore(), false);
      } else {
         return null;
      }
   }

   public static Screen preview(boolean hardcore) {
      return new OverhaulDeathScreen(sampleCause(), hardcore, true);
   }

   protected void build() {
      this.margin = Mth.clamp(this.width / 22, 12, 40);
      boolean scene = this.width >= 380 && this.height >= 170 && !this.sceneBroken;
      if (scene && (this.stage == null || this.stage.isClosed())) {
         try {
            this.createScene();
         } catch (RuntimeException e) {
            CFHGuiMenu.LOGGER.warn("[CFHGui Menu] the respawn screen's scene could not be made, left out: {}", e.toString());
            this.sceneBroken = true;
            scene = false;
         }
      }

      this.colW = scene ? Mth.clamp(Math.round((float)this.width * 0.34F), 150, 250) : Math.min(this.width - this.margin * 2, 250);
      Stage shown = scene ? this.stage : null;
      if (shown != null) {
         int sx = this.margin + this.colW + 8;
         this.stageRect = new Rect(sx, 0, this.width - sx, Math.max(60, this.height - Math.max(10, this.margin / 2)));
         this.frame(shown);
         this.addRenderableOnly(new StageWidget(this.stageRect.x(), this.stageRect.y(), this.stageRect.w(), this.stageRect.h(), shown, this.getTitle()));
      } else {
         this.stageRect = new Rect(0, 0, 0, 0);
         if (this.stage != null) {
            this.stage.close();
            this.stage = null;
         }
      }

      this.room = this.stageRect.w() > 0 ? this.colW + this.stageRect.w() / 4 : this.colW;
      this.scale = this.font.width(Fonts.heading(this.getTitle())) * 2 <= this.room ? 2.0F : 1.0F;
      int titleH = Math.round(9.0F * this.scale);
      int facts = this.factRows(this.colW);
      int below = this.height >= 300 ? 14 : 8;
      int fixed = 14 + titleH + 6 + facts * 12 + below;
      List<FormattedCharSequence> lines = this.wrapCause(this.colW, Mth.clamp((this.height - 12 - fixed - 6 - 30 - 8) / 10, 0, 5));
      int headH = fixed + (lines.isEmpty() ? 0 : lines.size() * 10 + 6);
      int rowH = Mth.clamp((this.height - headH - this.margin - 8) / 2, 15, 24);
      this.headTop = Math.max(6, (this.height - headH - 2 * rowH - 8) / 2);
      int y = this.headTop + 14 + titleH + 6;
      if (!lines.isEmpty()) {
         this.placeCause(lines, this.margin, y, this.colW, false);
         y += lines.size() * 10 + 6;
      }

      int factsY = y;
      y += facts * 12 + below;
      DeathRow respawn = (DeathRow)this.add(new DeathRow(this, this.margin - 9, y, this.colW + 9, rowH, this.respawnLabel(), this.respawnIcon(), true, () -> this.respawn()));
      y += rowH + 8;
      DeathRow leave = (DeathRow)this.add(new DeathRow(this, this.margin - 9, y, this.colW + 9, rowH, Component.translatable("deathScreen.titleScreen"), Icon.EXIT, false, () -> this.leave()));
      this.exits(respawn, leave);
      if (facts > 0) {
         this.placeFacts(this.margin, factsY, this.colW, false);
      }

   }

   private void frame(Stage s) {
      float aspect = (float)this.stageRect.w() / (float)Math.max(1, this.stageRect.h());
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
      float cy = 0.49999997F;
      float cz = 0.0F;
      float across = 0.0F;
      float up = 0.0F;
      float near = 0.0F;

      for(int i = 0; i < 8; ++i) {
         float x = ((i & 1) == 0 ? -1.3F : 1.3F) - 0.0F;
         float y = ((i & 2) == 0 ? -0.3F : 1.3F) - 0.49999997F;
         float z = ((i & 4) == 0 ? -1.3F : 1.3F) - 0.0F;
         across = Math.max(across, Math.abs(x * rx + z * rz));
         up = Math.max(up, Math.abs(x * ux + y * uy + z * uz));
         near = Math.max(near, x * ex + y * ey + z * ez);
      }

      float tan = (float)Math.tan(Math.toRadians((double)13.0F));
      float dist = Math.max(up / tan, across / (tan * Math.max(0.2F, aspect))) * 1.08F + near * 0.5F;
      s.camera().at(0.0F + dist * ex, 0.49999997F + dist * ey, 0.0F + dist * ez).lookAt(0.0F, 0.49999997F, 0.0F).fov(26.0F).clip(0.1F, dist + 60.0F);
   }

   private void createScene() {
      Minecraft mc = Minecraft.getInstance();
      Stage s = (new Stage()).bind(this);
      this.stage = s;
      s.background(0);
      s.soft(true);
      s.finish(StageFinish.glow().saturation(0.94F).warmth(-0.012F).shadows(0.03F, -0.004F, 0.0F).highlights(-0.004F, 0.0F, 0.014F));
      s.resolutionScale(mc.getWindow().getWidth() <= 2048 ? 2.0F : 1.5F);
      s.camera().idle(IdleMotion.sway(26000.0F, 5.0F, 0.4F, 0.03F));
      s.lighting().sun(0.55F, 0.85F, 0.45F).ambient(0.56F).key(6.0F, 8.0F, 9.0F).keyColor(0.52F, 0.54F, 0.62F).ambientColor(0.31F, 0.32F, 0.38F).fill(-1.0F, 0.3F, 0.3F, 0.34F, 0.08F, 0.08F).rim(1.0F, 0.38F, 0.32F, 0.3F);
      PivotNode root = (PivotNode)s.add(new PivotNode());
      float feet = 0.0F;
      ((FxNode)s.add(FxNode.glow(1.7F, 1722821162).fade(1.6F))).at(0.0F, 0.01F, 0.0F).attachTo(root);
      ((FxNode)s.add(FxNode.glow(0.62F, 2013265920).fade(1.5F))).at(0.0F, 0.02F, 0.0F).scale(1.9F, 1.0F, 0.9F).attachTo(root);
      ((FallenNode)s.add(new FallenNode())).at(0.0F, 0.0F, 0.0F).tint(-3159858).attachTo(root);
      s.lighting().lamp(0.3F, 1.1F, 1.6F, 0.7F, 0.13F, 0.1F, 5.5F);
      ((MotesNode)s.add(new MotesNode(18, 7.0F, 5.0F, 7.0F, 0.03F, 1090493580, 12L))).at(0.0F, 2.2F, -1.0F).attachTo(root);
      this.pop.snap(0.0F);
      this.pop.set(1.0F);
      root.onFrame((ctx) -> {
         boolean moving = Theme.current().motion() > 0.0F;
         root.scale(moving ? Math.max(0.001F, this.pop.get()) : 1.0F);
         root.rotate(moving ? 7.0F * Mth.sin(ctx.seconds() * 0.35F) : 0.0F, 0.0F, 0.0F);
      });
   }

   public void removed() {
      super.removed();
      if (this.stage != null) {
         this.stage.close();
         this.stage = null;
      }

   }

   public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      float over = this.veil.get();
      if (Theme.current().isVanilla()) {
         this.vanillaVeil(g, over);
      } else {
         this.shade(g, over, 112);
         CFHGuiDraw.hgradient(g, 0, 0, Math.round((float)this.width * 0.62F), this.height, Colors.withAlpha(0, Math.round(160.0F * over)), 0);
         this.edges(g, over);
      }
   }

   protected void renderContent(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      Theme t = Theme.current();
      Palette p = t.palette();
      boolean van = t.isVanilla();
      float a = this.rise.get();
      if (!(a <= 0.03F)) {
         int dy = this.slide();
         int ink = Colors.scaleAlpha(van ? -1 : p.text(), a);
         int y = this.headTop + dy;
         Component world = Component.literal(PauseActions.worldName().toUpperCase(Locale.ROOT));
         this.text(g, CFHGuiDraw.truncate(world, this.room), this.margin, y, Colors.scaleAlpha(van ? -3618616 : p.textMuted(), a), true);
         y += 14;
         FormattedCharSequence title = CFHGuiDraw.truncate(Fonts.heading(this.getTitle()), Math.max(40, Math.round((float)this.room / this.scale)));
         g.pose().pushPose();
         g.pose().translate((float)this.margin, (float)y, 0.0F);
         g.pose().scale(this.scale, this.scale, 1.0F);
         this.text(g, title, 0, 0, ink, true);
         g.pose().popPose();
         this.drawCause(g, dy, ink, true);
         this.drawFacts(g, dy, a, true);
      }
   }
}
