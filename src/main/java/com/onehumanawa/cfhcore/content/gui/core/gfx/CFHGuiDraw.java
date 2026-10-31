package com.onehumanawa.cfhcore.content.gui.core.gfx;

import com.mojang.blaze3d.systems.RenderSystem;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import java.util.Objects;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

public final class CFHGuiDraw {
   public static final ResourceLocation BUTTON = ResourceLocation.withDefaultNamespace("widget/button");
   public static final ResourceLocation BUTTON_DISABLED = ResourceLocation.withDefaultNamespace("widget/button_disabled");
   public static final ResourceLocation BUTTON_HIGHLIGHTED = ResourceLocation.withDefaultNamespace("widget/button_highlighted");
   public static final ResourceLocation SLIDER = ResourceLocation.withDefaultNamespace("widget/slider");
   public static final ResourceLocation SLIDER_HIGHLIGHTED = ResourceLocation.withDefaultNamespace("widget/slider_highlighted");
   public static final ResourceLocation SLIDER_HANDLE = ResourceLocation.withDefaultNamespace("widget/slider_handle");
   public static final ResourceLocation SLIDER_HANDLE_HIGHLIGHTED = ResourceLocation.withDefaultNamespace("widget/slider_handle_highlighted");
   public static final ResourceLocation TEXT_FIELD = ResourceLocation.withDefaultNamespace("widget/text_field");
   public static final ResourceLocation TEXT_FIELD_HIGHLIGHTED = ResourceLocation.withDefaultNamespace("widget/text_field_highlighted");
   public static final ResourceLocation CHECKBOX = ResourceLocation.withDefaultNamespace("widget/checkbox");
   public static final ResourceLocation CHECKBOX_SELECTED = ResourceLocation.withDefaultNamespace("widget/checkbox_selected");
   public static final ResourceLocation CHECKBOX_HIGHLIGHTED = ResourceLocation.withDefaultNamespace("widget/checkbox_highlighted");
   public static final ResourceLocation CHECKBOX_SELECTED_HIGHLIGHTED = ResourceLocation.withDefaultNamespace("widget/checkbox_selected_highlighted");
   public static final ResourceLocation SCROLLER = ResourceLocation.withDefaultNamespace("widget/scroller");
   public static final ResourceLocation SCROLLER_BACKGROUND = ResourceLocation.withDefaultNamespace("widget/scroller_background");
   public static final ResourceLocation TAB = ResourceLocation.withDefaultNamespace("widget/tab");
   public static final ResourceLocation TAB_SELECTED = ResourceLocation.withDefaultNamespace("widget/tab_selected");
   public static final ResourceLocation TAB_HIGHLIGHTED = ResourceLocation.withDefaultNamespace("widget/tab_highlighted");
   public static final ResourceLocation TAB_SELECTED_HIGHLIGHTED = ResourceLocation.withDefaultNamespace("widget/tab_selected_highlighted");
   public static final ResourceLocation MENU_BACKGROUND = ResourceLocation.withDefaultNamespace("textures/gui/menu_background.png");
   public static final ResourceLocation INWORLD_MENU_BACKGROUND = ResourceLocation.withDefaultNamespace("textures/gui/inworld_menu_background.png");
   public static final ResourceLocation MENU_LIST_BACKGROUND = ResourceLocation.withDefaultNamespace("textures/gui/menu_list_background.png");
   public static final ResourceLocation INWORLD_MENU_LIST_BACKGROUND = ResourceLocation.withDefaultNamespace("textures/gui/inworld_menu_list_background.png");
   public static final ResourceLocation HEADER_SEPARATOR = ResourceLocation.withDefaultNamespace("textures/gui/header_separator.png");
   public static final ResourceLocation FOOTER_SEPARATOR = ResourceLocation.withDefaultNamespace("textures/gui/footer_separator.png");
   public static final ResourceLocation INWORLD_HEADER_SEPARATOR = ResourceLocation.withDefaultNamespace("textures/gui/inworld_header_separator.png");
   public static final ResourceLocation INWORLD_FOOTER_SEPARATOR = ResourceLocation.withDefaultNamespace("textures/gui/inworld_footer_separator.png");
   private static final String ELLIPSIS = "...";
   private static final int FLOAT_SHADE = -300937456;

