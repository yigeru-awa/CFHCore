package com.onehumanawa.cfhcore.content.gui.menu.client.lookat;

import com.onehumanawa.cfhcore.content.gui.core.event.CFHGuiEvents;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.menu.CFHGuiMenu;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

public final class LookAt {
   private static final long LINGER_MS = 600L;
   private static final float SLIDE = 4.0F;
   private static final float FIGURE_FROM = 0.85F;
   private static final long FORGET_MS = 5000L;
   private static boolean initialised;
   private static boolean failed;
   private static boolean listening;
   private static boolean hudHidden;
   private static volatile boolean worldChanged;
   private static volatile boolean reloaded;
   private static @Nullable LivingEntity target;
   private static boolean awaited;
   private static long seenAtMs;
   private static long lastFrameNanos;
   private static int side = -1;
   private static int seenRevision = -1;
   private static int seenFacts = -1;
   private static @Nullable Theme seenTheme;
   private static final Glide ALPHA = new Glide();
   private static final Glide X = new Glide();
   private static final Glide Y = new Glide();
   private static boolean painting;
   private static @Nullable LookAtConfig config;
   private static @Nullable GuiGraphics graphics;
   private static final Matrix4f SAVED = new Matrix4f();
   private static final Runnable PAINT = LookAt::paintGuarded;

   private LookAt() {
   }

   public static synchronized void init() {
      if (!initialised) {
         initialised = true;
         LookAtConfig.file();
         CFHGuiEvents.CLIENT_TICK_END.register(LookAt::tick);
         CFHGuiEvents.HUD_RENDER.register(LookAt::render);
         CFHGuiEvents.CLIENT_JOINED_SERVER.register(() -> worldChanged = true);
         CFHGuiEvents.CLIENT_LEFT_SERVER.register(() -> worldChanged = true);
         LookAtSettings.init();
         LookAtHarness.init();
      }
   }

   private static boolean wanted(Minecraft mc, LookAtConfig cfg) {
      return cfg.enabled && (!cfg.yieldToMods || StandBack.owner() == null) && mc.player != null && mc.level != null && mc.screen == null && !mc.player.isSpectator();
   }

   private static void tick() {
      LivingEntity mob = target;
      if (mob != null && !failed) {
         Minecraft mc = Minecraft.getInstance();
         if (Util.getMillis() - seenAtMs > 5000L) {
            rest();
         } else if (!mob.isRemoved() && mob.level() == mc.level) {
            try {
               LookFacts.read(mc, mc.font, mob);
            } catch (RuntimeException e) {
               fail(e);
            }

         }
      }
   }

   private static void render(GuiGraphics g, float partialTick) {
      if (!failed) {
         LookAtConfig cfg = LookAtConfig.get();
         if (target != null || cfg.enabled) {
            if (LookAtHarness.PROFILE) {
               LookAtHarness.began();
            }

            boolean drew = false;

            try {
               drew = frame(g, Minecraft.getInstance(), cfg, partialTick);
            } catch (RuntimeException e) {
               fail(e);
            } finally {
               LookView.endFrame();
            }

            if (LookAtHarness.PROFILE) {
               LookAtHarness.ended(drew);
            }

         }
      }
   }

