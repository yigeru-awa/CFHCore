package com.onehumanawa.cfhcore.content.gui.earlywindow.scene;

final class CreateFactory extends Factory {
	private static final int ROW = 0;
	private static final int BACK = -1;
	private static final int TANK = -7;
	private static final int A0 = -5;
	private static final int A1 = -1;
	private static final int B0 = 0;
	private static final int B1 = 1;
	private static final int D0 = 2;
	private static final int D1 = 3;
	private static final int T0 = 4;
	private static final int T1 = 5;
	private static final int VAULT = 6;
	private static final int PRESS = 1;
	private static final int PRESS_LEVEL = 2;
	private static final int VAULT_SIDE = 3;
	private static final float BELT_Y = 1.1875F;
	private static final int VAULT_DEEP = 2;
	private static final float X0 = -8.0F;
	private static final float X1 = 9.75F;
	private static final float Z0 = -2.0F;
	private static final float Z1 = 1.5F;
	private static final float SLOPE_FOOT = 0.37055826F;
	private static final float HEAD_TRAVEL = 1.1875F;
	private static final float PULLEY = 0.3125F;
	private static final String BELT = "create:block/belt";
	private static final String BELT_OFFSET = "create:block/belt_offset";
	private static final String BELT_DIAGONAL = "create:block/belt_diagonal";
	private static final String TANK_SIDE = "create:block/fluid_tank";
	private static final String[] VAULT_PARTS = new String[]{"front", "side", "top", "bottom"};
	private final Kit kit;
	private final Kit.Model[] belt = new Kit.Model[6];
	private final Kit.Model[] diagonal = new Kit.Model[3];
	private final Kit.Model pulley;
	private final Kit.Model shaft;
	private final Kit.Model cog;
	private final Kit.Model largeCog;
	private final Kit.Model pressBlock;
	private final Kit.Model pressHead;
	private final Kit.Model tankBottom;
	private final Kit.Model tankMiddle;
	private final Kit.Model tankTop;
	private final Kit.Model vault;
	private final Kit.Model tunnel;
	private final Kit.Model tunnelFlap;
	private final Kit.Model funnel;
	private final Kit.Model casing;
	private final Kit.Model ingot;
	private final Kit.Model sheet;
	private final Sheet.Region beltScroll;
	private final Sheet.Region diagonalScroll;
	private final Sheet.Region tankJoined;
	private final Sheet.Region[] vaultJoined = new Sheet.Region[4];
	private final Sheet.Region vaultWindow;
	private final int brass;
	private static final int STACKS_X = 4;
	private static final int STACKS_Z = 3;
	private static final int STACKS = 12;
	private static final int SHEETS = 19;
	private static final float SHEET_UP = 0.088F;
	private static final float SHEET_THICK = 0.06F;
	private static final float STACK_X = 0.54F;
	private static final float STACK_Z = 0.42F;
	private static final float PILE_Y = 0.875F;
	private static final float FLIGHT = 0.4F;
	private final Mesh pile = new Mesh(16000);
	private int piled = -1;
	private int stacked = -1;
	private float flight = -1.0F;
	private float pileTime;
	private final Mesh fixed = new Mesh(8000);
	private final Mesh fixedGlass = new Mesh(512);
	private boolean fixedBuilt;
	private final float[] at = new float[3];
	private float scrollBy;
	private boolean scrollDiagonal;
	private final int[] joinAt = new int[3];
	private final int[] joinSize = new int[3];
	static volatile RuntimeException failure;

	static CreateFactory make(Assets assets, Sheet sheet) {
		failure = null;
		if (assets == null) {
			return null;
		} else {
			try {
				return assets.read("create", "models/block/belt/start.json") == null ? null : new CreateFactory(new Kit(assets, sheet));
			} catch (RuntimeException var3) {
				failure = var3;
				return null;
			}
		}
	}

