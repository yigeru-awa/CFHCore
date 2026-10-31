package com.onehumanawa.cfhcore.content.gui.core.compat.maps.xaero;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.compat.maps.Linkage;
import com.onehumanawa.cfhcore.content.gui.core.compat.maps.MapPins;
import com.onehumanawa.cfhcore.content.gui.core.compat.maps.PinPainter;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import xaero.common.graphics.renderer.multitexture.MultiTextureRenderTypeRendererProvider;
import xaero.hud.minimap.element.render.MinimapElementReader;
import xaero.hud.minimap.element.render.MinimapElementRenderInfo;
import xaero.hud.minimap.element.render.MinimapElementRenderLocation;
import xaero.hud.minimap.element.render.MinimapElementRenderProvider;
import xaero.hud.minimap.element.render.MinimapElementRenderer;

final class MinimapPins extends MinimapElementRenderer<MapPins.Pin, MinimapPins.View> {
   private static final double TOO_NEAR = (double)4.0F;
   private static final float ON_MAP = 0.75F;
   private final boolean world;
   private boolean broken;

   MinimapPins(boolean world) {
      super(new Reader(), new Provider(), new View(world));
      this.world = world;
   }

   static boolean fits() {
      return Linkage.complete(MinimapPins.class) && Linkage.complete(Provider.class) && Linkage.complete(Reader.class);
   }

   private static float worldUnit() {
      return (float)Mth.clamp(Minecraft.getInstance().getWindow().getGuiScale(), (double)2.0F, (double)4.0F);
   }

   public boolean shouldRender(MinimapElementRenderLocation location) {
      if (!this.broken && !XaeroMinimapPlaces.off() && !MapPins.pins().isEmpty()) {
         return this.world ? location == MinimapElementRenderLocation.IN_WORLD && MapPins.inWorld() : location == MinimapElementRenderLocation.OVER_MINIMAP;
      } else {
         return false;
      }
   }

   public int getOrder() {
      return 140;
   }

   public void preRender(MinimapElementRenderInfo info, MultiBufferSource.BufferSource buffers, MultiTextureRenderTypeRendererProvider renderers) {
      View view = (View)this.getContext();
      if (!view.keyed || info.mapDimension != view.mapKey) {
         view.mapKey = info.mapDimension;
         view.mapDimension = info.mapDimension == null ? current() : info.mapDimension.location().toString();
      }

      if (!view.keyed || info.renderEntityDimension != view.entityKey) {
         view.entityKey = info.renderEntityDimension;
         view.entityDimension = info.renderEntityDimension == null ? current() : info.renderEntityDimension.location().toString();
      }

      view.keyed = true;
      buffers.endBatch();
      RenderSystem.disableDepthTest();
   }

   public void postRender(MinimapElementRenderInfo info, MultiBufferSource.BufferSource buffers, MultiTextureRenderTypeRendererProvider renderers) {
      buffers.endBatch();
      RenderSystem.enableDepthTest();
      RenderSystem.depthFunc(515);
   }

   private static String current() {
      Minecraft mc = Minecraft.getInstance();
      return mc.level == null ? "" : mc.level.dimension().location().toString();
   }

   public boolean renderElement(MapPins.Pin pin, boolean highlighted, boolean outOfBounds, double optionalDepth, float optionalScale, double partialX, double partialY, MinimapElementRenderInfo info, GuiGraphics g, MultiBufferSource.BufferSource buffers) {
      if (this.broken) {
         return false;
      } else {
         if (this.world) {
            double distance = MapPins.distance(pin);
            if (distance >= (double)0.0F && distance < (double)4.0F) {
               return false;
            }
         }

         PoseStack pose = g.pose();
         pose.pushPose();

         try {
            pose.translate((double)0.0F, (double)0.0F, optionalDepth);
            if (this.world) {
               float s = worldUnit() * optionalScale;
               pose.scale(s, s, 1.0F);
               PinPainter.pin(g, pin, highlighted, highlighted ? 1.0F : 0.8F);
               if (highlighted) {
                  PinPainter.label(g, pin.name(), MapPins.distanceText(pin), 1.0F, 1.0F);
               }
            } else {
               pose.scale(optionalScale * 0.75F, optionalScale * 0.75F, 1.0F);
               if (outOfBounds) {
                  pose.translate(0.0F, 8.5F, 0.0F);
               }

               PinPainter.pin(g, pin, false, outOfBounds ? 0.8F : 1.0F);
            }
         } catch (Throwable t) {
            this.broken = true;
            CFHGui.LOGGER.warn("[CFHGui] drawing with Xaero's Minimap failed, CFHGui's places are off {}: {}", this.world ? "in the world" : "on it", t.toString());
         } finally {
            pose.popPose();
         }

         return true;
      }
   }

