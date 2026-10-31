package com.onehumanawa.cfhcore.content.gui.menu.client.loading.journey;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.onehumanawa.cfhcore.content.gui.core.client.DevHarness;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Fonts;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.screen.slot.Layout;
import com.onehumanawa.cfhcore.content.gui.core.screen.slot.MenuSlots;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.menu.CFHGuiMenu;
import com.onehumanawa.cfhcore.content.gui.menu.mixin.LevelLoadingScreenAccessor;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.ProgressScreen;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.progress.StoringChunkProgressListener;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.storage.LevelResource;
import org.jetbrains.annotations.Nullable;

public final class Journey {
   private static final ResourceLocation ICON = ResourceLocation.fromNamespaceAndPath("cfhgui", "journey/server_icon");
   private static final ResourceLocation NO_ICON = ResourceLocation.withDefaultNamespace("textures/misc/unknown_server.png");
   private static final Set<String> OPENING = Set.of("selectWorld.data_read", "selectWorld.resource_load", "createWorld.preparing", "cfhgui.loading.generic");
   private static final long OVER_MS = 20000L;
   private static @Nullable LandScene land;
   private static @Nullable GatewayScene gateway;
   private static final boolean WALK = Boolean.getBoolean("cfhgui.gatewayWalk");
   private static long gatewayAt;
   private static long drawnAt;
   private static boolean running;
   private static int elsewhere;
   private static @Nullable Land left;
   private static @Nullable String leftName;
   private static long leftAt;
   private static boolean leftLocal;
   private static @Nullable String serverName;
   private static @Nullable String serverAddress;
   private static boolean serverIcon;
   private static float shown;
   private static long shownAt;
   private static float wordsIn;
   private static boolean broken;

   private Journey() {
   }

   public static boolean active() {
      return MenuSlots.effective("minecraft:level_loading") == Layout.OVERHAUL;
   }

   public static void joining(@Nullable ServerData data, String address) {
      serverName = data != null && data.name != null && !data.name.isBlank() ? data.name : address;
      serverAddress = address;
      serverIcon = false;
      byte[] png = data == null ? null : data.getIconBytes();
      if (png != null && active()) {
         try {
            NativeImage image = NativeImage.read(new ByteArrayInputStream(png));
            Minecraft.getInstance().getTextureManager().register(ICON, new DynamicTexture(image));
            serverIcon = true;
         } catch (Exception e) {
            CFHGuiMenu.LOGGER.debug("[CFHGui Menu] the server's picture could not be read: {}", e.toString());
         }

      }
   }

   public static void leaving() {
      left = null;
      leftName = null;
      leftAt = 0L;
      Minecraft mc = Minecraft.getInstance();
      if (mc.level != null && mc.player != null) {
         IntegratedServer server = mc.getSingleplayerServer();
         leftAt = Util.getMillis();
         leftLocal = server != null;
         ServerData data = mc.getCurrentServer();
         leftName = server != null ? worldName(mc) : (data == null ? null : data.name);
         if (active() && server != null) {
            Land taken = LandSnapshot.take(mc);
            left = taken;
            if (taken != null) {
               try {
                  Path file = server.getWorldPath(LevelResource.ROOT).resolve("cfhgui/land.bin");
                  LandSnapshot.write(taken, file, mc.level.dimension().location().toString());
               } catch (Exception e) {
                  CFHGuiMenu.LOGGER.warn("[CFHGui Menu] the land could not be kept: {}", e.toString());
               }

            }
         }
      }
   }

   public static void tick() {
      if (running) {
         Screen s = Minecraft.getInstance().screen;
         boolean loading = s instanceof LevelLoadingScreen || s instanceof ConnectScreen || s instanceof ReceivingLevelScreen || s instanceof GenericMessageScreen || s instanceof ProgressScreen;
         elsewhere = loading ? 0 : elsewhere + 1;
         if (elsewhere >= 2 || Util.getMillis() - drawnAt > 20000L) {
            running = false;
            end();
            left = null;
         }

      }
   }

   private static void end() {
      if (land != null) {
         if (!Backdrop.keep(land)) {
            land.close();
         }

         land = null;
      }

      if (gateway != null) {
         gateway.close();
         gateway = null;
      }

   }

