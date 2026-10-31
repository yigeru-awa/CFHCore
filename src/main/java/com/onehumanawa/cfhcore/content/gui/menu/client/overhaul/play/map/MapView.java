package com.onehumanawa.cfhcore.content.gui.menu.client.overhaul.play.map;

import com.mojang.blaze3d.systems.RenderSystem;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Clock;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Textures;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSpinner;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiTooltips;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiWidget;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

public final class MapView extends CFHGuiWidget {
   private static final float MIN_ZOOM = 0.125F;
   private static final float MAX_ZOOM = 6.0F;
   private @Nullable MapTiles tiles;
   private List<Waypoints.Waypoint> waypoints = List.of();
   private double centreX;
   private double centreZ;
   private double homeX;
   private double homeZ;
   private final Anim zoom;
   private boolean hasSpawn;
   private boolean hasPlayer;
   private int spawnX;
   private int spawnZ;
   private double playerX;
   private double playerZ;
   private boolean dragging;
   private long lastClickMs;
   private Component placeholder;
   private Textures.@Nullable Loaded picture;
   private final Anim appear;

   public MapView(int x, int y, int width, int height) {
      super(x, y, width, height, Component.translatable("cfhgui.play.map"));
      this.zoom = new Anim(0.5F, 160, Ease.OUT_CUBIC);
      this.placeholder = Component.empty();
      this.appear = new Anim(0.0F, 260, Ease.OUT_CUBIC);
      this.silent();
   }

   public MapView show(@Nullable MapTiles tiles, double x, double z) {
      this.tiles = tiles;
      this.centreX = this.homeX = x;
      this.centreZ = this.homeZ = z;
      this.zoom.snap(0.5F);
      this.appear.snap(0.0F);
      this.appear.set(1.0F);
      return this;
   }

   public MapView spawn(int x, int z) {
      this.hasSpawn = true;
      this.spawnX = x;
      this.spawnZ = z;
      return this;
   }

   public MapView player(double x, double z) {
      this.hasPlayer = true;
      this.playerX = x;
      this.playerZ = z;
      return this;
   }

   public MapView clearMarks() {
      this.hasSpawn = false;
      this.hasPlayer = false;
      this.waypoints = List.of();
      return this;
   }

   public MapView waypoints(List<Waypoints.Waypoint> list) {
      this.waypoints = list == null ? List.of() : list;
      return this;
   }

   public MapView placeholder(Component text, Textures.@Nullable Loaded picture) {
      this.placeholder = text;
      this.picture = picture;
      return this;
   }

   public boolean hasMap() {
      return this.tiles != null;
   }

   public void onClick(double mouseX, double mouseY) {
      super.onClick(mouseX, mouseY);
      if (this.tiles != null) {
         if (this.overHome(mouseX, mouseY)) {
            this.home();
         } else {
            long now = Clock.nowMs();
            if (now - this.lastClickMs < 350L) {
               this.home();
            }

            this.lastClickMs = now;
            this.dragging = true;
         }
      }
   }

   public void onRelease(double mouseX, double mouseY) {
      super.onRelease(mouseX, mouseY);
      this.dragging = false;
   }

   protected void onDrag(double mouseX, double mouseY, double dragX, double dragY) {
      if (this.dragging && this.tiles != null) {
         float z = this.zoom.get();
         this.centreX -= dragX / (double)z;
         this.centreZ -= dragY / (double)z;
      }
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
      if (this.tiles != null && this.isMouseOver(mouseX, mouseY) && scrollY != (double)0.0F) {
         float from = this.zoom.target();
         float to = Mth.clamp(from * (scrollY > (double)0.0F ? 1.35F : 0.7407407F), 0.125F, 6.0F);
         if (to < from && this.regions(to) > 48L) {
            to = from;
         }

         if (to == from) {
            return true;
         } else {
            double px = mouseX - ((double)this.getX() + (double)this.getWidth() / (double)2.0F);
            double pz = mouseY - ((double)this.getY() + (double)this.getHeight() / (double)2.0F);
            float now = this.zoom.get();
            double worldX = this.centreX + px / (double)now;
            double worldZ = this.centreZ + pz / (double)now;
            this.centreX = worldX - px / (double)to;
            this.centreZ = worldZ - pz / (double)to;
            this.zoom.snap(to);
            return true;
         }
      } else {
         return false;
      }
   }

   private long regions(float z) {
      long across = (long)Math.ceil((double)this.getWidth() / (double)z / (double)512.0F) + 1L;
      long down = (long)Math.ceil((double)this.getHeight() / (double)z / (double)512.0F) + 1L;
      return across * down;
   }

   private void home() {
      this.centreX = this.homeX;
      this.centreZ = this.homeZ;
      this.zoom.set(0.5F);
   }

   private boolean overHome(double mx, double my) {
      int bx = this.getX() + this.getWidth() - 19;
      int by = this.getY() + this.getHeight() - 19;
      return mx >= (double)bx && mx < (double)(bx + 15) && my >= (double)by && my < (double)(by + 15);
   }

   protected void renderDark(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      this.draw(g, mouseX, mouseY, false);
   }

