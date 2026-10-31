package com.onehumanawa.cfhcore.content.gui.menu.client.screenshots;

import com.mojang.blaze3d.systems.RenderSystem;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Textures;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.core.screen.CFHGuiScreen;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiIconButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiModal;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSpinner;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiToasts;
import com.onehumanawa.cfhcore.content.gui.menu.api.ScreenshotShareProvider;
import com.onehumanawa.cfhcore.content.gui.menu.api.CFHGuiMenuApi;
import com.onehumanawa.cfhcore.content.gui.menu.client.Fmt;
import java.nio.file.Path;
import java.util.List;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

public final class ScreenshotViewer extends CFHGuiScreen {
   private final List<Screenshots.Shot> shots;
   private int index;
   private Textures.@Nullable Loaded image;
   private boolean loading;
   private boolean fit = true;
   private float zoom = 1.0F;
   private double panX;
   private double panY;
   private boolean dragging;
   private final Anim fade;
   private @Nullable CFHGuiIconButton fitButton;

   public ScreenshotViewer(@Nullable Screen parent, List<Screenshots.Shot> shots, int index) {
      super(Component.translatable("cfhgui.screenshots.title"), parent);
      this.fade = new Anim(0.0F, 200, Ease.OUT_CUBIC);
      this.shots = shots;
      this.index = Math.max(0, Math.min(shots.size() - 1, index));
      this.maxContentWidth = 0;
   }

   private Screenshots.@Nullable Shot current() {
      return this.shots.isEmpty() ? null : (Screenshots.Shot)this.shots.get(this.index);
   }

   public Component getTitle() {
      Screenshots.Shot s = this.current();
      return (Component)(s == null ? super.getTitle() : Component.literal(s.fileName()));
   }

   protected void build() {
      this.addHeaderAction(new CFHGuiIconButton(0, 0, 20, Icon.CHEVRON_RIGHT, Component.translatable("cfhgui.screenshots.next"), () -> this.step(1)));
      this.addHeaderAction(new CFHGuiIconButton(0, 0, 20, Icon.CHEVRON_LEFT, Component.translatable("cfhgui.screenshots.prev"), () -> this.step(-1)));
      this.fitButton = (CFHGuiIconButton)this.addHeaderAction(new CFHGuiIconButton(0, 0, 20, this.fit ? Icon.FULLSCREEN : Icon.WINDOWED, Component.translatable(this.fit ? "cfhgui.screenshots.actual_size" : "cfhgui.screenshots.fit"), this::toggleFit));
      this.addHeaderAction(new CFHGuiIconButton(0, 0, 20, Icon.TRASH, Component.translatable("cfhgui.screenshots.delete"), this::confirmDelete));
      this.addHeaderAction(new CFHGuiIconButton(0, 0, 20, Icon.EDIT, Component.translatable("cfhgui.screenshots.rename"), this::rename));
      this.addHeaderAction(new CFHGuiIconButton(0, 0, 20, Icon.COPY, Component.translatable("cfhgui.screenshots.copy"), () -> {
         Screenshots.Shot s = this.current();
         if (s != null) {
            CFHGuiScreenshotsScreen.copy(s);
         }

      }));
      ScreenshotShareProvider share = CFHGuiMenuApi.screenshotShareProvider();
      if (share != null) {
         this.addHeaderAction(new CFHGuiIconButton(0, 0, 20, Icon.SEND, Component.translatable("cfhgui.screenshots.share"), () -> {
            Screenshots.Shot s = this.current();
            if (s != null) {
               share.share(s.path());
            }

         }));
      }

      this.addHeaderAction(new CFHGuiIconButton(0, 0, 20, Icon.FOLDER, Component.translatable("cfhgui.screenshots.open_folder"), () -> Util.getPlatform().openPath(Screenshots.dir())));
      if (this.image == null && !this.loading) {
         this.loadImage();
      }

   }

   private void loadImage() {
      Screenshots.Shot s = this.current();
      this.image = null;
      if (s != null) {
         this.loading = true;
         this.fade.snap(0.0F);
         Textures.load(s.path(), (l) -> {
            if (this.current() == s) {
               this.image = l;
               this.loading = false;
               this.fade.set(1.0F);
            }
         });
      }
   }

