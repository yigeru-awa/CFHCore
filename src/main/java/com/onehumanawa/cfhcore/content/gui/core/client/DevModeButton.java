package com.onehumanawa.cfhcore.content.gui.core.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.event.CFHGuiEvents;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.layout.LayoutApplier;
import com.onehumanawa.cfhcore.content.gui.core.layout.editor.LayoutEditor;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiIconButton;
import java.util.ArrayList;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public final class DevModeButton {
   private static final int SIZE = 20;
   private static final int MARGIN = 4;

   static void init() {
      CFHGuiEvents.SCREEN_INIT_POST.register(DevModeButton::onScreenInit);
      Theme.onChange(() -> {
         if (RenderSystem.isOnRenderThread()) {
            refresh(Minecraft.getInstance().screen);
         }

      });
   }

   public static Place place() {
      return DevModeButton.Place.parse(CFHGui.config().devPencil);
   }

   private static Component tooltip() {
      return CFHGuiClient.EDITOR_KEY.isUnbound() ? Component.translatable("cfhgui.dev.edit_screen.no_key") : Component.translatable("cfhgui.dev.edit_screen.key", new Object[]{CFHGuiClient.EDITOR_KEY.getTranslatedKeyMessage()});
   }

   private static void onScreenInit(Screen screen) {
      Place place = place();
      if (place != DevModeButton.Place.HIDDEN && CFHGui.config().devMode && !LayoutEditor.isEditing() && LayoutEditor.canEdit(screen)) {
         if (screen instanceof LayoutApplier.ScreenAccess) {
            LayoutApplier.ScreenAccess access = (LayoutApplier.ScreenAccess)screen;
            access.cfhgui$add(new Pencil(x(screen, place), y(screen, place), () -> LayoutEditor.start(screen)));
         }
      }
   }

   private static int x(Screen screen, Place place) {
      return place != DevModeButton.Place.BOTTOM_LEFT && place != DevModeButton.Place.TOP_LEFT ? screen.width - 20 - 4 : 4;
   }

   private static int y(Screen screen, Place place) {
      return place != DevModeButton.Place.TOP_LEFT && place != DevModeButton.Place.TOP_RIGHT ? screen.height - 20 - 4 : 4;
   }

   private static void refresh(@Nullable Screen screen) {
      if (screen != null && screen instanceof LayoutApplier.ScreenAccess access) {
         ArrayList<Pencil> pencils = new ArrayList();

         for(GuiEventListener child : screen.children()) {
            if (child instanceof Pencil p) {
               pencils.add(p);
            }
         }

         Place place = place();
         if (place != DevModeButton.Place.HIDDEN && CFHGui.config().devMode) {
            if (pencils.isEmpty()) {
               if (!LayoutEditor.isEditing()) {
                  onScreenInit(screen);
               }

            } else {
               for(Pencil p : pencils) {
                  p.setPosition(x(screen, place), y(screen, place));
               }

            }
         } else {
            for(Pencil p : pencils) {
               access.cfhgui$remove(p);
            }

         }
      }
   }

   public static void editorToggled(Screen screen, boolean editing) {
      boolean found = false;

      for(GuiEventListener child : screen.children()) {
         if (child instanceof Pencil p) {
            p.visible = !editing;
            found = true;
         }
      }

      if (!editing && !found) {
         onScreenInit(screen);
      }

   }

   private DevModeButton() {
   }

   public static enum Place {
      BOTTOM_RIGHT,
      BOTTOM_LEFT,
      TOP_RIGHT,
      TOP_LEFT,
      HIDDEN;

      public static Place parse(@Nullable String value) {
         if (value != null) {
            for(Place p : values()) {
               if (p.name().equalsIgnoreCase(value.trim())) {
                  return p;
               }
            }
         }

         return BOTTOM_RIGHT;
      }

      public String key() {
         return this.name().toLowerCase(Locale.ROOT);
      }

      public Component title() {
         return Component.translatable("cfhgui.dev_pencil." + this.key());
      }
   }

   public static final class Pencil extends CFHGuiIconButton {
      Pencil(int x, int y, Runnable onPress) {
         super(x, y, 20, Icon.EDIT, DevModeButton.tooltip(), onPress);
         this.variant(CFHGuiButton.Variant.GHOST);
      }
   }
}