   protected void renderVanilla(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      this.draw(g, mouseX, mouseY, true);
   }

   private void draw(GuiGraphics g, int mouseX, int mouseY, boolean vanilla) {
      Theme t = Theme.current();
      Palette p = t.palette();
      float a = this.effectiveAlpha();
      if (!(a <= 0.004F)) {
         int x = this.getX();
         int y = this.getY() + this.enterOffset();
         int w = this.getWidth();
         int h = this.getHeight();
         int r = vanilla ? 0 : t.radius();
         CFHGuiDraw.shadow(g, x, y, w, h, 0.5F * a);
         CFHGuiDraw.pixelRound(g, x, y, w, h, Colors.scaleAlpha(vanilla ? -15724528 : -15855855, a), r);
         g.enableScissor(x + 1, y + 1, x + w - 1, y + h - 1);
         if (this.tiles != null) {
            this.tiles(g, x, y, w, h, a * this.appear.get());
         } else {
            this.placeholder(g, x, y, w, h, a, vanilla, p);
         }

         g.disableScissor();
         if (this.tiles != null) {
            this.marks(g, x, y, w, h, mouseX, mouseY, a, vanilla, p);
            CFHGuiDraw.vgradient(g, x + 1, y + 1, w - 2, 10, Colors.scaleAlpha(1879048192, a), 0);
            CFHGuiDraw.vgradient(g, x + 1, y + h - 11, w - 2, 10, 0, Colors.scaleAlpha(1879048192, a));
            if (this.isHovered()) {
               float z = this.zoom.get();
               int bx = (int)Math.floor(this.centreX + ((double)mouseX - ((double)x + (double)w / (double)2.0F)) / (double)z);
               int bz = (int)Math.floor(this.centreZ + ((double)mouseY - ((double)y + (double)h / (double)2.0F)) / (double)z);
               String at = bx + ", " + bz;
               g.drawString(CFHGuiDraw.font(), at, x + 5, y + h - 11, Colors.scaleAlpha(-1513760, a * this.hover()), true);
            }

            boolean overHome = this.overHome((double)mouseX, (double)mouseY);
            int bx = x + w - 19;
            int by = y + h - 19;
            CFHGuiDraw.pixelRound(g, bx, by, 15, 15, Colors.scaleAlpha(overHome ? -534765534 : -1609560046, a), vanilla ? 0 : 2);
            Icons.draw(g, Icon.COMPASS, bx + 3, by + 3, 9, Colors.scaleAlpha(overHome ? -1 : -4671824, a));
            if (overHome) {
               CFHGuiTooltips.request(Component.translatable("cfhgui.play.map.home"), this);
            }
         }

         CFHGuiDraw.outline(g, x, y, w, h, Colors.scaleAlpha(vanilla ? -16777216 : Colors.lerp(p.border(), p.borderStrong(), this.hover()), a), r);
         CFHGuiDraw.focusRing(g, x, y, w, h, this.focus() * a);
      }
   }

   private void tiles(GuiGraphics g, int x, int y, int w, int h, float a) {
      MapTiles map = this.tiles;
      if (map != null) {
         float z = this.zoom.get();
         double left = this.centreX - (double)w / (double)2.0F / (double)z;
         double top = this.centreZ - (double)h / (double)2.0F / (double)z;
         int rx0 = Math.floorDiv((int)Math.floor(left), 512);
         int rz0 = Math.floorDiv((int)Math.floor(top), 512);
         int rx1 = Math.floorDiv((int)Math.floor(left + (double)((float)w / z)), 512);
         int rz1 = Math.floorDiv((int)Math.floor(top + (double)((float)h / z)), 512);

         while((long)(rx1 - rx0 + 1) * (long)(rz1 - rz0 + 1) > 48L) {
            if (rx1 - rx0 >= rz1 - rz0) {
               ++rx0;
               if (rx1 > rx0) {
                  --rx1;
               }
            } else {
               ++rz0;
               if (rz1 > rz0) {
                  --rz1;
               }
            }
         }

         boolean waiting = false;
         RenderSystem.enableBlend();
         g.setColor(1.0F, 1.0F, 1.0F, a);

         for(int rz = rz0; rz <= rz1; ++rz) {
            for(int rx = rx0; rx <= rx1; ++rx) {
               Textures.Loaded tex = map.tile(rx, rz);
               if (tex == null) {
                  waiting |= map.loading(rx, rz);
               } else {
                  int sx0 = x + (int)Math.round(((double)rx * (double)512.0F - left) * (double)z);
                  int sx1 = x + (int)Math.round(((double)(rx + 1) * (double)512.0F - left) * (double)z);
                  int sy0 = y + (int)Math.round(((double)rz * (double)512.0F - top) * (double)z);
                  int sy1 = y + (int)Math.round(((double)(rz + 1) * (double)512.0F - top) * (double)z);
                  AbstractTexture texture = Minecraft.getInstance().getTextureManager().getTexture(tex.id());
                  texture.setFilter(z < 1.0F, false);
                  g.blit(tex.id(), sx0, sy0, sx1 - sx0, sy1 - sy0, 0.0F, 0.0F, tex.width(), tex.height(), tex.width(), tex.height());
               }
            }
         }

         g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
         RenderSystem.disableBlend();
         if (waiting) {
            CFHGuiSpinner.draw(g, x + w - 18, y + 6, 12, Colors.scaleAlpha(Theme.current().accentInk(), a));
         }

      }
   }

