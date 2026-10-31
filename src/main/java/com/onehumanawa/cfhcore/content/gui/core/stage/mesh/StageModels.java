package com.onehumanawa.cfhcore.content.gui.core.stage.mesh;

import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.block.model.BlockModel.GuiLight;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.SimpleBakedModel;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.jetbrains.annotations.Nullable;

public final class StageModels {
   private static final Map<ResourceLocation, BakedModel> CACHE = new HashMap();
   private static final Map<ResourceLocation, BlockModel> UNBAKED = new HashMap();
   private static final FaceBakery FACE_BAKERY = new FaceBakery();
   private static final BlockModel EMPTY;
   private static final Map<ResourceLocation, Long> FAILED;
   private static final Set<ResourceLocation> SAID;
   private static final long RETRY_MS = 2000L;

   public static synchronized @Nullable BakedModel get(ResourceLocation model) {
      BakedModel cached = (BakedModel)CACHE.get(model);
      if (cached != null) {
         return cached;
      } else {
         long now = System.currentTimeMillis();
         Long failedAt = (Long)FAILED.get(model);
         if (failedAt != null && now - failedAt < 2000L) {
            return null;
         } else {
            BakedModel baked = null;

            try {
               baked = bake(model);
            } catch (Exception e) {
               if (SAID.add(model)) {
                  CFHGui.LOGGER.warn("[CFHGui] stage: cannot bake model {}: {}", model, e.toString());
               }
            }

            if (baked == null) {
               FAILED.put(model, now);
               return null;
            } else {
               if (failedAt != null) {
                  CFHGui.LOGGER.info("[CFHGui] stage: model {} could be loaded after all", model);
               }

               FAILED.remove(model);
               CACHE.put(model, baked);
               return baked;
            }
         }
      }
   }

   public static synchronized void clear() {
      CACHE.clear();
      UNBAKED.clear();
      FAILED.clear();
      SAID.clear();
   }

   private static @Nullable BakedModel bake(ResourceLocation id) {
      BlockModel model = unbaked(id);
      if (model == null) {
         return null;
      } else {
         model.resolveParents(StageModels::parent);
         Function<Material, TextureAtlasSprite> sprites = (m) -> (TextureAtlasSprite)Minecraft.getInstance().getTextureAtlas(m.atlasLocation()).apply(m.texture());
         List<BakedQuad> unculled = new ArrayList();
         Map<Direction, List<BakedQuad>> culled = new EnumMap(Direction.class);

         for(Direction d : Direction.values()) {
            culled.put(d, new ArrayList());
         }

         for(BlockElement element : model.getElements()) {
            for(Map.Entry<Direction, BlockElementFace> e : element.faces.entrySet()) {
               BlockElementFace face = (BlockElementFace)e.getValue();
               TextureAtlasSprite sprite = (TextureAtlasSprite)sprites.apply(model.getMaterial(face.texture()));
               BakedQuad quad = FACE_BAKERY.bakeQuad(element.from, element.to, face, sprite, (Direction)e.getKey(), BlockModelRotation.X0_Y0, element.rotation, element.shade);
               if (face.cullForDirection() == null) {
                  unculled.add(quad);
               } else {
                  ((List)culled.get(face.cullForDirection())).add(quad);
               }
            }
         }

         TextureAtlasSprite particle = (TextureAtlasSprite)sprites.apply(model.getMaterial("particle"));
         return new SimpleBakedModel(unculled, culled, model.hasAmbientOcclusion(), model.getGuiLight().lightLikeBlock(), true, particle, model.getTransforms(), ItemOverrides.EMPTY);
      }
   }

   private static UnbakedModel parent(ResourceLocation id) {
      if (id.getPath().startsWith("builtin/")) {
         return EMPTY;
      } else {
         BlockModel m = unbaked(id);
         return m == null ? EMPTY : m;
      }
   }

   private static @Nullable BlockModel unbaked(ResourceLocation id) {
      BlockModel read = (BlockModel)UNBAKED.get(id);
      if (read != null) {
         return read;
      } else {
         BlockModel model = null;
         ResourceLocation file = ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "models/" + id.getPath() + ".json");

         try {
            Resource res = (Resource)Minecraft.getInstance().getResourceManager().getResource(file).orElse(null);
            if (res != null) {
               BufferedReader reader = res.openAsReader();

               try {
                  model = BlockModel.fromStream(reader);
                  model.name = id.toString();
               } catch (Throwable var9) {
                  if (reader != null) {
                     try {
                        reader.close();
                     } catch (Throwable var8) {
                        var9.addSuppressed(var8);
                     }
                  }

                  throw var9;
               }

               if (reader != null) {
                  reader.close();
               }
            } else if (SAID.add(file)) {
               CFHGui.LOGGER.warn("[CFHGui] stage: model {} not found", file);
            }
         } catch (Exception e) {
            if (SAID.add(file)) {
               CFHGui.LOGGER.warn("[CFHGui] stage: cannot read model {}: {}", file, e.toString());
            }
         }

         if (model != null) {
            UNBAKED.put(id, model);
         }

         return model;
      }
   }

   private StageModels() {
   }

   static {
      EMPTY = new BlockModel((ResourceLocation)null, List.of(), Map.of(), Boolean.FALSE, GuiLight.FRONT, ItemTransforms.NO_TRANSFORMS, List.of());
      FAILED = new HashMap();
      SAID = new HashSet();
   }
}
