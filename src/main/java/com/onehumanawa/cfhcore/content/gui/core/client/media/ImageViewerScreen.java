package com.onehumanawa.cfhcore.content.gui.core.client.media;

import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Clock;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.media.Attachment;
import com.onehumanawa.cfhcore.content.gui.core.media.ClipboardImages;
import com.onehumanawa.cfhcore.content.gui.core.media.MediaCache;
import com.onehumanawa.cfhcore.content.gui.core.platform.CFHGuiPlatform;
import com.onehumanawa.cfhcore.content.gui.core.screen.CFHGuiScreen;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiIconButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiToasts;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.List;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

public final class ImageViewerScreen extends CFHGuiScreen {
   private static final int OPEN_MS = 160;
   private static final int CLOSE_MS = 130;
   private final List<Attachment> gallery;
   private int index;
   private final long openedAt;
   private long lastFrameNanos;
   private long closingAt;
   private float zoom;
   private float zoomTarget;
   private float panX;
   private float panY;
   private float panTargetX;
   private float panTargetY;
   private int imgX0;
   private int imgY0;
   private int imgX1;
   private int imgY1;
   private boolean fitted;
   private CFHGuiIconButton prev;
   private CFHGuiIconButton next;

   public ImageViewerScreen(@Nullable Screen parent, Attachment attachment) {
      this(parent, List.of(attachment), 0);
   }

   public ImageViewerScreen(@Nullable Screen parent, List<Attachment> gallery, int index) {
      super(Component.translatable("cfhgui.viewer.title"), parent);
      this.openedAt = Clock.nowMs();
      this.lastFrameNanos = System.nanoTime();
      this.zoom = 1.0F;
      this.zoomTarget = 1.0F;
      this.gallery = gallery.isEmpty() ? List.of() : List.copyOf(gallery);
      this.index = Math.max(0, Math.min(gallery.size() - 1, index));
      this.showHeader = false;
      this.showBack = false;
   }

   private @Nullable Attachment current() {
      return this.gallery.isEmpty() ? null : (Attachment)this.gallery.get(this.index);
   }

   protected void build() {
      int size = 20;
      int x = this.width - 6 - 20;
      this.add(new CFHGuiIconButton(x, 6, 20, Icon.CLOSE, Component.translatable("gui.back"), this::onClose));
      x -= 22;
      Attachment att = this.current();
      if (att != null && att.url() != null) {
         this.add(new CFHGuiIconButton(x, 6, 20, Icon.LINK, Component.translatable("cfhgui.viewer.copy_link"), this::copyLink));
         x -= 22;
         this.add(new CFHGuiIconButton(x, 6, 20, Icon.EXTERNAL, Component.translatable("cfhgui.viewer.open_link"), this::openLink));
         x -= 22;
      }

      this.add(new CFHGuiIconButton(x, 6, 20, Icon.SAVE, Component.translatable("cfhgui.viewer.save"), this::save));
      x -= 22;
      this.add(new CFHGuiIconButton(x, 6, 20, Icon.REFRESH, Component.translatable("cfhgui.viewer.reset"), this::resetView));
      if (this.gallery.size() > 1) {
         this.prev = (CFHGuiIconButton)this.add(new CFHGuiIconButton(6, this.height / 2 - 10, 20, Icon.CHEVRON_LEFT, Component.translatable("cfhgui.viewer.prev"), () -> this.step(-1)));
         this.next = (CFHGuiIconButton)this.add(new CFHGuiIconButton(this.width - 6 - 20, this.height / 2 - 10, 20, Icon.CHEVRON_RIGHT, Component.translatable("cfhgui.viewer.next"), () -> this.step(1)));
      }

      this.fitted = false;
   }

   private void step(int d) {
      if (this.gallery.size() > 1) {
         this.index = ((this.index + d) % this.gallery.size() + this.gallery.size()) % this.gallery.size();
         this.fitted = false;
         this.panTargetX = this.panTargetY = this.panX = this.panY = 0.0F;
         this.rebuildWidgets();
      }
   }

   private void resetView() {
      this.fitted = false;
      this.panTargetX = this.panTargetY = this.panX = this.panY = 0.0F;
   }

   private void fitToScreen(MediaCache.Entry e) {
      if (e.width > 0 && e.height > 0) {
         this.zoomTarget = Math.min(1.0F, Math.min(((float)this.width - 40.0F) / (float)e.width, ((float)this.height - 40.0F) / (float)e.height));
         this.zoom = this.zoomTarget;
         this.fitted = true;
      }

   }

