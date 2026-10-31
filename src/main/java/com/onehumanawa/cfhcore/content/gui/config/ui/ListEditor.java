package com.onehumanawa.cfhcore.content.gui.config.ui;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiList;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiModal;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton.Variant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractContainerWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

public final class ListEditor {
   public static void open(Component title, List<String> initial, Consumer<List<String>> onSave) {
      List<String> items = new ArrayList(initial);
      int[] editing = new int[]{-1};
      CFHGuiList<String> list = (new CFHGuiList<String>(0, 0, 216, 110, 14, (g, item, index, x, y, w, h, hovered, selected, mx, my) -> {
         Palette p = Theme.current().palette();
         g.drawString(CFHGuiDraw.font(), CFHGuiDraw.truncate(Component.literal(item), w - 8), x + 4, y + 3, selected ? p.text() : p.textMuted(), Theme.current().isVanilla());
      })).gap(1).emptyText(Component.translatable("cfhgui_config.list.empty"));
      list.items(items);
      ConfigTextField field = new ConfigTextField(0, 0, 216, Component.translatable("cfhgui_config.list.value"));
      field.placeholder(Component.translatable("cfhgui_config.list.placeholder"));
      field.maxLength(1024);
      Runnable commit = () -> {
         String v = field.getValue().trim();
         if (!v.isEmpty()) {
            if (editing[0] >= 0 && editing[0] < items.size()) {
               items.set(editing[0], v);
            } else {
               items.add(v);
            }

            editing[0] = -1;
            field.setValue("");
            list.items(items);
         }
      };
      field.onEnter(commit);
      list.onActivate((item) -> {
         editing[0] = list.selectedIndex();
         field.setValue(item);
         field.setFocused(true);
      });
      CFHGuiButton add = (new CFHGuiButton(0, 0, 70, Component.translatable("cfhgui_config.list.add"), commit)).icon(Icon.PLUS);
      CFHGuiButton remove = (new CFHGuiButton(0, 0, 70, Component.translatable("cfhgui_config.list.remove"), () -> {
         int i = list.selectedIndex();
         if (i >= 0 && i < items.size()) {
            items.remove(i);
            list.items(items);
            list.select(Math.min(i, items.size() - 1));
         }

      })).icon(Icon.MINUS).variant(Variant.GHOST);
      CFHGuiButton up = (new CFHGuiButton(0, 0, 24, Component.empty(), () -> move(list, items, -1))).icon(Icon.ARROW_UP).variant(Variant.GHOST);
      CFHGuiButton down = (new CFHGuiButton(0, 0, 24, Component.empty(), () -> move(list, items, 1))).icon(Icon.ARROW_DOWN).variant(Variant.GHOST);
      CFHGuiModal modal = (new CFHGuiModal(title, Component.translatable("cfhgui_config.list.hint"), Icon.LIST)).width(300).extra(list).extra(field).extra(new ButtonRow(216, List.of(add, remove, up, down)));
      modal.button(Component.translatable("gui.cancel"), Variant.SECONDARY, (Runnable)null).button(Component.translatable("cfhgui_config.list.save"), Variant.PRIMARY, () -> onSave.accept(new ArrayList(items))).show();
   }

   private static void move(CFHGuiList<String> list, List<String> items, int dir) {
      int i = list.selectedIndex();
      int j = i + dir;
      if (i >= 0 && j >= 0 && j < items.size()) {
         String tmp = (String)items.get(i);
         items.set(i, (String)items.get(j));
         items.set(j, tmp);
         list.items(items);
         list.select(j);
      }
   }

   private ListEditor() {
   }

   static final class ButtonRow extends AbstractContainerWidget {
      private final List<CFHGuiButton> buttons;

      ButtonRow(int width, List<CFHGuiButton> buttons) {
         super(0, 0, width, 20, Component.empty());
         this.buttons = buttons;
      }

      private void layout() {
         int x = this.getX();

         for(CFHGuiButton b : this.buttons) {
            b.setX(x);
            b.setY(this.getY());
            x += b.getWidth() + 4;
         }

      }

      public List<? extends GuiEventListener> children() {
         return this.buttons;
      }

      public boolean mouseClicked(double mouseX, double mouseY, int button) {
         this.layout();

         for(CFHGuiButton b : this.buttons) {
            if (b.mouseClicked(mouseX, mouseY, button)) {
               return true;
            }
         }

         return false;
      }

      protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
         this.layout();

         for(CFHGuiButton b : this.buttons) {
            b.render(g, mouseX, mouseY, partialTick);
         }

      }

      protected void updateWidgetNarration(NarrationElementOutput out) {
      }
   }
}
