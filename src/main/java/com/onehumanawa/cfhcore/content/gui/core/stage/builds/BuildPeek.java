package com.onehumanawa.cfhcore.content.gui.core.stage.builds;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.GZIPInputStream;

public final class BuildPeek {
   public static final long MAX_FILE = 100663296L;
   private static final long MAX_UNPACKED = 1073741824L;
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

   private BuildPeek() {
   }

   public static Signature read(Path file) throws IOException {
      String name = file.getFileName().toString();
      int dot = name.lastIndexOf(46);
      String ext = dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
      if (Files.size(file) > 100663296L) {
         throw new IOException("too large to be a scene's build");
      } else {
         InputStream raw = Files.newInputStream(file);

         Signature var5;
         try {
            var5 = read(raw, ext);
         } catch (Throwable var8) {
            if (raw != null) {
               try {
                  raw.close();
               } catch (Throwable var7) {
                  var8.addSuppressed(var7);
               }
            }

            throw var8;
         }

         if (raw != null) {
            raw.close();
         }

         return var5;
      }
   }

   public static Signature read(InputStream raw, String ext) throws IOException {
      DataInputStream in = new DataInputStream(new BufferedInputStream(new Bounded(unpack(raw)), 65536));
      Signature var10000;
      switch (ext) {
         case "nbt" -> var10000 = structure(in);
         case "schem" -> var10000 = sponge(root(in));
         case "litematic" -> var10000 = litematic(root(in));
         default -> throw new IOException("not a build's file: ." + ext);
      }

      return var10000;
   }

   private static InputStream unpack(InputStream raw) throws IOException {
      BufferedInputStream in = new BufferedInputStream(raw, 65536);
      in.mark(2);
      int a = in.read();
      int b = in.read();
      in.reset();
      return (InputStream)(a == 31 && b == 139 ? new GZIPInputStream(in, 65536) : in);
   }

