package com.onehumanawa.cfhcore.content.gui.menu.client.overhaul.title;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Clock;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Fonts;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.platform.CFHGuiPlatform;
import com.onehumanawa.cfhcore.content.gui.core.stage.builds.BuildSpec;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiCard;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiIconButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton.Variant;
import com.onehumanawa.cfhcore.content.gui.menu.client.overhaul.title.set.TitleScene;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;

final class SceneFetchCard extends CFHGuiCard {
   static final int WIDTH = 196;
   static final int MIN_WIDTH = 132;
   static final int PAD = 7;
   private static final int BUTTON_H = 18;
   private static final int GAP = 4;
   private static final int LINE = 10;
   private final TitleScene scene;
   private final CFHGuiButton get;
   private final CFHGuiButton folder;
   private final CFHGuiIconButton folderSign;
   private final Component getFull;
   private final Component getBrief;
   private final @Nullable String page;
   private List<FormattedCharSequence> title = List.of();
   private List<FormattedCharSequence> body = List.of();
   private List<FormattedCharSequence> foot = List.of();
   private boolean watching;
   private boolean row;
   private @Nullable String problem;
   private final int maxHeight;
   private long checkedMs;

   SceneFetchCard(int x, int y, int width, int maxHeight, TitleScene scene) {
      super(x, y, width, 60);
      this.scene = scene;
      this.maxHeight = maxHeight;
      this.flat();
      BuildSpec b = scene.build();
      this.page = b != null && b.download() != null ? b.download().page() : null;
      this.getFull = Component.translatable("cfhgui.overhaul.fetch.get", new Object[]{host(this.page)});
      this.getBrief = Component.literal(host(this.page));
      this.get = (new CFHGuiButton(0, 0, width - 14, 18, this.getFull, this::openPage)).variant(Variant.PRIMARY).icon(Icon.EXTERNAL);
      if (this.page != null) {
         this.get.tip(Component.literal(this.page));
      }

      Component open = Component.translatable("cfhgui.overhaul.fetch.folder");
      Component where = Component.literal(BuildSpec.configDir().toAbsolutePath().toString());
      this.folder = (new CFHGuiButton(0, 0, width - 14, 18, fit(open, Component.translatable("cfhgui.overhaul.fetch.folder.short"), width - 14), SceneFetchCard::openFolder)).icon(Icon.FOLDER).tip(where);
      this.folderSign = (new CFHGuiIconButton(0, 0, 18, Icon.FOLDER, open, SceneFetchCard::openFolder)).variant(Variant.SECONDARY).tip(List.of(open, where.copy().withStyle(ChatFormatting.GRAY)));
      this.get.active = this.page != null;
      this.layoutText();
   }

   private static Component fit(Component label, Component brief, int buttonWidth) {
      return Minecraft.getInstance().font.width(label) <= buttonWidth - 12 - 16 ? label : brief;
   }

   TitleScene scene() {
      return this.scene;
   }

   static String host(@Nullable String url) {
      if (url == null) {
         return "?";
      } else {
         try {
            String h = URI.create(url).getHost();
            return h == null ? url : (h.startsWith("www.") ? h.substring(4) : h);
         } catch (RuntimeException var2) {
            return url;
         }
      }
   }

   private void openPage() {
      if (this.page != null) {
         try {
            CFHGuiPlatform.get().openUri(URI.create(this.page));
         } catch (RuntimeException var2) {
            Util.getPlatform().openUri(this.page);
         }

         SceneFetch.pageOpened();
         this.layoutText();
      }
   }

   private static void openFolder() {
      Path dir = BuildSpec.configDir();

      try {
         Files.createDirectories(dir);
      } catch (IOException var2) {
      }

      Util.getPlatform().openPath(dir);
   }

