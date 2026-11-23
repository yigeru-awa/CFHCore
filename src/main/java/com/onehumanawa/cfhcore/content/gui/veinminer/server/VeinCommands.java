package com.onehumanawa.cfhcore.content.gui.veinminer.server;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.onehumanawa.cfhcore.content.gui.core.perm.Perms;
import com.onehumanawa.cfhcore.content.gui.veinminer.CFHGuiVeinminer;
import com.onehumanawa.cfhcore.content.gui.veinminer.VeinShape;
import com.onehumanawa.cfhcore.content.gui.veinminer.config.VeinServerConfig;
import com.onehumanawa.cfhcore.content.gui.veinminer.vein.VeinRules;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

public final class VeinCommands {
	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		LiteralCommandNode<CommandSourceStack> root = dispatcher.register(tree("cfhveinminer"));
		if (dispatcher.getRoot().getChild("veinminer") == null) {
			dispatcher.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("veinminer").executes(VeinCommands::status)).redirect(root));
		}
	}

	private static boolean admin(CommandSourceStack s) {
		return Perms.has(s, "cfhgui_veinminer.admin", 2);
	}

	private static LiteralArgumentBuilder<CommandSourceStack> tree(String word) {
		return (LiteralArgumentBuilder<CommandSourceStack>)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal(word)
						.executes(VeinCommands::status))
					.then(Commands.literal("status").executes(VeinCommands::status)))
				.then(
					((LiteralArgumentBuilder)Commands.literal("reload").requires(VeinCommands::admin))
						.executes(
							c -> {
								VeinServer.reload();
								((CommandSourceStack)c.getSource())
									.sendSuccess(
										() -> say("cfhgui_veinminer.cmd.reloaded", "CFHGui Veinminer: rules read again and sent to the players.").withStyle(ChatFormatting.GREEN), true
									);
								return 1;
							}
						)
				))
			.then(((LiteralArgumentBuilder)Commands.literal("selftest").requires(VeinCommands::admin)).executes(c -> {
				SelfTest.Outcome o = SelfTest.run((CommandSourceStack)c.getSource());
				return o.failed() == 0 ? 1 : 0;
			}));
	}

	private static int status(CommandContext<CommandSourceStack> c) {
		CommandSourceStack s = (CommandSourceStack)c.getSource();
		VeinServerConfig cfg = VeinServerConfig.get();
		VeinRules rules = VeinRules.local();
		MutableComponent head = Component.literal("CFHGui Veinminer ")
			.withStyle(ChatFormatting.GOLD)
			.append(Component.literal(CFHGuiVeinminer.version()).withStyle(ChatFormatting.GRAY));
		s.sendSuccess(() -> head, false);
		if (!cfg.enabled) {
			s.sendSuccess(() -> say("cfhgui_veinminer.cmd.off", "switched off on this server").withStyle(ChatFormatting.GRAY), false);
			return 0;
		} else {
			ServerPlayer p = s.getPlayer();
			if (p != null) {
				boolean may = VeinServer.allowed(p, rules);
				s.sendSuccess(
					() -> (may
								? say("cfhgui_veinminer.cmd.may", "You may veinmine: hold the Veinminer key (grave by default) and mine.")
								: say("cfhgui_veinminer.cmd.may_not", "You may not veinmine on this server."))
							.withStyle(may ? ChatFormatting.GREEN : ChatFormatting.RED),
					false
				);
			}

			StringBuilder shapes = new StringBuilder();

			for (VeinShape shape : VeinShape.all()) {
				if (rules.shapeEnabled(shape)) {
					if (shapes.length() > 0) {
						shapes.append(", ");
					}

					shapes.append(shape.id());
				}
			}

			s.sendSuccess(
				() -> Component.translatableWithFallback(
							"cfhgui_veinminer.cmd.limits", "Up to %s blocks, %s a tick; shapes: %s", new Object[]{cfg.maxBlocks, cfg.blocksPerTick, shapes.toString()}
						)
						.withStyle(ChatFormatting.GRAY),
				false
			);
			s.sendSuccess(
				() -> Component.translatableWithFallback(
							"cfhgui_veinminer.cmd.costs",
							"Needs a tool that harvests: %s; hunger per block: %s; stops before the tool breaks: %s; drops: %s",
							new Object[]{yesNo(cfg.requireHarvestTool), cfg.exhaustionPerBlock, yesNo(cfg.stopBeforeToolBreaks), cfg.drops().name().toLowerCase(Locale.ROOT)}
						)
						.withStyle(ChatFormatting.GRAY),
				false
			);
			return 1;
		}
	}

	private static String yesNo(boolean b) {
		return b ? "yes" : "no";
	}

	static MutableComponent say(String key, String fallback) {
		return Component.translatableWithFallback(key, fallback);
	}

	private VeinCommands() {
	}
}
