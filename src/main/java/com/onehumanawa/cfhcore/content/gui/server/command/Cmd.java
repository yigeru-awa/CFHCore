package com.onehumanawa.cfhcore.content.gui.server.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.onehumanawa.cfhcore.content.gui.core.perm.Perms;
import com.onehumanawa.cfhcore.content.gui.core.perm.Perms.Node;
import com.onehumanawa.cfhcore.content.gui.server.model.Home;
import com.onehumanawa.cfhcore.content.gui.server.model.Kit;
import com.onehumanawa.cfhcore.content.gui.server.model.OnlinePlayer;
import com.onehumanawa.cfhcore.content.gui.server.model.Place;
import com.onehumanawa.cfhcore.content.gui.server.model.Role;
import com.onehumanawa.cfhcore.content.gui.server.model.SelfState;
import com.onehumanawa.cfhcore.content.gui.server.model.Warp;
import com.onehumanawa.cfhcore.content.gui.server.service.Actor;
import com.onehumanawa.cfhcore.content.gui.server.service.Msg;
import com.onehumanawa.cfhcore.content.gui.server.service.OpResult;
import com.onehumanawa.cfhcore.content.gui.server.service.ServerRuntime;
import com.onehumanawa.cfhcore.content.gui.server.service.ServerService;
import com.onehumanawa.cfhcore.content.gui.server.service.Text;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.ClickEvent.Action;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

final class Cmd {
	static final int ACCENT = 5621992;
	static final int HEAD = 14722136;
	static final int DIM = 11184810;
	static final int GOOD = 6074742;
	static final int BAD = 15026253;
	static final Map<String, Cmd.Info> HELP = new LinkedHashMap<>();
	static final List<String> TAKEN = new ArrayList<>();
	static final SuggestionProvider<CommandSourceStack> ONLINE = names(c -> {
		ServerService s = service();
		List<String> out = new ArrayList<>();
		if (s != null) {
			for (OnlinePlayer p : s.online(actor((CommandSourceStack)c.getSource()))) {
				out.add(p.name);
			}
		}

		return out;
	});
	static final SuggestionProvider<CommandSourceStack> KNOWN = (c, b) -> {
		ServerService s = service();
		if (s == null) {
			return b.buildFuture();
		} else {
			try {
				return SharedSuggestionProvider.suggest(s.knownNames(actor((CommandSourceStack)c.getSource()), b.getRemaining(), 20), b);
			} catch (RuntimeException var4) {
				return b.buildFuture();
			}
		}
	};
	static final SuggestionProvider<CommandSourceStack> HOMES = names(c -> {
		ServerService s = service();
		List<String> out = new ArrayList<>();
		SelfState self = s == null ? null : s.self(actor((CommandSourceStack)c.getSource()));
		if (self != null) {
			for (Home h : self.homes) {
				out.add(h.name);
			}
		}

		return out;
	});
	static final SuggestionProvider<CommandSourceStack> WARPS = names(c -> {
		ServerService s = service();
		List<String> out = new ArrayList<>();
		if (s != null) {
			for (Warp w : s.warps(actor((CommandSourceStack)c.getSource())).warps) {
				out.add(w.name);
			}
		}

		return out;
	});
	static final SuggestionProvider<CommandSourceStack> KITS = names(c -> {
		ServerService s = service();
		List<String> out = new ArrayList<>();
		SelfState self = s == null ? null : s.self(actor((CommandSourceStack)c.getSource()));
		if (self != null) {
			for (Kit.View k : self.kits) {
				out.add(k.id);
			}
		}

		return out;
	});
	static final SuggestionProvider<CommandSourceStack> ALL_KITS = names(c -> {
		ServerService s = service();
		List<String> out = new ArrayList<>();
		if (s != null) {
			for (Kit k : s.kits().kits) {
				out.add(k.id);
			}
		}

		return out;
	});
	static final SuggestionProvider<CommandSourceStack> ROLES = names(c -> {
		ServerService s = service();
		List<String> out = new ArrayList<>();
		if (s != null) {
			for (Role r : s.roles().roles) {
				out.add(r.id);
			}
		}

		return out;
	});
	static final SuggestionProvider<CommandSourceStack> ASKERS = names(c -> {
		ServerService s = service();
		return s == null ? List.of() : s.askers(actor((CommandSourceStack)c.getSource()));
	});
	static final SuggestionProvider<CommandSourceStack> TIMES = (c, b) -> SharedSuggestionProvider.suggest(new String[]{"10m", "1h", "12h", "1d", "7d", "30d"}, b);
	static final SuggestionProvider<CommandSourceStack> NODES = names(c -> {
		List<String> out = new ArrayList<>();
		out.add("*");

		for (Node node : Perms.declared()) {
			if (!node.number()) {
				out.add(node.id());
				int dot = node.id().lastIndexOf(46);
				String branch = dot < 0 ? null : node.id().substring(0, dot) + ".*";
				if (branch != null && !out.contains(branch)) {
					out.add(branch);
				}
			}
		}

		return out;
	});
	static final SuggestionProvider<CommandSourceStack> NUMBERS = names(c -> {
		List<String> out = new ArrayList<>();

		for (Node node : Perms.declared()) {
			if (node.number()) {
				out.add(node.id());
			}
		}

		return out;
	});