   private static boolean frame(GuiGraphics g, Minecraft mc, LookAtConfig cfg, float partialTick) {
      if (worldChanged) {
         worldChanged = false;
         rest();
      }

      if (mc.screen != null) {
         if (target != null) {
            rest();
         }

         return false;
      } else if (mc.options.hideGui) {
         hudHidden = true;
         return false;
      } else {
         long nanos = System.nanoTime();
         long millis = Util.getMillis();
         boolean may = wanted(mc, cfg);
         LivingEntity aimed = may ? LookTarget.of(mc) : null;
         if (aimed != null) {
            if (aimed != target) {
               take(mc, aimed);
            }

            seenAtMs = millis;
         }

         LivingEntity mob = target;
         if (mob == null) {
            hudHidden = false;
            return false;
         } else {
            if (awaited) {
               awaited = false;
               if (!LookView.ready()) {
                  return false;
               }
            }

            if (reloaded) {
               reloaded = false;
               LookCanvas.reloaded();
               LookFacts.forget();
            }

            float dt = lastFrameNanos == 0L ? 16.0F : Math.min(100.0F, (float)(nanos - lastFrameNanos) / 1000000.0F);
            lastFrameNanos = nanos;
            Theme theme = Theme.variant(Theme.globalStyle());
            float motion = theme.motion();
            float quick = motion <= 0.0F ? 1.0F : 1.0F - (float)Math.exp((double)(-dt / (70.0F * motion)));
            float easy = motion <= 0.0F ? 1.0F : 1.0F - (float)Math.exp((double)(-dt / (160.0F * motion)));
            float slow = motion <= 0.0F ? 1.0F : 1.0F - (float)Math.exp((double)(-dt / (420.0F * motion)));
            if (seenFacts != LookFacts.revision || seenRevision != LookAtConfig.revision() || seenTheme != theme) {
               seenFacts = LookFacts.revision;
               seenRevision = LookAtConfig.revision();
               seenTheme = theme;
               LookPlate.measure(cfg, LookPortrait.of(mob));
            }

            boolean shown = may && millis - seenAtMs <= 600L && LookPlate.height > 0;
            int sw = g.guiWidth();
            int sh = g.guiHeight();
            if (shown) {
               head(mob, sw, sh);
               int found = LookPlacement.solve(sw, sh, LookPlate.width, LookPlate.height, side);
               if (found == -1) {
                  shown = false;
               } else {
                  if (side != -1 && !(ALPHA.value <= 0.01F)) {
                     if (found != side) {
                        ALPHA.snap(0.0F);
                        X.snap((float)LookPlacement.x);
                        Y.snap((float)LookPlacement.y);
                     }
                  } else {
                     X.snap((float)LookPlacement.x);
                     Y.snap((float)LookPlacement.y);
                  }

                  side = found;
                  X.to((float)LookPlacement.x);
                  Y.to((float)LookPlacement.y);
               }
            }

            if (hudHidden) {
               hudHidden = false;
               ALPHA.snap(shown ? 1.0F : 0.0F);
            }

            ALPHA.to(shown ? 1.0F : 0.0F);
            float alpha = ALPHA.step(quick);
            if (alpha <= 0.01F) {
               if (aimed == null && millis - seenAtMs > 600L) {
                  rest();
               }

               return false;
            } else {
               LookPlate.step(quick, slow);
               float out = (1.0F - alpha) * 4.0F;
               float x = X.step(easy) + (side == 0 ? -out : (side == 1 ? out : 0.0F));
               float y = Y.step(easy) + (side == 2 ? -out : (side == 3 ? out : 0.0F));
               double guiScale = Math.max((double)1.0F, mc.getWindow().getGuiScale());
               float px = (float)((double)Math.round((double)x * guiScale) / guiScale);
               float py = (float)((double)Math.round((double)y * guiScale) / guiScale);
               LookPlate.begin(theme, mc.font, alpha);
               Matrix4f pose = g.pose().last().pose();
               SAVED.set(pose);
               pose.translate(px, py, 0.0F);
               config = cfg;
               graphics = g;
               painting = true;

               try {
                  batch(g);
               } finally {
                  painting = false;
                  graphics = null;
                  pose.set(SAVED);
               }

               if (LookAtHarness.PROFILE) {
                  LookAtHarness.part(0);
               }

               if (failed) {
                  return false;
               } else if (LookPlate.well && !(alpha < 0.85F) && !mob.isRemoved() && mob.level() == mc.level) {
                  if (!LookPortrait.draw(g, mob, px + 5.0F, py + 5.0F, 26, 28, partialTick)) {
                     seenFacts = -1;
                  }

                  if (LookAtHarness.PROFILE) {
                     LookAtHarness.part(1);
                  }

                  return true;
               } else {
                  return true;
               }
            }
         }
      }
   }

   private static void batch(GuiGraphics g) {
      g.drawManaged(PAINT);
   }

   private static void head(LivingEntity mob, int sw, int sh) {
      if (LookView.ready() && !mob.isRemoved()) {
         float partial = LookView.partialTick();
         double x = Mth.lerp((double)partial, mob.xo, mob.getX());
         double y = Mth.lerp((double)partial, mob.yo, mob.getY()) + (double)mob.getEyeHeight();
         double z = Mth.lerp((double)partial, mob.zo, mob.getZ());
         if (!LookView.project(x, y, z, sw, sh)) {
            LookPlacement.noHead();
         } else {
            float half = Mth.clamp(mob.getBbWidth() * 0.5F, 0.2F, 0.6F) * LookView.pixelsPerBlock;
            LookPlacement.head(LookView.x - half, LookView.y - half, LookView.x + half, LookView.y + half);
         }
      } else {
         LookPlacement.noHead();
      }
   }

   private static void take(Minecraft mc, LivingEntity mob) {
      target = mob;
      LookFacts.forget();
      LookFacts.read(mc, mc.font, mob);
      LookPlate.settle();
      awaited = !LookView.ready();
      LookView.want(true);
      if (!listening) {
         listening = true;
         ResourceManager var3 = mc.getResourceManager();
         if (var3 instanceof ReloadableResourceManager) {
            ReloadableResourceManager reloadable = (ReloadableResourceManager)var3;
            reloadable.registerReloadListener((ResourceManagerReloadListener)(manager) -> reloaded = true);
         }
      }

   }

   private static void rest() {
      target = null;
      awaited = false;
      side = -1;
      lastFrameNanos = 0L;
      ALPHA.snap(0.0F);
      LookView.want(false);
   }

   private static void fail(RuntimeException e) {
      failed = true;
      rest();
      CFHGuiMenu.LOGGER.error("[CFHGui Menu] drawing the look-at plate failed; it is off for this session", e);
   }

   private static void paintGuarded() {
      try {
         LookAtConfig cfg = config;
         GuiGraphics g = graphics;
         if (!painting || cfg == null || g == null) {
            return;
         }

         LookPlate.paint(g, cfg);
      } catch (RuntimeException e) {
         fail(e);
      }

   }

   static {
      ALPHA.snap(0.0F);
   }
}
