package com.onehumanawa.cfhcore.content.gui.config.doc;

import java.nio.file.Path;
import java.util.List;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public interface FileDocument {
   Path path();

   Component title();

   String kind();

   default String modId() {
      return "";
   }

   List<DocSection> sections();

   void reload();

   default @Nullable Component notice() {
      return null;
   }

   default boolean editable() {
      return true;
   }
}
