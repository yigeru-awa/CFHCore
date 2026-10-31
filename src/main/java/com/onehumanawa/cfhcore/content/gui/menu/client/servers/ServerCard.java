package com.onehumanawa.cfhcore.content.gui.menu.client.servers;

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
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiBadge;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiCard;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiIconButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiTooltips;
import com.onehumanawa.cfhcore.content.gui.menu.api.CFHGuiMenuApi;
import com.onehumanawa.cfhcore.content.gui.menu.client.Fmt;
import com.onehumanawa.cfhcore.content.gui.menu.client.play.model.ServerActions;
import com.onehumanawa.cfhcore.content.gui.menu.client.play.model.ServerMeta;
import com.onehumanawa.cfhcore.content.gui.menu.client.play.model.ServerPinger;
import com.onehumanawa.cfhcore.content.gui.menu.client.worlds.PlayPlates;
import com.onehumanawa.cfhcore.content.gui.menu.client.worlds.WorldCard;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerData.State;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;

public class ServerCard extends CFHGuiCard {
   public static final int HEIGHT = 44;
   private static final int RIGHT_W = 76;
   private static final int BARS_W = 14;
   private static final int CROSS = 7;
   private final ServerData data;
   private final Kind kind;
   private final CFHGuiServersScreen screen;
   private final @Nullable CFHGuiIconButton star;
   private final CFHGuiIconButton plate;
   private final Anim play;
   private final int rightW;
   private Textures.@Nullable Loaded favicon;
   private @Nullable byte[] faviconBytes;
   private final @Nullable String description;
   private boolean savedHint;

   public ServerCard savedHint(boolean saved) {
      this.savedHint = saved;
      return this;
   }

   public ServerCard(int x, int y, int w, ServerData data, Kind kind, CFHGuiServersScreen screen, @Nullable String description) {
      super(x, y, w, 44);
      this.play = new Anim(0.0F, 200, Ease.OUT_CUBIC);
      this.data = data;
      this.kind = kind;
      this.screen = screen;
      this.description = description;
      this.lift(2);
      int widest = Math.max(CFHGuiDraw.width(Component.translatable("cfhgui.servers.offline")) + 7 + 4, kind == ServerCard.Kind.LAN ? CFHGuiDraw.width(Component.translatable("lanServer.title")) : 0);
      this.rightW = Math.max(76, Math.min(w / 3, widest + 6));
      if (kind == ServerCard.Kind.SAVED) {
         boolean fav = ServerMeta.isFavorite(data.ip);
         this.star = (CFHGuiIconButton)this.add(new CFHGuiIconButton(0, 0, 18, fav ? Icon.STAR_FILLED : Icon.STAR, Component.translatable(fav ? "cfhgui.unfavorite" : "cfhgui.favorite"), () -> screen.toggleFavorite(data)), w - this.rightW - 20, 4);
         this.star.toggled(fav);
      } else {
         this.star = null;
      }

      this.plate = (CFHGuiIconButton)this.add(PlayPlates.create(18, () -> screen.join(this)), 13, 13);
      PlayPlates.label(this.plate, Component.translatable("cfhgui.worlds.join_this", new Object[]{data.name != null && !data.name.isBlank() ? data.name : data.ip}));
      this.onClick(() -> screen.clicked(this));
      this.onRightClick(() -> screen.contextMenu(this));
   }

   public ServerData data() {
      return this.data;
   }

   public Kind kind() {
      return this.kind;
   }

   public CFHGuiCard selected(boolean s) {
      super.selected(s);
      if (!s) {
         this.plate.visible = false;
      }

      return this;
   }

   private void ensureFavicon() {
      byte[] bytes = this.data.getIconBytes();
      if (bytes != null && bytes != this.faviconBytes) {
         this.faviconBytes = bytes;
         String var10002 = ServerActions.normalize(this.data.ip);
         this.favicon = (Textures.Loaded)Textures.fromBytes(bytes, "cfhgui:favicon:" + var10002 + ":" + bytes.length).orElse(null);
      }
   }

