package com.onehumanawa.cfhcore.content.gui.config.neoforge;

import com.onehumanawa.cfhcore.content.gui.config.ConfigPlatform;
import com.onehumanawa.cfhcore.content.gui.config.doc.FileDocument;
import com.onehumanawa.cfhcore.content.gui.config.mods.ModFacts;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.KeyMapping;
import net.neoforged.fml.ModList;
import net.neoforged.fml.config.IConfigSpec;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.config.ModConfigs;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforgespi.language.IModInfo;
import net.neoforged.neoforgespi.language.IModInfo.DependencyType;

public final class NeoForgeConfigPlatform implements ConfigPlatform {
   private static final Map<String, FileDocument> DOCS = new ConcurrentHashMap();

   public List<FileDocument> nativeConfigs(String modId) {
      List<FileDocument> out = new ArrayList();

      try {
         for(ModConfig mc : ModConfigs.getModConfigs(modId)) {
            Optional<FileDocument> var10000 = doc(mc);
            Objects.requireNonNull(out);
            var10000.ifPresent(out::add);
         }
      } catch (Throwable var5) {
      }

      return out;
   }

   public ModFacts modFacts() {
      Set<String> nested = new HashSet();
      Map<String, Set<String>> requires = new HashMap();

      for(IModInfo mod : ModList.get().getMods()) {
         if (mod.getOwningFile().getFile().getDiscoveryAttributes().parent() != null) {
            nested.add(mod.getModId());
         }

         Set<String> needs = new HashSet();

         for(IModInfo.ModVersion dependency : mod.getDependencies()) {
            if (dependency.getType() == DependencyType.REQUIRED) {
               needs.add(dependency.getModId());
            }
         }

         requires.put(mod.getModId(), needs);
      }

      return new ModFacts(Set.of(), nested, requires);
   }

   public String keyModifier(KeyMapping mapping) {
      String var10000;
      switch (mapping.getKeyModifier()) {
         case CONTROL -> var10000 = "ctrl";
         case SHIFT -> var10000 = "shift";
         case ALT -> var10000 = "alt";
         default -> var10000 = "";
      }

      return var10000;
   }

   public Optional<FileDocument> nativeConfig(String fileName) {
      try {
         ModConfig mc = (ModConfig)ModConfigs.getFileMap().get(fileName);
         return mc == null ? Optional.empty() : doc(mc);
      } catch (Throwable var3) {
         return Optional.empty();
      }
   }

   private static Optional<FileDocument> doc(ModConfig mc) {
      IConfigSpec var2 = mc.getSpec();
      if (var2 instanceof ModConfigSpec spec) {
         return Optional.of((FileDocument)DOCS.computeIfAbsent(mc.getFileName(), (k) -> new NeoForgeSpecDocument(mc, spec)));
      } else {
         return Optional.empty();
      }
   }
}
