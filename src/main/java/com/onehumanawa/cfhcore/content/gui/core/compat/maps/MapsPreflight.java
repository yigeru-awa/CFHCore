package com.onehumanawa.cfhcore.content.gui.core.compat.maps;

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

final class MapsPreflight {
   private static final String WRITTEN_AGAINST = "World Map 1.44, Minimap 26.4";
   private static final String[][] WORLD_MAP = new String[][]{{"xaero/map/gui/GuiMap", "getRightClickOptions", "rightClickX", "rightClickY", "rightClickZ", "rightClickDim", "mapTileSelection"}, {"xaero/map/gui/MapTileSelection", "getLeft", "getRight", "getTop", "getBottom"}, {"xaero/map/gui/dropdown/rightclick/RightClickOption", "onAction", "getDisplayName", "isActive"}, {"xaero/map/WorldMap", "mapElementRenderHandler"}, {"xaero/map/element/MapElementRenderHandler", "add"}, {"xaero/map/element/render/ElementRenderer", "renderElement", "shouldRender"}, {"xaero/map/element/render/ElementReader", "getRightClickOptions", "isHoveredOnMap"}};
   private static final String[][] MINIMAP = new String[][]{{"xaero/hud/minimap/Minimap", "getOverMapRendererHandler", "getWorldRendererHandler"}, {"xaero/hud/minimap/element/render/MinimapElementRendererHandler", "add"}, {"xaero/hud/minimap/element/render/MinimapElementRenderer", "renderElement", "shouldRender"}, {"xaero/hud/minimap/element/render/MinimapElementRenderLocation", "OVER_MINIMAP", "IN_WORLD"}};

   private MapsPreflight() {
   }

   static void run(boolean worldMap, boolean minimap) {
      try {
         List<String> missing = missing(worldMap, minimap);
         if (missing.isEmpty()) {
            return;
         }

         CFHGui.LOGGER.warn("[CFHGui] Xaero's maps here are not the versions CFHGui's places were written against ({}): there is no {}. What hangs on that is left out.", "World Map 1.44, Minimap 26.4", String.join(", ", missing));
      } catch (Throwable t) {
         CFHGui.LOGGER.debug("[CFHGui] could not look through Xaero's classes: {}", t.toString());
      }

   }

   static List<String> missing(boolean worldMap, boolean minimap) throws Exception {
      List<String> out = new ArrayList();
      if (worldMap) {
         missing(WORLD_MAP, out);
      }

      if (minimap) {
         missing(MINIMAP, out);
      }

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
      InputStream in = MapsPreflight.class.getClassLoader().getResourceAsStream(name + ".class");

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
