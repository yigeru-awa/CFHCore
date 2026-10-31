package com.onehumanawa.cfhcore.content.gui.config.page;

import com.onehumanawa.cfhcore.content.gui.config.mods.ModConfigTargets;
import com.onehumanawa.cfhcore.content.gui.config.option.Binding;
import com.onehumanawa.cfhcore.content.gui.config.option.KeyBinding;
import com.onehumanawa.cfhcore.content.gui.config.option.KeySearch;
import com.onehumanawa.cfhcore.content.gui.config.option.OptionType;
import com.onehumanawa.cfhcore.content.gui.config.resolver.VanillaOptions;
import com.onehumanawa.cfhcore.content.gui.config.ui.ConfigSearchField;
import com.onehumanawa.cfhcore.content.gui.config.ui.OptionPageBase;
import com.onehumanawa.cfhcore.content.gui.config.ui.OptionRow;
import com.onehumanawa.cfhcore.content.gui.config.ui.Section;
import com.onehumanawa.cfhcore.content.gui.core.client.KeyBinds;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.core.platform.ModInfo;
import com.onehumanawa.cfhcore.content.gui.core.platform.CFHGuiPlatform;
import com.onehumanawa.cfhcore.content.gui.core.screen.SidebarScreen;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiIconButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiKeybindButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiModal;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiToggle;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton.Variant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractContainerWidget;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public final class ControlsPage extends OptionPageBase {
   private static final List<String> VANILLA_ORDER = List.of("key.categories.movement", "key.categories.gameplay", "key.categories.inventory", "key.categories.creative", "key.categories.multiplayer", "key.categories.ui", "key.categories.misc");
   public static final String KEYS_TAB = "keys";
   private boolean unboundOnly;
   private String keySearch = "";
   private boolean keySearchFocus;
   private @Nullable ConfigSearchField keyField;
   private @Nullable Toolbar keyToolbar;
   private static final List<String> CONTROLLER_MODS = List.of("controlify", "controllable", "midnightcontrols");

   public ControlsPage() {
      super("controls", Component.translatable("cfhgui_config.page.controls"), Icon.KEYBOARD);
   }

   static boolean keyMatches(KeyMapping m, String query) {
      return KeySearch.matches(m, query, "");
   }

   protected List<Section> sections() {
      List<Section> out = new ArrayList();
      out.add(Section.of("mouse", Component.translatable("cfhgui_config.controls.mouse"), VanillaOptions.all("mouseSensitivity", "invertYMouse", "rawMouseInput", "discrete_mouse_scroll", "mouseWheelSensitivity", "touchscreen")));
      out.add(Section.of("movement", Component.translatable("cfhgui_config.controls.movement"), VanillaOptions.all("toggleSprint", "toggleCrouch", "autoJump")));
      Component keysTitle = Component.translatable("cfhgui_config.controls.keys");
      Section tools = Section.of("keys", Component.empty()).fixed().tab("keys", keysTitle);
      tools.custom(this::toolbar);
      out.add(tools);
      Map<String, List<KeyMapping>> byCategory = new LinkedHashMap();
      List<KeyMapping> all = new ArrayList(List.of(Minecraft.getInstance().options.keyMappings));
      all.sort(Comparator.comparing(KeyMapping::getCategory).thenComparing((mx) -> Component.translatable(mx.getName()).getString()));

      for(KeyMapping m : all) {
         if ((!this.unboundOnly || m.isUnbound()) && keyMatches(m, this.keySearch)) {
            ((List<KeyMapping>)byCategory.computeIfAbsent(m.getCategory(), (k) -> new ArrayList())).add(m);
         }
      }

      List<String> cats = new ArrayList(byCategory.keySet());
      cats.sort(Comparator.comparingInt((String c) -> {
         int i = VANILLA_ORDER.indexOf(c);
         return i < 0 ? 100 : i;
      }).thenComparing((String c) -> Component.translatable(c).getString()));

      for(String cat : cats) {
         Section s = Section.of("keys." + cat, Component.translatable(cat)).tab("keys", keysTitle);

         for(KeyMapping m : (List<KeyMapping>)byCategory.get(cat)) {
            s.add(new KeyBinding(m));
         }

         out.add(s);
      }

      Section controller = controllerSection();
      if (controller != null) {
         out.add(controller);
      }

      return out;
   }

   private static @Nullable Section controllerSection() {
      for(String modId : CONTROLLER_MODS) {
         if (CFHGuiPlatform.get().isModLoaded(modId)) {
            String name = modId;
            String version = "";

            for(ModInfo m : CFHGuiPlatform.get().allMods()) {
               if (m.id().equals(modId)) {
                  name = m.name();
                  version = m.version();
                  break;
               }
            }

            Component title = Component.translatable("cfhgui_config.controls.controller");
            Section s = Section.of("controller", title).fixed().tab("controller", title);
            String provider = name + (version.isBlank() ? "" : " " + version);
            s.add(Binding.of("controls:controller", OptionType.INFO, (Component)Component.translatable("cfhgui_config.controls.controller.info")).getter(() -> provider).searchWords("controller gamepad joystick " + modId));
            int i = 0;

            for(ModConfigTargets.Target t : ModConfigTargets.forMod(modId)) {
               int var10001 = i++;
               s.add(Binding.of("controls:controller_" + var10001, OptionType.ACTION, t.label()).tooltip((Component)Component.translatable("cfhgui_config.controls.controller.open", new Object[]{name})).actionIcon(t.icon()).action(Component.translatable("cfhgui_config.row.open"), t.open()).searchWords("controller gamepad " + modId));
            }

            return s;
         }
      }

      return null;
   }

   protected boolean pills(String tabKey) {
      return !"keys".equals(tabKey);
   }

   private AbstractWidget toolbar(int w) {
      boolean tight = w < 376;
      Component unboundLabel = Component.translatable("cfhgui_config.controls.unbound_only");
      int toggleW = tight ? Math.min(150, Minecraft.getInstance().font.width(unboundLabel) + 22 + 16) : 150;
      int resetW = tight ? 20 : 120;
      int searchW = Math.max(60, w - toggleW - resetW - 16);
      ConfigSearchField search = new ConfigSearchField(0, 0, searchW, (s) -> {
         if (!s.equals(this.keySearch)) {
            this.keySearch = s;
            this.keySearchFocus = true;
            this.rebuild();
         }
      });
      search.setValue(this.keySearch);
      search.placeholder(Component.translatable("cfhgui_config.controls.search_keys"));
      this.keyField = search;
      CFHGuiToggle unbound = new CFHGuiToggle(0, 0, toggleW, unboundLabel, this.unboundOnly, (v) -> {
         this.unboundOnly = v;
         this.rebuild();
      });
      Runnable resetAll = () -> CFHGuiModal.confirmDanger(Component.translatable("cfhgui_config.controls.reset_keys"), Component.translatable("cfhgui_config.controls.reset_keys.body"), Component.translatable("cfhgui_config.controls.reset_keys"), () -> {
            for(KeyMapping m : Minecraft.getInstance().options.keyMappings) {
               KeyBinds.reset(m);
            }

            KeyMapping.resetMapping();
            Minecraft.getInstance().options.save();
            this.rebuild();
         });
      CFHGuiButton reset = (CFHGuiButton)(tight ? (new CFHGuiIconButton(0, 0, 20, Icon.UNDO, Component.translatable("cfhgui_config.controls.reset_keys"), resetAll)).variant(Variant.DANGER) : (new CFHGuiButton(0, 0, resetW, Component.translatable("cfhgui_config.controls.reset_keys"), resetAll)).icon(Icon.UNDO).variant(Variant.DANGER));
      Toolbar t = new Toolbar(w, List.of(search, unbound, reset));
      this.keyToolbar = t;
      return t;
   }

   public void build(SidebarScreen screen, Rect area) {
      this.keyField = null;
      this.keyToolbar = null;
      super.build(screen, area);
      this.markConflicts();
      if (this.keySearchFocus) {
         this.keySearchFocus = false;
         if (this.keyField != null && this.keyToolbar != null) {
            screen.setFocused(this.keyToolbar);
            this.keyToolbar.setFocused(this.keyField);
            this.keyField.setFocused(true);
         }
      }

   }

   private void markConflicts() {
      List<KeyMapping> all = List.of(Minecraft.getInstance().options.keyMappings);

      for(OptionRow row : this.rows()) {
         AbstractWidget var5 = row.control();
         if (var5 instanceof CFHGuiKeybindButton btn) {
            KeyMapping m = btn.mapping();
            if (m.isUnbound()) {
               btn.conflict(false);
            } else {
               List<String> clashes = new ArrayList();

               for(KeyMapping o : all) {
                  if (o != m && !o.isUnbound() && m.same(o)) {
                     clashes.add(Component.translatable(o.getName()).getString());
                  }
               }

               btn.conflict(!clashes.isEmpty());
               if (!clashes.isEmpty()) {
                  btn.tip(Component.translatable("cfhgui_config.controls.conflict", new Object[]{String.join(", ", clashes)}));
               } else {
                  btn.tip((Component)null);
               }
            }
         }
      }

   }

   protected void onRowChanged(OptionRow row) {
      super.onRowChanged(row);
      this.markConflicts();
   }

   public @Nullable CFHGuiKeybindButton activeCapture() {
      for(OptionRow row : this.rows()) {
         AbstractWidget var4 = row.control();
         if (var4 instanceof CFHGuiKeybindButton b) {
            if (b.isCapturing()) {
               return b;
            }
         }
      }

      return null;
   }

   public boolean captureKey(int key, int scan, int mods) {
      CFHGuiKeybindButton b = this.activeCapture();
      if (b == null) {
         return false;
      } else {
         b.keyPressed(key, scan, mods);
         this.markConflicts();
         return true;
      }
   }

   public boolean captureMouse(int button) {
      CFHGuiKeybindButton b = this.activeCapture();
      if (b == null) {
         return false;
      } else {
         b.captureMouse(button);
         this.markConflicts();
         return true;
      }
   }

   static final class Toolbar extends AbstractContainerWidget {
      private final List<AbstractWidget> items;

      Toolbar(int width, List<AbstractWidget> items) {
         super(0, 0, width, 20, Component.empty());
         this.items = items;
      }

      private void layout() {
         int x = this.getX();

         for(AbstractWidget w : this.items) {
            w.setX(x);
            w.setY(this.getY());
            x += w.getWidth() + 8;
         }

      }

      public List<? extends GuiEventListener> children() {
         return this.items;
      }

      public boolean mouseClicked(double mouseX, double mouseY, int button) {
         this.layout();

         for(AbstractWidget w : this.items) {
            if (w.mouseClicked(mouseX, mouseY, button)) {
               this.setFocused(w);
               return true;
            }
         }

         return false;
      }

      protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
         this.layout();

         for(AbstractWidget w : this.items) {
            w.render(g, mouseX, mouseY, partialTick);
         }

      }

      protected void updateWidgetNarration(NarrationElementOutput out) {
      }
   }
}
