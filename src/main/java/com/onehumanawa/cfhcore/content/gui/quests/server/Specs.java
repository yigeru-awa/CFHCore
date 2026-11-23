package com.onehumanawa.cfhcore.content.gui.quests.server;

import com.mojang.brigadier.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.arguments.item.ItemParser;
import net.minecraft.commands.arguments.item.ItemPredicateArgument;
import net.minecraft.commands.arguments.item.ItemParser.ItemResult;
import net.minecraft.core.Registry;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.stats.Stat;
import net.minecraft.stats.StatType;
import net.minecraft.tags.TagKey;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

final class Specs {
	static final String NEEDS_FILTERS = "needs FTB Filter System";

	static List<String> split(String text) {
		List<String> out = new ArrayList<>(2);
		if (text.indexOf(124) < 0) {
			if (!text.isBlank()) {
				out.add(text.trim());
			}

			return out;
		} else {
			int depth = 0;
			int start = 0;
			char quote = 0;

			for (int i = 0; i < text.length(); i++) {
				char c = text.charAt(i);
				if (quote != 0) {
					if (c == '\\') {
						i++;
					} else if (c == quote) {
						quote = 0;
					}
				} else if (c == '"' || c == '\'') {
					quote = c;
				} else if (c == '[' || c == '{' || c == '(') {
					depth++;
				} else if (c == ']' || c == '}' || c == ')') {
					depth--;
				} else if (c == '|' && depth <= 0 && i > 0 && text.charAt(i - 1) == ' ' && i + 1 < text.length() && text.charAt(i + 1) == ' ') {
					if (!text.substring(start, i).isBlank()) {
						out.add(text.substring(start, i).trim());
					}

					start = i + 1;
				}
			}

			if (!text.substring(start).isBlank()) {
				out.add(text.substring(start).trim());
			}

			return out;
		}
	}

	static ItemStack stack(@Nullable String text, Provider registries, int count) {
		if (text == null) {
			return ItemStack.EMPTY;
		} else {
			String spec = text.trim();
			if (!spec.isEmpty() && spec.charAt(0) != '#' && spec.charAt(0) != '*') {
				try {
					if (spec.indexOf(91) < 0) {
						ResourceLocation id = ResourceLocation.tryParse(spec);
						Item item = id == null ? null : (Item)BuiltInRegistries.ITEM.getOptional(id).orElse(null);
						return item != null && item != Items.AIR ? new ItemStack(item, count) : ItemStack.EMPTY;
					} else {
						ItemResult r = new ItemParser(registries).parse(new StringReader(spec));
						return r.item().value() == Items.AIR ? ItemStack.EMPTY : new ItemStack(r.item(), count, r.components());
					}
				} catch (Exception var6) {
					return ItemStack.EMPTY;
				}
			} else {
				return ItemStack.EMPTY;
			}
		}
	}

	@Nullable
	static Stat<?> stat(@Nullable String text) {
		if (text == null) {
			return null;
		} else {
			String spec = text.trim();
			if (spec.isEmpty()) {
				return null;
			} else {
				int slash = spec.indexOf(47);
				ResourceLocation type = ResourceLocation.tryParse(slash < 0 ? "minecraft:custom" : spec.substring(0, slash));
				ResourceLocation id = ResourceLocation.tryParse(slash < 0 ? spec : spec.substring(slash + 1));
				if (type != null && id != null) {
					StatType<?> statType = (StatType<?>)BuiltInRegistries.STAT_TYPE.getOptional(type).orElse(null);
					return statType == null ? null : stat(statType, id);
				} else {
					return null;
				}
			}
		}
	}

	@Nullable
	private static <T> Stat<T> stat(StatType<T> type, ResourceLocation id) {
		T value = (T)type.getRegistry().getOptional(id).orElse(null);
		return value == null ? null : type.get(value);
	}

	private Specs() {
	}

	static final class ItemReader {
		private final Provider registries;
		@Nullable
		private ItemPredicateArgument predicates;
		String why = "";

		ItemReader(Provider registries) {
			this.registries = registries;
		}

		@Nullable
		Specs.ItemSpec read(@Nullable String text) {
			this.why = "";
			if (text == null) {
				return null;
			} else {
				List<String> parts = Specs.split(text);
				if (parts.size() <= 1) {
					return this.one(text);
				} else {
					List<Specs.ItemSpec> read = new ArrayList<>(parts.size());

					for (String part : parts) {
						Specs.ItemSpec one = this.one(part);
						if (one != null) {
							read.add(one);
						}
					}

					if (read.isEmpty()) {
						return null;
					} else {
						this.why = "";
						return read.size() == 1 ? read.get(0) : new Specs.ItemSpec(read);
					}
				}
			}
		}

