package com.onehumanawa.cfhcore.content.gui.core.compat.xaero;

import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;

final class XaeroPreflight {
   private static final String WRITTEN_AGAINST = "Minimap 26.4, World Map 1.44";
   private static final String[][] MINIMAP = new String[][]{{"xaero/common/minimap/render/MinimapRenderer", "renderMinimap"}, {"xaero/common/minimap/render/MinimapRendererHelper", "drawMyTexturedModalRectPre", "drawTexturedElipseInsideRectangle"}, {"xaero/hud/minimap/common/config/option/MinimapProfiledConfigOptions", "FRAME"}, {"xaero/hud/minimap/common/config/MinimapConfigConstants", "FRAME_NAMES"}, {"xaero/hud/minimap/compass/render/CompassRenderer", "drawCompass"}, {"xaero/hud/minimap/info/render/InfoDisplayRenderer", "render"}, {"xaero/hud/minimap/waypoint/render/world/WaypointWorldRenderer", "renderWaypointLabel"}, {"xaero/common/misc/Misc", "drawNormalText"}, {"xaero/hud/render/util/RenderBufferUtil", "addColoredRect"}};
   private static final String[][] WORLD_MAP = new String[][]{{"xaero/map/gui/GuiTexturedButton", "textureX", "textureY", "textureW", "textureH", "texture"}, {"xaero/map/graphics/MapRenderHelper", "drawStringWithBackground", "fillIntoExistingBuffer"}, {"xaero/map/element/MapElementMenuRenderer", "renderMenuElement", "renderScroll"}, {"xaero/map/gui/message/render/MessageBoxRenderer", "render"}};
   private static final String[][] LIBRARY = new String[][]{{"xaero/lib/client/gui/widget/Tooltip", "drawBox"}, {"xaero/lib/client/gui/widget/dropdown/DropDownWidget", "drawSlot", "drawMenu", "closed"}, {"xaero/lib/client/config/ClientConfigManager", "getEffective"}};

   private XaeroPreflight() {
   }

   static void run(boolean minimap, boolean worldMap) {
      try {
         List<String> missing = missing(minimap, worldMap);
         if (missing.isEmpty()) {
            return;
         }

         CFHGui.LOGGER.warn("[CFHGui] Xaero's maps here are not the versions CFHGui's restyle was written against ({}): there is no {}. What hangs on that keeps Xaero's own look.", "Minimap 26.4, World Map 1.44", String.join(", ", missing));
      } catch (Throwable t) {
         CFHGui.LOGGER.debug("[CFHGui] could not look through Xaero's classes: {}", t.toString());
      }

   }

   static List<String> missing(boolean minimap, boolean worldMap) throws Exception {
      List<String> out = new ArrayList();
      if (minimap) {
         missing(MINIMAP, out);
      }

      if (worldMap) {
         missing(WORLD_MAP, out);
      }

      missing(LIBRARY, out);
      return out;
   }

   private static void missing(String[][] expected, List<String> out) throws Exception {
      for(String[] entry : expected) {
         String simple = entry[0].substring(entry[0].lastIndexOf(47) + 1);
         Set<String> members = members(entry[0]);
         if (members == null) {
            out.add("class " + simple);
         } else {
            for(int i = 1; i < entry.length; ++i) {
               if (!members.contains(entry[i])) {
                  out.add(simple + "." + entry[i]);
               }
            }
         }
      }

   }

   private static Set<String> members(String name) throws Exception {
      InputStream in = XaeroMixinPlugin.class.getClassLoader().getResourceAsStream(name + ".class");

      final Set<String> names;
      label43: {
         Object var3;
         try {
            if (in == null) {
               names = null;
               break label43;
            }

            names = new HashSet();
            (new ClassReader(in)).accept(new ClassVisitor(589824) {
               public MethodVisitor visitMethod(int access, String method, String descriptor, String signature, String[] exceptions) {
                  names.add(method);
                  return null;
               }

               public FieldVisitor visitField(int access, String field, String descriptor, String signature, Object value) {
                  names.add(field);
                  return null;
               }
            }, 7);
            var3 = names;
         } catch (Throwable var5) {
            if (in != null) {
               try {
                  in.close();
               } catch (Throwable var4) {
                  var5.addSuppressed(var4);
               }
            }

            throw var5;
         }

         if (in != null) {
            in.close();
         }

         return (Set<String>)var3;
      }

      if (in != null) {
         in.close();
      }

      return names;
   }
}
