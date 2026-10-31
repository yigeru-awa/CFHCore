package com.onehumanawa.cfhcore.content.gui.menu.client.play.model;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiToasts;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.multiplayer.TransferState;
import net.minecraft.client.multiplayer.ServerData.Type;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public final class ServerActions {
   public static void join(ServerData data, @Nullable Screen parent) {
      Minecraft mc = Minecraft.getInstance();
      if (data != null && data.ip != null && !data.ip.isBlank()) {
         ConnectScreen.startConnecting((Screen)(parent != null ? parent : new TitleScreen()), mc, ServerAddress.parseString(data.ip), data, false, (TransferState)null);
      }
   }

   public static void join(String address, @Nullable String name, @Nullable Screen parent) {
      if (address != null && !address.isBlank()) {
         ServerData data = (ServerData)find(address).orElseGet(() -> new ServerData(name != null && !name.isBlank() ? name : address, address, Type.OTHER));
         join(data, parent);
      }
   }

   public static Optional<ServerData> find(String address) {
      if (address == null) {
         return Optional.empty();
      } else {
         String key = normalize(address);
         ServerList list = new ServerList(Minecraft.getInstance());
         list.load();

         for(int i = 0; i < list.size(); ++i) {
            ServerData d = list.get(i);
            if (normalize(d.ip).equals(key)) {
               return Optional.of(d);
            }
         }

         return Optional.empty();
      }
   }

   public static String normalize(String address) {
      return address == null ? "" : address.trim().toLowerCase(Locale.ROOT);
   }

   public static void copyAddress(String address) {
      Minecraft.getInstance().keyboardHandler.setClipboard(address == null ? "" : address);
      CFHGuiToasts.show(Component.translatable("cfhgui.copied"), Component.literal(address == null ? "" : address), Icon.COPY);
   }

   private ServerActions() {
   }
}