	private CreateFactory(Kit kit) {
		this.kit = kit;
		String[] parts = new String[]{"start", "middle", "end", "start_bottom", "middle_bottom", "end_bottom"};

		for (int i = 0; i < parts.length; i++) {
			this.belt[i] = kit.model("create:block/belt/" + parts[i]);
		}

		this.diagonal[0] = kit.model("create:block/belt/diagonal_start");
		this.diagonal[1] = kit.model("create:block/belt/diagonal_middle");
		this.diagonal[2] = kit.model("create:block/belt/diagonal_end");
		this.pulley = kit.model("create:block/belt_pulley");
		this.shaft = kit.model("create:block/shaft");
		this.cog = kit.model("create:block/cogwheel");
		this.largeCog = kit.model("create:block/large_cogwheel");
		this.pressBlock = kit.model("create:block/mechanical_press/block");
		this.pressHead = kit.model("create:block/mechanical_press/head");
		this.tankBottom = kit.model("create:block/fluid_tank/block_bottom_window");
		this.tankMiddle = kit.model("create:block/fluid_tank/block_middle_window");
		this.tankTop = kit.model("create:block/fluid_tank/block_top_window");
		this.vault = kit.model("create:block/item_vault");
		this.tunnel = kit.model("create:block/tunnel/andesite_tunnel/straight");
		this.tunnelFlap = kit.model("create:block/belt_tunnel/flap");
		this.funnel = kit.model("create:block/andesite_belt_funnel_retracted_unpowered");
		this.casing = kit.model("create:block/andesite_casing");
		this.ingot = kit.sprite("create:item/brass_ingot");
		this.sheet = kit.sprite("create:item/brass_sheet");

		for (String needed : new String[]{
			"create:block/belt",
			"create:block/belt_scroll",
			"create:block/belt_diagonal_scroll",
			"create:block/fluid_tank",
			"create:block/fluid_tank_connected",
			"create:item/brass_ingot",
			"create:item/brass_sheet",
			"create:block/vault/vault_front_large"
		}) {
			if (!kit.has(needed)) {
				throw new IllegalStateException("no texture " + needed);
			}
		}

		this.beltScroll = kit.texture("create:block/belt_scroll");
		this.diagonalScroll = kit.texture("create:block/belt_diagonal_scroll");
		this.tankJoined = kit.texture("create:block/fluid_tank_connected");

		for (int i = 0; i < VAULT_PARTS.length; i++) {
			this.vaultJoined[i] = kit.texture("create:block/vault/vault_" + VAULT_PARTS[i] + "_large");
		}

		this.vaultWindow = kit.windowed("create:block/vault/vault_front_large", 24, 8, 56, 40);
		this.brass = Atlas.fuller(kit.tone("create:item/brass_sheet"), 1.7F);
		this.beltY = 1.1875F;
		this.lumpX0 = -4.55F;
		this.lumpX1 = -0.3F;
		this.way = new Path(this.lumpX1, 1.1875F, 2.3705583F, 1.1875F, 3.3705583F, 2.1875F, 5.75F, 2.1875F);
		this.pressX = 1.5F;
		this.pressAt = this.pressX - this.lumpX1;
		this.planned(
			-8.0F,
			-2.0F,
			9.75F,
			1.5F,
			new float[][]{
				{-7.0F, 0.375F, -0.5F, -6.0F, 3.375F, 0.5F},
				{-5.0F, 0.375F, -1.5F, 0.0F, 2.975F, 0.5F},
				{1.0F, 0.375F, -1.5F, 3.0F, 4.075F, 0.5F},
				{6.0F, 0.375F, -1.5F, 9.0F, 3.375F, 0.5F}
			}
		);
		this.pitch = 20.0F;
		set(this.memoryAt, -6.5F, 3.5F, 0.0F);
		set(this.taskFrom, -4.9F, 1.1875F, 0.5F);
		set(this.taskTo, 0.9F, 2.375F, -0.5F);
		this.over = new float[][]{{-1.5F, 2.875F, -1.0F}, {-4.5F, 2.375F, -0.5F}};
		set(this.totalAt, 7.5F, 3.775F, -0.5F);
	}

	@Override
	void free() {
		this.fixed.free();
		this.fixedGlass.free();
		this.pile.free();
	}

	@Override
	float leaving(Motion m) {
		return 0.3F;
	}

	@Override
	void lamps(Motion m, SceneGl.Light light, int accent) {
		float flicker = 0.92F + 0.08F * (float)Math.sin((double)m.time * 7.3) * (float)Math.sin((double)m.time * 3.1 + 1.0);
		float lava = (0.35F + 0.65F * m.memory) * flicker;
		lamp(light, 0, -6.3F, 0.775F + 1.25F * m.memory, 0.9F, 4.6F, 1.0F * lava, 0.42F * lava, 0.1F * lava);
		float store = 0.16F + 0.3F * m.overall + 0.25F * (1.0F - m.landed);
		lamp(
			light,
			1,
			7.5F,
			this.pileTop() + 0.45F,
			0.1F,
			3.4F,
			(float)(this.brass >>> 16 & 0xFF) / 255.0F * store,
			(float)(this.brass >>> 8 & 0xFF) / 255.0F * store,
			(float)(this.brass & 0xFF) / 255.0F * store
		);
		float flash = m.stroke >= 0.11F ? Math.max(0.0F, 1.0F - (m.stroke - 0.11F) / 0.18F) : 0.0F;
		lamp(light, 2, this.pressX, 1.4875F, 0.5F, 2.6F, 1.0F * flash, 0.8F * flash, 0.5F * flash);
	}

