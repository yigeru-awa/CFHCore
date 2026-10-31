package com.onehumanawa.cfhcore.content.gui.menu.client.overhaul.pause;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Fonts;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.core.module.Features;
import com.onehumanawa.cfhcore.content.gui.core.module.KnownModules;
import com.onehumanawa.cfhcore.content.gui.core.screen.CFHGuiScreen;
import com.onehumanawa.cfhcore.content.gui.core.screen.slot.MenuSlots;
import com.onehumanawa.cfhcore.content.gui.core.stage.Stage;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageFinish;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageWidget;
import com.onehumanawa.cfhcore.content.gui.core.stage.anim.IdleMotion;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.BlocksNode;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.FxNode;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.MotesNode;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.PivotNode;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiToasts;
import com.onehumanawa.cfhcore.content.gui.menu.client.pause.PauseActions;
import com.onehumanawa.cfhcore.content.gui.menu.client.pause.PauseVitals;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

public final class OverhaulPauseScreen extends CFHGuiScreen {
   private static final float FOV = 26.0F;
   private static final float AIR = 1.08F;
   private static final float ROUND = 25.0F;
   private static final float OVER = 20.0F;
   private static final int LEAVE_GAP = 8;
   private final Anim open;
   private final Anim pop;
   private final Anim spin;
   private boolean pressedPiece;
   private boolean dragging;
   private double dragFrom;
   private float dragStart;
   private @Nullable Stage stage;
   private Rect stageRect;
   private float boxSide;
   private float boxBack;
   private float boxFront;
   private float boxBottom;
   private float boxTop;
   private int margin;
   private int colW;
   private int headTop;
   private int listBottom;
   private boolean big;

   public OverhaulPauseScreen() {
      super(Component.translatable("menu.game"), (Screen)null);
      this.open = new Anim(0.0F, 320, Ease.OUT_CUBIC);
      this.pop = new Anim(0.0F, 620, Ease.OUT_BACK);
      this.spin = new Anim(0.0F, 260, Ease.OUT_CUBIC);
      this.stageRect = new Rect(0, 0, 0, 0);
      this.boxSide = 0.8F;
      this.boxBack = -0.8F;
      this.boxFront = 0.8F;
      this.boxTop = 1.95F;
      this.showHeader = false;
      this.showBack = false;
   }

