package com.onehumanawa.cfhcore.content.gui.core.stage;

import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.AbortableIterationConsumer;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.InactiveProfiler;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.LightChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.ticks.BlackholeTickAccess;
import net.minecraft.world.ticks.LevelTickAccess;
import org.jetbrains.annotations.Nullable;

public final class StageLevel extends Level {
   private static final BlockState AIR;
   private static final BlockState VOID;
   private final Long2ObjectOpenHashMap<BlockState> blocks = new Long2ObjectOpenHashMap();
   private final Long2ObjectOpenHashMap<BlockEntity> blockEntities = new Long2ObjectOpenHashMap();
   private final List<Entity> entities = new ArrayList();
   private final StageChunkSource chunkSource = new StageChunkSource(this);
   private final StageEntityGetter entityGetter;
   private final Holder<Biome> biome;
   private final Scoreboard scoreboard;
   private final TickRateManager tickRate;
   private final DifficultyInstance difficulty;
   private final RandomSource random;
   private final StageLighting lighting;
   private @Nullable RecipeManager recipes;
   private long subTick;
   private int nextRegionX;
   private static final int REGION_SPACING = 4096;
   private final Int2ObjectOpenHashMap<int[]> regionTints;
   private int nextEntityId;
   private final BlockPos.MutableBlockPos scratch;
   private boolean flatShade;
   private boolean alone;
   private long aloneAt;

   public StageLevel(StageLighting lighting) {
      super(new ClientLevel.ClientLevelData(Difficulty.PEACEFUL, false, false), Level.OVERWORLD, StageRegistries.access(), StageRegistries.dimensionType(), () -> InactiveProfiler.INSTANCE, true, false, 0L, 0);
      this.entityGetter = new StageEntityGetter(this.entities);
      this.biome = StageRegistries.biome();
      this.scoreboard = new Scoreboard();
      this.tickRate = new TickRateManager();
      this.difficulty = new DifficultyInstance(Difficulty.PEACEFUL, 0L, 0L, 0.0F);
      this.random = RandomSource.create(20260929L);
      this.regionTints = new Int2ObjectOpenHashMap();
      this.nextEntityId = 1;
      this.scratch = new BlockPos.MutableBlockPos();
      this.lighting = lighting;
   }

   public StageLighting lighting() {
      return this.lighting;
   }

   public BlockPos allocateRegion() {
      BlockPos p = new BlockPos(this.nextRegionX, 0, 0);
      this.nextRegionX += 4096;
      return p;
   }

   public void place(BlockPos pos, BlockState state, @Nullable CompoundTag nbt) {
      this.place(pos, state, nbt, true);
   }

   public void place(BlockPos pos, BlockState state, @Nullable CompoundTag nbt, boolean withEntity) {
      long key = pos.asLong();
      BlockEntity old = (BlockEntity)this.blockEntities.remove(key);
      if (old != null) {
         old.setRemoved();
      }

      if (state.isAir()) {
         this.blocks.remove(key);
      } else {
         BlockPos immutable = pos.immutable();
         this.blocks.put(key, state);
         if (withEntity && state.hasBlockEntity()) {
            Block var10 = state.getBlock();
            if (var10 instanceof EntityBlock) {
               EntityBlock eb = (EntityBlock)var10;
               BlockEntity be = null;

               try {
                  be = nbt != null ? BlockEntity.loadStatic(immutable, state, nbt, this.registryAccess()) : eb.newBlockEntity(immutable, state);
               } catch (RuntimeException e) {
                  StageTrouble.once("the block entity of " + String.valueOf(BuiltInRegistries.BLOCK.getKey(state.getBlock())), "the block is drawn without it", e);
               }

               if (be != null) {
                  be.setLevel(this);
                  this.blockEntities.put(key, be);
               }
            }
         }

      }
   }