   public static Font font() {
      return Minecraft.getInstance().font;
   }

   public static void rect(GuiGraphics g, int x, int y, int w, int h, int color) {
      if (w > 0 && h > 0 && Colors.alpha(color) != 0) {
         g.fill(x, y, x + w, y + h, color);
      }
   }

   public static void pixelRound(GuiGraphics g, int x, int y, int w, int h, int color, int radius) {
      if (w > 0 && h > 0 && Colors.alpha(color) != 0) {
         int r = Math.max(0, Math.min(radius, Math.min(w, h) / 2));
         if (r == 0) {
            g.fill(x, y, x + w, y + h, color);
         } else if (!SmoothCorners.active() || !SmoothCorners.fill(g, x, y, w, h, color, r)) {
            g.fill(x, y + r, x + w, y + h - r, color);

            for(int i = 0; i < r; ++i) {
               int inset = r - i;
               g.fill(x + inset, y + i, x + w - inset, y + i + 1, color);
               g.fill(x + inset, y + h - 1 - i, x + w - inset, y + h - i, color);
            }

         }
      }
   }

   public static void outline(GuiGraphics g, int x, int y, int w, int h, int color, int radius) {
      if (w > 0 && h > 0 && Colors.alpha(color) != 0) {
         int r = Math.max(0, Math.min(radius, Math.min(w, h) / 2));
         if (r <= 0 || !SmoothCorners.active() || !SmoothCorners.outline(g, x, y, w, h, color, r)) {
            g.fill(x + r, y, x + w - r, y + 1, color);
            g.fill(x + r, y + h - 1, x + w - r, y + h, color);
            g.fill(x, y + r, x + 1, y + h - r, color);
            g.fill(x + w - 1, y + r, x + w, y + h - r, color);

            for(int i = 1; i <= r; ++i) {
               int px = r - i;
               int py = i - 1;
               g.fill(x + px, y + py + 1, x + px + 1, y + py + 2, color);
               g.fill(x + w - 1 - px, y + py + 1, x + w - px, y + py + 2, color);
               g.fill(x + px, y + h - 2 - py, x + px + 1, y + h - 1 - py, color);
               g.fill(x + w - 1 - px, y + h - 2 - py, x + w - px, y + h - 1 - py, color);
            }

         }
      }
   }

   public static void panel(GuiGraphics g, int x, int y, int w, int h, int fill, int border) {
      int r = Theme.current().radius();
      pixelRound(g, x, y, w, h, fill, r);
      outline(g, x, y, w, h, border, r);
   }

   public static void shadow(GuiGraphics g, int x, int y, int w, int h, float alpha) {
      if (!(alpha <= 0.01F)) {
         shadow(g, x, y, w, h, Colors.scaleAlpha(Theme.current().palette().shadow(), alpha), Theme.current().radius());
      }
   }

