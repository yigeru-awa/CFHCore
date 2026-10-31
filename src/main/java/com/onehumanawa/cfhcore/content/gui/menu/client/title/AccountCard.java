package com.onehumanawa.cfhcore.content.gui.menu.client.title;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.module.Features;
import com.onehumanawa.cfhcore.content.gui.core.screen.slot.MenuSlots;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiAvatar;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiCard;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiAvatar.Status;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.User.Type;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.SkinCustomizationScreen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public class AccountCard extends CFHGuiCard {
   public static final int HEIGHT = 36;
   private final boolean online;

   public AccountCard(int x, int y, int w, int h, @Nullable Screen parent) {
      super(x, y, w, h);
      Minecraft mc = Minecraft.getInstance();
      User user = mc.getUser();
      this.online = user.getType() == Type.MSA;
      CFHGuiAvatar avatar = (CFHGuiAvatar)this.add((new CFHGuiAvatar(0, 0, 24, mc.getGameProfile())).status(this.online ? Status.ONLINE : Status.OFFLINE), 8, (h - 24) / 2);
      avatar.tip(List.of(Component.literal(user.getName()), Component.literal(user.getProfileId().toString())));
      this.onClick(() -> {
         if (Features.present("cfhgui_profile")) {
            MenuSlots.open("cfhgui_profile:profile", parent);
         } else {
            mc.setScreen(new SkinCustomizationScreen(parent, mc.options));
         }

      });
   }

   protected void renderContent(GuiGraphics g, int x, int y, int w, int h, int mouseX, int mouseY, float partialTick) {
      Theme t = Theme.current();
      Palette p = t.palette();
      boolean van = t.isVanilla();
      Minecraft mc = Minecraft.getInstance();
      int tx = x + 40;
      g.drawString(CFHGuiDraw.font(), CFHGuiDraw.truncate(Component.literal(mc.getUser().getName()), w - 60), tx, y + 8, p.text(), van);
      Component status = Component.translatable(this.online ? "cfhgui.title.session_online" : "cfhgui.title.session_offline");
      g.drawString(CFHGuiDraw.font(), CFHGuiDraw.truncate(status, w - 60), tx, y + 19, van ? -4144960 : p.textMuted(), van);
      Icons.draw(g, Icon.EDIT, x + w - 18, y + (h - 10) / 2, 10, van ? -6250336 : p.textDim());
   }
}
