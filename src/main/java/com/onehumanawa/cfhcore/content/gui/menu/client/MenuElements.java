package com.onehumanawa.cfhcore.content.gui.menu.client;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.layout.ElementType;
import com.onehumanawa.cfhcore.content.gui.core.layout.ElementTypes;
import com.onehumanawa.cfhcore.content.gui.core.layout.ScreenLayout;
import com.onehumanawa.cfhcore.content.gui.core.layout.action.ActionType;
import com.onehumanawa.cfhcore.content.gui.core.layout.action.ActionType.Arg;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiList;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiWidget;
import com.onehumanawa.cfhcore.content.gui.menu.client.play.model.ServerActions;
import com.onehumanawa.cfhcore.content.gui.menu.client.play.model.ServerPinger;
import com.onehumanawa.cfhcore.content.gui.menu.client.play.model.WorldActions;
import com.onehumanawa.cfhcore.content.gui.menu.client.play.model.WorldEntry;
import com.onehumanawa.cfhcore.content.gui.menu.client.servers.ServerCard;
import com.onehumanawa.cfhcore.content.gui.menu.client.title.AccountCard;
import com.onehumanawa.cfhcore.content.gui.menu.client.title.ContinueCard;
import com.onehumanawa.cfhcore.content.gui.menu.client.worlds.WorldCard;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerData.State;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.storage.LevelSummary;

public final class MenuElements {
   static void registerAll() {
      ElementTypes.register(new Type("cfhgui:continue_card", "Continue card", Icon.PLAY, new int[]{220, 66}, List.of(), false, (screen, e, x, y, w, h, run) -> new ContinueCard(x, y, w, h, screen)));
      ElementTypes.register(new Type("cfhgui:account_card", "Account card", Icon.USER, new int[]{220, 36}, List.of(), false, (screen, e, x, y, w, h, run) -> new AccountCard(x, y, w, h, screen)));
      ElementTypes.register(new Type("cfhgui:world_list", "World list", Icon.WORLD, new int[]{220, 120}, List.of(Arg.number("max", Component.literal("Max worlds (0 = all)"), "5")), false, (screen, e, x, y, w, h, run) -> {
         int max = 5;

         try {
            max = Integer.parseInt(((String)e.props.getOrDefault("max", "5")).trim());
         } catch (NumberFormatException var9) {
         }

         return new WorldListElement(x, y, w, h, max, screen);
      }));
      ElementTypes.register(new Type("cfhgui:server_status", "Server status", Icon.SERVER, new int[]{220, 44}, List.of(Arg.text("address", Component.literal("Address"), "play.example.com"), Arg.text("name", Component.literal("Name"), "My server")), true, (screen, e, x, y, w, h, run) -> new ServerStatusElement(x, y, w, h, (String)e.props.getOrDefault("address", ""), (String)e.props.getOrDefault("name", ""), e.actions.isEmpty() ? null : run, screen)));
   }

   private MenuElements() {
   }

   private static record Type(String id, String labelText, Icon icon, int[] size, List<ActionType.Arg> props, boolean clickable, Factory factory) implements ElementType {
      public Component label() {
         return Component.literal(this.labelText);
      }

      public int[] defaultSize() {
         return this.size;
      }

      public AbstractWidget create(Screen screen, ScreenLayout.Element element, int x, int y, int w, int h, Runnable runActions) {
         return this.factory.create(screen, element, x, y, w, h, runActions);
      }
   }

