package com.onehumanawa.cfhcore.content.gui.menu.client.worlds;

import com.mojang.blaze3d.systems.RenderSystem;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiBadge;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiCard;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiIconButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiTooltips;
import com.onehumanawa.cfhcore.content.gui.menu.client.Fmt;
import com.onehumanawa.cfhcore.content.gui.menu.client.play.model.WorldEntry;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.storage.LevelSummary;

public class WorldCard extends CFHGuiCard {
   public static final int HEIGHT = 74;
   private static final int ICON = 48;
   public static final float HOVER_EDGE = 0.9F;
   private final WorldEntry entry;
   private final CFHGuiIconButton star;
   private final CFHGuiIconButton plate;
   private final Anim play;
   private static final int ROW_TEXT = 19;

   public WorldCard(int x, int y, int w, WorldEntry entry, CFHGuiWorldsScreen screen) {
      super(x, y, w, 74);
      this.play = new Anim(0.0F, 200, Ease.OUT_CUBIC);
      this.entry = entry;
      this.lift(2);
      this.star = (CFHGuiIconButton)this.add(new CFHGuiIconButton(0, 0, 18, entry.favorite ? Icon.STAR_FILLED : Icon.STAR, Component.translatable(entry.favorite ? "cfhgui.unfavorite" : "cfhgui.favorite"), () -> screen.toggleFavorite(entry)), w - 22, 4);
      this.star.toggled(entry.favorite);
      this.plate = (CFHGuiIconButton)this.add(PlayPlates.create(20, () -> screen.play(entry)), 22, 27);
      PlayPlates.label(this.plate, Component.translatable("cfhgui.worlds.play_this", new Object[]{entry.name()}));
      this.plate.active = entry.summary.primaryActionActive();
      this.onClick(() -> screen.clicked(entry));
      this.onRightClick(() -> screen.contextMenu(entry));
   }

   public WorldEntry entry() {
      return this.entry;
   }

   public CFHGuiCard selected(boolean s) {
      super.selected(s);
      if (!s) {
         this.plate.visible = false;
      }

      return this;
   }