	static LiteralArgumentBuilder<CommandSourceStack> command(String name, @Nullable Node node, boolean staff) {
		HELP.putIfAbsent(name, new Cmd.Info(name, node, staff));
		return alias(name, node);
	}

	static LiteralArgumentBuilder<CommandSourceStack> alias(String name, @Nullable Node node) {
		LiteralArgumentBuilder<CommandSourceStack> b = Commands.literal(name);
		if (node != null) {
			b.requires(may(node));
		}

		return b;
	}

	static Predicate<CommandSourceStack> may(Node node) {
		return source -> {
			Actor a = actor(source);
			ServerService s = service();
			return s != null ? s.may(a, node) : a.console() || node.defaultLevel() <= 0 || a.level() >= node.defaultLevel();
		};
	}

	static RequiredArgumentBuilder<CommandSourceStack, String> word(String name) {
		return Commands.argument(name, StringArgumentType.word());
	}

	static RequiredArgumentBuilder<CommandSourceStack, String> greedy(String name) {
		return Commands.argument(name, StringArgumentType.greedyString());
	}

	static String text(CommandContext<CommandSourceStack> c, String name) {
		return StringArgumentType.getString(c, name);
	}

	@Nullable
	static ServerService service() {
		return ServerRuntime.service();
	}

	static Actor actor(CommandSourceStack source) {
		return Actor.of(source);
	}

	static int act(CommandContext<CommandSourceStack> c, Cmd.Op op, @Nullable Function<OpResult, Component> said) throws CommandSyntaxException {
		CommandSourceStack source = (CommandSourceStack)c.getSource();
		ServerService s = service();
		if (s == null) {
			source.sendFailure(Msg.tr("cfhgui_server.command.not_running"));
			return 0;
		} else {
			OpResult r = op.run(s, actor(source));
			s.flush();
			if (!r.ok()) {
				source.sendFailure(r.message());
				return 0;
			} else {
				Component line = said == null ? null : said.apply(r);
				if (line != null) {
					source.sendSuccess(() -> line, false);
				}

				return 1;
			}
		}
	}

	static Function<OpResult, Component> journey(Function<OpResult, Component> arrived) {
		return r -> (Component)(r.code().equals("warmup") ? r.message().withStyle(style -> style.withColor(11184810)) : arrived.apply(r));
	}

	static MutableComponent say(String key, Object... args) {
		return Msg.tr("cfhgui_server.command." + key, args);
	}

	static MutableComponent help(String name) {
		String shown = TAKEN.contains(name) ? "/cfhserver " + name : "/" + name;
		return Component.literal(shown)
			.withStyle(
				style -> style.withColor(5621992)
						.withClickEvent(new ClickEvent(Action.SUGGEST_COMMAND, shown + " "))
						.withHoverEvent(new HoverEvent(net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT, Component.literal(shown)))
			)
			.append(Component.literal(" ").withStyle(ChatFormatting.RESET))
			.append(Msg.tr("cfhgui_server.help." + name).withStyle(style -> style.withColor(14540253)));
	}

	static int usage(CommandContext<CommandSourceStack> c, String name) {
		((CommandSourceStack)c.getSource()).sendFailure(help(name));
		return 0;
	}

	static MutableComponent head(MutableComponent line) {
		return line.withStyle(style -> style.withColor(14722136));
	}

	static MutableComponent dim(MutableComponent line) {
		return line.withStyle(style -> style.withColor(11184810));
	}

	static MutableComponent link(String label, String command) {
		return Component.literal(label)
			.withStyle(
				style -> style.withColor(5621992)
						.withClickEvent(new ClickEvent(Action.RUN_COMMAND, command))
						.withHoverEvent(new HoverEvent(net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT, Component.literal(command)))
			);
	}

	static MutableComponent place(Place p) {
		return Component.literal(p.blockX() + " " + p.blockY() + " " + p.blockZ() + " " + dimension(p.dimension)).withStyle(style -> style.withColor(11184810));
	}

	static String dimension(String id) {
		ResourceLocation rl = ResourceLocation.tryParse(id);
		return rl == null ? id : (rl.getNamespace().equals("minecraft") ? rl.getPath() : id);
	}

	static MutableComponent role(Role.Tag tag) {
		return Component.literal(tag.name.isEmpty() ? tag.id : tag.name).withStyle(style -> style.withColor(tag.color));
	}

	static Component length(long seconds) {
		return seconds > 0L ? Component.literal(Text.span(seconds)) : Msg.tr("cfhgui_server.command.forever");
	}

	static SuggestionProvider<CommandSourceStack> names(Function<CommandContext<CommandSourceStack>, Iterable<String>> source) {
		return (c, b) -> {
			try {
				return SharedSuggestionProvider.suggest(source.apply(c), b);
			} catch (RuntimeException var4) {
				return b.buildFuture();
			}
		};
	}

	private Cmd() {
	}

	static record Info(String name, @Nullable Node node, boolean staff) {
	}

	@FunctionalInterface
	interface Op {
		OpResult run(ServerService var1, Actor var2) throws CommandSyntaxException;
	}
}
