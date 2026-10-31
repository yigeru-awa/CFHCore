package com.onehumanawa.cfhcore.content.gui.core.compat.maps;

import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;

public interface PlaceBridge {
   String id();

   boolean attach();

   default void pinsChanged() {
   }

   default void tick() {
   }

   default void abandon() {
   }

   default @Nullable Screen mapScreen() {
      return null;
   }

   default String describe() {
      return "";
   }
}
