package com.onehumanawa.cfhcore.content.gui.quests.client.editor;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.quests.CFHGuiQuests;
import com.onehumanawa.cfhcore.content.gui.quests.client.QuestsState;
import com.onehumanawa.cfhcore.content.gui.quests.client.text.QuestText;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.ItemSpecs;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.QuestIcons;
import com.onehumanawa.cfhcore.content.gui.quests.logic.BookIndex;
import com.onehumanawa.cfhcore.content.gui.quests.model.Chapter;
import com.onehumanawa.cfhcore.content.gui.quests.model.QuestBook;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.stats.StatType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import org.jetbrains.annotations.Nullable;

final class Catalog {
	private static final Map<String, List<Catalog.Entry>> CACHE = new HashMap<>();
	@Nullable
	private static Object cachedFor;
	@Nullable
	private static String cachedLanguage;

	static List<Catalog.Entry> of(Catalog.Kind kind, String arg) {
		if (kind != Catalog.Kind.QUESTS && kind != Catalog.Kind.CHAPTERS) {
			Minecraft mc = Minecraft.getInstance();
			Object world = mc.getConnection();
			String language = mc.getLanguageManager().getSelected();
			if (world != cachedFor || !language.equals(cachedLanguage)) {
				CACHE.clear();
				cachedFor = world;
				cachedLanguage = language;
			}

			return CACHE.computeIfAbsent(kind.name() + "|" + arg, k -> make(kind, arg));
		} else {
			return make(kind, arg);
		}
	}

	static List<Catalog.Entry> find(List<Catalog.Entry> all, String query) {
		String q = query.trim().toLowerCase(Locale.ROOT);
		if (q.isEmpty()) {
			return all;
		} else {
			String[] words = q.split("\\s+");
			List<Catalog.Entry> out = new ArrayList<>();

			for (Catalog.Entry e : all) {
				boolean match = true;

				for (String word : words) {
					if (word.startsWith("@") ? !e.mod.contains(word.substring(1)) : !e.search.contains(word)) {
						match = false;
						break;
					}
				}

				if (match) {
					out.add(e);
				}
			}

			return out;
		}
	}

	private static List<Catalog.Entry> make(Catalog.Kind kind, String arg) {
		try {
			return switch (kind) {
				case ITEMS -> items();
				case ITEM_TAGS -> tags(BuiltInRegistries.ITEM, true);
				case GLYPHS -> glyphs();
				case BLOCKS -> blocks(false);
				case BLOCK_TAGS -> tags(BuiltInRegistries.BLOCK, false);
				case MOBS -> mobs();
				case MOB_TAGS -> tags(BuiltInRegistries.ENTITY_TYPE, false);
				case ADVANCEMENTS -> advancements();
				case STATS -> stats(arg);
				case DIMENSIONS -> dimensions();
				case BIOMES -> biomes();
				case STRUCTURES -> structures();
				case LOOT -> loot();
				case QUESTS -> quests();
				case CHAPTERS -> chapters();
				case BACKDROPS -> blocks(true);
				case IMAGES -> images();
				case FLUIDS -> fluids();
				case FLUID_TAGS -> tags(BuiltInRegistries.FLUID, false);
			};
		} catch (RuntimeException var3) {
			CFHGuiQuests.LOGGER.warn("[CFHGui Quests] the editor could not list {}: {}", kind, var3.toString());
			return List.of();
		}
	}

	private static List<Catalog.Entry> items() {
		List<Catalog.Entry> out = new ArrayList<>();

		for (Item item : BuiltInRegistries.ITEM) {
			if (item != Items.AIR) {
				String id = BuiltInRegistries.ITEM.getKey(item).toString();
				out.add(new Catalog.Entry(id, item.getDescription().getString(), id, new ItemStack(item), null, null));
			}
		}

		return out;
	}

	private static List<Catalog.Entry> blocks(boolean textures) {
		List<Catalog.Entry> out = new ArrayList<>();
		Minecraft mc = Minecraft.getInstance();

		for (Block block : BuiltInRegistries.BLOCK) {
			ResourceLocation key = BuiltInRegistries.BLOCK.getKey(block);
			Item item = block.asItem();
			ItemStack stack = item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
			if (textures) {
				ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(key.getNamespace(), "textures/block/" + key.getPath() + ".png");
				if (!mc.getResourceManager().getResource(texture).isEmpty()) {
					String value = key.getNamespace() + ":block/" + key.getPath();
					out.add(new Catalog.Entry(value, block.getName().getString(), value, stack, Icon.BLOCK, null));
				}
			} else if (!key.getPath().equals("air")) {
				out.add(new Catalog.Entry(key.toString(), block.getName().getString(), key.toString(), stack, Icon.BLOCK, null));
			}
		}

		return out;
	}

