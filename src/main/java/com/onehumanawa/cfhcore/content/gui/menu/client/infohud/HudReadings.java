package com.onehumanawa.cfhcore.content.gui.menu.client.infohud;

import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

final class HudReadings {
   static final int TEXT = 0;
   static final int MUTED = 1;
   static final int DIM = 2;
   static final int GOOD = 3;
   static final int WARN = 4;
   static final int BAD = 5;
   static final Seg POSITION = new Seg();
   static final Seg CHUNK = new Seg();
   static final Seg WITHIN = new Seg();
   static final Seg FACING = new Seg();
   static final Seg FACING_SHORT = new Seg();
   static final Seg FACING_AXIS = new Seg();
   static final Seg BIOME = new Seg();
   static final Seg DIMENSION = new Seg();
   static final Seg LIGHT = new Seg();
   static final Seg SKY = new Seg();
   static final Seg DAY = new Seg();
   static final Seg CLOCK = new Seg();
   static final Seg SPEED = new Seg();
   static boolean overworld = true;
   static boolean hasSky = true;
   static boolean reduced;
   static boolean speedShown;
   static boolean night;
   static final Seg FPS = new Seg();
   static final Seg NET = new Seg();
   static final Seg FRAME = new Seg();
   static final Seg LOW = new Seg();
   static final Seg FRAME_MS = new Seg();
   static final Seg RAM = new Seg();
   static final Seg RATE = new Seg();
   static final Seg VRAM = new Seg();
   static int fpsInk = 0;
   static int netInk = 1;
   static int netQuality = 1;
   static boolean local;
   static boolean vramKnown;
   static float ramUsed;
   static float ramAllocated;
   static float ramPressure;
   static float vramUsed;
   static final Seg L_CHUNK = new Seg();
   static final Seg L_WITHIN = new Seg();
   static final Seg L_LIGHT = new Seg();
   static final Seg L_SKY = new Seg();
   static final Seg L_DAY = new Seg();
   static final Seg L_SPEED = new Seg();
   static final Seg L_FPS = new Seg();
   static final Seg L_MS = new Seg();
   static final Seg L_PING = new Seg();
   static final Seg L_SERVER = new Seg();
   static final Seg L_RAM = new Seg();
   static final Seg L_VRAM = new Seg();
   private static int bx = Integer.MIN_VALUE;
   private static int by;
   private static int bz;
   private static boolean chunkWords;
   private static @Nullable Direction facing;
   private static @Nullable ResourceKey<Biome> biome;
   private static @Nullable ResourceKey<Level> dimension;
   private static int blockLight = -1;
   private static int skyLight = -1;
   private static int minute = -1;
   private static int fps = -2;
   private static int low = -2;
   private static int frameTenths = -2;
   private static int net = Integer.MIN_VALUE;
   private static long day = Long.MIN_VALUE;
   private static int ramTenths = -1;
   private static int ramMaxTenths = -1;
   private static int rateMb = -2;
   private static int vramTenths = -1;
   private static int vramMaxTenths = -1;
   private static int speedTenths = -1;
   private static @Nullable String language;
   private static double lastX;
   private static double lastY;
   private static double lastZ;
   private static double speed;
   private static boolean lastKnown;
   private static int slowTicks;

   private HudReadings() {
   }

   static void forget() {
      bx = Integer.MIN_VALUE;
      facing = null;
      biome = null;
      dimension = null;
      minute = -1;
      skyLight = -1;
      blockLight = -1;
      rateMb = -2;
      frameTenths = -2;
      low = -2;
      fps = -2;
      net = Integer.MIN_VALUE;
      day = Long.MIN_VALUE;
      speedTenths = -1;
      vramMaxTenths = -1;
      vramTenths = -1;
      ramMaxTenths = -1;
      ramTenths = -1;
      lastKnown = false;
      speed = (double)0.0F;
      speedShown = false;
   }

