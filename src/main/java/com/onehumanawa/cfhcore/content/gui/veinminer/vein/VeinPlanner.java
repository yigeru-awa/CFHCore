package com.onehumanawa.cfhcore.content.gui.veinminer.vein;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public final class VeinPlanner {
	private static volatile VeinPlanner.HarvestCheck harvestCheck = (player, state, level, pos) -> player.hasCorrectToolForDrops(state);

	public static boolean free(Player player) {
		return player.getAbilities().instabuild;
	}

	public static int limit(Player player, VeinRules rules) {
		int limit = rules.maxBlocks;
		if (free(player)) {
			return limit;
		} else {
			if (rules.stopBeforeToolBreaks) {
				ItemStack tool = player.getMainHandItem();
				if (tool.isDamageableItem()) {
					int left = tool.getMaxDamage() - tool.getDamageValue();
					limit = Math.min(limit, Math.max(1, left - 1));
				}
			}

			if (hungry(player, rules)) {
				limit = 1;
			}

			return limit;
		}
	}

	public static boolean hungry(Player player, VeinRules rules) {
		return !free(player) && rules.exhaustionPerBlock > 0.0F && player.getFoodData().getFoodLevel() <= 0;
	}

	public static boolean toolWorn(Player player, VeinRules rules) {
		if (!free(player) && rules.stopBeforeToolBreaks) {
			ItemStack tool = player.getMainHandItem();
			return tool.isDamageableItem() && tool.getMaxDamage() - tool.getDamageValue() - 1 < rules.maxBlocks;
		} else {
			return false;
		}
	}

	public static void setHarvestCheck(VeinPlanner.HarvestCheck check) {
		harvestCheck = check;
	}

	@Nullable
	public static VeinFinder.Harvest harvest(Player player, VeinRules rules) {
		if (rules.requireHarvestTool && !free(player)) {
			VeinPlanner.HarvestCheck check = harvestCheck;
			Level level = player.level();
			return (state, pos) -> check.harvests(player, state, level, pos);
		} else {
			return null;
		}
	}

	public static VeinFinder.Context context(
		Player player, Direction face, @Nullable Direction facing, @Nullable Boolean lookingUp, boolean diagonals, boolean treeDiagonals, BlockState start
	) {
		Direction f = facing != null && facing.getAxis().isHorizontal() ? facing : player.getDirection();
		boolean up = lookingUp != null ? lookingUp : lookingUp(player);
		boolean diag = diagonals || treeDiagonals && VeinRules.isLog(start);
		return new VeinFinder.Context(face, f, up, Mth.floor(player.getY()), diag);
	}

	public static boolean lookingUp(Player player) {
		return player.getXRot() < 0.0F;
	}

	private VeinPlanner() {
	}

	@FunctionalInterface
	public interface HarvestCheck {
		boolean harvests(Player var1, BlockState var2, Level var3, BlockPos var4);
	}
}
