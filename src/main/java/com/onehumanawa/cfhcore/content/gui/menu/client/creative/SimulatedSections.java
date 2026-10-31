package com.onehumanawa.cfhcore.content.gui.menu.client.creative;

import com.onehumanawa.cfhcore.content.gui.core.platform.CFHGuiPlatform;
import com.onehumanawa.cfhcore.content.gui.menu.CFHGuiMenu;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import org.jetbrains.annotations.Nullable;

final class SimulatedSections {
   static final int WIDTH = 162;
   static final int HEIGHT = 18;
   private static final int FAR = 1048576;
   private static boolean looked;
   private static @Nullable Object service;
   private static @Nullable Method serviceTab;
   private static @Nullable CreativeModeTab tab;
   private static @Nullable Field currentRow;
   private static @Nullable Object2IntMap<?> rows;
   private static @Nullable Field sections;
   private static @Nullable Method section;
   private static @Nullable Method sprite;
   private static @Nullable Method title;
   private static @Nullable Method playsOnHover;
   private static @Nullable Method text;
   private static @Nullable Method colour;
   private static @Nullable Method shade;
   private static @Nullable Method plate;
   private static @Nullable Method argb;
   private static @Nullable Method playing;
   private static final Map<Object, Banner> READ = new IdentityHashMap();

   private SimulatedSections() {
   }

   private static void look() {
      if (!looked) {
         looked = true;
         if (CFHGuiPlatform.get().isModLoaded("simulated")) {
            String tabs = "dev.simulated_team.simulated.registrate.simulated_tab.SimulatedCreativeTab";

            try {
               Class<?> type = Class.forName("dev.simulated_team.simulated.service.SimTabService");
               service = type.getField("INSTANCE").get(null);
               serviceTab = type.getMethod("getCreativeTab");
            } catch (RuntimeException | LinkageError | ReflectiveOperationException e) {
               service = null;
               CFHGuiMenu.LOGGER.warn("[CFHGui Menu] Create Simulated's tab is not where it was looked for ({}): its headings are plain rules", ((Throwable)e).toString());
            }

            try {
               currentRow = Class.forName("dev.simulated_team.simulated.registrate.simulated_tab.SimulatedCreativeTab").getField("CURRENT_ROW");
               if (currentRow.getType() != Integer.TYPE) {
                  currentRow = null;
               }
            } catch (RuntimeException | LinkageError | ReflectiveOperationException var5) {
               currentRow = null;
            }

            try {
               rows = (Object2IntMap)Class.forName("dev.simulated_team.simulated.registrate.simulated_tab.SimulatedCreativeTab").getField("SECTION_Y_VALUES").get(null);
               sections = Class.forName("dev.simulated_team.simulated.index.SimResourceManagers").getField("SIMULATED_SECTION");
               section = Class.forName("dev.simulated_team.simulated.api.SimpleResourceManager").getMethod("get", ResourceLocation.class);
               Class<?> one = Class.forName("dev.simulated_team.simulated.client.sections.SimulatedSection");
               sprite = one.getMethod("sprite");
               title = one.getMethod("title");
               playsOnHover = one.getMethod("animateOnHover");
               Class<?> name = title.getReturnType();
               text = name.getMethod("text");
               colour = name.getMethod("color");
               shade = name.getMethod("secondaryColor");
               plate = name.getMethod("background");
               argb = colour.getReturnType().getMethod("argb");
            } catch (RuntimeException | LinkageError | ReflectiveOperationException e) {
               rows = null;
               CFHGuiMenu.LOGGER.warn("[CFHGui Menu] Create Simulated's section banners cannot be read ({}): its tab's headings are plain rules", ((Throwable)e).toString());
            }

            try {
               playing = Class.forName("dev.simulated_team.simulated.registrate.simulated_tab.SimulatedCreativeTab").getMethod("setPlaying", ResourceLocation.class, Boolean.TYPE);
            } catch (RuntimeException | LinkageError | ReflectiveOperationException var3) {
               playing = null;
            }

         }
      }
   }

   static boolean owns(CreativeModeTab open) {
      look();
      if (tab == null && service != null && serviceTab != null) {
         try {
            tab = (CreativeModeTab)serviceTab.invoke(service);
         } catch (RuntimeException | LinkageError | ReflectiveOperationException var2) {
            service = null;
         }
      }

      return tab != null && tab == open;
   }

   static void quiet() {
      look();
      if (currentRow != null) {
         try {
            currentRow.setInt(null, 1048576);
         } catch (RuntimeException | ReflectiveOperationException var1) {
            currentRow = null;
         }

      }
   }

   static @Nullable Banner banner(int row) {
      look();
      Object2IntMap<?> known = rows;
      if (known != null && sections != null) {
         try {
            Object all = sections.get(null);
            if (all == null) {
               return null;
            } else {
               ObjectIterator var3 = known.object2IntEntrySet().iterator();

               while(var3.hasNext()) {
                  Object2IntMap.Entry<?> entry = (Object2IntMap.Entry)var3.next();
                  if (entry.getIntValue() == row) {
                     Object one = section.invoke(all, entry.getKey());
                     if (one != null) {
                        return read(one);
                     }
                  }
               }

               return null;
            }
         } catch (RuntimeException | LinkageError | ReflectiveOperationException e) {
            rows = null;
            CFHGuiMenu.LOGGER.warn("[CFHGui Menu] Create Simulated's section banners cannot be read ({}): its tab's headings are plain rules", ((Throwable)e).toString());
            return null;
         }
      } else {
         return null;
      }
   }

   private static Banner read(Object one) throws ReflectiveOperationException {
      Banner seen = (Banner)READ.get(one);
      if (seen != null) {
         return seen;
      } else {
         Object name = title.invoke(one);
         int light = (Integer)argb.invoke(colour.invoke(name));
         Optional<?> given = (Optional)shade.invoke(name);
         int dark = given.isPresent() ? (Integer)argb.invoke(given.get()) : light & -16777216 | fifthDarker(light >> 16) << 16 | fifthDarker(light >> 8) << 8 | fifthDarker(light);
         Banner banner = new Banner((ResourceLocation)sprite.invoke(one), (Component)text.invoke(name), (Integer)argb.invoke(plate.invoke(name)), light, dark, (Boolean)playsOnHover.invoke(one));
         if (READ.size() > 256) {
            READ.clear();
         }

         READ.put(one, banner);
         return banner;
      }
   }

   private static int fifthDarker(int channel) {
      return (channel & 255) * 4 / 5;
   }

   static void draw(GuiGraphics g, Font font, Banner banner, int x, int y, boolean pointed) {
      if (banner.playsOnHover() && playing != null) {
         try {
            playing.invoke(null, banner.sprite(), pointed);
         } catch (RuntimeException | LinkageError | ReflectiveOperationException var9) {
            playing = null;
         }
      }

      g.blitSprite(banner.sprite(), x, y, 162, 18);
      int nameX = x + 5;
      int nameY = y + 5;
      int nameW = font.width(banner.title());
      g.fill(x + 2, y + 2, nameX + nameW + 3, y + 18 - 2, banner.plate());
      g.drawString(font, banner.title(), nameX, nameY, banner.shade(), true);
      g.enableScissor(nameX, nameY, nameX + nameW, nameY + 5);
      g.drawString(font, banner.title(), nameX, nameY, banner.colour(), false);
      g.disableScissor();
   }

   static record Banner(ResourceLocation sprite, Component title, int plate, int colour, int shade, boolean playsOnHover) {
   }
}
