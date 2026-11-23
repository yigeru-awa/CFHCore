package com.onehumanawa.cfhcore.content.gui.quests.server;

import com.mojang.brigadier.StringReader;
import com.onehumanawa.cfhcore.content.gui.core.platform.CFHGuiPlatform;
import com.onehumanawa.cfhcore.content.gui.quests.CFHGuiQuests;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.commands.arguments.item.ItemParser;
import net.minecraft.commands.arguments.item.ItemParser.ItemResult;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

final class Mods {
	private static final Set<String> SAID = new HashSet<>();
	@Nullable
	private static Object stages;
	@Nullable
	private static Method stageHas;
	@Nullable
	private static Method stageAdd;
	@Nullable
	private static Method stageRemove;
	private static boolean stagesAsked;

	private static void once(String what, Throwable t) {
		if (SAID.add(what)) {
			CFHGuiQuests.LOGGER.warn("[CFHGui Quests] {} ({}); said once", what, t.toString());
		}
	}

	static boolean has(String mod) {
		try {
			return CFHGuiPlatform.get().isModLoaded(mod);
		} catch (Throwable var2) {
			return false;
		}
	}

	@Nullable
	private static Object stages() {
		if (!stagesAsked) {
			stagesAsked = true;
			if (has("ftblibrary")) {
				try {
					Class<?> helper = Class.forName("dev.ftb.mods.ftblibrary.integration.stages.StageHelper");
					Class<?> provider = Class.forName("dev.ftb.mods.ftblibrary.integration.stages.StageProvider");
					stageHas = provider.getMethod("has", Player.class, String.class);
					stageAdd = provider.getMethod("add", ServerPlayer.class, String.class);
					stageRemove = provider.getMethod("remove", ServerPlayer.class, String.class);
					stages = helper.getMethod("getInstance").invoke(null);
				} catch (Throwable var3) {
					stages = null;
					once("FTB Library's stages could not be reached: a stage is a tag on the player", var3);
				}
			}
		}

		if (stages == null) {
			return null;
		} else {
			try {
				return stages.getClass().getMethod("getProvider").invoke(stages);
			} catch (Throwable var2) {
				return null;
			}
		}
	}

	static boolean hasStage(ServerPlayer player, String stage) {
		if (stage.isBlank()) {
			return false;
		} else {
			Object provider = stages();
			if (provider != null && stageHas != null) {
				try {
					return Boolean.TRUE.equals(stageHas.invoke(provider, player, stage));
				} catch (Throwable var4) {
					once("asking for a stage failed: a stage is a tag on the player", var4);
				}
			}

			return player.getTags().contains(stage);
		}
	}

	static void stage(ServerPlayer player, String stage, boolean remove) {
		if (!stage.isBlank()) {
			Object provider = stages();
			Method how = remove ? stageRemove : stageAdd;
			if (provider != null && how != null) {
				try {
					how.invoke(provider, player, stage);
					return;
				} catch (Throwable var6) {
					once("setting a stage failed: a stage is a tag on the player", var6);
				}
			}

			if (remove) {
				player.removeTag(stage);
			} else {
				player.addTag(stage);
			}
		}
	}

	static boolean currency(ServerPlayer player, int amount) {
		if (!has("ftblibrary")) {
			return false;
		} else {
			try {
				Class<?> helper = Class.forName("dev.ftb.mods.ftblibrary.integration.currency.CurrencyHelper");
				Object provider = helper.getMethod("getProvider").invoke(helper.getMethod("getInstance").invoke(null));
				if (provider == null) {
					return false;
				} else {
					Class<?> type = Class.forName("dev.ftb.mods.ftblibrary.integration.currency.CurrencyProvider");
					if (!Boolean.TRUE.equals(type.getMethod("isValidProvider").invoke(provider))) {
						return false;
					} else {
						type.getMethod("giveCurrency", Player.class, int.class).invoke(provider, player, amount);
						return true;
					}
				}
			} catch (Throwable var5) {
				once("FTB Library's currency could not be reached: money rewards give nothing", var5);
				return false;
			}
		}
	}

	@Nullable
	static Predicate<ItemStack> filter(String expression, Provider registries) {
		if (!expression.isBlank() && has("ftbfiltersystem")) {
			try {
				ItemResult made = new ItemParser(registries).parse(new StringReader("ftbfiltersystem:smart_filter[ftbfiltersystem:filter=" + quote(expression) + "]"));
				ItemStack filter = new ItemStack(made.item(), 1, made.components());
				Class<?> api = Class.forName("dev.ftb.mods.ftbfiltersystem.api.FTBFilterSystemAPI");
				Object instance = api.getMethod("api").invoke(null);
				Method parse = Class.forName("dev.ftb.mods.ftbfiltersystem.api.FTBFilterSystemAPI$API").getMethod("parseFilter", ItemStack.class, Provider.class);
				@SuppressWarnings("unchecked") Object parsed = parse.invoke(instance, filter, registries);
				return !(parsed instanceof Predicate) ? null : stack -> {
					Predicate<ItemStack> test = (Predicate<ItemStack>)parsed;
					try {
						return test.test(stack);
					} catch (Throwable var3x) {
						return false;
					}
				};
			} catch (Throwable var9) {
				once("FTB Filter System did not read the filter \"" + (expression.length() > 80 ? expression.substring(0, 80) + "..." : expression) + "\"", var9);
				return null;
			}
		} else {
			return null;
		}
	}

	private static String quote(String s) {
		return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
	}

	private Mods() {
	}
}
