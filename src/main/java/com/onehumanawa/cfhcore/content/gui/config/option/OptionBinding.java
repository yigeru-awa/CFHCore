package com.onehumanawa.cfhcore.content.gui.config.option;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import java.util.List;
import java.util.Objects;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public interface OptionBinding {
   String id();

   Component label();

   @Nullable Component tooltip();

   OptionType type();

   @Nullable Object get();

   void set(@Nullable Object var1);

   default @Nullable Object defaultValue() {
      return null;
   }

   default boolean hasDefault() {
      return this.defaultValue() != null;
   }

   default boolean isDefault() {
      Object d = this.defaultValue();
      return d == null || Objects.equals(OptionValues.normalize(this.get()), OptionValues.normalize(d));
   }

   default void reset() {
      if (this.hasDefault()) {
         this.set(this.defaultValue());
      }

   }

   default @Nullable NumberRange range() {
      return null;
   }

   default List<Choice> choices() {
      return List.of();
   }

   default boolean requiresRestart() {
      return false;
   }

   default boolean enabled() {
      return true;
   }

   default boolean advanced() {
      return false;
   }

   default @Nullable Runnable action() {
      return null;
   }

   default Component actionLabel() {
      return Component.translatable("cfhgui_config.row.open");
   }

   default Icon actionIcon() {
      return Icon.EXTERNAL;
   }

   default Component valueText(@Nullable Object value) {
      return OptionValues.defaultText(this, value);
   }

   default String searchText() {
      Component t = this.tooltip();
      String var10000 = this.label().getString();
      return var10000 + (t == null ? "" : " " + t.getString());
   }
}
