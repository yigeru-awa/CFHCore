package com.onehumanawa.cfhcore.content.gui.menu.client.death;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Fonts;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.menu.mixin.DeathScreenAccessor;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

public final class CFHGuiDeathScreen extends DeathScreenBase {
   private static final int MAX_W = 280;
   private static final int PAD = 16;
   private static final int GAP = 8;
   private static final int RESPAWN_H = 24;
   private static final int LEAVE_H = 20;
   private static final int ABOVE = 14;
   private static final int BETWEEN = 6;
   private static final int BELOW = 14;
   private static final float TITLE = 2.0F;
   private static final int TITLE_H = Math.round(18.0F);
   private Rect plate = new Rect(0, 0, 0, 0);

   public CFHGuiDeathScreen(@Nullable Component cause, boolean hardcore, boolean preview) {
      super(cause, hardcore, preview);
   }

   public static @Nullable Screen replacing(@Nullable Screen vanilla) {
      if (vanilla instanceof DeathScreen && !(vanilla instanceof DeathScreenBase)) {
         DeathScreenAccessor opened = (DeathScreenAccessor)vanilla;
         return new CFHGuiDeathScreen(opened.cfhgui$causeOfDeath(), opened.cfhgui$hardcore(), false);
      } else {
         return null;
      }
   }

   public static Screen preview(boolean hardcore) {
      return new CFHGuiDeathScreen(sampleCause(), hardcore, true);
   }

   protected void build() {
      int w = Math.min(this.width - 24, 280);
      int inner = w - 32;
      int facts = this.factRows(inner);
      int fixed = 16 + TITLE_H + (facts > 0 ? 8 + facts * 12 : 0) + 14 + 24 + 6 + 20 + 14;
      List<FormattedCharSequence> lines = this.wrapCause(inner, Mth.clamp((this.height - 16 - fixed - 8) / 10, 0, 6));
      int h = fixed + (lines.isEmpty() ? 0 : 8 + lines.size() * 10);
      int x = (this.width - w) / 2;
      int y = Math.max(8, (this.height - h) / 2);
      this.plate = new Rect(x, y, w, h);
      int cy = y + 16 + TITLE_H;
      if (!lines.isEmpty()) {
         cy += 8;
         this.placeCause(lines, x + 16, cy, inner, true);
         cy += lines.size() * 10;
      }

      int factsY = cy + 8;
      if (facts > 0) {
         cy = factsY + facts * 12;
      }

      cy += 14;
      DeathButton respawn = (DeathButton)this.add(new DeathButton(this, x + 16, cy, inner, 24, this.respawnLabel(), this.respawnIcon(), true, this::respawn));
      DeathButton leave = (DeathButton)this.add(new DeathButton(this, x + 16, cy + 24 + 6, inner, 20, Component.translatable("deathScreen.titleScreen"), Icon.EXIT, false, this::leave));
      this.exits(respawn, leave);
      if (facts > 0) {
         this.placeFacts(x + 16, factsY, inner, true);
      }

   }

   public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      float over = this.veil.get();
      if (Theme.current().isVanilla()) {
         this.vanillaVeil(g, over);
      } else {
         this.shade(g, over, 102);
         this.edges(g, over);
      }

      CFHGuiDraw.floatingPanel(g, this.plate.x(), this.plate.y() + this.slide(), this.plate.w(), this.plate.h(), this.rise.get());
   }

   protected void renderContent(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      Theme t = Theme.current();
      Palette p = t.palette();
      boolean van = t.isVanilla();
      float a = this.rise.get();
      if (!(a <= 0.03F)) {
         int dy = this.slide();
         int ink = Colors.scaleAlpha(van ? -1 : p.text(), a);
         FormattedCharSequence title = CFHGuiDraw.truncate(Fonts.heading(this.getTitle()), Math.round((float)(this.plate.w() - 32) / 2.0F));
         int titleW = Math.round((float)this.font.width(title) * 2.0F);
         g.pose().pushPose();
         g.pose().translate((float)(this.plate.x() + (this.plate.w() - titleW) / 2), (float)(this.plate.y() + 16 + dy), 0.0F);
         g.pose().scale(2.0F, 2.0F, 1.0F);
         this.text(g, title, 0, 0, ink, van);
         g.pose().popPose();
         this.drawCause(g, dy, ink, van);
         this.drawFacts(g, dy, a, van);
      }
   }
}