   public static void drawSummary(GuiGraphics g, WorldEntry e, int x, int y, int w, int iconSize, boolean twoLine, int mouseX, int mouseY) {
      Theme t = Theme.current();
      Palette p = t.palette();
      boolean van = t.isVanilla();
      LevelSummary s = e.summary;
      e.requestIcon();
      e.requestSize();
      if (e.icon != null) {
         RenderSystem.enableBlend();
         g.blit(e.icon.id(), x, y, iconSize, iconSize, 0.0F, 0.0F, e.icon.width(), e.icon.height(), e.icon.width(), e.icon.height());
         RenderSystem.disableBlend();
      } else {
         CFHGuiDraw.pixelRound(g, x, y, iconSize, iconSize, van ? Integer.MIN_VALUE : p.bg2(), t.radius());
         Icons.draw(g, Icon.WORLD, x + iconSize / 2 - 8, y + iconSize / 2 - 8, 16, van ? -6250336 : p.textDim());
      }

      CFHGuiDraw.outline(g, x, y, iconSize, iconSize, van ? -16777216 : p.border(), t.radius());
      if (s.isLocked() || s.isDisabled() || s.requiresManualConversion()) {
         Icons.draw(g, Icon.WARNING, x + iconSize - 10, y + iconSize - 10, 10, p.warning());
         if (mouseX >= x && mouseX < x + iconSize && mouseY >= y && mouseY < y + iconSize) {
            CFHGuiTooltips.request(List.of(s.isLocked() ? Component.translatable("selectWorld.locked") : s.getInfo()), (AbstractWidget)null);
         }
      }

      int tx = x + iconSize + 8;
      int textW = w - iconSize - 8;
      int muted = van ? -4144960 : p.textMuted();
      int dim = van ? -6250336 : p.textDim();
      if (twoLine) {
         g.drawString(CFHGuiDraw.font(), CFHGuiDraw.truncate(Component.literal(e.name()), textW), tx, y, p.text(), van);
         g.drawString(CFHGuiDraw.font(), CFHGuiDraw.truncate(Component.literal(e.id()), textW), tx, y + 11, dim, van);
         int cy = y + 24;
         int cx = tx + CFHGuiBadge.draw(g, s.getGameMode().getShortDisplayName(), tx, cy, p.accent()) + 3;
         if (s.isHardcore()) {
            cx += CFHGuiBadge.draw(g, Component.translatable("cfhgui.worlds.hardcore"), cx, cy, p.danger()) + 3;
         }

         if (s.hasCommands()) {
            cx += CFHGuiBadge.draw(g, Component.translatable("cfhgui.worlds.cheats"), cx, cy, p.warning()) + 3;
         }

         if (s.isExperimental()) {
            cx += CFHGuiBadge.draw(g, Component.translatable("cfhgui.worlds.experimental"), cx, cy, p.warning()) + 3;
         }

         for(String tag : e.tags) {
            int bw = CFHGuiDraw.width(tag) + 8;
            if (cx + bw > x + w) {
               break;
            }

            cx += CFHGuiBadge.draw(g, Component.literal(tag), cx, cy, van ? -7631989 : p.borderStrong()) + 3;
         }

         String size = e.size >= 0L ? Fmt.bytes(e.size) : (e.size == -3L ? "..." : "");
         Component line = Component.empty().append(Fmt.ago(s.getLastPlayed())).append(" · ").append(s.getWorldVersionName()).append(size.isEmpty() ? "" : " · " + size);
         g.drawString(CFHGuiDraw.font(), CFHGuiDraw.truncate(line, textW), tx, y + 40, muted, van);
      } else {
         int top = y + Math.max(0, (iconSize - 19) / 2);
         g.drawString(CFHGuiDraw.font(), CFHGuiDraw.truncate(Component.literal(e.name()), textW), tx, top, p.text(), van);
         Component line = Component.empty().append(s.getGameMode().getShortDisplayName()).append(" · ").append(Fmt.ago(s.getLastPlayed())).append(" · ").append(s.getWorldVersionName());
         g.drawString(CFHGuiDraw.font(), CFHGuiDraw.truncate(line, textW), tx, top + 11, muted, van);
      }

   }

   protected void renderContent(GuiGraphics g, int x, int y, int w, int h, int mouseX, int mouseY, float partialTick) {
      drawSummary(g, this.entry, x + 8, y + (h - 48) / 2, w - 8 - 30, 48, true, mouseX, mouseY);
      if (this.isSelected() && !Theme.current().isVanilla()) {
         CFHGuiDraw.rect(g, x, y + 6, 2, h - 12, Colors.withAlpha(Theme.current().accent(), 255));
      }

      this.plate.visible = false;
   }

   protected void renderOverlay(GuiGraphics g, int x, int y, int w, int h, int mouseX, int mouseY, float partialTick) {
      float a = this.alpha * this.enterProgress();
      if (!this.isSelected() || !Theme.current().isVanilla()) {
         hoverEdge(g, x, y, w, h, this.hover() * a);
      }

      boolean mine = this.isSelected();
      boolean pointed = this.isMouseOver((double)mouseX, (double)mouseY) && g.containsPointInScissor(mouseX, mouseY);
      this.play.set(mine && (pointed || this.plate.isFocused()));
      float k = this.play.get();
      if (k > 0.01F) {
         this.plate.visible = true;
         PlayPlates.draw(g, this.plate, k, a, 0.0F, 0.0F, mine ? mouseX : -10000, mine ? mouseY : -10000, partialTick);
      }

      this.plate.visible = mine;
   }

   public static void hoverEdge(GuiGraphics g, int x, int y, int w, int h, float amount) {
      if (!(amount <= 0.01F)) {
         Theme t = Theme.current();
         CFHGuiDraw.outline(g, x, y, w, h, Colors.scaleAlpha(t.accentInk(), 0.9F * amount), t.radius());
      }
   }

   public void refresh() {
      this.star.setIcon(this.entry.favorite ? Icon.STAR_FILLED : Icon.STAR);
      this.star.toggled(this.entry.favorite);
      this.star.tip(Component.translatable(this.entry.favorite ? "cfhgui.unfavorite" : "cfhgui.favorite"));
   }
}