	@Override
	void build(Motion m, Frame f, int accent) {
		f.plinth(-8.0F, -2.0F, 9.75F, 1.5F, 0.375F);
		if (!this.fixedBuilt) {
			this.fixedBuilt = true;
			this.fixed.clear();
			this.fixedGlass.clear();
			this.standing(this.fixed, this.fixedGlass);
		}

		f.solid.add(this.fixed);
		f.glass.add(this.fixedGlass);
		Mesh solid = f.solid;
		plain(solid);
		float level = 0.0625F + 2.4375F * m.memory;
		f.lava(-6.92F, 0.625F, -0.42F, -6.08F, 0.625F + level, 0.42F);
		float heat = 0.25F + 0.75F * m.memory;
		f.halo(-6.5F, 0.625F + level * 0.5F, 0.55F, 1.3F + level * 0.55F, -34274, 0.3F * heat);
		f.halo(-6.5F, 0.625F + level, 0.55F, 0.9F, -19641, 0.18F * heat);
		plain(solid);
		this.level(f, -5, -1, 0, m.beltA);
		this.level(f, 0, 1, 0, m.beltB);
		this.slope(f, m.beltB);
		this.level(f, 4, 5, 1, m.beltB);
		float turnA = -m.beltA / 0.3125F * (float) (180.0 / Math.PI);
		float turnB = -m.beltB / 0.3125F * (float) (180.0 / Math.PI);
		this.wheel(f, this.cog, -1.0F, 0.0F, -1, turnA);
		this.wheel(f, this.largeCog, -2.0F, 1.0F, -1, -turnA / 2.0F + 11.25F);
		this.wheel(f, this.cog, 1.0F, 2.0F, -1, turnB);
		this.wheel(f, this.largeCog, 2.0F, 1.0F, -1, -turnB / 2.0F + 11.25F);
		this.wheel(f, this.shaft, 1.0F, 2.0F, 0, turnB);
		block(solid, 1.0F, 2.0F - 1.1875F * m.pressDown, 0);
		this.kit.emit(solid, f.glass, this.pressHead, null);
		solid.pop();
		if (m.stroke >= 0.11F) {
			float flash = Math.max(0.0F, 1.0F - (m.stroke - 0.11F) / 0.2F);
			f.halo(this.pressX, 1.3875F, 0.45F, 1.1F, -11638, 0.5F * flash);
		}

		for (Motion.Item it : m.items) {
			float scale = 1.0F;
			if (it.leaving >= 0.0F) {
				this.way.at(this.way.length(), this.at);
				this.at[0] = this.at[0] + 1.9F * it.leaving;
				scale = Math.max(0.0F, 1.0F - it.leaving / this.leaving(m));
			} else {
				this.place(it.s, this.at);
			}

			this.lying(f, it.pressed ? this.sheet : this.ingot, this.at[0], this.at[1], this.at[2], it.id, scale);
		}

		this.stored(m, f);
		air(m, f, -8.0F, 9.75F, 1.0F);
	}

	private static int sheets(Motion m) {
		return Math.round(Math.max(0.0F, Math.min(1.0F, m.overall)) * 12.0F * 19.0F);
	}

	private static int high(int stack, int count) {
		int turn = (2 - stack / 4) * 4 + stack % 4;
		return count / 12 + (turn < count % 12 ? 1 : 0);
	}

	private static int stackOf(int count) {
		int turn = (count - 1) % 12;
		return (2 - turn / 4) * 4 + turn % 4;
	}

	private float pileTop() {
		return 0.875F + (float)((Math.max(0, this.stacked) + 12 - 1) / 12) * 0.088F;
	}

	private static float stackX(int stack) {
		return 7.5F + ((float)(stack % 4) - 1.5F) * 0.54F;
	}

	private static float stackZ(int stack) {
		return 0.2F - (float)(stack / 4) * 0.42F;
	}

