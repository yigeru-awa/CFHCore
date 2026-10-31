package com.onehumanawa.cfhcore.content.gui.core.stage;

import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Collection;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

public final class StageAnimations {
   private static @Nullable MethodHandle mark = find();

   public static boolean needed() {
      return mark != null;
   }

   public static void keep(Collection<TextureAtlasSprite> sprites) {
      MethodHandle m = mark;
      if (m != null && !sprites.isEmpty()) {
         try {
            for(TextureAtlasSprite s : sprites) {
               m.invoke(s);
            }
         } catch (Throwable t) {
            mark = null;
            CFHGui.LOGGER.warn("[CFHGui] stage: animated textures could not be kept moving: {}", t.toString());
         }

      }
   }

   public static boolean animated(TextureAtlasSprite sprite) {
      return sprite.contents().createTicker() != null;
   }

   public static void collect(BakedModel model, @Nullable BlockState state, Set<TextureAtlasSprite> into) {
      RandomSource random = RandomSource.create(42L);

      for(Direction d : Direction.values()) {
         random.setSeed(42L);
         collect(model.getQuads(state, d, random), into);
      }

      random.setSeed(42L);
      collect(model.getQuads(state, (Direction)null, random), into);
   }

   private static void collect(Iterable<BakedQuad> quads, Set<TextureAtlasSprite> into) {
      for(BakedQuad q : quads) {
         add(q.getSprite(), into);
      }

   }

   public static void collect(FluidState fluid, Set<TextureAtlasSprite> into) {
      BlockModelShaper shaper = Minecraft.getInstance().getModelManager().getBlockModelShaper();
      if (!fluid.is(Fluids.WATER) && !fluid.is(Fluids.FLOWING_WATER)) {
         if (fluid.is(Fluids.LAVA) || fluid.is(Fluids.FLOWING_LAVA)) {
            add(shaper.getBlockModel(Blocks.LAVA.defaultBlockState()).getParticleIcon(), into);
            add(ModelBakery.LAVA_FLOW.sprite(), into);
         }
      } else {
         add(shaper.getBlockModel(Blocks.WATER.defaultBlockState()).getParticleIcon(), into);
         add(ModelBakery.WATER_FLOW.sprite(), into);
         add(ModelBakery.WATER_OVERLAY.sprite(), into);
      }

   }

   public static void add(@Nullable TextureAtlasSprite s, Set<TextureAtlasSprite> into) {
      if (s != null && !into.contains(s) && animated(s)) {
         into.add(s);
      }

   }

   private static @Nullable MethodHandle find() {
      MethodType type = MethodType.methodType(Void.TYPE, TextureAtlasSprite.class);

      try {
         Class<?> api = Class.forName("net.caffeinemc.mods.sodium.api.texture.SpriteUtil");
         Object instance = api.getField("INSTANCE").get(null);
         return MethodHandles.publicLookup().findVirtual(api, "markSpriteActive", type).bindTo(instance);
      } catch (LinkageError | RuntimeException | ReflectiveOperationException var4) {
         try {
            Class<?> api = Class.forName("org.embeddedt.embeddium.api.render.texture.SpriteUtil");
            return MethodHandles.publicLookup().findStatic(api, "markSpriteActive", type);
         } catch (LinkageError | RuntimeException | ReflectiveOperationException var3) {
            return null;
         }
      }
   }

   private StageAnimations() {
   }
}
