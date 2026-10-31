package com.onehumanawa.cfhcore.content.gui.config.page;

import com.onehumanawa.cfhcore.content.gui.config.ConfigPlatform;
import com.onehumanawa.cfhcore.content.gui.config.doc.ConfigFiles;
import com.onehumanawa.cfhcore.content.gui.config.doc.Documents;
import com.onehumanawa.cfhcore.content.gui.config.doc.FileDocument;
import com.onehumanawa.cfhcore.content.gui.config.editor.FileEditorScreen;
import com.onehumanawa.cfhcore.content.gui.config.option.Binding;
import com.onehumanawa.cfhcore.content.gui.config.option.OptionType;
import com.onehumanawa.cfhcore.content.gui.config.ui.OptionPageBase;
import com.onehumanawa.cfhcore.content.gui.config.ui.Section;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.platform.ModInfo;
import com.onehumanawa.cfhcore.content.gui.core.platform.CFHGuiPlatform;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class ConfigFilesPage extends OptionPageBase {
   public ConfigFilesPage() {
      super("files", Component.translatable("cfhgui_config.page.files"), Icon.CODE);
      this.level(OptionPageBase.Level.ALL);
      this.emptyText(Component.translatable("cfhgui_config.files.none"));
   }

   protected boolean pills(String tabKey) {
      return false;
   }

   protected List<Section> sections() {
      Minecraft mc = Minecraft.getInstance();
      List<ModInfo> mods = new ArrayList(CFHGuiPlatform.get().allMods());
      mods.sort(Comparator.comparing((mx) -> mx.name().toLowerCase(Locale.ROOT)));
      List<Section> out = new ArrayList();
      Path configDir = CFHGuiPlatform.get().configDir();

      for(ModInfo m : mods) {
         Section s = Section.of("mod." + m.id(), Component.literal(m.name()));
         Set<String> covered = new HashSet();
         int i = 0;

         for(FileDocument doc : ConfigPlatform.get().nativeConfigs(m.id())) {
            covered.add(key(doc.path()));
            String var10001 = m.id();
            Binding var14 = Binding.of("file:" + var10001 + ":" + i++, OptionType.ACTION, doc.title()).tooltip((Component)Component.literal(relative(configDir, doc.path()))).actionIcon(Icon.SLIDERS).action(Component.translatable("cfhgui_config.files.edit"), () -> mc.setScreen(new FileEditorScreen(mc.screen, doc)));
            String var17 = m.id();
            s.add(var14.searchWords(var17 + " " + m.name() + " config file"));
         }

         for(Path p : ConfigFiles.forMod(m.id())) {
            if (!covered.contains(key(p))) {
               String var15 = m.id();
               Binding var16 = Binding.of("file:" + var15 + ":" + i++, OptionType.ACTION, (Component)Component.literal(p.getFileName().toString())).tooltip((Component)Component.literal(relative(configDir, p))).actionIcon(Icon.CODE).action(Component.translatable("cfhgui_config.files.edit"), () -> Documents.open(p, m.id()).ifPresent((doc) -> mc.setScreen(new FileEditorScreen(mc.screen, doc))));
               String var19 = m.id();
               s.add(var16.searchWords(var19 + " " + m.name() + " config file"));
            }
         }

         if (!s.isEmpty()) {
            out.add(s);
         }
      }

      return out;
   }

   private static String relative(Path configDir, Path p) {
      try {
         String var10000 = configDir.toAbsolutePath().normalize().relativize(p.toAbsolutePath().normalize()).toString();
         return "config/" + var10000.replace('\\', '/');
      } catch (Exception var3) {
         return String.valueOf(p);
      }
   }

   private static String key(Path p) {
      try {
         return p.toAbsolutePath().normalize().toString();
      } catch (Exception var2) {
         return String.valueOf(p);
      }
   }
}
