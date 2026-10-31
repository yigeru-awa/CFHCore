package com.onehumanawa.cfhcore.content.gui.config.page;

import com.onehumanawa.cfhcore.content.gui.config.ConfigSettings;
import com.onehumanawa.cfhcore.content.gui.config.hub.ConfigHubScreen;
import com.onehumanawa.cfhcore.content.gui.config.option.Binding;
import com.onehumanawa.cfhcore.content.gui.config.option.OptionBinding;
import com.onehumanawa.cfhcore.content.gui.config.option.OptionType;
import com.onehumanawa.cfhcore.content.gui.config.preset.Presets;
import com.onehumanawa.cfhcore.content.gui.config.search.SearchIndex;
import com.onehumanawa.cfhcore.content.gui.config.ui.ConfigTextField;
import com.onehumanawa.cfhcore.content.gui.config.ui.OptionPageBase;
import com.onehumanawa.cfhcore.content.gui.config.ui.Section;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.core.screen.SidebarPage;
import com.onehumanawa.cfhcore.content.gui.core.screen.SidebarScreen;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiCard;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiDropdown;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiLabel;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiModal;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiScrollPanel;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiToasts;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton.Variant;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiLabel.Style;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class PresetsPage extends SidebarPage {
   private SidebarScreen screen;

   public PresetsPage() {
      super("presets", Component.translatable("cfhgui_config.page.presets"), Icon.BOOKMARK);
   }

   public void build(SidebarScreen screen, Rect area) {
      this.screen = screen;
      CFHGuiScrollPanel panel = new CFHGuiScrollPanel(area.x(), area.y(), area.w(), area.h());
      int w = area.w() - 8;
      int y = 0;
      panel.add((new CFHGuiButton(0, 0, 160, Component.translatable("cfhgui_config.presets.save_current"), this::saveDialog)).icon(Icon.SAVE).variant(Variant.PRIMARY), 0, y);
      y += 28;

      for(ConfigSettings.Preset p : Presets.all()) {
         boolean hasDesc = p.description != null && !p.description.isBlank();
         int h = hasDesc ? 54 : 42;
         CFHGuiCard card = (new CFHGuiCard(0, y, w, h)).flat();
         card.add((new CFHGuiLabel(10, 8, w - 120, Component.literal(p.name))).style(Style.TITLE), 10, 8);
         Component meta = p.builtin ? Component.translatable("cfhgui_config.presets.builtin", new Object[]{p.values.size()}) : Component.translatable("cfhgui_config.presets.options", new Object[]{p.values.size()});
         card.add((new CFHGuiLabel(10, 22, w - 120, meta)).style(Style.CAPTION), 10, 22);
         if (hasDesc) {
            card.add((new CFHGuiLabel(10, 34, w - 120, Component.literal(p.description))).style(Style.MUTED), 10, 34);
         }

         card.add((new CFHGuiButton(w - 100, 10, 90, Component.translatable("cfhgui_config.presets.apply"), () -> this.apply(p))).icon(Icon.CHECK), w - 100, 10);
         if (!p.builtin) {
            card.add((new CFHGuiButton(w - 100, h - 26, 90, Component.translatable("cfhgui_config.presets.delete"), () -> CFHGuiModal.confirmDanger(Component.translatable("cfhgui_config.presets.delete"), Component.translatable("cfhgui_config.presets.delete.body", new Object[]{p.name}), Component.translatable("cfhgui_config.presets.delete"), () -> {
                  Presets.deleteUser(p);
                  screen.refreshPage();
               }))).icon(Icon.TRASH).variant(Variant.DANGER), w - 100, h - 26);
         }

         panel.add(card, 0, y);
         y += h + 8;
      }

      if (Presets.all().isEmpty()) {
         panel.add((new CFHGuiLabel(4, y, w - 8, Component.translatable("cfhgui_config.presets.empty"))).style(Style.MUTED).wrap(true), 4, y);
         y += 20;
      }

      panel.setContentHeight(y);
      screen.addPageWidget(panel);
   }

   private void apply(ConfigSettings.Preset p) {
      Presets.ApplyResult r = Presets.apply(p);
      CFHGuiToasts.show(Component.translatable("cfhgui_config.presets.applied", new Object[]{p.name}), Component.translatable("cfhgui_config.presets.applied_body", new Object[]{r.applied(), r.skipped()}), Icon.BOOKMARK);
      SidebarScreen var4 = this.screen;
      if (var4 instanceof ConfigHubScreen hub) {
         hub.refreshOptionPages();
      }

   }

   private void saveDialog() {
      ConfigTextField name = new ConfigTextField(0, 0, 200, Component.translatable("cfhgui_config.presets.name"));
      name.placeholder(Component.translatable("cfhgui_config.presets.name"));
      SidebarScreen var4 = this.screen;
      OptionPageBase var10000;
      if (var4 instanceof ConfigHubScreen hub) {
         var10000 = hub.lastOptionPage();
      } else {
         var10000 = null;
      }

      OptionPageBase last = var10000;
      List<Source> sources = new ArrayList(List.of(PresetsPage.Source.FAVOURITES, PresetsPage.Source.VIDEO));
      if (last != null) {
         sources.add(PresetsPage.Source.PAGE);
      }

      CFHGuiDropdown<Source> source = new CFHGuiDropdown<Source>(0, 0, 200, sources, PresetsPage.Source.VIDEO, (Source s) -> {
         MutableComponent var43;
         switch (s.ordinal()) {
            case 0 -> var43 = Component.translatable("cfhgui_config.presets.source.favourites");
            case 1 -> var43 = Component.translatable("cfhgui_config.presets.source.video");
            case 2 -> var43 = Component.translatable("cfhgui_config.presets.source.page", new Object[]{last == null ? "" : last.fullTitle().getString()});
            default -> throw new MatchException((String)null, (Throwable)null);
         }

         return var43;
      }, (Source s) -> {
      });
      source.label(Component.translatable("cfhgui_config.presets.source"));
      (new CFHGuiModal(Component.translatable("cfhgui_config.presets.save_current"), Component.translatable("cfhgui_config.presets.save_current.body"), Icon.SAVE)).extra(name).extra(source).button(Component.translatable("gui.cancel"), Variant.SECONDARY, (Runnable)null).button(Component.translatable("cfhgui_config.list.save"), Variant.PRIMARY, () -> {
         String n = name.getValue().trim();
         if (n.isEmpty()) {
            CFHGuiToasts.show(Component.translatable("cfhgui_config.presets.invalid_name"), (Component)null, Icon.WARNING);
         } else {
            Object var44;
            switch (((Source)source.value()).ordinal()) {
               case 0:
                  var44 = FavouritesPage.resolved();
                  break;
               case 1:
                  var44 = VideoPage.presetBindings();
                  break;
               case 2:
                  List<OptionBinding> out = new ArrayList();
                  if (last != null) {
                     for(Section s : last.currentSections()) {
                        out.addAll(s.bindings());
                     }
                  }

                  var44 = out;
                  break;
               default:
                  throw new MatchException((String)null, (Throwable)null);
            }

            List<OptionBinding> bindings = (List<OptionBinding>)var44;
            Presets.saveUser(n, "", Presets.snapshot(bindings));
            CFHGuiToasts.show(Component.translatable("cfhgui_config.presets.saved", new Object[]{n}), (Component)null, Icon.SAVE);
            this.screen.refreshPage();
         }
      }).show();
   }

   public List<SearchIndex.Entry> searchEntries() {
      List<SearchIndex.Entry> out = new ArrayList();

      for(ConfigSettings.Preset p : Presets.all()) {
         out.add(new SearchIndex.Entry(this.id(), this.title(), Component.translatable("cfhgui_config.page.presets"), Binding.of("preset:" + p.name, OptionType.ACTION, (Component)Component.translatable("cfhgui_config.presets.apply_named", new Object[]{p.name})).action(Component.translatable("cfhgui_config.presets.apply"), () -> this.apply(p)).searchWords("preset " + p.description)));
      }

      return out;
   }

   private static enum Source {
      FAVOURITES,
      VIDEO,
      PAGE;
   }
}
