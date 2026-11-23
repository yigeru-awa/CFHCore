package com.onehumanawa.cfhcore.content.gui.server.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.onehumanawa.cfhcore.content.gui.core.perm.Perms.Node;
import com.onehumanawa.cfhcore.content.gui.core.platform.CFHGuiPlatform;
import com.onehumanawa.cfhcore.content.gui.server.Nodes;
import com.onehumanawa.cfhcore.content.gui.server.CFHGuiServer;
import com.onehumanawa.cfhcore.content.gui.server.model.ServerRules;
import com.onehumanawa.cfhcore.content.gui.server.service.Msg;
import com.onehumanawa.cfhcore.content.gui.server.service.OpResult;
import com.onehumanawa.cfhcore.content.gui.server.service.SelfTest;
import com.onehumanawa.cfhcore.content.gui.server.service.ServerService;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.ClickEvent.Action;

public final class ServerCommands {
	private static final int HELP_PAGE = 9;

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
		Cmd.HELP.clear();
		Cmd.TAKEN.clear();
		List<LiteralArgumentBuilder<CommandSourceStack>> commands = new ArrayList<>();
		PlayerCommands.build(commands);
		StaffCommands.build(commands);
		LiteralArgumentBuilder<CommandSourceStack> root = (LiteralArgumentBuilder<CommandSourceStack>)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal(
								"cfhserver"
							)
							.executes(c -> help((CommandSourceStack)c.getSource(), false, 1)))
						.then(
							((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("help").executes(c -> help((CommandSourceStack)c.getSource(), false, 1)))
									.then(
										Commands.argument("page", IntegerArgumentType.integer(1, 99))
											.executes(c -> help((CommandSourceStack)c.getSource(), false, IntegerArgumentType.getInteger(c, "page")))
									))
								.then(
									((LiteralArgumentBuilder)Commands.literal("staff").executes(c -> help((CommandSourceStack)c.getSource(), true, 1)))
										.then(
											Commands.argument("page", IntegerArgumentType.integer(1, 99))
												.executes(c -> help((CommandSourceStack)c.getSource(), true, IntegerArgumentType.getInteger(c, "page")))
										)
								)
						))
					.then(Commands.literal("version").executes(c -> {
						String version = CFHGuiPlatform.get().modInfo("cfhcore").map(info -> info.version()).orElse("?");
						((CommandSourceStack)c.getSource()).sendSuccess(() -> Cmd.say("version", version, 1), false);
						return 1;
					})))
				.then(
					((LiteralArgumentBuilder)Commands.literal("reload").requires(Cmd.may(Nodes.ADMIN_RULES)))
						.executes(c -> Cmd.act(c, (s, a) -> s.admin().rulesReload(a), r -> Cmd.say("reloaded")))
				))
			.then(
				((LiteralArgumentBuilder)Commands.literal("selftest").requires(Cmd.may(Nodes.ADMIN_SELFTEST))).executes(c -> selfTest((CommandSourceStack)c.getSource()))
			);

		for (LiteralArgumentBuilder<CommandSourceStack> builder : commands) {
			LiteralCommandNode<CommandSourceStack> node = builder.build();
			root.then(node);
			if (dispatcher.getRoot().getChild(node.getLiteral()) == null) {
				dispatcher.getRoot().addChild(node);
			} else {
				Cmd.TAKEN.add(node.getLiteral());
			}
		}

		dispatcher.register(root);
		if (!Cmd.TAKEN.isEmpty()) {
			CFHGuiServer.LOGGER
				.info("[CFHGui Server] these words were taken by the game or another mod and are reached as /cfhserver <word>: {}", String.join(", ", Cmd.TAKEN));
		}
	}

	private static int selfTest(CommandSourceStack source) {
		ServerService s = Cmd.service();
		if (s == null) {
			source.sendFailure(Msg.tr("cfhgui_server.command.not_running"));
			return 0;
		} else {
			SelfTest.Outcome outcome = SelfTest.run(source.getServer(), source.getPlayer());
			Component line = Cmd.say("selftest", outcome.passed(), outcome.failed());
			if (outcome.failed() == 0) {
				source.sendSuccess(() -> line, true);
			} else {
				source.sendFailure(line);
			}

			return outcome.failed() == 0 ? 1 : 0;
		}
	}

	static int help(CommandSourceStack source, boolean staff, int page) {
		ServerService s = Cmd.service();
		List<String> names = new ArrayList<>();

		for (Cmd.Info info : Cmd.HELP.values()) {
			if (info.staff() == staff) {
				Node node = info.node();
				if ((node == null || Cmd.may(node).test(source)) && (s == null || !off(s, info.name()))) {
					names.add(info.name());
				}
			}
		}

		if (names.isEmpty()) {
			source.sendFailure(OpResult.fail("no_permission").message());
			return 0;
		} else {
			int pages = (names.size() + 9 - 1) / 9;
			int shown = Math.max(1, Math.min(pages, page));
			String word = staff ? "/cfhserver help staff " : "/cfhserver help ";
			source.sendSuccess(() -> Cmd.head(Cmd.say(staff ? "help_header_staff" : "help_header", shown, pages)), false);

			for (int i = (shown - 1) * 9; i < Math.min(names.size(), shown * 9); i++) {
				Component line = Cmd.help(names.get(i));
				source.sendSuccess(() -> line, false);
			}

			MutableComponent foot = Component.empty();
			if (shown > 1) {
				foot.append(button("help_previous", word + (shown - 1))).append(" ");
			}

			if (shown < pages) {
				foot.append(button("help_next", word + (shown + 1))).append(" ");
			}

			if (!staff && Cmd.may(Nodes.ADMIN_MENU).test(source)) {
				foot.append(button("help_staff", "/cfhserver help staff"));
			}

			if (!foot.getSiblings().isEmpty()) {
				source.sendSuccess(() -> foot, false);
			}

			return names.size();
		}
	}

	private static MutableComponent button(String key, String command) {
		return Component.literal("[")
			.append(Cmd.say(key))
			.append("]")
			.withStyle(
				style -> style.withColor(5621992)
						.withClickEvent(new ClickEvent(Action.RUN_COMMAND, command))
						.withHoverEvent(new HoverEvent(net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT, Component.literal(command)))
			);
	}

	private static boolean off(ServerService s, String name) {
		ServerRules rules = s.rules();

		return switch (name) {
			case "home", "sethome", "delhome", "homes" -> !rules.homes;
			case "warp", "warps", "setwarp", "delwarp" -> !rules.warps;
			case "spawn", "setspawn" -> !rules.spawn;
			case "back" -> !rules.back;
			case "tpa", "tpahere", "tpaccept", "tpdeny", "tptoggle" -> !rules.requests;
			case "rtp" -> !rules.randomTeleport;
			case "afk" -> !rules.afk;
			case "mail" -> !rules.mail;
			case "nick" -> !rules.nicknames;
			case "kit", "kits" -> !rules.kits;
			case "top" -> !rules.rankings;
			case "staffchat" -> !rules.staffChat;
			default -> false;
		};
	}

	private ServerCommands() {
	}
}
