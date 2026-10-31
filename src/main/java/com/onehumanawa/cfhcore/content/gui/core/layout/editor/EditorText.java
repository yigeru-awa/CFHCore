package com.onehumanawa.cfhcore.content.gui.core.layout.editor;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

final class EditorText {
   static MutableComponent t(String key) {
      return Component.translatable("cfhgui_editor." + key);
   }

   static MutableComponent t(String key, Object... args) {
      return Component.translatable("cfhgui_editor." + key, args);
   }

   private EditorText() {
   }
}
