package com.onehumanawa.cfhcore.content.gui.quests.client.view;

import com.mojang.brigadier.StringReader;
import com.onehumanawa.cfhcore.content.gui.core.platform.ModInfo;
import com.onehumanawa.cfhcore.content.gui.core.platform.CFHGuiPlatform;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.arguments.item.ItemParser;
import net.minecraft.commands.arguments.item.ItemParser.ItemResult;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

public final class ItemSpecs {
	private static final Map<String, List<ItemStack>> CACHE = new HashMap<>();
	@Nullable
	private static Object cachedFor;
	private static final int SHOWN = 64;

	public static boolean isTag(@Nullable String spec) {
		return spec != null && spec.startsWith("#");
	}

	public static List<ItemStack> stacks(@Nullable String spec) {
		if (spec != null && !spec.isBlank()) {
			Object world = Minecraft.getInstance().getConnection();
			if (world != cachedFor) {
				CACHE.clear();
				cachedFor = world;
			}

			return CACHE.computeIfAbsent(spec.trim(), ItemSpecs::read);
		} else {
			return List.of();
		}
	}

	public static ItemStack stack(@Nullable String spec) {
		List<ItemStack> all = stacks(spec);
		return all.isEmpty() ? ItemStack.EMPTY : all.get(0);
	}

	public static String idOf(String spec) {
		String id = spec.trim();
		if (id.startsWith("#")) {
			id = id.substring(1);
		}

		int bracket = id.indexOf(91);
		return bracket >= 0 ? id.substring(0, bracket) : id;
	}

	public static List<String> split(@Nullable String text) {
		List<String> out = new ArrayList<>(2);
		if (text != null && !text.isBlank()) {
			if (text.indexOf(124) < 0) {
				out.add(text.trim());
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
		} else {
			return out;
		}
	}

	public static boolean isFilter(@Nullable String spec) {
		return spec != null && spec.trim().startsWith("ftbfilter:");
	}

	public static boolean filtersKnown() {
		return CFHGuiPlatform.get().isModLoaded("ftbfiltersystem");
	}

	private static List<ItemStack> read(String spec) {
		try {
			List<String> parts = split(spec);
			if (parts.size() > 1) {
				List<ItemStack> out = new ArrayList<>();

				for (String part : parts) {
					for (ItemStack stack : read(part)) {
						if (out.size() >= 64) {
							return out;
						}

						out.add(stack);
					}
				}

				return out;
			} else if (spec.startsWith("@")) {
				String mod = spec.substring(1).trim();
				List<ItemStack> out = new ArrayList<>();

				for (Item item : BuiltInRegistries.ITEM) {
					if (out.size() >= 64) {
						break;
					}

					if (item != Items.AIR && BuiltInRegistries.ITEM.getKey(item).getNamespace().equals(mod)) {
						out.add(new ItemStack(item));
					}
				}

				return out;
			} else if (isFilter(spec)) {
				return List.of();
			} else if (spec.startsWith("#")) {
				ResourceLocation id = ResourceLocation.tryParse(idOf(spec));
				if (id == null) {
					return List.of();
				} else {
					List<ItemStack> out = new ArrayList<>();

					for (Holder<Item> h : BuiltInRegistries.ITEM.getTagOrEmpty(TagKey.create(Registries.ITEM, id))) {
						out.add(new ItemStack(h));
					}

					return out;
				}
			} else if (spec.startsWith("*")) {
				return List.of();
			} else {
				ItemResult r = new ItemParser(registries()).parse(new StringReader(spec));
				ItemStack stack = new ItemStack(r.item(), 1, r.components());
				return !stack.isEmpty() && !stack.is(Items.AIR) ? List.of(stack) : List.of();
			}
		} catch (Exception var7) {
			return List.of();
		}
	}

	private static Provider registries() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level != null) {
			return mc.level.registryAccess();
		} else {
			return mc.getConnection() != null ? mc.getConnection().registryAccess() : RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
		}
	}

	public static Component name(@Nullable String spec) {
		if (spec != null && !spec.isBlank()) {
			List<String> parts = split(spec);
			if (parts.size() > 1) {
				Component a = name(parts.get(0));
				Component b = name(parts.get(1));
				return parts.size() == 2
					? Component.translatable("cfhgui_quests.either", new Object[]{a, b})
					: Component.translatable("cfhgui_quests.either.more", new Object[]{a, b, parts.size() - 2});
			} else {
				String one = spec.trim();
				if (one.startsWith("@")) {
					return Component.translatable("cfhgui_quests.any_of_mod", new Object[]{modName(one.substring(1).trim())});
				} else if (isFilter(one)) {
					return Component.translatable(filtersKnown() ? "cfhgui_quests.filter_items" : "cfhgui_quests.filter_items.missing");
				} else if (spec.trim().startsWith("*")) {
					return Component.translatable("cfhgui_quests.any_item");
				} else if (isTag(spec)) {
					return Component.translatable("cfhgui_quests.any", new Object[]{readable(idOf(spec))});
				} else {
					ItemStack s = stack(spec);
					return (Component)(s.isEmpty() ? Component.literal(readable(idOf(spec))) : s.getHoverName());
				}
			}
		} else {
			return Component.empty();
		}
	}

	public static String readable(String id) {
		String path = idOf(id);
		if (path.indexOf(47) >= 0) {
			path = path.substring(path.lastIndexOf(47) + 1);
		}

		if (path.indexOf(58) >= 0) {
			path = path.substring(path.lastIndexOf(58) + 1);
		}

		StringBuilder sb = new StringBuilder();

		for (String word : path.split("[_.]")) {
			if (!word.isEmpty()) {
				if (sb.length() > 0) {
					sb.append(' ');
				}

				sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
			}
		}

		return sb.toString();
	}

	private static String modName(String id) {
		try {
			String name = CFHGuiPlatform.get().modInfo(id).<String>map(ModInfo::name).orElse("");
			if (!name.isBlank()) {
				return name;
			}
		} catch (RuntimeException var2) {
		}

		return readable(id);
	}

	private ItemSpecs() {
	}
}