	private void stackedSheet(Mesh into, int stack, int k, float x, float y, float z, float turned, Kit.Skin skin) {
		into.push();
		into.translate(x + (Atlas.hash(stack, k, 54) - 0.5F) * 0.03F, y + 0.03F, z + (Atlas.hash(stack, k, 55) - 0.5F) * 0.03F);
		into.rotateY((Atlas.hash(stack, k, 53) - 0.5F) * 9.0F + turned);
		into.rotateX(-90.0F);
		into.scale(0.6F, 0.6F, 0.96F);
		into.translate(-0.5F, -0.5F, -0.5F);
		this.kit.emit(into, into, this.sheet, skin);
		into.pop();
	}

	private static Sheet.Region seen(Kit.Quad q, float[] uv) {
		return !(q.n[2] < -0.5F) && !(q.n[1] > 0.5F) ? null : Kit.SKIP;
	}

	private void stored(Motion m, Frame f) {
		int count = sheets(m);
		float dt = Math.max(0.0F, Math.min(0.1F, m.time - this.pileTime));
		this.pileTime = m.time;
		if (this.stacked < 0 || count < this.stacked) {
			this.stacked = count;
			this.flight = -1.0F;
		} else if (this.flight >= 0.0F) {
			this.flight += dt / 0.4F;
			if (this.flight >= 1.0F) {
				this.flight = -1.0F;
			}
		}

		if (this.flight < 0.0F && count > this.stacked) {
			this.stacked = count;
			this.flight = 0.0F;
		}

		int lying = this.flight >= 0.0F ? this.stacked - 1 : this.stacked;
		if (lying != this.piled) {
			this.piled = lying;
			this.pile.clear();
			this.pile.tile(Tile.WHITE).colour(-15000027).faces(61);
			this.pile.box(6.4F, 0.4375F, -1.2F, 8.6F, 0.871F, 0.44F);
			metal(this.pile);

			for (int i = 0; i < 12; i++) {
				int high = high(i, lying);

				for (int k = i < 4 ? 0 : Math.max(0, high - 3); k < high; k++) {
					this.stackedSheet(this.pile, i, k, stackX(i), 0.875F + (float)k * 0.088F, stackZ(i), 0.0F, CreateFactory::seen);
				}
			}

			this.pile.reset();
		}

		f.solid.add(this.pile);
		if (this.flight >= 0.0F) {
			int stack = stackOf(this.stacked);
			int k = high(stack, this.stacked) - 1;
			float t = this.flight;
			float left = 1.0F - t;
			float fromX = 6.2F;
			float fromY = 2.925F;
			float toY = 0.875F + (float)k * 0.088F;
			float x = 6.2F + (stackX(stack) - 6.2F) * t;
			float z = stackZ(stack) * t;
			float y = 2.925F + (toY - 2.925F) * t * t + 0.3F * (float)Math.sin(Math.PI * (double)t) * left;
			metal(f.solid);
			this.stackedSheet(f.solid, stack, k, x, y, z, 120.0F * left, null);
			plain(f.solid);
		}

		float glow = 0.08F + 0.12F * m.overall + 0.2F * (1.0F - m.landed);
		f.halo(7.5F, Math.min(2.775F, this.pileTop()), 0.6F, 1.6F, Atlas.mix(this.brass, -1, 0.25F), glow);
	}

	private static void metal(Mesh m) {
		m.reset();
		m.gloss(0.5F).emissive(0.22F);
	}

