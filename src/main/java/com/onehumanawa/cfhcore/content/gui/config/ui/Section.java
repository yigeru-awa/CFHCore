package com.onehumanawa.cfhcore.content.gui.config.ui;

import com.onehumanawa.cfhcore.content.gui.config.option.OptionBinding;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public final class Section {
   public final String id;
   public final Component title;
   public final @Nullable Component description;
   public final List<Item> items = new ArrayList();
   public boolean collapsible = true;
   public @Nullable String tabId;
   public @Nullable Component tabTitle;
   public Level level;

   public Section(String id, Component title, @Nullable Component description) {
      this.level = Section.Level.PER_ROW;
      this.id = id;
      this.title = title;
      this.description = description;
   }

   public static Section of(String id, Component title) {
      return new Section(id, title, (Component)null);
   }

   public static Section of(String id, Component title, List<? extends OptionBinding> bindings) {
      Section s = new Section(id, title, (Component)null);

      for(OptionBinding b : bindings) {
         s.add(b);
      }

      return s;
   }

   public Section add(OptionBinding b) {
      if (b != null) {
         this.items.add(new Item(b, (IntFunction)null));
      }

      return this;
   }

   public Section addAll(List<? extends OptionBinding> bs) {
      for(OptionBinding b : bs) {
         this.add(b);
      }

      return this;
   }

   public Section custom(IntFunction<AbstractWidget> factory) {
      this.items.add(new Item((OptionBinding)null, factory));
      return this;
   }

   public Section fixed() {
      this.collapsible = false;
      return this;
   }

   public Section advanced() {
      this.level = Section.Level.ADVANCED;
      return this;
   }

   public Section basic() {
      this.level = Section.Level.BASIC;
      return this;
   }

   public boolean isAdvanced(Item item) {
      boolean var10000;
      switch (this.level.ordinal()) {
         case 0 -> var10000 = item.binding() != null && item.binding().advanced();
         case 1 -> var10000 = false;
         case 2 -> var10000 = true;
         default -> throw new MatchException((String)null, (Throwable)null);
      }

      return var10000;
   }

   public Section filtered(Predicate<Item> keep) {
      Section s = new Section(this.id, this.title, this.description);
      s.collapsible = this.collapsible;
      s.tabId = this.tabId;
      s.tabTitle = this.tabTitle;
      s.level = this.level;

      for(Item i : this.items) {
         if (keep.test(i)) {
            s.items.add(i);
         }
      }

      return s;
   }

   public Section tab(String id, Component title) {
      this.tabId = id;
      this.tabTitle = title;
      return this;
   }

   public String tabKey() {
      return this.tabId != null ? this.tabId : this.id;
   }

   public Component tabLabel() {
      return this.tabTitle != null ? this.tabTitle : this.title;
   }

   public List<OptionBinding> bindings() {
      List<OptionBinding> out = new ArrayList();

      for(Item i : this.items) {
         if (i.binding != null) {
            out.add(i.binding);
         }
      }

      return out;
   }

   public boolean isEmpty() {
      return this.items.isEmpty();
   }

   public static record Item(@Nullable OptionBinding binding, @Nullable IntFunction<AbstractWidget> custom) {
   }

   public static enum Level {
      PER_ROW,
      BASIC,
      ADVANCED;
   }
}