   public static void shadow(GuiGraphics g, int x, int y, int w, int h, int c, int radius) {
      if (Colors.alpha(c) != 0 && w > 0 && h > 0) {
         int r = Math.max(0, Math.min(radius, Math.min(w, h) / 2));
         if (r <= 0 || !SmoothCorners.active() || !SmoothCorners.shadow(g, x, y, w, h, c, r)) {
            int runY = y + 2;
            int l0 = 0;
            int r0 = 0;
            int l1 = 0;
            int r1 = 0;

            for(int yy = y + 2; yy <= y + h + 2; ++yy) {
               int a0 = 0;
               int b0 = 0;
               int a1 = 0;
               int b1 = 0;
               if (yy < y + h + 2) {
                  int so = cornerInset(yy - y - 2, h, r);
                  int ox0 = x + 2 + so;
                  int ox1 = x + 2 + w - so;
                  if (yy < y + h) {
                     int sp = cornerInset(yy - y, h, r);
                     int px0 = x + sp;
                     int px1 = x + w - sp;
                     if (ox0 < px0) {
                        a0 = ox0;
                        b0 = Math.min(ox1, px0);
                     }

                     if (ox1 > px1) {
                        a1 = Math.max(ox0, px1);
                        b1 = ox1;
                     }
                  } else {
                     a1 = ox0;
                     b1 = ox1;
                  }
               }

               if (yy == y + h + 2 || a0 != l0 || b0 != r0 || a1 != l1 || b1 != r1) {
                  if (yy > runY) {
                     if (r0 > l0) {
                        g.fill(l0, runY, r0, yy, c);
                     }

                     if (r1 > l1) {
                        g.fill(l1, runY, r1, yy, c);
                     }
                  }

                  runY = yy;
                  l0 = a0;
                  r0 = b0;
                  l1 = a1;
                  r1 = b1;
               }
            }

         }
      }
   }

   private static int cornerInset(int i, int h, int r) {
      if (i < r) {
         return r - i;
      } else {
         return i >= h - r ? r - (h - 1 - i) : 0;
      }
   }

   public static void underlay(GuiGraphics g, int x, int y, int w, int h, int radius, float alpha) {
      if (!Frost.under(g, x, y, w, h, radius, alpha)) {
         pixelRound(g, x, y, w, h, Colors.scaleAlpha(-300937456, alpha), radius);
      }

   }

   public static void plate(GuiGraphics g, int x, int y, int w, int h, int fill, int border, float shadow, float alpha) {
      plate(g, x, y, w, h, fill, border, shadow, alpha, false);
   }

   public static void popupPlate(GuiGraphics g, int x, int y, int w, int h, int fill, int border, float shadow, float alpha) {
      plate(g, x, y, w, h, fill, border, shadow, alpha, true);
   }

   private static void plate(GuiGraphics g, int x, int y, int w, int h, int fill, int border, float shadow, float alpha, boolean floating) {
      if (!(alpha <= 0.004F) && w > 0 && h > 0) {
         Theme t = Theme.current();
         if (floating && t.panelAlpha() < 255) {
            underlay(g, x, y, w, h, t.radius(), alpha);
         }

         if (shadow > 0.0F) {
            shadow(g, x, y, w, h, shadow * alpha);
         }

         pixelRound(g, x, y, w, h, Colors.scaleAlpha(t.panel(fill), alpha), t.radius());
         outline(g, x, y, w, h, Colors.scaleAlpha(border, alpha), t.radius());
      }
   }

   public static void pixelCircle(GuiGraphics g, int cx, int cy, int r, int color) {
      if (r >= 0 && Colors.alpha(color) != 0) {
         if (r == 0) {
            g.fill(cx, cy, cx + 1, cy + 1, color);
         } else if (!SmoothCorners.circles() || !SmoothCorners.disc(g, cx, cy, r, color)) {
            float rr = (float)r + 0.5F;

            for(int dy = -r; dy <= r; ++dy) {
               int dx = (int)Math.floor(Math.sqrt((double)(rr * rr - (float)(dy * dy))));
               g.fill(cx - dx, cy + dy, cx + dx + 1, cy + dy + 1, color);
            }

         }
      }
   }

   public static void pixelRing(GuiGraphics g, int cx, int cy, int r, int color) {
      if (r > 0 && Colors.alpha(color) != 0) {
         if (!SmoothCorners.circles() || !SmoothCorners.ring(g, cx, cy, r, color)) {
            float rr = (float)r + 0.5F;
            int prev = -1;

            for(int dy = -r; dy <= r; ++dy) {
               int dx = (int)Math.floor(Math.sqrt((double)(rr * rr - (float)(dy * dy))));
               int from = prev < 0 ? dx : Math.min(dx, prev + 1);
               g.fill(cx - dx, cy + dy, cx - from + 1, cy + dy + 1, color);
               g.fill(cx + from, cy + dy, cx + dx + 1, cy + dy + 1, color);
               prev = dx;
            }

         }
      }
   }

