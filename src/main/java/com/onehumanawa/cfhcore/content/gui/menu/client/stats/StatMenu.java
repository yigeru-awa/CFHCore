package com.onehumanawa.cfhcore.content.gui.menu.client.stats;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.stats.StatsLink;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiContextMenu;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiToasts;
import com.onehumanawa.cfhcore.content.gui.core.widget.popup.MenuPopup;
import com.onehumanawa.cfhcore.content.gui.core.widget.popup.MenuPopup.Item;
import com.onehumanawa.cfhcore.content.gui.menu.client.stats.data.StatsFeed;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public final class StatMenu {
   private static final int CHAT_MAX = 256;

   public static void open(StatsHost host, double mouseX, double mouseY, Subject subject, List<MenuPopup.Item> first) {
      List<MenuPopup.Item> items = new ArrayList(first);
      if (!items.isEmpty()) {
         items.add(Item.sep());
      }

      items.add(Item.of(Component.translatable("cfhgui.stats.menu.copy"), Icon.COPY, () -> copy(host, subject)));
      if (host.canChat()) {
         items.add(Item.of(Component.translatable("cfhgui.stats.menu.chat"), Icon.SEND, () -> say(host, subject)));
      } else {
         items.add(Item.disabled(Component.translatable("cfhgui.stats.menu.chat"), Icon.SEND));
      }

      if (rankable(host, subject.key())) {
         items.add(Item.sep());
         items.add(Item.of(Component.translatable("cfhgui.stats.menu.rank"), Icon.TROPHY, () -> host.rankBy(subject.key())));
      }

      CFHGuiContextMenu.open(mouseX, mouseY, items);
   }

   private static boolean rankable(StatsHost host, @Nullable String key) {
      if (key != null && host.canRank()) {
         return !key.endsWith("|*") ? true : (Boolean)StatsFeed.provider().map(StatsLink.Provider::ranksTotals).orElse(false);
      } else {
         return false;
      }
   }

   public static void copy(StatsHost host, Subject subject) {
      String line = line(host, subject, true);
      Minecraft.getInstance().keyboardHandler.setClipboard(line);
      CFHGuiToasts.show(Component.translatable("cfhgui.copied"), Component.literal(line), Icon.COPY);
   }

   private static void say(StatsHost host, Subject subject) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && host.canChat()) {
         String raw = line(host, subject, false);
         StringBuilder clean = new StringBuilder(raw.length());

         for(int i = 0; i < raw.length() && clean.length() < 256; ++i) {
            char c = raw.charAt(i);
            if (c != 167 && c >= ' ' && c != 127) {
               clean.append(c);
            }
         }

         String line = clean.toString().trim();
         if (!line.isEmpty() && !line.startsWith("/")) {
            mc.player.connection.sendChat(line);
         }
      }
   }

   private static String line(StatsHost host, Subject subject, boolean exact) {
      StatFmt.Shown shown = subject.shown();
      String spoken = shown.spoken();
      String what = exact && !shown.exact().isEmpty() && !shown.exact().equals(shown.text()) ? I18n.get("cfhgui.stats.line.exact", new Object[]{subject.label().getString(), spoken, shown.exact()}) : I18n.get("cfhgui.stats.line", new Object[]{subject.label().getString(), spoken});
      return host.feed().subject() == null ? what : I18n.get("cfhgui.stats.line.of", new Object[]{host.feed().shownName(), what});
   }

   private StatMenu() {
   }

   public static record Subject(Component label, StatFmt.Shown shown, @Nullable String key) {
   }
}
