package com.onehumanawa.cfhcore.content.gui.menu.client.worlds;

import com.mojang.serialization.Dynamic;
import com.onehumanawa.cfhcore.content.gui.core.client.DevHarness;
import com.onehumanawa.cfhcore.content.gui.menu.client.play.model.WorldEntry;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerData.State;
import net.minecraft.client.multiplayer.ServerData.Type;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.storage.LevelSummary;
import net.minecraft.world.level.storage.LevelVersion;
import org.jetbrains.annotations.Nullable;

public final class SamplePlay {
   private static final long MINUTE = 60000L;
   private static final long HOUR = 3600000L;
   private static final long DAY = 86400000L;
   private static final List<World> WORLDS;
   private static @Nullable List<WorldEntry> worlds;
   private static @Nullable List<ServerData> servers;
   private static boolean lookedForReal;
   private static @Nullable Path real;

   public static boolean on() {
      return DevHarness.sampleData();
   }

   public static synchronized List<WorldEntry> worlds() {
      if (worlds == null) {
         long now = System.currentTimeMillis();
         Path saves = Minecraft.getInstance().getLevelSource().getBaseDir();
         Path borrowed = realWorld();
         List<WorldEntry> out = new ArrayList();

         for(World w : WORLDS) {
            CompoundTag tag = new CompoundTag();
            tag.putInt("version", 19133);
            tag.putLong("LastPlayed", now - w.ago());
            CompoundTag version = new CompoundTag();
            version.putString("Name", w.version().isEmpty() ? SharedConstants.getCurrentVersion().getName() : w.version());
            version.putInt("Id", w.dataVersion() == 0 ? SharedConstants.getCurrentVersion().getDataVersion().getVersion() : w.dataVersion());
            version.putString("Series", "main");
            version.putBoolean("Snapshot", false);
            tag.put("Version", version);
            LevelSettings settings = new LevelSettings(w.name(), w.mode(), w.hardcore(), w.hardcore() ? Difficulty.HARD : Difficulty.NORMAL, w.cheats(), new GameRules(), WorldDataConfiguration.DEFAULT);
            Path icon = borrowed != null ? borrowed.resolve("icon.png") : saves.resolve(w.id()).resolve("icon.png");
            WorldEntry e = new WorldEntry(new LevelSummary(settings, LevelVersion.parse(new Dynamic(NbtOps.INSTANCE, tag)), w.id(), false, false, w.experimental(), icon));
            e.favorite = w.favorite();
            e.tags = w.tags();
            e.size = w.megabytes() * 1024L * 1024L;
            out.add(e);
         }

         worlds = out;
      }

      return worlds;
   }

   public static boolean isSample(String id) {
      if (!on()) {
         return false;
      } else {
         for(World w : WORLDS) {
            if (w.id().equals(id)) {
               return true;
            }
         }

         return false;
      }
   }

   public static synchronized @Nullable Path realWorld() {
      if (!lookedForReal) {
         lookedForReal = true;

         try {
            Stream<Path> dirs = Files.list(Minecraft.getInstance().getLevelSource().getBaseDir());

            try {
               real = (Path)dirs.filter((d) -> Files.isDirectory(d.resolve("region"), new LinkOption[0]) && Files.isRegularFile(d.resolve("level.dat"), new LinkOption[0])).sorted().findFirst().orElse(null);
            } catch (Throwable var4) {
               if (dirs != null) {
                  try {
                     dirs.close();
                  } catch (Throwable var3) {
                     var4.addSuppressed(var3);
                  }
               }

               throw var4;
            }

            if (dirs != null) {
               dirs.close();
            }
         } catch (Exception var5) {
            real = null;
         }
      }

      return real;
   }

   public static Path folder(String id) {
      Path own = Minecraft.getInstance().getLevelSource().getLevelPath(id);
      if (!isSample(id)) {
         return own;
      } else {
         Path borrowed = realWorld();
         return borrowed != null ? borrowed : own;
      }
   }

