package com.onehumanawa.cfhcore.content.gui.quests.client.editor;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.quests.client.QuestsState;
import com.onehumanawa.cfhcore.content.gui.quests.client.text.QuestText;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.ItemSpecs;
import com.onehumanawa.cfhcore.content.gui.quests.model.QuestBook;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

final class Pickers {
	static void open(Pickers.What what, String current, Consumer<String> onPick) {
		new PickerPopup(what, current, onPick).show();
	}

	static boolean readable(Pickers.What what, String text) {
		String s = text == null ? "" : text.trim();
		if (s.isEmpty()) {
			return true;
		} else {
			try {
				return switch (what) {
					case ICON -> s.startsWith("icon:")
					? glyph(s.substring(5))
					: (s.startsWith("texture:") ? id(s.substring(8)) : (s.startsWith("#") ? id(s.substring(1)) : !ItemSpecs.stacks(s).isEmpty()));
					case ITEM -> s.startsWith("*") || (s.startsWith("#") ? id(ItemSpecs.idOf(s)) : BuiltInRegistries.ITEM.containsKey(parse(ItemSpecs.idOf(s))));
					case GIFT -> !ItemSpecs.stacks(s).isEmpty();
					case BLOCK -> s.startsWith("#") ? id(s.substring(1)) : BuiltInRegistries.BLOCK.containsKey(parse(s));
					case MOB -> s.startsWith("#") ? id(s.substring(1)) : BuiltInRegistries.ENTITY_TYPE.containsKey(parse(s));
					default -> id(s);
					case STAT -> QuestText.stat(s) != null;
					case BIOME, STRUCTURE -> id(s.startsWith("#") ? s.substring(1) : s);
					case QUEST -> QuestsState.entry(s) != null;
					case CHAPTER -> {
						QuestBook book = QuestsState.book();
						yield book != null && book.chapter(s) != null;
					}
					case FLUID -> s.startsWith("#") ? id(s.substring(1)) : BuiltInRegistries.FLUID.containsKey(parse(s));
				};
			} catch (RuntimeException var4) {
				return false;
			}
		}
	}

	private static boolean id(String s) {
		return ResourceLocation.tryParse(s.trim()) != null;
	}

	private static ResourceLocation parse(String s) {
		ResourceLocation id = ResourceLocation.tryParse(s.trim());
		if (id == null) {
			throw new IllegalArgumentException(s);
		} else {
			return id;
		}
	}

	private static boolean glyph(String name) {
		try {
			Icon.valueOf(name.trim().toUpperCase(Locale.ROOT));
			return true;
		} catch (IllegalArgumentException var2) {
			return false;
		}
	}

	private Pickers() {
	}

	static enum What {
		ICON(Catalog.Kind.ITEMS, Catalog.Kind.ITEM_TAGS, Catalog.Kind.GLYPHS),
		ITEM(Catalog.Kind.ITEMS, Catalog.Kind.ITEM_TAGS),
		GIFT(Catalog.Kind.ITEMS),
		BLOCK(Catalog.Kind.BLOCKS, Catalog.Kind.BLOCK_TAGS),
		MOB(Catalog.Kind.MOBS, Catalog.Kind.MOB_TAGS),
		ADVANCEMENT(Catalog.Kind.ADVANCEMENTS),
		STAT(Catalog.Kind.STATS),
		DIMENSION(Catalog.Kind.DIMENSIONS),
		BIOME(Catalog.Kind.BIOMES),
		STRUCTURE(Catalog.Kind.STRUCTURES),
		LOOT(Catalog.Kind.LOOT),
		QUEST(Catalog.Kind.QUESTS),
		CHAPTER(Catalog.Kind.CHAPTERS),
		BACKDROP(Catalog.Kind.BACKDROPS),
		IMAGE(Catalog.Kind.IMAGES),
		FLUID(Catalog.Kind.FLUIDS, Catalog.Kind.FLUID_TAGS);

		final List<Catalog.Kind> kinds;

		private What(final Catalog.Kind... kinds) {
			this.kinds = List.of(kinds);
		}

		String key() {
			return this.name().toLowerCase(Locale.ROOT);
		}

		boolean carried() {
			return this == ICON || this == ITEM || this == GIFT || this == BLOCK;
		}

		Icon glyph() {
			return switch (this) {
				case BLOCK, BACKDROP -> Icon.BLOCK;
				case MOB -> Icon.SWORD;
				case ADVANCEMENT -> Icon.TROPHY;
				case STAT -> Icon.SIGNAL;
				case DIMENSION -> Icon.WORLD;
				case BIOME, STRUCTURE -> Icon.MAP;
				case LOOT -> Icon.PACK;
				case QUEST, CHAPTER -> Icon.QUEST;
				case IMAGE -> Icon.IMAGE;
				case FLUID -> Icon.CLOUD;
				default -> Icon.SEARCH;
			};
		}
	}
}
