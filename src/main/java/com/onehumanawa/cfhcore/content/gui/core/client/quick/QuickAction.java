package com.onehumanawa.cfhcore.content.gui.core.client.quick;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.GameType;
import org.jetbrains.annotations.Nullable;

enum QuickAction {
   TIME("time") {
      State now(Minecraft mc) {
         ClientLevel level = mc.level;
         long time = level == null ? 0L : Math.floorMod(level.getDayTime(), 24000L);
         if (time >= 12000L && time < 23000L) {
            return QuickAction.State.NIGHT;
         } else {
            return time >= 3500L && time < 12000L ? QuickAction.State.NOON : QuickAction.State.DAY;
         }
      }

      State after(State now) {
         return now == QuickAction.State.DAY ? QuickAction.State.NOON : (now == QuickAction.State.NOON ? QuickAction.State.NIGHT : QuickAction.State.DAY);
      }
   },
   GAME_MODE("gamemode") {
      State now(Minecraft mc) {
         GameType mode = mc.gameMode == null ? GameType.SURVIVAL : mc.gameMode.getPlayerMode();
         if (mode == GameType.CREATIVE) {
            return QuickAction.State.CREATIVE;
         } else {
            return mode == GameType.ADVENTURE ? QuickAction.State.ADVENTURE : (mode == GameType.SPECTATOR ? QuickAction.State.SPECTATOR : QuickAction.State.SURVIVAL);
         }
      }

      State after(State now) {
         return now == QuickAction.State.CREATIVE ? QuickAction.State.SURVIVAL : QuickAction.State.CREATIVE;
      }

      Component title(State state) {
         return Component.translatable("cfhgui.quick.gamemode", new Object[]{state.mode.getShortDisplayName()});
      }

      Component hint(State next) {
         return Component.translatable("cfhgui.quick.gamemode.go", new Object[]{next.mode.getShortDisplayName()});
      }
   },
   WEATHER("weather") {
      State now(Minecraft mc) {
         ClientLevel level = mc.level;
         if (level == null) {
            return QuickAction.State.CLEAR;
         } else {
            return level.isThundering() ? QuickAction.State.THUNDER : (level.isRaining() ? QuickAction.State.RAIN : QuickAction.State.CLEAR);
         }
      }

      State after(State now) {
         return now == QuickAction.State.CLEAR ? QuickAction.State.RAIN : QuickAction.State.CLEAR;
      }
   };

   private static final long DAY_LENGTH = 24000L;
   private static final long LATE_MORNING = 3500L;
   private static final long SUNSET = 12000L;
   private static final long SUNRISE = 23000L;
   final String command;

   private QuickAction(final String command) {
      this.command = command;
   }

   abstract State now(Minecraft var1);

   abstract State after(State var1);

   Component title(State state) {
      return Component.translatable("cfhgui.quick." + this.name().toLowerCase(Locale.ROOT), new Object[]{Component.translatable("cfhgui.quick." + state.key)});
   }

   Component hint(State next) {
      return Component.translatable("cfhgui.quick." + next.key + ".go");
   }

   static enum State {
      DAY(Icon.SUNRISE, "time.day", "time set day", (GameType)null),
      NOON(Icon.SUN, "time.noon", "time set noon", (GameType)null),
      NIGHT(Icon.MOON, "time.night", "time set night", (GameType)null),
      SURVIVAL(Icon.SURVIVAL, "gamemode", "gamemode survival", GameType.SURVIVAL),
      CREATIVE(Icon.CREATIVE, "gamemode", "gamemode creative", GameType.CREATIVE),
      ADVENTURE(Icon.ADVENTURE, "gamemode", (String)null, GameType.ADVENTURE),
      SPECTATOR(Icon.SPECTATOR, "gamemode", (String)null, GameType.SPECTATOR),
      CLEAR(Icon.CLOUD, "weather.clear", "weather clear", (GameType)null),
      RAIN(Icon.RAIN, "weather.rain", "weather rain", (GameType)null),
      THUNDER(Icon.THUNDER, "weather.thunder", (String)null, (GameType)null);

      final Icon icon;
      final String key;
      final @Nullable String command;
      final @Nullable GameType mode;

      private State(final @Nullable Icon icon, final @Nullable String key, final String command, final GameType mode) {
         this.icon = icon;
         this.key = key;
         this.command = command;
         this.mode = mode;
      }
   }
}