   public static synchronized List<ServerData> servers() {
      if (servers == null) {
         int protocol = SharedConstants.getCurrentVersion().getProtocolVersion();
         String version = SharedConstants.getCurrentVersion().getName();
         List<ServerData> out = new ArrayList();
         out.add(server("Hollow Pine SMP", "play.hollowpine.example", State.SUCCESSFUL, "Season 4: the railways are open", 37, 120, 48L, version, protocol));
         out.add(server("Emberfall", "mc.emberfall.example", State.SUCCESSFUL, "Survival, towns and a market\nNo griefing, no resets", 212, 500, 96L, version, protocol));
         out.add(server("Quill's Realm", "quill.example", State.INCOMPATIBLE, "Still on the old version", 3, 20, 61L, "1.20.4", 765));
         out.add(server("Old Harrow", "harrow.example", State.UNREACHABLE, "", 0, 0, -1L, "", protocol));
         out.add(server("Saltwick Creative", "creative.saltwick.example", State.SUCCESSFUL, "Plots, WorldEdit, weekly contests", 8, 40, 142L, version, protocol));
         out.add(server("Tin Lantern Minigames", "tinlantern.example", State.PINGING, "", 0, 0, -1L, "", protocol));
         servers = out;
      }

      return servers;
   }

   private static ServerData server(String name, String address, ServerData.State state, String motd, int online, int max, long ping, String version, int protocol) {
      ServerData d = new ServerData(name, address, Type.OTHER);
      d.setState(state);
      d.protocol = protocol;
      d.ping = ping;
      d.version = Component.literal(version);
      if (state == State.UNREACHABLE) {
         d.motd = Component.translatable("multiplayer.status.cannot_connect").withStyle(ChatFormatting.RED);
      } else {
         d.motd = Component.literal(motd);
      }

      if (state != State.SUCCESSFUL && state != State.INCOMPATIBLE) {
         d.playerList = List.of();
      } else {
         d.players = new ServerStatus.Players(max, online, List.of());
         d.status = Component.literal(online + "/" + max);
         d.playerList = List.of(Component.literal("Mossback"), Component.literal("Quillfeather"), Component.literal("TinLantern"));
      }

      return d;
   }

   private SamplePlay() {
   }

   static {
      WORLDS = List.of(new World("hollow_pine", "Hollow Pine", GameType.SURVIVAL, false, false, false, 2520000L, "", 0, 412L, true, List.of("home", "railways")), new World("emberfall_hardcore", "Emberfall", GameType.SURVIVAL, true, false, false, 172800000L, "", 0, 96L, false, List.of()), new World("creative_testbed", "Creative Testbed", GameType.CREATIVE, false, true, false, 18000000L, "", 0, 23L, false, List.of("builds")), new World("skyblock_season_3", "Skyblock Season 3", GameType.SURVIVAL, false, false, false, 777600000L, "", 0, 58L, false, List.of()), new World("redstone_lab", "Redstone Lab", GameType.CREATIVE, false, true, false, 1987200000L, "", 0, 141L, false, List.of("redstone")), new World("the_long_dark", "The Long Dark", GameType.ADVENTURE, false, false, false, 5270400000L, "", 0, 37L, false, List.of()), new World("old_harrow", "Old Harrow", GameType.SURVIVAL, false, false, false, 14688000000L, "1.20.4", 3700, 1830L, false, List.of()), new World("snapshot_playground", "Snapshot Playground", GameType.CREATIVE, false, true, true, 1209600000L, "", 0, 12L, false, List.of()), new World("winter_outpost", "Winter Outpost", GameType.SURVIVAL, false, false, false, 10368000000L, "", 0, 264L, false, List.of()));
   }

   private static record World(String id, String name, GameType mode, boolean hardcore, boolean cheats, boolean experimental, long ago, String version, int dataVersion, long megabytes, boolean favorite, List<String> tags) {
   }
}
