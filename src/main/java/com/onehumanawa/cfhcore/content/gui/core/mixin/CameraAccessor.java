package com.onehumanawa.cfhcore.content.gui.core.mixin;

import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin({Camera.class})
public interface CameraAccessor {
   @Accessor("initialized")
   void cfhgui$setInitialized(boolean var1);

   @Accessor("level")
   void cfhgui$setLevel(BlockGetter var1);

   @Accessor("entity")
   void cfhgui$setEntity(Entity var1);

   @Accessor("partialTickTime")
   void cfhgui$setPartialTickTime(float var1);

   @Invoker("setRotation")
   void cfhgui$setRotation(float var1, float var2);

   @Invoker("setPosition")
   void cfhgui$setPosition(Vec3 var1);
}
