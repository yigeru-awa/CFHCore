package com.onehumanawa.cfhcore.content.gui.core.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.ImageFiles;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.layout.ElementType;
import com.onehumanawa.cfhcore.content.gui.core.layout.ElementTypes;
import com.onehumanawa.cfhcore.content.gui.core.layout.Placeholders;
import com.onehumanawa.cfhcore.content.gui.core.layout.ScreenLayout;
import com.onehumanawa.cfhcore.content.gui.core.layout.action.ActionType;
import com.onehumanawa.cfhcore.content.gui.core.layout.action.Actions;
import com.onehumanawa.cfhcore.content.gui.core.platform.CFHGuiPlatform;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiAvatar;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiIconButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiLabel;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSeparator;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiWidget;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public final class CoreElements {
   private static Component text(ScreenLayout.Element e, String key, String def) {
      return Component.literal(Placeholders.apply((String)e.props.getOrDefault(key, def)));
   }

   private static Icon icon(ScreenLayout.Element e, String key, Icon def) {
      try {
         return Icon.valueOf(((String)e.props.getOrDefault(key, def.name())).toUpperCase(Locale.ROOT));
      } catch (IllegalArgumentException var4) {
         return def;
      }
   }

   static void registerAll() {
      ElementTypes.register(new Simple("cfhgui:button", "Button", Icon.TOGGLE, new int[]{100, 20}, List.of(ActionType.Arg.text("label", Component.literal("Label"), "Button"), ActionType.Arg.choice("variant", Component.literal("Style"), "SECONDARY", List.of("PRIMARY", "SECONDARY", "GHOST", "DANGER")), ActionType.Arg.text("icon", Component.literal("Icon (name or empty)"), ""), ActionType.Arg.text("tooltip", Component.literal("Tooltip"), "")), (screen, e, x, y, w, h, run) -> {
         CFHGuiButton b = new CFHGuiButton(x, y, w, h, text(e, "label", "Button"), run);

         try {
            b.variant(CFHGuiButton.Variant.valueOf(((String)e.props.getOrDefault("variant", "SECONDARY")).toUpperCase(Locale.ROOT)));
         } catch (IllegalArgumentException var10) {
         }

         String ic = (String)e.props.getOrDefault("icon", "");
         if (!ic.isEmpty()) {
            b.icon(icon(e, "icon", Icon.DOT));
         }

         String tip = (String)e.props.getOrDefault("tooltip", "");
         if (!tip.isEmpty()) {
            b.tip(text(e, "tooltip", ""));
         }

         return b;
      }));
      ElementTypes.register(new Simple("cfhgui:icon_button", "Icon button", Icon.STAR, new int[]{20, 20}, List.of(ActionType.Arg.text("icon", Component.literal("Icon"), "STAR"), ActionType.Arg.text("tooltip", Component.literal("Tooltip"), "")), (screen, e, x, y, w, h, run) -> new CFHGuiIconButton(x, y, Math.min(w, h), icon(e, "icon", Icon.STAR), text(e, "tooltip", ""), run)));
      ElementTypes.register(new Simple("cfhgui:label", "Text", Icon.TEXT, new int[]{120, 10}, List.of(ActionType.Arg.multiline("text", Component.literal("Text ({player}, {fps}...)"), "Hello {player}"), ActionType.Arg.choice("style", Component.literal("Style"), "BODY", List.of("HEADING", "TITLE", "BODY", "MUTED", "CAPTION")), ActionType.Arg.choice("align", Component.literal("Align"), "LEFT", List.of("LEFT", "CENTER", "RIGHT")), ActionType.Arg.text("color", Component.literal("Colour (#hex, empty = theme)"), ""), ActionType.Arg.bool("wrap", Component.literal("Wrap"), false)), (screen, e, x, y, w, h, run) -> {
         CFHGuiLabel l = new LiveLabel(x, y, w, e);

         try {
            l.style(CFHGuiLabel.Style.valueOf(((String)e.props.getOrDefault("style", "BODY")).toUpperCase(Locale.ROOT)));
         } catch (IllegalArgumentException var10) {
         }

         try {
            l.align(CFHGuiLabel.Align.valueOf(((String)e.props.getOrDefault("align", "LEFT")).toUpperCase(Locale.ROOT)));
         } catch (IllegalArgumentException var9) {
         }

         int c = Colors.fromHex((String)e.props.getOrDefault("color", ""), 0);
         if (c != 0) {
            l.color(c);
         }

         l.wrap(Boolean.parseBoolean((String)e.props.getOrDefault("wrap", "false")));
         return l;
      }) {
         public boolean clickable() {
            return false;
         }
      });
      ElementTypes.register(new Simple("cfhgui:image", "Image", Icon.IMAGE, new int[]{64, 64}, List.of(ActionType.Arg.text("source", Component.literal("File (game dir) or namespace:path"), "config/cfhgui/images/logo.png"), ActionType.Arg.choice("fit", Component.literal("Fit"), "CONTAIN", List.of("CONTAIN", "COVER", "STRETCH")), ActionType.Arg.number("alpha", Component.literal("Opacity 0-1"), "1")), (screen, e, x, y, w, h, run) -> new ImageElement(x, y, w, h, e, run)));
      ElementTypes.register(new Simple("cfhgui:panel", "Panel", Icon.PANEL, new int[]{120, 80}, List.of(ActionType.Arg.text("fill", Component.literal("Fill (#hex, empty = surface)"), ""), ActionType.Arg.text("border", Component.literal("Border (#hex, empty = theme)"), ""), ActionType.Arg.number("alpha", Component.literal("Opacity 0-1"), "1")), (screen, e, x, y, w, h, run) -> new PanelElement(x, y, w, h, e)) {
         public boolean clickable() {
            return false;
         }
      });
      ElementTypes.register(new Simple("cfhgui:separator", "Separator", Icon.MINUS, new int[]{120, 1}, List.of(ActionType.Arg.text("caption", Component.literal("Caption"), ""), ActionType.Arg.bool("vertical", Component.literal("Vertical"), false)), (screen, e, x, y, w, h, run) -> {
         String cap = (String)e.props.getOrDefault("caption", "");
         if (Boolean.parseBoolean((String)e.props.getOrDefault("vertical", "false"))) {
            return new CFHGuiSeparator(x, y, h, true);
         } else {
            return cap.isEmpty() ? new CFHGuiSeparator(x, y, w, false) : new CFHGuiSeparator(x, y, w, Component.literal(cap));
         }
      }) {
         public boolean clickable() {
            return false;
         }
      });
      ElementTypes.register(new Simple("cfhgui:player_head", "Player head", Icon.USER, new int[]{32, 32}, List.of(ActionType.Arg.text("player", Component.literal("Player name (empty = you)"), "")), (screen, e, x, y, w, h, run) -> {
         String name = (String)e.props.getOrDefault("player", "");
         Minecraft mc = Minecraft.getInstance();
         CFHGuiAvatar a = name.isEmpty() ? new CFHGuiAvatar(x, y, Math.min(w, h), mc.getGameProfile()) : new CFHGuiAvatar(x, y, Math.min(w, h), UUIDUtil.createOfflinePlayerUUID(name), name);
         if (!e.actions.isEmpty()) {
            a.onClick(run);
         }

         return a;
      }));
      ElementTypes.register(new Simple("cfhgui:link", "Link", Icon.LINK, new int[]{100, 10}, List.of(ActionType.Arg.text("label", Component.literal("Label"), "Website"), ActionType.Arg.text("url", Component.literal("URL"), "https://")), (screen, e, x, y, w, h, run) -> {
         CFHGuiButton b = (new CFHGuiButton(x, y, w, h, text(e, "label", "Link"), () -> {
            Actions.run("cfhgui:open_url", Map.of("url", (String)e.props.getOrDefault("url", ""), "confirm", "true"));
            run.run();
         })).variant(CFHGuiButton.Variant.GHOST).leftAligned();
         b.icon(Icon.EXTERNAL);
         return b;
      }));
      ElementTypes.register(new Simple("cfhgui:spacer", "Spacer", Icon.MOVE, new int[]{20, 20}, List.of(), (screen, e, x, y, w, h, run) -> new PanelElement(x, y, w, h, e, true)) {
         public boolean clickable() {
            return false;
         }
      });
   }

   private CoreElements() {
   }

   static class Simple implements ElementType {
      private final String id;
      private final String label;
      private final Icon icon;
      private final int[] size;
      private final List<ActionType.Arg> props;
      private final Factory factory;

      Simple(String id, String label, Icon icon, int[] size, List<ActionType.Arg> props, Factory factory) {
         this.id = id;
         this.label = label;
         this.icon = icon;
         this.size = size;
         this.props = props;
         this.factory = factory;
      }

      public String id() {
         return this.id;
      }

      public Component label() {
         return Component.literal(this.label);
      }

      public Icon icon() {
         return this.icon;
      }

      public int[] defaultSize() {
         return this.size;
      }

      public List<ActionType.Arg> props() {
         return this.props;
      }

      public AbstractWidget create(Screen screen, ScreenLayout.Element element, int x, int y, int w, int h, Runnable runActions) {
         return this.factory.create(screen, element, x, y, w, h, runActions);
      }
   }

   static final class LiveLabel extends CFHGuiLabel {
      private final ScreenLayout.Element e;
      private String lastText = "";

      LiveLabel(int x, int y, int w, ScreenLayout.Element e) {
         super(x, y, w, Component.literal(Placeholders.apply((String)e.props.getOrDefault("text", ""))));
         this.e = e;
      }

      protected void renderDark(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
         this.refresh();
         super.renderDark(g, mouseX, mouseY, partialTick);
      }

      protected void renderVanilla(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
         this.refresh();
         super.renderVanilla(g, mouseX, mouseY, partialTick);
      }

      private void refresh() {
         String t = Placeholders.apply((String)this.e.props.getOrDefault("text", ""));
         if (!t.equals(this.lastText)) {
            this.lastText = t;
            this.text(Component.literal(t));
         }

      }
   }

   static final class PanelElement extends CFHGuiWidget {
      private final ScreenLayout.Element e;
      private final boolean invisible;

      PanelElement(int x, int y, int w, int h, ScreenLayout.Element e) {
         this(x, y, w, h, e, false);
      }

      PanelElement(int x, int y, int w, int h, ScreenLayout.Element e, boolean invisible) {
         super(x, y, w, h, Component.empty());
         this.e = e;
         this.invisible = invisible;
         this.active = false;
      }

      public boolean mouseClicked(double mouseX, double mouseY, int button) {
         return false;
      }

      public ComponentPath nextFocusPath(FocusNavigationEvent event) {
         return null;
      }

      protected void renderDark(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
         if (!this.invisible) {
            float alpha = 1.0F;

            try {
               alpha = Float.parseFloat((String)this.e.props.getOrDefault("alpha", "1"));
            } catch (NumberFormatException var10) {
            }

            Theme t = Theme.current();
            String own = (String)this.e.props.getOrDefault("fill", "");
            int fill = own.isBlank() ? t.panel(t.surface()) : Colors.fromHex(own, t.surface());
            int border = Colors.fromHex((String)this.e.props.getOrDefault("border", ""), t.border());
            CFHGuiDraw.panel(g, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Colors.scaleAlpha(fill, alpha), Colors.scaleAlpha(border, alpha));
         }
      }

      protected void renderVanilla(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
         if (!this.invisible) {
            float alpha = 1.0F;

            try {
               alpha = Float.parseFloat((String)this.e.props.getOrDefault("alpha", "1"));
            } catch (NumberFormatException var7) {
            }

            int fill = Colors.fromHex((String)this.e.props.getOrDefault("fill", ""), -16777216);
            g.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), Colors.scaleAlpha(Colors.withAlpha(fill, 144), alpha));
            CFHGuiDraw.outline(g, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Colors.scaleAlpha(-7631989, alpha), 0);
         }
      }
   }

   static final class ImageElement extends CFHGuiWidget {
      private final ScreenLayout.Element e;
      private final Runnable run;
      private @Nullable Path file;
      private ResourceLocation resource;
      private boolean requested;

      ImageElement(int x, int y, int w, int h, ScreenLayout.Element e, Runnable run) {
         super(x, y, w, h, Component.empty());
         this.e = e;
         this.run = run;
         this.active = !e.actions.isEmpty();
         this.silent();
      }

      public void onClick(double mouseX, double mouseY) {
         super.onClick(mouseX, mouseY);
         this.run.run();
      }

      private void ensure() {
         if (!this.requested) {
            this.requested = true;
            String src = (String)this.e.props.getOrDefault("source", "");
            if ((!src.contains(":") || src.contains("/") || src.contains("\\")) && !src.matches("^[a-z0-9_.-]+:[a-z0-9_./-]+$")) {
               this.file = CFHGuiPlatform.get().gameDir().resolve(src);
            } else {
               this.resource = ResourceLocation.tryParse(src);
            }
         }
      }

      protected void renderDark(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
         this.ensure();
         float alpha = 1.0F;

         try {
            alpha = Float.parseFloat((String)this.e.props.getOrDefault("alpha", "1"));
         } catch (NumberFormatException var18) {
         }

         int x = this.getX();
         int y = this.getY();
         int w = this.getWidth();
         int h = this.getHeight();
         RenderSystem.enableBlend();
         g.setColor(1.0F, 1.0F, 1.0F, alpha * this.effectiveAlpha());
         ImageFiles.Picture picture = this.file == null ? null : ImageFiles.get(this.file);
         ResourceLocation frame = picture == null ? null : picture.frame();
         if (this.resource != null) {
            g.blit(this.resource, x, y, w, h, 0.0F, 0.0F, 256, 256, 256, 256);
         } else if (frame != null) {
            String fit = ((String)this.e.props.getOrDefault("fit", "CONTAIN")).toUpperCase(Locale.ROOT);
            int dw = w;
            int dh = h;
            int dx = x;
            int dy = y;
            if (!"STRETCH".equals(fit)) {
               float s = "COVER".equals(fit) ? Math.max((float)w / (float)picture.width(), (float)h / (float)picture.height()) : Math.min((float)w / (float)picture.width(), (float)h / (float)picture.height());
               dw = Math.round((float)picture.width() * s);
               dh = Math.round((float)picture.height() * s);
               dx = x + (w - dw) / 2;
               dy = y + (h - dh) / 2;
               if ("COVER".equals(fit)) {
                  CFHGuiDraw.scissor(g, x, y, w, h);
               }
            }

            g.blit(frame, dx, dy, dw, dh, 0.0F, 0.0F, 1, 1, 1, 1);
            if ("COVER".equals(fit)) {
               CFHGuiDraw.unscissor(g);
            }
         } else {
            CFHGuiDraw.outline(g, x, y, w, h, Theme.current().border(), 0);
         }

         g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
         RenderSystem.disableBlend();
      }

      protected void renderVanilla(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
         this.renderDark(g, mouseX, mouseY, partialTick);
      }
   }

   @FunctionalInterface
   interface Factory {
      AbstractWidget create(Screen var1, ScreenLayout.Element var2, int var3, int var4, int var5, int var6, Runnable var7);
   }
}