   public static void hgradient(GuiGraphics g, int x, int y, int w, int h, int left, int right) {
      if (w > 0 && h > 0) {
         for(int i = 0; i < w; i += 4) {
            int c = Colors.lerp(left, right, (float)i / (float)Math.max(1, w - 1));
            g.fill(x + i, y, Math.min(x + w, x + i + 4), y + h, c);
         }

      }
   }

   public static void vgradient(GuiGraphics g, int x, int y, int w, int h, int top, int bottom) {
      if (w > 0 && h > 0) {
         g.fillGradient(x, y, x + w, y + h, top, bottom);
      }
   }

   public static void hline(GuiGraphics g, int x, int y, int w, int color) {
      if (w > 0) {
         g.fill(x, y, x + w, y + 1, color);
      }
   }

   public static void vline(GuiGraphics g, int x, int y, int h, int color) {
      if (h > 0) {
         g.fill(x, y, x + 1, y + h, color);
      }
   }

   public static void vignette(GuiGraphics g, int x, int y, int w, int h, float strength) {
      if (!(strength <= 0.01F) && w > 0 && h > 0) {
         int a = Math.round(255.0F * Mth.clamp(strength, 0.0F, 1.0F));
         int edge = Colors.withAlpha(0, a);
         int clear = 0;
         int vh = Math.max(8, h / 4);
         int hw = Math.max(8, w / 8);
         vgradient(g, x, y, w, vh, edge, 0);
         vgradient(g, x, y + h - vh, w, vh, 0, edge);
         hgradient(g, x, y, hw, h, Colors.withAlpha(0, a / 2), 0);
         hgradient(g, x + w - hw, y, hw, h, 0, Colors.withAlpha(0, a / 2));
      }
   }

   public static int textY(int y, int h) {
      return y + (h - 9) / 2 + 1;
   }

   public static void text(GuiGraphics g, Component text, int x, int y, int color) {
      g.drawString(font(), text, x, y, color, false);
   }

   public static void text(GuiGraphics g, String text, int x, int y, int color) {
      g.drawString(font(), text, x, y, color, false);
   }

   public static void text(GuiGraphics g, FormattedCharSequence text, int x, int y, int color) {
      g.drawString(font(), text, x, y, color, false);
   }

   public static void text(GuiGraphics g, Component text, int x, int y, int color, boolean shadow) {
      g.drawString(font(), text, x, y, color, shadow);
   }

   public static void textShadow(GuiGraphics g, Component text, int x, int y, int color) {
      g.drawString(font(), text, x, y, color, true);
   }

   public static void textCentered(GuiGraphics g, Component text, int cx, int y, int color) {
      FormattedCharSequence seq = text.getVisualOrderText();
      g.drawString(font(), seq, cx - font().width(seq) / 2, y, color, false);
   }

   public static void textCentered(GuiGraphics g, String text, int cx, int y, int color) {
      g.drawString(font(), text, cx - font().width(text) / 2, y, color, false);
   }

   public static void textCentered(GuiGraphics g, Component text, int cx, int y, int color, boolean shadow) {
      FormattedCharSequence seq = text.getVisualOrderText();
      g.drawString(font(), seq, cx - font().width(seq) / 2, y, color, shadow);
   }

   public static void textRight(GuiGraphics g, Component text, int rightX, int y, int color) {
      FormattedCharSequence seq = text.getVisualOrderText();
      g.drawString(font(), seq, rightX - font().width(seq), y, color, false);
   }

