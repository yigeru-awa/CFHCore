package com.onehumanawa.cfhcore.content.gui.menu.client.tablist;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.systems.RenderSystem;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Fonts;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.menu.CFHGuiMenu;
import com.onehumanawa.cfhcore.content.gui.menu.client.Fmt;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.numbers.NumberFormat;
import net.minecraft.network.chat.numbers.StyledFormat;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.level.GameType;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ReadOnlyScoreInfo;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria.RenderType;
import org.jetbrains.annotations.Nullable;

public final class CFHGuiTabList {
   private static final int TOP = 8;
   private static final int MARGIN = 8;
   private static final int PAD = 4;
   private static final int IN = 5;
   private static final int EDGE = 9;
   private static final int HEAD_H = 20;
   private static final int HEAD_TEXT = 6;
   private static final int HEAD_GAP = 12;
   private static final int TITLE_MAX = 180;
   private static final int LINE_H = 10;
   private static final int BLOCK_GAP = 4;
   private static final int FOOT = 4;
   private static final int[] ROW_HEIGHTS = new int[]{12, 10, 9};
   private static final int FACE = 8;
   private static final int FACE_GAP = 4;
   private static final int HEART = 9;
   private static final int GAP = 8;
   private static final int COL_GAP = 4;
   private static final int CELL_MIN = 96;
   private static final int NAME_MIN = 60;
   private static final int BARS = 5;
   private static final int SLIDE = 4;
   private static final ResourceLocation HEART_CONTAINER = ResourceLocation.withDefaultNamespace("hud/heart/container");
   private static final ResourceLocation HEART_FULL = ResourceLocation.withDefaultNamespace("hud/heart/full");
   private static final ResourceLocation[] PING = new ResourceLocation[]{ResourceLocation.withDefaultNamespace("icon/ping_unknown"), ResourceLocation.withDefaultNamespace("icon/ping_1"), ResourceLocation.withDefaultNamespace("icon/ping_2"), ResourceLocation.withDefaultNamespace("icon/ping_3"), ResourceLocation.withDefaultNamespace("icon/ping_4"), ResourceLocation.withDefaultNamespace("icon/ping_5")};
   private static final Anim SHOWN;
   private static boolean failed;

   public static boolean active() {
      return !failed && CFHGuiMenu.config().tabList;
   }

   public static void opened() {
      SHOWN.snap(0.0F);
      SHOWN.set(1.0F);
   }

   public static boolean render(GuiGraphics g, PlayerTabOverlay overlay, List<PlayerInfo> players, @Nullable Component header, @Nullable Component footer, Scoreboard scoreboard, @Nullable Objective objective) {
      if (!active()) {
         return false;
      } else {
         try {
            draw(g, model(overlay, players, header, footer, scoreboard, objective), SHOWN.get());
            return true;
         } catch (RuntimeException e) {
            failed = true;
            g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            CFHGuiMenu.LOGGER.error("[CFHGui Menu] the player list could not be drawn; the game's own is back for this session", e);
            return false;
         }
      }
   }

   private static Model model(PlayerTabOverlay overlay, List<PlayerInfo> players, @Nullable Component header, @Nullable Component footer, Scoreboard scoreboard, @Nullable Objective objective) {
      Minecraft mc = Minecraft.getInstance();
      Score score = objective == null ? CFHGuiTabList.Score.NONE : (objective.getRenderType() == RenderType.HEARTS ? CFHGuiTabList.Score.HEARTS : CFHGuiTabList.Score.NUMBER);
      NumberFormat format = objective == null ? null : objective.numberFormatOrDefault(plainNumbers());
      UUID self = mc.player == null ? null : mc.player.getUUID();
      List<Row> rows = new ArrayList(players.size());

      for(PlayerInfo info : players) {
         GameProfile profile = info.getProfile();
         boolean spectator = info.getGameMode() == GameType.SPECTATOR;
         Component value = null;
         if (objective != null && !spectator) {
            ReadOnlyScoreInfo held = scoreboard.getPlayerScoreInfo(ScoreHolder.fromGameProfile(profile), objective);
            value = score == CFHGuiTabList.Score.HEARTS ? Component.literal(hearts(held == null ? 0 : held.value())) : ReadOnlyScoreInfo.safeFormatValue(held, format);
         }

         Player player = mc.level == null ? null : mc.level.getPlayerByUUID(profile.getId());
         rows.add(new Row(overlay.getNameForDisplay(info), info.getSkin().texture(), player == null || player.isModelPartShown(PlayerModelPart.HAT), player != null && LivingEntityRenderer.isEntityUpsideDown(player), spectator, profile.getId().equals(self), info.getLatency(), value));
      }

      int listed = mc.player == null ? 0 : mc.player.connection.getListedOnlinePlayers().size();
      return new Model(title(), Math.max(listed, rows.size()), header, footer, rows, score);
   }

