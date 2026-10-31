package com.onehumanawa.cfhcore.content.gui.core.widget;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Fonts;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public class CFHGuiSectionHeader extends CFHGuiWidget {
   public static final int HEIGHT = 20;
   private boolean collapsed;
   private final int count;
   private final @Nullable Runnable onToggle;
   private final Anim chevron;
   private final int depth;

   public CFHGuiSectionHeader(int x, int y, int width, Component title, int count, boolean collapsed, @Nullable Runnable onToggle, int depth) {
      super(x, y, width, 20, title);
      this.chevron = new Anim(0.0F, 160, Ease.OUT_CUBIC);
      this.count = count;
      this.collapsed = collapsed;
      this.onToggle = onToggle;
      this.depth = depth;
      this.chevron.snap(collapsed ? 1.0F : 0.0F);
      if (onToggle == null) {
         this.active = false;
      }

   }

   public CFHGuiSectionHeader(int x, int y, int width, Component title, int count, boolean collapsed, @Nullable Runnable onToggle) {
      this(x, y, width, title, count, collapsed, onToggle, 0);
   }

   public boolean collapsed() {
      return this.collapsed;
   }

   private void toggle() {
      if (this.onToggle != null) {
         this.collapsed = !this.collapsed;
         this.chevron.set(this.collapsed ? 1.0F : 0.0F);
         CFHGuiSounds.tick();
         this.onToggle.run();
      }
   }

   public void onClick(double mouseX, double mouseY) {
      super.onClick(mouseX, mouseY);
      this.toggle();
   }

   public void playDownSound(SoundManager handler) {
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (!this.active || !this.visible || keyCode != 257 && keyCode != 32 && keyCode != 335) {
         return false;
      } else {
         this.toggle();
         return true;
      }
   }

   protected void renderDark(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      Theme t = Theme.current();
      Palette p = t.palette();
      float a = this.effectiveAlpha();
      int x = this.getX() + this.depth * 8;
      int y = this.getY() + this.enterOffset();
      int w = this.getWidth() - this.depth * 8;
      if (this.hover() > 0.01F) {
         CFHGuiDraw.pixelRound(g, x, y, w, 20, Colors.scaleAlpha(Colors.withAlpha(p.surfaceHover(), 144), a * this.hover()), t.radius());
      }

      int tx = x + 4;
      if (this.onToggle != null) {
         Icon ic = this.chevron.get() > 0.5F ? Icon.CHEVRON_RIGHT : Icon.CHEVRON_DOWN;
         Icons.draw(g, ic, tx, y + 5, 10, Colors.scaleAlpha(p.textMuted(), a));
         tx += 14;
      }

      Component title = Fonts.heading(this.getMessage());
      g.drawString(CFHGuiDraw.font(), title, tx, y + 5 + 1, Colors.scaleAlpha(p.text(), a), false);
      tx += CFHGuiDraw.width(title) + 6;
      if (this.count > 0) {
         String c = Integer.toString(this.count);
         g.drawString(CFHGuiDraw.font(), c, tx, y + 5 + 1, Colors.scaleAlpha(p.textDim(), a), false);
         tx += CFHGuiDraw.width(c) + 8;
      }

      CFHGuiDraw.hline(g, tx, y + 10, Math.max(0, x + w - 4 - tx), Colors.scaleAlpha(p.border(), a));
      CFHGuiDraw.focusRing(g, x, y, w, 20, this.focus() * a);
   }

   protected void renderVanilla(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      float a = this.effectiveAlpha();
      int x = this.getX() + this.depth * 8;
      int y = this.getY() + this.enterOffset();
      int w = this.getWidth() - this.depth * 8;
      int tx = x + 4;
      int fg = Colors.scaleAlpha(!(this.hover() > 0.5F) && !(this.focus() > 0.5F) ? -1 : -96, a);
      if (this.onToggle != null) {
         Icons.draw(g, this.chevron.get() > 0.5F ? Icon.CHEVRON_RIGHT : Icon.CHEVRON_DOWN, tx, y + 5, 10, fg);
         tx += 14;
      }

      Component title = Fonts.heading(this.getMessage());
      g.drawString(CFHGuiDraw.font(), title, tx, y + 5 + 1, fg, true);
      tx += CFHGuiDraw.width(title) + 6;
      if (this.count > 0) {
         String c = Integer.toString(this.count);
         g.drawString(CFHGuiDraw.font(), c, tx, y + 5 + 1, Colors.scaleAlpha(-6250336, a), true);
         tx += CFHGuiDraw.width(c) + 8;
      }

      CFHGuiDraw.hline(g, tx, y + 10, Math.max(0, x + w - 4 - tx), Colors.scaleAlpha(-9474193, a));
      if (this.focus() > 0.5F) {
         CFHGuiDraw.outline(g, x, y, w, 20, Colors.scaleAlpha(-1, a), 0);
      }

   }
}
