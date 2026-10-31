package com.onehumanawa.cfhcore.content.gui.core.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.onehumanawa.cfhcore.content.gui.core.event.Event;
import net.minecraft.client.Camera;
import org.joml.Matrix4f;

public final class CFHGuiRenderEvents {
   public static final Event<WorldRender> AFTER_TRANSLUCENT = new Event<WorldRender>("after_translucent");
   public static final Event<WorldRender> AFTER_LEVEL = new Event<WorldRender>("after_level");

   public static void fireAfterLevel(WorldRenderContext context) {
      AFTER_LEVEL.invoke((l) -> l.render(context));
   }

   public static boolean hasAfterLevelListeners() {
      return !AFTER_LEVEL.listeners().isEmpty();
   }

   public static void fireAfterTranslucent(WorldRenderContext context) {
      AFTER_TRANSLUCENT.invoke((l) -> l.render(context));
   }

   public static boolean hasAfterTranslucentListeners() {
      return !AFTER_TRANSLUCENT.listeners().isEmpty();
   }

   private CFHGuiRenderEvents() {
   }

   public static record WorldRenderContext(PoseStack poseStack, Matrix4f modelView, Matrix4f projection, Camera camera, float partialTick) {
   }

   @FunctionalInterface
   public interface WorldRender {
      void render(WorldRenderContext var1);
   }
}
