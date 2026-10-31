package com.onehumanawa.cfhcore.content.gui.menu.client.title;

import com.mojang.blaze3d.systems.RenderSystem;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Textures;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiBadge;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiCard;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiIconButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSpinner;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton.Variant;
import com.onehumanawa.cfhcore.content.gui.menu.client.Fmt;
import com.onehumanawa.cfhcore.content.gui.menu.client.LastPlayed;
import com.onehumanawa.cfhcore.content.gui.menu.client.play.model.ServerActions;
import com.onehumanawa.cfhcore.content.gui.menu.client.play.model.ServerPinger;
import com.onehumanawa.cfhcore.content.gui.menu.client.play.model.WorldActions;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerData.State;
import net.minecraft.client.multiplayer.ServerData.Type;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.storage.LevelSummary;
import org.jetbrains.annotations.Nullable;

public class ContinueCard extends CFHGuiCard {
   public static final int HEIGHT = 66;
   private final @Nullable Screen parent;
   private boolean loading = true;
   private LastPlayed.@Nullable Target target;
   private @Nullable ServerData server;
   private final ServerPinger pinger = new ServerPinger();
   private final Anim appear;
   private Textures.@Nullable Loaded icon;
   private boolean iconRequested;
   private @Nullable byte[] faviconBytes;
   private final CFHGuiButton play;
   private final CFHGuiButton create;

   public ContinueCard(int x, int y, int w, int h, @Nullable Screen parent) {
      super(x, y, w, h);
      this.appear = new Anim(0.0F, 260, Ease.OUT_CUBIC);
      this.parent = parent;
      this.play = (CFHGuiButton)this.add((new CFHGuiIconButton(0, 0, 20, Icon.PLAY, Component.translatable("cfhgui.title.play"), this::play)).variant(Variant.PRIMARY), w - 28, h - 28);
      this.play.visible = false;
      this.create = (CFHGuiButton)this.add((new CFHGuiButton(0, 0, 104, 18, Component.translatable("cfhgui.title.create_world"), () -> WorldActions.createNew(parent))).variant(Variant.SECONDARY).icon(Icon.NEW_WORLD), w - 112, 5);
      this.create.visible = false;
      this.onClick(this::play);
      this.appear.snap(0.0F);
      LastPlayed.resolve(this::accept);
   }

   private void accept(Optional<LastPlayed.Target> resolved) {
      this.loading = false;
      this.target = (LastPlayed.Target)resolved.orElse(null);
      this.appear.set(1.0F);
      if (this.target == null) {
         this.create.visible = true;
      } else {
         this.play.visible = true;
         if (this.target.kind() == LastPlayed.Kind.SERVER) {
            this.server = (ServerData)ServerActions.find(this.target.id()).orElseGet(() -> new ServerData(this.target.name(), this.target.id(), Type.OTHER));
            this.server.setState(State.INITIAL);
            this.pinger.ping(this.server, () -> {
            });
         }

      }
   }

   public void tick() {
      this.pinger.tick();
   }

   public void close() {
      this.pinger.close();
   }

   public LastPlayed.@Nullable Target target() {
      return this.target;
   }

   private void play() {
      if (this.target != null) {
         LastPlayed.play(this.target, this.parent);
      }

   }

   private void ensureIcon() {
      if (!this.iconRequested && this.target != null) {
         if (this.target.kind() == LastPlayed.Kind.WORLD) {
            this.iconRequested = true;
            LevelSummary s = this.target.summary();
            Path p = s == null ? null : s.getIcon();
            if (p != null && Files.isRegularFile(p, new LinkOption[0])) {
               Textures.load(p, (l) -> this.icon = l);
            }
         } else if (this.server != null && this.server.getIconBytes() != null && this.server.getIconBytes() != this.faviconBytes) {
            this.faviconBytes = this.server.getIconBytes();
            byte[] var10001 = this.faviconBytes;
            String var10002 = this.target.id();
            this.icon = (Textures.Loaded)Textures.fromBytes(var10001, "cfhgui:favicon:" + var10002 + ":" + this.faviconBytes.length).orElse(null);
         }

      }
   }

