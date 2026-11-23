package com.onehumanawa.cfhcore.content.gui.veinminer.server;

import com.onehumanawa.cfhcore.content.gui.veinminer.config.VeinServerConfig;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class DropCollector {
	@Nullable
	private static DropCollector.Capture active;

	public static void begin(ServerLevel level, ServerPlayer player, BlockPos origin, VeinServerConfig.Drops mode) {
		active = mode == VeinServerConfig.Drops.IN_PLACE ? null : new DropCollector.Capture(level, player, Vec3.atCenterOf(origin), mode, null);
	}

	static void collect(ServerLevel level, List<ItemStack> sink) {
		active = new DropCollector.Capture(level, null, Vec3.ZERO, VeinServerConfig.Drops.IN_PLACE, sink);
	}

	public static void end() {
		active = null;
	}

	public static boolean intercept(ServerLevel level, Entity entity) {
		DropCollector.Capture c = active;
		if (c == null || c.level != level) {
			return false;
		} else if (c.sink != null) {
			if (entity instanceof ItemEntity item) {
				c.sink.add(item.getItem().copy());
				return true;
			} else {
				return false;
			}
		} else if (entity instanceof ItemEntity item) {
			if (c.mode == VeinServerConfig.Drops.INVENTORY && c.player != null && !c.player.isRemoved()) {
				ItemStack stack = item.getItem().copy();
				active = null;

				try {
					c.player.getInventory().add(stack);
				} finally {
					active = c;
				}

				if (stack.isEmpty()) {
					return true;
				} else {
					item.setItem(stack);
					item.setPos(c.player.getX(), c.player.getY(), c.player.getZ());
					item.setNoPickUpDelay();
					return false;
				}
			} else {
				item.setPos(c.at.x, c.at.y, c.at.z);
				return false;
			}
		} else {
			if (entity instanceof ExperienceOrb orb) {
				Vec3 to = c.mode == VeinServerConfig.Drops.INVENTORY && c.player != null ? c.player.position() : c.at;
				orb.setPos(to.x, to.y, to.z);
			}

			return false;
		}
	}

	private DropCollector() {
	}

	private static record Capture(ServerLevel level, @Nullable ServerPlayer player, Vec3 at, VeinServerConfig.Drops mode, @Nullable List<ItemStack> sink) {
	}
}