   private void step(int dir) {
      if (!this.shots.isEmpty()) {
         this.index = ((this.index + dir) % this.shots.size() + this.shots.size()) % this.shots.size();
         this.panX = this.panY = (double)0.0F;
         this.fit = true;
         this.zoom = 1.0F;
         this.loading = false;
         this.loadImage();
         if (this.fitButton != null) {
            this.fitButton.setIcon(Icon.FULLSCREEN);
            this.fitButton.tip(Component.translatable("cfhgui.screenshots.actual_size"));
         }

      }
   }

   private void toggleFit() {
      if (this.fit) {
         this.fit = false;
         this.zoom = 1.0F;
         this.panX = this.panY = (double)0.0F;
      } else {
         this.fit = true;
         this.panX = this.panY = (double)0.0F;
      }

      if (this.fitButton != null) {
         this.fitButton.setIcon(this.fit ? Icon.FULLSCREEN : Icon.WINDOWED);
         this.fitButton.tip(Component.translatable(this.fit ? "cfhgui.screenshots.actual_size" : "cfhgui.screenshots.fit"));
      }

   }

   private void rename() {
      Screenshots.Shot s = this.current();
      if (s != null) {
         CFHGuiModal.prompt(Component.translatable("cfhgui.screenshots.rename"), Component.translatable("cfhgui.screenshots.rename_body"), s.name(), (name) -> {
            if (!name.isBlank() && !name.trim().equals(s.name())) {
               if (Screenshots.rename(s, name)) {
                  String file = s.fileName();
                  int dot = file.lastIndexOf(46);
                  String ext = dot > 0 ? file.substring(dot) : ".png";
                  Path var10000 = s.path();
                  String var10001 = name.trim();
                  Path np = var10000.resolveSibling(var10001 + ext);
                  this.shots.set(this.index, new Screenshots.Shot(np, name.trim(), name.trim() + ext, s.modified(), s.size()));
                  this.loadImage();
               } else {
                  CFHGuiToasts.show(Component.translatable("cfhgui.screenshots.rename_failed"), Component.literal(name), Icon.WARNING);
               }

            }
         });
      }
   }

   private void confirmDelete() {
      Screenshots.Shot s = this.current();
      if (s != null) {
         CFHGuiModal.confirmDanger(Component.translatable("cfhgui.screenshots.delete"), Component.translatable("cfhgui.screenshots.delete_body", new Object[]{s.fileName()}), Component.translatable("cfhgui.screenshots.delete"), () -> {
            if (Screenshots.delete(s)) {
               CFHGuiToasts.show(Component.translatable("cfhgui.screenshots.deleted"), Component.literal(s.fileName()), Icon.TRASH);
               this.shots.remove(this.index);
               if (this.shots.isEmpty()) {
                  this.back();
               } else {
                  this.index = Math.min(this.index, this.shots.size() - 1);
                  this.image = null;
                  this.loadImage();
               }
            }
         });
      }
   }

