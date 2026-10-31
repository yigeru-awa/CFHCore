package com.onehumanawa.cfhcore.content.gui.core.stage;

import com.mojang.serialization.Lifecycle;
import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalLong;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.animal.WolfVariant;
import net.minecraft.world.entity.animal.WolfVariants;
import net.minecraft.world.entity.decoration.PaintingVariant;
import net.minecraft.world.entity.decoration.PaintingVariants;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BannerPatterns;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;

public final class StageRegistries {
   private static RegistryAccess access;
   private static Holder<Biome> biome;
   private static Holder<DimensionType> dimensionType;
   private static final List<ResourceKey<DamageType>> DAMAGE_TYPES;

   public static synchronized RegistryAccess access() {
      if (access == null) {
         build();
      }

      return access;
   }

   public static synchronized Holder<Biome> biome() {
      if (access == null) {
         build();
      }

      return biome;
   }

   public static synchronized Holder<DimensionType> dimensionType() {
      if (access == null) {
         build();
      }

      return dimensionType;
   }

   private static void build() {
      MappedRegistry<WolfVariant> wolves = new MappedRegistry(Registries.WOLF_VARIANT, Lifecycle.stable());
      wolf(wolves, WolfVariants.PALE, "wolf");
      wolf(wolves, WolfVariants.SPOTTED, "wolf_spotted");
      wolf(wolves, WolfVariants.SNOWY, "wolf_snowy");
      wolf(wolves, WolfVariants.BLACK, "wolf_black");
      wolf(wolves, WolfVariants.ASHEN, "wolf_ashen");
      wolf(wolves, WolfVariants.RUSTY, "wolf_rusty");
      wolf(wolves, WolfVariants.WOODS, "wolf_woods");
      wolf(wolves, WolfVariants.CHESTNUT, "wolf_chestnut");
      wolf(wolves, WolfVariants.STRIPED, "wolf_striped");
      wolves.freeze();
      MappedRegistry<DamageType> damage = new MappedRegistry(Registries.DAMAGE_TYPE, Lifecycle.stable());

      for(ResourceKey<DamageType> key : DAMAGE_TYPES) {
         Registry.register(damage, key, new DamageType(key.location().getPath(), 0.1F));
      }

      damage.freeze();
      MappedRegistry<DimensionType> dims = new MappedRegistry(Registries.DIMENSION_TYPE, Lifecycle.stable());
      Registry.register(dims, BuiltinDimensionTypes.OVERWORLD, new DimensionType(OptionalLong.of(6000L), true, false, false, true, (double)1.0F, true, false, -64, 384, 384, BlockTags.INFINIBURN_OVERWORLD, BuiltinDimensionTypes.OVERWORLD_EFFECTS, 0.0F, new DimensionType.MonsterSettings(false, true, UniformInt.of(0, 7), 0)));
      dims.freeze();
      MappedRegistry<Biome> biomes = new MappedRegistry(Registries.BIOME, Lifecycle.stable());
      Registry.register(biomes, Biomes.PLAINS, (new Biome.BiomeBuilder()).hasPrecipitation(true).temperature(0.8F).downfall(0.4F).specialEffects((new BiomeSpecialEffects.Builder()).fogColor(12638463).waterColor(4159204).waterFogColor(329011).skyColor(7907327).build()).mobSpawnSettings(MobSpawnSettings.EMPTY).generationSettings(BiomeGenerationSettings.EMPTY).build());
      biomes.freeze();
      MappedRegistry<PaintingVariant> paintings = new MappedRegistry(Registries.PAINTING_VARIANT, Lifecycle.stable());
      Registry.register(paintings, PaintingVariants.KEBAB, new PaintingVariant(1, 1, ResourceLocation.withDefaultNamespace("kebab")));
      paintings.freeze();
      final MappedRegistry<BannerPattern> patterns = new MappedRegistry(Registries.BANNER_PATTERN, Lifecycle.stable());

      try {
         BannerPatterns.bootstrap(new BootstrapContext<BannerPattern>() {
            public Holder.Reference<BannerPattern> register(ResourceKey<BannerPattern> key, BannerPattern value, Lifecycle lifecycle) {
               return Registry.registerForHolder(patterns, key, value);
            }

            public <S> HolderGetter<S> lookup(ResourceKey<? extends Registry<? extends S>> registry) {
               throw new UnsupportedOperationException("banner patterns look nothing up");
            }
         });
      } catch (RuntimeException e) {
         CFHGui.LOGGER.debug("[CFHGui] stage: banner patterns not registered: {}", e.toString());
      }

      patterns.freeze();
      List<Registry<?>> all = new ArrayList();

      for(Registry<?> r : BuiltInRegistries.REGISTRY) {
         all.add(r);
      }

      all.add(wolves);
      all.add(damage);
      all.add(dims);
      all.add(biomes);
      all.add(paintings);
      all.add(patterns);
      access = new RegistryAccess.ImmutableRegistryAccess(all);
      biome = biomes.getHolderOrThrow(Biomes.PLAINS);
      dimensionType = dims.getHolderOrThrow(BuiltinDimensionTypes.OVERWORLD);
      CFHGui.LOGGER.debug("[CFHGui] stage registries ready ({} registries)", all.size());
   }

   private static void wolf(MappedRegistry<WolfVariant> registry, ResourceKey<WolfVariant> key, String name) {
      Registry.register(registry, key, new WolfVariant(ResourceLocation.withDefaultNamespace("entity/wolf/" + name), ResourceLocation.withDefaultNamespace("entity/wolf/" + name + "_tame"), ResourceLocation.withDefaultNamespace("entity/wolf/" + name + "_angry"), HolderSet.empty()));
   }

   private StageRegistries() {
   }

   static {
      DAMAGE_TYPES = List.of(DamageTypes.ARROW, DamageTypes.BAD_RESPAWN_POINT, DamageTypes.CACTUS, DamageTypes.CAMPFIRE, DamageTypes.CRAMMING, DamageTypes.DRAGON_BREATH, DamageTypes.DROWN, DamageTypes.DRY_OUT, DamageTypes.EXPLOSION, DamageTypes.FALL, DamageTypes.FALLING_ANVIL, DamageTypes.FALLING_BLOCK, DamageTypes.FALLING_STALACTITE, DamageTypes.FELL_OUT_OF_WORLD, DamageTypes.FIREBALL, DamageTypes.FIREWORKS, DamageTypes.FLY_INTO_WALL, DamageTypes.FREEZE, DamageTypes.GENERIC, DamageTypes.GENERIC_KILL, DamageTypes.HOT_FLOOR, DamageTypes.INDIRECT_MAGIC, DamageTypes.IN_FIRE, DamageTypes.IN_WALL, DamageTypes.LAVA, DamageTypes.LIGHTNING_BOLT, DamageTypes.MAGIC, DamageTypes.MOB_ATTACK, DamageTypes.MOB_ATTACK_NO_AGGRO, DamageTypes.MOB_PROJECTILE, DamageTypes.ON_FIRE, DamageTypes.OUTSIDE_BORDER, DamageTypes.PLAYER_ATTACK, DamageTypes.PLAYER_EXPLOSION, DamageTypes.SONIC_BOOM, DamageTypes.SPIT, DamageTypes.STALAGMITE, DamageTypes.STARVE, DamageTypes.STING, DamageTypes.SWEET_BERRY_BUSH, DamageTypes.THORNS, DamageTypes.THROWN, DamageTypes.TRIDENT, DamageTypes.UNATTRIBUTED_FIREBALL, DamageTypes.WIND_CHARGE, DamageTypes.WITHER, DamageTypes.WITHER_SKULL);
   }
}
