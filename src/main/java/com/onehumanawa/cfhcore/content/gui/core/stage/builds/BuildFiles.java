package com.onehumanawa.cfhcore.content.gui.core.stage.builds;

import com.mojang.datafixers.DataFixer;
import com.mojang.serialization.Dynamic;
import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.GZIPInputStream;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.EndTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.ShortTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.Nullable;

public final class BuildFiles {
   private static final int END = 0;
   private static final int BYTE = 1;
   private static final int SHORT = 2;
   private static final int INT = 3;
   private static final int LONG = 4;
   private static final int FLOAT = 5;
   private static final int DOUBLE = 6;
   private static final int BYTE_ARRAY = 7;
   private static final int STRING = 8;
   private static final int LIST = 9;
   private static final int COMPOUND = 10;
   private static final int INT_ARRAY = 11;
   private static final int LONG_ARRAY = 12;
   private static final List<String> UNSEEN = List.of("Items", "Book", "RecordItem", "item", "LootTable", "LootTableSeed", "Lock");
   private static final List<String> UNSEEN_EXTRA = List.of("neoforge:attachments", "NeoForgeData", "ForgeCaps", "ForgeData");

   private BuildFiles() {
   }

   public static BuildVolume read(BuildSpec spec, BuildSpec.Found found) throws IOException {
      InputStream raw = found.open();

      Grid grid;
      try {
         Grid var10000;
         switch (found.extension()) {
            case "nbt" -> var10000 = readStructure(new DataInputStream(new BufferedInputStream(gunzip(raw), 65536)), spec);
            case "litematic" -> var10000 = readLitematic(NbtIo.readCompressed(raw, NbtAccounter.unlimitedHeap()), spec);
            default -> var10000 = readSponge(NbtIo.readCompressed(raw, NbtAccounter.unlimitedHeap()), spec);
         }

         grid = var10000;
      } catch (Throwable var7) {
         if (raw != null) {
            try {
               raw.close();
            } catch (Throwable var6) {
               var7.addSuppressed(var6);
            }
         }

         throw var7;
      }

      if (raw != null) {
         raw.close();
      }

      return place(spec, grid);
   }

   private static InputStream gunzip(InputStream raw) throws IOException {
      BufferedInputStream in = new BufferedInputStream(raw, 65536);
      in.mark(2);
      int a = in.read();
      int b = in.read();
      in.reset();
      return (InputStream)(a == 31 && b == 139 ? new GZIPInputStream(in, 65536) : in);
   }

   private static long key(int x, int y, int z) {
      return (long)(x & 2097151) << 42 | (long)(y & 2097151) << 21 | (long)(z & 2097151);
   }

