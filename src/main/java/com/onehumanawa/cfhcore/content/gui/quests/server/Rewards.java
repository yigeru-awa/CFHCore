package com.onehumanawa.cfhcore.content.gui.quests.server;

import com.onehumanawa.cfhcore.content.gui.quests.QuestsServerConfig;
import com.onehumanawa.cfhcore.content.gui.quests.CFHGuiQuests;
import com.onehumanawa.cfhcore.content.gui.quests.api.QuestsApi;
import com.onehumanawa.cfhcore.content.gui.quests.model.Reward;
import com.onehumanawa.cfhcore.content.gui.quests.net.QuestsNet;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.LootParams.Builder;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

final class Rewards {
	private static final int MAX_STACKS = 100;

	static void give(MinecraftServer server, ServerPlayer player, Reward reward, String teamName) {
		give(server, player, reward, new Rewards.About(teamName, "", 1L, 1L, ""));
	}

	static void give(MinecraftServer server, ServerPlayer player, Reward reward, Rewards.About about) {
		String teamName = about.team();
		String var5 = reward.type;
		switch (var5) {
			case "toast":
				String title = reward.title;
				String text = reward.text;
				if (QuestsNet.has(player)) {
					QuestsNet.send(player, new QuestsNet.Notice(cut(title, 256), cut(text, 1024), cut(reward.icon, 512)));
				} else if (!title.isBlank() || !text.isBlank()) {
					MutableComponent line = ServerText.text(title);
					if (!title.isBlank() && !text.isBlank()) {
						line.append(": ");
					}

					player.sendSystemMessage(line.append(ServerText.text(text)));
				}
				break;
			case "stage":
				Mods.stage(player, reward.value, reward.remove);
				break;
			case "currency":
				if (!Mods.currency(player, reward.count)) {
					player.sendSystemMessage(
						ServerText.msg("cfhgui_quests.reward.currency.none", "This reward is %s coins, but no mod on this server keeps money.", reward.count)
					);
					CFHGuiQuests.LOGGER
						.warn(
							"[CFHGui Quests] a reward of {} coins for {} was not handed out: no mod here keeps money (FTB Library with a currency)",
							reward.count,
							player.getGameProfile().getName()
						);
				}
				break;
			case "custom":
				String name = reward.value;
				String quest = about.quest();
				QuestsApi.REWARD.invoke(l -> l.reward(player, name, quest));
				break;
			case "item":
				ItemStack one = Specs.stack(reward.item, server.registryAccess(), 1);
				if (one.isEmpty()) {
					CFHGuiQuests.LOGGER.warn("[CFHGui Quests] the reward \"{}\" names no item and was not handed out", reward.item);
					return;
				}

				items(player, one, reward.count);
				break;
			case "xp":
				if (reward.levels) {
					player.giveExperienceLevels(reward.xp);
				} else {
					player.giveExperiencePoints(reward.xp);
				}
				break;
			case "command":
				command(server, player, reward.command, about, reward.level);
				if (!reward.text.isBlank()) {
					player.sendSystemMessage(ServerText.text(reward.text));
				}
				break;
			case "loot":
				loot(server, player, reward.loot);
				break;
			case "advancement":
				advancement(server, player, reward.advancement, reward.criterion);
				break;
			default:
				CFHGuiQuests.LOGGER.warn("[CFHGui Quests] a reward of the unknown type \"{}\" was not handed out", reward.type);
		}
	}

	private static void items(ServerPlayer player, ItemStack one, int count) {
		if (!one.isEmpty()) {
			int max = Math.max(1, one.getMaxStackSize());
			int left = Math.min(Math.max(1, count), max * 100);

			while (left > 0) {
				int n = Math.min(left, max);
				left -= n;
				stack(player, one.copyWithCount(n));
			}
		}
	}

