package com.onehumanawa.cfhcore.content.gui.core.theme;

import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public enum PixelFont {
   PIXELOID("pixeloid"),
   MONOCRAFT("monocraft"),
   PIXELIFY("pixelify");

   public static final PixelFont DEFAULT = PIXELOID;
   private final String key;
   private final ResourceLocation id;

   private PixelFont(final String key) {
      this.key = key;
      this.id = CFHGui.id("heading_" + key);
   }

   public String key() {
      return this.key;
   }

   public ResourceLocation id() {
      return this.id;
   }

   public MutableComponent label() {
      return Component.translatable("cfhgui.pixel_font." + this.key).withStyle((s) -> s.withFont(this.id));
   }

   public static PixelFont parse(@Nullable String s) {
      if (s != null) {
         String k = s.trim().toLowerCase(Locale.ROOT);

         for(PixelFont f : values()) {
            if (f.key.equals(k)) {
               return f;
            }
         }
      }

      return DEFAULT;
   }
}