   private static BuildVolume place(BuildSpec spec, Grid grid) {
      int[] from = spec.from();
      int[] to = spec.to();
      int x0 = from == null ? 0 : Math.max(0, Math.min(from[0], to == null ? from[0] : to[0]));
      int y0 = from == null ? 0 : Math.max(0, Math.min(from[1], to == null ? from[1] : to[1]));
      int z0 = from == null ? 0 : Math.max(0, Math.min(from[2], to == null ? from[2] : to[2]));
      int x1 = to == null ? grid.sizeX() - 1 : Math.min(grid.sizeX() - 1, Math.max(from == null ? to[0] : from[0], to[0]));
      int y1 = to == null ? grid.sizeY() - 1 : Math.min(grid.sizeY() - 1, Math.max(from == null ? to[1] : from[1], to[1]));
      int z1 = to == null ? grid.sizeZ() - 1 : Math.min(grid.sizeZ() - 1, Math.max(from == null ? to[2] : from[2], to[2]));
      int w = x1 - x0 + 1;
      int h = y1 - y0 + 1;
      int d = z1 - z0 + 1;
      if (w > 0 && h > 0 && d > 0) {
         int rotate = spec.rotate();
         boolean quarter = rotate == 90 || rotate == 270;
         BuildVolume.Builder out = new BuildVolume.Builder(quarter ? d : w, h, quarter ? w : d);
         Rotation var10000;
         switch (rotate) {
            case 90 -> var10000 = Rotation.CLOCKWISE_90;
            case 180 -> var10000 = Rotation.CLOCKWISE_180;
            case 270 -> var10000 = Rotation.COUNTERCLOCKWISE_90;
            default -> var10000 = Rotation.NONE;
         }

         Rotation rotation = var10000;
         Map<Integer, BlockState> turned = new HashMap();
         int[] frame = new int[]{0, 0, 0, 0};
         List<CompoundTag> placedAt = new ArrayList();
         grid.blocks((xx, yx, zx, p) -> {
            if (xx >= x0 && xx <= x1 && yx >= y0 && yx <= y1 && zx >= z0 && zx <= z1) {
               BlockState s = (BlockState)turned.computeIfAbsent(p, (i) -> {
                  BlockState raw = grid.state(i);
                  return rotation == Rotation.NONE ? raw : raw.rotate(rotation);
               });
               if (!s.isAir()) {
                  int cx = xx - x0;
                  int cz = zx - z0;
                  int lx;
                  int lz;
                  switch (rotate) {
                     case 90:
                        lx = d - 1 - cz;
                        lz = cx;
                        break;
                     case 180:
                        lx = w - 1 - cx;
                        lz = d - 1 - cz;
                        break;
                     case 270:
                        lx = cz;
                        lz = w - 1 - cx;
                        break;
                     default:
                        lx = cx;
                        lz = cz;
                  }

                  CompoundTag nbt = s.hasBlockEntity() ? grid.entity(xx, yx, zx) : null;
                  if (nbt != null) {
                     int[] last = blockPos(nbt, "LastKnownPos");
                     if (last != null && frame[3] == 0) {
                        frame[0] = last[0] - xx;
                        frame[1] = last[1] - yx;
                        frame[2] = last[2] - zx;
                        frame[3] = 1;
                     }

                     if (nbt.contains("Controller")) {
                        placedAt.add(nbt);
                     }
                  }

                  out.set(lx, yx - y0, lz, s, nbt);
               }
            }
         });
         BuildVolume.Builder placed = out;
         int ax = Math.round(spec.at().x);
         int ay = Math.round(spec.at().y);
         int az = Math.round(spec.at().z);
         int cropX = 0;
         int cropY = 0;
         int cropZ = 0;
         BuildSpec.Fit fit = spec.fit();
         if (fit != null) {
            int[] box = out.bounds();
            if (box != null) {
               placed = out.crop(box[0], box[1], box[2], box[3], box[4], box[5]);
               cropX = box[0];
               cropY = box[1];
               cropZ = box[2];
               ax = fit.x() - (placed.sizeX() - 1) / 2;
               ay = fit.ground();
               az = fit.front() - (placed.sizeZ() - 1);
            }
         }

         for(CompoundTag nbt : placedAt) {
            int[] c = blockPos(nbt, "Controller");
            if (c != null) {
               int fx = c[0] - frame[0] - x0;
               int fy = c[1] - frame[1] - y0;
               int fz = c[2] - frame[2] - z0;
               int vx;
               int vz;
               switch (rotate) {
                  case 90:
                     vx = d - 1 - fz;
                     vz = fx;
                     break;
                  case 180:
                     vx = w - 1 - fx;
                     vz = d - 1 - fz;
                     break;
                  case 270:
                     vx = fz;
                     vz = w - 1 - fx;
                     break;
                  default:
                     vx = fx;
                     vz = fz;
               }

               putBlockPos(nbt, "Controller", vx - cropX, fy - cropY, vz - cropZ);
            }
         }

         for(int[] c : spec.clear()) {
            for(int x = Math.min(c[0], c[3]); x <= Math.max(c[0], c[3]); ++x) {
               for(int y = Math.min(c[1], c[4]); y <= Math.max(c[1], c[4]); ++y) {
                  for(int z = Math.min(c[2], c[5]); z <= Math.max(c[2], c[5]); ++z) {
                     placed.set(x - ax, y - ay, z - az, Blocks.AIR.defaultBlockState(), (CompoundTag)null);
                  }
               }
            }
         }

         for(BuildSpec.Edit e : spec.edits()) {
            BlockState s = BuildStates.parse(e.block());
            if (s != null) {
               for(int x = e.x0(); x <= e.x1(); ++x) {
                  for(int y = e.y0(); y <= e.y1(); ++y) {
                     for(int z = e.z0(); z <= e.z1(); ++z) {
                        placed.set(x - ax, y - ay, z - az, s, (CompoundTag)null);
                     }
                  }
               }
            }
         }

         return placed.build(ax, ay, az);
      } else {
         throw new IllegalArgumentException("the part of the build shown is empty");
      }
   }