   public static void textRight(GuiGraphics g, Component text, int rightX, int y, int color, boolean shadow) {
      FormattedCharSequence seq = text.getVisualOrderText();
      g.drawString(font(), seq, rightX - font().width(seq), y, color, shadow);
   }

   public static FormattedCharSequence truncate(Component text, int width) {
      Font f = font();
      if (f.width(text) <= width) {
         return text.getVisualOrderText();
      } else if (width < f.width("...")) {
         return FormattedCharSequence.EMPTY;
      } else {
         FormattedText cut = f.substrByWidth(text, width - f.width("..."));
         return Language.getInstance().getVisualOrder(FormattedText.composite(new FormattedText[]{cut, FormattedText.of("...", text.getStyle())}));
      }
   }

   public static FormattedCharSequence truncateWords(Component text, int width) {
      Font f = font();
      if (f.width(text) <= width) {
         return text.getVisualOrderText();
      } else {
         String s = text.getString();
         int room = width - f.width("...");
         int cut = -1;

         for(int i = s.indexOf(32); i > 0; i = s.indexOf(32, i + 1)) {
            String head = stripDots(s.substring(0, i));
            if (head.isEmpty() || f.width(head) > room) {
               break;
            }

            cut = i;
         }

         if (cut >= 0) {
            return Language.getInstance().getVisualOrder(FormattedText.of(stripDots(s.substring(0, cut)) + "...", text.getStyle()));
         } else {
            int space = s.indexOf(32);
            String first = stripDots(space > 0 ? s.substring(0, space) : s);
            return !first.isEmpty() && f.width(first) <= width ? Language.getInstance().getVisualOrder(FormattedText.of(first, text.getStyle())) : truncate(text, width);
         }
      }
   }

   private static String stripDots(String s) {
      int end;
      for(end = s.length(); end > 0 && (s.charAt(end - 1) == '.' || s.charAt(end - 1) == 8230 || Character.isWhitespace(s.charAt(end - 1))); --end) {
      }

      return s.substring(0, end);
   }

   public static void drawScrollingText(GuiGraphics g, Component text, int x, int y, int width, int color, boolean shadow) {
      drawScrollingText(g, text.getVisualOrderText(), x, y, width, color, shadow);
   }

   public static void drawScrollingText(GuiGraphics g, FormattedCharSequence text, int x, int y, int width, int color, boolean shadow) {
      int tw = font().width(text);
      if (tw <= width) {
         g.drawString(font(), text, x, y, color, shadow);
      } else {
         int overflow = tw - width;
         double seconds = (double)Util.getMillis() / (double)1000.0F;
         double period = Math.max((double)overflow * (double)0.5F, (double)3.0F);
         double e = Math.sin((Math.PI / 2D) * Math.cos((Math.PI * 2D) * seconds / period)) / (double)2.0F + (double)0.5F;
         int offset = (int)Math.round(Mth.lerp(e, (double)0.0F, (double)overflow));
         g.enableScissor(x, y - 1, x + width, y + 10);
         g.drawString(font(), text, x - offset, y, color, shadow);
         g.disableScissor();
      }
   }

   public static void drawScrollingTextCentered(GuiGraphics g, Component text, int x, int y, int width, int color, boolean shadow) {
      FormattedCharSequence seq = text.getVisualOrderText();
      int tw = font().width(seq);
      if (tw <= width) {
         g.drawString(font(), seq, x + (width - tw) / 2, y, color, shadow);
      } else {
         drawScrollingText(g, seq, x, y, width, color, shadow);
      }

   }

   public static int width(Component text) {
      return font().width(text);
   }

   public static int width(String text) {
      return font().width(text);
   }

   public static int width(FormattedCharSequence text) {
      return font().width(text);
   }

   public static int lineHeight() {
      Objects.requireNonNull(font());
      return 9;
   }

   public static void blitScaled(GuiGraphics g, ResourceLocation texture, int x, int y, int w, int h, int texW, int texH, float alpha) {
      blit(g, texture, x, y, w, h, 0.0F, 0.0F, texW, texH, texW, texH, alpha);
   }