   public void clear(BlockPos min, BlockPos max) {
      for(int x = min.getX(); x <= max.getX(); ++x) {
         for(int y = min.getY(); y <= max.getY(); ++y) {
            for(int z = min.getZ(); z <= max.getZ(); ++z) {
               long key = BlockPos.asLong(x, y, z);
               this.blocks.remove(key);
               BlockEntity be = (BlockEntity)this.blockEntities.remove(key);
               if (be != null) {
                  be.setRemoved();
               }
            }
         }
      }

   }

   public boolean isEmptyAt(int x, int y, int z) {
      return !this.blocks.containsKey(BlockPos.asLong(x, y, z));
   }

   public List<BlockEntity> blockEntitiesIn(BlockPos min, BlockPos max, List<BlockEntity> out) {
      out.clear();
      ObjectIterator var4 = this.blockEntities.values().iterator();

      while(var4.hasNext()) {
         BlockEntity be = (BlockEntity)var4.next();
         BlockPos p = be.getBlockPos();
         if (p.getX() >= min.getX() && p.getX() <= max.getX() && p.getY() >= min.getY() && p.getY() <= max.getY() && p.getZ() >= min.getZ() && p.getZ() <= max.getZ()) {
            out.add(be);
         }
      }

      return out;
   }

   public BlockState getBlockState(BlockPos pos) {
      if (this.isOutsideBuildHeight(pos)) {
         return VOID;
      } else {
         BlockState s = (BlockState)this.blocks.get(pos.asLong());
         return s == null ? AIR : s;
      }
   }

   public BlockState getBlockState(int x, int y, int z) {
      BlockState s = (BlockState)this.blocks.get(BlockPos.asLong(x, y, z));
      return s == null ? AIR : s;
   }

   public FluidState getFluidState(BlockPos pos) {
      return this.getBlockState(pos).getFluidState();
   }

   public @Nullable BlockEntity getBlockEntity(BlockPos pos) {
      long key = pos.asLong();
      return this.alone && key != this.aloneAt ? null : (BlockEntity)this.blockEntities.get(key);
   }

   public void alone(@Nullable BlockPos pos) {
      this.alone = pos != null;
      if (pos != null) {
         this.aloneAt = pos.asLong();
      }

   }

   public boolean setBlock(BlockPos pos, BlockState state, int flags, int recursion) {
      if (this.isOutsideBuildHeight(pos)) {
         return false;
      } else {
         this.place(pos, state, (CompoundTag)null);
         return true;
      }
   }

   public boolean removeBlock(BlockPos pos, boolean moving) {
      return this.setBlock(pos, AIR, 3, 512);
   }

   public boolean destroyBlock(BlockPos pos, boolean drop, @Nullable Entity entity, int recursion) {
      return this.setBlock(pos, AIR, 3, 512);
   }

   public void setBlockEntity(BlockEntity be) {
      be.setLevel(this);
      this.blockEntities.put(be.getBlockPos().asLong(), be);
   }

   public void removeBlockEntity(BlockPos pos) {
      BlockEntity be = (BlockEntity)this.blockEntities.remove(pos.asLong());
      if (be != null) {
         be.setRemoved();
      }

   }

   public void blockEntityChanged(BlockPos pos) {
   }

   public void sendBlockUpdated(BlockPos pos, BlockState old, BlockState state, int flags) {
   }

   public float getShade(Direction direction, boolean shade) {
      return this.flatShade ? 1.0F : this.lighting.shade(direction, shade);
   }

   public void flatShade(boolean on) {
      this.flatShade = on;
   }

   public int getBrightness(LightLayer layer, BlockPos pos) {
      return layer == LightLayer.SKY ? this.lighting.skyLight() : this.lighting.blockLight();
   }

   public int getRawBrightness(BlockPos pos, int amount) {
      return Math.max(this.lighting.skyLight() - amount, this.lighting.blockLight());
   }

   public LevelLightEngine getLightEngine() {
      return this.chunkSource.getLightEngine();
   }

   public void tintRegion(BlockPos origin, int grass, int foliage, int water) {
      this.regionTints.put(Math.floorDiv(origin.getX(), 4096), new int[]{grass, foliage, water});
   }

