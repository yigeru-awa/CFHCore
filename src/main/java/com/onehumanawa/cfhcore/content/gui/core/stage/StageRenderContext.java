package com.onehumanawa.cfhcore.content.gui.core.stage;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import org.joml.Vector3f;

public final class StageRenderContext {
   public final PoseStack pose = new PoseStack();
   public MultiBufferSource.BufferSource buffers;
   public Stage stage;
   public StageCamera camera;
   public StageLighting lighting;
   public StageLevel level;
   public int light = 15728880;
   public float partial;
   public float deltaMs;
   public float timeMs;
   public boolean soft;
   public long frame;
   public long nowMs;
   public boolean hasMouse;
   public float mouseNdcX;
   public float mouseNdcY;
   public final Vector3f rayOrigin = new Vector3f();
   public final Vector3f rayDir = new Vector3f();
   public final Vector3f scratch = new Vector3f();
   public final Vector3f scratch2 = new Vector3f();

   public float seconds() {
      return this.timeMs / 1000.0F;
   }
}
