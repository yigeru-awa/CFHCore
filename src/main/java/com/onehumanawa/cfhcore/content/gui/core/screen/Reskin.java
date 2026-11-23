package com.onehumanawa.cfhcore.content.gui.core.screen;

import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.config.CoreConfig;
import com.onehumanawa.cfhcore.content.gui.core.screen.reskin.BetterInventoryPalette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.jetbrains.annotations.Nullable;

public final class Reskin {
   private static Screen cachedScreen;
   private static boolean cachedResult;
   private static final List<String> OWN = new CopyOnWriteArrayList();
   private static int ownTooltips;
   private static final List<Predicate<Screen>> OTHERS = new CopyOnWriteArrayList();

   public static void own(String classPrefix) {
      if (classPrefix != null && !classPrefix.isEmpty() && !OWN.contains(classPrefix)) {
         OWN.add(classPrefix);
         invalidate();
      }
   }

   private static boolean cfhguiOwn(Screen s) {
      return ownClass(s.getClass().getName());
   }

   public static boolean ownClass(String cls) {
      if (cls.startsWith("com.onehumanawa.cfhcore.content.gui.")) {
         return true;
      } else if (OWN.isEmpty()) {
         return false;
      } else {
         for(String prefix : OWN) {
            if (cls.startsWith(prefix)) {
               return true;
            }
         }

         return false;
      }
   }

   public static void leaveTo(Predicate<Screen> other) {
      OTHERS.add(other);
      invalidate();
   }

   public static boolean leftToOther(@Nullable Screen s) {
      if (s != null && !OTHERS.isEmpty()) {
         for(Predicate<Screen> other : OTHERS) {
            try {
               if (other.test(s)) {
                  return true;
               }
            } catch (RuntimeException e) {
               CFHGui.LOGGER.warn("[CFHGui] asking whether another mod lays out {} failed: {}", s.getClass().getName(), e.toString());
            }
         }

         return false;
      } else {
         return false;
      }
   }

   public static void beginOwnTooltip() {
      ++ownTooltips;
   }

   public static void endOwnTooltip() {
      if (ownTooltips > 0) {
         --ownTooltips;
      }

   }

   public static boolean tooltip() {
      return ownTooltips > 0 ? !Theme.current().isVanilla() : dark();
   }

   public static boolean active() {
      Screen s = Minecraft.getInstance().screen;
      if (s == null) {
         return false;
      } else if (s == cachedScreen) {
         return cachedResult;
      } else {
         cachedScreen = s;
         cachedResult = inScope(s);
         return cachedResult;
      }
   }

   public static boolean dark() {
      return !Theme.current().isVanilla() && active();
   }

   public static boolean vanillaMotion() {
      return Theme.current().isVanilla() && active();
   }

   public static boolean inScope(@Nullable Screen s) {
      if (s == null) {
         return false;
      } else {
         CoreConfig cfg = CFHGui.config();
         String cls = s.getClass().getName();

         for(String deny : cfg.reskinDenylist) {
            if (!deny.isEmpty() && cls.startsWith(deny)) {
               return false;
            }
         }

         if (leftToOther(s)) {
            return false;
         } else if (BetterInventoryPalette.ownLook(s)) {
            return false;
         } else if (ScreenIds.isContainer(s)) {
            return containerScope(s, cfg);
         } else {
            String scope = cfg.reskinScope == null ? "" : cfg.reskinScope.toUpperCase(Locale.ROOT);
            {
               boolean var10000;
               switch (scope) {
                  case "NONE":
                     var10000 = false;
                     break;
                  case "ALL_NON_CONTAINER":
                     var10000 = true;
                     break;
                  case "VANILLA_AND_CFH":
                     var10000 = ScreenIds.isVanilla(s) || cfhguiOwn(s);
                     break;
                  default:
                     if (!ScreenIds.isVanilla(s) && !cfhguiOwn(s)) {
                        Iterator var6 = cfg.reskinAllowlist.iterator();

                        while(true) {
                           if (!var6.hasNext()) {
                              var10000 = false;
                              break;
                           }

                           String allow = (String)var6.next();
                           if (!allow.isEmpty() && (cls.startsWith(allow) || ScreenIds.guessNamespace(cls).equals(allow))) {
                              var10000 = true;
                              break;
                           }
                        }
                     } else {
                        var10000 = true;
                     }
               }

               return var10000;
            }
         }
      }
   }

   private static boolean containerScope(Screen s, CoreConfig cfg) {
      return cfg.reskinContainers;
   }

   public static boolean containers() {
      return Minecraft.getInstance().screen instanceof AbstractContainerScreen && active();
   }

   public static void invalidate() {
      cachedScreen = null;
   }

   private Reskin() {
   }
}
