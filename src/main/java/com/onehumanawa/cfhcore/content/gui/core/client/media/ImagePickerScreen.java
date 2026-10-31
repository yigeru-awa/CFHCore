package com.onehumanawa.cfhcore.content.gui.core.client.media;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.core.media.FilePicker;
import com.onehumanawa.cfhcore.content.gui.core.screen.CFHGuiScreen;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiList;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSpinner;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public final class ImagePickerScreen extends CFHGuiScreen {
   private static final ResourceLocation PREVIEW_TEX = CFHGui.id("picker_preview");
   private static final String[] PLACES = new String[]{"Pictures", "Downloads", "Desktop", "Screenshots"};
   private static Path lastDir;
   private final Consumer<Path> onPick;
   private Path dir;
   private List<Path> entries = List.of();
   private @Nullable Path selected;
   private CFHGuiList<Path> list;
   private CFHGuiButton sendButton;
   private Rect previewRect = new Rect(0, 0, 0, 0);
   private volatile int previewW;
   private volatile int previewH;
   private volatile long previewBytes;
   private volatile boolean previewReady;
   private volatile boolean previewLoading;
   private int previewGeneration;

   public ImagePickerScreen(@Nullable Screen parent, Consumer<Path> onPick) {
      super(Component.translatable("cfhgui.picker.title"), parent);
      this.onPick = onPick;
   }

   protected void build() {
      if (this.dir == null) {
         this.dir = lastDir != null && Files.isDirectory(lastDir, new LinkOption[0]) ? lastDir : firstExisting();
      }

      Rect c = this.contentRect();
      int x = c.x();

      for(String name : PLACES) {
         Path target = FilePicker.place(name);
         int var10004 = c.y();
         String var10007 = name.toLowerCase(Locale.ROOT);
         CFHGuiButton b = (CFHGuiButton)this.add((new CFHGuiButton(x, var10004, 62, 16, Component.translatable("cfhgui.picker.place." + var10007), () -> this.navigate(target))).variant(CFHGuiButton.Variant.GHOST).icon(name.equals("Screenshots") ? Icon.CAMERA : Icon.FOLDER));
         b.active = target != null && Files.isDirectory(target, new LinkOption[0]);
         x += 64;
      }

      if (FilePicker.available()) {
         this.add((new CFHGuiButton(x + 4, c.y(), 76, 16, Component.translatable("cfhgui.picker.browse"), () -> FilePicker.pickImage((p) -> {
               if (Files.isRegularFile(p, new LinkOption[0])) {
                  this.pick(p);
               }

            }))).variant(CFHGuiButton.Variant.GHOST).icon(Icon.EXTERNAL));
      }

      int listTop = c.y() + 32;
      int listW = Math.max(120, c.w() / 2 - 6);
      this.list = (CFHGuiList)this.add(new CFHGuiList(c.x(), listTop, listW, c.bottom() - listTop - 26, 12, new RowRenderer()));
      this.list.gap(1).emptyText(Component.translatable("cfhgui.picker.empty"));
      this.list.onSelect((p) -> {
         if (p != null && !this.isUp(p) && !Files.isDirectory(p, new LinkOption[0])) {
            this.select(p);
         }

      });
      this.list.onActivate((p) -> {
         if (p != null) {
            if (this.isUp(p)) {
               this.navigate(this.dir.getParent());
            } else if (Files.isDirectory(p, new LinkOption[0])) {
               this.navigate(p);
            } else {
               this.pick(p);
            }

         }
      });
      this.previewRect = new Rect(c.x() + listW + 8, listTop, c.w() - listW - 8, c.bottom() - listTop - 26);
      this.sendButton = (CFHGuiButton)this.add((new CFHGuiButton(c.right() - 90, c.bottom() - 20, 90, 20, Component.translatable("cfhgui.picker.send"), () -> {
         if (this.selected != null) {
            this.pick(this.selected);
         }

      })).variant(CFHGuiButton.Variant.PRIMARY).icon(Icon.SEND));
      this.sendButton.active = this.selected != null;
      this.refresh();
   }

   private static Path firstExisting() {
      for(String name : PLACES) {
         Path p = FilePicker.place(name);
         if (p != null && Files.isDirectory(p, new LinkOption[0])) {
            return p;
         }
      }

      return Path.of(System.getProperty("user.home"));
   }

   private boolean isUp(Path p) {
      return !this.entries.isEmpty() && p == this.entries.get(0) && this.dir.getParent() != null && p.equals(this.dir.getParent());
   }

   private void navigate(@Nullable Path target) {
      if (target != null && Files.isDirectory(target, new LinkOption[0])) {
         this.dir = target;
         lastDir = target;
         this.selected = null;
         this.previewReady = false;
         if (this.sendButton != null) {
            this.sendButton.active = false;
         }

         this.refresh();
      }
   }

   private void refresh() {
      List<Path> dirs = new ArrayList();
      List<Path> files = new ArrayList();

      try {
         DirectoryStream<Path> stream = Files.newDirectoryStream(this.dir);

         try {
            for(Path p : stream) {
               String n = p.getFileName() == null ? "" : p.getFileName().toString();
               if (!n.startsWith(".")) {
                  if (Files.isDirectory(p, new LinkOption[0])) {
                     dirs.add(p);
                  } else if (n.toLowerCase(Locale.ROOT).matches(".*\\.(png|jpe?g|gif|webp)")) {
                     files.add(p);
                  }
               }
            }
         } catch (Throwable var8) {
            if (stream != null) {
               try {
                  stream.close();
               } catch (Throwable var7) {
                  var8.addSuppressed(var7);
               }
            }

            throw var8;
         }

         if (stream != null) {
            stream.close();
         }
      } catch (SecurityException | IOException e) {
         CFHGui.LOGGER.debug("[CFHGui] cannot list {}: {}", this.dir, ((Exception)e).toString());
      }

      dirs.sort(Comparator.comparing((px) -> px.getFileName().toString().toLowerCase(Locale.ROOT)));
      files.sort(Comparator.comparing((px) -> px.getFileName().toString().toLowerCase(Locale.ROOT)));
      List<Path> all = new ArrayList(dirs.size() + files.size() + 1);
      if (this.dir.getParent() != null) {
         all.add(this.dir.getParent());
      }

      all.addAll(dirs);
      all.addAll(files);
      this.entries = all;
      if (this.list != null) {
         this.list.items(all);
      }

   }

   private void select(Path p) {
      this.selected = p;
      this.previewReady = false;
      this.previewLoading = true;
      this.sendButton.active = true;
      int generation = ++this.previewGeneration;
      Thread t = new Thread(() -> {
         try {
            byte[] data = Files.readAllBytes(p);
            NativeImage img = NativeImage.read(new ByteArrayInputStream(data));
            Minecraft.getInstance().execute(() -> {
               if (generation != this.previewGeneration) {
                  img.close();
               } else {
                  Minecraft.getInstance().getTextureManager().release(PREVIEW_TEX);
                  DynamicTexture tex = new DynamicTexture(img);
                  tex.setFilter(true, false);
                  Minecraft.getInstance().getTextureManager().register(PREVIEW_TEX, tex);
                  this.previewW = img.getWidth();
                  this.previewH = img.getHeight();
                  this.previewBytes = (long)data.length;
                  this.previewReady = true;
                  this.previewLoading = false;
               }
            });
         } catch (Exception e) {
            CFHGui.LOGGER.debug("[CFHGui] preview failed for {}: {}", p, e.toString());
            this.previewLoading = false;
         }

      }, "cfhgui-preview");
      t.setDaemon(true);
      t.start();
   }

   private void pick(Path p) {
      this.back();
      if (this.onPick != null) {
         this.onPick.accept(p);
      }

   }

   protected void renderContent(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      Theme t = Theme.current();
      Palette p = t.palette();
      Rect c = this.contentRect();

      String path;
      for(path = this.dir.toString(); this.font.width(path) > c.w() - 4 && path.length() > 8; path = "..." + path.substring(4)) {
      }

      g.drawString(this.font, path, c.x(), c.y() + 20, p.textMuted(), t.isVanilla());
      Rect pr = this.previewRect;
      if (t.isVanilla()) {
         g.fill(pr.x(), pr.y(), pr.right(), pr.bottom(), Integer.MIN_VALUE);
         CFHGuiDraw.outline(g, pr.x(), pr.y(), pr.w(), pr.h(), -16777216, 0);
      } else {
         CFHGuiDraw.panel(g, pr.x(), pr.y(), pr.w(), pr.h(), t.panel(p.bg2()), p.border());
      }

      if (this.selected != null && this.previewReady && this.previewW > 0) {
         int[] s = MediaDraw.fit(this.previewW, this.previewH, pr.w() - 8, pr.h() - 20, false);
         RenderSystem.enableBlend();
         g.blit(PREVIEW_TEX, pr.x() + (pr.w() - s[0]) / 2, pr.y() + 4 + (pr.h() - 20 - s[1]) / 2, s[0], s[1], 0.0F, 0.0F, this.previewW, this.previewH, this.previewW, this.previewH);
         RenderSystem.disableBlend();
         String info = "%dx%d  %d KB".formatted(this.previewW, this.previewH, this.previewBytes / 1024L);
         g.drawString(this.font, info, pr.x() + 4, pr.bottom() - 12, p.textMuted(), t.isVanilla());
         String name = this.selected.getFileName() == null ? "" : this.selected.getFileName().toString();
         g.drawString(this.font, CFHGuiDraw.truncate(Component.literal(name), pr.w() - this.font.width(info) - 12), pr.x() + 8 + this.font.width(info), pr.bottom() - 12, p.textDim(), t.isVanilla());
      } else if (this.selected != null && this.previewLoading) {
         CFHGuiSpinner.draw(g, pr.centerX() - 6, pr.centerY() - 6, 12, t.isVanilla() ? -1 : p.accentInk());
      } else {
         CFHGuiDraw.textCentered(g, (Component)Component.translatable(this.selected == null ? "cfhgui.picker.hint" : "cfhgui.media.failed"), pr.centerX(), pr.centerY() - 4, p.textDim());
      }

   }

   public void removed() {
      Minecraft.getInstance().getTextureManager().release(PREVIEW_TEX);
      super.removed();
   }

   public boolean isPauseScreen() {
      return false;
   }

   private final class RowRenderer implements CFHGuiList.RowRenderer<Path> {
      public void render(GuiGraphics g, Path item, int index, int x, int y, int w, int h, boolean hovered, boolean selectedRow, int mouseX, int mouseY) {
         Theme t = Theme.current();
         Palette p = t.palette();
         boolean up = ImagePickerScreen.this.isUp(item);
         boolean isDir = up || Files.isDirectory(item, new LinkOption[0]);
         Icon icon = up ? Icon.ARROW_UP : (isDir ? Icon.FOLDER : Icon.IMAGE);
         int fg = isDir ? (t.isVanilla() ? -8064 : p.warning()) : (!hovered && !selectedRow ? p.textMuted() : p.text());
         Icons.draw(g, icon, x + 3, y + 2, 8, Colors.withAlpha(fg, 208));
         String label = up ? ".." : (item.getFileName() == null ? item.toString() : item.getFileName().toString());
         g.drawString(ImagePickerScreen.this.font, CFHGuiDraw.truncate(Component.literal(label), w - 18), x + 14, y + 2, fg, t.isVanilla());
      }
   }
}