   private static @Nullable int[] blockPos(CompoundTag nbt, String key) {
      Tag t = nbt.get(key);
      if (t instanceof IntArrayTag a) {
         if (a.size() == 3) {
            return new int[]{a.get(0).getAsInt(), a.get(1).getAsInt(), a.get(2).getAsInt()};
         }
      }

      if (t instanceof CompoundTag c) {
         if (c.contains("X") && c.contains("Y") && c.contains("Z")) {
            return new int[]{c.getInt("X"), c.getInt("Y"), c.getInt("Z")};
         }
      }

      return null;
   }

   private static void putBlockPos(CompoundTag nbt, String key, int x, int y, int z) {
      Tag var6 = nbt.get(key);
      if (var6 instanceof CompoundTag c) {
         c.putInt("X", x);
         c.putInt("Y", y);
         c.putInt("Z", z);
      } else {
         nbt.putIntArray(key, new int[]{x, y, z});
      }

   }

   private static Grid readStructure(DataInputStream in, BuildSpec spec) throws IOException {
      if (in.readByte() != 10) {
         throw new IOException("not a structure file");
      } else {
         in.readUTF();
         int sx = 0;
         int sy = 0;
         int sz = 0;
         int version = 0;
         List<CompoundTag> palette = null;
         final IntList xs = new IntList();
         final IntList ys = new IntList();
         final IntList zs = new IntList();
         final IntList ps = new IntList();
         Map<Long, CompoundTag> entities = new HashMap();

         while(true) {
            int type = in.readByte();
            if (type == 0) {
               if (palette == null) {
                  throw new IOException("a structure file without a palette");
               }

               final BlockState[] states = resolve(palette, version, spec);
               final Map<Long, CompoundTag> fixedEntities = fixEntities(entities, version);
               final int fX = sx;
               final int fY = sy;
               final int fZ = sz;
               return new Grid() {
                  public int sizeX() {
                     return fX;
                  }

                  public int sizeY() {
                     return fY;
                  }

                  public int sizeZ() {
                     return fZ;
                  }

                  public void blocks(BlockVisitor visit) {
                     for(int i = 0; i < ps.size; ++i) {
                        int p = ps.data[i];
                        if (p >= 0 && p < states.length && !states[p].isAir()) {
                           visit.block(xs.data[i], ys.data[i], zs.data[i], p);
                        }
                     }

                  }

                  public BlockState state(int i) {
                     return i >= 0 && i < states.length ? states[i] : Blocks.AIR.defaultBlockState();
                  }

                  public @Nullable CompoundTag entity(int x, int y, int z) {
                     return (CompoundTag)fixedEntities.get(BuildFiles.key(x, y, z));
                  }
               };
            }

            switch (in.readUTF()) {
               case "size":
                  Tag t = readPayload(in, type, 0);
                  if (t instanceof ListTag) {
                     ListTag l = (ListTag)t;
                     if (l.size() == 3) {
                        sx = l.getInt(0);
                        sy = l.getInt(1);
                        sz = l.getInt(2);
                        break;
                     }
                  }

                  if (t instanceof IntArrayTag) {
                     IntArrayTag a = (IntArrayTag)t;
                     if (a.size() == 3) {
                        sx = a.get(0).getAsInt();
                        sy = a.get(1).getAsInt();
                        sz = a.get(2).getAsInt();
                     }
                  }
                  break;
               case "DataVersion":
                  version = type == 3 ? in.readInt() : ((NumericTag)readPayload(in, type, 0)).getAsInt();
                  break;
               case "palette":
                  palette = compounds(readPayload(in, type, 0));
                  break;
               case "palettes":
                  Tag t2 = readPayload(in, type, 0);
                  if (t2 instanceof ListTag) {
                     ListTag all = (ListTag)t2;
                     if (!all.isEmpty()) {
                        Tag var19 = all.get(0);
                        if (var19 instanceof ListTag) {
                           ListTag first = (ListTag)var19;
                           palette = compounds(first);
                        }
                     }
                  }
                  break;
               case "blocks":
                  if (type != 9) {
                     skip(in, type, 0);
                  } else {
                     int element = in.readByte();
                     int count = in.readInt();
                     if (element != 10) {
                        for(int i = 0; i < count; ++i) {
                           skip(in, element, 1);
                        }
                     } else {
                        for(int i = 0; i < count; ++i) {
                           readBlock(in, xs, ys, zs, ps, entities);
                        }
                     }
                  }
                  break;
               default:
                  skip(in, type, 0);
            }
         }
      }
   }