		@Nullable
		private Specs.ItemSpec one(@Nullable String text) {
			if (text == null) {
				return null;
			} else {
				String spec = text.trim();
				if (spec.isEmpty()) {
					return null;
				} else if (spec.startsWith("ftbfilter:")) {
					Predicate<ItemStack> filter = Mods.filter(spec.substring("ftbfilter:".length()), this.registries);
					if (filter == null) {
						this.why = "needs FTB Filter System";
					}

					return filter == null ? null : new Specs.ItemSpec(null, null, filter);
				} else if (spec.charAt(0) == '@') {
					String mod = spec.substring(1).trim();
					return mod.isEmpty() ? null : new Specs.ItemSpec(null, null, stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equals(mod));
				} else {
					int bracket = spec.indexOf(91);
					String head = (bracket < 0 ? spec : spec.substring(0, bracket)).trim();
					Item item = null;
					TagKey<Item> tag = null;
					if (head.startsWith("#")) {
						ResourceLocation id = ResourceLocation.tryParse(head.substring(1));
						if (id == null) {
							return null;
						}

						tag = TagKey.create(Registries.ITEM, id);
					} else if (!head.equals("*")) {
						ResourceLocation id = ResourceLocation.tryParse(head);
						item = id == null ? null : (Item)BuiltInRegistries.ITEM.getOptional(id).orElse(null);
						if (item == null || item == Items.AIR) {
							return null;
						}
					}

					if (bracket < 0) {
						return new Specs.ItemSpec(item, tag, null);
					} else {
						try {
							if (this.predicates == null) {
								this.predicates = new ItemPredicateArgument(CommandBuildContext.simple(this.registries, FeatureFlags.REGISTRY.allFlags()));
							}

							StringReader reader = new StringReader(spec);
							Predicate<ItemStack> more = this.predicates.parse(reader);
							return reader.canRead() ? null : new Specs.ItemSpec(item, tag, more);
						} catch (Exception var9) {
							return null;
						}
					}
				}
			}
		}
	}

	static final class ItemSpec {
		@Nullable
		final Item item;
		@Nullable
		final TagKey<Item> tag;
		@Nullable
		private final Predicate<ItemStack> more;
		@Nullable
		private final List<Specs.ItemSpec> either;

		private ItemSpec(@Nullable Item item, @Nullable TagKey<Item> tag, @Nullable Predicate<ItemStack> more) {
			this.item = item;
			this.tag = tag;
			this.more = more;
			this.either = null;
		}

		private ItemSpec(List<Specs.ItemSpec> either) {
			this.item = null;
			this.tag = null;
			this.more = null;
			this.either = either;
		}

		boolean test(ItemStack stack) {
			if (stack.isEmpty()) {
				return false;
			} else if (this.either != null) {
				for (Specs.ItemSpec one : this.either) {
					if (one.test(stack)) {
						return true;
					}
				}

				return false;
			} else {
				return (this.item != null ? stack.is(this.item) : this.tag == null || stack.is(this.tag)) ? this.more == null || this.more.test(stack) : false;
			}
		}

		boolean carries(ItemStack stack) {
			return this.either != null ? this.test(stack) : this.more == null || this.more.test(stack);
		}

		boolean any() {
			return this.names() == null;
		}

		@Nullable
		List<Specs.Named> names() {
			if (this.either == null) {
				return this.item == null && this.tag == null
					? null
					: List.of(this.tag != null ? new Specs.Named(this.tag.location(), true) : new Specs.Named(BuiltInRegistries.ITEM.getKey(this.item), false));
			} else {
				List<Specs.Named> out = new ArrayList<>(this.either.size());

				for (Specs.ItemSpec one : this.either) {
					List<Specs.Named> names = one.names();
					if (names == null) {
						return null;
					}

					out.addAll(names);
				}

				return out;
			}
		}
	}

	static record Named(ResourceLocation id, boolean tag) {
		@Nullable
		static Specs.Named parse(@Nullable String text) {
			if (text == null) {
				return null;
			} else {
				String spec = text.trim();
				if (spec.isEmpty()) {
					return null;
				} else {
					boolean tag = spec.charAt(0) == '#';
					ResourceLocation id = ResourceLocation.tryParse(tag ? spec.substring(1) : spec);
					return id == null ? null : new Specs.Named(id, tag);
				}
			}
		}

		<T> boolean matches(net.minecraft.core.Holder<T> holder, ResourceKey<? extends Registry<T>> registry) {
			return this.tag ? holder.is(TagKey.create(registry, this.id)) : holder.is(this.id);
		}
	}
}
