package com.onehumanawa.cfhcore.content.gui.menu.client.servers;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiLabel;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiModal;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSegmented;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiTextField;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiToasts;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton.Variant;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiLabel.Style;
import com.onehumanawa.cfhcore.content.gui.core.widget.popup.Popups;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerData.ServerPackStatus;
import net.minecraft.client.multiplayer.ServerData.Type;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public final class ServerDialog {
   public static void open(Component title, @Nullable ServerData existing, Consumer<ServerData> onOk) {
      open(title, existing == null ? I18n.get("selectServer.defaultName", new Object[0]) : existing.name, existing == null ? "" : existing.ip, existing == null ? ServerPackStatus.PROMPT : existing.getResourcePackStatus(), existing, onOk);
   }

   private static void open(Component title, String initialName, String initialAddress, ServerData.ServerPackStatus initialPack, @Nullable ServerData existing, Consumer<ServerData> onOk) {
      int w = 216;
      CFHGuiTextField name = new CFHGuiTextField(0, 0, 216, Component.translatable("addServer.enterName"));
      name.placeholder(Component.translatable("addServer.enterName")).text(initialName).maxLength(128);
      CFHGuiTextField address = new CFHGuiTextField(0, 0, 216, Component.translatable("addServer.enterIp"));
      address.placeholder(Component.translatable("addServer.enterIp")).text(initialAddress).maxLength(128);
      address.setInvalid(!initialAddress.isEmpty() && !ServerAddress.isValidAddress(initialAddress));
      address.onChange((s) -> address.setInvalid(!s.isBlank() && !ServerAddress.isValidAddress(s)));
      CFHGuiSegmented<ServerData.ServerPackStatus> pack = new CFHGuiSegmented<ServerData.ServerPackStatus>(0, 0, 216, List.of(ServerPackStatus.ENABLED, ServerPackStatus.PROMPT, ServerPackStatus.DISABLED), initialPack, ServerData.ServerPackStatus::getName, (Consumer<ServerData.ServerPackStatus>)null);
      CFHGuiModal modal = (new CFHGuiModal(title, (Component)null, Icon.SERVER)).extra((new CFHGuiLabel(0, 0, 216, Component.translatable("addServer.enterName"))).style(Style.CAPTION)).extra(name).extra((new CFHGuiLabel(0, 0, 216, Component.translatable("addServer.enterIp"))).style(Style.CAPTION)).extra(address).extra((new CFHGuiLabel(0, 0, 216, Component.translatable("addServer.resourcePack"))).style(Style.CAPTION)).extra(pack);
      Runnable ok = () -> {
         Popups.close(modal);
         String n = name.getValue().trim();
         String a = address.getValue().trim();
         if (!a.isEmpty() && ServerAddress.isValidAddress(a)) {
            ServerData d = existing != null ? existing : new ServerData(n, a, Type.OTHER);
            d.name = n.isEmpty() ? a : n;
            d.ip = a;
            d.setResourcePackStatus((ServerData.ServerPackStatus)pack.value());
            onOk.accept(d);
         } else {
            CFHGuiToasts.show(Component.translatable("cfhgui.servers.invalid_address"), Component.literal(a), Icon.WARNING);
            open(title, n, a, (ServerData.ServerPackStatus)pack.value(), existing, onOk);
         }
      };
      name.onEnter(ok);
      address.onEnter(ok);
      modal.button(Component.translatable("gui.cancel"), Variant.SECONDARY, (Runnable)null).button(existing == null ? Component.translatable("addServer.add") : Component.translatable("gui.done"), Variant.PRIMARY, ok).show();
   }

   private ServerDialog() {
   }
}
