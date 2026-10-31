package com.onehumanawa.cfhcore.content.gui.menu.client.loading.journey;

import com.onehumanawa.cfhcore.content.gui.core.client.DevHarness;
import com.onehumanawa.cfhcore.content.gui.core.stage.Stage;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageFinish;
import com.onehumanawa.cfhcore.content.gui.core.stage.anim.IdleMotion;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.FxNode;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.HazeNode;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.MotesNode;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.menu.CFHGuiMenu;
import com.onehumanawa.cfhcore.content.gui.menu.client.overhaul.create.Aerial;
import com.onehumanawa.cfhcore.content.gui.menu.client.overhaul.create.LandSampler;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import org.jetbrains.annotations.Nullable;

final class LandScene {
   private static final float CELL = 0.1F;
   private static final Aerial.Margins MARGINS = new Aerial.Margins(0.9F, 0.8F, 1.2F);
   private static final float LEAVE_MS = 1700.0F;
   private static final float TURN_MS = 700.0F;
   private final Stage stage = new Stage();
   private final boolean leaving;
   private final HazeNode sky;
   private final FxNode ground;
   private final FxNode clouds;
   private final FxNode stars;
   private final FxNode beam;
   private final FxNode pool;
   private final MotesNode motes;
   private final StageFinish finish = StageFinish.cinematic().vignette(0.3F);
   private @Nullable Land land;
   private @Nullable LandNode node;
   private @Nullable CompletableFuture<LandSnapshot.Kept> kept;
   private @Nullable String dimension;
   private @Nullable String name;
   private Hour was;
   private Hour hour;
   private boolean asked;
   private boolean nothing;
   private boolean guessed;
   private int spotX = 80;
   private int spotZ = 64;
   private boolean spotKnown;
   private static final int RIM = 28;
   private float reach;
   private float since;
   private float spot;
   private float turnedAt;
   private float leftAt = -1.0F;
   private int tries;
   private boolean looking = true;
   private static final float VEILED_AIR = 0.45F;
   private static final int VEILED_MOTES = -1;
   private static final float VEILED_MOTES_SIZE = 1.7F;
   private static final float VEILED_MOTES_STEADY = 0.5F;
   private boolean veiled;
   private int moteColor;

   LandScene(@Nullable Land left) {
      this.leaving = left != null;
      this.hour = this.was = left != null ? LandScene.Hour.of(left.dayTime) : LandScene.Hour.DUSK;
      this.stage.soft(true);
      this.stage.finish(this.finish);
      this.stage.resolutionScale(resolution());
      this.stage.camera().clip(0.1F, 90.0F);
      this.stage.camera().idle(IdleMotion.sway(24000.0F, 2.5F, 0.3F, 0.04F));
      this.sky = (HazeNode)this.stage.add(new HazeNode());
      this.ground = (FxNode)((FxNode)this.stage.add(FxNode.glow(7.0400004F, this.hour.ground).fade(1.3F))).at(0.0F, 0.005F, 0.0F);
      this.clouds = (FxNode)this.stage.add(FxNode.clouds(12, 22.0F, 15.0F, 6.6F, 0.5F, 23L, 0.1F));
      this.motes = (MotesNode)((MotesNode)this.stage.add(new MotesNode(34, 13.0F, 3.2F, 8.0F, 0.035F, this.hour.motes, 5L))).at(0.0F, 3.6F, 3.0F);
      this.stars = (FxNode)((FxNode)this.stage.add(FxNode.sparkles(70, 44.0F, 9.0F, 2.0F, 0.09F, -2234113, 11L))).at(0.0F, 15.0F, -24.0F);
      this.pool = (FxNode)((FxNode)this.stage.add(FxNode.glow(0.9F, this.hour.beam))).at(0.0F, 0.02F, 0.0F);
      this.beam = (FxNode)((FxNode)this.stage.add(FxNode.cone(0.035F, 0.17F, 8.0F, this.hour.beam))).at(0.0F, 0.0F, 0.0F);
      this.pool.opacity(0.0F);
      this.beam.opacity(0.0F);
      this.light(this.hour);
      if (left != null) {
         this.show(left);
      }

      this.spotKnown = left != null;
   }

