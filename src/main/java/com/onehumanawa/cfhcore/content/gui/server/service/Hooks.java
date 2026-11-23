package com.onehumanawa.cfhcore.content.gui.server.service;

import com.mojang.authlib.GameProfile;
import com.onehumanawa.cfhcore.content.gui.server.Nodes;
import com.onehumanawa.cfhcore.content.gui.server.CFHGuiServer;
import com.onehumanawa.cfhcore.content.gui.server.mixin.server.CommandSourceStackAccessor;
import com.onehumanawa.cfhcore.content.gui.server.model.PlayerRecord;
import com.onehumanawa.cfhcore.content.gui.server.model.Role;
import com.onehumanawa.cfhcore.content.gui.server.model.ServerRules;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Entry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.PlayerTeam;
import org.jetbrains.annotations.Nullable;

public final class Hooks {
	@Nullable
	private static ServerPlayer joining;
	private static boolean complained;

	@Nullable
	private static ServerService service() {
		return ServerRuntime.service();
	}

	private static void complain(String what, RuntimeException e) {
		if (!complained) {
			complained = true;
			CFHGuiServer.LOGGER.error("[CFHGui Server] {} failed; the game's own behaviour stands (said once)", what, e);
		}
	}

	public static Component nameForTeam(Player player, Component name) {
		if (player instanceof ServerPlayer p) {
			try {
				ServerService s = service();
				if (s != null && s.rules().nicknames && s.server.isSameThread()) {
					PlayerRecord rec = record(s, p.getUUID());
					return (Component)(rec != null && !rec.nick.isEmpty() ? Text.styled(rec.nick) : name);
				} else {
					return name;
				}
			} catch (RuntimeException var5) {
				complain("a nickname", var5);
				return name;
			}
		} else {
			return name;
		}
	}

	@Nullable
	public static Component displayName(Player player, Component made) {
		if (player instanceof ServerPlayer p) {
			try {
				ServerService s = service();
				if (s != null && s.rules().prefixInChat && s.server.isSameThread()) {
					MutableComponent prefix = prefix(s, p);
					return prefix == null ? null : prefix.append(made);
				} else {
					return null;
				}
			} catch (RuntimeException var5) {
				complain("a role's prefix", var5);
				return null;
			}
		} else {
			return null;
		}
	}

