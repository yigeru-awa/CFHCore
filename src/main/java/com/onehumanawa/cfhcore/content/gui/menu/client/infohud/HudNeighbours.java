package com.onehumanawa.cfhcore.content.gui.menu.client.infohud;

import com.onehumanawa.cfhcore.content.gui.core.module.Modules;
import com.onehumanawa.cfhcore.content.gui.core.platform.CFHGuiPlatform;
import com.onehumanawa.cfhcore.content.gui.menu.CFHGuiMenu;
import com.onehumanawa.cfhcore.content.gui.menu.mixin.BossHealthOverlayAccessor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.ChatVisiblity;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.Scoreboard;
import org.jetbrains.annotations.Nullable;

final class HudNeighbours {
   private static final int SUBTITLES_LINES = 6;
   private static final int SUBTITLES_WIDTH = 128;
   private static boolean looked;
   private static boolean teams;
   private static boolean quests;
   private static long scoreboardAtMs;
   private static int scoreboardRows;
   private static int scoreboardWidth;
   private static @Nullable Method partyBounds;
   private static @Nullable Method questsConfig;
   private static @Nullable Method entryHeight;
   private static @Nullable Field trackerOn;
   private static @Nullable Field trackerAnchor;
   private static @Nullable Field trackerX;
   private static @Nullable Field trackerY;
   private static @Nullable Field trackerScale;
   private static @Nullable Field trackerEntries;
   private static final int TRACKER_WIDTH = 150;
   private static final int TRACKER_GAP = 4;

   private HudNeighbours() {
   }

   private static void look() {
      if (!looked) {
         looked = true;

         teams = Modules.isLoaded("cfhgui_teams");
         quests = Modules.isLoaded("cfhgui_quests");
      }
   }

   static void collect(Rects out, Minecraft mc, int sw, int sh, long nowMs) {
      look();
      out.clear();
      game(out, mc, sw, sh, nowMs);

      if (teams) {
         try {
            partyFrames(out);
         } catch (Throwable t) {
            teams = false;
            gone("CFHGui Teams' party frames", t);
         }
      }

      if (quests) {
         try {
            questTracker(out, sw, sh);
         } catch (Throwable t) {
            quests = false;
            gone("CFHGui Quests' tracker", t);
         }
      }

   }

   private static void gone(String what, Throwable t) {
      CFHGuiMenu.LOGGER.info("[CFHGui Menu] info overlay: cannot see where {} is on the screen, so it will not keep off it ({})", what, t.toString());
   }

   private static void game(Rects out, Minecraft mc, int sw, int sh, long nowMs) {
      int bars = 0;

      try {
         bars = ((BossHealthOverlayAccessor)mc.gui.getBossOverlay()).cfhgui$events().size();
      } catch (RuntimeException var12) {
      }

      if (bars > 0) {
         int shown = Math.min(bars, Math.max(1, (sh / 3 - 12) / 19 + 1));
         out.add(sw / 2 - 93, 0, sw / 2 + 93, 19 * shown + 2);
      }

      if (mc.player != null) {
         Collection<MobEffectInstance> effects = mc.player.getActiveEffects();
         if (!effects.isEmpty()) {
            int good = 0;
            int bad = 0;

            for(MobEffectInstance effect : effects) {
               if (effect.showIcon()) {
                  if (((MobEffect)effect.getEffect().value()).isBeneficial()) {
                     ++good;
                  } else {
                     ++bad;
                  }
               }
            }

            int top = mc.isDemo() ? 15 : 0;
            if (good > 0) {
               out.add(sw - 25 * good - 2, top, sw, top + 27);
            }

            if (bad > 0) {
               out.add(sw - 25 * bad - 2, top + 26, sw, top + 53);
            }
         }
      }

      if (mc.options.chatVisibility().get() != ChatVisiblity.HIDDEN) {
         ChatComponent chat = mc.gui.getChat();
         out.add(0, sh - 40 - chat.getHeight() - 2, chat.getWidth() + 12, sh - 26);
      }

      if ((Boolean)mc.options.showSubtitles().get()) {
         out.add(sw - 128, sh - 30 - 60, sw, sh - 30);
      }

      out.add(sw / 2 - 124, sh - 62, sw / 2 + 124, sh);
      if (mc.level != null && nowMs - scoreboardAtMs > 1000L) {
         scoreboardAtMs = nowMs;
         scoreboard(mc);
      }

      if (scoreboardRows > 0) {
         int height = scoreboardRows * 9;
         int foot = sh / 2 + height / 3;
         out.add(sw - scoreboardWidth - 7, foot - height - 11, sw, foot + 2);
      }

   }

