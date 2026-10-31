package com.onehumanawa.cfhcore.content.gui.core.compat.maps.xaero;

import com.mojang.blaze3d.vertex.PoseStack;
import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.compat.maps.Linkage;
import com.onehumanawa.cfhcore.content.gui.core.compat.maps.MapMenus;
import com.onehumanawa.cfhcore.content.gui.core.compat.maps.MapPins;
import com.onehumanawa.cfhcore.content.gui.core.compat.maps.PinPainter;
import com.onehumanawa.cfhcore.content.gui.core.map.MapAction;
import com.onehumanawa.cfhcore.content.gui.core.map.MapPoints;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import xaero.map.element.render.ElementReader;
import xaero.map.element.render.ElementRenderInfo;
import xaero.map.element.render.ElementRenderLocation;
import xaero.map.element.render.ElementRenderProvider;
import xaero.map.element.render.ElementRenderer;
import xaero.map.graphics.renderer.multitexture.MultiTextureRenderTypeRendererProvider;
import xaero.map.gui.IRightClickableElement;
import xaero.map.gui.dropdown.rightclick.RightClickOption;

final class WorldMapPins extends ElementRenderer<MapPins.Pin, WorldMapPins.View, WorldMapPins> {
   private boolean broken;

   WorldMapPins() {
      super(new View(), new Provider(), new Reader());
   }

   static boolean fits() {
      return Linkage.complete(WorldMapPins.class) && Linkage.complete(Provider.class) && Linkage.complete(Reader.class);
   }

   static float unit() {
      return (float)Mth.clamp(Minecraft.getInstance().getWindow().getGuiScale(), (double)2.0F, (double)3.0F);
   }

   public boolean shouldRender(ElementRenderLocation location, boolean shadow) {
      return !shadow && !this.broken && !XaeroWorldMapPlaces.off() && location == ElementRenderLocation.WORLD_MAP && !MapPins.pins().isEmpty();
   }

   public int getOrder() {
      return 240;
   }

   public boolean shouldBeDimScaled() {
      return false;
   }

   public void preRender(ElementRenderInfo info, MultiBufferSource.BufferSource buffers, MultiTextureRenderTypeRendererProvider renderers, boolean shadow) {
      View view = (View)this.getContext();
      ResourceKey<Level> key = info == null ? null : info.mapDimension;
      if (key == null) {
         view.key = null;
         Minecraft mc = Minecraft.getInstance();
         view.dimension = mc.level == null ? "" : mc.level.dimension().location().toString();
      } else if (key != view.key) {
         view.key = key;
         view.dimension = key.location().toString();
      }

   }

   public void postRender(ElementRenderInfo info, MultiBufferSource.BufferSource buffers, MultiTextureRenderTypeRendererProvider renderers, boolean shadow) {
   }

   public void renderElementShadow(MapPins.Pin pin, boolean hovered, float optionalScale, double partialX, double partialY, ElementRenderInfo info, GuiGraphics g, MultiBufferSource.BufferSource buffers, MultiTextureRenderTypeRendererProvider renderers) {
   }

   public boolean renderElement(MapPins.Pin pin, boolean hovered, double optionalDepth, float optionalScale, double partialX, double partialY, ElementRenderInfo info, GuiGraphics g, MultiBufferSource.BufferSource buffers, MultiTextureRenderTypeRendererProvider renderers) {
      if (this.broken) {
         return false;
      } else {
         PoseStack pose = g.pose();
         pose.pushPose();

         try {
            float s = unit() * optionalScale;
            pose.translate((double)0.0F, (double)0.0F, optionalDepth);
            pose.scale(s, s, 1.0F);
            PinPainter.pin(g, pin, hovered, 1.0F);
            if (hovered) {
               PinPainter.label(g, pin.name(), MapPins.distanceText(pin), 1.0F, 1.0F);
            }
         } catch (Throwable t) {
            this.broken = true;
            CFHGui.LOGGER.warn("[CFHGui] drawing on Xaero's World Map failed, CFHGui's places are off there: {}", t.toString());
         } finally {
            pose.popPose();
         }

         return true;
      }
   }

