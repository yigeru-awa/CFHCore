package com.onehumanawa.cfhcore.content.gui.menu.client.pause;

import com.mojang.blaze3d.systems.RenderSystem;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Fonts;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Textures;
import com.onehumanawa.cfhcore.content.gui.core.module.Features;
import com.onehumanawa.cfhcore.content.gui.core.screen.CFHGuiScreen;
import com.onehumanawa.cfhcore.content.gui.core.screen.slot.MenuSlots;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiAvatar;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiCard;
import com.onehumanawa.cfhcore.content.gui.menu.client.title.NavButton;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.SkinCustomizationScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;

public final class CFHGuiPauseScreen extends CFHGuiScreen {
   private static final int NAV_W = 176;
   private static final int GUTTER = 16;
   private static final int WORLD_H = 66;
   private static final int PLAYER_H = 66;
   private final Anim open;
   private int left;
   private int top;
   private int navTop;
   private int navBottom;
   private int contentW;
   private boolean compact;

   public CFHGuiPauseScreen() {
      super(Component.translatable("menu.game"), (Screen)null);
      this.open = new Anim(0.0F, 260, Ease.OUT_CUBIC);
      this.showHeader = false;
      this.showBack = false;
   }

   protected void build() {
      if (this.open.target() == 0.0F) {
         this.open.set(1.0F);
      }

      List<PauseActions.Action> actions = PauseActions.actions(this, false);
      this.compact = this.height < 300;
      boolean wide = this.width >= 400;
      int rowH = this.compact ? 20 : 24;
      int gap = this.compact ? 2 : 4;
      int navH = actions.size() * rowH + (actions.size() - 1) * gap;
      int headH = this.compact ? 20 : 34;
      this.contentW = wide ? Math.min(this.width - 24, 540) : 176;
      this.left = (this.width - this.contentW) / 2;
      this.top = Math.max(6, (this.height - headH - navH) / 2);
      this.navTop = this.top + headH;
      int y = this.navTop;

      for(PauseActions.Action a : actions) {
         NavButton b = new NavButton(this.left, y, 176, a.icon(), a.label(), a.run());
         b.setHeight(rowH);
         if (a.kind() == PauseActions.Kind.PRIMARY) {
            b.primary();
         } else if (a.kind() == PauseActions.Kind.DANGER) {
            b.danger();
         }

         if (!a.enabled()) {
            b.enabled(false);
         }

         if (a.tip() != null) {
            b.tip(a.tip());
         }

         this.add(b);
         y += rowH + gap;
      }

      this.navBottom = y - gap;
      if (wide) {
         int cx = this.left + 176 + 16;
         int cw = this.contentW - 176 - 16;
         int cy = this.navTop;
         if (cy + 66 <= this.height - 6) {
            this.add(new WorldCard(cx, cy, cw, 66));
            cy += 74;
         }

         if (cy + 66 <= this.height - 6) {
            this.add(new PlayerCard(cx, cy, cw, 66, this));
         }

      }
   }

