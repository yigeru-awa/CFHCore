package com.onehumanawa.cfhcore.content.gui.core.stage.builds;

import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageRegistries;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

public final class BuildEntities {
   private static final Set<BlockEntityType<?>> FAILED = ConcurrentHashMap.newKeySet();
   private static final Map<Class<?>, Optional<Method>> VIRTUAL = new ConcurrentHashMap();
   private static @Nullable List<Field> frustumFields;
   private static @Nullable Frustum everything;

   private BuildEntities() {
   }

   public static @Nullable BlockEntity make(BlockPos pos, BlockState state, @Nullable CompoundTag nbt) {
      if (!state.hasBlockEntity()) {
         return null;
      } else {
         BlockEntityType<?> type = null;
         if (nbt != null && nbt.contains("id")) {
            ResourceLocation id = ResourceLocation.tryParse(nbt.getString("id"));
            type = id == null ? null : (BlockEntityType)BuiltInRegistries.BLOCK_ENTITY_TYPE.getOptional(id).orElse(null);
            if (type == null || !type.isValid(state)) {
               return null;
            }
         } else {
            for(BlockEntityType<?> t : BuiltInRegistries.BLOCK_ENTITY_TYPE) {
               if (t.isValid(state)) {
                  type = t;
                  break;
               }
            }

            if (type == null) {
               return null;
            }
         }

         if (FAILED.contains(type)) {
            return null;
         } else {
            BlockEntity be;
            try {
               be = type.create(pos, state);
            } catch (LinkageError | RuntimeException e) {
               if (FAILED.add(type)) {
                  CFHGui.LOGGER.info("[CFHGui] build: {} of {} cannot be made outside a world and is not drawn in scenes ({})", new Object[]{BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(type), state.getBlock(), ((Throwable)e).getMessage() == null ? ((Throwable)e).toString() : ((Throwable)e).getMessage()});
               }

               return null;
            }

            if (be == null) {
               return null;
            } else {
               if (nbt != null) {
                  try {
                     be.loadWithComponents(nbt.copy(), StageRegistries.access());
                  } catch (RuntimeException var6) {
                  }
               }

               return be;
            }
         }
      }
   }

   public static void virtual(BlockEntity be) {
      Optional<Method> m = (Optional)VIRTUAL.computeIfAbsent(be.getClass(), BuildEntities::markVirtual);
      if (!m.isEmpty()) {
         try {
            ((Method)m.get()).invoke(be);
         } catch (RuntimeException | ReflectiveOperationException var3) {
            VIRTUAL.put(be.getClass(), Optional.empty());
         }

      }
   }

   private static Optional<Method> markVirtual(Class<?> type) {
      try {
         Method m = type.getMethod("markVirtual");
         return m.getParameterCount() == 0 && !Modifier.isStatic(m.getModifiers()) ? Optional.of(m) : Optional.empty();
      } catch (RuntimeException | NoSuchMethodException var2) {
         return Optional.empty();
      }
   }

   public static Runnable cullingAnything() {
      LevelRenderer lr = Minecraft.getInstance().levelRenderer;
      if (lr == null) {
         return () -> {
         };
      } else {
         List<Field> fields = frustumFields;
         if (fields == null) {
            fields = new ArrayList();

            for(Field f : LevelRenderer.class.getDeclaredFields()) {
               if (f.getType() == Frustum.class && !Modifier.isStatic(f.getModifiers())) {
                  try {
                     f.setAccessible(true);
                     fields.add(f);
                  } catch (RuntimeException var8) {
                  }
               }
            }

            frustumFields = fields;
         }

         if (fields.isEmpty()) {
            return () -> {
            };
         } else {
            List<Field> set = new ArrayList(fields.size());

            for(Field f : fields) {
               try {
                  if (f.get(lr) == null) {
                     if (everything == null) {
                        everything = new Frustum(new Matrix4f(), (new Matrix4f()).ortho(-3.0E7F, 3.0E7F, -3.0E7F, 3.0E7F, -3.0E7F, 3.0E7F));
                        everything.prepare((double)0.0F, (double)0.0F, (double)0.0F);
                     }

                     f.set(lr, everything);
                     set.add(f);
                  }
               } catch (RuntimeException | ReflectiveOperationException var7) {
               }
            }

            return set.isEmpty() ? () -> {
            } : () -> {
               for(Field f : set) {
                  try {
                     if (f.get(lr) == everything) {
                        f.set(lr, null);
                     }
                  } catch (RuntimeException | ReflectiveOperationException var5) {
                  }
               }

            };
         }
      }
   }
}