	private static List<Catalog.Entry> images() {
		List<Catalog.Entry> out = new ArrayList<>();
		Minecraft mc = Minecraft.getInstance();

		for (Item item : BuiltInRegistries.ITEM) {
			if (item != Items.AIR) {
				ResourceLocation key = BuiltInRegistries.ITEM.getKey(item);

				for (String folder : new String[]{"item", "block"}) {
					ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(key.getNamespace(), "textures/" + folder + "/" + key.getPath() + ".png");
					if (!mc.getResourceManager().getResource(texture).isEmpty()) {
						out.add(new Catalog.Entry(texture.toString(), item.getDescription().getString(), texture.toString(), new ItemStack(item), null, null));
						break;
					}
				}
			}
		}

		return out;
	}

	private static List<Catalog.Entry> fluids() {
		List<Catalog.Entry> out = new ArrayList<>();

		for (Fluid fluid : BuiltInRegistries.FLUID) {
			if (fluid != Fluids.EMPTY && fluid.isSource(fluid.defaultFluidState())) {
				String id = BuiltInRegistries.FLUID.getKey(fluid).toString();
				Block block = fluid.defaultFluidState().createLegacyBlock().getBlock();
				String name = block == Blocks.AIR ? ItemSpecs.readable(id) : block.getName().getString();
				Item bucket = fluid.getBucket();
				out.add(new Catalog.Entry(id, name, id, bucket == Items.AIR ? ItemStack.EMPTY : new ItemStack(bucket), Icon.CLOUD, null));
			}
		}

		out.sort(Comparator.comparing(e -> e.name));
		return out;
	}

	private static List<Catalog.Entry> mobs() {
		List<Catalog.Entry> out = new ArrayList<>();

		for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
			String id = BuiltInRegistries.ENTITY_TYPE.getKey(type).toString();
			SpawnEggItem egg = SpawnEggItem.byId(type);
			out.add(new Catalog.Entry(id, type.getDescription().getString(), id, egg == null ? ItemStack.EMPTY : new ItemStack(egg), Icon.SWORD, null));
		}

