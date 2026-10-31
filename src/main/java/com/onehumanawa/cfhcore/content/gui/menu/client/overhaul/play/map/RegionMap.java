package com.onehumanawa.cfhcore.content.gui.menu.client.overhaul.play.map;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.MapColor.Brightness;
import org.jetbrains.annotations.Nullable;

public final class RegionMap {
   public static final int SIZE = 512;
   private static final int SECTOR = 4096;

   public static @Nullable Picture render(Path regionFile) {
      if (!Files.isRegularFile(regionFile, new LinkOption[0])) {
         return null;
      } else {
         int[] pixels = new int[262144];
         short[] heights = new short[262144];
         byte[] colours = new byte[262144];
         short[] depths = new short[262144];
         Arrays.fill(heights, (short)Short.MIN_VALUE);
         int chunks = 0;

         try {
            FileChannel ch = FileChannel.open(regionFile, StandardOpenOption.READ);

            label97: {
               Object var9;
               try {
                  long size = ch.size();
                  if (size >= 8192L) {
                     ByteBuffer header = ByteBuffer.allocate(4096);
                     readFully(ch, header, 0L);
                     header.flip();
                     int i = 0;

                     while(true) {
                        if (i >= 1024) {
                           break label97;
                        }

                        int entry = header.getInt(i * 4);
                        int offset = entry >>> 8;
                        int sectors = entry & 255;
                        if (offset >= 2 && sectors != 0) {
                           long at = (long)offset * 4096L;
                           if (at + 5L <= size) {
                              ByteBuffer head = ByteBuffer.allocate(5);
                              readFully(ch, head, at);
                              head.flip();
                              int length = head.getInt();
                              int compression = head.get() & 255;
                              if (length > 1 && length <= sectors * 4096 && (compression & 128) == 0 && at + 4L + (long)length <= size) {
                                 ByteBuffer body = ByteBuffer.allocate(length - 1);
                                 readFully(ch, body, at + 5L);

                                 try {
                                    CompoundTag chunk = decode(body.array(), compression);
                                    if (chunk != null && paint(chunk, i & 31, i >> 5, heights, colours, depths)) {
                                       ++chunks;
                                    }
                                 } catch (Exception var22) {
                                 }
                              }
                           }
                        }

                        ++i;
                     }
                  }

                  var9 = null;
               } catch (Throwable var23) {
                  if (ch != null) {
                     try {
                        ch.close();
                     } catch (Throwable var21) {
                        var23.addSuppressed(var21);
                     }
                  }

                  throw var23;
               }

               if (ch != null) {
                  ch.close();
               }

               return (Picture)var9;
            }

            if (ch != null) {
               ch.close();
            }
         } catch (IOException var24) {
            return null;
         }

         if (chunks == 0) {
            return null;
         } else {
            shade(pixels, heights, colours, depths);
            return new Picture(pixels, chunks);
         }
      }
   }

   private static void readFully(FileChannel ch, ByteBuffer into, long at) throws IOException {
      int n;
      for(long pos = at; into.hasRemaining(); pos += (long)n) {
         n = ch.read(into, pos);
         if (n < 0) {
            break;
         }
      }

   }

   private static @Nullable CompoundTag decode(byte[] data, int compression) throws IOException {
      InputStream raw = new ByteArrayInputStream(data);
      Object var10000;
      switch (compression) {
         case 1 -> var10000 = new GZIPInputStream(raw);
         case 2 -> var10000 = new InflaterInputStream(raw);
         case 3 -> var10000 = raw;
         default -> var10000 = null;
      }

      InputStream in = (InputStream)var10000;
      if (in == null) {
         return null;
      } else {
         DataInputStream din = new DataInputStream(new BufferedInputStream(in, 16384));

         CompoundTag var5;
         try {
            var5 = NbtIo.read(din, NbtAccounter.unlimitedHeap());
         } catch (Throwable var8) {
            try {
               din.close();
            } catch (Throwable var7) {
               var8.addSuppressed(var7);
            }

            throw var8;
         }

         din.close();
         return var5;
      }
   }