   protected void renderContent(GuiGraphics g, int x, int y, int w, int h, int mouseX, int mouseY, float partialTick) {
      Theme t = Theme.current();
      Palette p = t.palette();
      boolean van = t.isVanilla();
      if (this.kind != ServerCard.Kind.LAN) {
         ServerPinger var10000 = this.screen.pinger();
         ServerData var10001 = this.data;
         CFHGuiServersScreen var10002 = this.screen;
         Objects.requireNonNull(var10002);
         var10000.ping(var10001, var10002::onPingChanged);
      }

      this.ensureFavicon();
      int ix = x + 6;
      int iy = y + 6;
      int is = 32;
      if (this.favicon != null) {
         RenderSystem.enableBlend();
         g.blit(this.favicon.id(), ix, iy, 32, 32, 0.0F, 0.0F, this.favicon.width(), this.favicon.height(), this.favicon.width(), this.favicon.height());
         RenderSystem.disableBlend();
      } else {
         CFHGuiDraw.pixelRound(g, ix, iy, 32, 32, van ? Integer.MIN_VALUE : p.bg2(), t.radius());
         Icons.draw(g, this.kind == ServerCard.Kind.LAN ? Icon.LAN : Icon.SERVER, ix + 8, iy + 8, 16, van ? -6250336 : p.textDim());
      }

      CFHGuiDraw.outline(g, ix, iy, 32, 32, van ? -16777216 : p.border(), t.radius());
      int tx = ix + 32 + 8;
      int rightX = x + w - this.rightW;
      int textW = rightX - tx - (this.star != null ? 22 : 4);
      Component name = Component.literal(this.data.name != null && !this.data.name.isBlank() ? this.data.name : this.data.ip);
      int nameW = Math.min(CFHGuiDraw.width(name), textW);
      g.drawString(CFHGuiDraw.font(), CFHGuiDraw.truncate(name, textW), tx, y + 6, p.text(), van);
      int cx = tx + nameW + 6;
      int cy = y + 5;
      int friends = CFHGuiMenuApi.friendsOn(this.data.ip);
      if (friends > 0 && cx + 40 < rightX) {
         cx += CFHGuiBadge.draw(g, Component.translatable("cfhgui.servers.friends_here", new Object[]{friends}), cx, cy, p.success()) + 3;
      }

      if (this.kind == ServerCard.Kind.SAVED) {
         String group = ServerMeta.group(this.data.ip);
         if (!group.isEmpty() && cx + CFHGuiDraw.width(group) + 8 < rightX) {
            int var30 = cx + CFHGuiBadge.draw(g, Component.literal(group), cx, cy, van ? -7631989 : p.borderStrong()) + 3;
         }
      } else if (this.kind == ServerCard.Kind.LAN) {
         if (cx + 30 < rightX) {
            int var31 = cx + CFHGuiBadge.draw(g, Component.literal("LAN"), cx, cy, p.accent()) + 3;
         }
      } else if (this.kind == ServerCard.Kind.RECENT && this.savedHint && cx + 40 < rightX) {
         int var32 = cx + CFHGuiBadge.draw(g, Component.translatable("cfhgui.servers.saved"), cx, cy, van ? -7631989 : p.borderStrong()) + 3;
      }

      int muted = van ? -4144960 : p.textMuted();
      boolean address = this.kind == ServerCard.Kind.RECENT || this.kind == ServerCard.Kind.COMMUNITY || this.kind == ServerCard.Kind.LAN;
      Component motd = (Component)(this.data.motd == null ? Component.empty() : this.data.motd);
      List<FormattedCharSequence> lines = CFHGuiDraw.font().split(motd, textW);
      int ly = y + 18;

      for(int i = 0; i < Math.min(address ? 1 : 2, lines.size()); ly += 10) {
         g.drawString(CFHGuiDraw.font(), (FormattedCharSequence)lines.get(i), tx, ly, muted, van);
         ++i;
      }

      if (lines.isEmpty() && this.description != null && !this.description.isEmpty()) {
         g.drawString(CFHGuiDraw.font(), CFHGuiDraw.truncate(Component.literal(this.description), textW), tx, ly, muted, van);
         ly += 10;
      }

      if (address) {
         g.drawString(CFHGuiDraw.font(), CFHGuiDraw.truncate(Component.literal(this.data.ip), textW), tx, ly, van ? -6250336 : p.textDim(), van);
      }

      drawStatus(g, this.data, rightX, y + 6, this.rightW - 6, mouseX, mouseY, this.kind == ServerCard.Kind.LAN);
      if (this.description != null && !this.description.isEmpty() && mouseX >= x && mouseX < rightX && mouseY >= y && mouseY < y + h && g.containsPointInScissor(mouseX, mouseY)) {
         CFHGuiTooltips.request(List.of(Component.literal(this.description)), (AbstractWidget)null);
      }

      this.plate.visible = false;
   }

   protected void renderOverlay(GuiGraphics g, int x, int y, int w, int h, int mouseX, int mouseY, float partialTick) {
      float a = this.alpha * this.enterProgress();
      if (!this.isSelected() || !Theme.current().isVanilla()) {
         WorldCard.hoverEdge(g, x, y, w, h, this.hover() * a);
      }

      boolean mine = this.isSelected();
      boolean pointed = this.isMouseOver((double)mouseX, (double)mouseY) && g.containsPointInScissor(mouseX, mouseY);
      this.play.set(mine && (pointed || this.plate.isFocused()));
      float k = this.play.get();
      if (k > 0.01F) {
         this.plate.visible = true;
         PlayPlates.draw(g, this.plate, k, a, 0.0F, 0.0F, mine ? mouseX : -10000, mine ? mouseY : -10000, partialTick);
      }

      this.plate.visible = mine;
   }

