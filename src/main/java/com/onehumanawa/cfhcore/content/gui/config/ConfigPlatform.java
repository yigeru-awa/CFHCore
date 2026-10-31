package com.onehumanawa.cfhcore.content.gui.config;

import com.onehumanawa.cfhcore.content.gui.config.doc.FileDocument;
import com.onehumanawa.cfhcore.content.gui.config.mods.ModFacts;
import com.onehumanawa.cfhcore.content.gui.core.platform.Services;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.KeyMapping;

public interface ConfigPlatform {
   static ConfigPlatform get() {
      return (ConfigPlatform)Services.load(ConfigPlatform.class);
   }

   List<FileDocument> nativeConfigs(String var1);

   Optional<FileDocument> nativeConfig(String var1);

   default ModFacts modFacts() {
      return ModFacts.NONE;
   }

   default String keyModifier(KeyMapping mapping) {
      return "";
   }
}
