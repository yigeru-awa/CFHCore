package com.onehumanawa.cfhcore.content.gui.core.widget;

import com.onehumanawa.cfhcore.content.gui.core.widget.popup.MenuPopup;
import com.onehumanawa.cfhcore.content.gui.core.widget.popup.Popups;
import java.util.List;

public final class CFHGuiContextMenu {
   public static MenuPopup open(double mouseX, double mouseY, List<MenuPopup.Item> items) {
      MenuPopup p = new MenuPopup((int)mouseX, (int)mouseY, items);
      Popups.open(p);
      return p;
   }

   private CFHGuiContextMenu() {
   }
}