   private static void labels(Minecraft mc, Font font) {
      String selected = mc.getLanguageManager().getSelected();
      if (!selected.equals(language)) {
         language = selected;
         forget();
         L_CHUNK.set(I18n.get("cfhgui_infohud.label.chunk", new Object[0]), font);
         L_WITHIN.set(I18n.get("cfhgui_infohud.label.within", new Object[0]), font);
         L_LIGHT.set(I18n.get("cfhgui_infohud.label.light", new Object[0]), font);
         L_SKY.set(I18n.get("cfhgui_infohud.label.sky", new Object[0]), font);
         L_DAY.set(I18n.get("cfhgui_infohud.label.day", new Object[0]), font);
         L_SPEED.set(I18n.get("cfhgui_infohud.label.speed", new Object[0]), font);
         L_FPS.set(I18n.get("cfhgui_infohud.label.fps", new Object[0]), font);
         L_MS.set(I18n.get("cfhgui_infohud.label.ms", new Object[0]), font);
         L_PING.set(I18n.get("cfhgui_infohud.label.ping", new Object[0]), font);
         L_SERVER.set(I18n.get("cfhgui_infohud.label.server", new Object[0]), font);
         L_RAM.set(I18n.get("cfhgui_infohud.label.memory", new Object[0]), font);
         L_VRAM.set(I18n.get("cfhgui_infohud.label.video_memory", new Object[0]), font);
      }
   }

   static void place(Minecraft mc, Font font, InfoHudConfig cfg) {
      labels(mc, font);
      reduced = mc.showOnlyReducedInfo();
      Entity camera = (Entity)(mc.getCameraEntity() != null ? mc.getCameraEntity() : mc.player);
      if (camera != null) {
         BlockPos pos = camera.blockPosition();
         int x = pos.getX();
         int y = pos.getY();
         int z = pos.getZ();
         if (x != bx || y != by || z != bz || cfg.chunk != chunkWords) {
            bx = x;
            by = y;
            bz = z;
            chunkWords = cfg.chunk;
            String sx = Integer.toString(x);
            String sy = Integer.toString(y);
            String sz = Integer.toString(z);
            String gap = sx.length() + sy.length() + sz.length() > 13 ? " " : "  ";
            String words = sx + gap + sy + gap + sz;
            POSITION.set(words, font);
            if (cfg.chunk) {
               Seg var10000 = CHUNK;
               int var10001 = SectionPos.blockToSectionCoord(x);
               var10000.set(var10001 + "  " + SectionPos.blockToSectionCoord(y) + "  " + SectionPos.blockToSectionCoord(z), font);
               WITHIN.set((x & 15) + "  " + (y & 15) + "  " + (z & 15), font);
            }
         }

         Direction d = camera.getDirection();
         if (d != facing) {
            facing = d;
            String name = d.getName();
            FACING.set(I18n.get("cfhgui_infohud.facing." + name, new Object[0]), font);
            FACING_SHORT.set(I18n.get("cfhgui_infohud.facing.short." + name, new Object[0]), font);
            Seg var16 = FACING_AXIS;
            String var17 = d.getAxisDirection() == AxisDirection.POSITIVE ? "+" : "-";
            var16.set(var17 + d.getAxis().getName().toUpperCase(Locale.ROOT), font);
         }

         if (cfg.speed) {
            int tenths = (int)Math.round(speed * (double)10.0F);
            if (tenths != speedTenths) {
               speedTenths = tenths;
               SPEED.set(I18n.get("cfhgui_infohud.speed", new Object[]{tenths(tenths)}), font);
            }
         }

      }
   }

   static void tick(Minecraft mc, InfoHudConfig cfg) {
      if (cfg.speed && mc.player != null && !mc.isPaused()) {
         Vec3 at = mc.player.position();
         if (lastKnown) {
            double dx = at.x - lastX;
            double dy = at.y - lastY;
            double dz = at.z - lastZ;
            boolean airborne = mc.player.isFallFlying() || !mc.player.onGround() && mc.player.getVehicle() == null;
            double now = Math.sqrt(dx * dx + dz * dz + (airborne ? dy * dy : (double)0.0F)) * (double)20.0F;
            if (now < (double)400.0F) {
               speed += (now - speed) * (double)0.25F;
            }
         }

         lastX = at.x;
         lastY = at.y;
         lastZ = at.z;
         lastKnown = true;
         if (speed >= (double)6.5F) {
            speedShown = true;
            slowTicks = 0;
         } else if (speedShown && speed < 5.8 && ++slowTicks > 30) {
            speedShown = false;
         }

      } else {
         lastKnown = false;
      }
   }