   Stage stage() {
      return this.stage;
   }

   static float resolution() {
      return Minecraft.getInstance().getWindow().getWidth() <= 1600 ? 2.0F : 1.5F;
   }

   LandScene looking(boolean on) {
      this.looking = on;
      return this;
   }

   LandScene veiled(boolean on) {
      if (this.veiled != on) {
         this.veiled = on;
         this.motes.grow(on ? 1.7F : 1.0F).steady(on ? 0.5F : 0.0F);
         this.light(this.lit());
      }

      return this;
   }

   @Nullable Land land() {
      return this.land;
   }

   @Nullable String name() {
      return this.name;
   }

   boolean leaving() {
      return this.leaving;
   }

   private void turn(Hour to) {
      if (to != this.hour) {
         this.was = LandScene.Hour.between(this.was, this.hour, this.turned());
         this.hour = to;
         this.turnedAt = this.stage.timeMs();
      }
   }

   private float turned() {
      return this.was == this.hour ? 1.0F : Mth.clamp((this.stage.timeMs() - this.turnedAt) / 700.0F, 0.0F, 1.0F);
   }

   private Hour lit() {
      if (this.was == this.hour) {
         return this.hour;
      } else {
         float t = this.turned();
         return LandScene.Hour.between(this.was, this.hour, t * t * (3.0F - 2.0F * t));
      }
   }

   private void light(Hour of) {
      Hour h = this.veiled ? of.dimmed(0.45F) : of;
      this.stage.background(h.fog);
      this.sky.colors(h.skyTop, h.fog);
      this.stage.fog(true, h.fogFrom, h.fogTo, h.fog);
      this.finish.warmth(h.warmth);
      this.stage.lighting().key(9.0F, 9.0F, 5.0F).keyColor(h.key[0], h.key[1], h.key[2]).ambientColor(h.ambient[0], h.ambient[1], h.ambient[2]).fill(-1.0F, 0.3F, 0.2F, h.fill[0], h.fill[1], h.fill[2]).rim(h.rim[0], h.rim[1], h.rim[2], h.rim[3]).sun(h.sun[0], h.sun[1], h.sun[2]);
      this.ground.color(Colors.lerp(h.fog, h.ground, 0.55F));
      this.clouds.alpha(h.clouds);
      this.clouds.color(this.veiled ? Colors.scale(-1, 0.45F) : -1);
      this.moteColor = h.motes;
      this.motes.color(this.veiled ? -1 : this.moteColor);
      this.stars.opacity(h.stars ? 1.0F : 0.0F);
      this.beam.color(h.beam);
      this.pool.color(h.beam);
   }

   private void show(Land shown) {
      this.land = shown;
      this.node = (LandNode)this.stage.add(new LandNode(shown));
      this.node.scale(0.1F);
      this.node.at(0.0F, 0.0F, 0.0F);
      if (this.leaving) {
         this.node.reach(1.0F);
         this.reach = 1.0F;
      }

   }

   private static @Nullable BlockPos lastPlace(IntegratedServer server) {
      CompoundTag player = server.getWorldData().getLoadedPlayerTag();
      if (player != null && player.contains("Pos", 9)) {
         if (player.contains("Dimension", 8) && !Level.OVERWORLD.location().toString().equals(player.getString("Dimension"))) {
            return null;
         } else {
            ListTag pos = player.getList("Pos", 6);
            return pos.size() < 3 ? null : BlockPos.containing(pos.getDouble(0), pos.getDouble(1), pos.getDouble(2));
         }
      } else {
         return null;
      }
   }