   private static void readBlock(DataInputStream in, IntList xs, IntList ys, IntList zs, IntList ps, Map<Long, CompoundTag> entities) throws IOException {
      int x = 0;
      int y = 0;
      int z = 0;
      int state = -1;
      CompoundTag nbt = null;

      while(true) {
         int type = in.readByte();
         if (type == 0) {
            if (state < 0) {
               return;
            }

            xs.add(x);
            ys.add(y);
            zs.add(z);
            ps.add(state);
            if (nbt != null) {
               entities.put(key(x, y, z), nbt);
            }

            return;
         }

         switch (in.readUTF()) {
            case "pos":
               Tag t = readPayload(in, type, 1);
               if (t instanceof ListTag l) {
                  if (l.size() == 3) {
                     x = l.getInt(0);
                     y = l.getInt(1);
                     z = l.getInt(2);
                     break;
                  }
               }

               if (t instanceof IntArrayTag a) {
                  if (a.size() == 3) {
                     x = a.get(0).getAsInt();
                     y = a.get(1).getAsInt();
                     z = a.get(2).getAsInt();
                  }
               }
               break;
            case "state":
               state = type == 3 ? in.readInt() : ((NumericTag)readPayload(in, type, 1)).getAsInt();
               break;
            case "nbt":
               Tag t2 = readPayload(in, type, 1);
               if (t2 instanceof CompoundTag c) {
                  nbt = c;
               }
               break;
            default:
               skip(in, type, 1);
         }
      }
   }

   private static List<CompoundTag> compounds(Tag t) {
      List<CompoundTag> out = new ArrayList();
      if (t instanceof ListTag) {
         for(Tag e : (ListTag)t) {
            if (e instanceof CompoundTag) {
               CompoundTag c = (CompoundTag)e;
               out.add(c);
            }
         }
      }

      return out;
   }

   private static Tag readPayload(DataInputStream in, int type, int depth) throws IOException {
      if (depth > 512) {
         throw new IOException("tags nested too deep");
      } else {
         Object var10000;
         switch (type) {
            case 0:
               var10000 = EndTag.INSTANCE;
               break;
            case 1:
               var10000 = ByteTag.valueOf(in.readByte());
               break;
            case 2:
               var10000 = ShortTag.valueOf(in.readShort());
               break;
            case 3:
               var10000 = IntTag.valueOf(in.readInt());
               break;
            case 4:
               var10000 = LongTag.valueOf(in.readLong());
               break;
            case 5:
               var10000 = FloatTag.valueOf(in.readFloat());
               break;
            case 6:
               var10000 = DoubleTag.valueOf(in.readDouble());
               break;
            case 7:
               byte[] b = new byte[length(in)];
               in.readFully(b);
               var10000 = new ByteArrayTag(b);
               break;
            case 8:
               var10000 = StringTag.valueOf(in.readUTF());
               break;
            case 9:
               int element = in.readByte();
               int count = in.readInt();
               ListTag l = new ListTag();

               for(int i = 0; i < count; ++i) {
                  l.add(readPayload(in, element, depth + 1));
               }

               var10000 = l;
               break;
            case 10:
               CompoundTag c = new CompoundTag();

               while(true) {
                  int t = in.readByte();
                  if (t == 0) {
                     var10000 = c;
                     return (Tag)var10000;
                  }

                  String name = in.readUTF();
                  c.put(name, readPayload(in, t, depth + 1));
               }
            case 11:
               int[] a = new int[length(in)];

               for(int i = 0; i < a.length; ++i) {
                  a[i] = in.readInt();
               }

               var10000 = new IntArrayTag(a);
               break;
            case 12:
               long[] la = new long[length(in)];

               for(int i = 0; i < la.length; ++i) {
                  la[i] = in.readLong();
               }

               var10000 = new LongArrayTag(la);
               break;
            default:
               throw new IOException("unknown tag type " + type);
         }

         return (Tag)var10000;
      }
   }

   private static int length(DataInputStream in) throws IOException {
      int n = in.readInt();
      if (n >= 0 && n <= 536870912) {
         return n;
      } else {
         throw new IOException("bad array length " + n);
      }
   }