	private void standing(Mesh solid, Mesh glass) {
		plain(solid);
		plain(glass);
		Kit.Skin walls = (q, uv) -> {
			if (!"create:block/fluid_tank".equals(q.texture)) {
				return null;
			} else {
				Kit.joined(q, uv, this.joinAt, this.joinSize);
				return this.tankJoined;
			}
		};
		this.size(1, 3, 1);
		Kit.Model[] tank = new Kit.Model[]{this.tankBottom, this.tankMiddle, this.tankTop};

		for (int y = 0; y < 3; y++) {
			this.joinAt[0] = this.joinAt[2] = 0;
			this.joinAt[1] = y;
			block(solid, -7.0F, (float)y, 0);
			this.kit.emit(solid, glass, tank[y], walls);
			solid.pop();
		}

		block(solid, -5.0F, 1.0F, 0);
		this.kit.emit(solid, glass, this.tunnel, null);
		turn(solid, 0.0F, -90.0F, 0.0F);

		for (int i = 0; i < 4; i++) {
			solid.push();
			solid.translate((float)(-i) * 3.0F / 16.0F, 0.0F, 0.0F);
			this.kit.emit(solid, glass, this.tunnelFlap, null);
			solid.pop();
		}

		solid.pop();
		block(solid, 1.0F, 2.0F, 0);
		this.kit.emit(solid, glass, this.pressBlock, null);
		solid.pop();
		this.size(3, 3, 2);

		for (int x = 0; x < 3; x++) {
			for (int y = 0; y < 3; y++) {
				for (int z = 0; z < 2; z++) {
					int bx = x;
					int by = y;
					int bz = z;
					block(solid, (float)(6 + x), (float)y, z - 2 + 1);
					this.kit.emit(solid, glass, this.vault, (q, uv) -> {
						int nx = Math.round(q.n[0]);
						int ny = Math.round(q.n[1]);
						int nz = Math.round(q.n[2]);
						if (bx + nx >= 0 && bx + nx < 3 && by + ny >= 0 && by + ny < 3 && bz + nz >= 0 && bz + nz < 2) {
							return Kit.SKIP;
						} else {
							this.joinAt[0] = bx;
							this.joinAt[1] = by;
							this.joinAt[2] = bz;
							Kit.joined(q, uv, this.joinAt, this.joinSize);
							return nz > 0 ? this.vaultWindow : this.vaultJoined[nz != 0 ? 0 : (ny > 0 ? 2 : (ny < 0 ? 3 : 1))];
						}
					});
					solid.pop();
				}
			}
		}

		float ix0 = 6.03F;
		float ix1 = 8.97F;
		float iy0 = 0.405F;
		float iy1 = 3.345F;
		float iz0 = -1.47F;
		float iz1 = 0.47F;
		solid.reset();
		solid.tile(Tile.WHITE).colour(-15460837);
		solid.quad(6.03F, 3.345F, -1.47F, 6.03F, 0.405F, -1.47F, 8.97F, 0.405F, -1.47F, 8.97F, 3.345F, -1.47F, 0.0F, 0.0F, 1.0F, 1.0F);
		solid.colour(-15000027);
		solid.quad(6.03F, 0.405F, -1.47F, 6.03F, 0.405F, 0.47F, 8.97F, 0.405F, 0.47F, 8.97F, 0.405F, -1.47F, 0.0F, 0.0F, 1.0F, 1.0F);
		solid.colour(-15724010);
		solid.quad(6.03F, 3.345F, 0.47F, 6.03F, 3.345F, -1.47F, 8.97F, 3.345F, -1.47F, 8.97F, 3.345F, 0.47F, 0.0F, 0.0F, 1.0F, 1.0F);
		solid.colour(-15263200);
		solid.quad(6.03F, 3.345F, 0.47F, 6.03F, 0.405F, 0.47F, 6.03F, 0.405F, -1.47F, 6.03F, 3.345F, -1.47F, 0.0F, 0.0F, 1.0F, 1.0F);
		solid.quad(8.97F, 3.345F, -1.47F, 8.97F, 0.405F, -1.47F, 8.97F, 0.405F, 0.47F, 8.97F, 3.345F, 0.47F, 0.0F, 0.0F, 1.0F, 1.0F);
		glass.reset();
		glass.tile(Tile.GLASS_BACK).emissive(0.5F);
		glass.quad(6.5F, 2.875F, 0.46F, 6.5F, 0.875F, 0.46F, 8.5F, 0.875F, 0.46F, 8.5F, 2.875F, 0.46F, 0.0F, 0.0F, 1.0F, 1.0F);
		plain(solid);
		plain(glass);
		block(solid, 5.0F, 2.0F, 0);
		turn(solid, 0.0F, 90.0F, 0.0F);
		this.kit.emit(solid, glass, this.funnel, null);
		solid.pop();

		for (int x = 3; x <= 5; x++) {
			block(solid, (float)x, 0.0F, 0);
			this.kit.emit(solid, glass, this.casing, null);
			solid.pop();
		}

		this.flat(solid, glass, this.ingot, -3.6F, 0.375F, 1.15F, 0.0F, 35.0F, 1.0F);
		this.flat(solid, glass, this.ingot, -3.2F, 0.375F, 1.3F, 0.0F, -20.0F, 1.0F);
		this.flat(solid, glass, this.sheet, 3.3F, 0.375F, 1.2F, 0.0F, 15.0F, 1.0F);
		plain(solid);
		plain(glass);
	}

