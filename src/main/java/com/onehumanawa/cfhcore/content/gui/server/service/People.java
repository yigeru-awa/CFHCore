package com.onehumanawa.cfhcore.content.gui.server.service;

import com.onehumanawa.cfhcore.content.gui.core.perm.Perms.Node;
import com.onehumanawa.cfhcore.content.gui.core.stats.StatsLink;
import com.onehumanawa.cfhcore.content.gui.server.Nodes;
import com.onehumanawa.cfhcore.content.gui.server.CFHGuiServer;
import com.onehumanawa.cfhcore.content.gui.server.api.ServerApi;
import com.onehumanawa.cfhcore.content.gui.server.model.Kit;
import com.onehumanawa.cfhcore.content.gui.server.model.PlayerCard;
import com.onehumanawa.cfhcore.content.gui.server.model.PlayerRecord;
import com.onehumanawa.cfhcore.content.gui.server.model.Role;
import com.onehumanawa.cfhcore.content.gui.server.model.ServerRules;
import com.onehumanawa.cfhcore.content.gui.server.net.ServerNet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class People {
	private static final long MAIL_GAP_MS = 3000L;
	private static final long NICK_GAP_MS = 3000L;
	private static final long AFK_GAP_MS = 3000L;
	private static final long HIDE_GAP_MS = 3000L;
	static final int IDLE_STEP = 10;
	private static final String PLAY_TIME = StatsLink.key("minecraft:custom", "minecraft:play_time");
	private final ServerService s;
	private final Map<String, List<Kit.View.Entry>> contents = new HashMap<>();

	People(ServerService s) {
		this.s = s;
	}

	void joined(Session session, boolean first) {
		ServerRules rules = this.s.rules();
		String shown = Text.shown(session.record);

		for (String raw : rules.joinMessage) {
			if (raw != null && !raw.isBlank()) {
				this.s.say(session, "join_message", Text.styled(raw.replace("%player%", shown).replace("%online%", Integer.toString(this.s.sessions.size()))));
			}
		}

		if (first && rules.kits) {
			for (Kit kit : this.s.kits().kits) {
				if (kit.firstJoin && !kit.items.isEmpty() && session.player != null && this.give(session.player, kit) > 0) {
					this.s.notice(session, "kit_received", 0, Msg.tr("cfhgui_server.chat.kit_received", kit.name), kit.name);
				}
			}
		}

		int unread = session.record.unreadMail();
		if (rules.mail && unread > 0 && session.player != null && !ServerNet.has(session.player)) {
			this.s
				.say(session, "mail_unread", Msg.tr("cfhgui_server.chat.mail_unread", unread).append(" ").append(Travel.button("cfhgui_server.chat.button.read", "/mail")));
		}
	}

	void left(Session session) {
		if (session.afk) {
			session.afk = false;
			if (this.s.live && session.player != null) {
				ServerApi.AFK_CHANGED.invoke(l -> l.changed(session.player, false));
			}
		}
	}

	void tickIdle() {
		ServerRules rules = this.s.rules();
		long now = this.s.now();

		for (Session session : new ArrayList<>(this.s.online())) {
			ServerPlayer p = session.player;
			if (p != null) {
				long action = p.getLastActionTime();
				float yaw = p.getYRot();
				float pitch = p.getXRot();
				boolean active = action != session.actionSeen || yaw != session.yaw || pitch != session.pitch;
				session.actionSeen = action;
				session.yaw = yaw;
				session.pitch = pitch;
				if (session.afkGrace > 0) {
					session.afkGrace--;
					active = false;
				}

				if (active) {
					session.lastActive = now;
					if (session.afk) {
						this.setAfk(session, false, false);
					}
				} else if (!session.afk && rules.afk && now - session.lastActive >= (long)rules.afkSeconds * 1000L) {
					this.setAfk(session, true, false);
				}

				if (session.afk) {
					if (!rules.afk) {
						this.setAfk(session, false, false);
					} else {
						if (!session.record.vanished) {
							session.record.afkTicks += 10L;
						}

						if (this.s.live
							&& rules.afkKickMinutes > 0
							&& now - session.afkSince >= (long)rules.afkKickMinutes * 60000L
							&& !this.s.may(session.actor(), Nodes.AFK_EXEMPT)) {
							p.connection.disconnect(Msg.tr("cfhgui_server.kick.afk", rules.afkKickMinutes));
						}
					}
				}
			}
		}
	}

	public OpResult afk(Actor a, @Nullable Boolean on) {
		if (!this.s.rules().afk) {
			return OpResult.fail("disabled");
		} else if (!this.s.may(a, Nodes.AFK_USE)) {
			return OpResult.fail("no_permission");
		} else {
			Session session = this.s.session(a);
			if (session == null) {
				return OpResult.fail("player_only");
			} else {
				boolean afk = on == null ? !session.afk : on;
				boolean said = false;
				if (afk != session.afk) {
					if (!session.allow("afk", 3000L, this.s.now())) {
						return OpResult.fail("too_fast");
					}

					said = this.setAfk(session, afk, true);
				}

				if (afk) {
					session.afkGrace = 3;
				}

				return OpResult.ok(afk, said);
			}
		}
	}

	boolean setAfk(Session session, boolean afk, boolean manual) {
		if (session.afk == afk) {
			return false;
		} else {
			long now = this.s.now();
			session.afk = afk;
			session.afkManual = afk && manual;
			session.afkSince = now;
			if (!afk) {
				session.lastActive = now;
			}

			this.s.sync.mark(session.id, "self");
			this.s.sync.markAll("online");
			boolean said = this.s.rules().afkAnnounce && !session.record.vanished && !this.s.moderation.muted(session);
			if (said) {
				this.s
					.sayAll(
						afk ? "afk_on" : "afk_off",
						Msg.tr(afk ? "cfhgui_server.chat.afk_on" : "cfhgui_server.chat.afk_off", Text.shown(session.record)).withStyle(style -> style.withColor(11184810))
					);
			}

			ServerPlayer p = session.player;
			if (this.s.live && p != null) {
				this.s.refreshName(p);
				p.serverLevel().updateSleepingPlayerList();
				ServerApi.AFK_CHANGED.invoke(l -> l.changed(p, afk));
				return said;
			} else {
				return said;
			}
		}
	}

	public OpResult nick(Actor a, @Nullable String raw) {
		if (!this.s.rules().nicknames) {
			return OpResult.fail("disabled");
		} else if (!this.s.may(a, Nodes.NICK_USE)) {
			return OpResult.fail("no_permission");
		} else {
			Session session = this.s.session(a);
			if (session == null) {
				return OpResult.fail("player_only");
			} else {
				PlayerRecord rec = session.record;
				OpResult muted = this.s.moderation.mutedResult(session);
				if (muted != null) {
					return muted;
				} else {
					String nick = "";
					if (raw != null && !raw.isBlank()) {
						nick = Text.nick(raw, this.s.may(a, Nodes.NICK_COLOR));
						if (nick == null) {
							return OpResult.fail("nick_invalid");
						}

						String plain = Text.plainNick(nick);
						if (plain.equals(rec.name) && plain.equals(nick)) {
							nick = "";
						} else if (this.s.store.nameTaken(plain, rec.id) || !plain.equalsIgnoreCase(rec.name) && this.s.nameReserved(plain)) {
							return OpResult.fail("nick_taken");
						}
					}

					if (nick.equals(rec.nick)) {
						return OpResult.ok(Text.shown(rec));
					} else if (!session.allow("nick", 3000L, this.s.now())) {
						return OpResult.fail("too_fast");
					} else {
						rec.nick = nick;
						this.s.touch(rec);
						this.s.sync.mark(rec.id, "self");
						this.s.sync.markAll("online");
						if (session.player != null) {
							this.s.refreshName(session.player);
						}

						return OpResult.ok(Text.shown(rec));
					}
				}
			}
		}
	}

	public OpResult mailSend(Actor a, @Nullable ServerService.Target target, String wanted, String text) {
		ServerRules rules = this.s.rules();
		if (!rules.mail) {
			return OpResult.fail("disabled");
		} else if (!this.s.may(a, Nodes.MAIL_USE)) {
			return OpResult.fail("no_permission");
		} else {
			Session from = this.s.session(a);
			if (from == null && !a.console()) {
				return OpResult.fail("player_only");
			} else {
				if (from != null) {
					OpResult muted = this.s.moderation.mutedResult(from);
					if (muted != null) {
						return muted;
					}
				}

				if (target == null) {
					return OpResult.fail("no_such_player", Text.line(wanted, 32));
				} else if (target.id().equals(a.id())) {
					return OpResult.fail("self");
				} else if (!Text.fits(text, 256)) {
					return OpResult.fail("text_invalid");
				} else if (from != null && !from.allow("mail", 3000L, this.s.now())) {
					return OpResult.fail("too_fast");
				} else {
					PlayerRecord rec = target.record();

					while (rec.mail.size() >= rules.mailBoxSize) {
						PlayerRecord.Mail oldestRead = null;

						for (PlayerRecord.Mail m : rec.mail) {
							if (m.read) {
								oldestRead = m;
								break;
							}
						}

						if (oldestRead == null) {
							return OpResult.fail("mail_full", Text.shown(rec));
						}

						rec.mail.remove(oldestRead);
					}

					PlayerRecord.Mail mail = new PlayerRecord.Mail();
					mail.id = Long.toHexString(UUID.randomUUID().getLeastSignificantBits() & 1099511627775L);
					mail.from = a.console() ? Actor.NOBODY : a.id();
					mail.fromName = from == null ? a.name() : Text.shown(from.record);
					mail.text = Text.line(text, 256);
					mail.at = this.s.now();
					rec.mail.add(mail);
					this.s.touch(rec);
					Session to = target.session();
					if (to != null) {
						this.s.sync.mark(to.id, "mail", "self");
						this.s
							.notice(
								to,
								"mail_received",
								0,
								Msg.tr("cfhgui_server.chat.mail_received", mail.fromName).append(" ").append(Travel.button("cfhgui_server.chat.button.read", "/mail")),
								mail.fromName
							);
					}

					return OpResult.ok(Text.shown(rec));
				}
			}
		}
	}

	public OpResult mailRead(Actor a, @Nullable String id) {
		if (!this.s.rules().mail) {
			return OpResult.fail("disabled");
		} else if (!this.s.may(a, Nodes.MAIL_USE)) {
			return OpResult.fail("no_permission");
		} else {
			Session session = this.s.session(a);
			if (session == null) {
				return OpResult.fail("player_only");
			} else {
				PlayerRecord rec = session.record;
				boolean one = id != null && !id.isEmpty();
				int n = 0;
				boolean found = !one;

				for (PlayerRecord.Mail m : rec.mail) {
					if (!one || m.id.equals(id)) {
						found = true;
						if (!m.read) {
							m.read = true;
							n++;
						}

						if (one) {
							break;
						}
					}
				}

				if (!found) {
					return OpResult.fail("no_such_mail");
				} else {
					if (n > 0) {
						this.s.touch(rec);
						this.s.sync.mark(rec.id, "mail", "self");
					}

					return OpResult.ok(n);
				}
			}
		}
	}

	public OpResult mailDelete(Actor a, @Nullable String id) {
		if (!this.s.rules().mail) {
			return OpResult.fail("disabled");
		} else if (!this.s.may(a, Nodes.MAIL_USE)) {
			return OpResult.fail("no_permission");
		} else {
			Session session = this.s.session(a);
			if (session == null) {
				return OpResult.fail("player_only");
			} else {
				PlayerRecord rec = session.record;
				boolean one = id != null && !id.isEmpty();
				int n = 0;
				Iterator<PlayerRecord.Mail> it = rec.mail.iterator();

				while (it.hasNext()) {
					PlayerRecord.Mail m = it.next();
					if (one ? m.id.equals(id) : m.read) {
						it.remove();
						n++;
						if (one) {
							break;
						}
					}
				}

				if (one && n == 0) {
					return OpResult.fail("no_such_mail");
				} else {
					if (n > 0) {
						this.s.touch(rec);
						this.s.sync.mark(rec.id, "mail", "self");
					}

					return OpResult.ok(n);
				}
			}
		}
	}

	static String kitCooldown(String kitId) {
		return "kit:" + kitId.toLowerCase(Locale.ROOT);
	}

	@Nullable
	Kit kitFor(Actor a, @Nullable String id) {
		Kit kit = id == null ? null : this.s.kits().get(id.trim());
		if (kit == null) {
			return null;
		} else {
			return kit.restricted && !this.s.mayNamed(a, kit.node()) && !this.s.may(a, Nodes.ADMIN_KITS) ? null : kit;
		}
	}

	public OpResult kitClaim(Actor a, String id) {
		if (!this.s.rules().kits) {
			return OpResult.fail("disabled");
		} else if (!this.s.may(a, Nodes.KIT_USE)) {
			return OpResult.fail("no_permission");
		} else {
			Session session = this.s.session(a);
			if (session == null) {
				return OpResult.fail("player_only");
			} else {
				Kit kit = this.kitFor(a, id);
				if (kit == null) {
					return OpResult.fail("no_such_kit", Text.line(id, 24));
				} else if (kit.items.isEmpty()) {
					return OpResult.fail("kit_empty");
				} else {
					PlayerRecord rec = session.record;
					long now = this.s.now();
					String key = kit.id.toLowerCase(Locale.ROOT);
					if (kit.oneTime && rec.kitsTaken.contains(key)) {
						return OpResult.fail("kit_taken");
					} else {
						Long until = rec.cooldowns.get(kitCooldown(kit.id));
						if (until != null && until > now) {
							return OpResult.fail("kit_cooldown", Text.until(until, now));
						} else if (session.player != null && this.give(session.player, kit) == 0) {
							return OpResult.fail("kit_empty");
						} else {
							if (kit.oneTime) {
								rec.kitsTaken.add(key);
							}

							if (kit.cooldownSeconds > 0) {
								rec.cooldowns.put(kitCooldown(kit.id), now + (long)kit.cooldownSeconds * 1000L);
							}

							this.s.touch(rec);
							this.s.sync.mark(rec.id, "self");
							return OpResult.ok(kit.name);
						}
					}
				}
			}
		}
	}

	int give(ServerPlayer player, Kit kit) {
		int given = 0;

		for (String snbt : kit.items) {
			ItemStack stack = stack(player, snbt);
			if (stack.isEmpty()) {
				CFHGuiServer.LOGGER
					.warn("[CFHGui Server] the kit {} holds an item this server cannot read: {}", kit.id, snbt.length() > 120 ? snbt.substring(0, 120) + "..." : snbt);
			} else {
				given++;
				player.getInventory().add(stack);
				if (!stack.isEmpty()) {
					ItemEntity dropped = player.drop(stack, false);
					if (dropped != null) {
						dropped.setNoPickUpDelay();
						dropped.setTarget(player.getUUID());
					}
				}
			}
		}

		if (given > 0) {
			player.containerMenu.broadcastChanges();
		}

		return given;
	}

	private static ItemStack stack(ServerPlayer player, String snbt) {
		try {
			return ItemStack.parse(player.registryAccess(), TagParser.parseTag(snbt)).orElse(ItemStack.EMPTY);
		} catch (Exception var3) {
			return ItemStack.EMPTY;
		}
	}

	List<Kit.View> kitViews(Session session) {
		List<Kit.View> out = new ArrayList<>();
		Actor a = session.actor();
		if (this.s.rules().kits && this.s.may(a, Nodes.KIT_USE)) {
			long now = this.s.now();

			for (Kit kit : this.s.kits().kits) {
				if (!kit.restricted || this.s.mayNamed(a, kit.node())) {
					Kit.View v = new Kit.View();
					v.id = kit.id;
					v.name = kit.name;
					v.icon = kit.icon;
					v.contents = this.contents(kit);
					Long until = session.record.cooldowns.get(kitCooldown(kit.id));
					v.readyAt = kit.oneTime && session.record.kitsTaken.contains(kit.id.toLowerCase(Locale.ROOT)) ? -1L : (until != null && until > now ? until : 0L);
					out.add(v);
				}
			}

			return out;
		} else {
			return out;
		}
	}

	List<Kit.View.Entry> contents(Kit kit) {
		String key = kit.id + "#" + kit.items.hashCode();
		List<Kit.View.Entry> known = this.contents.get(key);
		if (known != null) {
			return known;
		} else {
			List<Kit.View.Entry> var9 = new ArrayList();

			for (String snbt : kit.items) {
				if (var9.size() >= 12) {
					break;
				}

				try {
					CompoundTag tag = TagParser.parseTag(snbt);
					Kit.View.Entry e = new Kit.View.Entry();
					e.item = tag.getString("id");
					e.count = tag.contains("count") ? Math.max(1, tag.getInt("count")) : 1;
					if (!e.item.isEmpty()) {
						var9.add(e);
					}
				} catch (Exception var8) {
				}
			}

			if (this.contents.size() > 256) {
				this.contents.clear();
			}

			this.contents.put(key, var9);
			return var9;
		}
	}

	public OpResult open(Actor a, String what) {
		String want = what == null ? "" : what;

		Node node = switch (want) {
			case "trash" -> Nodes.TRASH_USE;
			case "enderchest" -> Nodes.ENDERCHEST_USE;
			case "workbench" -> Nodes.WORKBENCH_USE;
			default -> null;
		};
		if (node == null) {
			return OpResult.fail("invalid");
		} else if (!this.s.may(a, node)) {
			return OpResult.fail("no_permission");
		} else {
			Session session = this.s.session(a);
			ServerPlayer player = session == null ? null : session.player;
			if (player == null) {
				return OpResult.fail("player_only");
			} else if (this.s.moderation.frozen(session)) {
				return OpResult.fail("frozen");
			} else if (!this.s.live) {
				return OpResult.ok(what);
			} else {
				switch (what) {
					case "trash":
						player.openMenu(
							new SimpleMenuProvider((id, inventory, p) -> ChestMenu.threeRows(id, inventory, new SimpleContainer(27)), Msg.tr("cfhgui_server.container.trash"))
						);
						break;
					case "enderchest":
						player.openMenu(
							new SimpleMenuProvider(
								(id, inventory, p) -> ChestMenu.threeRows(id, inventory, player.getEnderChestInventory()), Component.translatable("container.enderchest")
							)
						);
						break;
					default:
						player.openMenu(
							new SimpleMenuProvider((id, inventory, p) -> new CraftingMenu(id, inventory, ContainerLevelAccess.create(player.level(), player.blockPosition())) {
									public boolean stillValid(Player who) {
										return true;
									}
								}, Component.translatable("container.crafting"))
						);
				}

				return OpResult.ok(what);
			}
		}
	}

	public OpResult card(Actor a, @Nullable ServerService.Target t, String wanted) {
		if (!this.s.may(a, Nodes.SEEN_USE)) {
			return OpResult.fail("no_permission");
		} else {
			return t == null ? OpResult.fail("no_such_player", Text.line(wanted, 32)) : OpResult.answer(this.card(a, t));
		}
	}

	PlayerCard card(Actor viewer, ServerService.Target t) {
		PlayerRecord rec = t.record();
		Session session = t.session();
		boolean shown = session != null && this.s.visible(viewer, session);
		PlayerCard c = new PlayerCard();
		c.id = t.id();
		c.name = t.name();
		c.nick = rec.nick;

		for (Role role : this.s.perms.held(t.id(), this.s.levelOf(t.id(), t.name()))) {
			c.roles.add(new Role.Tag(role));
		}

		c.online = shown;
		c.afk = shown && session.afk;
		c.firstSeen = rec.firstSeen;
		c.lastSeen = session != null && !shown ? session.seenBefore : rec.lastSeen;
		c.sessions = rec.sessions;
		c.playTicks = this.s.statistics.value(t.id(), PLAY_TIME, session != null && !shown);
		c.afkTicks = rec.afkTicks;
		c.statsHidden = rec.statsHidden;
		return c;
	}

	public OpResult statsHide(Actor a, boolean on) {
		if (!this.s.may(a, Nodes.STATS_HIDE)) {
			return OpResult.fail("no_permission");
		} else {
			Session session = this.s.session(a);
			if (session == null) {
				return OpResult.fail("player_only");
			} else {
				if (session.record.statsHidden != on) {
					if (!session.allow("stats_hide", 3000L, this.s.now())) {
						return OpResult.fail("too_fast");
					}

					session.record.statsHidden = on;
					this.s.touch(session.record);
					this.s.statistics.changed();
					this.s.sync.mark(session.id, "self");
				}

				return OpResult.ok(on);
			}
		}
	}
}