   public void clearRegionTint(BlockPos origin) {
      this.regionTints.remove(Math.floorDiv(origin.getX(), 4096));
   }

   public int getBlockTint(BlockPos pos, ColorResolver resolver) {
      if (!this.regionTints.isEmpty()) {
         int[] t = (int[])this.regionTints.get(Math.floorDiv(pos.getX(), 4096));
         if (t != null) {
            int c = resolver == BiomeColors.GRASS_COLOR_RESOLVER ? t[0] : (resolver == BiomeColors.FOLIAGE_COLOR_RESOLVER ? t[1] : (resolver == BiomeColors.WATER_COLOR_RESOLVER ? t[2] : 0));
            if (c != 0) {
               return c;
            }
         }
      }

      return resolver.getColor((Biome)this.biome.value(), (double)pos.getX(), (double)pos.getZ());
   }

   public Holder<Biome> getUncachedNoiseBiome(int x, int y, int z) {
      return this.biome;
   }

   public int getSkyDarken() {
      return 0;
   }

   public @Nullable ChunkAccess getChunk(int x, int z, ChunkStatus status, boolean create) {
      return null;
   }

   public boolean hasChunk(int x, int z) {
      return true;
   }

   public int getHeight(Heightmap.Types type, int x, int z) {
      int top = this.getMinBuildHeight();

      for(int y = this.getMaxBuildHeight() - 1; y >= this.getMinBuildHeight(); --y) {
         if (this.blocks.containsKey(BlockPos.asLong(x, y, z))) {
            top = y + 1;
            break;
         }
      }

      return top;
   }

   public int getSeaLevel() {
      return 63;
   }

   public FeatureFlagSet enabledFeatures() {
      return FeatureFlags.DEFAULT_FLAGS;
   }

   public ChunkSource getChunkSource() {
      return this.chunkSource;
   }

   public LevelTickAccess<Block> getBlockTicks() {
      return BlackholeTickAccess.emptyLevelList();
   }

   public LevelTickAccess<Fluid> getFluidTicks() {
      return BlackholeTickAccess.emptyLevelList();
   }

   public long nextSubTickCount() {
      return (long)(this.subTick++);
   }

   public DifficultyInstance getCurrentDifficultyAt(BlockPos pos) {
      return this.difficulty;
   }

   public @Nullable MinecraftServer getServer() {
      return null;
   }

   public RandomSource getRandom() {
      return this.random;
   }

   public String gatherChunkSourceStats() {
      return "stage";
   }

   public void playSound(@Nullable Player player, BlockPos pos, SoundEvent sound, SoundSource source, float volume, float pitch) {
   }

   public void playSeededSound(@Nullable Player player, double x, double y, double z, Holder<SoundEvent> sound, SoundSource source, float volume, float pitch, long seed) {
   }

   public void playSeededSound(@Nullable Player player, Entity entity, Holder<SoundEvent> sound, SoundSource source, float volume, float pitch, long seed) {
   }

   public void addParticle(ParticleOptions options, double x, double y, double z, double dx, double dy, double dz) {
   }

   public void levelEvent(@Nullable Player player, int type, BlockPos pos, int data) {
   }

   public void gameEvent(Holder<GameEvent> event, Vec3 pos, GameEvent.Context context) {
   }

   public void destroyBlockProgress(int id, BlockPos pos, int progress) {
   }

   public TickRateManager tickRateManager() {
      return this.tickRate;
   }

   public @Nullable MapItemSavedData getMapData(MapId id) {
      return null;
   }

   public void setMapData(MapId id, MapItemSavedData data) {
   }

   public MapId getFreeMapId() {
      return new MapId(0);
   }

   public Scoreboard getScoreboard() {
      return this.scoreboard;
   }

   public RecipeManager getRecipeManager() {
      if (this.recipes == null) {
         this.recipes = new RecipeManager(this.registryAccess());
      }

      return this.recipes;
   }