   private static String key(@Nullable Component c) {
      String var10000;
      if (c != null) {
         ComponentContents var2 = c.getContents();
         if (var2 instanceof TranslatableContents) {
            TranslatableContents t = (TranslatableContents)var2;
            var10000 = t.getKey();
            return var10000;
         }
      }

      var10000 = "";
      return var10000;
   }

   private static int step(@Nullable Component status) {
      byte var10000;
      switch (key(status)) {
         case "connect.connecting":
         case "connect.transferring":
            var10000 = 0;
            break;
         case "connect.authorizing":
            var10000 = 1;
            break;
         case "connect.encrypting":
            var10000 = 2;
            break;
         case "connect.joining":
            var10000 = 3;
            break;
         default:
            var10000 = 2;
      }

      return var10000;
   }

   private static int walk(long ms) {
      return ms < 1500L ? 0 : (ms < 2700L ? 1 : (ms < 3700L ? 2 : (ms < 5200L ? 3 : 4)));
   }

   public static boolean render(Screen screen, GuiGraphics g, int mouseX, int mouseY, float partialTick, Component title, @Nullable Component status, float progress, @Nullable StoringChunkProgressListener chunks, List<? extends AbstractWidget> buttons) {
      if (broken) {
         return false;
      } else {
         try {
            frame(screen, g, mouseX, mouseY, partialTick, title, status, progress, chunks, buttons);
            return true;
         } catch (Exception e) {
            broken = true;
            CFHGuiMenu.LOGGER.error("[CFHGui Menu] the loading scene failed; the loading screens fall back to the Custom layout", e);

            try {
               end();
            } catch (Exception var12) {
               land = null;
               gateway = null;
            }

            return false;
         }
      }
   }

   private static void frame(Screen screen, GuiGraphics g, int mouseX, int mouseY, float partialTick, Component title, @Nullable Component status, float progress, @Nullable StoringChunkProgressListener chunks, List<? extends AbstractWidget> buttons) {
      Minecraft mc = Minecraft.getInstance();
      long now = Util.getMillis();
      boolean fresh = !running || now - drawnAt > 20000L;
      running = true;
      elsewhere = 0;
      boolean world = screen instanceof LevelLoadingScreen;
      boolean connect = screen instanceof ConnectScreen;
      boolean terrain = screen instanceof ReceivingLevelScreen;
      boolean message = screen instanceof ProgressScreen || screen instanceof GenericMessageScreen;
      boolean out = message && (mc.level != null && leftAt != 0L && now - leftAt < 120000L || !fresh && (land != null && land.leaving() || gateway != null && gateway.leaving()));
      Kind kind;
      if (connect) {
         kind = Journey.Kind.GATEWAY;
      } else if (world) {
         kind = Journey.Kind.LAND;
      } else if (out) {
         kind = leftLocal ? Journey.Kind.LAND : Journey.Kind.GATEWAY;
      } else if (!fresh && gateway != null) {
         kind = Journey.Kind.GATEWAY;
      } else if (!fresh && land != null) {
         kind = Journey.Kind.LAND;
      } else if (mc.level != null) {
         kind = Journey.Kind.PLAIN;
      } else {
         kind = Journey.Kind.LAND;
      }

      if (fresh) {
         end();
         shown = 0.0F;
         wordsIn = 0.0F;
      }

      float dt = fresh ? 0.0F : Math.min(0.25F, (float)(now - shownAt) / 1000.0F);
      shownAt = now;
      drawnAt = now;
      wordsIn = Math.min(1.0F, wordsIn + dt * 2.6F);
      float target;
      if (out) {
         target = -1.0F;
      } else if (kind == Journey.Kind.LAND) {
         target = world ? 0.1F + 0.86F * Mth.clamp(progress, 0.0F, 1.0F) : (terrain ? 1.0F : Math.max(shown, 0.06F));
      } else {
         target = progress;
      }

      if (target < 0.0F) {
         shown = 0.0F;
      } else {
         shown = !fresh && !(Theme.current().motion() <= 0.0F) ? shown + (target - shown) * Math.min(1.0F, dt * 7.0F) : target;
      }

      String step = "cfhgui.loading.generic".equals(key(title)) ? null : title.getString();
      String heading = title.getString();
      String under = status != null && !status.getString().isBlank() ? status.getString() : null;
      String name = null;
      float fade = 1.0F;
      boolean icon = false;
      String kicker;
      switch (kind.ordinal()) {
         case 0:
            if (gateway != null) {
               gateway.close();
               gateway = null;
            }

            if (land == null || land.leaving() != out) {
               if (land != null) {
                  land.close();
               }

               land = out ? new LandScene(left != null ? left : empty()) : Backdrop.hand();
            }

            land.render(g, screen.width, screen.height, shown, partialTick);
            boolean opening = world || terrain || mc.getSingleplayerServer() != null || OPENING.contains(key(title));
            kicker = Component.translatable(out ? "cfhgui.journey.leaving_world" : (opening ? "cfhgui.journey.opening_world" : "cfhgui.journey.waiting")).getString();
            name = out ? leftName : land.name();
            if (out && step == null) {
               under = Component.translatable("menu.savingLevel").getString();
            }
            break;
         case 1:
            if (land != null) {
               land.close();
               land = null;
            }

            if (gateway == null || gateway.leaving() != out) {
               if (gateway != null) {
                  gateway.close();
               }

               gateway = new GatewayScene(out);
               gatewayAt = Util.getMillis();
            }

            int at = WALK ? walk(Util.getMillis() - gatewayAt) : (terrain ? 4 : step(status));
            gateway.render(g, screen.width, screen.height, at, partialTick);
            fade = 1.0F - Mth.clamp(gateway.through() * 1.6F, 0.0F, 1.0F);
            kicker = Component.translatable(out ? "cfhgui.journey.leaving_server" : "cfhgui.journey.joining_server").getString();
            name = out ? leftName : serverName;
            icon = name != null && !out;
            if (out && step == null) {
               under = Component.translatable("cfhgui.journey.disconnecting").getString();
            }
            break;
         default:
            end();
            screen.renderBackground(g, mouseX, mouseY, partialTick);
            kicker = Component.translatable("cfhgui.journey.waiting").getString();
      }

      if (name != null && !name.isBlank()) {
         if (under == null) {
            under = step;
         }

         heading = name;
      } else if (heading.equalsIgnoreCase(kicker) && under != null) {
         heading = under;
         under = null;
      }

      words(screen, g, mouseX, mouseY, partialTick, kicker, heading, under, icon, target < 0.0F ? -1.0F : shown, world ? progress : -1.0F, chunks, buttons, fade * wordsIn * wordsIn * (3.0F - 2.0F * wordsIn));
      DevHarness.loadingFrame(g, screen.getClass().getSimpleName());
   }