   public static void blit(GuiGraphics g, ResourceLocation texture, int x, int y, int w, int h, float u, float v, int uw, int vh, int texW, int texH, float alpha) {
      if (!(alpha <= 0.004F) && w > 0 && h > 0) {
         RenderSystem.enableBlend();
         g.setColor(1.0F, 1.0F, 1.0F, alpha);
         g.blit(texture, x, y, w, h, u, v, uw, vh, texW, texH);
         g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
         RenderSystem.disableBlend();
      }
   }

   public static void blitFit(GuiGraphics g, ResourceLocation texture, int x, int y, int w, int h, int texW, int texH, boolean cover, float alpha) {
      if (texW > 0 && texH > 0 && w > 0 && h > 0) {
         float s = cover ? Math.max((float)w / (float)texW, (float)h / (float)texH) : Math.min((float)w / (float)texW, (float)h / (float)texH);
         int dw = Math.max(1, Math.round((float)texW * s));
         int dh = Math.max(1, Math.round((float)texH * s));
         int dx = x + (w - dw) / 2;
         int dy = y + (h - dh) / 2;
         if (cover) {
            g.enableScissor(x, y, x + w, y + h);
         }

         blitScaled(g, texture, dx, dy, dw, dh, texW, texH, alpha);
         if (cover) {
            g.disableScissor();
         }

      }
   }

   public static void playerHead(GuiGraphics g, ResourceLocation skin, int x, int y, int size, float alpha) {
      if (!(alpha <= 0.004F)) {
         RenderSystem.enableBlend();
         g.setColor(1.0F, 1.0F, 1.0F, alpha);
         PlayerFaceRenderer.draw(g, skin, x, y, size, true, false);
         g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
         RenderSystem.disableBlend();
      }
   }

   public static void vanillaButton(GuiGraphics g, int x, int y, int w, int h, float hover, boolean enabled, float alpha) {
      vanillaFace(g, BUTTON, BUTTON_HIGHLIGHTED, BUTTON_DISABLED, x, y, w, h, hover, enabled, alpha);
   }

   public static void vanillaFace(GuiGraphics g, ResourceLocation base, ResourceLocation highlighted, ResourceLocation disabled, int x, int y, int w, int h, float hover, boolean enabled, float alpha) {
      if (!(alpha <= 0.004F) && w > 0 && h > 0) {
         RenderSystem.enableBlend();
         g.setColor(1.0F, 1.0F, 1.0F, alpha);
         if (!enabled && disabled != null) {
            g.blitSprite(disabled, x, y, w, h);
         } else {
            g.blitSprite(base, x, y, w, h);
            if (hover > 0.01F && highlighted != null) {
               g.setColor(1.0F, 1.0F, 1.0F, alpha * Math.min(1.0F, hover));
               g.blitSprite(highlighted, x, y, w, h);
            }
         }

         g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
         RenderSystem.disableBlend();
      }
   }

   public static void vanillaSlider(GuiGraphics g, int x, int y, int w, int h, int handleX, float hover, boolean enabled, float alpha) {
      vanillaFace(g, SLIDER, SLIDER_HIGHLIGHTED, (ResourceLocation)null, x, y, w, h, enabled ? hover : 0.0F, true, alpha);
      vanillaFace(g, SLIDER_HANDLE, SLIDER_HANDLE_HIGHLIGHTED, (ResourceLocation)null, handleX, y, 8, h, enabled ? hover : 0.0F, true, alpha);
   }

   public static void vanillaCheckbox(GuiGraphics g, int x, int y, int size, boolean selected, float hover, float alpha) {
      vanillaFace(g, selected ? CHECKBOX_SELECTED : CHECKBOX, selected ? CHECKBOX_SELECTED_HIGHLIGHTED : CHECKBOX_HIGHLIGHTED, (ResourceLocation)null, x, y, size, size, hover, true, alpha);
   }