   static void world(Minecraft mc, Font font, InfoHudConfig cfg) {
      ClientLevel level = mc.level;
      Entity camera = (Entity)(mc.getCameraEntity() != null ? mc.getCameraEntity() : mc.player);
      if (level != null && camera != null) {
         BlockPos pos = camera.blockPosition();
         ResourceKey<Level> dim = level.dimension();
         if (dim != dimension) {
            dimension = dim;
            overworld = dim == Level.OVERWORLD;
            hasSky = level.dimensionType().hasSkyLight();
            DIMENSION.set(overworld ? "" : named("cfhgui_infohud.dimension.", dim.location()), font);
         }

         if (cfg.biome && !reduced) {
            Holder<Biome> holder = level.getBiome(pos);
            ResourceKey<Biome> key = (ResourceKey)holder.unwrapKey().orElse(null);
            if (key != biome) {
               biome = key;
               BIOME.set(key == null ? I18n.get("cfhgui_infohud.biome.unknown", new Object[0]) : named("biome.", key.location()), font);
            }
         }

         if (cfg.light && !reduced) {
            int block = level.getBrightness(LightLayer.BLOCK, pos);
            if (block != blockLight) {
               blockLight = block;
               LIGHT.set(Integer.toString(block), font);
            }

            int sky = hasSky ? level.getBrightness(LightLayer.SKY, pos) : -1;
            if (sky != skyLight) {
               skyLight = sky;
               SKY.set(sky < 0 ? "" : Integer.toString(sky), font);
            }
         }

         if (cfg.time) {
            long time = level.getDayTime();
            long d = Math.floorDiv(time, 24000L);
            if (d != day) {
               day = d;
               DAY.set(Long.toString(d), font);
            }

            int ofDay = (int)Math.floorMod(time, 24000L);
            night = ofDay >= 12542 && ofDay < 23460;
            int m = (ofDay / 1000 + 6) % 24 * 60 + ofDay % 1000 * 60 / 1000;
            if (m != minute) {
               minute = m;
               CLOCK.set(two(m / 60) + ":" + two(m % 60), font);
            }
         }

      }
   }

   private static String named(String prefix, ResourceLocation id) {
      String key = prefix + id.getNamespace() + "." + id.getPath().replace('/', '.');
      if (I18n.exists(key)) {
         return I18n.get(key, new Object[0]);
      } else {
         String path = id.getPath();
         StringBuilder sb = new StringBuilder();
         boolean up = true;

         for(int i = path.lastIndexOf(47) + 1; i < path.length(); ++i) {
            char c = path.charAt(i);
            if (c == '_') {
               sb.append(' ');
               up = true;
            } else {
               sb.append(up ? Character.toUpperCase(c) : c);
               up = false;
            }
         }

         return sb.toString();
      }
   }

   private static String two(int n) {
      return n < 10 ? "0" + n : Integer.toString(n);
   }

   private static String tenths(int tenths) {
      return tenths / 10 + "." + Math.abs(tenths % 10);
   }

   static void game(Minecraft mc, Font font, InfoHudConfig cfg, long nanos, long millis, boolean frameRateDue) {
      if (cfg.fps && frameRateDue) {
         FrameTimes.refresh(nanos);
         int now = FrameTimes.fps();
         if (now != fps) {
            fps = now;
            FPS.set(now < 0 ? "--" : Integer.toString(now), font);
            fpsInk = now < 0 ? 1 : (now < 20 ? 5 : (now < 30 ? 4 : 0));
         }

         frame(font);
      }

      if (cfg.memory) {
         MemoryProbe.refresh(millis);
         long max = MemoryProbe.max();
         ramUsed = max > 0L ? (float)((double)MemoryProbe.used() / (double)max) : 0.0F;
         ramAllocated = max > 0L ? (float)((double)MemoryProbe.allocated() / (double)max) : 0.0F;
         ramPressure = MemoryProbe.pressure();
         if (InfoHudHarness.pretendFill >= 0.0F) {
            ramUsed = ramPressure = InfoHudHarness.pretendFill;
            ramAllocated = Math.min(1.0F, ramUsed + 0.08F);
         }

         int used = gigaTenths(MemoryProbe.used());
         int most = gigaTenths(max);
         if (used != ramTenths || most != ramMaxTenths) {
            ramTenths = used;
            ramMaxTenths = most;
            RAM.set(amount(MemoryProbe.used(), max), font);
         }

         long rate = MemoryProbe.rate();
         int mb = rate < 0L ? -1 : (int)Math.min(99999L, rate / 1048576L);
         if (mb != rateMb) {
            rateMb = mb;
            RATE.set(mb < 0 ? "" : I18n.get("cfhgui_infohud.memory.rate", new Object[]{Integer.toString(mb)}), font);
         }
      }

   }

