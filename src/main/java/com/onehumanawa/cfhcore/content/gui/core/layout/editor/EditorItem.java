package com.onehumanawa.cfhcore.content.gui.core.layout.editor;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.layout.ElementType;
import com.onehumanawa.cfhcore.content.gui.core.layout.LayoutAware;
import com.onehumanawa.cfhcore.content.gui.core.layout.ScreenLayout;
import com.onehumanawa.cfhcore.content.gui.core.layout.WidgetKey;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

final class EditorItem {
   final String id;
   final @Nullable String key;
   final ScreenLayout.@Nullable Element element;
   final @Nullable ElementType type;
   final @Nullable AbstractWidget widget;
   final LayoutAware.@Nullable Part part;
   final boolean backdrop;
   private final boolean shown;
   Rect rect;

   private EditorItem(String id, @Nullable String key, ScreenLayout.@Nullable Element element, @Nullable ElementType type, @Nullable AbstractWidget widget, LayoutAware.@Nullable Part part, boolean backdrop, boolean shown, Rect rect) {
      this.id = id;
      this.key = key;
      this.element = element;
      this.type = type;
      this.widget = widget;
      this.part = part;
      this.backdrop = backdrop;
      this.shown = shown;
      this.rect = rect;
   }

   static EditorItem vanilla(String key, AbstractWidget w, boolean backdrop) {
      return new EditorItem("w:" + key, key, (ScreenLayout.Element)null, (ElementType)null, w, (LayoutAware.Part)null, backdrop, true, Rect.of(w.getX(), w.getY(), w.getWidth(), w.getHeight()));
   }

   static EditorItem custom(ScreenLayout.Element e, @Nullable ElementType type, @Nullable AbstractWidget w, Rect rect) {
      return new EditorItem("e:" + e.id, (String)null, e, type, w, (LayoutAware.Part)null, false, true, rect);
   }

   static EditorItem part(LayoutAware.Part part, boolean shown) {
      Rect r = part.rect() == null ? Rect.of(0, 0, 0, 0) : part.rect();
      return new EditorItem("w:" + part.key(), part.key(), (ScreenLayout.Element)null, (ElementType)null, (AbstractWidget)null, part, false, shown, r);
   }

   static EditorItem ghost(String key) {
      return new EditorItem("w:" + key, key, (ScreenLayout.Element)null, (ElementType)null, (AbstractWidget)null, (LayoutAware.Part)null, false, false, Rect.of(0, 0, 0, 0));
   }

   boolean isCustom() {
      return this.element != null;
   }

   boolean isPart() {
      return this.part != null;
   }

   boolean fixed() {
      return this.element == null && this.widget == null;
   }

   boolean visible() {
      if (this.element != null) {
         return this.element.visible;
      } else {
         return this.widget != null ? this.widget.visible : this.shown;
      }
   }

   boolean locked() {
      return this.element != null && this.element.locked;
   }

   boolean hittable() {
      if (this.visible() && !this.backdrop) {
         if (this.part == null) {
            return this.widget != null;
         } else {
            return this.rect.w() > 0 && this.rect.h() > 0;
         }
      } else {
         return false;
      }
   }

   boolean movable() {
      return !this.locked() && !this.backdrop && !this.fixed();
   }

   Component name() {
      if (this.element != null) {
         return Component.literal(this.element.id);
      } else if (this.part != null) {
         return this.part.name();
      } else if (this.widget == null) {
         return (Component)(this.key == null ? Component.literal("?") : WidgetKey.name(this.key));
      } else {
         return Component.literal(widgetName(this.widget));
      }
   }

   Component typeName() {
      if (this.element != null) {
         return (Component)(this.type != null ? this.type.label() : Component.literal(this.element.type));
      } else if (this.part != null) {
         return EditorText.t("item.part");
      } else {
         return this.widget == null ? EditorText.t("item.left_out") : Component.literal(this.widget.getClass().getSimpleName());
      }
   }

   Icon icon() {
      if (this.element != null) {
         return this.type != null ? this.type.icon() : Icon.QUESTION;
      } else if (this.part != null) {
         return Icon.IMAGE;
      } else if (this.widget instanceof EditBox) {
         return Icon.TEXT;
      } else if (this.widget instanceof Checkbox) {
         return Icon.CHECK;
      } else {
         return !(this.widget instanceof Button) && this.widget != null ? Icon.PANEL : Icon.TOGGLE;
      }
   }

   static String widgetName(AbstractWidget w) {
      Component m = w.getMessage();
      String s = m == null ? "" : m.getString().trim();
      return s.isEmpty() ? w.getClass().getSimpleName() : s;
   }

   public String toString() {
      return this.id;
   }
}
