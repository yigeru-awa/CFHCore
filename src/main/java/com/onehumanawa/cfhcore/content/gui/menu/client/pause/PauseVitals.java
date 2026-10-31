package com.onehumanawa.cfhcore.content.gui.menu.client.pause;

import com.mojang.blaze3d.systems.RenderSystem;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Fonts;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public final class PauseVitals {
   private static final ResourceLocation HEART_CONTAINER = ResourceLocation.withDefaultNamespace("hud/heart/container");
   private static final ResourceLocation HEART = ResourceLocation.withDefaultNamespace("hud/heart/full");
   private static final ResourceLocation HEART_HARDCORE = ResourceLocation.withDefaultNamespace("hud/heart/hardcore_full");
   private static final ResourceLocation ARMOR = ResourceLocation.withDefaultNamespace("hud/armor_full");
   private static final ResourceLocation FOOD_EMPTY = ResourceLocation.withDefaultNamespace("hud/food_empty");
   private static final ResourceLocation FOOD = ResourceLocation.withDefaultNamespace("hud/food_full");
   private static final int ICON = 9;
   private static final int GAP = 3;
   private static final int BETWEEN = 9;
   private static final int LEVEL = -8323296;

   private static List<Item> items() {
      Minecraft mc = Minecraft.getInstance();
      LocalPlayer p = mc.player;
      List<Item> out = new ArrayList();
      if (p != null && PauseActions.survives()) {
         boolean hardcore = mc.level != null && mc.level.getLevelData().isHardcore();
         float extra = p.getAbsorptionAmount();
         String var10000 = halves(p.getHealth());
         String health = var10000 + (extra > 0.01F ? " +" + halves(extra) : "");
         out.add(new Item(HEART_CONTAINER, hardcore ? HEART_HARDCORE : HEART, Fonts.heading(health), 0));
         if (p.getArmorValue() > 0) {
            out.add(new Item((ResourceLocation)null, ARMOR, Fonts.heading(halves((float)p.getArmorValue())), 0));
         }

         out.add(new Item(FOOD_EMPTY, FOOD, Fonts.heading(halves((float)p.getFoodData().getFoodLevel())), 0));
         if (p.experienceLevel > 0) {
            out.add(new Item((ResourceLocation)null, (ResourceLocation)null, Fonts.heading(Component.translatable("cfhgui.pause.level", new Object[]{p.experienceLevel}).getString()), -8323296));
         }

         return out;
      } else {
         return out;
      }
   }

   private static String halves(float points) {
      int half = Math.max(0, (int)Math.ceil((double)points));
      return half % 2 == 0 ? Integer.toString(half / 2) : String.format(Locale.ROOT, "%d.5", half / 2);
   }

   public static int width() {
      Font font = CFHGuiDraw.font();
      int w = 0;

      for(Item it : items()) {
         w += (w > 0 ? 9 : 0) + (it.sprite() == null ? 0 : 12) + font.width(it.text());
      }

      return w;
   }

   public static int draw(GuiGraphics g, int x, int y, float alpha, int textColor) {
      Font font = CFHGuiDraw.font();
      boolean shadow = Theme.current().isVanilla();
      int cx = x;

      for(Item it : items()) {
         if (cx > x) {
            cx += 9;
         }

         if (it.sprite() != null) {
            RenderSystem.enableBlend();
            g.setColor(1.0F, 1.0F, 1.0F, alpha);
            if (it.under() != null) {
               g.blitSprite(it.under(), cx, y - 1, 9, 9);
            }

            g.blitSprite(it.sprite(), cx, y - 1, 9, 9);
            g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            cx += 12;
         }

         g.drawString(font, it.text(), cx, y, Colors.scaleAlpha(it.color() != 0 ? it.color() : textColor, alpha), shadow);
         cx += font.width(it.text());
      }

      return cx - x;
   }

   private PauseVitals() {
   }

   private static record Item(@Nullable ResourceLocation under, @Nullable ResourceLocation sprite, Component text, int color) {
   }
}
