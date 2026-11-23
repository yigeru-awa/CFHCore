package com.onehumanawa.cfhcore.content.gui.veinminer.server;

import com.onehumanawa.cfhcore.content.gui.veinminer.CFHGuiVeinminer;
import com.onehumanawa.cfhcore.content.gui.veinminer.VeinShape;
import com.onehumanawa.cfhcore.content.gui.veinminer.config.VeinServerConfig;
import com.onehumanawa.cfhcore.content.gui.veinminer.vein.VeinFinder;
import com.onehumanawa.cfhcore.content.gui.veinminer.vein.VeinPlanner;
import com.onehumanawa.cfhcore.content.gui.veinminer.vein.VeinRules;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

final class SelfTest {
	private static final int FLAGS = 18;
	private static final int HALF = 16;
	private final ServerLevel level;
	private final BlockPos o;
	private final List<BlockPos> placed = new ArrayList<>();
	private final List<String> failures = new ArrayList<>();
	private int passed;

	private SelfTest(ServerLevel level, BlockPos origin) {
		this.level = level;
		this.o = origin;
	}

	static SelfTest.Outcome run(CommandSourceStack source) {
		ServerLevel level = source.getLevel();
		BlockPos at = BlockPos.containing(source.getPosition());
		BlockPos origin = area(level, at);
		if (origin == null) {
			source.sendFailure(
				Component.translatableWithFallback(
					"cfhgui_veinminer.selftest.no_room", "CFHGui Veinminer self-test: no empty room above you (it needs 32 blocks of air on every side, high up)."
				)
			);
			return new SelfTest.Outcome(0, 1);
		} else {
			SelfTest t = new SelfTest(level, origin);

			try {
				t.finderTests();
				t.extrasTests();
				ServerPlayer player = source.getPlayer();
				if (player != null) {
					t.breakTest(player);
					t.harvestTest(player);
				}
			} catch (RuntimeException var11) {
				t.failures.add("threw " + var11);
				CFHGuiVeinminer.LOGGER.error("[CFHGui Veinminer] selftest threw", var11);
			} finally {
				t.clear();
			}

			int failed = t.failures.size();

			for (String f : t.failures) {
				CFHGuiVeinminer.LOGGER.warn("[CFHGui Veinminer] selftest FAIL {}", f);
			}

			CFHGuiVeinminer.LOGGER.info("[CFHGui Veinminer] selftest: {} passed, {} failed", t.passed, failed);
			int p = t.passed;
			source.sendSuccess(
				() -> Component.literal("CFHGui Veinminer selftest: " + p + " passed, " + failed + " failed")
						.withStyle(failed == 0 ? ChatFormatting.GREEN : ChatFormatting.RED),
				false
			);

			for (String f : t.failures) {
				source.sendSuccess(() -> Component.literal("  FAIL " + f).withStyle(ChatFormatting.RED), false);
			}

			return new SelfTest.Outcome(p, failed);
		}
	}

	@Nullable
	private static BlockPos area(ServerLevel level, BlockPos at) {
		int top = level.getMaxBuildHeight() - 16 - 2;

		for (int y : new int[]{Math.min(top, at.getY() + 48), top}) {
			BlockPos c = new BlockPos(at.getX(), y, at.getZ());
			if (allAir(level, c)) {
				return c;
			}
		}

		return null;
	}

	private static boolean allAir(ServerLevel level, BlockPos c) {
		MutableBlockPos p = new MutableBlockPos();

		for (int x = -16; x < 16; x++) {
			for (int y = -16; y < 16; y++) {
				for (int z = -16; z < 16; z++) {
					if (!level.getBlockState(p.set(c.getX() + x, c.getY() + y, c.getZ() + z)).isAir()) {
						return false;
					}
				}
			}
		}

		return true;
	}

