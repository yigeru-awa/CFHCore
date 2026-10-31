package com.onehumanawa.cfhcore.content.gui.core.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.onehumanawa.cfhcore.content.gui.core.client.render.GameView;
import com.onehumanawa.cfhcore.content.gui.core.client.render.CFHGuiRenderEvents;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({GameRenderer.class})
public abstract class GameRendererMixin {
   @Inject(
      method = {"render(Lnet/minecraft/client/DeltaTracker;Z)V"},
      at = {@At(
   value = "INVOKE",
   target = "Lcom/mojang/blaze3d/pipeline/RenderTarget;bindWrite(Z)V",
   shift = Shift.AFTER
)}
   )
   private void cfhgui$afterLevel(DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo ci) {
      GameView.afterLevelRender();
   }

   @Inject(
      method = {"renderLevel(Lnet/minecraft/client/DeltaTracker;)V"},
      at = {@At(
   value = "INVOKE",
   shift = Shift.AFTER,
   target = "Lnet/minecraft/client/renderer/LevelRenderer;renderLevel(Lnet/minecraft/client/DeltaTracker;ZLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V"
)}
   )
   private void cfhgui$afterLevelDrawn(DeltaTracker deltaTracker, CallbackInfo ci) {
      if (CFHGuiRenderEvents.hasAfterLevelListeners()) {
         Camera camera = ((GameRenderer)(Object)this).getMainCamera();
         Matrix4f view = (new Matrix4f()).rotation(camera.rotation().conjugate(new Quaternionf()));
         CFHGuiRenderEvents.fireAfterLevel(new CFHGuiRenderEvents.WorldRenderContext(new PoseStack(), view, new Matrix4f(RenderSystem.getProjectionMatrix()), camera, deltaTracker.getGameTimeDeltaPartialTick(false)));
      }
   }
}