   protected void renderContent(GuiGraphics g, int x, int y, int w, int h, int mouseX, int mouseY, float partialTick) {
      Theme t = Theme.current();
      Palette p = t.palette();
      boolean van = t.isVanilla();
      int muted = van ? -4144960 : p.textMuted();
      this.pinger.tick();
      g.drawString(CFHGuiDraw.font(), Component.translatable("cfhgui.title.continue"), x + 10, y + 7, van ? -6250336 : p.textDim(), van);
      if (this.loading) {
         CFHGuiSpinner.draw(g, x + 10, y + 24, 16, van ? -1 : p.accentInk());
         g.drawString(CFHGuiDraw.font(), Component.translatable("cfhgui.title.looking"), x + 32, y + 28, muted, van);
      } else {
         float a = this.appear.get();
         if (this.target == null) {
            g.drawString(CFHGuiDraw.font(), CFHGuiDraw.truncate(Component.translatable("cfhgui.title.nothing_yet"), w - 124), x + 10, y + 26, Colors.scaleAlpha(p.text(), a), van);
            g.drawString(CFHGuiDraw.font(), CFHGuiDraw.truncate(Component.translatable("cfhgui.title.nothing_yet_hint"), w - 20), x + 10, y + 40, Colors.scaleAlpha(muted, a), van);
         } else {
            this.ensureIcon();
            int ix = x + 10;
            int iy = y + 21;
            int is = 32;
            if (this.icon != null) {
               RenderSystem.enableBlend();
               g.setColor(1.0F, 1.0F, 1.0F, a);
               g.blit(this.icon.id(), ix, iy, 32, 32, 0.0F, 0.0F, this.icon.width(), this.icon.height(), this.icon.width(), this.icon.height());
               g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
               RenderSystem.disableBlend();
            } else {
               CFHGuiDraw.pixelRound(g, ix, iy, 32, 32, Colors.scaleAlpha(van ? Integer.MIN_VALUE : p.bg2(), a), t.radius());
               Icons.draw(g, this.target.kind() == LastPlayed.Kind.WORLD ? Icon.WORLD : Icon.SERVER, ix + 8, iy + 8, 16, Colors.scaleAlpha(muted, a));
            }

            CFHGuiDraw.outline(g, ix, iy, 32, 32, Colors.scaleAlpha(van ? -16777216 : p.border(), a), t.radius());
            int tx = ix + 32 + 8;
            int full = x + w - 10;
            int lh = CFHGuiDraw.lineHeight();
            FormattedCharSequence name = CFHGuiDraw.truncate(Component.literal(this.target.name() != null && !this.target.name().isBlank() ? this.target.name() : this.target.id()), this.rowRight(y + 22, lh, full) - tx);
            g.drawString(CFHGuiDraw.font(), name, tx, y + 22, Colors.scaleAlpha(p.text(), a), van);
            g.drawString(CFHGuiDraw.font(), CFHGuiDraw.truncate(this.subtitle(), this.rowRight(y + 34, lh, full) - tx), tx, y + 34, Colors.scaleAlpha(muted, a), van);
            int right = this.rowRight(y + 46, 10, full);
            if (this.target.kind() == LastPlayed.Kind.SERVER && this.server != null) {
               int dot;
               Component detail;
               switch (this.server.state()) {
                  case SUCCESSFUL:
                  case INCOMPATIBLE:
                     dot = p.success();
                     String players = ServerPinger.players(this.server);
                     detail = players.isEmpty() ? Component.translatable("cfhgui.servers.online") : Component.translatable("cfhgui.servers.online_count", new Object[]{players, this.server.ping});
                     break;
                  case UNREACHABLE:
                     dot = p.danger();
                     detail = Component.translatable("cfhgui.servers.offline");
                     break;
                  default:
                     dot = p.warning();
                     detail = Component.translatable("cfhgui.servers.pinging");
               }

               g.fill(tx, y + 48, tx + 4, y + 52, Colors.scaleAlpha(dot, a));
               g.drawString(CFHGuiDraw.font(), CFHGuiDraw.truncate(detail, right - tx - 6), tx + 6, y + 46, Colors.scaleAlpha(muted, a), van);
            } else if (this.target.summary() != null) {
               LevelSummary s = this.target.summary();
               int cx = chip(g, s.getGameMode().getShortDisplayName(), tx, y + 46, right, Colors.scaleAlpha(p.accent(), a));
               if (s.isHardcore()) {
                  cx = chip(g, Component.translatable("cfhgui.worlds.hardcore"), cx, y + 46, right, Colors.scaleAlpha(p.danger(), a));
               }

               if (s.hasCommands()) {
                  chip(g, Component.translatable("cfhgui.worlds.cheats"), cx, y + 46, right, Colors.scaleAlpha(p.warning(), a));
               }
            }

         }
      }
   }

   private int rowRight(int rowY, int rowH, int full) {
      boolean level = this.play.visible && rowY + rowH > this.play.getY() && rowY < this.play.getY() + this.play.getHeight();
      return level ? this.play.getX() - 4 : full;
   }

   private static int chip(GuiGraphics g, Component text, int x, int y, int right, int color) {
      return x + CFHGuiDraw.width(text) + 8 > right ? right : x + CFHGuiBadge.draw(g, text, x, y, color) + 4;
   }

   private Component subtitle() {
      if (this.target == null) {
         return Component.empty();
      } else {
         Component kind = Component.translatable(this.target.kind() == LastPlayed.Kind.WORLD ? "cfhgui.title.singleplayer" : "cfhgui.title.multiplayer");
         return Component.empty().append(kind).append(" · ").append(Component.translatable("cfhgui.title.last_played", new Object[]{Fmt.ago(this.target.at())}));
      }
   }
}
