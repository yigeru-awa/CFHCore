package com.onehumanawa.cfhcore.content.gui.veinminer.server;

import com.onehumanawa.cfhcore.content.gui.core.net.JsonWire;
import com.onehumanawa.cfhcore.content.gui.core.net.CFHGuiNetwork;
import com.onehumanawa.cfhcore.content.gui.core.perm.Perms;
import com.onehumanawa.cfhcore.content.gui.veinminer.CFHGuiVeinminer;
import com.onehumanawa.cfhcore.content.gui.veinminer.VeinShape;
import com.onehumanawa.cfhcore.content.gui.veinminer.config.VeinServerConfig;
import com.onehumanawa.cfhcore.content.gui.veinminer.net.VeinNet;
import com.onehumanawa.cfhcore.content.gui.veinminer.vein.VeinFinder;
import com.onehumanawa.cfhcore.content.gui.veinminer.vein.VeinPlanner;
import com.onehumanawa.cfhcore.content.gui.veinminer.vein.VeinRules;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public final class VeinServer {
	private static final Map<UUID, VeinServer.Mode> MODES = new HashMap<>();
	private static final Map<UUID, Boolean> SENT_ALLOWED = new HashMap<>();
	private static final List<VeinJob> JOBS = new ArrayList<>();
	private static int syncedVersion = -1;
	private static int ticks;
	private static boolean breaking;
	@Nullable
	private static MinecraftServer server;

	public static void onServerStarted(MinecraftServer s) {
		server = s;
		VeinServerConfig.reload();
		syncedVersion = VeinServerConfig.version();
	}

	public static void onServerStopping(MinecraftServer s) {
		JOBS.clear();
		MODES.clear();
		SENT_ALLOWED.clear();
		DropCollector.end();
		breaking = false;
		server = null;
	}

	public static void onJoin(ServerPlayer player) {
		sync(player);
	}

	public static void onLeave(ServerPlayer player) {
		MODES.remove(player.getUUID());
		SENT_ALLOWED.remove(player.getUUID());
		JOBS.removeIf(j -> j.player == player);
	}

	public static void onState(ServerPlayer player, VeinNet.State s) {
		Direction facing = Direction.from3DDataValue(s.facing());
		MODES.put(
			player.getUUID(),
			new VeinServer.Mode(
				s.active(), VeinShape.byOrdinal(s.shape()), s.diagonals(), s.treeDiagonals(), facing.getAxis().isHorizontal() ? facing : null, s.lookingUp()
			)
		);
	}

	public static boolean allowed(ServerPlayer player, VeinRules rules) {
		return rules.enabled && Perms.has(player, "cfhgui_veinminer.use", rules.operatorsOnly ? 2 : 0);
	}

	public static boolean active(ServerPlayer player) {
		VeinServer.Mode m = MODES.get(player.getUUID());
		return m != null && m.active();
	}

	static boolean diagonals(ServerPlayer player, VeinRules rules, boolean tree) {
		VeinServer.Mode m = MODES.get(player.getUUID());
		return m != null && rules.allowDiagonals && (m.diagonals() || tree && m.treeDiagonals());
	}

	public static void onServerTick(MinecraftServer s) {
		DropCollector.end();
		breaking = false;
		if (!JOBS.isEmpty()) {
			runJobs();
		}

		if (++ticks % 20 == 0) {
			int v = VeinServerConfig.version();
			if (v != syncedVersion) {
				syncedVersion = v;
				syncAll();
			} else if (ticks % 100 == 0) {
				for (ServerPlayer p : s.getPlayerList().getPlayers()) {
					Boolean sent = SENT_ALLOWED.get(p.getUUID());
					if (sent != null && sent != allowed(p, VeinRules.local())) {
						sync(p);
					}
				}
			}
		}
	}

	public static void sync(ServerPlayer player) {
		CFHGuiNetwork net = CFHGuiNetwork.get();
		if (net.canSendToPlayer(player, VeinNet.Rules.TYPE)) {
			boolean allowed = allowed(player, VeinRules.local());
			SENT_ALLOWED.put(player.getUUID(), allowed);
			net.sendToPlayer(player, new VeinNet.Rules(JsonWire.encode(VeinServerConfig.get().copy()), allowed));
		}
	}

	public static void syncAll() {
		MinecraftServer s = server;
		if (s != null) {
			for (ServerPlayer p : s.getPlayerList().getPlayers()) {
				sync(p);
			}
		}
	}

	public static void reload() {
		VeinServerConfig.reload();
		syncedVersion = VeinServerConfig.version();
		syncAll();
	}

	@Nullable
	public static VeinServer.Plan beforeBreak(ServerPlayer player, ServerLevel level, BlockPos pos, @Nullable Direction face) {
		if (!breaking && face != null && !VeinExtras.busy()) {
			VeinServer.Mode m = MODES.get(player.getUUID());
			if (m != null && m.active() && !player.isSpectator()) {
				VeinRules rules = VeinRules.local();
				if (allowed(player, rules) && rules.shapeEnabled(m.shape())) {
					BlockState state = level.getBlockState(pos);
					VeinFinder.Context ctx = VeinPlanner.context(
						player, face, m.facing(), m.lookingUp(), m.diagonals() && rules.allowDiagonals, m.treeDiagonals() && rules.allowDiagonals, state
					);
					VeinFinder.Result r = VeinFinder.find(
						level, pos, state, m.shape(), ctx, rules, VeinPlanner.harvest(player, rules), VeinPlanner.limit(player, rules), new VeinFinder.Result()
					);
					if (r.isEmpty()) {
						return null;
					} else {
						long[] positions = new long[r.size() - 1];
						Block[] expected = new Block[positions.length];

						for (int i = 1; i < r.size(); i++) {
							positions[i - 1] = r.get(i);
							expected[i - 1] = level.getBlockState(BlockPos.of(r.get(i))).getBlock();
						}

						if (rules.drops == VeinServerConfig.Drops.INVENTORY) {
							DropCollector.begin(level, player, pos, rules.drops);
						}

						return new VeinServer.Plan(level, pos.immutable(), positions, expected, r.truncated);
					}
				} else {
					return null;
				}
			} else {
				return null;
			}
		} else {
			return null;
		}
	}

	public static void afterBreak(ServerPlayer player, @Nullable VeinServer.Plan plan, boolean broken) {
		if (plan != null) {
			DropCollector.end();
			if (broken && plan.positions.length != 0) {
				JOBS.add(new VeinJob(player, plan.level, plan.origin, plan.positions, plan.expected, player.getInventory().selected, player.getMainHandItem().getItem()));
			}
		}
	}

	static boolean breakExtra(VeinJob job, BlockPos pos, VeinRules rules) {
		breaking = true;
		DropCollector.begin(job.level, job.player, job.origin, rules.drops);

		boolean var3;
		try {
			var3 = job.player.gameMode.destroyBlock(pos);
		} finally {
			DropCollector.end();
			breaking = false;
		}

		return var3;
	}

	private static void runJobs() {
		VeinRules rules = VeinRules.local();
		Map<UUID, Integer> used = new HashMap<>();
		Iterator<VeinJob> it = JOBS.iterator();

		while (it.hasNext()) {
			VeinJob job = it.next();
			int left = rules.blocksPerTick - used.getOrDefault(job.player.getUUID(), 0);
			if (left > 0) {
				used.merge(job.player.getUUID(), job.run(left, rules), Integer::sum);
			}

			if (job.done()) {
				it.remove();
				tell(job);
			}
		}
	}

	private static void tell(VeinJob job) {
		Component line = job.stopMessage();
		if (line != null && !job.player.hasDisconnected()) {
			job.player.displayClientMessage(line, true);
		}

		if ("true".equalsIgnoreCase(CFHGuiVeinminer.devProperty("log"))) {
			CFHGuiVeinminer.LOGGER
				.info(
					"[CFHGui Veinminer] {} veinmined {} of {} blocks from {} ({})",
					new Object[]{job.player.getGameProfile().getName(), job.broken + 1, job.total() + 1, job.origin.toShortString(), job.stop}
				);
		}
	}

	public static int veinmineNow(ServerPlayer player, BlockPos pos, Direction face, VeinShape shape) {
		VeinServer.Mode before = MODES.get(player.getUUID());
		MODES.put(player.getUUID(), new VeinServer.Mode(true, shape, false, false, null, VeinPlanner.lookingUp(player)));

		int job;
		try {
			ServerLevel level = (ServerLevel)player.level();
			VeinServer.Plan plan = beforeBreak(player, level, pos, face);
			breaking = true;

			boolean ok;
			try {
				ok = player.gameMode.destroyBlock(pos);
			} finally {
				breaking = false;
				DropCollector.end();
			}

			if (plan != null) {
				if (ok) {
					VeinJob jobx = new VeinJob(player, level, plan.origin, plan.positions, plan.expected, player.getInventory().selected, player.getMainHandItem().getItem());
					VeinRules rules = VeinRules.local();

					while (!jobx.done()) {
						jobx.run(Integer.MAX_VALUE, rules);
					}

					return 1 + jobx.broken;
				}

				return -1;
			}

			job = ok ? 1 : -1;
		} finally {
			if (before == null) {
				MODES.remove(player.getUUID());
			} else {
				MODES.put(player.getUUID(), before);
			}
		}

		return job;
	}

	private VeinServer() {
	}

	private static record Mode(boolean active, VeinShape shape, boolean diagonals, boolean treeDiagonals, @Nullable Direction facing, boolean lookingUp) {
	}

	public static final class Plan {
		final ServerLevel level;
		final BlockPos origin;
		final long[] positions;
		final Block[] expected;
		final boolean truncated;

		Plan(ServerLevel level, BlockPos origin, long[] positions, Block[] expected, boolean truncated) {
			this.level = level;
			this.origin = origin;
			this.positions = positions;
			this.expected = expected;
			this.truncated = truncated;
		}
	}
}
