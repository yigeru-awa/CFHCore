package com.onehumanawa.cfhcore.content.gui.veinminer.vein;

import com.onehumanawa.cfhcore.content.gui.veinminer.CFHGuiVeinminer;
import com.onehumanawa.cfhcore.content.gui.veinminer.VeinShape;
import com.onehumanawa.cfhcore.content.gui.veinminer.config.VeinServerConfig;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.AttachedStemBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public final class VeinRules {
	public final boolean enabled;
	public final boolean operatorsOnly;
	public final int maxBlocks;
	public final int blocksPerTick;
	public final boolean requireHarvestTool;
	public final float exhaustionPerBlock;
	public final boolean stopBeforeToolBreaks;
	public final boolean allowBlockEntities;
	public final boolean allowDiagonals;
	public final VeinServerConfig.Drops drops;
	public final boolean harvestCrops;
	public final boolean toolActions;
	public final boolean deepslateOres;
	public final boolean logsByWood;
	public final boolean plantsAsOne;
	private final boolean[] shapes = new boolean[VeinShape.count()];
	@Nullable
	private final VeinRules.Matcher allow;
	private final VeinRules.Matcher deny;
	private final List<VeinRules.TagPattern> oreTags;
	private final List<VeinRules.Matcher> groups;
	@Nullable
	private static VeinRules local;
	private static int localVersion = -1;
	private static final Object[] NONE = new Object[0];
	private static final String PLANT = "plant";

	private VeinRules(VeinServerConfig c) {
		this.enabled = c.enabled;
		this.operatorsOnly = c.permission() == VeinServerConfig.Permission.OPERATORS;
		this.maxBlocks = c.maxBlocks;
		this.blocksPerTick = c.blocksPerTick;
		this.requireHarvestTool = c.requireHarvestTool;
		this.exhaustionPerBlock = (float)c.exhaustionPerBlock;
		this.stopBeforeToolBreaks = c.stopBeforeToolBreaks;
		this.allowBlockEntities = c.allowBlockEntities;
		this.allowDiagonals = c.allowDiagonals;
		this.drops = c.drops();
		this.harvestCrops = c.harvestCrops;
		this.toolActions = c.toolActions;
		this.deepslateOres = c.deepslateOres;
		this.logsByWood = c.logsByWood;
		this.plantsAsOne = c.plantsAsOne;

		for (VeinShape s : VeinShape.all()) {
			this.shapes[s.ordinal()] = c.shapes.enabled(s);
		}

		this.allow = c.allow.isEmpty() ? null : VeinRules.Matcher.of(c.allow);
		this.deny = VeinRules.Matcher.of(c.deny);
		List<VeinRules.TagPattern> patterns = new ArrayList<>();

		for (String p : c.oreTags) {
			VeinRules.TagPattern tp = VeinRules.TagPattern.parse(p);
			if (tp != null) {
				patterns.add(tp);
			}
		}

		this.oreTags = List.copyOf(patterns);
		List<VeinRules.Matcher> gs = new ArrayList<>();

		for (String g : c.groups) {
			List<String> entries = new ArrayList<>();

			for (String part : g.split("[,;\\s]+")) {
				if (!part.isBlank()) {
					entries.add(part.trim());
				}
			}

			if (entries.size() > 0) {
				gs.add(VeinRules.Matcher.of(entries));
			}
		}

		this.groups = List.copyOf(gs);
	}

	public static VeinRules of(VeinServerConfig config) {
		return new VeinRules(config);
	}

	public static synchronized VeinRules local() {
		int v = VeinServerConfig.version();
		if (local == null || v != localVersion) {
			local = of(VeinServerConfig.get());
			localVersion = v;
		}

		return local;
	}

	public boolean shapeEnabled(VeinShape shape) {
		return this.shapes[shape.ordinal()];
	}

	public VeinShape firstEnabledFrom(VeinShape wanted) {
		int n = VeinShape.count();

		for (int i = 0; i < n; i++) {
			VeinShape s = VeinShape.byOrdinal((wanted.ordinal() + i) % n);
			if (this.shapeEnabled(s)) {
				return s;
			}
		}

		return wanted;
	}

	public boolean anyShape() {
		for (boolean b : this.shapes) {
			if (b) {
				return true;
			}
		}

		return false;
	}

	public boolean blockAllowed(BlockState state) {
		return this.deny.matches(state) ? false : this.allow == null || this.allow.matches(state);
	}

	public boolean sameGroup(BlockState start, BlockState other, IdentityHashMap<Block, Object[]> cache) {
		Block a = start.getBlock();
		Block b = other.getBlock();
		if (a == b) {
			return true;
		} else {
			Object[] ka = this.keys(start, cache);
			Object[] kb = this.keys(other, cache);
			if (ka.length != 0 && kb.length != 0) {
				for (Object x : ka) {
					for (Object y : kb) {
						if (x.equals(y)) {
							return true;
						}
					}
				}

				return false;
			} else {
				return false;
			}
		}
	}

	public static boolean isLog(BlockState state) {
		return state.is(BlockTags.LOGS);
	}

	public static boolean isPlant(BlockState state) {
		if (!state.is(BlockTags.CROPS) && !state.is(BlockTags.SAPLINGS) && !state.is(BlockTags.LEAVES)) {
			Block b = state.getBlock();
			if (b instanceof CropBlock || b instanceof StemBlock || b instanceof AttachedStemBlock || b instanceof NetherWartBlock || b instanceof SaplingBlock) {
				return false;
			} else if (b instanceof BushBlock) {
				return true;
			} else if ((state.is(BlockTags.FLOWERS) || state.is(BlockTags.REPLACEABLE_BY_TREES)) && state.getFluidState().isEmpty()) {
				try {
					return state.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO).isEmpty();
				} catch (RuntimeException var3) {
					return false;
				}
			} else {
				return false;
			}
		} else {
			return false;
		}
	}

	private Object[] keys(BlockState state, IdentityHashMap<Block, Object[]> cache) {
		Block block = state.getBlock();
		Object[] hit = cache.get(block);
		if (hit != null) {
			return hit;
		} else {
			List<Object> out = new ArrayList<>(2);
			if (!this.oreTags.isEmpty()) {
				state.getTags().forEach(tag -> {
					for (VeinRules.TagPattern p : this.oreTags) {
						if (p.matches(tag.location())) {
							out.add(tag);
							break;
						}
					}
				});
			}

			ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
			String path = id.getPath();
			if (this.deepslateOres && path.endsWith("_ore")) {
				out.add("ore:" + id.getNamespace() + ":" + (path.startsWith("deepcfhgui_") ? path.substring("deepcfhgui_".length()) : path));
			}

			if (this.logsByWood && isLog(state)) {
				String wood = wood(path);
				if (wood != null) {
					out.add("wood:" + id.getNamespace() + ":" + wood);
				}
			}

			for (int i = 0; i < this.groups.size(); i++) {
				if (this.groups.get(i).matches(state)) {
					out.add(i);
				}
			}

			if (this.plantsAsOne && isPlant(state)) {
				out.add("plant");
			}

			Object[] keys = out.isEmpty() ? NONE : out.toArray();
			cache.put(block, keys);
			return keys;
		}
	}

	@Nullable
	static String wood(String path) {
		String p = path.startsWith("stripped_") ? path.substring("stripped_".length()) : path;

		for (String suffix : new String[]{"_log", "_wood", "_stem", "_hyphae"}) {
			if (p.endsWith(suffix) && p.length() > suffix.length()) {
				return p.substring(0, p.length() - suffix.length());
			}
		}

		return null;
	}

	static final class Matcher {
		private final Set<Block> blocks;
		private final List<TagKey<Block>> tags;

		private Matcher(Set<Block> blocks, List<TagKey<Block>> tags) {
			this.blocks = blocks;
			this.tags = tags;
		}

		static VeinRules.Matcher of(List<String> entries) {
			Set<Block> blocks = Collections.newSetFromMap(new IdentityHashMap<>());
			List<TagKey<Block>> tags = new ArrayList<>();

			for (String raw : entries) {
				String e = raw.trim().toLowerCase(Locale.ROOT);
				if (!e.isEmpty()) {
					if (e.startsWith("#")) {
						ResourceLocation rl = ResourceLocation.tryParse(e.substring(1));
						if (rl != null) {
							tags.add(TagKey.create(Registries.BLOCK, rl));
						} else {
							CFHGuiVeinminer.LOGGER.warn("[CFHGui Veinminer] not a tag: {}", raw);
						}
					} else {
						ResourceLocation rl = ResourceLocation.tryParse(e);
						if (rl == null) {
							CFHGuiVeinminer.LOGGER.warn("[CFHGui Veinminer] not a block id: {}", raw);
						} else {
							BuiltInRegistries.BLOCK.getOptional(rl).ifPresent(blocks::add);
						}
					}
				}
			}

			return new VeinRules.Matcher(blocks, List.copyOf(tags));
		}

		boolean matches(BlockState state) {
			if (this.blocks.contains(state.getBlock())) {
				return true;
			} else {
				for (TagKey<Block> t : this.tags) {
					if (state.is(t)) {
						return true;
					}
				}

				return false;
			}
		}
	}

	static record TagPattern(String namespace, String prefix, String suffix, boolean wildcard) {
		@Nullable
		static VeinRules.TagPattern parse(String raw) {
			String s = raw.trim().toLowerCase(Locale.ROOT);
			if (s.startsWith("#")) {
				s = s.substring(1);
			}

			int colon = s.indexOf(58);
			if (colon > 0 && colon != s.length() - 1) {
				String ns = s.substring(0, colon);
				String path = s.substring(colon + 1);
				int star = path.indexOf(42);
				if (star < 0) {
					return new VeinRules.TagPattern(ns, path, "", false);
				} else {
					return path.indexOf(42, star + 1) >= 0 ? null : new VeinRules.TagPattern(ns, path.substring(0, star), path.substring(star + 1), true);
				}
			} else {
				return null;
			}
		}

		boolean matches(ResourceLocation tag) {
			if (!tag.getNamespace().equals(this.namespace)) {
				return false;
			} else {
				String p = tag.getPath();
				return !this.wildcard
					? p.equals(this.prefix)
					: p.length() > this.prefix.length() + this.suffix.length() && p.startsWith(this.prefix) && p.endsWith(this.suffix);
			}
		}
	}
}
