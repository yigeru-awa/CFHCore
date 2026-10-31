package com.onehumanawa.cfhcore.content.gui.config.option;

import com.mojang.blaze3d.platform.InputConstants;
import com.onehumanawa.cfhcore.content.gui.core.client.KeyBinds;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public final class KeyBinding extends Binding {
   private final KeyMapping mapping;

   public KeyBinding(KeyMapping mapping) {
      super("key:" + mapping.getName(), OptionType.KEYBIND, Component.translatable(mapping.getName()));
      this.mapping = mapping;
      this.getter(() -> KeyBinds.save(mapping));
      this.setter((v) -> {
         String saved = OptionValues.asString(v);
         KeyBinds.bind(mapping, KeyBinds.key(saved), KeyBinds.modifier(saved));
         KeyMapping.resetMapping();
         Minecraft.getInstance().options.save();
      });
      this.def(KeyBinds.saveDefault(mapping));
      this.tooltip(Component.translatable(mapping.getCategory()));
      this.searchWords(Component.translatable(mapping.getCategory()).getString() + " key bind");
   }

   public KeyMapping mapping() {
      return this.mapping;
   }

   public String searchText() {
      String var10000 = super.searchText();
      return var10000 + " " + KeySearch.text(this.mapping);
   }

   public static InputConstants.Key parse(@Nullable String name) {
      return KeyBinds.key(name);
   }

   public Component valueText(@Nullable Object value) {
      String saved = OptionValues.asString(value);
      Component key = parse(saved).getDisplayName();
      Object var10000;
      switch (KeyBinds.modifier(saved)) {
         case "shift" -> var10000 = Component.translatable("cfhgui_config.key.with_shift", new Object[]{key});
         case "ctrl" -> var10000 = Component.translatable("cfhgui_config.key.with_ctrl", new Object[]{key});
         case "alt" -> var10000 = Component.translatable("cfhgui_config.key.with_alt", new Object[]{key});
         default -> var10000 = key;
      }

      return (Component)var10000;
   }
}