   static final class View {
      String dimension = "";
      @Nullable ResourceKey<Level> key;
   }

   private static final class Provider extends ElementRenderProvider<MapPins.Pin, View> {
      private List<MapPins.Pin> pins = List.of();
      private int next;

      public void begin(ElementRenderLocation location, View view) {
         this.pins = MapPins.pins();
         this.next = 0;
      }

      public boolean hasNext(ElementRenderLocation location, View view) {
         return this.next < this.pins.size();
      }

      public MapPins.Pin getNext(ElementRenderLocation location, View view) {
         return (MapPins.Pin)this.pins.get(this.next++);
      }

      public void end(ElementRenderLocation location, View view) {
         this.pins = List.of();
      }
   }

   private static final class Reader extends ElementReader<MapPins.Pin, View, WorldMapPins> {
      public boolean isHidden(MapPins.Pin pin, View view) {
         return !pin.dimension().equals(view.dimension);
      }

      public double getRenderX(MapPins.Pin pin, View view, float partialTick) {
         return pin.x();
      }

      public double getRenderY(MapPins.Pin pin, View view, float partialTick) {
         return pin.y();
      }

      public double getRenderZ(MapPins.Pin pin, View view, float partialTick) {
         return pin.z();
      }

      public boolean hasYCoordinate() {
         return true;
      }

      public int getInteractionBoxLeft(MapPins.Pin pin, View view, float partialTick) {
         return -8;
      }

      public int getInteractionBoxRight(MapPins.Pin pin, View view, float partialTick) {
         return 8;
      }

      public int getInteractionBoxTop(MapPins.Pin pin, View view, float partialTick) {
         return -18;
      }

      public int getInteractionBoxBottom(MapPins.Pin pin, View view, float partialTick) {
         return 1;
      }

      public int getRenderBoxLeft(MapPins.Pin pin, View view, float partialTick) {
         return -96;
      }

      public int getRenderBoxRight(MapPins.Pin pin, View view, float partialTick) {
         return 96;
      }

      public int getRenderBoxTop(MapPins.Pin pin, View view, float partialTick) {
         return -20;
      }

      public int getRenderBoxBottom(MapPins.Pin pin, View view, float partialTick) {
         return 18;
      }

      public int getLeftSideLength(MapPins.Pin pin, Minecraft mc) {
         return 9 + mc.font.width(pin.name());
      }

      public String getMenuName(MapPins.Pin pin) {
         return pin.name();
      }

      public String getFilterName(MapPins.Pin pin) {
         return this.getMenuName(pin);
      }

      public int getMenuTextFillLeftPadding(MapPins.Pin pin) {
         return 0;
      }

      public int getRightClickTitleBackgroundColor(MapPins.Pin pin) {
         return -16777216 | pin.rgb();
      }

      public boolean shouldScaleBoxWithOptionalScale() {
         return true;
      }

      public float getBoxScale(ElementRenderLocation location, MapPins.Pin pin, View view) {
         return WorldMapPins.unit();
      }

      public boolean isInteractable(ElementRenderLocation location, MapPins.Pin pin) {
         return true;
      }

      public boolean isRightClickValid(MapPins.Pin pin) {
         return MapPins.byId(pin.id) == pin;
      }

      public ArrayList<RightClickOption> getRightClickOptions(MapPins.Pin pin, IRightClickableElement target) {
         try {
            ArrayList<RightClickOption> options = new ArrayList();
            options.add(XaeroOptions.title(pin.name(), 0, target));

            for(MapAction action : MapPoints.actions(pin.point())) {
               options.add(XaeroOptions.action(action, options.size(), target));
            }

            for(MapAction action : MapMenus.hideActions(pin)) {
               options.add(XaeroOptions.action(action, options.size(), target));
            }

            return options;
         } catch (Throwable var6) {
            return null;
         }
      }
   }
}
