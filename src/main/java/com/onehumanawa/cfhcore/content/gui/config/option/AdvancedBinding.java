package com.onehumanawa.cfhcore.content.gui.config.option;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import java.util.List;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public final class AdvancedBinding implements OptionBinding {
   private final OptionBinding base;

   private AdvancedBinding(OptionBinding base) {
      this.base = base;
   }

   public static OptionBinding of(OptionBinding base) {
      return (OptionBinding)(base.advanced() ? base : new AdvancedBinding(base));
   }

   public OptionBinding base() {
      return this.base;
   }

   public String id() {
      return this.base.id();
   }

   public Component label() {
      return this.base.label();
   }

   public @Nullable Component tooltip() {
      return this.base.tooltip();
   }

   public OptionType type() {
      return this.base.type();
   }

   public @Nullable Object get() {
      return this.base.get();
   }

   public void set(@Nullable Object value) {
      this.base.set(value);
   }

   public @Nullable Object defaultValue() {
      return this.base.defaultValue();
   }

   public @Nullable NumberRange range() {
      return this.base.range();
   }

   public List<Choice> choices() {
      return this.base.choices();
   }

   public boolean requiresRestart() {
      return this.base.requiresRestart();
   }

   public boolean enabled() {
      return this.base.enabled();
   }

   public boolean advanced() {
      return true;
   }

   public @Nullable Runnable action() {
      return this.base.action();
   }

   public Component actionLabel() {
      return this.base.actionLabel();
   }

   public Icon actionIcon() {
      return this.base.actionIcon();
   }

   public Component valueText(@Nullable Object value) {
      return this.base.valueText(value);
   }

   public String searchText() {
      return this.base.searchText();
   }
}