   public static void drawStatus(GuiGraphics g, ServerData d, int x, int y, int w, int mouseX, int mouseY, boolean lan) {
      Theme t = Theme.current();
      Palette p = t.palette();
      boolean van = t.isVanilla();
      int right = x + w;
      if (lan) {
         Component lanText = Component.translatable("lanServer.title");
         g.drawString(CFHGuiDraw.font(), CFHGuiDraw.truncate(lanText, w), right - Math.min(CFHGuiDraw.width(lanText), w) + 1, y, van ? -4144960 : p.textMuted(), van);
      } else {
         int beside = right - 14 - 3;
         switch (d.state()) {
            case INITIAL:
            case PINGING:
               int lit = (int)(Clock.nowMs() / 120L % 5L);
               drawBars(g, right - 14, y - 1, lit + 1, Colors.withAlpha(van ? -1 : p.textMuted(), 160), 5);
               Component txt = Component.translatable("multiplayer.status.pinging");
               int room = w - 14 - 4;
               g.drawString(CFHGuiDraw.font(), CFHGuiDraw.truncate(txt, room), beside - Math.min(CFHGuiDraw.width(txt), room), y, van ? -6250336 : p.textDim(), van);
               break;
            case UNREACHABLE:
               for(int i = 0; i < 7; ++i) {
                  g.fill(right - 7 + i, y + i, right - 7 + i + 1, y + i + 1, p.danger());
                  if (i != 3) {
                     g.fill(right - 1 - i, y + i, right - i, y + i + 1, p.danger());
                  }
               }

               Component txt2 = Component.translatable("cfhgui.servers.offline");
               int room2 = w - 7 - 4;
               g.drawString(CFHGuiDraw.font(), CFHGuiDraw.truncate(txt2, room2), right - 7 - 3 - Math.min(CFHGuiDraw.width(txt2), room2), y, p.danger(), van);
               break;
            default:
               int bars = Fmt.pingBars(d.ping);
               int barColor = bars >= 4 ? p.success() : (bars >= 2 ? p.warning() : p.danger());
               drawBars(g, right - 14, y - 1, bars, barColor, 5);
               String players = ServerPinger.players(d);
               int pw = CFHGuiDraw.width(players);
               g.drawString(CFHGuiDraw.font(), players, beside - pw, y, van ? -1 : p.text(), van);
               String ms = d.ping + " ms";
               g.drawString(CFHGuiDraw.font(), ms, right - CFHGuiDraw.width(ms) + 1, y + 11, van ? -6250336 : p.textDim(), van);
               if (d.state() == State.INCOMPATIBLE) {
                  Component v = (Component)(d.version == null ? Component.literal("?") : d.version);
                  int vw = CFHGuiDraw.width(v) + 8;
                  CFHGuiBadge.draw(g, (Component)(CFHGuiDraw.width(v) > w - 8 ? Component.literal("!") : v), right - Math.min(vw, w), y + 22, p.danger());
               } else if (SharedConstants.getCurrentVersion().getProtocolVersion() == d.protocol) {
                  Icons.draw(g, Icon.CHECK, right - 7, y + 23, 8, Colors.withAlpha(p.success(), 192));
               }

               if (mouseX >= beside - pw && mouseX <= right && mouseY >= y - 2 && mouseY < y + 20 && g.containsPointInScissor(mouseX, mouseY)) {
                  List<Component> tip = new ArrayList();
                  tip.add(Component.translatable("cfhgui.servers.players_online", new Object[]{players}));
                  if (d.playerList != null) {
                     tip.addAll(d.playerList);
                  }

                  if (d.version != null) {
                     tip.add(Component.translatable("cfhgui.servers.version", new Object[]{d.version}));
                  }

                  CFHGuiTooltips.request(tip, (AbstractWidget)null);
               }
         }

      }
   }

   public static void drawBars(GuiGraphics g, int x, int y, int lit, int color, int total) {
      Palette p = Theme.current().palette();

      for(int i = 0; i < total; ++i) {
         int bh = 2 + i * 2;
         int bx = x + i * 3;
         int by = y + 10 - bh;
         g.fill(bx, by, bx + 2, by + bh, i < lit ? color : Colors.withAlpha(Theme.current().isVanilla() ? -8355712 : p.borderStrong(), 144));
      }

   }

   public void refreshStar() {
      if (this.star != null) {
         boolean fav = ServerMeta.isFavorite(this.data.ip);
         this.star.setIcon(fav ? Icon.STAR_FILLED : Icon.STAR);
         this.star.toggled(fav);
         this.star.tip(Component.translatable(fav ? "cfhgui.unfavorite" : "cfhgui.favorite"));
      }
   }

   public static enum Kind {
      SAVED,
      RECENT,
      LAN,
      COMMUNITY;
   }
}