   private static Land empty() {
      Land none = new Land(0, 0);
      none.dayTime = 12500;
      none.close();
      return none;
   }

   private static @Nullable String worldName(Minecraft mc) {
      IntegratedServer server = mc.getSingleplayerServer();

      try {
         return server == null ? null : server.getWorldData().getLevelName();
      } catch (Exception var3) {
         return null;
      }
   }

   private static void words(Screen screen, GuiGraphics g, int mouseX, int mouseY, float partialTick, String kicker, String heading, @Nullable String under, boolean icon, float progress, float percent, @Nullable StoringChunkProgressListener chunks, List<? extends AbstractWidget> buttons, float alpha) {
      Theme t = Theme.current();
      Palette p = t.palette();
      boolean van = t.isVanilla();
      Font font = Minecraft.getInstance().font;
      int w = screen.width;
      int h = screen.height;
      if (!(alpha <= 0.01F)) {
         int margin = Mth.clamp(w / 22, 12, 40);
         CFHGuiDraw.vgradient(g, 0, h - Math.round((float)h * 0.5F), w, Math.round((float)h * 0.5F), 0, Colors.withAlpha(-16777216, Math.round(200.0F * alpha)));
         int accent = van ? -1 : p.accent();
         int bar = h - Mth.clamp(h / 14, 14, 26);
         float big = h >= 300 ? 2.0F : 1.5F;
         int headingH = Math.round(9.0F * big);
         int y = bar - 12 - (under == null ? 0 : 12) - headingH - 14;
         int slide = Math.round((1.0F - alpha) * 6.0F);
         String label = kicker.toUpperCase(Locale.ROOT);
         if (!van) {
            CFHGuiDraw.rect(g, margin, y + slide + 3, 10, 2, Colors.scaleAlpha(accent, alpha));
         }

         g.drawString(font, label, margin + (van ? 0 : 15), y + slide, Colors.scaleAlpha(van ? -2039584 : accent, alpha), true);
         y += 14;
         int x = margin;
         if (icon) {
            int side = headingH + 2;
            float[] was = (float[])RenderSystem.getShaderColor().clone();
            RenderSystem.enableBlend();
            g.setColor(1.0F, 1.0F, 1.0F, alpha);
            g.blit(serverIcon ? ICON : NO_ICON, margin, y + slide - 1, side, side, 0.0F, 0.0F, 64, 64, 64, 64);
            g.setColor(was[0], was[1], was[2], was[3]);
            x = margin + side + 7;
         }

         int room = Math.round((float)(w - x - margin - (chunks != null ? 70 : (buttons.isEmpty() ? 0 : 120))) / big);
         FormattedCharSequence name = CFHGuiDraw.truncate(Fonts.heading(heading), Math.max(40, room));
         g.pose().pushPose();
         g.pose().translate((float)x, (float)(y + slide), 0.0F);
         g.pose().scale(big, big, 1.0F);
         g.drawString(font, name, 0, 0, Colors.scaleAlpha(van ? -1 : p.text(), alpha), true);
         g.pose().popPose();
         y += headingH + 5;
         if (under != null) {
            g.drawString(font, CFHGuiDraw.truncate(Component.literal(under), w - margin * 2 - 60), margin, y + slide, Colors.scaleAlpha(van ? -3618616 : p.textMuted(), alpha), true);
         }

         int barW = w - margin * 2;
         CFHGuiDraw.rect(g, margin, bar, barW, 2, Colors.scaleAlpha(1358954495, alpha));
         if (progress < 0.0F) {
            int seg = Math.max(30, barW / 4);
            float phase = (float)(Util.getMillis() % 1500L) / 1500.0F;
            int sx = margin - seg + Math.round((float)(barW + seg) * phase);
            int from = Math.max(margin, sx);
            int to = Math.min(margin + barW, sx + seg);
            if (to > from) {
               CFHGuiDraw.rect(g, from, bar, to - from, 2, Colors.scaleAlpha(accent, alpha));
            }
         } else {
            int fill = Math.round((float)barW * Mth.clamp(progress, 0.0F, 1.0F));
            CFHGuiDraw.rect(g, margin, bar, fill, 2, Colors.scaleAlpha(accent, alpha));
            if (fill > 2 && fill < barW) {
               CFHGuiDraw.rect(g, margin + fill - 2, bar - 1, 3, 4, Colors.scaleAlpha(van ? -1 : p.accentHover(), alpha));
            }
         }

         if (percent >= 0.0F) {
            float var10000 = Mth.clamp(percent, 0.0F, 1.0F);
            String number = Math.round(var10000 * 100.0F) + "%";
            g.drawString(font, number, margin + barW - font.width(number), bar - 12, Colors.scaleAlpha(van ? -1 : p.text(), alpha), true);
         }

         if (chunks != null && chunks.getDiameter() > 0) {
            int d = chunks.getDiameter();
            int cell = Mth.clamp(56 / d, 1, 4);
            int side = d * cell;
            int mx = margin + barW - side;
            int my = bar - 18 - side;
            CFHGuiDraw.rect(g, mx - 3, my - 3, side + 6, side + 6, Colors.withAlpha(-16777216, Math.round(112.0F * alpha)));
            if (!van) {
               CFHGuiDraw.outline(g, mx - 3, my - 3, side + 6, side + 6, Colors.scaleAlpha(1090519039, alpha), 0);
            }

            Object2IntMap<ChunkStatus> colors = LevelLoadingScreenAccessor.cfhgui$colors();
            int a = Math.round(255.0F * alpha);
            g.drawManaged(() -> {
               for(int r = 0; r < d; ++r) {
                  for(int s = 0; s < d; ++s) {
                     ChunkStatus status = chunks.getStatus(r, s);
                     if (status != null) {
                        g.fill(mx + r * cell, my + s * cell, mx + r * cell + cell, my + s * cell + cell, colors.getInt(status) & 16777215 | a << 24);
                     }
                  }
               }

            });
         }

         int by = bar - 14 - 20;

         for(AbstractWidget b : buttons) {
            b.setWidth(Math.min(120, Math.max(80, w / 5)));
            b.setX(margin + barW - b.getWidth());
            b.setY(by);
            b.setAlpha(alpha);
            b.render(g, mouseX, mouseY, partialTick);
            by -= 24;
         }

      }
   }

   private static enum Kind {
      LAND,
      GATEWAY,
      PLAIN;
   }
}