	private void finderTests() {
		VeinRules rules = rules(c -> {
		});
		this.put(0, 0, 0, Blocks.IRON_ORE);
		this.put(1, 0, 0, Blocks.IRON_ORE);
		this.put(2, 0, 0, Blocks.IRON_ORE);
		this.put(3, 0, 0, Blocks.DEEPSLATE_IRON_ORE);
		this.put(0, 1, 0, Blocks.COAL_ORE);
		this.put(1, 1, 1, Blocks.IRON_ORE);
		this.expect(
			"shapeless: the deepslate ore joins the vein, the coal does not", this.find(0, 0, 0, VeinShape.SHAPELESS, Direction.UP, false, false, rules, 64), 4, false
		);
		this.expect("shapeless: diagonals reach the ore edge to edge", this.find(0, 0, 0, VeinShape.SHAPELESS, Direction.UP, false, true, rules, 64), 5, false);
		this.expect("shapeless: from the deepslate ore the stone ones join", this.find(3, 0, 0, VeinShape.SHAPELESS, Direction.UP, false, false, rules, 64), 4, false);
		VeinRules apart = rules(c -> {
			c.oreTags.clear();
			c.deepslateOres = false;
		});
		this.expect(
			"shapeless: without ore tags and the deepslate rule the deepslate ore stays apart",
			this.find(0, 0, 0, VeinShape.SHAPELESS, Direction.UP, false, false, apart, 64),
			3,
			false
		);
		VeinRules namesOnly = rules(c -> c.oreTags.clear());
		this.expect("shapeless: the deepslate rule alone joins them", this.find(0, 0, 0, VeinShape.SHAPELESS, Direction.UP, false, false, namesOnly, 64), 4, false);
		VeinFinder.Result denied = this.find(0, 0, 0, VeinShape.SHAPELESS, Direction.UP, false, false, rules(c -> c.deny.add("minecraft:iron_ore")), 64);
		this.expectReason("deny list: a denied block starts nothing", denied, VeinFinder.Reason.DENIED);
		VeinFinder.Result deniedTag = this.find(0, 0, 0, VeinShape.SHAPELESS, Direction.UP, false, false, rules(c -> c.deny.add("#minecraft:iron_ores")), 64);
		this.expectReason("deny list: a denied tag starts nothing", deniedTag, VeinFinder.Reason.DENIED);
		this.expectReason(
			"allow list: a block off the list starts nothing",
			this.find(0, 0, 0, VeinShape.SHAPELESS, Direction.UP, false, false, rules(c -> c.allow.add("#minecraft:logs")), 64),
			VeinFinder.Reason.DENIED
		);
		VeinFinder.Harvest bareHand = (s, p) -> !s.requiresCorrectToolForDrops();
		this.expectReason(
			"tool: ore without a pickaxe starts nothing",
			VeinFinder.find(
				this.level,
				this.at(0, 0, 0),
				this.level.getBlockState(this.at(0, 0, 0)),
				VeinShape.SHAPELESS,
				this.ctx(Direction.UP, false, false),
				rules,
				bareHand,
				64,
				new VeinFinder.Result()
			),
			VeinFinder.Reason.NEEDS_TOOL
		);
		this.clear();
		this.box(-2, -2, -2, 2, 2, 2, Blocks.STONE);
		VeinFinder.Result cut = this.find(0, 0, 0, VeinShape.SHAPELESS, Direction.UP, false, false, rules, 20);
		this.expect("limit: 20 of the cube's 125", cut, 20, true);
		int far = 0;

		for (int i = 0; i < cut.size(); i++) {
			BlockPos p = BlockPos.of(cut.get(i));
			far = Math.max(far, Math.abs(p.getX() - this.o.getX()) + Math.abs(p.getY() - this.o.getY()) + Math.abs(p.getZ() - this.o.getZ()));
		}

		this.check("limit: the nearest blocks are taken first (all within 2)", far <= 2, "farthest " + far);
		this.expect("limit: all 125 under a limit of 200", this.find(0, 0, 0, VeinShape.SHAPELESS, Direction.UP, false, false, rules, 200), 125, false);
		this.clear();

		for (int y = 0; y < 4; y++) {
			this.put(0, y, 0, Blocks.OAK_LOG);
		}

		this.put(1, 3, 0, Blocks.OAK_WOOD);
		this.put(2, 3, 0, Blocks.STRIPPED_OAK_LOG);
		this.put(-1, 0, 0, Blocks.BIRCH_LOG);
		this.place(this.at(0, 4, 0), (BlockState)Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true));
		this.put(3, 4, 0, Blocks.OAK_LOG);
		this.expect(
			"logs: the trunk, its wood and the stripped log, not the birch nor the leaves",
			this.find(0, 0, 0, VeinShape.SHAPELESS, Direction.UP, false, false, rules, 64),
			6,
			false
		);
		this.expect("logs: with diagonals the crooked branch joins", this.find(0, 0, 0, VeinShape.SHAPELESS, Direction.UP, false, true, rules, 64), 7, false);
		this.expect(
			"logs: without the wood rule the oak log stays alone with its own kind",
			this.find(0, 0, 0, VeinShape.SHAPELESS, Direction.UP, false, false, rules(c -> c.logsByWood = false), 64),
			4,
			false
		);
		this.expect(
			"diagonals: off on the server, none", this.find(0, 0, 0, VeinShape.SHAPELESS, Direction.UP, false, true, rules(c -> c.allowDiagonals = false), 64), 6, false
		);
		this.clear();
		this.put(0, 0, 0, Blocks.FERN);
		this.put(1, 0, 0, Blocks.SHORT_GRASS);
		this.put(2, 0, 0, Blocks.POPPY);
		this.put(3, 0, 0, Blocks.WHEAT);
		this.put(-1, 0, 0, Blocks.OAK_SAPLING);
		this.expect(
			"plants: the fern takes the grass and the poppy, not the wheat nor the sapling",
			this.find(0, 0, 0, VeinShape.SHAPELESS, Direction.UP, false, false, rules, 64),
			3,
			false
		);
		this.expect(
			"plants: without the rule the fern stays alone",
			this.find(0, 0, 0, VeinShape.SHAPELESS, Direction.UP, false, false, rules(c -> c.plantsAsOne = false), 64),
			1,
			false
		);
		this.clear();
		this.put(0, 0, 0, Blocks.CHEST);
		this.put(1, 0, 0, Blocks.CHEST);
		this.expectReason(
			"block entities: off by default", this.find(0, 0, 0, VeinShape.SHAPELESS, Direction.UP, false, false, rules, 64), VeinFinder.Reason.BLOCK_ENTITY
		);
		this.expect(
			"block entities: on when allowed",
			this.find(0, 0, 0, VeinShape.SHAPELESS, Direction.UP, false, false, rules(c -> c.allowBlockEntities = true), 64),
			2,
			false
		);
		this.put(0, 2, 0, Blocks.BEDROCK);
		this.put(1, 2, 0, Blocks.BEDROCK);
		this.expectReason("bedrock: never", this.find(0, 2, 0, VeinShape.SHAPELESS, Direction.UP, false, false, rules, 64), VeinFinder.Reason.UNBREAKABLE);
		this.expectReason(
			"a shape the server switched off: nothing",
			this.find(0, 2, 0, VeinShape.COLUMN, Direction.UP, false, false, rules(c -> c.shapes.column = false), 64),
			VeinFinder.Reason.SHAPE_OFF
		);
		this.clear();
		Runnable wall = () -> this.box(-1, -2, -7, 1, 1, 0, Blocks.STONE);
		wall.run();
		this.expect("tunnel 1x2: two a layer, eight layers", this.tunnel(VeinShape.TUNNEL_1X2, rules, 64, -1), 16, false);
		this.expect("tunnel 1x2: cut short", this.tunnel(VeinShape.TUNNEL_1X2, rules, 10, -1), 10, true);
		VeinFinder.Result low = this.tunnel(VeinShape.TUNNEL_1X2, rules, 64, 0);
		this.expect("tunnel 1x2: mined at the feet, the second block is above", low, 16, false);
		this.check("tunnel 1x2: above, not below", low.positions.contains(this.at(0, 1, 0).asLong()) && !low.positions.contains(this.at(0, -1, 0).asLong()), "");
		this.expect("tunnel 3x3: nine a layer, cut at 64", this.tunnel(VeinShape.TUNNEL_3X3, rules, 64, -1), 64, true);
		this.expect("tunnel 3x3: all 72 under a limit of 100", this.tunnel(VeinShape.TUNNEL_3X3, rules, 100, -1), 72, false);
		this.expect("square 3x3: one layer", this.tunnel(VeinShape.SQUARE_3X3, rules, 64, -1), 9, false);
		this.place(this.at(0, -1, -1), Blocks.TORCH.defaultBlockState());
		this.expect("tunnel 1x2: a torch in the way stays", this.tunnel(VeinShape.TUNNEL_1X2, rules, 64, -1), 15, false);

