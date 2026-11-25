package com.onehumanawa.cfhcore.content.gui.core.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.onehumanawa.cfhcore.content.gui.core.perm.Perms;
import com.onehumanawa.cfhcore.content.gui.quests.server.QuestCommands;
import com.onehumanawa.cfhcore.content.gui.shops.command.ShopCommands;
import com.onehumanawa.cfhcore.content.gui.server.command.ServerCommands;
import com.onehumanawa.cfhcore.content.gui.teams.server.TeamCommands;
import com.onehumanawa.cfhcore.content.gui.veinminer.server.VeinCommands;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.Optional;

public final class CoreCommands {
	private CoreCommands() {}

	public static void init(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
		LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("cfhcore");

		// /cfhcore gui - open recipe editor
		root.then(Commands.literal("gui")
			.executes(c -> {
				c.getSource().sendSuccess(() -> Component.literal("Use /cfhcore gui open"), false);
				return 0;
			})
			.then(Commands.literal("open").executes(c -> {
				// Client-side only, server does nothing
				return 0;
			}))
		);

		// /cfhcore quests
		QuestCommands.buildSubcommand(context).ifPresent(root::then);

		// /cfhcore teams
		TeamCommands.buildSubcommand(context).ifPresent(root::then);

		// /cfhcore shops
		ShopCommands.buildSubcommand(context).ifPresent(root::then);

		// /cfhcore veinminer
		VeinCommands.buildSubcommand(context).ifPresent(root::then);

		// /cfhcore server
		ServerCommands.buildSubcommand(context).ifPresent(root::then);

		// /cfhcore version
		root.then(Commands.literal("version").executes(c -> {
			String version = com.onehumanawa.cfhcore.content.gui.core.platform.CFHGuiPlatform.get()
				.modInfo("cfhcore")
				.map(info -> info.version())
				.orElse("?");
			c.getSource().sendSuccess(() -> Component.literal("CFHCore " + version), false);
			return 1;
		}));

		// /cfhcore help
		root.then(Commands.literal("help").executes(c -> {
			c.getSource().sendSuccess(() -> Component.literal("§6/cfhcore gui§r - Open recipe editor"), false);
			c.getSource().sendSuccess(() -> Component.literal("§6/cfhcore quests§r - Quest management"), false);
			c.getSource().sendSuccess(() -> Component.literal("§6/cfhcore teams§r - Team management"), false);
			c.getSource().sendSuccess(() -> Component.literal("§6/cfhcore shops§r - Shop management"), false);
			c.getSource().sendSuccess(() -> Component.literal("§6/cfhcore veinminer§r - Veinminer settings"), false);
			c.getSource().sendSuccess(() -> Component.literal("§6/cfhcore server§r - Server admin"), false);
			c.getSource().sendSuccess(() -> Component.literal("§6/cfhcore version§r - Show version"), false);
			return 1;
		}));

		dispatcher.register(root);
	}
}