   public static void vanillaTextField(GuiGraphics g, int x, int y, int w, int h, boolean focused) {
      RenderSystem.enableBlend();
      g.blitSprite(focused ? TEXT_FIELD_HIGHLIGHTED : TEXT_FIELD, x, y, w, h);
      RenderSystem.disableBlend();
   }

   public static void vanillaTab(GuiGraphics g, int x, int y, int w, int h, boolean selected, float hover, float alpha) {
      vanillaFace(g, selected ? TAB_SELECTED : TAB, selected ? TAB_SELECTED_HIGHLIGHTED : TAB_HIGHLIGHTED, (ResourceLocation)null, x, y, w, h, hover, true, alpha);
   }

   public static void vanillaPanel(GuiGraphics g, int x, int y, int w, int h, boolean inWorld) {
      RenderSystem.enableBlend();
      g.blit(inWorld ? INWORLD_MENU_BACKGROUND : MENU_BACKGROUND, x, y, 0.0F, 0.0F, w, h, 32, 32);
      RenderSystem.disableBlend();
   }

   public static void vanillaListBackground(GuiGraphics g, int x, int y, int w, int h, boolean inWorld) {
      RenderSystem.enableBlend();
      g.blit(inWorld ? INWORLD_MENU_LIST_BACKGROUND : MENU_LIST_BACKGROUND, x, y, (float)x, (float)y, w, h, 32, 32);
      RenderSystem.disableBlend();
   }

   public static void vanillaSeparator(GuiGraphics g, int x, int y, int w, boolean header, boolean inWorld) {
      RenderSystem.enableBlend();
      ResourceLocation tex = inWorld ? (header ? INWORLD_HEADER_SEPARATOR : INWORLD_FOOTER_SEPARATOR) : (header ? HEADER_SEPARATOR : FOOTER_SEPARATOR);
      g.blit(tex, x, y, 0.0F, 0.0F, w, 2, 32, 2);
      RenderSystem.disableBlend();
   }

   public static void vanillaDialog(GuiGraphics g, int x, int y, int w, int h, float alpha) {
      rect(g, x, y, w, h, Colors.withAlpha(0, Math.round(224.0F * alpha)));
      RenderSystem.enableBlend();
      g.setColor(1.0F, 1.0F, 1.0F, alpha);
      g.blit(MENU_BACKGROUND, x, y, 0.0F, 0.0F, w, h, 32, 32);
      g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.disableBlend();
      outline(g, x, y, w, h, Colors.withAlpha(0, Math.round(255.0F * alpha)), 0);
      outline(g, x + 1, y + 1, w - 2, h - 2, Colors.withAlpha(9145227, Math.round(255.0F * alpha)), 0);
   }

   public static void dim(GuiGraphics g, Screen screen, int color) {
      g.fill(0, 0, screen.width, screen.height, color);
   }

   public static void dim(GuiGraphics g, int w, int h, int color) {
      g.fill(0, 0, w, h, color);
   }

   public static boolean focusShown() {
      return Minecraft.getInstance().getLastInputType().isKeyboard();
   }

   public static void focusRing(GuiGraphics g, int x, int y, int w, int h, float alpha) {
      if (!(alpha <= 0.02F) && focusShown()) {
         outline(g, x - 1, y - 1, w + 2, h + 2, Colors.scaleAlpha(Theme.current().accent(), alpha), Theme.current().radius() + 1);
      }
   }

   public static void darkFace(GuiGraphics g, int x, int y, int w, int h, float hover, float press, boolean enabled, float alpha) {
      Theme t = Theme.current();
      Palette p = t.palette();
      int fill = Colors.lerp(p.surface(), p.surfaceHover(), hover);
      int border = Colors.lerp(p.border(), p.borderStrong(), hover);
      if (!enabled) {
         fill = Colors.withAlpha(p.surface(), 128);
         border = Colors.withAlpha(p.border(), 128);
      }

      fill = Colors.brighten(fill, -0.12F * press);
      shadow(g, x, y, w, h, 0.35F * alpha * (1.0F - press));
      pixelRound(g, x, y, w, h, Colors.scaleAlpha(fill, alpha), t.radius());
      outline(g, x, y, w, h, Colors.scaleAlpha(border, alpha), t.radius());
   }