   private static Component title() {
      Minecraft mc = Minecraft.getInstance();
      String name = null;
      if (mc.hasSingleplayerServer() && mc.getSingleplayerServer() != null) {
         name = mc.getSingleplayerServer().getWorldData().getLevelName();
      } else {
         ServerData server = mc.getCurrentServer();
         if (server != null) {
            name = server.name != null && !server.name.isBlank() ? server.name : (mc.options.hideServerAddress ? null : server.ip);
         }
      }

      return name != null && !name.isBlank() ? Component.literal(name) : Component.translatable("cfhgui.tablist.title");
   }

   static NumberFormat plainNumbers() {
      return Theme.current().isVanilla() ? StyledFormat.PLAYER_LIST_DEFAULT : StyledFormat.NO_STYLE;
   }

   static String hearts(int points) {
      int half = Math.max(0, points);
      return half % 2 == 0 ? Integer.toString(half / 2) : half / 2 + ".5";
   }

   static void draw(GuiGraphics g, Model m, float alpha) {
      if (!(alpha < 0.05F)) {
         Theme t = Theme.current();
         Palette p = t.palette();
         boolean van = t.isVanilla();
         Font font = CFHGuiDraw.font();
         List<Row> rows = m.rows();
         int n = rows.size();
         int sw = g.guiWidth();
         int sh = g.guiHeight();
         int plateMax = Math.max(58, sw - 16);
         int gridMax = plateMax - 8;
         int nameW = 0;
         int scoreW = 0;
         int pingW = 0;
         String[] pings = new String[n];

         for(int i = 0; i < n; ++i) {
            Row r = (Row)rows.get(i);
            nameW = Math.max(nameW, font.width(r.name()));
            if (r.score() != null) {
               scoreW = Math.max(scoreW, font.width(r.score()));
            }

            if (r.ping() >= 0) {
               pings[i] = r.ping() + " ms";
               pingW = Math.max(pingW, font.width(pings[i]));
            }
         }

         boolean hearts = m.score() == CFHGuiTabList.Score.HEARTS;
         int scoreCol = m.score() != CFHGuiTabList.Score.NONE && scoreW != 0 ? 8 + scoreW + (hearts ? 11 : 0) : 0;
         int barsW = van ? 10 : 9;
         Component title = Fonts.heading(m.title());
         Component count = Component.translatable(m.count() == 1 ? "cfhgui.tablist.players.one" : "cfhgui.tablist.players", new Object[]{m.count()});
         int countW = font.width(count);
         List<FormattedCharSequence> header = wrap(font, m.header(), plateMax - 18);
         List<FormattedCharSequence> footer = wrap(font, m.footer(), plateMax - 18);
         int textW = Math.min(font.width(title), 180) + 12 + countW;

         for(FormattedCharSequence line : header) {
            textW = Math.max(textW, font.width(line));
         }

         for(FormattedCharSequence line : footer) {
            textW = Math.max(textW, font.width(line));
         }

         int chrome = 24 + (header.isEmpty() ? 0 : header.size() * 10 + 4) + (footer.isEmpty() ? 0 : 4 + footer.size() * 10) + 4;
         int room = sh - 8 - 8 - chrome;
         int closest = ROW_HEIGHTS[ROW_HEIGHTS.length - 1];
         int fit = Mth.clamp(room / closest, 1, 20);
         int readable = Math.max(1, (gridMax + 4) / 100);
         int cols = Math.max(1, Math.max(Mth.positiveCeilDiv(n, 20), Math.min(Mth.positiveCeilDiv(n, fit), readable)));
         int perCol = Mth.positiveCeilDiv(n, cols);
         int rowH = closest;

         for(int h : ROW_HEIGHTS) {
            if (perCol * h <= room) {
               rowH = h;
               break;
            }
         }

         int cellMax = (gridMax - (cols - 1) * 4) / cols;
         boolean numbers = pingW > 0;
         int fixed = 22 + scoreCol + 8 + barsW + (numbers ? 3 + pingW : 0);
         if (numbers && cellMax - fixed < Math.min(nameW, 60)) {
            numbers = false;
            fixed -= 3 + pingW;
         }

         int gridWant = cols * (fixed + nameW) + (cols - 1) * 4;
         int plateW = Math.min(plateMax, Math.max(gridWant + 8, textW + 18));
         int cellW = (plateW - 8 - (cols - 1) * 4) / cols;
         int gridW = cols * cellW + (cols - 1) * 4;
         int nameRoom = Math.max(0, cellW - fixed);
         int plateH = chrome + perCol * rowH;
         int x = (sw - plateW) / 2;
         int y = 8 - Math.round((1.0F - alpha) * 4.0F);
         int gridX = x + 4 + (plateW - 8 - gridW) / 2;
         int text = Colors.scaleAlpha(p.text(), alpha);
         int muted = Colors.scaleAlpha(van ? -4144960 : p.textMuted(), alpha);
         if (van) {
            CFHGuiDraw.rect(g, x, y, plateW, plateH, Colors.scaleAlpha(Integer.MIN_VALUE, alpha));
         } else {
            CFHGuiDraw.floatingPanel(g, x, y, plateW, plateH, alpha);
         }

         int left = x + 9;
         int right = x + plateW - 9;
         g.drawString(font, CFHGuiDraw.truncate(title, right - left - countW - 12), left, y + 6, text, van);
         g.drawString(font, count, right - countW, y + 6, muted, van);
         CFHGuiDraw.hline(g, left, y + 20 - 1, right - left, Colors.scaleAlpha(van ? 1090519039 : p.border(), alpha));
         int cy = y + 20 + 4;

         for(FormattedCharSequence line : header) {
            g.drawString(font, line, x + (plateW - font.width(line)) / 2, cy, muted, van);
            cy += 10;
         }

         if (!header.isEmpty()) {
            cy += 4;
         }

         int cell = Colors.scaleAlpha(Minecraft.getInstance().options.getBackgroundColor(553648127), alpha);
         int mark = van ? -1 : p.accent();
         int unlit = Colors.scaleAlpha(Colors.withAlpha(p.borderStrong(), 144), alpha);
         int dim = Colors.scaleAlpha(p.textDim(), alpha);
         int pad = (rowH - 8) / 2;
         int band = pad >= 2 ? 1 : 0;

         for(int i = 0; i < n; ++i) {
            Row r = (Row)rows.get(i);
            int cx = gridX + i / perCol * (cellW + 4);
            int ry = cy + i % perCol * rowH + pad;
            if (van) {
               CFHGuiDraw.rect(g, cx, ry - band, cellW, 8 + band * 2, cell);
            }

            if (r.self()) {
               CFHGuiDraw.hgradient(g, cx, ry - band, cellW, 8 + band * 2, Colors.scaleAlpha(Colors.withAlpha(mark, van ? 56 : 70), alpha), Colors.withAlpha(mark, 0));
               CFHGuiDraw.rect(g, cx, ry, 2, 8, Colors.scaleAlpha(mark, alpha));
            }

            face(g, r.skin(), cx + 5, ry, r.hat(), r.flipped(), r.spectator() ? alpha * 0.5F : alpha);
            g.drawString(font, CFHGuiDraw.truncate(r.name(), nameRoom), cx + 5 + 8 + 4, ry, r.spectator() ? Colors.scaleAlpha(text, 0.56F) : text, van);
            int rx = cx + cellW - 5 - barsW;
            int lit = Fmt.pingBars((long)r.ping());
            int quality = Colors.scaleAlpha(lit >= 4 ? p.success() : (lit >= 2 ? p.warning() : p.danger()), alpha);
            if (van) {
               sprite(g, PING[lit], rx, ry, 10, 8, alpha);
            } else if (lit == 0) {
               bars(g, rx, ry, 5, dim, dim);
            } else {
               bars(g, rx, ry, lit, quality, unlit);
            }

            if (numbers) {
               if (pings[i] != null) {
                  g.drawString(font, pings[i], rx - 3 - font.width(pings[i]), ry, lit >= 4 ? muted : quality, van);
               }

               rx -= 3 + pingW;
            }

            if (scoreCol > 0 && r.score() != null) {
               rx -= 8;
               if (hearts) {
                  rx -= 9;
                  sprite(g, HEART_CONTAINER, rx, ry - 1, 9, 9, alpha);
                  sprite(g, HEART_FULL, rx, ry - 1, 9, 9, alpha);
                  rx -= 2;
               }

               g.drawString(font, r.score(), rx - font.width(r.score()), ry, text, van);
            }
         }

         cy += perCol * rowH;
         if (!footer.isEmpty()) {
            cy += 4;
         }

         for(FormattedCharSequence line : footer) {
            g.drawString(font, line, x + (plateW - font.width(line)) / 2, cy, muted, van);
            cy += 10;
         }

      }
   }