   static final class WorldListElement extends CFHGuiList<WorldEntry> {
      WorldListElement(int x, int y, int w, int h, int max, Screen screen) {
         super(x, y, w, h, 36, (g, item, index, rx, ry, rw, rh, hovered, selected, mx, my) -> WorldCard.drawSummary(g, item, rx + 4, ry + 2, rw - 8, 32, false, mx, my));
         this.emptyText(Component.translatable("cfhgui.worlds.loading"));
         this.onActivate((e) -> WorldActions.play(e.summary, screen));
         WorldActions.loadAll().whenCompleteAsync((list, err) -> {
            List<WorldEntry> out = new ArrayList();
            if (list != null) {
               List<LevelSummary> sorted = new ArrayList(list);
               sorted.sort(Comparator.comparingLong(LevelSummary::getLastPlayed).reversed());

               for(LevelSummary s : sorted) {
                  if (max > 0 && out.size() >= max) {
                     break;
                  }

                  out.add(new WorldEntry(s));
               }
            }

            this.items(out);
            this.emptyText(Component.translatable("cfhgui.worlds.empty"));
         }, Minecraft.getInstance());
      }
   }

   static final class ServerStatusElement extends CFHGuiWidget {
      private final ServerData data;
      private final ServerPinger pinger = new ServerPinger();
      private final Runnable run;
      private final Screen screen;

      ServerStatusElement(int x, int y, int w, int h, String address, String name, Runnable run, Screen screen) {
         super(x, y, w, h, Component.literal(name.isEmpty() ? address : name));
         this.data = (ServerData)ServerActions.find(address).orElseGet(() -> new ServerData(name.isEmpty() ? address : name, address, net.minecraft.client.multiplayer.ServerData.Type.OTHER));
         this.data.setState(State.INITIAL);
         this.run = run;
         this.screen = screen;
         this.active = !address.isBlank();
      }

      public void onClick(double mouseX, double mouseY) {
         super.onClick(mouseX, mouseY);
         if (this.run != null) {
            this.run.run();
         } else {
            ServerActions.join(this.data, this.screen);
         }

      }

      private void draw(GuiGraphics g, int mouseX, int mouseY, boolean van) {
         this.pinger.tick();
         if (!this.data.ip.isBlank()) {
            this.pinger.ping(this.data, () -> {
            });
         }

         Theme t = Theme.current();
         Palette p = t.palette();
         float a = this.effectiveAlpha();
         int x = this.getX();
         int y = this.getY() + this.enterOffset();
         int w = this.getWidth();
         int h = this.getHeight();
         if (van) {
            g.fill(x, y, x + w, y + h, Colors.scaleAlpha(-1879048192, a));
            CFHGuiDraw.outline(g, x, y, w, h, Colors.scaleAlpha(Colors.lerp(-7631989, -1, this.hover()), a), 0);
         } else {
            CFHGuiDraw.pixelRound(g, x, y, w, h, Colors.scaleAlpha(t.panel(Colors.lerp(p.surface(), p.surfaceHover(), this.hover())), a), t.radius());
            CFHGuiDraw.outline(g, x, y, w, h, Colors.scaleAlpha(Colors.lerp(p.border(), p.borderStrong(), this.hover()), a), t.radius());
         }

         Icons.draw(g, Icon.SERVER, x + 8, y + (h - 16) / 2, 16, Colors.scaleAlpha(van ? -1 : p.textMuted(), a));
         int tx = x + 30;
         int tw = w - 30 - 80;
         g.drawString(CFHGuiDraw.font(), CFHGuiDraw.truncate(this.getMessage(), tw), tx, y + 6, Colors.scaleAlpha(p.text(), a), van);
         Component motd = (Component)(this.data.motd == null ? Component.literal(this.data.ip) : this.data.motd);
         g.drawString(CFHGuiDraw.font(), CFHGuiDraw.truncate(motd, tw), tx, y + 18, Colors.scaleAlpha(van ? -4144960 : p.textMuted(), a), van);
         ServerCard.drawStatus(g, this.data, x + w - 78, y + 6, 72, mouseX, mouseY, false);
         CFHGuiDraw.focusRing(g, x, y, w, h, this.focus() * a);
      }

      protected void renderDark(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
         this.draw(g, mouseX, mouseY, false);
      }

      protected void renderVanilla(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
         this.draw(g, mouseX, mouseY, true);
      }
   }

   @FunctionalInterface
   interface Factory {
      AbstractWidget create(Screen var1, ScreenLayout.Element var2, int var3, int var4, int var5, int var6, Runnable var7);
   }
}