   public static void floatingPanel(GuiGraphics g, int x, int y, int w, int h, float alpha) {
      if (!(alpha <= 0.004F) && w > 0 && h > 0) {
         Theme t = Theme.current();
         Palette p = t.palette();
         if (t.isVanilla()) {
            rect(g, x, y, w, h, Colors.scaleAlpha(-1207959552, alpha));
            outline(g, x, y, w, h, Colors.scaleAlpha(-16777216, alpha), 0);
            outline(g, x + 1, y + 1, w - 2, h - 2, Colors.scaleAlpha(1224736767, alpha), 0);
         } else {
            int r = t.radius();
            if (t.panelAlpha() < 255) {
               underlay(g, x, y, w, h, r, alpha);
            }

            shadow(g, x, y, w, h, 0.6F * alpha);
            pixelRound(g, x, y, w, h, Colors.scaleAlpha(t.panel(p.bg()), alpha), r);
            hline(g, x + r + 1, y + 1, w - 2 * r - 2, Colors.scaleAlpha(352321535, alpha));
            outline(g, x, y, w, h, Colors.scaleAlpha(p.border(), alpha), r);
         }
      }
   }

   public static void sectionRule(GuiGraphics g, Component caption, int x, int y, int w, float alpha) {
      if (!(alpha <= 0.004F) && w > 0) {
         Theme t = Theme.current();
         Palette p = t.palette();
         boolean van = t.isVanilla();
         Component text = Fonts.heading(caption);
         FormattedCharSequence seq = truncate(text, w);
         g.drawString(font(), seq, x, y + 1, Colors.scaleAlpha(van ? -1 : p.textMuted(), alpha), van);
         int lx = x + font().width(seq) + 6;
         hline(g, lx, y + 5, x + w - lx, Colors.scaleAlpha(van ? 1090519039 : p.border(), alpha));
      }
   }

   public static void accentCap(GuiGraphics g, int x, int y, int w, float alpha) {
      if (!(alpha <= 0.004F) && w > 0) {
         int accent = Theme.current().accent();
         hgradient(g, x, y, w, 2, Colors.scaleAlpha(accent, alpha), Colors.scaleAlpha(Colors.withAlpha(accent, 48), alpha));
      }
   }

   public static int chip(GuiGraphics g, Component label, int x, int y, int color, float alpha) {
      int w = width(label) + 8;
      if (alpha <= 0.004F) {
         return w;
      } else {
         Theme t = Theme.current();
         if (t.isVanilla()) {
            rect(g, x, y - 1, w, 14, Colors.scaleAlpha(Integer.MIN_VALUE, alpha));
            outline(g, x, y - 1, w, 14, Colors.scaleAlpha(Colors.withAlpha(color, 144), alpha), 0);
         } else {
            pixelRound(g, x, y - 1, w, 13, Colors.scaleAlpha(Colors.withAlpha(color, 42), alpha), 2);
            outline(g, x, y - 1, w, 13, Colors.scaleAlpha(Colors.withAlpha(color, 128), alpha), 2);
         }

         g.drawString(font(), label, x + 4, y + 2, Colors.scaleAlpha(t.isVanilla() ? -1 : Colors.brighten(color, 0.3F), alpha), t.isVanilla());
         return w;
      }
   }

   public static void scissor(GuiGraphics g, int x, int y, int w, int h) {
      g.enableScissor(x, y, x + Math.max(0, w), y + Math.max(0, h));
   }

   public static void unscissor(GuiGraphics g) {
      g.disableScissor();
   }

   private CFHGuiDraw() {
   }
}
