package com.onehumanawa.cfhcore.content.gui.veinminer.config;

import com.onehumanawa.cfhcore.content.gui.core.config.JsonConfig;
import com.onehumanawa.cfhcore.content.gui.veinminer.VeinShape;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public final class VeinServerConfig {
	public boolean enabled = true;
	public String permission = "EVERYONE";
	public int maxBlocks = 64;
	public int blocksPerTick = 16;
	public boolean requireHarvestTool = true;
	public double exhaustionPerBlock = 0.1;
	public boolean stopBeforeToolBreaks = true;
	public boolean allowBlockEntities = false;
	public boolean allowDiagonals = true;
	public String drops = "FIRST_BLOCK";
	public boolean harvestCrops = true;
	public boolean toolActions = true;
	public VeinServerConfig.Shapes shapes = new VeinServerConfig.Shapes();
	public List<String> allow = new ArrayList<>();
	public List<String> deny = new ArrayList<>(List.of("minecraft:budding_amethyst", "minecraft:reinforced_deepslate"));
	public List<String> oreTags = new ArrayList<>(List.of("c:ores/*", "forge:ores/*", "c:*_ores"));
	public boolean deepslateOres = true;
	public boolean logsByWood = true;
	public boolean plantsAsOne = true;
	public List<String> groups = new ArrayList<>(
		List.of(
			"minecraft:red_mushroom_block, minecraft:brown_mushroom_block, minecraft:mushroom_stem",
			"minecraft:nether_wart_block, minecraft:shroomlight",
			"minecraft:warped_wart_block, minecraft:shroomlight"
		)
	);
	public static final int MAX_BLOCKS_CAP = 4096;
	private static JsonConfig<VeinServerConfig> file;
	private static volatile int version;

	public VeinServerConfig.Permission permission() {
		try {
			return VeinServerConfig.Permission.valueOf(this.permission.trim().toUpperCase(Locale.ROOT));
		} catch (RuntimeException var2) {
			return VeinServerConfig.Permission.EVERYONE;
		}
	}

	public VeinServerConfig.Drops drops() {
		try {
			return VeinServerConfig.Drops.valueOf(this.drops.trim().toUpperCase(Locale.ROOT));
		} catch (RuntimeException var2) {
			return VeinServerConfig.Drops.FIRST_BLOCK;
		}
	}

	public VeinServerConfig sanitize() {
		if (this.permission == null) {
			this.permission = "EVERYONE";
		}

		this.permission = this.permission().name();
		if (this.drops == null) {
			this.drops = "FIRST_BLOCK";
		}

		this.drops = this.drops().name();
		this.maxBlocks = Math.max(1, Math.min(4096, this.maxBlocks));
		this.blocksPerTick = Math.max(1, Math.min(4096, this.blocksPerTick));
		if (!Double.isFinite(this.exhaustionPerBlock)) {
			this.exhaustionPerBlock = 0.1;
		}

		this.exhaustionPerBlock = Math.max(0.0, Math.min(40.0, this.exhaustionPerBlock));
		if (this.shapes == null) {
			this.shapes = new VeinServerConfig.Shapes();
		}

		this.allow = clean(this.allow);
		this.deny = clean(this.deny);
		this.oreTags = clean(this.oreTags);
		this.groups = clean(this.groups);
		return this;
	}

	private static List<String> clean(List<String> in) {
		List<String> out = new ArrayList<>();
		if (in == null) {
			return out;
		} else {
			for (String s : in) {
				if (s != null && !s.isBlank()) {
					out.add(s.trim());
				}
			}

			return out;
		}
	}

	public VeinServerConfig copy() {
		return ((VeinServerConfig)JsonConfig.GSON.fromJson(JsonConfig.GSON.toJson(this), VeinServerConfig.class)).sanitize();
	}

	public static synchronized JsonConfig<VeinServerConfig> file() {
		if (file == null) {
			file = JsonConfig.of("veinminer-server", VeinServerConfig.class, VeinServerConfig::new);
			((VeinServerConfig)file.get()).sanitize();
			version++;
		}

		return file;
	}

	public static VeinServerConfig get() {
		return (VeinServerConfig)file().get();
	}

	public static synchronized VeinServerConfig reload() {
		file().load();
		((VeinServerConfig)file.get()).sanitize();
		version++;
		return (VeinServerConfig)file.get();
	}

	public static synchronized void edit(Consumer<VeinServerConfig> change) {
		file().update(c -> {
			change.accept(c);
			c.sanitize();
		});
		version++;
	}

	public static int version() {
		file();
		return version;
	}

	public static enum Drops {
		FIRST_BLOCK,
		INVENTORY,
		IN_PLACE;
	}

	public static enum Permission {
		EVERYONE,
		OPERATORS;
	}

	public static final class Shapes {
		public boolean shapeless = true;
		public boolean tunnel1x2 = true;
		public boolean tunnel3x3 = true;
		public boolean staircase = true;
		public boolean square3x3 = true;
		public boolean column = true;

		public boolean enabled(VeinShape shape) {
			return switch (shape) {
				case SHAPELESS -> this.shapeless;
				case TUNNEL_1X2 -> this.tunnel1x2;
				case TUNNEL_3X3 -> this.tunnel3x3;
				case STAIRCASE -> this.staircase;
				case SQUARE_3X3 -> this.square3x3;
				case COLUMN -> this.column;
			};
		}

		public void set(VeinShape shape, boolean on) {
			switch (shape) {
				case SHAPELESS:
					this.shapeless = on;
					break;
				case TUNNEL_1X2:
					this.tunnel1x2 = on;
					break;
				case TUNNEL_3X3:
					this.tunnel3x3 = on;
					break;
				case STAIRCASE:
					this.staircase = on;
					break;
				case SQUARE_3X3:
					this.square3x3 = on;
					break;
				case COLUMN:
					this.column = on;
			}
		}
	}
}