	static void stack(ServerPlayer player, ItemStack stack) {
		if (!stack.isEmpty()) {
			boolean added = player.getInventory().add(stack);
			if (added && stack.isEmpty()) {
				player.level()
					.playSound(
						null,
						player.getX(),
						player.getY(),
						player.getZ(),
						SoundEvents.ITEM_PICKUP,
						SoundSource.PLAYERS,
						0.2F,
						((player.getRandom().nextFloat() - player.getRandom().nextFloat()) * 0.7F + 1.0F) * 2.0F
					);
				player.containerMenu.broadcastChanges();
			} else {
				ItemEntity dropped = player.drop(stack, false);
				if (dropped != null) {
					dropped.setNoPickUpDelay();
					dropped.setTarget(player.getUUID());
				}
			}
		}
	}

	private static void command(MinecraftServer server, ServerPlayer player, String raw, Rewards.About about, int asked) {
		String command = raw.trim();
		if (command.startsWith("/")) {
			command = command.substring(1);
		}

		if (!command.isEmpty()) {
			String name = player.getGameProfile().getName();
			command = command.replace("{player}", name)
				.replace("{team}", about.team().isBlank() ? name : about.team())
				.replace("{team_id}", about.teamId().isBlank() ? player.getUUID().toString() : about.teamId())
				.replace("{members}", Long.toString(about.members()))
				.replace("{online}", Long.toString(about.online()))
				.replace("{x}", Integer.toString(player.blockPosition().getX()))
				.replace("{y}", Integer.toString(player.blockPosition().getY()))
				.replace("{z}", Integer.toString(player.blockPosition().getZ()))
				.replace("{quest}", about.quest());
			int level = asked > 0 ? Math.min(4, asked) : Math.max(2, Math.min(4, QuestsServerConfig.get().editorPermissionLevel));
			CommandSourceStack source = server.createCommandSourceStack()
				.withEntity(player)
				.withPosition(player.position())
				.withRotation(player.getRotationVector())
				.withLevel(player.serverLevel())
				.withPermission(level)
				.withSuppressedOutput();
			server.getCommands().performPrefixedCommand(source, command);
		}
	}

	private static void loot(MinecraftServer server, ServerPlayer player, String table) {
		ResourceLocation id = ResourceLocation.tryParse(table.trim());
		LootTable loot = id == null ? LootTable.EMPTY : server.reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, id));
		if (loot == LootTable.EMPTY) {
			CFHGuiQuests.LOGGER.warn("[CFHGui Quests] the loot reward \"{}\" names no loot table and gave nothing", table);
		} else {
			LootParams params = new Builder(player.serverLevel())
				.withParameter(LootContextParams.THIS_ENTITY, player)
				.withParameter(LootContextParams.ORIGIN, player.position())
				.withLuck(player.getLuck())
				.create(LootContextParamSets.ADVANCEMENT_REWARD);
			ObjectListIterator var6 = loot.getRandomItems(params).iterator();

			while (var6.hasNext()) {
				ItemStack stack = (ItemStack)var6.next();
				stack(player, stack);
			}
		}
	}

	private static void advancement(MinecraftServer server, ServerPlayer player, String advancement, String only) {
		ResourceLocation id = ResourceLocation.tryParse(advancement.trim());
		AdvancementHolder holder = id == null ? null : server.getAdvancements().get(id);
		if (holder == null) {
			CFHGuiQuests.LOGGER.warn("[CFHGui Quests] the advancement reward \"{}\" names no advancement", advancement);
		} else if (!only.isBlank()) {
			player.getAdvancements().award(holder, only.trim());
		} else {
			List<String> remaining = new ArrayList<>();

			for (String criterion : player.getAdvancements().getOrStartProgress(holder).getRemainingCriteria()) {
				remaining.add(criterion);
			}

			for (String criterion : remaining) {
				player.getAdvancements().award(holder, criterion);
			}
		}
	}

	private static String cut(String s, int max) {
		return s.length() > max ? s.substring(0, max) : s;
	}

	private Rewards() {
	}

	static record About(String team, String teamId, long members, long online, String quest) {
	}
}