   private static @Nullable ServerLevel level(IntegratedServer server, @Nullable String dimension) {
      try {
         if (dimension != null) {
            ResourceLocation id = ResourceLocation.tryParse(dimension);
            if (id != null) {
               ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, id));
               if (level != null) {
                  return level;
               }
            }
         }

         return server.getLevel(Level.OVERWORLD);
      } catch (Exception var4) {
         return null;
      }
   }

   private void find() {
      if (this.looking && !this.leaving && !this.nothing && (this.land == null || !this.asked || this.guessed)) {
         IntegratedServer server = Minecraft.getInstance().getSingleplayerServer();
         if (server != null) {
            try {
               if (this.name == null) {
                  this.name = server.getWorldData().getLevelName();
               }

               if (this.kept == null) {
                  Path file = server.getWorldPath(LevelResource.ROOT).resolve("cfhgui/land.bin");
                  this.kept = LandSnapshot.read(file);
               }

               if (!this.kept.isDone()) {
                  return;
               }

               if (this.land == null) {
                  LandSnapshot.Kept k = (LandSnapshot.Kept)this.kept.getNow(null);
                  if (k != null) {
                     this.dimension = k.dimension();
                     this.turn(LandScene.Hour.of(k.land().dayTime));
                     this.show(k.land());
                  } else {
                     BlockPos last = lastPlace(server);
                     BlockPos at = last;
                     if (last == null) {
                        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
                        if (overworld == null) {
                           return;
                        }

                        if (server.getWorldData().overworldData().isInitialized()) {
                           at = overworld.getSharedSpawnPos();
                        } else {
                           at = BlockPos.ZERO;
                           this.guessed = true;
                        }
                     }

                     Land fresh = new Land(at.getX(), at.getZ());
                     fresh.dayTime = (int)Math.floorMod(server.getWorldData().overworldData().getDayTime(), 24000L);
                     this.turn(LandScene.Hour.of(fresh.dayTime));
                     this.show(fresh);
                  }

                  this.spotKnown = !this.guessed;
               }

               if (this.guessed && server.getWorldData().overworldData().isInitialized()) {
                  this.guessed = false;
                  ServerLevel overworld = server.getLevel(Level.OVERWORLD);
                  BlockPos spawn = overworld == null ? BlockPos.ZERO : overworld.getSharedSpawnPos();
                  int x = 80 + Math.floorDiv(spawn.getX() - this.land.centerX, 4);
                  int z = 64 + Math.floorDiv(spawn.getZ() - this.land.centerZ, 4);
                  if (x >= 28 && z >= 28 && x < 132 && z < 100) {
                     this.spotX = x;
                     this.spotZ = z;
                  } else {
                     this.land.drop();
                     if (this.node != null) {
                        this.stage.remove(this.node);
                     }

                     Land fresh = new Land(spawn.getX(), spawn.getZ());
                     fresh.dayTime = this.land.dayTime;
                     this.show(fresh);
                     this.reach = 0.0F;
                     this.asked = false;
                  }

                  this.spotKnown = true;
               }

               if (!this.asked) {
                  ServerLevel level = level(server, this.dimension);
                  if (level == null) {
                     return;
                  }

                  this.asked = true;
                  if (level.dimensionType().hasCeiling()) {
                     this.land.close();
                     return;
                  }

                  this.land.ask(LandSampler.of(level.getChunkSource().getGenerator(), level.getChunkSource().randomState(), level, level.getSeed()));
               }
            } catch (Exception e) {
               if (++this.tries > 600) {
                  this.nothing = true;
                  if (this.land != null) {
                     this.land.close();
                  }

                  CFHGuiMenu.LOGGER.warn("[CFHGui Menu] the land of the world could not be found: {}", e.toString());
               }
            }

         }
      }
   }

   void render(GuiGraphics g, int width, int height, float progress, float partialTick) {
      this.find();
      float motion = Theme.current().motion();
      float now = this.stage.timeMs();
      float dt = Math.min(0.25F, Math.max(0.0F, now - this.since) / 1000.0F);
      this.since = now;
      if (this.was != this.hour) {
         float t = this.turned();
         this.light(LandScene.Hour.between(this.was, this.hour, t * t * (3.0F - 2.0F * t)));
         if (t >= 1.0F) {
            this.was = this.hour;
         }
      }

      if (this.node != null) {
         if (this.leaving) {
            if (this.leftAt < 0.0F) {
               this.leftAt = now;
               this.node.settle(now);
            }

            this.node.leave(Mth.clamp((now - this.leftAt - 380.0F) / 1700.0F, 0.0F, 1.0F));
         } else {
            float to = Mth.clamp(progress, 0.0F, 1.0F);
            this.reach = motion <= 0.0F ? 1.0F : Math.min(1.0F, Math.max(this.reach + dt * 0.3F, Math.min(to, this.reach + dt * 1.1F)));
            this.node.reach(this.reach);
            if (motion <= 0.0F) {
               this.node.settle(now);
            }
         }
      }

      float wanted = this.node != null && this.node.up() > 0 && !this.leaving && this.spotKnown ? 1.0F : 0.0F;
      this.spot += (wanted - this.spot) * Math.min(1.0F, dt * 2.4F);
      if (this.land != null) {
         Land.Cell there = this.land.at(this.spotX, this.spotZ);
         float x = ((float)(this.spotX - 80) + 0.5F) * 0.1F;
         float z = ((float)(this.spotZ - 64) + 0.5F) * 0.1F;
         float y = (float)(there == null ? this.land.sea : there.level() + (there.tree() ? 2 : 0)) * 0.1F;
         this.beam.at(x, y, z);
         this.pool.at(x, y + 0.02F, z);
      }

      this.beam.opacity(this.spot * ((float)(this.hour.beam >>> 24) / 255.0F));
      this.pool.opacity(this.spot * ((float)(this.hour.beam >>> 24) / 255.0F));
      this.aim((float)width / (float)Math.max(1, height), this.leaving ? 1.0F : this.reach, motion);
      this.stage.render(g, 0, 0, width, height, (double)-1.0F, (double)-1.0F, partialTick);
   }

   private void aim(float aspect, float in, float motion) {
      int seaCells = this.land == null ? 9 : this.land.sea;
      float sea = (float)seaCells * 0.1F;
      float pitch = Mth.lerp(smooth((aspect - 1.2F) / 1.4F), 52.0F, 38.0F);
      float floor = (float)Math.max(1, seaCells - 4) * 0.1F;
      Aerial.View seen = Aerial.over(8.0F, 6.4F, floor, MARGINS, (double)27.0F, aspect, (double)pitch);
      Aerial.View v = seen.back(Mth.lerp(motion <= 0.0F ? 1.0F : smooth(in / 0.55F), 1.3F, 1.0F));
      this.stage.camera().at(0.0F, v.eyeY(), v.eyeZ()).lookAt(0.0F, v.targetY(), v.targetZ()).fov(v.fov());
      this.sky.line(0.0F, sea, -6.4F);
   }

   private static float smooth(float t) {
      float u = Mth.clamp(t, 0.0F, 1.0F);
      return u * u * (3.0F - 2.0F * u);
   }

   void close() {
      if (this.land != null && !this.leaving) {
         this.land.drop();
      }

      if (DevHarness.active()) {
         CFHGuiMenu.LOGGER.info("[CFHGui Menu] land scene: {} frames, {} ms a frame, {}x{}", new Object[]{this.stage.frames(), "%.2f".formatted(this.stage.frameMs()), this.stage.targetWidth(), this.stage.targetHeight()});
      }

      this.stage.close();
   }

   private static record Hour(int skyTop, int fog, float fogFrom, float fogTo, float[] key, float[] ambient, float[] fill, float[] rim, float[] sun, int ground, int beam, int motes, float clouds, boolean stars, float warmth) {
      static final Hour DAWN = new Hour(-13943701, -1133409, 9.0F, 21.0F, new float[]{0.8F, 0.6F, 0.52F}, new float[]{0.4F, 0.39F, 0.48F}, new float[]{0.12F, 0.15F, 0.26F}, new float[]{1.0F, 0.78F, 0.7F, 0.16F}, new float[]{0.8F, 0.5F, 0.3F}, -12897465, 1493164996, 1358944704, 0.85F, false, 0.014F);
      static final Hour DAY = new Hour(-13668400, -3874315, 10.0F, 23.0F, new float[]{0.8F, 0.77F, 0.68F}, new float[]{0.47F, 0.49F, 0.54F}, new float[]{0.14F, 0.18F, 0.26F}, new float[]{1.0F, 0.97F, 0.9F, 0.12F}, new float[]{0.5F, 0.85F, 0.35F}, -12958126, 1224736767, 956301311, 0.95F, false, 0.004F);
      static final Hour DUSK = new Hour(-15194297, -2119036, 9.0F, 21.0F, new float[]{0.78F, 0.66F, 0.5F}, new float[]{0.4F, 0.4F, 0.47F}, new float[]{0.12F, 0.16F, 0.26F}, new float[]{1.0F, 0.85F, 0.65F, 0.16F}, new float[]{0.75F, 0.6F, 0.35F}, -13422529, 1493164720, 1358946992, 0.85F, false, 0.012F);
      static final Hour NIGHT = new Hour(-16513522, -15393224, 8.0F, 20.0F, new float[]{0.34F, 0.4F, 0.62F}, new float[]{0.2F, 0.23F, 0.34F}, new float[]{0.06F, 0.08F, 0.16F}, new float[]{0.6F, 0.7F, 1.0F, 0.2F}, new float[]{-0.4F, 0.8F, 0.45F}, -15855070, 1892671743, 1086313727, 0.35F, true, 0.0F);

      static Hour between(Hour a, Hour b, float t) {
         if (t <= 0.0F) {
            return a;
         } else {
            return t >= 1.0F ? b : new Hour(Colors.lerp(a.skyTop, b.skyTop, t), Colors.lerp(a.fog, b.fog, t), Mth.lerp(t, a.fogFrom, b.fogFrom), Mth.lerp(t, a.fogTo, b.fogTo), mix(a.key, b.key, t), mix(a.ambient, b.ambient, t), mix(a.fill, b.fill, t), mix(a.rim, b.rim, t), mix(a.sun, b.sun, t), Colors.lerp(a.ground, b.ground, t), Colors.lerp(a.beam, b.beam, t), Colors.lerp(a.motes, b.motes, t), Mth.lerp(t, a.clouds, b.clouds), t < 0.5F ? a.stars : b.stars, Mth.lerp(t, a.warmth, b.warmth));
         }
      }

      private static float[] mix(float[] a, float[] b, float t) {
         float[] out = new float[a.length];

         for(int i = 0; i < out.length; ++i) {
            out[i] = Mth.lerp(t, a[i], b[i]);
         }

         return out;
      }

      Hour dimmed(float share) {
         return new Hour(Colors.scale(this.skyTop, share), Colors.scale(this.fog, share), this.fogFrom, this.fogTo, dimmed(this.key, share), dimmed(this.ambient, share), dimmed(this.fill, share), dimmed(this.rim, share), this.sun, Colors.scale(this.ground, share), this.beam, this.motes, this.clouds, this.stars, this.warmth);
      }

      private static float[] dimmed(float[] light, float share) {
         float[] out = (float[])((float[]) light).clone();

         for(int i = 0; i < Math.min(3, out.length); ++i) {
            out[i] *= share;
         }

         return out;
      }

      static Hour of(int dayTime) {
         int t = Math.floorMod(dayTime, 24000);
         if (t < 22300 && t >= 1200) {
            if (t < 10800) {
               return DAY;
            } else {
               return t < 13600 ? DUSK : NIGHT;
            }
         } else {
            return DAWN;
         }
      }
   }
}
