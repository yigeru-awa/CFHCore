package com.onehumanawa.cfhcore.content.gui.quests.server.adv;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.onehumanawa.cfhcore.content.gui.quests.server.BookDoor;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;

public final class AdvCommands {
	private static final Map<String, String> ENGLISH = Map.of(
		"disabled",
		"That is not available here.",
		"no_permission",
		"Only operators may change the quest book.",
		"invalid",
		"The server could not read that.",
		"write_failed",
		"The chapter could not be written to the book's folder.",
		"no_tree",
		"No advancement tree is named %s. \"/cfhquests convert list\" lists them.",
		"nothing_to_undo",
		"There is no conversion to take back.",
		"edited_since",
		"The chapter %s was changed since it was converted: it is not taken back.",
		"bad_id",
		"%s cannot be an id: lower case letters, digits and _ - . only.",
		"id_twice",
		"Two trees would become the chapter %s."
	);
	private static final SuggestionProvider<CommandSourceStack> WHAT = (c, b) -> {
		String typed = b.getRemaining();
		int space = typed.lastIndexOf(32);
		if (space >= 0) {
			String head = typed.substring(0, space + 1);
			List<String> words = new ArrayList<>();

			for (String w : AdvOptions.WORDS) {
				words.add(head + w);
			}

			words.add(head + "id=");
			return SharedSuggestionProvider.suggest(words, b);
		} else {
			Set<String> names = new LinkedHashSet<>();
			MinecraftServer server = ((CommandSourceStack)c.getSource()).getServer();

			for (AdvancementNode root : AdvTrees.roots(server)) {
				names.add(root.holder().id().getNamespace());
			}

			for (AdvancementNode root : AdvTrees.roots(server)) {
				names.add(root.holder().id().toString());
			}

			names.add("*");
			return SharedSuggestionProvider.suggest(names, b);
		}
	};
	private static final Set<String> OWN = Set.of("no_tree", "nothing_to_undo", "edited_since", "bad_id", "id_twice");

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(
			(LiteralArgumentBuilder)Commands.literal("cfhquests")
				.then(
					((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("convert")
										.requires(BookDoor::mayEdit))
									.then(
										Commands.literal("advancements")
											.then(
												Commands.argument("what", StringArgumentType.greedyString())
													.suggests(WHAT)
													.executes(c -> convert((CommandSourceStack)c.getSource(), StringArgumentType.getString(c, "what")))
											)
									))
								.then(Commands.literal("list").executes(c -> list((CommandSourceStack)c.getSource()))))
							.then(Commands.literal("undo").executes(c -> undo((CommandSourceStack)c.getSource()))))
						.then(Commands.literal("selftest").executes(c -> selftest((CommandSourceStack)c.getSource())))
				)
		);
	}

	private static MutableComponent msg(String key, String english, Object... args) {
		return Component.translatableWithFallback("cfhgui_quests.adv.command." + key, english, args);
	}

	private static MutableComponent result(AdvService.Result r) {
		String key = (OWN.contains(r.code()) ? "cfhgui_quests.adv.result." : "cfhgui_quests.result.") + r.code();
		return Component.translatableWithFallback(key, ENGLISH.getOrDefault(r.code(), r.code()), r.args().toArray());
	}

	private static int fail(CommandSourceStack source, AdvService.Result r) {
		source.sendFailure(result(r));
		return 0;
	}

	private static int convert(CommandSourceStack source, String what) {
		if (!BookDoor.running()) {
			return fail(source, AdvService.Result.fail("disabled"));
		} else {
			String[] words = what.trim().split("\\s+");
			AdvOptions options = new AdvOptions();
			String id = "";

			for (int i = 1; i < words.length; i++) {
				if (words[i].startsWith("id=")) {
					id = words[i].substring(3);
				} else if (!options.word(words[i])) {
					source.sendFailure(
						msg(
							"word",
							"\"%s\" is not one of the choices. They are: tasks=translate|advancement, order=tree|free, kind=quest|advancement, rewards=true, names=words, spacing=<1 to 3>, group=<id>, exists=replace|merge|copy, id=<chapter id>.",
							words[i]
						)
					);
					return 0;
				}
			}

			MinecraftServer server = source.getServer();
			List<AdvancementNode> roots = AdvTrees.find(server, words[0]);
			if (roots.isEmpty()) {
				return fail(source, AdvService.Result.fail("no_tree", words[0]));
			} else {
				if (!id.isEmpty()) {
					if (roots.size() != 1) {
						source.sendFailure(msg("one_id", "id= names the chapter of one tree, and \"%s\" is %s trees.", words[0], roots.size()));
						return 0;
					}

					options.names.put(roots.get(0).holder().id().toString(), new AdvOptions.Name(id, ""));
				}

				List<String> names = new ArrayList<>();

				for (AdvancementNode root : roots) {
					names.add(root.holder().id().toString());
				}

				AdvService.Result r = AdvService.convert(server, names, options);
				if (r.ok() && r.data() instanceof AdvService.Done done) {
					for (AdvAccount a : done.accounts) {
						source.sendSuccess(() -> account(a), true);
					}

					source.sendSuccess(
						() -> msg("done", "Converted: %s chapter(s), %s quests. \"/cfhquests convert undo\" takes it back.", r.args().get(0), r.args().get(1))
								.withStyle(ChatFormatting.GREEN),
						true
					);
					return done.accounts.size();
				} else {
					return fail(source, r);
				}
			}
		}
	}

	static MutableComponent account(AdvAccount a) {
		MutableComponent line = msg(
			"account",
			"%s is the chapter %s (%s): %s quests, %s with tasks of their own, %s partly, %s kept the advancement.",
			a.root,
			a.chapter,
			msg("mode." + a.mode, a.mode),
			a.quests,
			a.translated,
			a.partly,
			a.kept
		);
		if (!a.triggers.isEmpty()) {
			line.append(" ").append(msg("account.triggers", "No task says: %s.", a.triggersInWords(6)));
		}

		if ("merged".equals(a.mode)) {
			line.append(" ")
				.append(
					msg(
						"account.merged",
						"Merged: %s quests added, %s fields brought up, %s kept as edited, %s quests gone. %s",
						a.added,
						a.updated,
						a.edited,
						a.removed,
						String.join(", ", a.changes)
					)
				);
		}

		return line;
	}

	private static int list(CommandSourceStack source) {
		if (!BookDoor.running()) {
			return fail(source, AdvService.Result.fail("disabled"));
		} else {
			AdvService.Trees trees = AdvService.trees(source.getServer());
			if (trees.trees.isEmpty()) {
				source.sendFailure(msg("list.none", "This game has no advancement trees."));
				return 0;
			} else {
				for (AdvTrees.Tree t : trees.trees) {
					MutableComponent line = msg("list.tree", "%s (%s): %s advancements", t.root, t.mod, t.count);
					if (!t.converted.isEmpty()) {
						line.append(" ").append(msg("list.converted", "- in the book as %s", t.converted).withStyle(ChatFormatting.GRAY));
					}

					source.sendSuccess(() -> line, false);
				}

				return trees.trees.size();
			}
		}
	}

	private static int undo(CommandSourceStack source) {
		if (!BookDoor.running()) {
			return fail(source, AdvService.Result.fail("disabled"));
		} else {
			AdvService.Result r = AdvService.undo(source.getServer());
			if (!r.ok()) {
				return fail(source, r);
			} else {
				source.sendSuccess(() -> msg("undone", "The last conversion was taken back (%s chapter(s)).", r.args().get(0)), true);
				return 1;
			}
		}
	}

	private static int selftest(CommandSourceStack source) {
		if (!BookDoor.running()) {
			return fail(source, AdvService.Result.fail("disabled"));
		} else {
			AdvSelfTest.Report report = AdvSelfTest.run(source.getServer(), source.getPlayer());

			for (String failure : report.failures()) {
				source.sendFailure(Component.literal(failure));
			}

			MutableComponent line = msg("selftest", "Converter self-test: %s passed, %s failed.", report.passed(), report.failures().size());
			source.sendSuccess(() -> line.withStyle(report.failures().isEmpty() ? ChatFormatting.GREEN : ChatFormatting.RED), false);
			return report.failures().isEmpty() ? 1 : 0;
		}
	}

	private AdvCommands() {
	}
}