		for (int x = -1; x <= 1; x++) {
			for (int y = -2; y <= 1; y++) {
				this.place(this.at(x, y, -4), Blocks.AIR.defaultBlockState());
			}
		}

		this.expect("tunnel 1x2: stops where it breaks through", this.tunnel(VeinShape.TUNNEL_1X2, rules, 64, -1), 7, false);
		this.clear();

		for (int y = 0; y > -6; y--) {
			this.put(0, y, 0, Blocks.STONE);
		}

		this.expect("column: down from the top face until air", this.find(0, 0, 0, VeinShape.COLUMN, Direction.UP, false, false, rules, 64), 6, false);
		this.expect("column: up from the bottom face", this.find(0, -5, 0, VeinShape.COLUMN, Direction.DOWN, false, false, rules, 64), 6, false);
		this.expect("column: a side face, looking down", this.find(0, -2, 0, VeinShape.COLUMN, Direction.SOUTH, false, false, rules, 64), 4, false);
		this.expect("column: a side face, looking up", this.find(0, -2, 0, VeinShape.COLUMN, Direction.SOUTH, true, false, rules, 64), 3, false);
		this.clear();
		this.box(0, -12, -10, 0, 14, 0, Blocks.STONE);
		VeinFinder.Result down = VeinFinder.find(
			this.level,
			this.at(0, 2, 0),
			this.level.getBlockState(this.at(0, 2, 0)),
			VeinShape.STAIRCASE,
			new VeinFinder.Context(Direction.SOUTH, Direction.NORTH, false, this.o.getY() + 1, false),
			rules,
			null,
			64,
			new VeinFinder.Result()
		);
		this.expect("staircase down: 11 steps of 3", down, 33, false);
		this.check(
			"staircase down: the first step is the mined block and the two below",
			down.positions.contains(this.at(0, 1, 0).asLong())
				&& down.positions.contains(this.at(0, 0, 0).asLong())
				&& !down.positions.contains(this.at(0, 3, 0).asLong()),
			""
		);
		this.check(
			"staircase down: the second step is one forward and one down",
			down.positions.contains(this.at(0, 1, -1).asLong())
				&& down.positions.contains(this.at(0, -1, -1).asLong())
				&& !down.positions.contains(this.at(0, 2, -1).asLong()),
			""
		);
		VeinFinder.Result up = VeinFinder.find(
			this.level,
			this.at(0, 2, 0),
			this.level.getBlockState(this.at(0, 2, 0)),
			VeinShape.STAIRCASE,
			new VeinFinder.Context(Direction.SOUTH, Direction.NORTH, true, this.o.getY() + 1, false),
			rules,
			null,
			64,
			new VeinFinder.Result()
		);
		this.expect("staircase up: 11 steps of 3", up, 33, false);
		this.check(
			"staircase up: the second step is one forward and one up",
			up.positions.contains(this.at(0, 3, -1).asLong()) && up.positions.contains(this.at(0, 5, -1).asLong()) && !up.positions.contains(this.at(0, 2, -1).asLong()),
			""
		);
		this.expect(
			"staircase: cut short",
			VeinFinder.find(
				this.level,
				this.at(0, 2, 0),
				this.level.getBlockState(this.at(0, 2, 0)),
				VeinShape.STAIRCASE,
				new VeinFinder.Context(Direction.SOUTH, Direction.NORTH, false, this.o.getY() + 1, false),
				rules,
				null,
				20,
				new VeinFinder.Result()
			),
			20,
			true
		);
		this.clear();
	}

	private void extrasTests() {
		VeinRules rules = rules(c -> {
		});
		this.field();
		this.put(2, -1, 0, Blocks.FARMLAND);
		this.place(this.at(2, 0, 0), (BlockState)Blocks.CARROTS.defaultBlockState().setValue(CropBlock.AGE, 7));
		CropBlock wheat = (CropBlock)Blocks.WHEAT;
		this.check(
			"crops: the ripe wheat of the field, not the young one nor the carrots",
			VeinExtras.ripe(this.level, this.at(0, 0, 0), wheat, 64, false).size() == 8,
			"got " + VeinExtras.ripe(this.level, this.at(0, 0, 0), wheat, 64, false).size()
		);
		this.check("crops: cut short by the limit", VeinExtras.ripe(this.level, this.at(0, 0, 0), wheat, 5, false).size() == 5, "");
		this.clear();
		this.box(-1, -1, -1, 1, -1, 1, Blocks.GRASS_BLOCK);
		this.put(2, -1, 1, Blocks.DIRT);
		this.put(0, -2, 0, Blocks.GRASS_BLOCK);
		this.check(
			"ground: the grass around on its level, not the dirt nor the grass below",
			VeinExtras.ground(this.level, this.at(0, -1, 0), this.level.getBlockState(this.at(0, -1, 0)), 64, false, rules).size() == 9,
			"got " + VeinExtras.ground(this.level, this.at(0, -1, 0), this.level.getBlockState(this.at(0, -1, 0)), 64, false, rules).size()
		);
		this.check(
			"tools: axes, hoes and shovels act on groups, a pickaxe does not",
			VeinExtras.isTool(Items.IRON_AXE) && VeinExtras.isTool(Items.IRON_HOE) && VeinExtras.isTool(Items.IRON_SHOVEL) && !VeinExtras.isTool(Items.IRON_PICKAXE),
			""
		);
		this.clear();
	}

	private void field() {
		this.box(-1, -1, -1, 1, -1, 1, Blocks.FARMLAND);

		for (int x = -1; x <= 1; x++) {
			for (int z = -1; z <= 1; z++) {
				this.place(this.at(x, 0, z), (BlockState)Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, x == 1 && z == 1 ? 3 : 7));
			}
		}
	}

	private void harvestTest(ServerPlayer player) {
		VeinRules live = VeinRules.local();
		if (VeinServer.allowed(player, live) && live.harvestCrops && live.maxBlocks >= 8 && !VeinPlanner.hungry(player, live)) {
			this.field();
			CropBlock wheat = (CropBlock)Blocks.WHEAT;
			int n = VeinExtras.harvest(player, this.level, this.at(0, 0, 0), wheat, live);
			int young = 0;
			int planted = 0;

			for (int x = -1; x <= 1; x++) {
				for (int z = -1; z <= 1; z++) {
					BlockState s = this.level.getBlockState(this.at(x, 0, z));
					if (s.getBlock() == wheat) {
						if (wheat.getAge(s) == 0) {
							planted++;
						}

						if (x == 1 && z == 1 && wheat.getAge(s) == 3) {
							young++;
						}
					}
				}
			}

			this.check(
				"a real harvest takes the eight ripe crops and plants them again, the young one stays",
				n == 8 && planted == 8 && young == 1,
				"harvested " + n + ", planted " + planted + ", young " + young
			);

			for (ItemEntity item : this.level.getEntitiesOfClass(ItemEntity.class, new AABB(this.o).inflate(16.0))) {
				item.discard();
			}

			this.clear();
		} else {
			CFHGuiVeinminer.LOGGER
				.info("[CFHGui Veinminer] selftest: the real harvest is skipped (the rules switch it off, or too small a limit, or the player is hungry)");
		}
	}

	private void breakTest(ServerPlayer player) {
		VeinRules live = VeinRules.local();
		BlockState coal = Blocks.COAL_ORE.defaultBlockState();
		VeinFinder.Harvest harvest = VeinPlanner.harvest(player, live);
		boolean canTool = harvest == null || harvest.test(coal, this.o);
		if (VeinServer.allowed(player, live)
			&& live.shapeEnabled(VeinShape.SHAPELESS)
			&& canTool
			&& VeinFinder.check(this.level, this.o, coal, live, null) == VeinFinder.Reason.OK
			&& VeinPlanner.limit(player, live) >= 5) {
			for (int x = 0; x < 5; x++) {
				this.put(x, 0, 0, Blocks.COAL_ORE);
			}

			int n = VeinServer.veinmineNow(player, this.at(0, 0, 0), Direction.UP, VeinShape.SHAPELESS);
			int left = 0;

			for (int x = 0; x < 5; x++) {
				if (!this.level.getBlockState(this.at(x, 0, 0)).isAir()) {
					left++;
				}
			}

			this.check("a real veinmine breaks all five through the game", n == 5 && left == 0, "broke " + n + ", " + left + " left");
		} else {
			CFHGuiVeinminer.LOGGER
				.info("[CFHGui Veinminer] selftest: the real veinmine is skipped (needs creative or a pickaxe, and the rules to let coal ore be veinmined)");
		}
	}

	private VeinFinder.Result tunnel(VeinShape shape, VeinRules rules, int limit, int feet) {
		return VeinFinder.find(
			this.level,
			this.o,
			this.level.getBlockState(this.o),
			shape,
			new VeinFinder.Context(Direction.SOUTH, Direction.NORTH, false, this.o.getY() + feet, false),
			rules,
			null,
			limit,
			new VeinFinder.Result()
		);
	}

	private VeinFinder.Result find(int x, int y, int z, VeinShape shape, Direction face, boolean up, boolean diagonals, VeinRules rules, int limit) {
		BlockPos p = this.at(x, y, z);
		return VeinFinder.find(this.level, p, this.level.getBlockState(p), shape, this.ctx(face, up, diagonals), rules, null, limit, new VeinFinder.Result());
	}

	private VeinFinder.Context ctx(Direction face, boolean up, boolean diagonals) {
		return new VeinFinder.Context(face, Direction.NORTH, up, this.o.getY() - 10, diagonals);
	}

	private static VeinRules rules(Consumer<VeinServerConfig> change) {
		VeinServerConfig c = new VeinServerConfig();
		change.accept(c);
		return VeinRules.of(c.sanitize());
	}

	private BlockPos at(int x, int y, int z) {
		return this.o.offset(x, y, z);
	}

	private void put(int x, int y, int z, Block block) {
		this.place(this.at(x, y, z), block.defaultBlockState());
	}

	private void box(int x0, int y0, int z0, int x1, int y1, int z1, Block block) {
		for (int x = x0; x <= x1; x++) {
			for (int y = y0; y <= y1; y++) {
				for (int z = z0; z <= z1; z++) {
					this.put(x, y, z, block);
				}
			}
		}
	}

	private void place(BlockPos p, BlockState state) {
		this.level.setBlock(p, state, 18);
		this.placed.add(p.immutable());
	}

	private void clear() {
		for (BlockPos p : this.placed) {
			this.level.setBlock(p, Blocks.AIR.defaultBlockState(), 18);
		}

		this.placed.clear();
	}

	private void expect(String what, VeinFinder.Result r, int size, boolean truncated) {
		this.check(
			what,
			r.size() == size && r.truncated == truncated && r.reason == VeinFinder.Reason.OK,
			"got " + r.size() + (r.truncated ? " (cut)" : "") + ", " + r.reason + "; wanted " + size + (truncated ? " (cut)" : "")
		);
	}

	private void expectReason(String what, VeinFinder.Result r, VeinFinder.Reason reason) {
		this.check(what, r.isEmpty() && r.reason == reason, "got " + r.size() + ", " + r.reason + "; wanted " + reason);
	}

	private void check(String what, boolean ok, String detail) {
		if (ok) {
			this.passed++;
			CFHGuiVeinminer.LOGGER.info("[CFHGui Veinminer] selftest PASS {}", what);
		} else {
			this.failures.add(what + (detail.isEmpty() ? "" : " (" + detail + ")"));
		}
	}

	static record Outcome(int passed, int failed) {
	}
}
