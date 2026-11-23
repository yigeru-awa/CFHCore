package com.onehumanawa.cfhcore.content.gui.veinminer.client;

import com.onehumanawa.cfhcore.content.gui.veinminer.VeinShape;
import com.onehumanawa.cfhcore.content.gui.veinminer.config.VeinClientConfig;
import com.onehumanawa.cfhcore.content.gui.veinminer.vein.VeinFinder;
import com.onehumanawa.cfhcore.content.gui.veinminer.vein.VeinPlanner;
import com.onehumanawa.cfhcore.content.gui.veinminer.vein.VeinRules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.HitResult.Type;
import org.jetbrains.annotations.Nullable;

public final class VeinPreview {
	private static final int REFRESH_TICKS = 10;
	private static VeinFinder.Result shown = new VeinFinder.Result();
	private static VeinFinder.Result spare = new VeinFinder.Result();
	private static boolean target;
	@Nullable
	private static BlockPos origin;
	private static int positionsVersion;
	private static boolean toolLimited;
	private static boolean hungry;
	private static long keyPos = Long.MIN_VALUE;
	@Nullable
	private static Direction keyFace;
	@Nullable
	private static BlockState keyState;
	@Nullable
	private static VeinShape keyShape;
	private static int keyRules = Integer.MIN_VALUE;
	private static int keyClient = Integer.MIN_VALUE;
	@Nullable
	private static Item keyItem;
	private static int keyDamage;
	@Nullable
	private static Direction keyFacing;
	private static boolean keyUp;
	private static int keyFeet;
	private static boolean keyHungry;
	private static int age;

	static void tick(Minecraft mc, boolean active) {
		LocalPlayer player = mc.player;
		ClientLevel level = mc.level;
		VeinRules rules = ClientRules.get();
		if (active && player != null && level != null && rules != null) {
			HitResult hit = mc.hitResult;
			if (hit instanceof BlockHitResult bhr && hit.getType() == Type.BLOCK) {
				BlockPos pos = bhr.getBlockPos();
				BlockState state = level.getBlockState(pos);
				VeinShape shape = VeinClient.shape();
				ItemStack held = player.getMainHandItem();
				Direction facing = player.getDirection();
				boolean up = VeinPlanner.lookingUp(player);
				int feet = Mth.floor(player.getY());
				boolean starving = VeinPlanner.hungry(player, rules);
				int rulesVersion = ClientRules.version();
				int clientVersion = VeinClientConfig.version();
				boolean same = target
					&& keyPos == pos.asLong()
					&& keyFace == bhr.getDirection()
					&& keyState == state
					&& keyShape == shape
					&& keyRules == rulesVersion
					&& keyClient == clientVersion
					&& keyItem == held.getItem()
					&& keyDamage == held.getDamageValue()
					&& keyFacing == facing
					&& keyUp == up
					&& keyFeet == feet
					&& keyHungry == starving;
				if (same && ++age < 10) {
					return;
				}

				age = 0;
				keyPos = pos.asLong();
				keyFace = bhr.getDirection();
				keyState = state;
				keyShape = shape;
				keyRules = rulesVersion;
				keyClient = clientVersion;
				keyItem = held.getItem();
				keyDamage = held.getDamageValue();
				keyFacing = facing;
				keyUp = up;
				keyFeet = feet;
				keyHungry = starving;
				VeinClientConfig cfg = VeinClientConfig.get();
				VeinFinder.Context ctx = VeinPlanner.context(
					player, bhr.getDirection(), facing, up, cfg.diagonals && rules.allowDiagonals, cfg.treeDiagonals && rules.allowDiagonals, state
				);
				VeinFinder.find(level, pos, state, shape, ctx, rules, VeinPlanner.harvest(player, rules), VeinPlanner.limit(player, rules), spare);
				toolLimited = VeinPlanner.toolWorn(player, rules);
				hungry = starving;
				target = true;
				if (!spare.positions.equals(shown.positions) || origin == null || origin.asLong() != pos.asLong()) {
					positionsVersion++;
					origin = pos.immutable();
				}

				VeinFinder.Result t = shown;
				shown = spare;
				spare = t;
				return;
			}

			clear();
		} else {
			clear();
		}
	}

	static void clear() {
		if (target || !shown.isEmpty()) {
			target = false;
			shown.clear();
			spare.clear();
			origin = null;
			keyPos = Long.MIN_VALUE;
			positionsVersion++;
		}
	}

	public static boolean hasTarget() {
		return target;
	}

	public static VeinFinder.Result result() {
		return shown;
	}

	public static VeinFinder.Reason reason() {
		return shown.reason;
	}

	public static int count() {
		return shown.size();
	}

	public static boolean truncated() {
		return shown.truncated;
	}

	public static boolean toolLimited() {
		return toolLimited;
	}

	public static boolean hungry() {
		return hungry;
	}

	@Nullable
	public static BlockPos origin() {
		return origin;
	}

	public static int positionsVersion() {
		return positionsVersion;
	}

	private VeinPreview() {
	}
}