   private static List<FormattedCharSequence> wrap(Font font, @Nullable Component text, int width) {
      return text == null ? List.of() : font.split(text, width);
   }

   private static void bars(GuiGraphics g, int x, int y, int lit, int on, int off) {
      for(int i = 0; i < 5; ++i) {
         int h = 3 + i;
         CFHGuiDraw.rect(g, x + i * 2, y + 7 - h, 1, h, i < lit ? on : off);
      }

   }

   private static void face(GuiGraphics g, ResourceLocation skin, int x, int y, boolean hat, boolean flipped, float alpha) {
      RenderSystem.enableBlend();
      g.setColor(1.0F, 1.0F, 1.0F, alpha);
      PlayerFaceRenderer.draw(g, skin, x, y, 8, hat, flipped);
      g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.disableBlend();
   }

   private static void sprite(GuiGraphics g, ResourceLocation sprite, int x, int y, int w, int h, float alpha) {
      RenderSystem.enableBlend();
      g.setColor(1.0F, 1.0F, 1.0F, alpha);
      g.blitSprite(sprite, x, y, w, h);
      g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.disableBlend();
   }

   private CFHGuiTabList() {
   }

   static {
      SHOWN = new Anim(1.0F, 100, Ease.OUT_CUBIC);
   }

   static record Row(Component name, ResourceLocation skin, boolean hat, boolean flipped, boolean spectator, boolean self, int ping, @Nullable Component score) {
   }

   static enum Score {
      NONE,
      HEARTS,
      NUMBER;
   }

   static record Model(Component title, int count, @Nullable Component header, @Nullable Component footer, List<Row> rows, Score score) {
   }
}
