package com.onehumanawa.cfhcore.content.gui.config.page;

import com.onehumanawa.cfhcore.content.gui.config.option.Binding;
import com.onehumanawa.cfhcore.content.gui.config.option.OptionType;
import com.onehumanawa.cfhcore.content.gui.config.ui.OptionPageBase;
import com.onehumanawa.cfhcore.content.gui.config.ui.Section;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.media.FilePicker;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiToasts;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.network.chat.Component;

public final class ResourcePacksPage extends OptionPageBase {
   private static final String TAB = "packs";

   public ResourcePacksPage() {
      super("packs", Component.translatable("cfhgui_config.page.packs"), Icon.PACK);
   }

   protected boolean pills(String tabKey) {
      return false;
   }

   protected List<Section> sections() {
      Minecraft mc = Minecraft.getInstance();
      List<Section> out = new ArrayList();
      Section actions = Section.of("actions", Component.translatable("cfhgui_config.packs.manage")).fixed().tab("packs", this.title());
      actions.add(Binding.of("packs:open", OptionType.ACTION, (Component)Component.translatable("cfhgui_config.packs.open")).tooltip((Component)Component.translatable("cfhgui_config.packs.open.tip")).action(Component.translatable("cfhgui_config.row.open"), () -> {
         Screen back = mc.screen;
         mc.setScreen(new PackSelectionScreen(mc.getResourcePackRepository(), (repo) -> {
            mc.options.updateResourcePacks(repo);
            mc.setScreen(back);
         }, mc.getResourcePackDirectory(), Component.translatable("resourcePack.title")));
      }).searchWords("resource packs select"));
      actions.add(Binding.of("packs:import", OptionType.ACTION, (Component)Component.translatable("cfhgui_config.packs.import")).tooltip((Component)Component.translatable("cfhgui_config.packs.import.tip")).actionIcon(Icon.IMPORT).action(Component.translatable("cfhgui_config.row.choose_files"), () -> {
         Path dir = mc.getResourcePackDirectory();
         List<Path> picked = FilePicker.openFiles(Component.translatable("cfhgui_config.packs.import"), dir, "Resource packs (*.zip)", new String[]{"*.zip"});
         if (!picked.isEmpty()) {
            List<String> names = FilePicker.copyInto(picked, dir);
            mc.getResourcePackRepository().reload();
            if (!names.isEmpty()) {
               CFHGuiToasts.show(Component.translatable("cfhgui_config.packs.imported", new Object[]{names.size()}), Component.literal(String.join(", ", names)), Icon.PACK);
            }

            this.rebuild();
         }
      }).searchWords("resource packs import add zip file"));
      actions.add(Binding.of("packs:folder", OptionType.ACTION, (Component)Component.translatable("cfhgui_config.packs.folder")).actionIcon(Icon.FOLDER).action(Component.translatable("cfhgui_config.row.open"), () -> Util.getPlatform().openPath(mc.getResourcePackDirectory())).searchWords("resource packs folder"));
      out.add(actions);
      Section active = Section.of("active", Component.translatable("cfhgui_config.packs.active")).fixed().tab("packs", this.title());
      List<String> packs = mc.options.resourcePacks;
      if (packs.isEmpty()) {
         active.add(Binding.of("packs:none", OptionType.INFO, (Component)Component.translatable("cfhgui_config.packs.none")).getter(() -> ""));
      }

      int i = 0;

      for(String id : packs) {
         boolean incompatible = mc.options.incompatibleResourcePacks.contains(id);
         int var10001 = i++;
         active.add(Binding.of("packs:active." + var10001, OptionType.INFO, (Component)Component.literal(id)).getter(() -> incompatible ? Component.translatable("cfhgui_config.packs.incompatible").getString() : "").searchWords("resource pack"));
      }

      out.add(active);
      return out;
   }
}
