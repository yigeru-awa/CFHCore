package com.onehumanawa.cfhcore.content.gui.config.editor;

import com.onehumanawa.cfhcore.content.gui.config.doc.DocSection;
import com.onehumanawa.cfhcore.content.gui.config.doc.FileDocument;
import com.onehumanawa.cfhcore.content.gui.config.option.OptionBinding;
import com.onehumanawa.cfhcore.content.gui.config.option.OptionResolvers;
import com.onehumanawa.cfhcore.content.gui.config.ui.ApplyQueue;
import com.onehumanawa.cfhcore.content.gui.config.ui.ConfigSearchField;
import com.onehumanawa.cfhcore.content.gui.config.ui.OptionRow;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.core.screen.CFHGuiScreen;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiIconButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiLabel;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiScrollPanel;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSectionHeader;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiLabel.Style;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.Util;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public final class FileEditorScreen extends CFHGuiScreen {
   private final FileDocument doc;
   private String filter = "";
   private final Set<String> collapsed = new HashSet();
   private final List<OptionRow> rows = new ArrayList();
   private @Nullable CFHGuiScrollPanel panel;
   private double keepScroll = (double)-1.0F;

   public FileEditorScreen(@Nullable Screen parent, FileDocument doc) {
      super(doc.title(), parent);
      this.doc = doc;
   }

   public FileDocument document() {
      return this.doc;
   }

   protected void build() {
      this.rows.clear();
      this.addHeaderAction(new CFHGuiIconButton(0, 0, 20, Icon.FOLDER, Component.translatable("cfhgui_config.editor.folder"), () -> {
         Path parent = this.doc.path().getParent();
         if (parent != null) {
            Util.getPlatform().openPath(parent);
         }

      }));
      this.addHeaderAction(new CFHGuiIconButton(0, 0, 20, Icon.EXTERNAL, Component.translatable("cfhgui_config.editor.external"), () -> Util.getPlatform().openPath(this.doc.path())));
      this.addHeaderAction(new CFHGuiIconButton(0, 0, 20, Icon.REFRESH, Component.translatable("cfhgui_config.editor.reload"), () -> {
         this.doc.reload();
         OptionResolvers.invalidate(this.doc.kind());
         this.rebuild();
      }));
      ConfigSearchField search = new ConfigSearchField(0, 0, 140, (t) -> {
         if (!t.equals(this.filter)) {
            this.filter = t;
            this.keepScroll = (double)0.0F;
            this.rebuild();
         }

      });
      search.setValue(this.filter);
      this.addHeaderAction(search);
      Rect c = this.contentRect();
      int top = c.y() + 2;
      CFHGuiLabel path = (CFHGuiLabel)this.add((new CFHGuiLabel(c.x(), top, c.w(), Component.literal(this.doc.path().toString()))).style(Style.CAPTION));
      top += path.getHeight() + 4;
      if (this.doc.notice() != null) {
         CFHGuiLabel notice = (CFHGuiLabel)this.add((new CFHGuiLabel(c.x(), top, c.w(), this.doc.notice())).style(Style.BODY).color(Theme.current().palette().warning()).wrap(true));
         top += notice.getHeight() + 6;
      }

      CFHGuiScrollPanel p = (CFHGuiScrollPanel)this.add(new CFHGuiScrollPanel(c.x(), top, c.w(), c.bottom() - top));
      this.panel = p;
      int w = c.w() - 8;
      String q = this.filter.toLowerCase(Locale.ROOT).trim();
      int y = 0;
      List<DocSection> sections = this.doc.editable() ? this.doc.sections() : List.of();

      for(DocSection s : sections) {
         List<OptionBinding> items = new ArrayList();

         for(OptionBinding b : s.options()) {
            if (!q.isEmpty()) {
               String var10000 = b.searchText();
               if (!(var10000 + " " + s.title().getString() + " " + s.path()).toLowerCase(Locale.ROOT).contains(q)) {
                  continue;
               }
            }

            items.add(b);
         }

         boolean hasHeader = !s.path().isEmpty() || sections.size() > 1;
         if (!items.isEmpty() || q.isEmpty() && (!s.options().isEmpty() || hasHeader) || q.isEmpty() && (!s.options().isEmpty() || !s.path().isEmpty())) {
            boolean isCollapsed = q.isEmpty() && this.collapsed.contains(s.path());
            if (hasHeader) {
               p.add(new CFHGuiSectionHeader(0, y, w, s.title(), s.options().size(), isCollapsed, () -> {
                  if (!this.collapsed.remove(s.path())) {
                     this.collapsed.add(s.path());
                  }

                  this.rebuild();
               }, Math.max(0, s.depth() - 1)), 0, y);
               y += 22;
               if (s.comment() != null && !isCollapsed) {
                  CFHGuiLabel d = (new CFHGuiLabel(0, y, w - 8 - s.depth() * 8, s.comment())).style(Style.MUTED).wrap(true);
                  p.add(d, 4 + Math.max(0, s.depth() - 1) * 8, y);
                  y += d.getHeight() + 4;
               }
            }

            if (isCollapsed) {
               y += 4;
            } else {
               for(OptionBinding b : items) {
                  OptionResolvers.publish(b);
                  OptionRow row = (new OptionRow(0, y, w, b, Math.max(0, s.depth() - 1))).onChanged((r) -> {
                     for(OptionRow o : this.rows) {
                        if (o != r) {
                           o.refresh();
                        }
                     }

                  });
                  this.rows.add(row);
                  p.add(row, 0, y);
                  y += 26;
               }

               y += 6;
            }
         }
      }

      if (this.rows.isEmpty() && this.doc.editable()) {
         p.add((new CFHGuiLabel(4, y + 4, w - 8, Component.translatable(q.isEmpty() ? "cfhgui_config.editor.empty" : "cfhgui_config.search.no_matches"))).style(Style.MUTED), 4, y + 4);
         y += 20;
      }

      p.setContentHeight(y + 4);
      if (this.keepScroll >= (double)0.0F) {
         p.snapScroll(this.keepScroll);
         this.keepScroll = (double)-1.0F;
      }

   }

   private void rebuild() {
      this.keepScroll = this.panel != null ? this.panel.scrollAmount() : (double)-1.0F;
      this.clearWidgets();
      this.init(this.minecraft, this.width, this.height);
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if ((keyCode == 47 || keyCode == 70 && hasControlDown()) && !(this.getFocused() instanceof EditBox)) {
         for(Renderable r : this.renderableList()) {
            if (r instanceof ConfigSearchField) {
               ConfigSearchField f = (ConfigSearchField)r;
               this.setFocused(f);
               return true;
            }
         }
      }

      return super.keyPressed(keyCode, scanCode, modifiers);
   }

   public void removed() {
      ApplyQueue.flush();
      super.removed();
   }
}