   private void marks(GuiGraphics g, int x, int y, int w, int h, int mouseX, int mouseY, float a, boolean vanilla, Palette p) {
      float z = this.zoom.get();
      double left = this.centreX - (double)w / (double)2.0F / (double)z;
      double top = this.centreZ - (double)h / (double)2.0F / (double)z;
      g.enableScissor(x + 1, y + 1, x + w - 1, y + h - 1);
      Component tip = null;

      for(Waypoints.Waypoint wp : this.waypoints) {
         int sx = x + (int)Math.round(((double)wp.x() + (double)0.5F - left) * (double)z);
         int sy = y + (int)Math.round(((double)wp.z() + (double)0.5F - top) * (double)z);
         if (sx >= x - 6 && sy >= y - 6 && sx <= x + w + 6 && sy <= y + h + 6) {
            CFHGuiDraw.pixelCircle(g, sx, sy, 3, Colors.scaleAlpha(-15724528, a));
            CFHGuiDraw.pixelCircle(g, sx, sy, 2, Colors.scaleAlpha(wp.argb(), a));
            if (Math.abs(mouseX - sx) <= 4 && Math.abs(mouseY - sy) <= 4) {
               String var10000 = wp.name();
               tip = Component.literal(var10000 + "  " + wp.x() + ", " + wp.y() + ", " + wp.z());
            }
         }
      }

      if (this.hasSpawn) {
         int sx = x + (int)Math.round(((double)this.spawnX + (double)0.5F - left) * (double)z);
         int sy = y + (int)Math.round(((double)this.spawnZ + (double)0.5F - top) * (double)z);
         CFHGuiDraw.pixelCircle(g, sx, sy, 4, Colors.scaleAlpha(-15724528, a));
         CFHGuiDraw.pixelRing(g, sx, sy, 3, Colors.scaleAlpha(-1, a));
         CFHGuiDraw.pixelCircle(g, sx, sy, 1, Colors.scaleAlpha(-1750963, a));
         if (Math.abs(mouseX - sx) <= 5 && Math.abs(mouseY - sy) <= 5) {
            tip = Component.translatable("cfhgui.play.map.spawn", new Object[]{this.spawnX, this.spawnZ});
         }
      }

      if (this.hasPlayer) {
         int sx = x + (int)Math.round((this.playerX - left) * (double)z);
         int sy = y + (int)Math.round((this.playerZ - top) * (double)z);
         float pulse = Theme.current().motion() > 0.0F ? (float)(Clock.nowMs() % 1800L) / 1800.0F : 0.4F;
         CFHGuiDraw.pixelRing(g, sx, sy, 4 + Math.round(pulse * 5.0F), Colors.scaleAlpha(vanilla ? -1 : p.accent(), a * (1.0F - pulse) * 0.9F));
         CFHGuiDraw.pixelCircle(g, sx, sy, 3, Colors.scaleAlpha(-15724528, a));
         CFHGuiDraw.pixelCircle(g, sx, sy, 2, Colors.scaleAlpha(vanilla ? -1 : p.accent(), a));
         if (Math.abs(mouseX - sx) <= 5 && Math.abs(mouseY - sy) <= 5) {
            tip = Component.translatable("cfhgui.play.map.player", new Object[]{(int)Math.floor(this.playerX), (int)Math.floor(this.playerZ)});
         }
      }

      g.disableScissor();
      if (tip != null && this.isHovered()) {
         CFHGuiTooltips.request(tip, this);
      }

   }

   private void placeholder(GuiGraphics g, int x, int y, int w, int h, float a, boolean vanilla, Palette p) {
      Textures.Loaded pic = this.picture;
      int muted = Colors.scaleAlpha(vanilla ? -4144960 : p.textMuted(), a);
      int textY = y + h / 2 - 4;
      if (pic != null) {
         CFHGuiDraw.blitFit(g, pic.id(), x, y, w, h, pic.width(), pic.height(), true, a * 0.22F);
         int s = Math.min(48, h - 30);
         if (s >= 16) {
            CFHGuiDraw.blitScaled(g, pic.id(), x + (w - s) / 2, y + (h - s) / 2 - 8, s, s, pic.width(), pic.height(), a);
            textY = y + (h - s) / 2 - 8 + s + 5;
         }
      } else {
         Icons.draw(g, Icon.MAP, x + w / 2 - 8, y + h / 2 - 18, 16, muted);
         textY = y + h / 2 + 3;
      }

      for(FormattedCharSequence line : CFHGuiDraw.font().split(this.placeholder, w - 16)) {
         g.drawString(CFHGuiDraw.font(), line, x + (w - CFHGuiDraw.font().width(line)) / 2, textY, muted, vanilla);
         textY += 10;
      }

   }
}