   private static void skip(DataInputStream in, int type, int depth) throws IOException {
      if (depth > 512) {
         throw new IOException("tags nested too deep");
      } else {
         switch (type) {
            case 0:
               break;
            case 1:
               in.skipNBytes(1L);
               break;
            case 2:
               in.skipNBytes(2L);
               break;
            case 3:
            case 5:
               in.skipNBytes(4L);
               break;
            case 4:
            case 6:
               in.skipNBytes(8L);
               break;
            case 7:
               in.skipNBytes((long)length(in));
               break;
            case 8:
               in.skipNBytes((long)in.readUnsignedShort());
               break;
            case 9:
               int element = in.readByte();
               int count = in.readInt();

               for(int i = 0; i < count; ++i) {
                  skip(in, element, depth + 1);
               }
               break;
            case 10:
               while(true) {
                  int t = in.readByte();
                  if (t == 0) {
                     return;
                  }

                  in.skipNBytes((long)in.readUnsignedShort());
                  skip(in, t, depth + 1);
               }
            case 11:
               in.skipNBytes(4L * (long)length(in));
               break;
            case 12:
               in.skipNBytes(8L * (long)length(in));
               break;
            default:
               throw new IOException("unknown tag type " + type);
         }

      }
   }

   private static Grid readSponge(CompoundTag file, BuildSpec spec) throws IOException {
      CompoundTag root = file.contains("Schematic", 10) ? file.getCompound("Schematic") : file;
      if (root.contains("Blocks", 7) && !root.contains("Palette")) {
         throw new IOException("an MCEdit schematic of the game before 1.13 (numeric block ids), which scenes do not read: save it again as .schem");
      } else {
         int version = root.getInt("DataVersion");
         final int w = root.getShort("Width") & '\uffff';
         final int h = root.getShort("Height") & '\uffff';
         final int l = root.getShort("Length") & '\uffff';
         CompoundTag blocks = root.contains("Blocks", 10) ? root.getCompound("Blocks") : root;
         CompoundTag paletteTag = blocks.getCompound("Palette");
         final byte[] data = blocks.contains("Data", 7) ? blocks.getByteArray("Data") : root.getByteArray("BlockData");
         int max = 0;

         for(String k : paletteTag.getAllKeys()) {
            max = Math.max(max, paletteTag.getInt(k));
         }

         List<CompoundTag> palette = new ArrayList(max + 1);

         for(int i = 0; i <= max; ++i) {
            palette.add((CompoundTag)null);
         }

         for(String k : paletteTag.getAllKeys()) {
            int i = paletteTag.getInt(k);
            if (i >= 0 && i <= max) {
               palette.set(i, BuildStates.toTag(k));
            }
         }

         final BlockState[] states = resolve(palette, version, spec);
         Map<Long, CompoundTag> entities = new HashMap();
         ListTag list = blocks.contains("BlockEntities", 9) ? blocks.getList("BlockEntities", 10) : (root.contains("BlockEntities", 9) ? root.getList("BlockEntities", 10) : root.getList("TileEntities", 10));

         for(int i = 0; i < list.size(); ++i) {
            CompoundTag e = list.getCompound(i);
            int[] pos = e.getIntArray("Pos");
            if (pos.length == 3) {
               CompoundTag nbt = e.contains("Data", 10) ? e.getCompound("Data").copy() : e.copy();
               nbt.remove("Pos");
               nbt.remove("Id");
               nbt.putString("id", e.getString("Id"));
               entities.put(key(pos[0], pos[1], pos[2]), nbt);
            }
         }

         final Map<Long, CompoundTag> fixedEntities = fixEntities(entities, version);
         return new Grid() {
            public int sizeX() {
               return w;
            }

            public int sizeY() {
               return h;
            }

            public int sizeZ() {
               return l;
            }

            public void blocks(BlockVisitor visit) {
               int i = 0;
               int cell = 0;

               for(int layer = w * l; i < data.length && cell < w * h * l; ++cell) {
                  int value = 0;
                  int shift = 0;

                  byte b;
                  do {
                     b = data[i++];
                     value |= (b & 127) << shift;
                     shift += 7;
                  } while((b & 128) != 0 && i < data.length && shift < 35);

                  if (value < states.length && !states[value].isAir()) {
                     visit.block(cell % w, cell / layer, cell / w % l, value);
                  }
               }

            }

            public BlockState state(int i) {
               return i >= 0 && i < states.length ? states[i] : Blocks.AIR.defaultBlockState();
            }

            public @Nullable CompoundTag entity(int x, int y, int z) {
               return (CompoundTag)fixedEntities.get(BuildFiles.key(x, y, z));
            }
         };
      }
   }

