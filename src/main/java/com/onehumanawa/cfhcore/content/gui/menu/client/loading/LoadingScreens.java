package com.onehumanawa.cfhcore.content.gui.menu.client.loading;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Fonts;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.screen.Reskin;
import com.onehumanawa.cfhcore.content.gui.core.screen.slot.Layout;
import com.onehumanawa.cfhcore.content.gui.core.screen.slot.MenuSlots;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSpinner;
import com.onehumanawa.cfhcore.content.gui.menu.client.loading.journey.Journey;
import com.onehumanawa.cfhcore.content.gui.menu.mixin.LevelLoadingScreenAccessor;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.List;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.progress.StoringChunkProgressListener;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.jetbrains.annotations.Nullable;

public final class LoadingScreens {
   private static final int PAD = 12;
   private static final int MAX_W = 300;
   private static final int MAP_MAX = 90;
   private static @Nullable Screen lastScreen;
   private static float shownProgress;
   private static long lastFrameMs;

   public static boolean active() {
      return MenuSlots.effective("minecraft:level_loading") != Layout.VANILLA && !Reskin.leftToOther(Minecraft.getInstance().screen);
   }

   public static void render(Screen screen, GuiGraphics g, int mouseX, int mouseY, float partialTick, Component title, @Nullable Component status, float progress, @Nullable StoringChunkProgressListener chunks, List<? extends AbstractWidget> buttons) {
      if (!Journey.active() || !Journey.render(screen, g, mouseX, mouseY, partialTick, title, status, progress, chunks, buttons)) {
         screen.renderBackground(g, mouseX, mouseY, partialTick);
         Theme t = Theme.current();
         Palette p = t.palette();
         boolean van = t.isVanilla();
         Font font = Minecraft.getInstance().font;
         int w = Math.min(screen.width - 24, 300);
         int inner = w - 24;
         List<FormattedCharSequence> lines = status != null && !status.getString().isBlank() ? font.split(status, inner) : List.of();
         int statusLines = Math.min(3, lines.size());
         int diameter = chunks == null ? 0 : chunks.getDiameter();
         int cell = diameter <= 0 ? 0 : Mth.clamp(90 / diameter, 2, 8);
         int mapH = diameter * cell;
         int h = 30;
         h += statusLines == 0 ? 0 : statusLines * 10 + 6;
         h += mapH == 0 ? 0 : mapH + 12;
         h += 9;
         h += buttons.isEmpty() ? 12 : 10 + buttons.size() * 24 - 4 + 12;
         int x = (screen.width - w) / 2;
         int y = Math.max(4, (screen.height - h) / 2);
         CFHGuiDraw.floatingPanel(g, x, y, w, h, 1.0F);
         CFHGuiSpinner.draw(g, x + 12 - 1, y + 12 - 1, 12, van ? -1 : p.accentInk());
         Component heading = Fonts.heading(title);
         FormattedCharSequence shownTitle = CFHGuiDraw.truncate(heading, inner - 16);
         g.drawString(font, shownTitle, x + 12 + 16, y + 12 + 1, van ? -1 : p.text(), van);
         if (!van) {
            CFHGuiDraw.accentCap(g, x + 12 + 16, y + 12 + 11, Math.max(16, Math.min(font.width(shownTitle), 48)), 1.0F);
         }

         int cy = y + 12 + 12 + 6;

         for(int i = 0; i < statusLines; ++i) {
            g.drawString(font, (FormattedCharSequence)lines.get(i), x + 12, cy, van ? -4144960 : p.textMuted(), van);
            cy += 10;
         }

         if (statusLines > 0) {
            cy += 6;
         }

         if (mapH > 0) {
            int mapX = x + (w - mapH) / 2;
            if (van) {
               CFHGuiDraw.rect(g, mapX - 3, cy - 3, mapH + 6, mapH + 6, Integer.MIN_VALUE);
            } else {
               CFHGuiDraw.pixelRound(g, mapX - 3, cy - 3, mapH + 6, mapH + 6, p.bg2(), t.radius());
               CFHGuiDraw.outline(g, mapX - 3, cy - 3, mapH + 6, mapH + 6, p.border(), t.radius());
            }

            chunkMap(g, chunks, mapX, cy, cell);
            cy += mapH + 12;
         }

         progressRow(g, font, screen, x + 12, cy, inner, progress, van, p);
         cy += 9;
         if (!buttons.isEmpty()) {
            cy += 10;

            for(AbstractWidget b : buttons) {
               b.setX(x + 12);
               b.setY(cy);
               b.setWidth(inner);
               b.render(g, mouseX, mouseY, partialTick);
               cy += 24;
            }
         }

      }
   }

   private static void chunkMap(GuiGraphics g, StoringChunkProgressListener chunks, int x, int y, int cell) {
      Object2IntMap<ChunkStatus> colors = LevelLoadingScreenAccessor.cfhgui$colors();
      int d = chunks.getDiameter();
      int gap = cell >= 4 ? 1 : 0;
      g.drawManaged(() -> {
         for(int r = 0; r < d; ++r) {
            for(int s = 0; s < d; ++s) {
               ChunkStatus status = chunks.getStatus(r, s);
               if (status != null) {
                  int cx = x + r * cell;
                  int cy = y + s * cell;
                  g.fill(cx, cy, cx + cell - gap, cy + cell - gap, colors.getInt(status) | -16777216);
               }
            }
         }

      });
   }

   private static void progressRow(GuiGraphics g, Font font, Screen screen, int x, int y, int w, float progress, boolean van, Palette p) {
      long now = Util.getMillis();
      int track = van ? -12566464 : p.border();
      int fill = van ? -8323200 : p.accent();
      if (progress < 0.0F) {
         CFHGuiDraw.rect(g, x, y + 3, w, 3, track);
         int seg = Math.max(24, w / 3);
         float phase = (float)(now % 1400L) / 1400.0F;
         int sx = x - seg + Math.round((float)(w + seg) * phase);
         int from = Math.max(x, sx);
         int to = Math.min(x + w, sx + seg);
         if (to > from) {
            CFHGuiDraw.rect(g, from, y + 3, to - from, 3, fill);
         }

         lastScreen = screen;
         shownProgress = 0.0F;
         lastFrameMs = now;
      } else {
         float target = Mth.clamp(progress, 0.0F, 1.0F);
         if (screen != lastScreen) {
            lastScreen = screen;
            shownProgress = target;
         } else {
            float dt = Math.min(0.25F, (float)(now - lastFrameMs) / 1000.0F);
            shownProgress += (target - shownProgress) * Math.min(1.0F, dt * 8.0F);
         }

         lastFrameMs = now;
         Component pct = Component.literal(Math.round(target * 100.0F) + "%");
         int pctW = font.width(pct);
         int barW = Math.max(20, w - pctW - 8);
         CFHGuiDraw.rect(g, x, y + 3, barW, 3, track);
         CFHGuiDraw.rect(g, x, y + 3, Math.round((float)barW * shownProgress), 3, fill);
         g.drawString(font, pct, x + w - pctW, y, van ? -1 : p.text(), van);
      }
   }

   private LoadingScreens() {
   }
}