   static final class View {
      final boolean world;
      String mapDimension = "";
      String entityDimension = "";
      @Nullable ResourceKey<Level> mapKey;
      @Nullable ResourceKey<Level> entityKey;
      boolean keyed;

      View(boolean world) {
         this.world = world;
      }
   }

   private static final class Provider extends MinimapElementRenderProvider<MapPins.Pin, View> {
      private List<MapPins.Pin> pins = List.of();
      private int next;

      public void begin(MinimapElementRenderLocation location, View view) {
         this.pins = MapPins.pins();
         this.next = 0;
      }

      public boolean hasNext(MinimapElementRenderLocation location, View view) {
         return this.next < this.pins.size();
      }

      public MapPins.Pin getNext(MinimapElementRenderLocation location, View view) {
         return (MapPins.Pin)this.pins.get(this.next++);
      }

      public void end(MinimapElementRenderLocation location, View view) {
         this.pins = List.of();
      }
   }

   private static final class Reader extends MinimapElementReader<MapPins.Pin, View> {
      public boolean isHidden(MapPins.Pin pin, View view) {
         if (pin.dimension().equals(view.entityDimension)) {
            return false;
         } else {
            return view.world || !pin.dimension().equals(view.mapDimension);
         }
      }

      public double getRenderX(MapPins.Pin pin, View view, float partialTick) {
         return pin.x();
      }

      public double getRenderY(MapPins.Pin pin, View view, float partialTick) {
         return view.world ? pin.y() + 1.6 : pin.y();
      }

      public double getRenderZ(MapPins.Pin pin, View view, float partialTick) {
         return pin.z();
      }

      public double getCoordinateScale(MapPins.Pin pin, View view, MinimapElementRenderInfo info) {
         return pin.dimension().equals(view.entityDimension) ? info.renderEntityDimensionScale : info.backgroundCoordinateScale;
      }

      public int getInteractionBoxLeft(MapPins.Pin pin, View view, float partialTick) {
         return -8 - (view.world ? 8 : 0);
      }

      public int getInteractionBoxRight(MapPins.Pin pin, View view, float partialTick) {
         return 8 + (view.world ? 8 : 0);
      }

      public int getInteractionBoxTop(MapPins.Pin pin, View view, float partialTick) {
         return -18 - (view.world ? 8 : 0);
      }

      public int getInteractionBoxBottom(MapPins.Pin pin, View view, float partialTick) {
         return 1 + (view.world ? 12 : 0);
      }

      public int getRenderBoxLeft(MapPins.Pin pin, View view, float partialTick) {
         return view.world ? -96 : -16;
      }

      public int getRenderBoxRight(MapPins.Pin pin, View view, float partialTick) {
         return view.world ? 96 : 16;
      }

      public int getRenderBoxTop(MapPins.Pin pin, View view, float partialTick) {
         return -20;
      }

      public int getRenderBoxBottom(MapPins.Pin pin, View view, float partialTick) {
         return view.world ? 18 : 4;
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

      public boolean isInteractable(MinimapElementRenderLocation location, MapPins.Pin pin) {
         return location == MinimapElementRenderLocation.IN_WORLD;
      }

      public float getBoxScale(MinimapElementRenderLocation location, MapPins.Pin pin, View view) {
         return view.world ? MinimapPins.worldUnit() : 1.0F;
      }
   }
}
