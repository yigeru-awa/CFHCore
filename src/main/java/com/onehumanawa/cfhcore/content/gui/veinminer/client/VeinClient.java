package com.onehumanawa.cfhcore.content.gui.veinminer.client;

import com.onehumanawa.cfhcore.content.gui.core.event.CFHGuiEvents;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Clock;
import com.onehumanawa.cfhcore.content.gui.core.net.CFHGuiNetwork;
import com.onehumanawa.cfhcore.content.gui.veinminer.CFHGuiVeinminer;
import com.onehumanawa.cfhcore.content.gui.veinminer.VeinShape;
import com.onehumanawa.cfhcore.content.gui.veinminer.config.VeinClientConfig;
import com.onehumanawa.cfhcore.content.gui.veinminer.net.VeinNet;
import com.onehumanawa.cfhcore.content.gui.veinminer.vein.VeinPlanner;
import com.onehumanawa.cfhcore.content.gui.veinminer.vein.VeinRules;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.HitResult.Type;
import org.jetbrains.annotations.Nullable;

public final class VeinClient {
	private static boolean toggled;
	private static boolean wanted;
	private static boolean active;
	@Nullable
	private static VeinNet.State lastSent;
	private static double scroll;
	private static long shapeChangedMs;
	private static int saveIn = -1;
	private static boolean initialised;

	public static synchronized void init() {
		if (!initialised) {
			initialised = true;
			VeinKeys.init();
			VeinNet.onClientRules(ClientRules::accept);
			CFHGuiEvents.CLIENT_TICK_END.register(VeinClient::tick);
			CFHGuiEvents.CLIENT_LEFT_SERVER.register(VeinClient::onLeave);
			CFHGuiEvents.HUD_RENDER.register(VeinHud::render);
			OutlineRenderer.init();
			String shape = CFHGuiVeinminer.devProperty("forceShape");
			if (!shape.isEmpty()) {
				VeinClientConfig.get().shape = VeinShape.parse(shape, VeinShape.SHAPELESS).id();
			}

			String style = CFHGuiVeinminer.devProperty("forceStyle");
			if (!style.isEmpty()) {
				VeinClientConfig.get().previewStyle = style;
			}

			VeinClientConfig.get().sanitize();
		}
	}

	static boolean forced() {
		return "true".equalsIgnoreCase(CFHGuiVeinminer.devProperty("forceActive"));
	}

	public static boolean active() {
		return active;
	}

	public static boolean wanted() {
		return wanted;
	}

	public static VeinShape shape() {
		VeinShape s = VeinClientConfig.get().shape();
		VeinRules rules = ClientRules.get();
		return rules == null ? s : rules.firstEnabledFrom(s);
	}

	static long shapeChangedMs() {
		return shapeChangedMs;
	}

	public static boolean replacesBlockOutline(@Nullable HitResult hit) {
		if (active && hit instanceof BlockHitResult block && hit.getType() == Type.BLOCK && OutlineRenderer.drawsOutlineOf(block.getBlockPos())) {
			return true;
		}

		return false;
	}

	private static void tick() {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		VeinClientConfig cfg = VeinClientConfig.get();
		boolean pressed = false;

		while (VeinKeys.ACTIVATE.consumeClick()) {
			pressed = true;
		}

		if (pressed && cfg.activation() == VeinClientConfig.Activation.TOGGLE) {
			toggled = !toggled;
		}

		while (VeinKeys.NEXT_SHAPE.consumeClick()) {
			cycle(1);
		}

		while (VeinKeys.PREVIOUS_SHAPE.consumeClick()) {
			cycle(-1);
		}

		if (saveIn >= 0 && --saveIn < 0) {
			VeinClientConfig.file().save();
		}

		if (player != null && mc.level != null) {
			boolean held = cfg.activation() == VeinClientConfig.Activation.HOLD ? VeinKeys.ACTIVATE.isDown() : toggled;
			wanted = (held || forced()) && !player.isSpectator();
			active = wanted && ClientRules.serverHasMod() && ClientRules.allowed();
			send(player);
			VeinPreview.tick(mc, active);
		} else {
			wanted = false;
			active = false;
			VeinPreview.clear();
		}
	}

	private static void send(LocalPlayer player) {
		if (!ClientRules.serverHasMod()) {
			lastSent = null;
		} else {
			VeinClientConfig cfg = VeinClientConfig.get();
			VeinNet.State now = active
				? new VeinNet.State(true, shape().ordinal(), cfg.diagonals, cfg.treeDiagonals, player.getDirection().get3DDataValue(), VeinPlanner.lookingUp(player))
				: new VeinNet.State(false, 0, false, false, 0, false);
			if (!Objects.equals(now, lastSent)) {
				lastSent = now;
				CFHGuiNetwork.get().sendToServer(now);
			}
		}
	}

	private static void onLeave() {
		toggled = false;
		wanted = false;
		active = false;
		lastSent = null;
		ClientRules.clear();
		VeinPreview.clear();
	}

	public static boolean onScroll(double yOffset) {
		if (active && VeinClientConfig.get().scrollCycles) {
			Minecraft mc = Minecraft.getInstance();
			boolean discrete = (Boolean)mc.options.discreteMouseScroll().get();
			double sensitivity = (Double)mc.options.mouseWheelSensitivity().get();
			scroll = scroll + (discrete ? Math.signum(yOffset) : yOffset) * sensitivity;
			int steps = (int)scroll;
			if (steps != 0) {
				scroll -= (double)steps;
				cycle(-steps);
			}

			return true;
		} else {
			return false;
		}
	}

	static void cycle(int steps) {
		if (steps != 0) {
			VeinRules rules = ClientRules.get();
			int n = VeinShape.count();
			int dir = steps > 0 ? 1 : -1;
			VeinShape s = shape();

			for (int i = 0; i < Math.abs(steps); i++) {
				for (int tries = 0; tries < n; tries++) {
					s = VeinShape.byOrdinal(Math.floorMod(s.ordinal() + dir, n));
					if (rules == null || rules.shapeEnabled(s)) {
						break;
					}
				}
			}

			setShape(s);
		}
	}

	public static void setShape(VeinShape shape) {
		VeinClientConfig cfg = VeinClientConfig.get();
		if (!shape.id().equals(cfg.shape)) {
			cfg.shape = shape.id();
			saveIn = 40;
		}

		shapeChangedMs = Clock.nowMs();
	}

	private VeinClient() {
	}
}