   private void copyLink() {
      Attachment att = this.current();
      if (att != null && att.url() != null) {
         Minecraft.getInstance().keyboardHandler.setClipboard(att.url());
         CFHGuiToasts.show(Component.translatable("cfhgui.copied"), (Component)null, Icon.COPY);
      }
   }

   private void openLink() {
      Attachment att = this.current();
      if (att != null && att.url() != null) {
         try {
            CFHGuiPlatform.get().openUri(URI.create(att.url()));
         } catch (Exception e) {
            CFHGuiToasts.show(Component.translatable("cfhgui.media.failed"), Component.literal(e.getMessage() == null ? "" : e.getMessage()), Icon.WARNING);
         }

      }
   }

   private void save() {
      Attachment att = this.current();
      if (att != null) {
         MediaCache.Entry e = MediaCache.get(att);
         byte[] bytes = e.bytes;
         if (bytes == null) {
            CFHGuiToasts.show(Component.translatable("cfhgui.viewer.save_failed"), Component.translatable("cfhgui.media.loading"), Icon.WARNING);
         } else {
            Path dir = Minecraft.getInstance().gameDirectory.toPath().resolve("screenshots");
            String ext = ClipboardImages.extensionFor(bytes);
            String var10001 = att.id();
            Path target = dir.resolve("cfhgui-" + var10001 + "." + ext);
            Thread t = new Thread(() -> {
               try {
                  Files.createDirectories(dir);
                  Files.write(target, bytes, new OpenOption[0]);
                  Minecraft.getInstance().execute(() -> CFHGuiToasts.show(Component.translatable("cfhgui.viewer.saved"), Component.literal(target.getFileName().toString()), Icon.SAVE, () -> Util.getPlatform().openPath(dir)));
               } catch (Exception ex) {
                  CFHGui.LOGGER.warn("[CFHGui] could not save image: {}", ex.toString());
                  Minecraft.getInstance().execute(() -> CFHGuiToasts.show(Component.translatable("cfhgui.viewer.save_failed"), Component.literal(String.valueOf(ex.getMessage())), Icon.WARNING));
               }

            }, "cfhgui-save-image");
            t.setDaemon(true);
            t.start();
         }
      }
   }

   public void onClose() {
      if (this.closingAt == 0L) {
         this.closingAt = Clock.nowMs();
      }

   }

   public void back() {
      this.onClose();
   }

   private float appear() {
      if (Theme.current().motion() <= 0.0F) {
         return this.closingAt > 0L ? 0.0F : 1.0F;
      } else if (this.closingAt > 0L) {
         float t = Mth.clamp((float)(Clock.nowMs() - this.closingAt) / (float)Theme.current().ms(130), 0.0F, 1.0F);
         return 1.0F - t * t;
      } else {
         float t = Mth.clamp((float)(Clock.nowMs() - this.openedAt) / (float)Theme.current().ms(160), 0.0F, 1.0F);
         return 1.0F - (1.0F - t) * (1.0F - t);
      }
   }