   public PotionBrewing potionBrewing() {
      return PotionBrewing.EMPTY;
   }

   protected LevelEntityGetter<Entity> getEntities() {
      return this.entityGetter;
   }

   public @Nullable Entity getEntity(int id) {
      for(Entity e : this.entities) {
         if (e.getId() == id) {
            return e;
         }
      }

      return null;
   }

   public List<? extends Player> players() {
      return List.of();
   }

   public <T extends Entity> @Nullable T spawn(EntityType<T> type, double x, double y, double z, float yaw) {
      T e;
      try {
         e = type.create(this);
      } catch (Exception ex) {
         CFHGui.LOGGER.warn("[CFHGui] stage: cannot create {}: {}", EntityType.getKey(type), ex.toString());
         return null;
      }

      if (e == null) {
         return null;
      } else {
         e.setId(this.nextEntityId++);
         e.moveTo(x, y, z, yaw, 0.0F);
         e.setYHeadRot(yaw);
         e.setYBodyRot(yaw);
         e.setOldPosAndRot();
         this.entities.add(e);
         return e;
      }
   }

   public void discard(Entity e) {
      this.entities.remove(e);
   }

   public List<Entity> entities() {
      return this.entities;
   }

   public void setDayTimeFraction(float fraction) {
   }

   public float getDayTimeFraction() {
      return 0.0F;
   }

   public float getDayTimePerTick() {
      return 1.0F;
   }

   public void setDayTimePerTick(float perTick) {
   }

   static {
      AIR = Blocks.AIR.defaultBlockState();
      VOID = Blocks.VOID_AIR.defaultBlockState();
   }

   private static final class StageChunkSource extends ChunkSource {
      private final StageLevel level;
      private final LevelLightEngine light;

      StageChunkSource(StageLevel level) {
         this.level = level;
         this.light = new LevelLightEngine(this, false, false);
      }

      public @Nullable ChunkAccess getChunk(int x, int z, ChunkStatus status, boolean create) {
         return null;
      }

      public @Nullable LightChunk getChunkForLighting(int x, int z) {
         return null;
      }

      public void tick(BooleanSupplier hasTime, boolean tickChunks) {
      }

      public String gatherStats() {
         return "stage";
      }

      public int getLoadedChunksCount() {
         return 0;
      }

      public LevelLightEngine getLightEngine() {
         return this.light;
      }

      public BlockGetter getLevel() {
         return this.level;
      }
   }

   private static final class StageEntityGetter implements LevelEntityGetter<Entity> {
      private final List<Entity> entities;

      StageEntityGetter(List<Entity> entities) {
         this.entities = entities;
      }

      public @Nullable Entity get(int id) {
         for(Entity e : this.entities) {
            if (e.getId() == id) {
               return e;
            }
         }

         return null;
      }

      public @Nullable Entity get(UUID uuid) {
         for(Entity e : this.entities) {
            if (e.getUUID().equals(uuid)) {
               return e;
            }
         }

         return null;
      }

      public Iterable<Entity> getAll() {
         return this.entities;
      }

      public <U extends Entity> void get(EntityTypeTest<Entity, U> test, AbortableIterationConsumer<U> consumer) {
         for(Entity e : this.entities) {
            U u = (U)(test.tryCast(e));
            if (u != null && consumer.accept(u).shouldAbort()) {
               return;
            }
         }

      }

      public void get(AABB box, Consumer<Entity> consumer) {
         for(Entity e : this.entities) {
            if (e.getBoundingBox().intersects(box)) {
               consumer.accept(e);
            }
         }

      }

      public <U extends Entity> void get(EntityTypeTest<Entity, U> test, AABB box, AbortableIterationConsumer<U> consumer) {
         for(Entity e : this.entities) {
            if (e.getBoundingBox().intersects(box)) {
               U u = (U)(test.tryCast(e));
               if (u != null && consumer.accept(u).shouldAbort()) {
                  return;
               }
            }
         }

      }
   }
}