	private void level(Frame f, int from, int to, int level, float travel) {
		Mesh solid = f.solid;

		for (int b = from; b <= to; b++) {
			int part = b == from ? 0 : (b == to ? 2 : 1);
			block(solid, (float)b, (float)level, 0);
			turn(solid, 0.0F, 90.0F, 0.0F);
			this.run(travel - (float)(b - from), false);
			this.kit.emit(solid, f.glass, this.belt[part], this::running);
			this.run(travel - (float)(b - from) + 0.5F, false);
			this.kit.emit(solid, f.glass, this.belt[part + 3], this::running);
			solid.pop();
		}

		float angle = -travel / 0.3125F * (float) (180.0 / Math.PI);
		this.wheel(f, this.pulley, (float)from, (float)level, 0, angle);
		this.wheel(f, this.pulley, (float)to, (float)level, 0, angle);
	}

	private void slope(Frame f, float travel) {
		Mesh solid = f.solid;

		for (int i = 0; i < 2; i++) {
			block(solid, (float)(2 + i), (float)i, 0);
			turn(solid, 0.0F, -90.0F, 0.0F);
			this.run(travel * 1.4142135F - (float)i, true);
			this.kit.emit(solid, f.glass, this.diagonal[i == 0 ? 0 : 2], this::running);
			solid.pop();
		}

		float angle = -travel / 0.3125F * (float) (180.0 / Math.PI);
		this.wheel(f, this.pulley, 2.0F, 0.0F, 0, angle);
		this.wheel(f, this.pulley, 3.0F, 1.0F, 0, angle);
	}

	private void wheel(Frame f, Kit.Model model, float bx, float by, int row, float angle) {
		Mesh solid = f.solid;
		block(solid, bx, by, row);
		solid.translate(0.5F, 0.5F, 0.5F);
		solid.rotateX(90.0F);
		solid.rotateY(angle);
		solid.translate(-0.5F, -0.5F, -0.5F);
		this.kit.emit(solid, f.glass, model, null);
		solid.pop();
	}

	private void lying(Frame f, Kit.Model thing, float x, float y, float slope, int id, float scale) {
		if (!(scale <= 0.01F)) {
			this.flat(f.solid, f.glass, thing, x, y, 0.0F, slope, (Atlas.hash(id, 3, 19) - 0.5F) * 50.0F, scale);
		}
	}

	private void flat(Mesh solid, Mesh glass, Kit.Model thing, float x, float y, float z, float slope, float yaw, float scale) {
		plain(solid);
		solid.gloss(0.45F);
		solid.push();
		solid.translate(x, y, z);
		solid.rotateZ(slope);
		solid.translate(0.0F, 0.015625F, 0.0F);
		solid.rotateY(yaw);
		solid.rotateX(-90.0F);
		solid.scale(0.6F * scale);
		solid.translate(-0.5F, -0.5F, -0.5F);
		this.kit.emit(solid, glass, thing, null);
		solid.pop();
		plain(solid);
	}

	private void run(float blocks, boolean slope) {
		this.scrollBy = blocks;
		this.scrollDiagonal = slope;
	}

	private Sheet.Region running(Kit.Quad q, float[] uv) {
		boolean level = "create:block/belt".equals(q.texture) || "create:block/belt_offset".equals(q.texture);
		if (!level && !"create:block/belt_diagonal".equals(q.texture)) {
			return null;
		} else {
			float period = this.scrollDiagonal ? 12.0F : 16.0F;
			float texels = this.scrollBy * 16.0F;
			float by = texels - (float)Math.floor((double)(texels / period)) * period;

			for (int c = 0; c < 4; c++) {
				uv[c * 2 + 1] = (uv[c * 2 + 1] + by) / 2.0F;
			}

			return this.scrollDiagonal ? this.diagonalScroll : this.beltScroll;
		}
	}

	private void size(int x, int y, int z) {
		this.joinSize[0] = x;
		this.joinSize[1] = y;
		this.joinSize[2] = z;
	}

	private static void block(Mesh m, float bx, float by, int row) {
		m.push();
		m.translate(bx, 0.375F + by, (float)row - 0.5F);
	}

	private static void turn(Mesh m, float x, float y, float z) {
		m.translate(0.5F, 0.5F, 0.5F);
		if (y != 0.0F) {
			m.rotateY(y);
		}

		if (x != 0.0F) {
			m.rotateX(x);
		}

		if (z != 0.0F) {
			m.rotateZ(z);
		}

		m.translate(-0.5F, -0.5F, -0.5F);
	}

	private static void plain(Mesh m) {
		m.reset();
		m.gloss(0.2F);
	}
}
