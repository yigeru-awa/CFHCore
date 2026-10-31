package com.onehumanawa.cfhcore.content.gui.core.stage.node;

import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageLevel;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class StructureNode extends MeshedBlocksNode {
   public StructureNode(StageLevel level, CompoundTag template) {
      this(level, template, (Set)null);
   }

   public StructureNode(StageLevel level, CompoundTag template, @Nullable Set<BlockPos> hide) {
      super(level);
      this.load(template, hide);
      this.named("structure");
   }

   private void load(CompoundTag tag, @Nullable Set<BlockPos> hide) {
      ListTag sizeTag = tag.getList("size", 3);
      Vec3i sz = sizeTag.size() == 3 ? new Vec3i(sizeTag.getInt(0), sizeTag.getInt(1), sizeTag.getInt(2)) : Vec3i.ZERO;
      ListTag palette;
      if (tag.contains("palettes", 9)) {
         ListTag palettes = tag.getList("palettes", 9);
         palette = palettes.isEmpty() ? new ListTag() : palettes.getList(0);
      } else {
         palette = tag.getList("palette", 10);
      }

      BlockState[] states = new BlockState[palette.size()];

      for(int i = 0; i < palette.size(); ++i) {
         try {
            states[i] = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), palette.getCompound(i));
         } catch (Exception var20) {
            states[i] = Blocks.AIR.defaultBlockState();
         }
      }

      ListTag blocks = tag.getList("blocks", 10);
      int placed = 0;
      int maxX = sz.getX() - 1;
      int maxY = sz.getY() - 1;
      int maxZ = sz.getZ() - 1;

      for(int i = 0; i < blocks.size(); ++i) {
         CompoundTag b = blocks.getCompound(i);
         ListTag pos = b.getList("pos", 3);
         if (pos.size() == 3) {
            int x = pos.getInt(0);
            int y = pos.getInt(1);
            int z = pos.getInt(2);
            int stateIndex = b.getInt("state");
            if (stateIndex >= 0 && stateIndex < states.length) {
               BlockState state = states[stateIndex];
               if (!state.isAir() && (hide == null || !hide.contains(new BlockPos(x, y, z)))) {
                  maxX = Math.max(maxX, x);
                  maxY = Math.max(maxY, y);
                  maxZ = Math.max(maxZ, z);
                  this.set(x, y, z, state, b.contains("nbt", 10) ? b.getCompound("nbt") : null);
                  ++placed;
               }
            }
         }
      }

      this.finishFill(new Vec3i(maxX + 1, maxY + 1, maxZ + 1));
      CFHGui.LOGGER.debug("[CFHGui] stage: structure {}x{}x{}, {} blocks", new Object[]{this.size.getX(), this.size.getY(), this.size.getZ(), placed});
   }

   public static @Nullable CompoundTag readResource(ResourceLocation location) {
      try {
         Resource res = (Resource)Minecraft.getInstance().getResourceManager().getResource(location).orElse(null);
         if (res == null) {
            return null;
         } else {
            InputStream in = res.open();

            CompoundTag var3;
            try {
               var3 = NbtIo.readCompressed(in, NbtAccounter.unlimitedHeap());
            } catch (Throwable var6) {
               if (in != null) {
                  try {
                     in.close();
                  } catch (Throwable var5) {
                     var6.addSuppressed(var5);
                  }
               }

               throw var6;
            }

            if (in != null) {
               in.close();
            }

            return var3;
         }
      } catch (IOException e) {
         CFHGui.LOGGER.warn("[CFHGui] stage: cannot read structure {}: {}", location, e.toString());
         return null;
      }
   }

   public static @Nullable CompoundTag readFile(Path file) {
      if (!Files.isRegularFile(file, new LinkOption[0])) {
         return null;
      } else {
         try {
            InputStream in = Files.newInputStream(file);

            CompoundTag var2;
            try {
               var2 = NbtIo.readCompressed(in, NbtAccounter.unlimitedHeap());
            } catch (Throwable var5) {
               if (in != null) {
                  try {
                     in.close();
                  } catch (Throwable var4) {
                     var5.addSuppressed(var4);
                  }
               }

               throw var5;
            }

            if (in != null) {
               in.close();
            }

            return var2;
         } catch (IOException e) {
            CFHGui.LOGGER.warn("[CFHGui] stage: cannot read structure {}: {}", file, e.toString());
            return null;
         }
      }
   }
}