   private static Signature structure(DataInputStream in) throws IOException {
      if (in.readByte() != 10) {
         throw new IOException("not a structure file");
      } else {
         in.readUTF();
         int sx = -1;
         int sy = -1;
         int sz = -1;
         List<String> palette = null;
         Map<Integer, int[]> counts = new HashMap();

         while(true) {
            int type = in.readByte();
            if (type == 0) {
               if (palette != null && sx >= 0) {
                  type = 0;
                  Set<String> kinds = new TreeSet();

                  for(Map.Entry<Integer, int[]> e : counts.entrySet()) {
                     int i = (Integer)e.getKey();
                     if (i < palette.size() && !air((String)palette.get(i))) {
                        type += ((int[])e.getValue())[0];
                        kinds.add((String)palette.get(i));
                     }
                  }

                  return new Signature(sx, sy, sz, type, kinds);
               }

               throw new IOException("a structure file without a size or a palette");
            }

            switch (in.readUTF()) {
               case "size":
                  Object v = payload(in, type, 0);
                  int[] s = ints(v);
                  if (s != null && s.length == 3) {
                     sx = s[0];
                     sy = s[1];
                     sz = s[2];
                  }
                  break;
               case "palette":
                  palette = names(payload(in, type, 0));
                  break;
               case "palettes":
                  Object v2 = payload(in, type, 0);
                  if (v2 instanceof List) {
                     List<?> all = (List)v2;
                     if (!all.isEmpty()) {
                        palette = names(all.get(0));
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
                           int state = blockState(in);
                           if (state >= 0) {
                              int var10002 = ((int[])counts.computeIfAbsent(state, (k) -> new int[1]))[0]++;
                           }
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

   private static int blockState(DataInputStream in) throws IOException {
      int state = -1;

      while(true) {
         int type = in.readByte();
         if (type == 0) {
            return state;
         }

         String name = in.readUTF();
         if (name.equals("state") && type == 3) {
            state = in.readInt();
         } else {
            skip(in, type, 1);
         }
      }
   }

   private static Map<String, Object> root(DataInputStream in) throws IOException {
      if (in.readByte() != 10) {
         throw new IOException("not an NBT file");
      } else {
         in.readUTF();
         Map<String, Object> m = (Map)payload(in, 10, 0);
         return m;
      }
   }

   private static Signature sponge(Map<String, Object> file) throws IOException {
      Map<String, Object> root = map(file.get("Schematic")) != null ? map(file.get("Schematic")) : file;
      int w = num(root.get("Width")) & '\uffff';
      int h = num(root.get("Height")) & '\uffff';
      int l = num(root.get("Length")) & '\uffff';
      Map<String, Object> blocksTag = map(root.get("Blocks"));
      Map<String, Object> paletteTag = map(blocksTag != null ? blocksTag.get("Palette") : root.get("Palette"));
      Object dataTag = blocksTag != null ? blocksTag.get("Data") : root.get("BlockData");
      if (paletteTag != null && dataTag instanceof byte[] data) {
         HashMap palette = new HashMap();

         for(Map.Entry<String, Object> e : paletteTag.entrySet()) {
            palette.put(num(e.getValue()), id((String)e.getKey()));
         }

         Map<Integer, int[]> counts = new HashMap();
         int i = 0;
         int cell = 0;

         for(long cells = (long)w * (long)h * (long)l; i < data.length && (long)cell < cells; ++cell) {
            int value = 0;
            int shift = 0;

            byte b;
            do {
               b = data[i++];
               value |= (b & 127) << shift;
               shift += 7;
            } while((b & 128) != 0 && i < data.length && shift < 35);

            int var10002 = ((int[])counts.computeIfAbsent(value, (k) -> new int[1]))[0]++;
         }

         return signature(w, h, l, counts, palette);
      } else {
         throw new IOException("a schematic without a palette or blocks");
      }
   }

   private static Signature litematic(Map<String, Object> root) throws IOException {
      Map<String, Object> regions = map(root.get("Regions"));
      if (regions == null) {
         throw new IOException("a litematic without regions");
      } else {
         int minX = Integer.MAX_VALUE;
         int minY = Integer.MAX_VALUE;
         int minZ = Integer.MAX_VALUE;
         int maxX = Integer.MIN_VALUE;
         int maxY = Integer.MIN_VALUE;
         int maxZ = Integer.MIN_VALUE;
         int blocks = 0;
         Set<String> kinds = new TreeSet();

         for(Object o : regions.values()) {
            Map<String, Object> r = map(o);
            if (r != null) {
               Map<String, Object> pos = map(r.get("Position"));
               Map<String, Object> size = map(r.get("Size"));
               if (pos != null && size != null) {
                  int px = num(pos.get("x"));
                  int py = num(pos.get("y"));
                  int pz = num(pos.get("z"));
                  int sx = num(size.get("x"));
                  int sy = num(size.get("y"));
                  int sz = num(size.get("z"));
                  int ax = Math.abs(sx);
                  int ay = Math.abs(sy);
                  int az = Math.abs(sz);
                  if (ax != 0 && ay != 0 && az != 0) {
                     int x0 = sx < 0 ? px + sx + 1 : px;
                     int y0 = sy < 0 ? py + sy + 1 : py;
                     int z0 = sz < 0 ? pz + sz + 1 : pz;
                     minX = Math.min(minX, x0);
                     minY = Math.min(minY, y0);
                     minZ = Math.min(minZ, z0);
                     maxX = Math.max(maxX, x0 + ax - 1);
                     maxY = Math.max(maxY, y0 + ay - 1);
                     maxZ = Math.max(maxZ, z0 + az - 1);
                     List<String> palette = names(r.get("BlockStatePalette"));
                     if (palette != null) {
                        Object var29 = r.get("BlockStates");
                        if (var29 instanceof long[]) {
                           long[] bits = (long[])var29;
                           int width = Math.max(2, 32 - Integer.numberOfLeadingZeros(Math.max(1, palette.size() - 1)));
                           long mask = (1L << width) - 1L;
                           long cells = (long)ax * (long)ay * (long)az;
                           int[] counts = new int[palette.size()];

                           for(long i = 0L; i < cells; ++i) {
                              long start = i * (long)width;
                              int a = (int)(start >> 6);
                              int b = (int)((i + 1L) * (long)width - 1L >> 6);
                              if (b >= bits.length) {
                                 break;
                              }

                              int shift = (int)(start & 63L);
                              int v = (int)(a == b ? bits[a] >>> shift & mask : (bits[a] >>> shift | bits[b] << 64 - shift) & mask);
                              if (v < counts.length) {
                                 int var10002 = counts[v]++;
                              }
                           }

                           for(int v = 0; v < counts.length; ++v) {
                              if (counts[v] != 0 && !air((String)palette.get(v))) {
                                 blocks += counts[v];
                                 kinds.add((String)palette.get(v));
                              }
                           }
                        }
                     }
                  }
               }
            }
         }

         if (minX > maxX) {
            throw new IOException("a litematic with nothing in it");
         } else {
            return new Signature(maxX - minX + 1, maxY - minY + 1, maxZ - minZ + 1, blocks, kinds);
         }
      }
   }

   private static Signature signature(int w, int h, int l, Map<Integer, int[]> counts, Map<Integer, String> palette) {
      int blocks = 0;
      Set<String> kinds = new TreeSet();

      for(Map.Entry<Integer, int[]> e : counts.entrySet()) {
         String id = (String)palette.get(e.getKey());
         if (id != null && !air(id)) {
            blocks += ((int[])e.getValue())[0];
            kinds.add(id);
         }
      }

      return new Signature(w, h, l, blocks, kinds);
   }

   static String id(String state) {
      int open = state.indexOf(91);
      String name = (open < 0 ? state : state.substring(0, open)).trim();
      return name.contains(":") ? name : "minecraft:" + name;
   }

   static boolean air(String id) {
      return id.equals("minecraft:air") || id.equals("minecraft:cave_air") || id.equals("minecraft:void_air") || id.equals("minecraft:structure_void");
   }

   private static List<String> names(Object list) {
      if (!(list instanceof List<?> l)) {
         return null;
      } else {
         ArrayList out = new ArrayList(l.size());

         for(Object o : l) {
            String var10001;
            label22: {
               Map<String, Object> m = map(o);
               if (m != null) {
                  Object var7 = m.get("Name");
                  if (var7 instanceof String) {
                     String s = (String)var7;
                     var10001 = id(s);
                     break label22;
                  }
               }

               var10001 = "minecraft:air";
            }

            out.add(var10001);
         }

         return out;
      }
   }

   private static Map<String, Object> map(Object o) {
      Map var10000;
      if (o instanceof Map<?, ?> m) {
         var10000 = m;
      } else {
         var10000 = null;
      }

      return var10000;
   }

   private static int num(Object o) {
      int var10000;
      if (o instanceof Number n) {
         var10000 = n.intValue();
      } else {
         var10000 = 0;
      }

      return var10000;
   }

   private static int[] ints(Object o) {
      if (o instanceof int[] a) {
         return a;
      } else if (!(o instanceof List<?> l)) {
         return null;
      } else {
         int[] out = new int[l.size()];

         for(int i = 0; i < out.length; ++i) {
            out[i] = num(l.get(i));
         }

         return out;
      }
   }

   private static int length(DataInputStream in, int max) throws IOException {
      int n = in.readInt();
      if (n >= 0 && n <= max) {
         return n;
      } else {
         throw new IOException("bad array length " + n);
      }
   }

   private static Object payload(DataInputStream in, int type, int depth) throws IOException {
      if (depth > 256) {
         throw new IOException("tags nested too deep");
      } else {
         Object var10000;
         switch (type) {
            case 0:
               var10000 = null;
               break;
            case 1:
               var10000 = in.readByte();
               break;
            case 2:
               var10000 = in.readShort();
               break;
            case 3:
               var10000 = in.readInt();
               break;
            case 4:
               var10000 = in.readLong();
               break;
            case 5:
               var10000 = in.readFloat();
               break;
            case 6:
               var10000 = in.readDouble();
               break;
            case 7:
               byte[] b = new byte[length(in, 268435456)];
               in.readFully(b);
               var10000 = b;
               break;
            case 8:
               var10000 = in.readUTF();
               break;
            case 9:
               int element = in.readByte();
               int count = length(in, 67108864);
               List<Object> l = new ArrayList(Math.min(count, 4096));

               for(int i = 0; i < count; ++i) {
                  l.add(payload(in, element, depth + 1));
               }

               var10000 = l;
               break;
            case 10:
               Map<String, Object> m = new LinkedHashMap();

               while(true) {
                  int t = in.readByte();
                  if (t == 0) {
                     var10000 = m;
                     return var10000;
                  }

                  String name = in.readUTF();
                  m.put(name, payload(in, t, depth + 1));
               }
            case 11:
               int[] a = new int[length(in, 67108864)];

               for(int i = 0; i < a.length; ++i) {
                  a[i] = in.readInt();
               }

               var10000 = a;
               break;
            case 12:
               long[] la = new long[length(in, 33554432)];

               for(int i = 0; i < la.length; ++i) {
                  la[i] = in.readLong();
               }

               var10000 = la;
               break;
            default:
               throw new IOException("unknown tag type " + type);
         }

         return var10000;
      }
   }

   private static void skip(DataInputStream in, int type, int depth) throws IOException {
      if (depth > 256) {
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
               in.skipNBytes((long)length(in, Integer.MAX_VALUE));
               break;
            case 8:
               in.skipNBytes((long)in.readUnsignedShort());
               break;
            case 9:
               int element = in.readByte();
               int count = length(in, Integer.MAX_VALUE);

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
               in.skipNBytes(4L * (long)length(in, 536870911));
               break;
            case 12:
               in.skipNBytes(8L * (long)length(in, 268435455));
               break;
            default:
               throw new IOException("unknown tag type " + type);
         }

      }
   }

   public static void main(String[] args) throws IOException {
      for(String a : args) {
         Signature s = read(Path.of(a));
         System.out.println(a + ": size [" + s.sizeX() + ", " + s.sizeY() + ", " + s.sizeZ() + "], " + s.blocks() + " blocks, " + s.kinds().size() + " kinds");
         System.out.println("  " + String.valueOf(s.kinds()));
      }

   }

   public static record Signature(int sizeX, int sizeY, int sizeZ, int blocks, Set<String> kinds) {
      public String toString() {
         int var10000 = this.sizeX;
         return var10000 + "x" + this.sizeY + "x" + this.sizeZ + ", " + this.blocks + " blocks, " + this.kinds.size() + " kinds";
      }
   }

   private static final class Bounded extends FilterInputStream {
      private long read;

      Bounded(InputStream in) {
         super(in);
      }

      private void count(long n) throws IOException {
         if (n > 0L && (this.read += n) > 1073741824L) {
            throw new IOException("unpacks too large to be a build");
         }
      }

      public int read() throws IOException {
         int b = super.read();
         if (b >= 0) {
            this.count(1L);
         }

         return b;
      }

      public int read(byte[] b, int off, int len) throws IOException {
         int n = super.read(b, off, len);
         this.count((long)n);
         return n;
      }

      public long skip(long n) throws IOException {
         long s = super.skip(n);
         this.count(s);
         return s;
      }
   }
}
