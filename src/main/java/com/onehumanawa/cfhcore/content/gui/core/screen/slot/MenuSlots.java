package com.onehumanawa.cfhcore.content.gui.core.screen.slot;

import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.client.CoreActions;
import com.onehumanawa.cfhcore.content.gui.core.config.CoreConfig;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.module.Modules;
import com.onehumanawa.cfhcore.content.gui.core.screen.ScreenSwaps;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiToasts;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public final class MenuSlots {
   public static final String UI_MODULE = "cfhgui";
   public static final String STANDALONE_WINDOW = "cfhgui.earlywindow.standalone";
   private static final Object LOCK = new Object();
   private static final Map<String, MenuSlot> SLOTS = new LinkedHashMap();
   private static final Set<String> PLACEHOLDERS = new HashSet();
   private static final Map<String, Map<Layout, Provider>> PROVIDERS = new HashMap();
   private static final Map<String, EnumSet<Layout>> SUPPORTED = new HashMap();
   private static final Map<String, String> CLASS_TO_SLOT = new ConcurrentHashMap();
   private static final List<Runnable> LISTENERS = new CopyOnWriteArrayList();
   private static final Set<String> STYLE_ONLY = ConcurrentHashMap.newKeySet();
   private static final List<Function<Screen, String>> RESOLVERS = new CopyOnWriteArrayList();
   private static final List<Function<MenuSlot, String>> HOLDERS = new CopyOnWriteArrayList();

   public static void register(MenuSlot slot) {
      synchronized(LOCK) {
         MenuSlot existing = (MenuSlot)SLOTS.get(slot.id());
         if (existing != null && !PLACEHOLDERS.remove(slot.id())) {
            return;
         }

         SLOTS.put(slot.id(), slot);

         for(String cls : slot.vanillaClasses()) {
            CLASS_TO_SLOT.put(cls, slot.id());
            ScreenSwaps.registerByName(cls, (s) -> swap(slot.id(), s));
         }
      }

      changed();
   }

   public static void registerStyleOnly(MenuSlot slot) {
      STYLE_ONLY.add(slot.id());
      register(slot);
   }

   public static boolean styleOnly(String slotId) {
      return STYLE_ONLY.contains(slotId);
   }

   public static void resolveWith(Function<Screen, String> resolver) {
      RESOLVERS.add(resolver);
   }

   public static void heldBy(Function<MenuSlot, String> holder) {
      HOLDERS.add(holder);
      changed();
   }

   public static @Nullable String holder(String slotId) {
      if (!HOLDERS.isEmpty() && CFHGui.config().yieldToMenuMods && !layoutOverride(slotId).isPresent()) {
         MenuSlot slot;
         synchronized(LOCK) {
            slot = (MenuSlot)SLOTS.get(slotId);
         }

         if (slot != null && slot.vanillaMenu()) {
            for(Function<MenuSlot, String> h : HOLDERS) {
               String who = (String)h.apply(slot);
               if (who != null) {
                  return who;
               }
            }

            return null;
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   public static void provide(String slotId, Layout layout, Function<Screen, Screen> factory) {
      provide(slotId, layout, 0, factory);
   }

   public static void provide(String slotId, Layout layout, int priority, Function<Screen, Screen> factory) {
      synchronized(LOCK) {
         ensure(slotId);
         if (layout == Layout.VANILLA && ((MenuSlot)SLOTS.get(slotId)).vanillaMenu()) {
            throw new IllegalArgumentException("the vanilla layout of " + slotId + " is Minecraft's own screen");
         }

         Map<Layout, Provider> m = (Map)PROVIDERS.computeIfAbsent(slotId, (k) -> new EnumMap(Layout.class));
         Provider current = (Provider)m.get(layout);
         if (current == null || priority >= current.priority()) {
            m.put(layout, new Provider(priority, factory));
         }
      }

      changed();
   }

   public static void support(String slotId, Layout layout, boolean on) {
      synchronized(LOCK) {
         ensure(slotId);
         EnumSet<Layout> set = (EnumSet)SUPPORTED.computeIfAbsent(slotId, (k) -> EnumSet.noneOf(Layout.class));
         if (on) {
            set.add(layout);
         } else {
            set.remove(layout);
         }
      }

      changed();
   }

   private static void ensure(String slotId) {
      if (!SLOTS.containsKey(slotId)) {
         int colon = slotId.indexOf(58);
         SLOTS.put(slotId, MenuSlot.custom(slotId, Component.literal(colon < 0 ? slotId : slotId.substring(colon + 1)), colon < 0 ? "" : slotId.substring(0, colon)));
         PLACEHOLDERS.add(slotId);
      }
   }

   public static void onChange(Runnable listener) {
      LISTENERS.add(listener);
   }

   private static void changed() {
      for(Runnable r : LISTENERS) {
         try {
            r.run();
         } catch (Exception e) {
            CFHGui.LOGGER.warn("[CFHGui] menu slot listener threw: {}", e.toString());
         }
      }

   }

   public static List<MenuSlot> all() {
      synchronized(LOCK) {
         return new ArrayList(SLOTS.values());
      }
   }

   public static Optional<MenuSlot> get(String slotId) {
      synchronized(LOCK) {
         return Optional.ofNullable((MenuSlot)SLOTS.get(slotId));
      }
   }

   public static boolean uiModuleLoaded() {
      return Modules.isLoaded("cfhgui");
   }

   public static boolean windowAlone(String slotId) {
      return "cfhgui:loading".equals(slotId) && !uiModuleLoaded() && Boolean.getBoolean("cfhgui.earlywindow.standalone");
   }

   private static boolean has(String slotId, Layout layout) {
      Map<Layout, Provider> m = (Map)PROVIDERS.get(slotId);
      if (m != null && m.containsKey(layout)) {
         return true;
      } else {
         EnumSet<Layout> s = (EnumSet)SUPPORTED.get(slotId);
         return s != null && s.contains(layout);
      }
   }

   public static Set<Layout> available(String slotId) {
      synchronized(LOCK) {
         MenuSlot slot = (MenuSlot)SLOTS.get(slotId);
         EnumSet<Layout> out = EnumSet.noneOf(Layout.class);
         if (slot == null) {
            return out;
         } else if (windowAlone(slotId)) {
            out.add(Layout.OVERHAUL);
            return out;
         } else {
            boolean ui = uiModuleLoaded();
            if (slot.vanillaMenu() || has(slotId, Layout.VANILLA)) {
               out.add(Layout.VANILLA);
            }

            if (has(slotId, Layout.CUSTOM) && (ui || !slot.vanillaMenu())) {
               out.add(Layout.CUSTOM);
            }

            if (has(slotId, Layout.OVERHAUL) && ui) {
               out.add(Layout.OVERHAUL);
            }

            return out;
         }
      }
   }

   // the overhaul layout is the only requestable layout; effective() still degrades per slot
   public static Layout requested(String slotId) {
      return Layout.OVERHAUL;
   }

   public static Layout effective(String slotId) {
      Layout requested = requested(slotId);
      if (requested != Layout.VANILLA && holder(slotId) != null) {
         return Layout.VANILLA;
      } else {
         Set<Layout> avail = available(slotId);
         if (avail.contains(requested)) {
            return requested;
         } else {
            for(Layout l = requested.lower(); l != null; l = l.lower()) {
               if (avail.contains(l)) {
                  return l;
               }
            }

            for(Layout l : Layout.values()) {
               if (avail.contains(l)) {
                  return l;
               }
            }

            MenuSlot slot = (MenuSlot)get(slotId).orElse(null);
            return slot != null && !slot.vanillaMenu() ? Layout.CUSTOM : Layout.VANILLA;
         }
      }
   }

   // the CFHGui skin is forced for every slot
   public static Style style(String slotId) {
      return Style.CFHGUI;
   }

   public static boolean hasOverride(String slotId) {
      return layoutOverride(slotId).isPresent() || styleOverride(slotId).isPresent();
   }

   public static Optional<Layout> layoutOverride(String slotId) {
      CoreConfig.ScreenOverride o = CFHGui.config().override(slotId);
      return o != null && o.layout != null ? Optional.of(Layout.parse(o.layout, globalLayout())) : Optional.empty();
   }

   public static Optional<Style> styleOverride(String slotId) {
      CoreConfig.ScreenOverride o = CFHGui.config().override(slotId);
      return o != null && o.style != null ? Optional.of(Style.parse(o.style, CFHGui.config().style())) : Optional.empty();
   }

   public static Layout globalLayout() {
      return CFHGui.config().layout();
   }

   public static @Nullable String slotOf(@Nullable Screen screen) {
      if (screen == null) {
         return null;
      } else {
         String slot = (String)CLASS_TO_SLOT.get(screen.getClass().getName());
         if (slot == null && !RESOLVERS.isEmpty()) {
            for(Function<Screen, String> resolver : RESOLVERS) {
               String resolved = (String)resolver.apply(screen);
               if (resolved != null) {
                  return resolved;
               }
            }

            return null;
         } else {
            return slot;
         }
      }
   }

   public static Style styleFor(@Nullable Screen screen) {
      String slot = slotOf(screen);
      return slot == null ? CFHGui.config().style() : style(slot);
   }

   public static void setGlobalLayout(Layout layout) {
      CFHGui.configFile().update((c) -> c.setLayout(layout));
      refresh();
   }

   public static void setGlobalStyle(Style style) {
      CFHGui.configFile().update((c) -> c.setStyle(style));
      refresh();
   }

   public static void setLayoutOverride(String slotId, @Nullable Layout layout) {
      CFHGui.configFile().update((c) -> {
         if (layout == null) {
            CoreConfig.ScreenOverride o = c.override(slotId);
            if (o != null) {
               o.layout = null;
            }
         } else {
            c.overrideOrCreate(slotId).layout = layout.name();
         }

         c.clearEmptyOverrides();
      });
      refresh();
   }

   public static void setStyleOverride(String slotId, @Nullable Style style) {
      CFHGui.configFile().update((c) -> {
         if (style == null) {
            CoreConfig.ScreenOverride o = c.override(slotId);
            if (o != null) {
               o.style = null;
            }
         } else {
            c.overrideOrCreate(slotId).style = style.name();
         }

         c.clearEmptyOverrides();
      });
      refresh();
   }

   public static void clearOverrides(String slotId) {
      CFHGui.configFile().update((c) -> {
         if (c.screens != null) {
            c.screens.remove(slotId);
         }

      });
      refresh();
   }

   public static void refresh() {
      Theme.reload();
      changed();
   }

   public static void open(String slotId, @Nullable Screen parent) {
      Minecraft mc = Minecraft.getInstance();
      MenuSlot slot = (MenuSlot)get(slotId).orElse(null);
      if (slot != null && slot.vanillaMenu()) {
         Function<Screen, Screen> vanilla = (Function)CoreActions.SCREEN_FACTORIES.get(slotId);
         if (vanilla != null) {
            mc.setScreen((Screen)vanilla.apply(parent));
            return;
         }
      }

      Screen s = create(slotId, parent);
      if (s != null) {
         mc.setScreen(s);
      } else {
         CFHGui.LOGGER.warn("[CFHGui] nothing provides the menu slot {}", slotId);
         CFHGuiToasts.show(Component.translatable("cfhgui.slot.unavailable"), Component.literal(slotId), Icon.WARNING);
      }
   }

   public static Screen preview(String slotId, Supplier<Screen> vanilla) {
      Screen v = (Screen)vanilla.get();
      Screen s = swap(slotId, v);
      return s != null ? s : v;
   }

   public static @Nullable Screen create(String slotId, @Nullable Screen parent) {
      for(Layout l = effective(slotId); l != null; l = l.lower()) {
         Screen s = build(slotId, l, parent);
         if (s != null) {
            return s;
         }
      }

      return null;
   }

   static @Nullable Screen swap(String slotId, Screen vanilla) {
      for(Layout l = effective(slotId); l != null && l != Layout.VANILLA; l = l.lower()) {
         Screen s = build(slotId, l, vanilla);
         if (s != null) {
            return s;
         }
      }

      return null;
   }

   private static @Nullable Screen build(String slotId, Layout layout, @Nullable Screen input) {
      Provider p;
      synchronized(LOCK) {
         Map<Layout, Provider> m = (Map)PROVIDERS.get(slotId);
         p = m == null ? null : (Provider)m.get(layout);
      }

      if (p == null) {
         return null;
      } else {
         try {
            Screen s = (Screen)p.factory().apply(input);
            if (s != null) {
               CLASS_TO_SLOT.put(s.getClass().getName(), slotId);
            }

            return s;
         } catch (Exception e) {
            CFHGui.LOGGER.error("[CFHGui] the {} layout of {} failed to build; falling back", new Object[]{layout.key(), slotId, e});
            return null;
         }
      }
   }

   private MenuSlots() {
   }

   static {
      CoreSlots.register();
   }

   private static record Provider(int priority, Function<Screen, Screen> factory) {
   }
}