   private static void scoreboard(Minecraft mc) {
      scoreboardRows = 0;
      scoreboardWidth = 0;
      Scoreboard board = mc.level.getScoreboard();
      Objective objective = board.getDisplayObjective(DisplaySlot.SIDEBAR);
      if (objective != null) {
         Font font = mc.font;
         int rows = 0;
         int width = font.width(objective.getDisplayName());

         for(PlayerScoreEntry entry : board.listPlayerScores(objective)) {
            if (!entry.isHidden()) {
               ++rows;
               if (rows > 15) {
                  rows = 15;
                  break;
               }

               width = Math.max(width, font.width(entry.ownerName()) + 12 + font.width(Integer.toString(entry.value())));
            }
         }

         scoreboardRows = rows;
         scoreboardWidth = width;
      }
   }

   private static void partyFrames(Rects out) throws ReflectiveOperationException {
      if (partyBounds == null) {
         Method m = Class.forName("com.onehumanawa.cfhcore.content.gui.teams.client.hud.PartyFrames").getDeclaredMethod("bounds");
         m.setAccessible(true);
         partyBounds = m;
      }

      Object var2 = partyBounds.invoke(null);
      if (var2 instanceof int[] b) {
         if (b.length >= 4) {
            out.add(b[0] - 2, b[1] - 2, b[2] + 2, b[3] + 2);
         }
      }

   }

   private static void questTracker(Rects out, int sw, int sh) throws ReflectiveOperationException {
      if (trackerEntries == null) {
         Class<?> config = Class.forName("com.onehumanawa.cfhcore.content.gui.quests.QuestsConfig");
         questsConfig = config.getMethod("get");
         trackerOn = config.getField("tracker");
         trackerAnchor = config.getField("trackerAnchor");
         trackerX = config.getField("trackerX");
         trackerY = config.getField("trackerY");
         trackerScale = config.getField("trackerScale");
         Method height = Class.forName("com.onehumanawa.cfhcore.content.gui.quests.client.hud.QuestTracker$Entry").getDeclaredMethod("height");
         height.setAccessible(true);
         entryHeight = height;
         Field entries = Class.forName("com.onehumanawa.cfhcore.content.gui.quests.client.hud.QuestTracker").getDeclaredField("ENTRIES");
         entries.setAccessible(true);
         trackerEntries = entries;
      }

      Object config = questsConfig.invoke(null);
      if (config != null && trackerOn.getBoolean(config)) {
         Object total = trackerEntries.get(null);
         if (total instanceof List) {
            List<?> entries = (List)total;
            if (!entries.isEmpty()) {
               int sum = 0;

               for(int i = 0; i < entries.size(); ++i) {
                  sum += (Integer)entryHeight.invoke(entries.get(i)) + 4;
               }

               double scale = Math.max((double)0.5F, Math.min((double)2.0F, trackerScale.getDouble(config)));
               String anchor = String.valueOf(trackerAnchor.get(config));
               int w = (int)Math.ceil((double)150.0F * scale);
               int h = (int)Math.ceil((double)sum * scale);
               int x = anchor.endsWith("RIGHT") ? sw - trackerX.getInt(config) - w : trackerX.getInt(config);
               int y = anchor.startsWith("BOTTOM") ? sh - trackerY.getInt(config) - h : trackerY.getInt(config);
               out.add(x, y, x + w, y + h - (int)Math.floor((double)4.0F * scale));
               return;
            }
         }

      }
   }
}