   public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      super.renderBackground(g, mouseX, mouseY, partialTick);
      if (!Theme.current().isVanilla() && this.navBottom > 0) {
         CFHGuiDraw.floatingPanel(g, this.left - 6, this.navTop - 6, 188, this.navBottom - this.navTop + 12, this.open.get());
      }

   }

   protected void renderContent(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      Theme t = Theme.current();
      Palette p = t.palette();
      boolean van = t.isVanilla();
      float a = this.open.get();
      float big = this.compact ? 1.0F : 2.0F;
      int slide = Math.round((1.0F - a) * 5.0F);
      FormattedCharSequence heading = CFHGuiDraw.truncate(Fonts.heading(this.getTitle()), Math.round((float)this.contentW / big));
      g.pose().pushPose();
      g.pose().translate((float)this.left, (float)(this.top + slide), 0.0F);
      g.pose().scale(big, big, 1.0F);
      g.drawString(this.font, heading, 0, 0, Colors.scaleAlpha(van ? -1 : p.text(), a), van);
      g.pose().popPose();
      if (!van) {
         CFHGuiDraw.accentCap(g, this.left, this.top + slide + Math.round(9.0F * big) + 2, Math.min(this.contentW, Math.max(24, Math.round((float)this.font.width(heading) * big * 0.5F))), a);
      }

      Component session = PauseActions.session();
      if (session != null && this.contentW > 176) {
         int w = this.font.width(session);
         if (w < this.contentW - Math.round((float)this.font.width(heading) * big) - 12) {
            g.drawString(this.font, session, this.left + this.contentW - w, this.top + slide + Math.round(7.0F * big) - 7, Colors.scaleAlpha(van ? -4144960 : p.textMuted(), a), van);
         }
      }

   }

   private static int chip(GuiGraphics g, @Nullable Component text, int x, int y, int right, int color) {
      if (text == null) {
         return x;
      } else {
         return x + CFHGuiDraw.width(text) + 8 > right ? right : x + CFHGuiDraw.chip(g, text, x, y, color, 1.0F) + 4;
      }
   }

   private static final class WorldCard extends CFHGuiCard {
      private Textures.@Nullable Loaded icon;

      WorldCard(int x, int y, int w, int h) {
         super(x, y, w, h);
         this.flat();
         Minecraft mc = Minecraft.getInstance();
         if (mc.hasSingleplayerServer() && mc.getSingleplayerServer() != null) {
            Path file = (Path)mc.getSingleplayerServer().getWorldScreenshotFile().orElse(null);
            if (file != null && Files.isRegularFile(file, new LinkOption[0])) {
               Textures.load(file, (l) -> this.icon = l);
            }
         } else {
            ServerData server = mc.getCurrentServer();
            byte[] bytes = server == null ? null : server.getIconBytes();
            if (bytes != null) {
               this.icon = (Textures.Loaded)Textures.fromBytes(bytes, "cfhgui:favicon:" + server.ip + ":" + bytes.length).orElse(null);
            }
         }

      }

      protected void renderContent(GuiGraphics g, int x, int y, int w, int h, int mouseX, int mouseY, float partialTick) {
         Theme t = Theme.current();
         Palette p = t.palette();
         boolean van = t.isVanilla();
         Minecraft mc = Minecraft.getInstance();
         boolean sp = PauseActions.singleplayer();
         int muted = van ? -4144960 : p.textMuted();
         g.drawString(CFHGuiDraw.font(), Component.translatable(sp ? "cfhgui.pause.card.world" : "cfhgui.pause.card.server"), x + 10, y + 7, van ? -6250336 : p.textDim(), van);
         int ix = x + 10;
         int iy = y + 21;
         int is = 32;
         if (this.icon != null) {
            RenderSystem.enableBlend();
            g.blit(this.icon.id(), ix, iy, 32, 32, 0.0F, 0.0F, this.icon.width(), this.icon.height(), this.icon.width(), this.icon.height());
            RenderSystem.disableBlend();
         } else {
            CFHGuiDraw.pixelRound(g, ix, iy, 32, 32, van ? Integer.MIN_VALUE : p.bg2(), t.radius());
            Icons.draw(g, sp ? Icon.WORLD : Icon.SERVER, ix + 8, iy + 8, 16, muted);
         }

         CFHGuiDraw.outline(g, ix, iy, 32, 32, van ? -16777216 : p.border(), t.radius());
         int tx = ix + 32 + 8;
         int right = x + w - 10;
         g.drawString(CFHGuiDraw.font(), CFHGuiDraw.truncate(Component.literal(PauseActions.worldName()), right - tx), tx, y + 22, van ? -1 : p.text(), van);
         Component dimension = PauseActions.dimension();
         Component sub = Component.translatable(sp ? "cfhgui.title.singleplayer" : "cfhgui.title.multiplayer");
         if (!sp && mc.getCurrentServer() != null) {
            sub = Component.empty().append(sub).append(" · ").append(mc.getCurrentServer().ip);
         } else if (dimension != null) {
            sub = Component.empty().append(sub).append(" · ").append(dimension);
         }

         g.drawString(CFHGuiDraw.font(), CFHGuiDraw.truncate(sub, right - tx), tx, y + 34, muted, van);
         int cx = CFHGuiPauseScreen.chip(g, PauseActions.mode(), tx, y + 46, right, p.accent());
         if (mc.level != null && mc.level.getLevelData().isHardcore()) {
            cx = CFHGuiPauseScreen.chip(g, Component.translatable("cfhgui.worlds.hardcore"), cx, y + 46, right, p.danger());
         }

         cx = CFHGuiPauseScreen.chip(g, PauseActions.dayTime(), cx, y + 46, right, p.warning());
         if (mc.level != null) {
            CFHGuiPauseScreen.chip(g, mc.level.getDifficulty().getDisplayName(), cx, y + 46, right, p.textMuted());
         }

      }
   }

   private static final class PlayerCard extends CFHGuiCard {
      PlayerCard(int x, int y, int w, int h, Screen parent) {
         super(x, y, w, h);
         Minecraft mc = Minecraft.getInstance();
         this.add(new CFHGuiAvatar(0, 0, 32, mc.getGameProfile()), 10, 21);
         this.onClick(() -> {
            if (Features.present("cfhgui_profile")) {
               MenuSlots.open("cfhgui_profile:profile", parent);
            } else {
               mc.setScreen(new SkinCustomizationScreen(parent, mc.options));
            }

         });
      }

      protected void renderContent(GuiGraphics g, int x, int y, int w, int h, int mouseX, int mouseY, float partialTick) {
         Theme t = Theme.current();
         Palette p = t.palette();
         boolean van = t.isVanilla();
         Minecraft mc = Minecraft.getInstance();
         int muted = van ? -4144960 : p.textMuted();
         g.drawString(CFHGuiDraw.font(), Component.translatable("cfhgui.pause.card.you"), x + 10, y + 7, van ? -6250336 : p.textDim(), van);
         Icons.draw(g, Icon.EDIT, x + w - 18, y + 7, 10, van ? -6250336 : p.textDim());
         int tx = x + 50;
         int right = x + w - 10;
         g.drawString(CFHGuiDraw.font(), CFHGuiDraw.truncate(Component.literal(mc.getUser().getName()), right - tx), tx, y + 22, van ? -1 : p.text(), van);
         if (PauseVitals.width() > 0) {
            PauseVitals.draw(g, tx, y + 35, 1.0F, van ? -1 : p.text());
         } else if (PauseActions.mode() != null) {
            g.drawString(CFHGuiDraw.font(), PauseActions.mode(), tx, y + 35, muted, van);
         }

         Component position = PauseActions.position();
         Component biome = PauseActions.biome();
         Component where = position;
         if (position != null && biome != null) {
            where = Component.empty().append(position).append(" · ").append(biome);
         } else if (biome != null) {
            where = biome;
         }

         if (where != null) {
            g.drawString(CFHGuiDraw.font(), CFHGuiDraw.truncate(where, right - tx), tx, y + 48, muted, van);
         }

      }
   }
}
