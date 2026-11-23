package com.onehumanawa.cfhcore.content.gui.teams.mixin.server;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.onehumanawa.cfhcore.content.gui.teams.server.protect.LandGuard;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Explosion.class})
public abstract class ExplosionMixin {
	@Shadow
	@Final
	private Level level;
	@Shadow
	@Final
	private ObjectArrayList<BlockPos> toBlow;

	@ModifyExpressionValue(
		method = {"explode"},
		require = 0,
		at = {@At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/level/Level;getEntities(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;"
		)}
	)
	private List<Entity> slateTeams$spareClaimedLand(List<Entity> entities) {
		LandGuard.filterExplosion(this.level, this.toBlow, entities);
		return entities;
	}

	@Inject(
		method = {"explode"},
		at = {@At("TAIL")}
	)
	private void slateTeams$spareClaimedBlocks(CallbackInfo ci) {
		LandGuard.filterExplosion(this.level, this.toBlow, null);
	}
}
