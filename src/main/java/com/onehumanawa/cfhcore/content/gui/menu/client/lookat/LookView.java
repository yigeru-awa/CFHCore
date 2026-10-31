package com.onehumanawa.cfhcore.content.gui.menu.client.lookat;

import com.onehumanawa.cfhcore.content.gui.core.client.render.CFHGuiRenderEvents;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;

final class LookView {
   private static final Matrix4f VIEW = new Matrix4f();
   private static final Matrix4f PROJECTION = new Matrix4f();
   private static final Vector4f POINT = new Vector4f();
   private static final CFHGuiRenderEvents.WorldRender CAPTURE = LookView::capture;
   private static double camX;
   private static double camY;
   private static double camZ;
   private static float partialTick;
   private static boolean listening;
   private static boolean fresh;
   static float x;
   static float y;
   static float pixelsPerBlock;

   private LookView() {
   }

   static void want(boolean on) {
      if (on != listening) {
         listening = on;
         if (on) {
            CFHGuiRenderEvents.AFTER_LEVEL.register(CAPTURE);
         } else {
            CFHGuiRenderEvents.AFTER_LEVEL.unregister(CAPTURE);
            fresh = false;
         }

      }
   }

   private static void capture(CFHGuiRenderEvents.WorldRenderContext context) {
      VIEW.set(context.modelView());
      PROJECTION.set(context.projection());
      Vec3 cam = context.camera().getPosition();
      camX = cam.x;
      camY = cam.y;
      camZ = cam.z;
      partialTick = context.partialTick();
      fresh = true;
   }

   static boolean ready() {
      return fresh;
   }

   static void endFrame() {
      fresh = false;
   }

   static float partialTick() {
      return partialTick;
   }

   static boolean project(double wx, double wy, double wz, int sw, int sh) {
      POINT.set((float)(wx - camX), (float)(wy - camY), (float)(wz - camZ), 1.0F);
      VIEW.transform(POINT);
      PROJECTION.transform(POINT);
      if (POINT.w <= 0.05F) {
         return false;
      } else {
         x = (POINT.x / POINT.w * 0.5F + 0.5F) * (float)sw;
         y = (0.5F - POINT.y / POINT.w * 0.5F) * (float)sh;
         pixelsPerBlock = PROJECTION.m11() * 0.5F * (float)sh / POINT.w;
         return true;
      }
   }
}