   private float scale(Rect area) {
      if (this.image == null) {
         return 1.0F;
      } else {
         return !this.fit ? this.zoom : Math.min(1.0F, Math.min((float)area.w() / (float)this.image.width(), (float)area.h() / (float)this.image.height()));
      }
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
      if (super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
         return true;
      } else {
         Rect area = this.contentRect();
         if (this.image != null && area.contains(mouseX, mouseY)) {
            float old = this.scale(area);
            float target = Mth.clamp(old * (scrollY > (double)0.0F ? 1.2F : 0.8333333F), 0.05F, 12.0F);
            double cx = (double)area.centerX() + this.panX;
            double cy = (double)area.centerY() + this.panY;
            double dx = mouseX - cx;
            double dy = mouseY - cy;
            this.panX -= dx * (double)(target / old - 1.0F);
            this.panY -= dy * (double)(target / old - 1.0F);
            this.fit = false;
            this.zoom = target;
            if (this.fitButton != null) {
               this.fitButton.setIcon(Icon.WINDOWED);
               this.fitButton.tip(Component.translatable("cfhgui.screenshots.fit"));
            }

            return true;
         } else {
            return false;
         }
      }
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (super.mouseClicked(mouseX, mouseY, button)) {
         return true;
      } else if (button == 0 && this.contentRect().contains(mouseX, mouseY)) {
         this.dragging = true;
         return true;
      } else {
         return false;
      }
   }

   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      this.dragging = false;
      return super.mouseReleased(mouseX, mouseY, button);
   }

   public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
      if (this.dragging && button == 0) {
         this.panX += dragX;
         this.panY += dragY;
         return true;
      } else {
         return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
      }
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (super.keyPressed(keyCode, scanCode, modifiers)) {
         return true;
      } else {
         switch (keyCode) {
            case 32:
            case 262:
            case 267:
               this.step(1);
               return true;
            case 45:
            case 333:
               this.fit = false;
               this.zoom = Mth.clamp(this.scale(this.contentRect()) / 1.2F, 0.05F, 12.0F);
               return true;
            case 48:
            case 320:
               this.fit = true;
               this.panX = this.panY = (double)0.0F;
               return true;
            case 49:
            case 321:
               this.fit = false;
               this.zoom = 1.0F;
               this.panX = this.panY = (double)0.0F;
               return true;
            case 61:
            case 334:
               this.fit = false;
               this.zoom = Mth.clamp(this.scale(this.contentRect()) * 1.2F, 0.05F, 12.0F);
               return true;
            case 67:
               if (hasControlDown()) {
                  Screenshots.Shot s = this.current();
                  if (s != null) {
                     CFHGuiScreenshotsScreen.copy(s);
                  }

                  return true;
               }
            default:
               return false;
            case 261:
               this.confirmDelete();
               return true;
            case 263:
            case 266:
               this.step(-1);
               return true;
            case 291:
               this.rename();
               return true;
         }
      }
   }

   public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      super.renderBackground(g, mouseX, mouseY, partialTick);
      if (!Theme.current().isVanilla()) {
         g.fill(0, 32, this.width, this.height, Integer.MIN_VALUE);
      }

   }

   protected void renderContent(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      Theme t = Theme.current();
      Palette p = t.palette();
      Rect area = this.contentRect();
      Screenshots.Shot s = this.current();
      if (s == null) {
         CFHGuiDraw.textCentered(g, Component.translatable("cfhgui.screenshots.empty"), area.centerX(), area.centerY(), p.textMuted());
      } else if (this.image == null) {
         CFHGuiSpinner.draw(g, area.centerX() - 8, area.centerY() - 8, 16, t.isVanilla() ? -1 : p.accentInk());
      } else {
         float sc = this.scale(area);
         int dw = Math.max(1, Math.round((float)this.image.width() * sc));
         int dh = Math.max(1, Math.round((float)this.image.height() * sc));
         float a = this.fade.get();
         CFHGuiDraw.scissor(g, area.x(), area.y(), area.w(), area.h());
         g.pose().pushPose();
         g.pose().translate((double)area.centerX() + this.panX, (double)area.centerY() + this.panY, (double)0.0F);
         g.pose().scale(sc, sc, 1.0F);
         RenderSystem.enableBlend();
         g.setColor(1.0F, 1.0F, 1.0F, a);
         g.blit(this.image.id(), -this.image.width() / 2, -this.image.height() / 2, this.image.width(), this.image.height(), 0.0F, 0.0F, this.image.width(), this.image.height(), this.image.width(), this.image.height());
         g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
         RenderSystem.disableBlend();
         g.pose().popPose();
         CFHGuiDraw.unscissor(g);
         int var10000 = this.index + 1;
         Component info = Component.literal(var10000 + " / " + this.shots.size() + " · " + this.image.width() + "×" + this.image.height() + " · " + Fmt.bytes(s.size()) + " · " + Math.round(sc * 100.0F) + "%" + (this.fit ? " (" + Component.translatable("cfhgui.screenshots.fit").getString() + ")" : ""));
         int iw = this.font.width(info) + 12;
         int ix = area.centerX() - iw / 2;
         int iy = area.bottom() - 16;
         CFHGuiDraw.pixelRound(g, ix, iy, iw, 14, Colors.withAlpha(t.isVanilla() ? 0 : p.bg(), 192), t.radius());
         g.drawString(this.font, info, ix + 6, iy + 3, t.isVanilla() ? -1 : p.textMuted(), t.isVanilla());
         if (dw >= 8 && dh < 8) {
         }

      }
   }
}
