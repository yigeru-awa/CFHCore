package com.onehumanawa.cfhcore.content.gui.menu.client.overhaul.play;

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;

public record LevelInfo(boolean known, long seed, boolean hasSeed, long dayTime, long gameTime, int difficulty, boolean difficultyLocked, int spawnX, int spawnY, int spawnZ, boolean hasPlayer, double playerX, double playerY, double playerZ, String dimension, float health, int food, int xpLevel, boolean raining, boolean thundering, List<String> dataPacks) {
   public static final LevelInfo UNKNOWN = new LevelInfo(false, 0L, false, 0L, 0L, -1, false, 0, 64, 0, false, (double)0.0F, (double)64.0F, (double)0.0F, "", 20.0F, 20, 0, false, false, List.of());

   public long days() {
      return this.gameTime / 24000L;
   }

   public int centreX() {
      return this.hasPlayer && this.overworld() ? (int)Math.floor(this.playerX) : this.spawnX;
   }

   public int centreZ() {
      return this.hasPlayer && this.overworld() ? (int)Math.floor(this.playerZ) : this.spawnZ;
   }

   public boolean overworld() {
      return this.dimension.isEmpty() || this.dimension.endsWith("overworld");
   }

   public static CompletableFuture<LevelInfo> read(Path folder) {
      return CompletableFuture.supplyAsync(() -> parse(folder.resolve("level.dat")), PlayIo.POOL).thenApplyAsync((i) -> i, Minecraft.getInstance());
   }

   private static LevelInfo parse(Path file) {
      if (!Files.isRegularFile(file, new LinkOption[0])) {
         return UNKNOWN;
      } else {
         try {
            CompoundTag root = NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
            CompoundTag data = root.getCompound("Data");
            CompoundTag gen = data.getCompound("WorldGenSettings");
            boolean hasSeed = gen.contains("seed", 99);
            CompoundTag player = data.getCompound("Player");
            boolean hasPlayer = player.contains("Pos", 9);
            double px = (double)0.0F;
            double py = (double)64.0F;
            double pz = (double)0.0F;
            if (hasPlayer) {
               ListTag pos = player.getList("Pos", 6);
               if (pos.size() == 3) {
                  px = pos.getDouble(0);
                  py = pos.getDouble(1);
                  pz = pos.getDouble(2);
               }
            }

            List<String> packs = new ArrayList();
            ListTag enabled = data.getCompound("DataPacks").getList("Enabled", 8);

            for(int i = 0; i < enabled.size(); ++i) {
               String p = enabled.getString(i);
               if (!p.equals("vanilla")) {
                  packs.add(p);
               }
            }

            return new LevelInfo(true, gen.getLong("seed"), hasSeed, data.getLong("DayTime"), data.getLong("Time"), data.contains("Difficulty", 99) ? data.getByte("Difficulty") : -1, data.getBoolean("DifficultyLocked"), data.getInt("SpawnX"), data.getInt("SpawnY"), data.getInt("SpawnZ"), hasPlayer, px, py, pz, player.getString("Dimension"), player.contains("Health", 99) ? player.getFloat("Health") : 20.0F, player.contains("foodLevel", 99) ? player.getInt("foodLevel") : 20, player.getInt("XpLevel"), data.getBoolean("raining"), data.getBoolean("thundering"), List.copyOf(packs));
         } catch (Exception var17) {
            return UNKNOWN;
         }
      }
   }
}
