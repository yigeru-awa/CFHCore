package com.onehumanawa.cfhcore.content.gui.core.media;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class GifEncoder {
   private static final int COLOURS = 255;
   private static final int SAME = 255;
   private static final int BINS = 65536;
   private static final int TOLERANCE = 5;

   public static byte[] encode(List<int[]> frames, int[] delays, int w, int h) {
      ByteArrayOutputStream out = new ByteArrayOutputStream(frames.size() * w * h / 3 + 1024);
      ascii(out, "GIF89a");
      word(out, w);
      word(out, h);
      out.write(112);
      out.write(0);
      out.write(0);
      out.write(33);
      out.write(255);
      out.write(11);
      ascii(out, "NETSCAPE2.0");
      out.write(3);
      out.write(1);
      word(out, 0);
      out.write(0);
      List<Part> parts = new ArrayList();
      int[] shown = new int[w * h];

      for(int f = 0; f < frames.size(); ++f) {
         int[] frame = (int[])frames.get(f);
         int delay = Math.max(20, f < delays.length ? delays[f] : 80);
         Part part = f == 0 ? whole(frame, w, h) : changed(frame, shown, w, h);
         if (part == null) {
            if (!parts.isEmpty()) {
               Part var10000 = (Part)parts.get(parts.size() - 1);
               var10000.delay += delay;
            }
         } else {
            part.delay = delay;
            quantize(part, frame, shown, w);
            parts.add(part);
         }
      }

      for(Part part : parts) {
         write(out, part);
      }

      out.write(59);
      return out.toByteArray();
   }

   public static int[] scale(int[] src, int w, int h, int nw, int nh) {
      if (nw == w && nh == h) {
         return src;
      } else {
         int[] out = new int[nw * nh];

         for(int y = 0; y < nh; ++y) {
            int y0 = y * h / nh;
            int y1 = Math.max(y0 + 1, (y + 1) * h / nh);

            for(int x = 0; x < nw; ++x) {
               int x0 = x * w / nw;
               int x1 = Math.max(x0 + 1, (x + 1) * w / nw);
               long r = 0L;
               long g = 0L;
               long b = 0L;

               for(int sy = y0; sy < y1; ++sy) {
                  for(int sx = x0; sx < x1; ++sx) {
                     int c = src[sy * w + sx];
                     r += (long)(c >>> 16 & 255);
                     g += (long)(c >>> 8 & 255);
                     b += (long)(c & 255);
                  }
               }

               int n = (y1 - y0) * (x1 - x0);
               out[y * nw + x] = -16777216 | (int)(r / (long)n) << 16 | (int)(g / (long)n) << 8 | (int)(b / (long)n);
            }
         }

         return out;
      }
   }

   private static Part whole(int[] frame, int w, int h) {
      Part p = new Part();
      p.w = w;
      p.h = h;
      p.same = new boolean[w * h];
      return p;
   }

   private static Part changed(int[] frame, int[] shown, int w, int h) {
      int x0 = w;
      int y0 = h;
      int x1 = -1;
      int y1 = -1;

      for(int y = 0; y < h; ++y) {
         for(int x = 0; x < w; ++x) {
            if (!near(frame[y * w + x], shown[y * w + x])) {
               if (x < x0) {
                  x0 = x;
               }

               if (x > x1) {
                  x1 = x;
               }

               if (y < y0) {
                  y0 = y;
               }

               if (y > y1) {
                  y1 = y;
               }
            }
         }
      }

      if (x1 < 0) {
         return null;
      } else {
         Part p = new Part();
         p.x = x0;
         p.y = y0;
         p.w = x1 - x0 + 1;
         p.h = y1 - y0 + 1;
         p.same = new boolean[p.w * p.h];

         for(int y = 0; y < p.h; ++y) {
            for(int x = 0; x < p.w; ++x) {
               int i = (y0 + y) * w + x0 + x;
               if (near(frame[i], shown[i])) {
                  p.same[y * p.w + x] = true;
                  p.sameUsed = true;
               }
            }
         }

         return p;
      }
   }

   private static boolean near(int a, int b) {
      return Math.abs((a >>> 16 & 255) - (b >>> 16 & 255)) <= 5 && Math.abs((a >>> 8 & 255) - (b >>> 8 & 255)) <= 5 && Math.abs((a & 255) - (b & 255)) <= 5;
   }

   private static int bin(int argb) {
      return (argb >>> 19 & 31) << 11 | (argb >>> 10 & 63) << 5 | argb >>> 3 & 31;
   }

   private static void quantize(Part p, int[] frame, int[] shown, int w) {
      int[] count = new int[65536];
      int colours = 0;

      for(int y = 0; y < p.h; ++y) {
         for(int x = 0; x < p.w; ++x) {
            if (!p.same[y * p.w + x] && count[bin(frame[(p.y + y) * w + p.x + x])]++ == 0) {
               ++colours;
            }
         }
      }

      int[] list = new int[colours];
      int n = 0;

      for(int i = 0; i < 65536; ++i) {
         if (count[i] > 0) {
            list[n++] = i;
         }
      }

      List<Box> boxes = new ArrayList();
      if (colours > 0) {
         boxes.add(span(list, count, 0, colours));
      }

      while(boxes.size() < 255) {
         Box widest = null;
         long most = 0L;

         for(Box b : boxes) {
            if (b.to - b.from >= 2) {
               long score = b.count * (long)(b.longest() + 1);
               if (score > most) {
                  most = score;
                  widest = b;
               }
            }
         }

         if (widest == null) {
            break;
         }

         Box box = widest;
         int axis = widest.r1 - widest.r0 >= (widest.g1 - widest.g0) / 2 && widest.r1 - widest.r0 >= widest.b1 - widest.b0 ? 0 : ((widest.g1 - widest.g0) / 2 >= widest.b1 - widest.b0 ? 1 : 2);
         sort(list, widest.from, widest.to, axis);
         long half = 0L;

         int cut;
         for(cut = widest.from; cut < box.to - 1 && half + (long)count[list[cut]] <= box.count / 2L; half += (long)count[list[cut++]]) {
         }

         if (cut == box.from) {
            ++cut;
         }

         boxes.remove(box);
         boxes.add(span(list, count, box.from, cut));
         boxes.add(span(list, count, cut, box.to));
      }

      p.palette = new byte[768];
      int[] placeOf = new int[65536];
      int[] colourOf = new int[256];

      for(int i = 0; i < boxes.size(); ++i) {
         Box b = (Box)boxes.get(i);
         long r = 0L;
         long g = 0L;
         long bl = 0L;

         for(int k = b.from; k < b.to; ++k) {
            int c = list[k];
            int weight = count[c];
            int r5 = c >>> 11 & 31;
            int g6 = c >>> 5 & 63;
            int b5 = c & 31;
            r += (long)(r5 << 3 | r5 >>> 2) * (long)weight;
            g += (long)(g6 << 2 | g6 >>> 4) * (long)weight;
            bl += (long)(b5 << 3 | b5 >>> 2) * (long)weight;
            placeOf[c] = i;
         }

         int cr = (int)(r / b.count);
         int cg = (int)(g / b.count);
         int cb = (int)(bl / b.count);
         p.palette[i * 3] = (byte)cr;
         p.palette[i * 3 + 1] = (byte)cg;
         p.palette[i * 3 + 2] = (byte)cb;
         colourOf[i] = -16777216 | cr << 16 | cg << 8 | cb;
      }

      p.pixels = new byte[p.w * p.h];

      for(int y = 0; y < p.h; ++y) {
         for(int x = 0; x < p.w; ++x) {
            int i = y * p.w + x;
            int at = (p.y + y) * w + p.x + x;
            if (p.same[i]) {
               p.pixels[i] = -1;
            } else {
               int place = placeOf[bin(frame[at])];
               p.pixels[i] = (byte)place;
               shown[at] = colourOf[place];
            }
         }
      }

   }

   private static Box span(int[] list, int[] count, int from, int to) {
      Box b = new Box();
      b.from = from;
      b.to = to;
      b.r0 = b.g0 = b.b0 = Integer.MAX_VALUE;
      b.r1 = b.g1 = b.b1 = -1;

      for(int k = from; k < to; ++k) {
         int c = list[k];
         int r = c >>> 11 & 31;
         int g = c >>> 5 & 63;
         int bl = c & 31;
         b.r0 = Math.min(b.r0, r);
         b.r1 = Math.max(b.r1, r);
         b.g0 = Math.min(b.g0, g);
         b.g1 = Math.max(b.g1, g);
         b.b0 = Math.min(b.b0, bl);
         b.b1 = Math.max(b.b1, bl);
         b.count += (long)count[c];
      }

      return b;
   }

   private static void sort(int[] list, int from, int to, int axis) {
      int shift = axis == 0 ? 11 : (axis == 1 ? 5 : 0);
      int mask = axis == 1 ? 63 : 31;
      long[] keyed = new long[to - from];

      for(int k = from; k < to; ++k) {
         keyed[k - from] = (long)(list[k] >>> shift & mask) << 32 | (long)list[k];
      }

      Arrays.sort(keyed);

      for(int k = from; k < to; ++k) {
         list[k] = (int)keyed[k - from];
      }

   }

   private static void write(ByteArrayOutputStream out, Part p) {
      out.write(33);
      out.write(249);
      out.write(4);
      out.write(4 | (p.sameUsed ? 1 : 0));
      word(out, Math.max(2, Math.round((float)p.delay / 10.0F)));
      out.write(p.sameUsed ? 255 : 0);
      out.write(0);
      out.write(44);
      word(out, p.x);
      word(out, p.y);
      word(out, p.w);
      word(out, p.h);
      out.write(135);
      out.write(p.palette, 0, p.palette.length);
      pack(out, p.pixels);
   }

   private static void pack(ByteArrayOutputStream out, byte[] pixels) {
      int table = 5003;
      int clear = 256;
      int end = 257;
      int[] key = new int[5003];
      int[] code = new int[5003];
      Arrays.fill(key, -1);
      out.write(8);
      Bits bits = new Bits(out);
      int width = 9;
      int next = 258;
      int limit = 1 << width;
      bits.put(256, width);
      if (pixels.length == 0) {
         bits.put(257, width);
         bits.close();
      } else {
         int run = pixels[0] & 255;

         label45:
         for(int i = 1; i < pixels.length; ++i) {
            int c = pixels[i] & 255;
            int k = c << 12 | run;
            int at = (c << 4 ^ run) % 5003;

            while(key[at] >= 0) {
               if (key[at] == k) {
                  run = code[at];
                  continue label45;
               }

               ++at;
               if (at == 5003) {
                  at = 0;
               }
            }

            bits.put(run, width);
            run = c;
            if (next < 4096) {
               key[at] = k;
               code[at] = next;
               if (next == limit && width < 12) {
                  ++width;
                  limit = 1 << width;
               }

               ++next;
            } else {
               bits.put(256, width);
               Arrays.fill(key, -1);
               width = 9;
               limit = 1 << width;
               next = 258;
            }
         }

         bits.put(run, width);
         if (next == limit && width < 12) {
            ++width;
         }

         bits.put(257, width);
         bits.close();
      }
   }

   private static void word(ByteArrayOutputStream out, int v) {
      out.write(v & 255);
      out.write(v >>> 8 & 255);
   }

   private static void ascii(ByteArrayOutputStream out, String s) {
      for(int i = 0; i < s.length(); ++i) {
         out.write(s.charAt(i));
      }

   }

   private GifEncoder() {
   }

   private static final class Part {
      int x;
      int y;
      int w;
      int h;
      int delay;
      boolean sameUsed;
      byte[] palette;
      byte[] pixels;
      boolean[] same;
   }

   private static final class Box {
      int from;
      int to;
      int r0;
      int r1;
      int g0;
      int g1;
      int b0;
      int b1;
      long count;

      int longest() {
         return Math.max(Math.max(this.r1 - this.r0, (this.g1 - this.g0) / 2), this.b1 - this.b0);
      }
   }

   private static final class Bits {
      private final ByteArrayOutputStream out;
      private final byte[] block = new byte[255];
      private int length;
      private int held;
      private int bits;

      Bits(ByteArrayOutputStream out) {
         this.out = out;
      }

      void put(int code, int width) {
         this.held |= code << this.bits;

         for(this.bits += width; this.bits >= 8; this.bits -= 8) {
            this.add(this.held & 255);
            this.held >>>= 8;
         }

      }

      private void add(int b) {
         this.block[this.length++] = (byte)b;
         if (this.length == 255) {
            this.flush();
         }

      }

      private void flush() {
         if (this.length != 0) {
            this.out.write(this.length);
            this.out.write(this.block, 0, this.length);
            this.length = 0;
         }
      }

      void close() {
         if (this.bits > 0) {
            this.add(this.held & 255);
         }

         this.flush();
         this.out.write(0);
      }
   }
}
