package com.onehumanawa.cfhcore.content.gui.menu.client.disconnect;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Fonts;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.core.screen.CFHGuiScreen;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiLabel;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiScrollPanel;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton.Variant;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiLabel.Style;
import com.onehumanawa.cfhcore.content.gui.menu.client.LastPlayed;
import java.net.URI;
import java.nio.file.Path;
import java.util.Optional;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public final class CFHGuiDisconnectedScreen extends CFHGuiScreen {
   private final DisconnectionDetails details;
   private Rect card = new Rect(0, 0, 0, 0);

   public CFHGuiDisconnectedScreen(@Nullable Screen parent, Component title, DisconnectionDetails details) {
      super(title, parent);
      this.details = details;
      this.showHeader = false;
      this.showBack = false;
   }

   public boolean shouldCloseOnEsc() {
      return false;
   }

   protected void build() {
      Minecraft mc = Minecraft.getInstance();
      int w = Math.min(this.width - 24, 320);
      int inner = w - 24;
      int textH = Math.min(Math.max(20, this.font.split(this.details.reason(), inner).size() * 10), Math.max(40, this.height - 150));
      Optional<LastPlayed.Target> last = LastPlayed.quick().filter((tx) -> tx.kind() == LastPlayed.Kind.SERVER);
      int buttons = 2 + (last.isPresent() ? 1 : 0) + (this.details.report().isPresent() ? 1 : 0) + (this.details.bugReportLink().isPresent() ? 1 : 0);
      int h = 28 + textH + 10 + buttons * 24 + 4;
      int x = (this.width - w) / 2;
      int y = Math.max(4, (this.height - h) / 2);
      this.card = new Rect(x, y, w, h);
      CFHGuiScrollPanel panel = (CFHGuiScrollPanel)this.add(new CFHGuiScrollPanel(x + 12, y + 30, inner, textH));
      CFHGuiLabel reason = (new CFHGuiLabel(0, 0, inner - 8, this.details.reason())).style(Style.BODY).wrap(true);
      panel.add(reason, 0, 0);
      int by = y + 30 + textH + 10;
      if (last.isPresent()) {
         LastPlayed.Target t = (LastPlayed.Target)last.get();
         this.add((new CFHGuiButton(x + 12, by, inner, Component.translatable("cfhgui.disconnected.reconnect", new Object[]{t.name() != null && !t.name().isBlank() ? t.name() : t.id()}), () -> LastPlayed.play(t, this.parent))).variant(Variant.PRIMARY).icon(Icon.REFRESH));
         by += 24;
      }

      this.add((new CFHGuiButton(x + 12, by, inner, Component.translatable("gui.back"), this::back)).icon(Icon.ARROW_LEFT).variant(last.isPresent() ? Variant.SECONDARY : Variant.PRIMARY));
      by += 24;
      if (this.details.report().isPresent()) {
         Path report = (Path)this.details.report().get();
         this.add((new CFHGuiButton(x + 12, by, inner, Component.translatable("gui.open_report_dir"), () -> Util.getPlatform().openPath(report.getParent()))).icon(Icon.FOLDER));
         by += 24;
      }

      if (this.details.bugReportLink().isPresent()) {
         URI uri = (URI)this.details.bugReportLink().get();
         this.add((new CFHGuiButton(x + 12, by, inner, Component.translatable("gui.report_to_server"), () -> mc.setScreen(new ConfirmLinkScreen((ok) -> {
               if (ok) {
                  Util.getPlatform().openUri(uri);
               }

               mc.setScreen(this);
            }, uri.toString(), true)))).icon(Icon.EXTERNAL));
         by += 24;
      }

      this.add((new CFHGuiButton(x + 12, by, inner, Component.translatable("gui.toTitle"), () -> mc.setScreen(new TitleScreen()))).variant(Variant.GHOST).icon(Icon.HOME));
   }

   public void back() {
      Minecraft.getInstance().setScreen((Screen)(this.parent != null ? this.parent : new TitleScreen()));
   }

   public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      super.renderBackground(g, mouseX, mouseY, partialTick);
      Theme t = Theme.current();
      Palette p = t.palette();
      if (t.isVanilla()) {
         CFHGuiDraw.vanillaPanel(g, this.card.x(), this.card.y(), this.card.w(), this.card.h(), false);
         CFHGuiDraw.outline(g, this.card.x(), this.card.y(), this.card.w(), this.card.h(), -16777216, 0);
         CFHGuiDraw.outline(g, this.card.x() + 1, this.card.y() + 1, this.card.w() - 2, this.card.h() - 2, -7631989, 0);
      } else {
         CFHGuiDraw.plate(g, this.card.x(), this.card.y(), this.card.w(), this.card.h(), p.surface(), p.borderStrong(), 0.6F, 1.0F);
      }

   }

   protected void renderContent(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      Theme t = Theme.current();
      Palette p = t.palette();
      Icons.draw(g, Icon.ERROR, this.card.x() + 12, this.card.y() + 11, 12, p.danger());
      g.drawString(this.font, CFHGuiDraw.truncate(Fonts.heading(this.getTitle()), this.card.w() - 40), this.card.x() + 28, this.card.y() + 12, p.text(), t.isVanilla());
   }
}
