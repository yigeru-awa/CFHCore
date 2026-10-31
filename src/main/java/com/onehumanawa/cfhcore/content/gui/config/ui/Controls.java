package com.onehumanawa.cfhcore.content.gui.config.ui;

import com.onehumanawa.cfhcore.content.gui.config.option.Choice;
import com.onehumanawa.cfhcore.content.gui.config.option.KeyBinding;
import com.onehumanawa.cfhcore.content.gui.config.option.NumberRange;
import com.onehumanawa.cfhcore.content.gui.config.option.OptionBinding;
import com.onehumanawa.cfhcore.content.gui.config.option.OptionType;
import com.onehumanawa.cfhcore.content.gui.config.option.OptionValues;
import com.onehumanawa.cfhcore.content.gui.core.client.KeyBinds;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiDropdown;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiKeybindButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSlider;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiToggle;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.Nullable;

public final class Controls {
   public static final int WIDTH = 150;

   public static @Nullable AbstractWidget create(OptionBinding b, int x, int y, int w, Consumer<Object> onChange) {
      Object v = b.get();
      switch (b.type()) {
         case BOOLEAN:
            int var10002 = x + w - 22 - 10;
            MutableComponent var10005 = Component.empty();
            boolean var10006 = OptionValues.asBoolean(v, false);
            Objects.requireNonNull(onChange);
            return new CFHGuiToggle(var10002, y, 32, var10005, var10006, onChange::accept);
         case INT:
         case DOUBLE:
            NumberRange r = b.range();
            boolean integer = b.type() == OptionType.INT;
            if (r != null && r.isBounded()) {
               double step = integer ? (double)Math.max(1L, Math.round(r.effectiveStep())) : r.effectiveStep();
               return new ConfigSlider(x, y, w, r.min(), r.max(), step, OptionValues.asDouble(v, r.min()), (d) -> b.valueText(integer ? Math.round(d) : d).getString(), (d) -> onChange.accept(integer ? Math.round(d) : d));
            }

            ConfigTextField f = new ConfigTextField(x, y, w, Component.empty());
            f.setValue(v == null ? "" : (integer ? Long.toString(OptionValues.asLong(v, 0L)) : OptionValues.formatDouble(OptionValues.asDouble(v, (double)0.0F))));
            f.maxLength(64);
            f.onChange((s) -> {
               String t = s.trim();

               boolean ok;
               try {
                  if (integer) {
                     Long.parseLong(t);
                  } else {
                     Double.parseDouble(t);
                  }

                  ok = true;
               } catch (NumberFormatException var8) {
                  ok = false;
               }

               f.setInvalid(!ok);
               if (ok) {
                  ApplyQueue.later("field:" + b.id(), 400, () -> onChange.accept(integer ? Long.parseLong(t) : Double.parseDouble(t)));
               }

            });
            return f;
         case CHOICE:
            List<Choice> choices = b.choices();
            Choice cur = null;
            String id = OptionValues.asString(v);

            for(Choice c : choices) {
               if (c.id().equals(id)) {
                  cur = c;
                  break;
               }
            }

            return new CFHGuiDropdown<Choice>(x, y, w, choices, cur, Choice::label, (cx) -> onChange.accept(cx.id()));
         case STRING:
            ConfigTextField f2 = new ConfigTextField(x, y, w, Component.empty());
            f2.maxLength(4096);
            f2.setValue(OptionValues.asString(v));
            f2.onChange((s) -> ApplyQueue.later("field:" + b.id(), 500, () -> onChange.accept(s)));
            return f2;
         case COLOR:
            return new ConfigColorField(x, y, w, OptionValues.asColor(v, -1), (cx) -> onChange.accept(cx));
         case LIST:
            List<String> items = OptionValues.asList(v);
            return (new CFHGuiButton(x, y, w, Component.translatable("cfhgui_config.row.edit_list", new Object[]{items.size()}), () -> {
               Component var10000 = b.label();
               List var10001 = OptionValues.asList(b.get());
               Objects.requireNonNull(onChange);
               ListEditor.open(var10000, var10001, onChange::accept);
            })).icon(Icon.LIST);
         case KEYBIND:
            if (b instanceof KeyBinding kb) {
               return new CFHGuiKeybindButton(x, y, w, kb.mapping(), (m) -> onChange.accept(KeyBinds.save(m)));
            }

            return null;
         case ACTION:
            Runnable rr = b.action();
            return (new CFHGuiButton(x, y, w, b.actionLabel(), rr == null ? () -> {
            } : rr)).icon(b.actionIcon());
         default:
            return null;
      }
   }

   public static void refresh(OptionBinding b, @Nullable AbstractWidget control) {
      if (control != null) {
         Object v = b.get();
         if (control instanceof CFHGuiToggle) {
            CFHGuiToggle t = (CFHGuiToggle)control;
            t.setValue(OptionValues.asBoolean(v, false));
         } else if (control instanceof CFHGuiSlider) {
            CFHGuiSlider s = (CFHGuiSlider)control;
            s.setValue(OptionValues.asDouble(v, (double)0.0F));
         } else if (control instanceof CFHGuiDropdown) {
            CFHGuiDropdown<Choice> d = (CFHGuiDropdown<Choice>)control;
            String id = OptionValues.asString(v);

            for(Choice c : b.choices()) {
               if (c.id().equals(id)) {
                  d.setValue(c);
                  break;
               }
            }
         } else if (control instanceof ConfigColorField) {
            ConfigColorField c = (ConfigColorField)control;
            c.setColor(OptionValues.asColor(v, -1));
         } else if (control instanceof CFHGuiKeybindButton) {
            CFHGuiKeybindButton k = (CFHGuiKeybindButton)control;
            k.refresh();
         } else if (control instanceof ConfigTextField) {
            ConfigTextField f = (ConfigTextField)control;
            String text = b.type() == OptionType.INT ? Long.toString(OptionValues.asLong(v, 0L)) : (b.type() == OptionType.DOUBLE ? OptionValues.formatDouble(OptionValues.asDouble(v, (double)0.0F)) : OptionValues.asString(v));
            if (!f.getValue().equals(text)) {
               f.setValue(text);
            }

            f.setInvalid(false);
         } else if (control instanceof CFHGuiButton) {
            CFHGuiButton btn = (CFHGuiButton)control;
            if (b.type() == OptionType.LIST) {
               btn.setMessage(Component.translatable("cfhgui_config.row.edit_list", new Object[]{OptionValues.asList(v).size()}));
            }
         }

         control.active = b.enabled();
      }
   }

   private Controls() {
   }
}