   public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      float a = this.appear();
      g.fill(0, 0, this.width, this.height, Colors.withAlpha(Theme.current().isVanilla() ? 0 : 657930, Math.round(228.0F * a)));
   }

   public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      long now = System.nanoTime();
      float dt = Math.min((float)(now - this.lastFrameNanos) / 1.0E9F, 0.05F);
      this.lastFrameNanos = now;
      float chase = Theme.current().motion() <= 0.0F ? 1.0F : 1.0F - (float)Math.pow(5.0E-4, (double)dt);
      this.zoom += (this.zoomTarget - this.zoom) * chase;
      this.panX += (this.panTargetX - this.panX) * chase;
      this.panY += (this.panTargetY - this.panY) * chase;
      float appear = this.appear();
      if (this.closingAt > 0L && appear <= 0.001F) {
         this.minecraft.setScreen(this.parent);
      } else {
         this.renderBackground(g, mouseX, mouseY, partialTick);
         Attachment att = this.current();
         MediaCache.Entry e = att == null ? null : MediaCache.get(att);
         Palette p = Theme.current().palette();
         if (e != null && e.ready() && !e.frames.isEmpty()) {
            if (!this.fitted) {
               this.fitToScreen(e);
            }

            float appearScale = 0.88F + 0.12F * appear;
            int w = Math.max(1, Math.round((float)e.width * this.zoom * appearScale));
            int h = Math.max(1, Math.round((float)e.height * this.zoom * appearScale));
            this.imgX0 = Math.round((float)this.width / 2.0F - (float)w / 2.0F - this.panX * this.zoom * appearScale);
            this.imgY0 = Math.round((float)this.height / 2.0F - (float)h / 2.0F - this.panY * this.zoom * appearScale);
            this.imgX1 = this.imgX0 + w;
            this.imgY1 = this.imgY0 + h;
            MediaDraw.drawFrame(g, e, this.imgX0, this.imgY0, w, h, appear);
            if (this.closingAt == 0L) {
               String who = e.sender.isEmpty() ? "" : e.sender + "  -  ";
               String info = "%s%dx%d  %.0f%%%s".formatted(who, e.width, e.height, this.zoom * 100.0F, this.gallery.size() > 1 ? "  (%d/%d)".formatted(this.index + 1, this.gallery.size()) : "");
               g.drawString(this.font, info, 6, this.height - 14, Colors.withAlpha(p.textMuted(), Math.round(255.0F * appear)), Theme.current().isVanilla());
            }
         } else {
            if (e != null) {
               MediaDraw.drawPlaceholder(g, e, this.width / 2 - 20, this.height / 2 - 20, 40, 40, appear);
            }

            if (e != null && e.status == MediaCache.Status.FAILED) {
               CFHGuiDraw.textCentered(g, MediaDraw.statusText(e), this.width / 2, this.height / 2 + 14, p.textMuted());
            }
         }

         for(Renderable r : this.renderableList()) {
            if (r instanceof AbstractWidget) {
               AbstractWidget wd = (AbstractWidget)r;
               wd.setAlpha(appear);
            }

            r.render(g, mouseX, mouseY, partialTick);
         }

      }
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (this.closingAt > 0L) {
         return true;
      } else if (super.mouseClicked(mouseX, mouseY, button)) {
         return true;
      } else {
         boolean onImage = mouseX >= (double)this.imgX0 && mouseX < (double)this.imgX1 && mouseY >= (double)this.imgY0 && mouseY < (double)this.imgY1;
         if (!onImage && button == 0) {
            this.onClose();
            return true;
         } else {
            return onImage;
         }
      }
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double dx, double dy) {
      if (this.closingAt > 0L) {
         return true;
      } else {
         float oldTarget = this.zoomTarget;
         this.zoomTarget = Mth.clamp(this.zoomTarget * (dy > (double)0.0F ? 1.25F : 0.8F), 0.05F, 16.0F);
         float ix = (float)((mouseX - (double)((float)this.width / 2.0F)) / (double)oldTarget + (double)this.panTargetX);
         float iy = (float)((mouseY - (double)((float)this.height / 2.0F)) / (double)oldTarget + (double)this.panTargetY);
         this.panTargetX = ix - (float)(mouseX - (double)((float)this.width / 2.0F)) / this.zoomTarget;
         this.panTargetY = iy - (float)(mouseY - (double)((float)this.height / 2.0F)) / this.zoomTarget;
         return true;
      }
   }

   public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
      if (this.closingAt > 0L) {
         return true;
      } else {
         this.panTargetX -= (float)(dragX / (double)Math.max(0.01F, this.zoom));
         this.panTargetY -= (float)(dragY / (double)Math.max(0.01F, this.zoom));
         this.panX = this.panTargetX;
         this.panY = this.panTargetY;
         return true;
      }
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      switch (keyCode) {
         case 45:
         case 333:
            this.zoomTarget = Mth.clamp(this.zoomTarget / 1.25F, 0.05F, 16.0F);
            return true;
         case 48:
         case 320:
            this.resetView();
            return true;
         case 61:
         case 334:
            this.zoomTarget = Mth.clamp(this.zoomTarget * 1.25F, 0.05F, 16.0F);
            return true;
         case 83:
            if (Screen.hasControlDown()) {
               this.save();
               return true;
            }
         default:
            return super.keyPressed(keyCode, scanCode, modifiers);
         case 262:
            this.step(1);
            return true;
         case 263:
            this.step(-1);
            return true;
      }
   }

   public boolean isPauseScreen() {
      return false;
   }
}