		out.sort(Comparator.<Catalog.Entry, Boolean>comparing(e -> e.stack.isEmpty()).thenComparing(e -> e.name));
		return out;
	}

	private static <T> List<Catalog.Entry> tags(Registry<T> registry, boolean items) {
		List<Catalog.Entry> out = new ArrayList<>();
		registry.getTagNames().forEach(tag -> {
			String value = "#" + tag.location();
			out.add(new Catalog.Entry(value, ItemSpecs.readable(tag.location().toString()), value, ItemStack.EMPTY, Icon.TAG, items ? QuestIcons.of(value) : null));
		});
		out.sort(Comparator.comparing(e -> e.id));
		return out;
	}

	private static List<Catalog.Entry> glyphs() {
		List<Catalog.Entry> out = new ArrayList<>();

		for (Icon icon : Icon.values()) {
			String value = "icon:" + icon.name().toLowerCase(Locale.ROOT);
			out.add(new Catalog.Entry(value, ItemSpecs.readable(icon.name().toLowerCase(Locale.ROOT)), value, ItemStack.EMPTY, icon, null));
		}

		return out;
	}

	private static List<Catalog.Entry> stats(String typeId) {
		List<Catalog.Entry> out = new ArrayList<>();
		ResourceLocation id = ResourceLocation.tryParse(typeId);
		StatType<?> type = id == null ? null : (StatType)BuiltInRegistries.STAT_TYPE.get(id);
		if (type == null) {
			return out;
		} else {
			Registry registry = type.getRegistry();

			for (Object key : registry.keySet()) {
				String value;
				ItemStack var10000;
				label47: {
					value = typeId + "/" + key;
					Object thing = registry.get((ResourceLocation)key);
					if (thing instanceof Item i && i != Items.AIR) {
						var10000 = new ItemStack(i);
						break label47;
					}

					if (thing instanceof Block b && b.asItem() != Items.AIR) {
						var10000 = new ItemStack(b.asItem());
						break label47;
					}

					if (thing instanceof EntityType<?> e && SpawnEggItem.byId(e) != null) {
						var10000 = new ItemStack(SpawnEggItem.byId(e));
						break label47;
					}

					var10000 = ItemStack.EMPTY;
				}

				ItemStack stack = var10000;
				out.add(new Catalog.Entry(value, QuestText.statName(value).getString(), key.toString(), stack, Icon.SIGNAL, null));
			}

			out.sort(Comparator.comparing(ex -> ex.name));
			return out;
		}
	}

	static List<String> statTypes() {
		List<String> out = new ArrayList<>();

		for (ResourceLocation id : BuiltInRegistries.STAT_TYPE.keySet()) {
			out.add(id.toString());
		}

		out.sort(Comparator.<String, Boolean>comparing(s -> !s.endsWith(":custom")).thenComparing(s -> (String)s));
		return out;
	}

	@Nullable
	private static MinecraftServer host() {
		return Minecraft.getInstance().getSingleplayerServer();
	}

	private static List<Catalog.Entry> loot() {
		Set<String> ids = new TreeSet<>();
		MinecraftServer host = host();
		if (host != null) {
			for (ResourceLocation id : host.reloadableRegistries().getKeys(Registries.LOOT_TABLE)) {
				ids.add(id.toString());
			}
		}

		if (ids.isEmpty()) {
			for (ResourceKey<?> key : BuiltInLootTables.all()) {
				ids.add(key.location().toString());
			}
		}

		return named(new ArrayList<>(ids), Icon.PACK);
	}

	private static List<Catalog.Entry> structures() {
		Set<String> ids = new TreeSet<>();
		MinecraftServer host = host();
		if (host != null) {
			host.registryAccess().registry(Registries.STRUCTURE).ifPresent(r -> {
				for (ResourceLocation id : r.keySet()) {
					ids.add(id.toString());
				}
			});
		}

		if (ids.isEmpty()) {
			ids.addAll(constants(BuiltinStructures.class));
		}

		return named(new ArrayList<>(ids), Icon.MAP);
	}

	private static List<Catalog.Entry> advancements() {
		List<Catalog.Entry> out = new ArrayList<>();
		MinecraftServer host = host();
		ClientPacketListener connection = Minecraft.getInstance().getConnection();
		if (host != null) {
			for (AdvancementHolder h : host.getAdvancements().getAllAdvancements()) {
				out.add(advancement(h.id().toString(), h.value().display()));
			}
		} else if (connection != null) {
			for (AdvancementNode node : connection.getAdvancements().getTree().nodes()) {
				out.add(advancement(node.holder().id().toString(), node.advancement().display()));
			}
		}

		out.sort(Comparator.<Catalog.Entry, Boolean>comparing(e -> e.stack.isEmpty()).thenComparing(e -> e.id));
		return out;
	}

	private static Catalog.Entry advancement(String id, Optional<DisplayInfo> display) {
		String name = display.<String>map(d -> d.getTitle().getString()).orElseGet(() -> ItemSpecs.readable(id));
		return new Catalog.Entry(id, name, id, display.<ItemStack>map(d -> d.getIcon().copy()).orElse(ItemStack.EMPTY), Icon.TROPHY, null);
	}

	private static List<Catalog.Entry> dimensions() {
		Set<String> ids = new LinkedHashSet<>(List.of("minecraft:overworld", "minecraft:the_nether", "minecraft:the_end"));
		ClientPacketListener connection = Minecraft.getInstance().getConnection();
		if (connection != null) {
			for (ResourceKey<Level> key : connection.levels()) {
				ids.add(key.location().toString());
			}
		}

		return named(new ArrayList<>(ids), Icon.WORLD);
	}

	private static List<Catalog.Entry> biomes() {
		Set<String> ids = new LinkedHashSet<>();
		ClientPacketListener connection = Minecraft.getInstance().getConnection();
		if (connection != null) {
			connection.registryAccess().registry(Registries.BIOME).ifPresent(r -> {
				for (ResourceLocation id : r.keySet()) {
					ids.add(id.toString());
				}
			});
		}

		if (ids.isEmpty()) {
			ids.addAll(constants(Biomes.class));
		}

		List<String> sorted = new ArrayList<>(ids);
		sorted.sort(Comparator.naturalOrder());
		return named(sorted, Icon.MAP);
	}

	private static List<String> constants(Class<?> holder) {
		List<String> out = new ArrayList<>();

		for (Field f : holder.getDeclaredFields()) {
			if (Modifier.isStatic(f.getModifiers()) && Modifier.isPublic(f.getModifiers()) && ResourceKey.class.isAssignableFrom(f.getType())) {
				try {
					out.add(((ResourceKey)f.get(null)).location().toString());
				} catch (RuntimeException | IllegalAccessException var7) {
				}
			}
		}

		out.sort(Comparator.naturalOrder());
		return out;
	}

	private static List<Catalog.Entry> named(List<String> ids, Icon glyph) {
		List<Catalog.Entry> out = new ArrayList<>();

		for (String id : ids) {
			out.add(new Catalog.Entry(id, ItemSpecs.readable(id), id, ItemStack.EMPTY, glyph, null));
		}

		return out;
	}

	private static List<Catalog.Entry> quests() {
		List<Catalog.Entry> out = new ArrayList<>();
		BookIndex index = QuestsState.index();
		if (index == null) {
			return out;
		} else {
			for (BookIndex.Entry e : index.all()) {
				if (!e.chapter().readOnly) {
					out.add(
						new Catalog.Entry(
							e.key(), QuestText.title(e).getString(), e.key(), ItemStack.EMPTY, Icon.QUEST, QuestIcons.of(e), QuestText.title(e.chapter()).getString()
						)
					);
				}
			}

			return out;
		}
	}

	private static List<Catalog.Entry> chapters() {
		List<Catalog.Entry> out = new ArrayList<>();
		QuestBook book = QuestsState.book();
		if (book == null) {
			return out;
		} else {
			for (Chapter c : book.chapters.values()) {
				if (!c.readOnly) {
					out.add(new Catalog.Entry(c.id, QuestText.title(c).getString(), c.id, ItemStack.EMPTY, Icon.QUEST, QuestIcons.of(c.icon)));
				}
			}

			return out;
		}
	}

	static List<Catalog.Entry> carried(boolean whole, boolean blocksOnly) {
		List<Catalog.Entry> out = new ArrayList<>();
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) {
			return out;
		} else {
			Inventory inv = mc.player.getInventory();
			List<ItemStack> stacks = new ArrayList<>();
			stacks.add(inv.getSelected());
			stacks.addAll(inv.offhand);
			stacks.addAll(inv.items);
			Set<String> seen = new LinkedHashSet<>();

			for (ItemStack stack : stacks) {
				if (!stack.isEmpty()) {
					String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
					String value = id;
					if (blocksOnly) {
						Block block = Block.byItem(stack.getItem());
						ResourceLocation key = BuiltInRegistries.BLOCK.getKey(block);
						if (key.getPath().equals("air")) {
							continue;
						}

						value = key.toString();
					} else if (whole && !stack.getComponentsPatch().isEmpty()) {
						try {
							value = new ItemInput(stack.getItemHolder(), stack.getComponentsPatch()).serialize(mc.player.registryAccess());
						} catch (RuntimeException var13) {
							value = id;
						}
					}

					if (seen.add(value)) {
						ItemStack one = stack.copyWithCount(1);
						out.add(new Catalog.Entry(value, stack.getHoverName().getString(), value, one, null, null));
					}
				}
			}

			return out;
		}
	}

	private Catalog() {
	}

	static final class Entry {
		final String value;
		final String name;
		final String id;
		final ItemStack stack;
		@Nullable
		final Icon glyph;
		@Nullable
		final QuestIcons.Drawable drawable;
		private final String search;
		private final String mod;

		Entry(String value, String name, String id, ItemStack stack, @Nullable Icon glyph, @Nullable QuestIcons.Drawable drawable) {
			this(value, name, id, stack, glyph, drawable, "");
		}

		Entry(String value, String name, String id, ItemStack stack, @Nullable Icon glyph, @Nullable QuestIcons.Drawable drawable, String also) {
			this.value = value;
			this.name = name;
			this.id = id;
			this.stack = stack;
			this.glyph = glyph;
			this.drawable = drawable;
			this.search = (name + " " + id + " " + also).toLowerCase(Locale.ROOT);
			int colon = id.indexOf(58);
			this.mod = (colon > 0 ? id.substring(id.charAt(0) == '#' ? 1 : 0, colon) : "").toLowerCase(Locale.ROOT);
		}

		void draw(GuiGraphics g, int cx, int cy, int size, int tint) {
			if (!this.stack.isEmpty()) {
				g.pose().pushPose();
				g.pose().translate((float)cx - (float)size / 2.0F, (float)cy - (float)size / 2.0F, 0.0F);
				g.pose().scale((float)size / 16.0F, (float)size / 16.0F, 1.0F);
				g.renderFakeItem(this.stack, 0, 0);
				g.pose().popPose();
			} else if (this.drawable != null) {
				this.drawable.draw(g, (float)cx, (float)cy, (float)size, tint);
			} else if (this.glyph != null) {
				Icons.draw(g, this.glyph, cx - 6, cy - 6, 12, tint);
			}
		}

		boolean hasPicture() {
			return !this.stack.isEmpty() || this.drawable != null || this.glyph != null;
		}
	}

	static enum Kind {
		ITEMS,
		ITEM_TAGS,
		GLYPHS,
		BLOCKS,
		BLOCK_TAGS,
		MOBS,
		MOB_TAGS,
		ADVANCEMENTS,
		STATS,
		DIMENSIONS,
		BIOMES,
		STRUCTURES,
		LOOT,
		QUESTS,
		CHAPTERS,
		BACKDROPS,
		IMAGES,
		FLUIDS,
		FLUID_TAGS;

		boolean grid() {
			return this == ITEMS || this == GLYPHS || this == BLOCKS || this == BACKDROPS || this == IMAGES;
		}
	}
}
