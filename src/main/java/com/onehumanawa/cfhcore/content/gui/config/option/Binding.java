package com.onehumanawa.cfhcore.content.gui.config.option;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public class Binding implements OptionBinding {
   private final String id;
   private final OptionType type;
   private Component label;
   private @Nullable Component tooltip;
   private Supplier<Object> getter = () -> null;
   private Consumer<Object> setter = (v) -> {
   };
   private @Nullable Object defaultValue;
   private @Nullable NumberRange range;
   private List<Choice> choices = List.of();
   private boolean restart;
   private boolean advanced;
   private BooleanSupplier enabled = () -> true;
   private @Nullable Runnable action;
   private @Nullable Component actionLabel;
   private @Nullable Icon actionIcon;
   private @Nullable Function<Object, Component> format;
   private @Nullable String extraSearch;

   public Binding(String id, OptionType type, Component label) {
      this.id = id;
      this.type = type;
      this.label = label;
   }

   public static Binding of(String id, OptionType type, Component label) {
      return new Binding(id, type, label);
   }

   public static Binding of(String id, OptionType type, String label) {
      return new Binding(id, type, Component.literal(label));
   }

   public Binding label(Component c) {
      this.label = c;
      return this;
   }

   public Binding tooltip(@Nullable Component c) {
      this.tooltip = c;
      return this;
   }

   public Binding tooltip(@Nullable String s) {
      this.tooltip = s != null && !s.isBlank() ? Component.literal(s) : null;
      return this;
   }

   public Binding getter(Supplier<Object> g) {
      this.getter = g;
      return this;
   }

   public Binding setter(Consumer<Object> s) {
      this.setter = s;
      return this;
   }

   public Binding def(@Nullable Object d) {
      this.defaultValue = d;
      return this;
   }

   public Binding range(@Nullable NumberRange r) {
      this.range = r;
      return this;
   }

   public Binding range(double min, double max, double step) {
      this.range = new NumberRange(min, max, step);
      return this;
   }

   public Binding choices(List<Choice> c) {
      this.choices = c == null ? List.of() : List.copyOf(c);
      return this;
   }

   public Binding restart(boolean r) {
      this.restart = r;
      return this;
   }

   public Binding advanced(boolean a) {
      this.advanced = a;
      return this;
   }

   public Binding enabledIf(BooleanSupplier s) {
      this.enabled = s;
      return this;
   }

   public Binding action(Runnable r) {
      this.action = r;
      return this;
   }

   public Binding action(Component buttonLabel, Runnable r) {
      this.actionLabel = buttonLabel;
      this.action = r;
      return this;
   }

   public Binding actionIcon(Icon icon) {
      this.actionIcon = icon;
      return this;
   }

   public Binding format(Function<Object, Component> f) {
      this.format = f;
      return this;
   }

   public Binding searchWords(String words) {
      this.extraSearch = words;
      return this;
   }

   public String id() {
      return this.id;
   }

   public Component label() {
      return this.label;
   }

   public @Nullable Component tooltip() {
      return this.tooltip;
   }

   public OptionType type() {
      return this.type;
   }

   public @Nullable Object get() {
      return this.getter.get();
   }

   public void set(@Nullable Object value) {
      this.setter.accept(value);
   }

   public @Nullable Object defaultValue() {
      return this.defaultValue;
   }

   public @Nullable NumberRange range() {
      return this.range;
   }

   public List<Choice> choices() {
      return this.choices;
   }

   public boolean requiresRestart() {
      return this.restart;
   }

   public boolean enabled() {
      return this.enabled.getAsBoolean();
   }

   public boolean advanced() {
      return this.advanced;
   }

   public @Nullable Runnable action() {
      return this.action;
   }

   public Component actionLabel() {
      return this.actionLabel != null ? this.actionLabel : OptionBinding.super.actionLabel();
   }

   public Icon actionIcon() {
      return this.actionIcon != null ? this.actionIcon : OptionBinding.super.actionIcon();
   }

   public Component valueText(@Nullable Object value) {
      return this.format != null && value != null ? (Component)this.format.apply(value) : OptionValues.defaultText(this, value);
   }

   public String searchText() {
      String var10000 = OptionBinding.super.searchText();
      return var10000 + (this.extraSearch == null ? "" : " " + this.extraSearch);
   }

   public static OptionBinding override(final OptionBinding base, final @Nullable Component label, final @Nullable Component tooltip, final @Nullable NumberRange range, final @Nullable List<Choice> choices, final @Nullable Boolean restart) {
      return new OptionBinding() {
         public String id() {
            return base.id();
         }

         public Component label() {
            return label != null ? label : base.label();
         }

         public @Nullable Component tooltip() {
            return tooltip != null ? tooltip : base.tooltip();
         }

         public OptionType type() {
            return choices != null && !choices.isEmpty() && base.type() == OptionType.STRING ? OptionType.CHOICE : base.type();
         }

         public @Nullable Object get() {
            return base.get();
         }

         public void set(@Nullable Object value) {
            base.set(value);
         }

         public @Nullable Object defaultValue() {
            return base.defaultValue();
         }

         public @Nullable NumberRange range() {
            return range != null ? range : base.range();
         }

         public List<Choice> choices() {
            return choices != null && !choices.isEmpty() ? choices : base.choices();
         }

         public boolean requiresRestart() {
            return restart != null ? restart : base.requiresRestart();
         }

         public boolean enabled() {
            return base.enabled();
         }

         public boolean advanced() {
            return base.advanced();
         }

         public @Nullable Runnable action() {
            return base.action();
         }

         public Component actionLabel() {
            return base.actionLabel();
         }

         public Icon actionIcon() {
            return base.actionIcon();
         }

         public Component valueText(@Nullable Object value) {
            return base.valueText(value);
         }

         public String searchText() {
            List<String> parts = new ArrayList();
            parts.add(this.label().getString());
            if (this.tooltip() != null) {
               parts.add(this.tooltip().getString());
            }

            parts.add(base.searchText());
            return String.join(" ", parts);
         }
      };
   }
}