	@Nullable
	public static Component tabName(ServerPlayer player) {
		try {
			ServerService s = service();
			if (s != null && s.server.isSameThread()) {
				ServerRules rules = s.rules();
				Session session = s.session(player.getUUID());
				PlayerRecord rec = session != null ? session.record : record(s, player.getUUID());
				boolean afk = session != null && session.afk;
				boolean nick = rec != null && !rec.nick.isEmpty() && rules.nicknames;
				MutableComponent prefix = rules.prefixInTabList ? prefix(s, player) : null;
				if (!afk && !nick && prefix == null) {
					return null;
				} else {
					MutableComponent name = PlayerTeam.formatNameForTeam(player.getTeam(), (Component)(nick ? Text.styled(rec.nick) : player.getName()));
					MutableComponent line = prefix == null ? name : prefix.append(name);
					return !afk
						? line
						: Msg.tr("cfhgui_server.tab.afk", Component.literal(line.getString())).withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC});
				}
			} else {
				return null;
			}
		} catch (RuntimeException var10) {
			complain("a tab list name", var10);
			return null;
		}
	}

	@Nullable
	private static MutableComponent prefix(ServerService s, ServerPlayer player) {
		for (Role role : s.perms.held(player.getUUID(), player.server.getProfilePermissions(player.getGameProfile()))) {
			if (!role.prefix.isEmpty()) {
				return prefix(role);
			}
		}

		return null;
	}

	static MutableComponent prefix(Role role) {
		String text = role.prefix.trim();
		boolean plain = !text.isEmpty() && Character.isLetterOrDigit(text.codePointAt(0));
		return Component.empty().append(Component.literal(plain ? "[" + text + "]" : text).withStyle(style -> style.withColor(role.color))).append(" ");
	}

	@Nullable
	private static PlayerRecord record(ServerService s, UUID id) {
		Session session = s.session(id);
		if (session != null) {
			return session.record;
		} else {
			return s.store.known(id) == null ? null : s.store.record(id);
		}
	}

	public static List<ServerPlayer> sleepers(List<ServerPlayer> players) {
		try {
			ServerService s = service();
			if (s != null && !players.isEmpty()) {
				boolean skipIdle = s.rules().afk && s.rules().afkSkipsSleep;
				List<ServerPlayer> out = null;

				for (int i = 0; i < players.size(); i++) {
					ServerPlayer p = players.get(i);
					Session session = s.session(p.getUUID());
					boolean skip = session != null && !p.isSleeping() && (skipIdle && session.afk || session.record.vanished);
					if (skip && out == null) {
						out = new ArrayList<>(players.subList(0, i));
					} else if (!skip && out != null) {
						out.add(p);
					}
				}

				return out == null ? players : out;
			} else {
				return players;
			}
		} catch (RuntimeException var8) {
			complain("counting sleepers", var8);
			return players;
		}
	}

	public static boolean held(ServerPlayer player) {
		try {
			ServerService s = service();
			Session session = s == null ? null : s.session(player.getUUID());
			if (session != null && s.moderation.frozen(session)) {
				s.moderation.tellHeld(session);
				return true;
			} else {
				return false;
			}
		} catch (RuntimeException var3) {
			complain("a freeze", var3);
			return false;
		}
	}

	public static void moved(ServerPlayer player) {
		try {
			ServerService s = service();
			Session session = s == null ? null : s.session(player.getUUID());
			if (session != null && session.player == player) {
				s.moderation.moved(session, session.place());
			}
		} catch (RuntimeException var3) {
			complain("a freeze", var3);
		}
	}

	public static boolean commandRefused(CommandSourceStack source, String command) {
		try {
			ServerService s = service();
			if (s != null && source.getEntity() instanceof ServerPlayer p && ((CommandSourceStackAccessor)source).cfhServer$source() == p) {
				Session session = s.session(p.getUUID());
				return session != null && s.moderation.commandRefused(session, command);
			} else {
				return false;
			}
		} catch (RuntimeException var5) {
			complain("a command's check", var5);
			return false;
		}
	}

	public static void beforeTick(ServerPlayer player) {
		try {
			ServerService s = service();
			Session session = s == null ? null : s.session(player.getUUID());
			if (session != null && session.player == player) {
				s.moderation.steady(session);
			}
		} catch (RuntimeException var3) {
			complain("holding hunger and thirst", var3);
		}
	}

	@Nullable
	public static Component loginRefusal(GameProfile profile) {
		try {
			ServerService s = service();
			return s == null ? null : s.moderation.loginRefusal(profile);
		} catch (RuntimeException var2) {
			complain("the maintenance check", var2);
			return null;
		}
	}

	public static void worldSaved() {
		try {
			ServerService s = service();
			if (s != null && s.live && s.server.isSameThread()) {
				s.worldSaved();
			}
		} catch (RuntimeException var1) {
			complain("saving with the world", var1);
		}
	}

	public static void joining(@Nullable ServerPlayer player) {
		joining = player;
	}

	public static boolean hiddenFrom(ServerPlayer subject, ServerPlayer viewer) {
		try {
			ServerService s = service();
			return s != null && s.vanish.hiddenFrom(subject, viewer);
		} catch (RuntimeException var3) {
			complain("vanish", var3);
			return false;
		}
	}

	public static boolean hideBroadcast(PlayerList list, Component message) {
		try {
			ServerService s = service();
			if (s != null && message.getContents() instanceof TranslatableContents line) {
				String key = line.getKey();
				ServerPlayer subject;
				if (key.startsWith("multiplayer.player.joined")) {
					subject = joining;
				} else {
					if (!key.equals("multiplayer.player.left")) {
						return false;
					}

					subject = leaving(s, line);
				}

				if (subject != null && s.vanish.is(subject.getUUID())) {
					for (ServerPlayer viewer : list.getPlayers()) {
						if (viewer != subject && !s.vanish.hiddenFrom(subject, viewer)) {
							viewer.sendSystemMessage(message);
						}
					}

					return true;
				} else {
					return false;
				}
			} else {
				return false;
			}
		} catch (RuntimeException var8) {
			complain("vanish", var8);
			return false;
		}
	}

	@Nullable
	private static ServerPlayer leaving(ServerService s, TranslatableContents line) {
		if (line.getArgs().length != 0 && line.getArgs()[0] instanceof Component who) {
			String var7 = who.getString();

			for (Session session : s.online()) {
				ServerPlayer p = session.player;
				if (p != null && session.record.vanished && p.getDisplayName().getString().equals(var7)) {
					return p;
				}
			}

			return null;
		} else {
			return null;
		}
	}

	public static boolean hideTabEntry(PlayerList list, Packet<?> packet) {
		try {
			ServerService s = service();
			if (s != null && packet instanceof ClientboundPlayerInfoUpdatePacket info) {
				List<UUID> hidden = null;

				for (Entry entry : info.entries()) {
					if (s.vanish.is(entry.profileId())) {
						if (hidden == null) {
							hidden = new ArrayList<>(1);
						}

						hidden.add(entry.profileId());
					}
				}

				if (hidden == null) {
					return false;
				} else {
					ServerPlayer coming = joining;

					for (ServerPlayer viewer : list.getPlayers()) {
						if (s.may(Actor.of(viewer), Nodes.MOD_SEE_VANISHED)) {
							viewer.connection.send(packet);
						} else {
							List<ServerPlayer> shown = new ArrayList<>(info.entries().size());

							for (Entry entryx : info.entries()) {
								UUID id = entryx.profileId();
								if (!hidden.contains(id) || id.equals(viewer.getUUID())) {
									ServerPlayer p = coming != null && coming.getUUID().equals(id) ? coming : list.getPlayer(id);
									if (p != null) {
										shown.add(p);
									}
								}
							}

							if (!shown.isEmpty()) {
								viewer.connection.send(new ClientboundPlayerInfoUpdatePacket(info.actions(), shown));
							}
						}
					}

					return true;
				}
			} else {
				return false;
			}
		} catch (RuntimeException var13) {
			complain("vanish", var13);
			return false;
		}
	}

	public static List<ServerPlayer> shown(List<ServerPlayer> players, @Nullable ServerPlayer viewer) {
		try {
			ServerService s = service();
			return s == null ? players : s.vanish.shown(players, viewer);
		} catch (RuntimeException var3) {
			complain("vanish", var3);
			return players;
		}
	}

	public static List<String> shownNames(CommandSourceStack source, List<String> names) {
		try {
			ServerService s = service();
			if (s != null && !names.isEmpty()) {
				ServerPlayer viewer = source.getEntity() instanceof ServerPlayer p ? p : null;
				if (viewer == null) {
					return names;
				} else {
					List<String> out = null;

					for (Session session : s.online()) {
						if (session.record.vanished && session.player != null && s.vanish.hiddenFrom(session.player, viewer)) {
							if (out == null) {
								out = new ArrayList<>(names);
							}

							out.remove(session.name);
						}
					}

					return out == null ? names : out;
				}
			} else {
				return names;
			}
		} catch (RuntimeException var7) {
			complain("vanish", var7);
			return names;
		}
	}

	private Hooks() {
	}
}
