package com.onehumanawa.cfhcore.content.gui.config.page;

import com.onehumanawa.cfhcore.content.gui.config.resolver.VanillaOptions;
import com.onehumanawa.cfhcore.content.gui.config.ui.ConfigSearchField;
import com.onehumanawa.cfhcore.content.gui.config.ui.OptionPageBase;
import com.onehumanawa.cfhcore.content.gui.config.ui.Section;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiList;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton.Variant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.LanguageInfo;
import net.minecraft.client.resources.language.LanguageManager;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public final class LanguagePage extends OptionPageBase {
   private static final String TAB = "language";
   private String search = "";
   private String selected;
   private @Nullable CFHGuiList<Lang> list;
   private @Nullable CFHGuiButton applyButton;
   private static final int HEADER_H = 22;

   public LanguagePage() {
      super("language", Component.translatable("cfhgui_config.page.language"), Icon.LANGUAGE);
   }

   protected boolean pills(String tabKey) {
      return false;
   }

   private List<Lang> languages() {
      LanguageManager lm = Minecraft.getInstance().getLanguageManager();
      List<Lang> out = new ArrayList();
      String q = this.search.toLowerCase(Locale.ROOT).trim();

      for(Map.Entry<String, LanguageInfo> e : lm.getLanguages().entrySet()) {
         String var10000 = (String)e.getKey();
         String hay = (var10000 + " " + ((LanguageInfo)e.getValue()).name() + " " + ((LanguageInfo)e.getValue()).region()).toLowerCase(Locale.ROOT);
         if (q.isEmpty() || hay.contains(q)) {
            out.add(new Lang((String)e.getKey(), (LanguageInfo)e.getValue()));
         }
      }

      return out;
   }

   private void refreshList() {
      if (this.list != null) {
         List<Lang> langs = this.languages();
         this.list.items(langs);
         this.list.clearSelection();

         for(int i = 0; i < langs.size(); ++i) {
            if (((Lang)langs.get(i)).code.equals(this.selected)) {
               this.list.select(i);
               this.list.ensureVisible(i);
               break;
            }
         }

      }
   }

   private void updateApply() {
      if (this.applyButton != null) {
         this.applyButton.enabled(this.selected != null && !this.selected.equals(Minecraft.getInstance().getLanguageManager().getSelected()));
      }

   }

   private int listHeight() {
      int h = this.area == null ? 300 : this.area.h();
      return Math.max(80, h - 26 - 22 - 52 - 24);
   }

   protected List<Section> sections() {
      if (this.selected == null) {
         this.selected = Minecraft.getInstance().getLanguageManager().getSelected();
      }

      List<Section> out = new ArrayList();
      Section pick = Section.of("pick", Component.empty()).fixed().tab("language", this.title());
      pick.custom((w) -> {
         ConfigSearchField field = new ConfigSearchField(0, 0, Math.max(80, w - 148), (s) -> {
            this.search = s;
            this.refreshList();
         });
         field.setValue(this.search);
         CFHGuiButton apply = (new CFHGuiButton(0, 0, 140, Component.translatable("cfhgui_config.language.apply"), this::apply)).icon(Icon.CHECK).variant(Variant.PRIMARY);
         this.applyButton = apply;
         this.updateApply();
         return new ControlsPage.Toolbar(w, List.of(field, apply));
      });
      pick.custom((w) -> {
         CFHGuiList<Lang> l = (new CFHGuiList<Lang>(0, 0, w, this.listHeight(), 16, (g, item, index, x, y, rw, h, hovered, sel, mx, my) -> {
            Palette p = Theme.current().palette();
            boolean current = item.code().equals(Minecraft.getInstance().getLanguageManager().getSelected());
            String var10000 = item.info().name();
            Component name = Component.literal(var10000 + " (" + item.info().region() + ")");
            g.drawString(CFHGuiDraw.font(), CFHGuiDraw.truncate(name, rw - 60), x + 6, y + 4, sel ? p.text() : (Theme.current().isVanilla() ? -2039584 : p.textMuted()), Theme.current().isVanilla());
            if (current) {
               CFHGuiDraw.textRight(g, Component.translatable("cfhgui_config.language.current"), x + rw - 6, y + 4, p.accentInk());
            }

         })).gap(1).emptyText(Component.translatable("cfhgui_config.search.no_matches"));
         this.list = l;
         l.onSelect((x) -> {
            this.selected = x.code();
            this.updateApply();
         });
         l.onActivate((x) -> {
            this.selected = x.code();
            this.apply();
         });
         this.refreshList();
         return l;
      });
      out.add(pick);
      out.add(Section.of("font", Component.translatable("cfhgui_config.language.font"), VanillaOptions.all("forceUnicodeFont", "japaneseGlyphVariants")).fixed().tab("language", this.title()));
      return out;
   }

   private void apply() {
      Minecraft mc = Minecraft.getInstance();
      LanguageManager lm = mc.getLanguageManager();
      if (this.selected != null && !this.selected.equals(lm.getSelected())) {
         lm.setSelected(this.selected);
         mc.options.languageCode = this.selected;
         mc.options.save();
         mc.reloadResourcePacks();
      }
   }

   private static record Lang(String code, LanguageInfo info) {
   }
}