   protected void build() {
      if (this.open.target() == 0.0F) {
         this.open.set(1.0F);
      }

      List<PauseActions.Action> actions = PauseActions.actions(this, true);
      this.margin = Mth.clamp(this.width / 22, 12, 40);
      boolean scene = this.width >= 380 && this.height >= 170;
      this.colW = scene ? Mth.clamp(Math.round((float)this.width * 0.34F), 150, 250) : Math.min(this.width - this.margin * 2, 250);
      this.big = this.height >= 300;
      int headH = 14 + Math.round(9.0F * (this.big ? 2.0F : 1.5F)) + 5 + 12 + (this.big ? 14 : 6);
      int rowH = Mth.clamp((this.height - headH - this.margin - 8) / Math.max(1, actions.size()), 15, 24);
      this.headTop = Math.max(6, (this.height - headH - actions.size() * rowH - 8) / 2);
      StageWidget view = null;
      if (scene) {
         int sx = this.margin + this.colW + 8;
         this.stageRect = new Rect(sx, 0, this.width - sx, Math.max(60, this.height - Math.max(10, this.margin / 2) - 32));
         if (this.stage == null || this.stage.isClosed()) {
            this.createScene();
         }

         this.frame(this.stage);
         view = new StageWidget(this.stageRect.x(), this.stageRect.y(), this.stageRect.w(), this.stageRect.h(), this.stage, this.getTitle());
         this.addRenderableOnly(view);
      } else {
         this.stageRect = new Rect(0, 0, 0, 0);
         if (this.stage != null) {
            this.stage.close();
            this.stage = null;
         }
      }

      int y = this.headTop + headH;

      for(PauseActions.Action a : actions) {
         if (a.kind() == PauseActions.Kind.DANGER) {
            y += 8;
         }

         this.add(new Row(this.margin - 9, y, this.colW + 9, rowH, a));
         y += rowH;
      }

      this.listBottom = y;
      if (view != null) {
         this.addWidget(view);
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
      s.camera().at(0.0F + dist * ex, cy + dist * ey, cz + dist * ez).lookAt(0.0F, cy, cz).fov(26.0F).clip(0.1F, dist + 60.0F);
   }

   private void createScene() {
      Minecraft mc = Minecraft.getInstance();
      Theme theme = Theme.current();
      Stage s = (new Stage()).bind(this);
      this.stage = s;
      s.background(0);
      s.soft(true);
      s.finish(StageFinish.glow());
      s.resolutionScale(mc.getWindow().getWidth() <= 2048 ? 2.0F : 1.5F);
      s.camera().idle(IdleMotion.sway(26000.0F, 5.0F, 0.4F, 0.03F));
      s.lighting().sun(0.55F, 0.85F, 0.45F).ambient(0.62F).key(6.0F, 8.0F, 9.0F).keyColor(0.74F, 0.66F, 0.54F).ambientColor(0.42F, 0.42F, 0.46F).fill(-1.0F, 0.3F, 0.3F, 0.14F, 0.18F, 0.26F).rim(1.0F, 0.9F, 0.72F, 0.2F);
      PivotNode root = (PivotNode)s.add(new PivotNode());
      int warm = Colors.lerp(-5688, theme.accent(), theme.isVanilla() ? 0.0F : 0.3F);
      SelfNode self = new SelfNode();
      Diorama.Piece piece = mc.player != null && mc.level != null ? Diorama.cut(s, mc.player, mc.level, 25.0F, LivePlayer.groundY(mc.player)) : null;
      float feet = 0.0F;
      if (piece != null) {
         ((BlocksNode)s.add(piece.land())).at(-4.5F, -3.0F, -5.5F).attachTo(root);
         feet = piece.feet();
         this.boxSide = 4.5F;
         this.boxBack = -5.5F;
         this.boxFront = 2.5F;
         this.boxBottom = piece.bottom();
         this.boxTop = Math.max(piece.top(), feet + self.height());
         ((FxNode)s.add(FxNode.glow(5.6F, Colors.withAlpha(warm, 48)).fade(1.9F))).at(0.0F, -3.6F, -1.0F).attachTo(root);
      } else {
         this.boxSide = Math.max(1.3F, self.reach() + 0.5F);
         this.boxBack = -this.boxSide;
         this.boxFront = this.boxSide;
         this.boxBottom = -0.2F;
         this.boxTop = Math.max(2.1F, self.height() + 0.15F);
         ((FxNode)s.add(FxNode.glow(Math.max(1.5F, self.reach() * 1.6F), Colors.withAlpha(warm, 102)).fade(1.6F))).at(0.0F, 0.01F, 0.0F).attachTo(root);
      }

      ((FxNode)s.add(FxNode.glow(Math.max(0.62F, self.reach() * 1.1F), 2013265920).fade(1.5F))).at(0.0F, feet + 0.02F, 0.0F).attachTo(root);
      ((SelfNode)s.add(self)).at(0.0F, feet, 0.0F).attachTo(root);
      if (Features.present("cfhgui_profile")) {
         Component profile = Component.translatable("cfhgui.overhaul.profile");
         self.named(profile).tooltip(profile).onClick(() -> MenuSlots.open("cfhgui_profile:profile", this));
      } else {
         self.tooltip(Features.lockedTooltip("cfhgui_profile")).onClick(() -> CFHGuiToasts.show(KnownModules.name("cfhgui_profile"), Features.lockedTooltip("cfhgui_profile"), Icon.LOCK));
      }

      ((MotesNode)s.add(new MotesNode(24, 7.0F, 5.0F, 7.0F, 0.03F, 1224730562, 12L))).at(0.0F, 2.2F, -1.0F).attachTo(root);
      this.pop.snap(0.0F);
      this.pop.set(1.0F);
      root.onFrame((ctx) -> {
         boolean moving = Theme.current().motion() > 0.0F;
         root.scale(moving ? Math.max(0.001F, this.pop.get()) : 1.0F);
         root.rotate((moving ? 7.0F * Mth.sin(ctx.seconds() * 0.35F) : 0.0F) + this.spin.get(), 0.0F, 0.0F);
      });
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
      if (this.stage != null && scrollY != (double)0.0F && this.stageRect.contains(mouseX, mouseY)) {
         this.spin.set(this.spin.target() + (float)scrollY * 22.5F);
         return true;
      } else {
         return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
      }
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button == 0 && this.stage != null && this.stageRect.contains(mouseX, mouseY)) {
         this.pressedPiece = true;
         this.dragging = false;
         this.dragFrom = mouseX;
         this.dragStart = this.spin.target();
      }

      return super.mouseClicked(mouseX, mouseY, button);
   }

