package com.onehumanawa.cfhcore.content.gui.menu.client.tablist;

import com.onehumanawa.cfhcore.content.gui.core.event.CFHGuiEvents;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

public final class TabListPreview {
   private static final float Z = 6000.0F;
   private static final String[] NAMES = new String[]{"Steve", "Alex", "xX_DragonSlayer_Xx", "Notch", "jeb_", "Dinnerbone", "Mo", "TheLegend27", "pixel_painter", "Sunny", "a_really_long_username", "Kai", "Herobrine", "Zuri", "Makena", "redstone_wiz", "Noor", "Efe", "Grumm", "Ari"};
   private static final int[] PINGS = new int[]{24, 58, 9, 112, 176, 41, 260, 85, 430, 33, 740, 1280};
   private static final String[] TEAMS = new String[]{"", "admin", "builder"};

   public static void register() {
      int n = Math.min(Integer.getInteger("cfhgui.tabListPreview", 0), 80);
      if (n > 0) {
         CFHGuiEvents.HUD_RENDER.register((CFHGuiEvents.HudRender)(g, partialTick) -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.screen == null && !mc.options.hideGui) {
               g.pose().pushPose();
               g.pose().translate(0.0F, 0.0F, 6000.0F);

               try {
                  CFHGuiTabList.draw(g, model(mc, n), 1.0F);
               } finally {
                  g.pose().popPose();
               }

            }
         });
      }
   }

   private static CFHGuiTabList.Model model(Minecraft mc, int n) {
      String asked = System.getProperty("cfhgui.tabListPreviewScore", "");
      CFHGuiTabList.Score score = "hearts".equalsIgnoreCase(asked) ? CFHGuiTabList.Score.HEARTS : ("number".equalsIgnoreCase(asked) ? CFHGuiTabList.Score.NUMBER : CFHGuiTabList.Score.NONE);
      List<Fake> fakes = new ArrayList(n);

      for(int i = 0; i < n; ++i) {
         String name = i == 0 ? mc.getUser().getName() : (i <= NAMES.length ? NAMES[i - 1] : "Player" + i);
         int team = i % 5 == 1 ? 1 : (i % 5 == 3 ? 2 : 0);
         int ping = i == 2 ? -1 : PINGS[i % PINGS.length];
         int value = score == CFHGuiTabList.Score.HEARTS ? 20 - i * 7 % 20 : i * 731 % 2500;
         fakes.add(new Fake(name, team, ping, i > 0 && i == n - 1, i == 0, value));
      }

      fakes.sort(Comparator.comparing(Fake::spectator).thenComparing((fx) -> TEAMS[fx.team()]).thenComparing(Fake::name, String::compareToIgnoreCase));
      List<CFHGuiTabList.Row> rows = new ArrayList(n);

      for(Fake f : fakes) {
         MutableComponent name = Component.literal(f.name());
         if (f.team() == 1) {
            name = Component.empty().append("[Admin] ").append(name).withStyle(ChatFormatting.RED);
         } else if (f.team() == 2) {
            name = Component.empty().append("[Builder] ").append(name).withStyle(ChatFormatting.AQUA);
         }

         if (f.spectator()) {
            name = name.withStyle(ChatFormatting.ITALIC);
         }

         ResourceLocation skin = f.self() ? mc.player.getSkin().texture() : DefaultPlayerSkin.get(UUID.nameUUIDFromBytes(("OfflinePlayer:" + f.name()).getBytes(StandardCharsets.UTF_8))).texture();
         Component value = score != CFHGuiTabList.Score.NONE && !f.spectator() ? (score == CFHGuiTabList.Score.HEARTS ? Component.literal(CFHGuiTabList.hearts(f.score())) : CFHGuiTabList.plainNumbers().format(f.score())) : null;
         boolean flipped = "Dinnerbone".equals(f.name()) || "Grumm".equals(f.name());
         rows.add(new CFHGuiTabList.Row(name, skin, true, flipped, f.spectator(), f.self(), f.ping(), value));
      }

      Component header = Component.empty().append("Welcome to ").append(Component.literal("Hollowbrook").withStyle(ChatFormatting.GOLD)).append("\nSurvival, season 3");
      Component footer = Component.literal("play.hollowbrook.example  ·  20.0 TPS");
      return new CFHGuiTabList.Model(Component.literal("Hollowbrook"), n, header, footer, rows, score);
   }

   private TabListPreview() {
   }

   private static record Fake(String name, int team, int ping, boolean spectator, boolean self, int score) {
   }
}