   private static Grid readLitematic(CompoundTag root, BuildSpec spec) {
      int version = root.getInt("MinecraftDataVersion");
      CompoundTag regions = root.getCompound("Regions");

      record Region(int x, int y, int z, int sx, int sy, int sz, BlockState[] states, long[] bits, int width, Map<Long, CompoundTag> entities) {
      }

      final List<Region> list = new ArrayList();
      int minX = Integer.MAX_VALUE;
      int minY = Integer.MAX_VALUE;
      int minZ = Integer.MAX_VALUE;
      int maxX = Integer.MIN_VALUE;
      int maxY = Integer.MIN_VALUE;
      int maxZ = Integer.MIN_VALUE;

      for(String name : regions.getAllKeys()) {
         CompoundTag r = regions.getCompound(name);
         CompoundTag pos = r.getCompound("Position");
         CompoundTag size = r.getCompound("Size");
         int px = pos.getInt("x");
         int py = pos.getInt("y");
         int pz = pos.getInt("z");
         int sx = size.getInt("x");
         int sy = size.getInt("y");
         int sz = size.getInt("z");
         int x0 = sx < 0 ? px + sx + 1 : px;
         int y0 = sy < 0 ? py + sy + 1 : py;
         int z0 = sz < 0 ? pz + sz + 1 : pz;
         int ax = Math.abs(sx);
         int ay = Math.abs(sy);
         int az = Math.abs(sz);
         if (ax != 0 && ay != 0 && az != 0) {
            ListTag p = r.getList("BlockStatePalette", 10);
            List<CompoundTag> palette = new ArrayList(p.size());

            for(int i = 0; i < p.size(); ++i) {
               palette.add(p.getCompound(i));
            }

            BlockState[] states = resolve(palette, version, spec);
            int width = Math.max(2, 32 - Integer.numberOfLeadingZeros(Math.max(1, palette.size() - 1)));
            Map<Long, CompoundTag> entities = new HashMap();
            long[] bits = r.getLongArray("BlockStates");
            ListTag tiles = r.getList("TileEntities", 10);

            for(int i = 0; i < tiles.size(); ++i) {
               CompoundTag t = tiles.getCompound(i).copy();
               int tx = t.getInt("x");
               int ty = t.getInt("y");
               int tz = t.getInt("z");
               if (tx >= 0 && ty >= 0 && tz >= 0 && tx < ax && ty < ay && tz < az) {
                  if (!t.contains("id", 8)) {
                     int value = packed(bits, width, (ty * az + tz) * ax + tx);
                     String id = value > 0 && value < states.length ? entityId(states[value]) : null;
                     if (id == null) {
                        continue;
                     }

                     t.putString("id", id);
                  }

                  entities.put(key(tx, ty, tz), t);
               }
            }

            list.add(new Region(x0, y0, z0, ax, ay, az, states, bits, width, fixEntities(entities, version)));
            minX = Math.min(minX, x0);
            minY = Math.min(minY, y0);
            minZ = Math.min(minZ, z0);
            maxX = Math.max(maxX, x0 + ax - 1);
            maxY = Math.max(maxY, y0 + ay - 1);
            maxZ = Math.max(maxZ, z0 + az - 1);
         }
      }

      final int ox = list.isEmpty() ? 0 : minX;
      final int oy = list.isEmpty() ? 0 : minY;
      final int oz = list.isEmpty() ? 0 : minZ;
      final int w = list.isEmpty() ? 1 : maxX - minX + 1;
      final int h = list.isEmpty() ? 1 : maxY - minY + 1;
      final int d = list.isEmpty() ? 1 : maxZ - minZ + 1;
      final List<BlockState> all = new ArrayList();
      final int[] offsets = new int[list.size()];

      for(int i = 0; i < list.size(); ++i) {
         offsets[i] = all.size();
         all.addAll(List.of(((Region)list.get(i)).states()));
      }

      return new Grid() {
         public int sizeX() {
            return w;
         }

         public int sizeY() {
            return h;
         }

         public int sizeZ() {
            return d;
         }

         public void blocks(BlockVisitor visit) {
            for(int ri = 0; ri < list.size(); ++ri) {
               Region r = (Region)list.get(ri);
               long[] bits = r.bits();
               int cells = r.sx() * r.sy() * r.sz();

               for(int i = 0; i < cells; ++i) {
                  int value = BuildFiles.packed(bits, r.width(), i);
                  if (value < 0) {
                     break;
                  }

                  if (value != 0 && value < r.states().length && !r.states()[value].isAir()) {
                     int x = i % r.sx();
                     int z = i / r.sx() % r.sz();
                     int y = i / (r.sx() * r.sz());
                     visit.block(r.x() + x - ox, r.y() + y - oy, r.z() + z - oz, offsets[ri] + value);
                  }
               }
            }

         }

         public BlockState state(int i) {
            return i >= 0 && i < all.size() ? (BlockState)all.get(i) : Blocks.AIR.defaultBlockState();
         }

         public @Nullable CompoundTag entity(int x, int y, int z) {
            for(Region r : list) {
               int lx = x + ox - r.x();
               int ly = y + oy - r.y();
               int lz = z + oz - r.z();
               if (lx >= 0 && ly >= 0 && lz >= 0 && lx < r.sx() && ly < r.sy() && lz < r.sz()) {
                  CompoundTag t = (CompoundTag)r.entities().get(BuildFiles.key(lx, ly, lz));
                  if (t != null) {
                     return t;
                  }
               }
            }

            return null;
         }
      };
   }