   private static boolean paint(CompoundTag chunk, int cx, int cz, short[] heights, byte[] colours, short[] depths) {
      String status = chunk.getString("Status");
      if (!status.endsWith("full")) {
         return false;
      } else {
         CompoundTag maps = chunk.getCompound("Heightmaps");
         long[] surface = maps.getLongArray("WORLD_SURFACE");
         if (surface.length == 0) {
            return false;
         } else {
            long[] floor = maps.getLongArray("OCEAN_FLOOR");
            ListTag sections = chunk.getList("sections", 10);
            int minSection = chunk.contains("yPos", 99) ? chunk.getInt("yPos") : -4;
            Map<Integer, Section> byY = new HashMap();

            for(int i = 0; i < sections.size(); ++i) {
               CompoundTag s = sections.getCompound(i);
               int y = s.getByte("Y");
               minSection = Math.min(minSection, y);
               if (s.contains("block_states", 10)) {
                  byY.put(y, new Section(s.getCompound("block_states")));
               }
            }

            int minY = minSection * 16;
            int bits = Math.max(1, surface.length == 0 ? 9 : 64 / (int)Math.ceil((double)256.0F / (double)surface.length));

            for(int z = 0; z < 16; ++z) {
               for(int x = 0; x < 16; ++x) {
                  int h = unpack(surface, bits, z * 16 + x);
                  if (h > 0) {
                     int y = minY + h - 1;
                     BlockState state = block(byY, x, y, z);

                     for(int guard = 0; guard < 24 && y > minY && state.getMapColor(EmptyBlockGetter.INSTANCE, BlockPos.ZERO) == MapColor.NONE; ++guard) {
                        --y;
                        state = block(byY, x, y, z);
                     }

                     MapColor colour = state.getMapColor(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
                     if (colour != MapColor.NONE) {
                        int at = (cz * 16 + z) * 512 + cx * 16 + x;
                        heights[at] = (short)y;
                        colours[at] = (byte)colour.id;
                        if (!state.getFluidState().isEmpty() && floor.length == surface.length) {
                           depths[at] = (short)Math.max(1, h - unpack(floor, bits, z * 16 + x));
                        }
                     }
                  }
               }
            }

            return true;
         }
      }
   }

   private static int unpack(long[] data, int bits, int index) {
      int perLong = 64 / bits;
      int li = index / perLong;
      return li >= data.length ? 0 : (int)(data[li] >>> index % perLong * bits & (1L << bits) - 1L);
   }

   private static BlockState block(Map<Integer, Section> sections, int x, int y, int z) {
      Section s = (Section)sections.get(y >> 4);
      return s == null ? Blocks.AIR.defaultBlockState() : s.get(x, y & 15, z);
   }

   private static void shade(int[] pixels, short[] heights, byte[] colours, short[] depths) {
      for(int z = 0; z < 512; ++z) {
         for(int x = 0; x < 512; ++x) {
            int at = z * 512 + x;
            int id = colours[at] & 255;
            if (id != 0) {
               MapColor.Brightness b = Brightness.NORMAL;
               if (depths[at] > 0) {
                  double d = (double)depths[at] * 0.1 + (double)(x + z & 1) * 0.2;
                  b = d < (double)0.5F ? Brightness.HIGH : (d > 0.9 ? Brightness.LOW : Brightness.NORMAL);
               } else if (z > 0 && heights[at - 512] != Short.MIN_VALUE) {
                  int slope = heights[at] - heights[at - 512];
                  b = slope > 0 ? Brightness.HIGH : (slope < 0 ? Brightness.LOW : Brightness.NORMAL);
               }

               pixels[at] = MapColor.byId(id).calculateRGBColor(b);
            }
         }
      }

   }

   private RegionMap() {
   }

   public static record Picture(int[] abgr, int chunks) {
   }

   private static final class Section {
      private final BlockState[] palette;
      private final long[] data;
      private final int bits;

      Section(CompoundTag states) {
         ListTag list = states.getList("palette", 10);
         this.palette = new BlockState[Math.max(1, list.size())];

         for(int i = 0; i < this.palette.length; ++i) {
            BlockState s = Blocks.AIR.defaultBlockState();
            if (i < list.size()) {
               try {
                  s = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), list.getCompound(i));
               } catch (Exception var6) {
                  s = Blocks.STONE.defaultBlockState();
               }
            }

            this.palette[i] = s;
         }

         this.data = states.getLongArray("data");
         this.bits = Math.max(4, 32 - Integer.numberOfLeadingZeros(this.palette.length - 1));
      }

      BlockState get(int x, int y, int z) {
         if (this.palette.length != 1 && this.data.length != 0) {
            int index = y << 8 | z << 4 | x;
            int id = RegionMap.unpack(this.data, this.bits, index);
            return id < this.palette.length ? this.palette[id] : this.palette[0];
         } else {
            return this.palette[0];
         }
      }
   }
}