   private static void frame(Font font) {
      int l = FrameTimes.low();
      float ms = FrameTimes.frameMs();
      int t = ms < 0.0F ? -1 : Math.round(ms * 10.0F);
      if (l != low || t != frameTenths) {
         low = l;
         frameTenths = t;
         if (t < 0) {
            FRAME_MS.clear();
         } else {
            FRAME_MS.set(I18n.get("cfhgui_infohud.frame", new Object[]{tenths(t)}), font);
         }

         if (l < 0) {
            LOW.clear();
         } else {
            LOW.set(I18n.get("cfhgui_infohud.low", new Object[]{Integer.toString(l)}), font);
         }

         if (t < 0) {
            FRAME.clear();
         } else if (l < 0) {
            FRAME.set(FRAME_MS.s, font);
         } else {
            FRAME.set(I18n.get("cfhgui_infohud.frame.low", new Object[]{Integer.toString(l), tenths(t)}), font);
         }

      }
   }

   static void slow(Minecraft mc, Font font, InfoHudConfig cfg, long nanos, long millis) {
      if (cfg.fps) {
         FrameTimes.refreshLow(nanos);
         frame(font);
      }

      local = mc.getSingleplayerServer() != null && InfoHudHarness.pretendPing < 0;
      if (cfg.latency) {
         if (local) {
            float ms = LatencyProbe.tickMs();
            float budget = LatencyProbe.tickBudgetMs();
            int t = Math.round(ms * 10.0F);
            int key = -1000 - t;
            if (key != net) {
               net = key;
               NET.set(ms >= 10.0F ? Integer.toString(Math.round(ms)) : tenths(t), font);
            }

            netQuality = ms < budget * 0.8F ? 3 : (ms < budget ? 4 : 5);
         } else {
            int ms = InfoHudHarness.pretendPing >= 0 ? InfoHudHarness.pretendPing : LatencyProbe.latencyMs();
            if (ms != net) {
               net = ms;
               NET.set(ms < 0 ? "--" : Integer.toString(ms), font);
            }

            netQuality = ms < 0 ? 1 : (ms < 100 ? 3 : (ms < 250 ? 4 : 5));
         }

         netInk = netQuality == 3 ? 0 : netQuality;
      }

      if (cfg.videoMemory) {
         VideoMemory.refresh(millis);
         vramKnown = VideoMemory.known();
         if (vramKnown) {
            long total = VideoMemory.totalBytes();
            long used = VideoMemory.usedBytes();
            vramUsed = total > 0L ? (float)((double)used / (double)total) : 0.0F;
            if (InfoHudHarness.pretendFill >= 0.0F) {
               vramUsed = Math.min(1.0F, InfoHudHarness.pretendFill + 0.06F);
            }

            int u = gigaTenths(used);
            int most = gigaTenths(total);
            if (u != vramTenths || most != vramMaxTenths) {
               vramTenths = u;
               vramMaxTenths = most;
               VRAM.set(amount(used, total), font);
            }
         }
      } else {
         vramKnown = false;
      }

   }

   private static int gigaTenths(long bytes) {
      return (int)Math.round((double)bytes / (double)1.0737418E9F * (double)10.0F);
   }

   private static String amount(long used, long whole) {
      return whole < 1073741824L ? I18n.get("cfhgui_infohud.amount.mb", new Object[]{Long.toString(used / 1048576L), Long.toString(whole / 1048576L)}) : I18n.get("cfhgui_infohud.amount.gb", new Object[]{tenths(gigaTenths(used)), tenths(gigaTenths(whole))});
   }
}