   private static int packed(long[] bits, int width, int i) {
      long start = (long)i * (long)width;
      int a = (int)(start >> 6);
      int b = (int)((long)(i + 1) * (long)width - 1L >> 6);
      if (b >= bits.length) {
         return -1;
      } else {
         long mask = (1L << width) - 1L;
         int shift = (int)(start & 63L);
         return (int)(a == b ? bits[a] >>> shift & mask : (bits[a] >>> shift | bits[b] << 64 - shift) & mask);
      }
   }

   private static @Nullable String entityId(BlockState state) {
      if (!state.hasBlockEntity()) {
         return null;
      } else {
         for(BlockEntityType<?> type : BuiltInRegistries.BLOCK_ENTITY_TYPE) {
            if (type.isValid(state)) {
               ResourceLocation key = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(type);
               return key == null ? null : key.toString();
            }
         }

         return null;
      }
   }

   private static int current() {
      return SharedConstants.getCurrentVersion().getDataVersion().getVersion();
   }

   private static BlockState[] resolve(List<CompoundTag> palette, int version, BuildSpec spec) {
      BlockState[] out = new BlockState[palette.size()];
      boolean fix = version > 0 && version < current();
      DataFixer fixer = fix ? Minecraft.getInstance().getFixerUpper() : null;
      Set<String> missing = new TreeSet();
      Set<String> stood = new TreeSet();

      for(int i = 0; i < out.length; ++i) {
         CompoundTag tag = (CompoundTag)palette.get(i);
         if (tag == null) {
            out[i] = Blocks.AIR.defaultBlockState();
         } else {
            if (fixer != null) {
               try {
                  tag = (CompoundTag)fixer.update(References.BLOCK_STATE, new Dynamic(NbtOps.INSTANCE, tag), version, current()).getValue();
               } catch (RuntimeException var14) {
               }
            }

            BlockState s = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), tag);
            String name = tag.getString("Name");
            ResourceLocation id = ResourceLocation.tryParse(name);
            if (s.isAir() && !name.endsWith("air") && (id == null || !BuiltInRegistries.BLOCK.containsKey(id))) {
               BlockState in = standIn(spec, name, tag);
               if (in != null) {
                  s = in;
                  stood.add(name);
               } else {
                  missing.add(name);
               }
            }

            if (s.is(Blocks.STRUCTURE_BLOCK) || s.is(Blocks.JIGSAW) || s.is(Blocks.STRUCTURE_VOID)) {
               s = Blocks.AIR.defaultBlockState();
            }

            out[i] = s;
         }
      }

      if (!missing.isEmpty() || !stood.isEmpty()) {
         CFHGui.LOGGER.info("[CFHGui] build {}: {} kind(s) of block are not in this game{}{}", new Object[]{spec.baseName(), missing.size() + stood.size(), missing.isEmpty() ? "" : "; left out: " + listed(missing), stood.isEmpty() ? "" : "; stood in for by the scene's own: " + listed(stood)});
      }

      return out;
   }

   private static String listed(Set<String> names) {
      return names.size() > 12 ? String.join(", ", new ArrayList<String>(names).subList(0, 12)) + ", ... (" + names.size() + ")" : String.join(", ", names);
   }

   private static @Nullable BlockState standIn(BuildSpec spec, String name, CompoundTag tag) {
      List<String> candidates = spec.standinsFor(BuildPeek.id(name));
      if (candidates == null) {
         return null;
      } else {
         for(String candidate : candidates) {
            CompoundTag own = BuildStates.toTag(candidate);
            ResourceLocation id = ResourceLocation.tryParse(own.getString("Name"));
            Block block = id == null ? null : BuiltInRegistries.BLOCK.getOptional(id).orElse((Block)null);
            if (block != null && !block.defaultBlockState().isAir()) {
               BlockState s = block.defaultBlockState();
               CompoundTag was = tag.getCompound("Properties");

               for(String key : was.getAllKeys()) {
                  s = with(s, key, was.getString(key));
               }

               CompoundTag says = own.getCompound("Properties");

               for(String key : says.getAllKeys()) {
                  s = with(s, key, says.getString(key));
               }

               return s;
            }
         }

         return null;
      }
   }

   private static BlockState with(BlockState state, String key, String value) {
      for(Property<?> p : state.getProperties()) {
         if (p.getName().equals(key)) {
            return set(state, p, value);
         }
      }

      return state;
   }

   private static <T extends Comparable<T>> BlockState set(BlockState state, Property<T> p, String value) {
      return (BlockState)p.getValue(value).map((v) -> (BlockState)state.setValue(p, v)).orElse(state);
   }

   private static void pruneItems(CompoundTag t) {
      if ("create:belt".equals(t.getString("id")) && t.contains("Inventory", 10)) {
         t.getCompound("Inventory").remove("Items");
      }

      prune(t, 0);
   }

   private static void prune(Tag tag, int depth) {
      if (depth <= 64) {
         if (tag instanceof CompoundTag) {
            CompoundTag c = (CompoundTag)tag;

            for(String key : new ArrayList<String>(c.getAllKeys())) {
               Tag v = c.get(key);
               if (v instanceof CompoundTag) {
                  CompoundTag item = (CompoundTag)v;
                  if (isItem(item)) {
                     if (!knownItem(item)) {
                        c.remove(key);
                     } else {
                        plain(item);
                     }
                     continue;
                  }
               }

               if (v != null) {
                  prune(v, depth + 1);
               }
            }
         } else if (tag instanceof ListTag) {
            ListTag l = (ListTag)tag;

            for(int i = l.size() - 1; i >= 0; --i) {
               Tag v = l.get(i);
               if (v instanceof CompoundTag) {
                  CompoundTag item = (CompoundTag)v;
                  if (isItem(item)) {
                     if (!knownItem(item)) {
                        l.remove(i);
                     } else {
                        plain(item);
                     }
                     continue;
                  }
               }

               prune(v, depth + 1);
            }
         }

      }
   }

   private static boolean isItem(CompoundTag c) {
      return c.contains("id", 8) && (c.contains("count", 99) || c.contains("Count", 99));
   }

   private static boolean knownItem(CompoundTag c) {
      ResourceLocation id = ResourceLocation.tryParse(c.getString("id"));
      return id != null && BuiltInRegistries.ITEM.containsKey(id);
   }

   private static void plain(CompoundTag item) {
      item.remove("components");
   }

   private static Map<Long, CompoundTag> fixEntities(Map<Long, CompoundTag> entities, int version) {
      if (entities.isEmpty()) {
         return entities;
      } else {
         boolean fix = version > 0 && version < current();
         DataFixer fixer = fix ? Minecraft.getInstance().getFixerUpper() : null;
         Map<Long, CompoundTag> out = new HashMap(entities.size() * 2);

         for(Map.Entry<Long, CompoundTag> e : entities.entrySet()) {
            CompoundTag t = (CompoundTag)e.getValue();

            for(String key : UNSEEN) {
               t.remove(key);
            }

            for(String key : UNSEEN_EXTRA) {
               t.remove(key);
            }

            pruneItems(t);
            if (fixer != null) {
               try {
                  t = (CompoundTag)fixer.update(References.BLOCK_ENTITY, new Dynamic(NbtOps.INSTANCE, t), version, current()).getValue();
               } catch (RuntimeException var10) {
               }
            }

            out.put((Long)e.getKey(), t);
         }

         return out;
      }
   }

   private static final class IntList {
      int[] data = new int[1024];
      int size;

      void add(int v) {
         if (this.size == this.data.length) {
            this.data = Arrays.copyOf(this.data, this.data.length * 2);
         }

         this.data[this.size++] = v;
      }
   }

   @FunctionalInterface
   private interface BlockVisitor {
      void block(int var1, int var2, int var3, int var4);
   }

   private interface Grid {
      int sizeX();

      int sizeY();

      int sizeZ();

      void blocks(BlockVisitor var1);

      BlockState state(int var1);

      @Nullable CompoundTag entity(int var1, int var2, int var3);
   }
}
