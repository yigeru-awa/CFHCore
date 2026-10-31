package com.onehumanawa.cfhcore.content.gui.menu.client.overhaul.death;

import com.mojang.blaze3d.systems.RenderSystem;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Fonts;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSpinner;
import com.onehumanawa.cfhcore.content.gui.menu.client.death.DeathScreenBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

final class DeathRow extends CFHGuiButton {
   private static final ResourceLocation DRAFT_REPORT = ResourceLocation.withDefaultNamespace("icon/draft_report");
   private final DeathScreenBase screen;
   private final boolean respawn;
   private final Component heading;
   private final Component busyHeading;
   private final Anim ready;

   DeathRow(DeathScreenBase screen, int x, int y, int w, int h, Component label, Icon icon, boolean respawn, Runnable onPress) {
      super(x, y, w, h, label, onPress);
      this.ready = new Anim(0.0F, 260, Ease.OUT_CUBIC);
      this.screen = screen;
      this.respawn = respawn;
      this.icon = icon;
      this.heading = Fonts.heading(label);
      this.busyHeading = Fonts.heading(Component.translatable("menu.respawning"));
      if (screen.armed()) {
         this.ready.snap(1.0F);
      }

   }

   protected void renderDark(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      Palette p = Theme.current().palette();
      if (this.respawn) {
         this.draw(g, p.accentInk(), p.accent(), p.text(), p.textMuted(), 70);
      } else {
         this.draw(g, p.danger(), p.danger(), p.danger(), p.textMuted(), 70);
      }

   }

   protected void renderVanilla(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      if (this.respawn) {
         this.draw(g, -1, -1, -1, -3618616, 56);
      } else {
         this.draw(g, -38294, -38294, -38294, -3618616, 56);
      }

   }

   private void draw(GuiGraphics g, int ink, int glow, int lit, int rest, int shine) {
      float a = this.effectiveAlpha();
      if (!(a <= 0.02F)) {
         boolean busy = this.respawn && this.screen.respawning();
         float arm = this.screen.arming();
         this.ready.set(arm >= 1.0F);
         float on = this.ready.get();
         float lift = Math.max(this.hover(), this.focus());
         int x = this.getX();
         int y = this.getY() + this.enterOffset() + Math.round(this.press());
         int w = this.getWidth();
         int h = this.getHeight();
         float key = this.respawn ? on : lift;
         float light = this.respawn ? 0.25F + 0.35F * on + 0.4F * lift : lift;
         if (light > 0.01F) {
            CFHGuiDraw.hgradient(g, x, y + 1, w, h - 2, Colors.withAlpha(glow, Math.round((float)shine * light * a)), Colors.withAlpha(glow, 0));
            CFHGuiDraw.rect(g, x, y + 2, 2, h - 4, Colors.scaleAlpha(ink, light * a));
         }

         int shift = Math.round(lift * 4.0F);
         boolean draft = !this.respawn && Minecraft.getInstance().getReportingContext().hasDraftReport();
         FormattedCharSequence words = CFHGuiDraw.truncate(busy ? this.busyHeading : this.heading, w - 29 - (draft ? 18 : 0));
         if (busy) {
            CFHGuiSpinner.draw(g, x + 9 + shift, y + (h - 12) / 2, 12, Colors.scaleAlpha(ink, a));
         } else {
            Icons.draw(g, this.icon, x + 9 + shift, y + (h - 12) / 2, 12, Colors.scaleAlpha(Colors.lerp(rest, ink, key), a));
         }

         g.drawString(CFHGuiDraw.font(), words, x + 27 + shift, y + (h - 9) / 2 + 1, Colors.scaleAlpha(busy ? rest : Colors.lerp(rest, lit, key), a), true);
         if (this.respawn && !busy && on < 0.99F) {
            CFHGuiDraw.rect(g, x + 9, y + h - 2, Math.round((float)(w - 11) * arm), 1, Colors.scaleAlpha(ink, 0.75F * (1.0F - on) * a));
         }

         if (draft) {
            RenderSystem.enableBlend();
            g.setColor(1.0F, 1.0F, 1.0F, a);
            g.blitSprite(DRAFT_REPORT, x + w - 17, y + (h - 15) / 2 + 1, 15, 15);
            g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.disableBlend();
         }

      }
   }
}