   public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
      if (button == 0 && this.pressedPiece) {
         if (!this.dragging && Math.abs(mouseX - this.dragFrom) > (double)3.0F) {
            this.dragging = true;
            if (this.stage != null) {
               this.stage.cancelPress();
            }
         }

         if (this.dragging) {
            this.spin.snap(this.dragStart + (float)(mouseX - this.dragFrom) * 1.2F);
            return true;
         }
      }

      return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
   }

   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      if (button == 0 && this.pressedPiece) {
         this.pressedPiece = false;
         if (this.dragging) {
            this.dragging = false;
            return true;
         }
      }

      return super.mouseReleased(mouseX, mouseY, button);
   }

   public void removed() {
      super.removed();
      if (this.stage != null) {
         this.stage.close();
         this.stage = null;
      }

   }

   public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      super.renderBackground(g, mouseX, mouseY, partialTick);
      if (!Theme.current().isVanilla()) {
         float a = this.open.get();
         CFHGuiDraw.hgradient(g, 0, 0, Math.round((float)this.width * 0.62F), this.height, Colors.withAlpha(-16777216, Math.round(150.0F * a)), 0);
         CFHGuiDraw.vignette(g, 0, 0, this.width, this.height, 0.4F * a);
      }
   }

   protected void renderContent(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      Theme t = Theme.current();
      Palette p = t.palette();
      boolean van = t.isVanilla();
      float a = this.open.get();
      if (!(a <= 0.01F)) {
         int slide = Math.round((1.0F - a) * 6.0F);
         int accent = van ? -1 : p.accent();
         int muted = van ? -3618616 : p.textMuted();
         int room = this.stageRect.w() > 0 ? this.colW + this.stageRect.w() / 4 : this.colW;
         int y = this.headTop + slide;
         String kicker = this.getTitle().getString().toUpperCase(Locale.ROOT);
         if (!van) {
            CFHGuiDraw.rect(g, this.margin, y + 3, 10, 2, Colors.scaleAlpha(accent, a));
         }

         g.drawString(this.font, kicker, this.margin + (van ? 0 : 15), y, Colors.scaleAlpha(van ? -2039584 : accent, a), true);
         y += 14;
         float scale = this.big ? 2.0F : 1.5F;
         FormattedCharSequence name = CFHGuiDraw.truncate(Fonts.heading(PauseActions.worldName()), Math.max(40, Math.round((float)room / scale)));
         g.pose().pushPose();
         g.pose().translate((float)this.margin, (float)y, 0.0F);
         g.pose().scale(scale, scale, 1.0F);
         g.drawString(this.font, name, 0, 0, Colors.scaleAlpha(van ? -1 : p.text(), a), true);
         g.pose().popPose();
         y += Math.round(9.0F * scale) + 5;
         MutableComponent state = Component.empty();

         for(Component part : new Component[]{PauseActions.mode(), PauseActions.dimension(), PauseActions.dayTime()}) {
            if (part != null) {
               if (!state.getSiblings().isEmpty()) {
                  state.append(" · ");
               }

               state.append(part);
            }
         }

         g.drawString(this.font, CFHGuiDraw.truncate(state, room), this.margin, y, Colors.scaleAlpha(muted, a), true);
         Component session = PauseActions.session();
         int footY = this.height - Math.max(10, this.margin / 2) - 9;
         if (session != null && footY >= this.listBottom + 8) {
            g.drawString(this.font, session, this.margin, footY, Colors.scaleAlpha(van ? -6250336 : p.textDim(), a), true);
         }

         if (this.stageRect.w() > 0) {
            int cx = this.stageRect.x() + this.stageRect.w() / 2;
            Component position = PauseActions.position();
            Component biome = PauseActions.biome();
            Component where = position;
            if (position != null && biome != null) {
               where = Component.empty().append(position).append(" · ").append(biome);
            } else if (biome != null) {
               where = biome;
            }

            int line = footY;
            if (where != null) {
               g.drawString(this.font, where, cx - this.font.width(where) / 2, footY + slide, Colors.scaleAlpha(muted, a), true);
               line = footY - 14;
            }

            int vitals = PauseVitals.width();
            if (vitals > 0) {
               PauseVitals.draw(g, cx - vitals / 2, line + slide, a, van ? -1 : p.text());
            }
         }

      }
   }

   private static final class Row extends CFHGuiButton {
      private final PauseActions.Kind kind;
      private final Component heading;

      Row(int x, int y, int w, int h, PauseActions.Action a) {
         super(x, y, w, h, a.label(), a.run());
         this.icon = a.icon();
         this.kind = a.kind();
         this.heading = Fonts.heading(a.label());
         if (!a.enabled()) {
            this.enabled(false);
         }

         if (a.locked()) {
            this.locked(a.lockedModule());
         } else if (a.tip() != null) {
            this.tip(a.tip());
         }

      }

      protected void renderDark(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
         Palette p = Theme.current().palette();
         boolean danger = this.kind == PauseActions.Kind.DANGER;
         this.draw(g, danger ? p.danger() : p.accent(), p.text(), p.textMuted(), Colors.withAlpha(p.textDim(), 176), danger ? p.danger() : p.text(), 70);
      }

      protected void renderVanilla(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
         boolean danger = this.kind == PauseActions.Kind.DANGER;
         this.draw(g, danger ? -38294 : -1, -1, -3618616, -7303024, danger ? -38294 : -1, 56);
      }

      private void draw(GuiGraphics g, int accent, int lit, int rest, int off, int over, int glow) {
         float a = this.effectiveAlpha();
         if (!(a <= 0.004F)) {
            boolean primary = this.kind == PauseActions.Kind.PRIMARY;
            boolean usable = this.active && !this.isLocked();
            float lift = Math.max(this.hover(), this.focus());
            int x = this.getX();
            int y = this.getY() + this.enterOffset() + Math.round(this.press());
            int w = this.getWidth();
            int h = this.getHeight();
            float light = !usable ? 0.0F : (primary ? 0.6F + 0.4F * lift : lift);
            if (light > 0.01F) {
               CFHGuiDraw.hgradient(g, x, y + 1, w, h - 2, Colors.withAlpha(accent, Math.round((float)glow * light * a)), Colors.withAlpha(accent, 0));
               CFHGuiDraw.rect(g, x, y + 2, 2, h - 4, Colors.scaleAlpha(accent, light * a));
            }

            int shift = usable ? Math.round(lift * 4.0F) : 0;
            int fg = !usable ? off : (primary ? lit : Colors.lerp(rest, over, lift));
            int ic = !usable ? off : Colors.lerp(primary ? accent : rest, accent, lift);
            Icons.draw(g, this.icon, x + 9 + shift, y + (h - 12) / 2, 12, Colors.scaleAlpha(ic, a));
            FormattedCharSequence text = CFHGuiDraw.truncate(this.heading, w - 27 - (this.isLocked() ? 14 : 2));
            int tx = x + 27 + shift;
            int ty = y + (h - 9) / 2 + 1;
            g.drawString(CFHGuiDraw.font(), text, tx, ty, Colors.scaleAlpha(fg, a), true);
            if (this.isLocked()) {
               Icons.draw(g, Icon.LOCK, tx + CFHGuiDraw.width(text) + 5, y + (h - 8) / 2, 8, Colors.scaleAlpha(fg, a));
            }

         }
      }
   }
}