   private void layoutText() {
      Font font = Minecraft.getInstance().font;
      int w = this.getWidth() - 14;
      this.problem = this.scene.problem();
      this.watching = SceneFetch.watchingDownloads();
      BuildSpec b = this.scene.build();
      String author = b != null && b.credit() != null ? b.credit().author() : "?";
      Component head = this.problem != null ? Component.translatable("cfhgui.overhaul.fetch.broken", new Object[]{this.scene.title()}) : Component.translatable("cfhgui.overhaul.fetch.title", new Object[]{this.scene.title()});
      this.title = font.split(Fonts.heading(head), w - 14);
      Component words;
      if (this.problem != null) {
         BuildSpec.Found found = b == null ? null : b.find();
         String file = found != null && found.path() != null ? found.path().getFileName().toString() : (b == null ? "?" : b.fileName("nbt"));
         words = Component.translatable("cfhgui.overhaul.fetch.broken.body", new Object[]{file, this.problem});
      } else {
         words = Component.translatable("cfhgui.overhaul.fetch.body", new Object[]{author, host(this.page)});
      }

      List<FormattedCharSequence> said = font.split(words, w);
      List<FormattedCharSequence> line = font.split(Component.translatable(this.watching ? "cfhgui.overhaul.fetch.watching" : "cfhgui.overhaul.fetch.auto"), w - 10);
      boolean keep = this.problem != null;
      Fit[] tries = new Fit[]{new Fit(true, false, true), new Fit(keep, false, true), new Fit(keep, true, true), new Fit(keep, false, false), new Fit(keep, true, false)};
      Fit chosen = tries[tries.length - 1];

      for(Fit t : tries) {
         if (this.heightWith(t.words() ? said : List.of(), t.row(), t.line() ? line : List.of()) <= this.maxHeight) {
            chosen = t;
            break;
         }
      }

      this.body = chosen.words() ? said : List.of();
      this.foot = chosen.line() ? line : List.of();
      this.row = chosen.row();
      this.clearChildren();
      int y = 7 + this.title.size() * 10 + 3 + this.body.size() * 10 + 6;
      int getW = this.row ? w - 18 - 4 : w;
      this.get.setWidth(getW);
      this.get.setMessage(fit(this.getFull, this.getBrief, getW));
      this.add(this.get, 7, y);
      if (this.row) {
         this.add(this.folderSign, 7 + w - 18, y);
      } else {
         y += 22;
         this.add(this.folder, 7, y);
      }

      y += 24;
      this.setHeight(y + this.foot.size() * 10 + 7 - 2);
   }

   private int heightWith(List<FormattedCharSequence> words, boolean row, List<FormattedCharSequence> line) {
      int buttons = row ? 18 : 40;
      return 7 + this.title.size() * 10 + 3 + words.size() * 10 + 6 + buttons + 6 + line.size() * 10 + 7 - 2;
   }

   protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      long now = Clock.nowMs();
      if (now - this.checkedMs > 500L) {
         this.checkedMs = now;
         if (this.watching != SceneFetch.watchingDownloads() || !Objects.equals(this.problem, this.scene.problem())) {
            this.layoutText();
         }
      }

      SceneFetch.asking();
      super.renderWidget(g, mouseX, mouseY, partialTick);
   }

   protected void renderContent(GuiGraphics g, int x, int y, int w, int h, int mouseX, int mouseY, float partialTick) {
      Theme t = Theme.current();
      Palette p = t.palette();
      boolean vanilla = t.isVanilla();
      float a = this.alpha * this.enterProgress();
      Font font = Minecraft.getInstance().font;
      int ink = vanilla ? -1 : p.text();
      int muted = vanilla ? -3618616 : p.textMuted();
      int dim = vanilla ? -6250336 : p.textDim();
      int mark = this.problem != null ? (vanilla ? -40864 : p.warning()) : (vanilla ? -96 : t.accentInk());
      int ty = y + 7;
      Icons.draw(g, this.problem != null ? Icon.WARNING : Icon.DOWNLOAD, x + 7, ty, 8, Colors.scaleAlpha(mark, a));

      for(FormattedCharSequence line : this.title) {
         g.drawString(font, line, x + 7 + 12, ty, Colors.scaleAlpha(ink, a), vanilla);
         ty += 10;
      }

      ty += 3;

      for(FormattedCharSequence line : this.body) {
         g.drawString(font, line, x + 7, ty, Colors.scaleAlpha(muted, a), vanilla);
         ty += 10;
      }

      int fy = y + h - 7 + 2 - this.foot.size() * 10;
      if (this.watching) {
         float pulse = 0.45F + 0.55F * (0.5F + 0.5F * (float)Math.sin((double)Clock.nowMs() / (double)260.0F));
         CFHGuiDraw.rect(g, x + 7 + 1, fy + 3, 3, 3, Colors.scaleAlpha(mark, a * pulse));
      } else {
         CFHGuiDraw.rect(g, x + 7 + 1, fy + 3, 3, 3, Colors.scaleAlpha(dim, a * 0.8F));
      }

      for(FormattedCharSequence line : this.foot) {
         g.drawString(font, line, x + 7 + 10, fy, Colors.scaleAlpha(muted, a), vanilla);
         fy += 10;
      }

   }

   private static record Fit(boolean words, boolean row, boolean line) {
   }
}
