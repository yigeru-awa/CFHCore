package com.onehumanawa.cfhcore.content.gui.server.service;

import com.mojang.authlib.GameProfile;
import com.onehumanawa.cfhcore.content.gui.core.perm.Perms.Node;
import com.onehumanawa.cfhcore.content.gui.server.Nodes;
import com.onehumanawa.cfhcore.content.gui.server.CFHGuiServer;
import com.onehumanawa.cfhcore.content.gui.server.api.ServerApi;
import com.onehumanawa.cfhcore.content.gui.server.model.Place;
import com.onehumanawa.cfhcore.content.gui.server.model.PlayerRecord;
import com.onehumanawa.cfhcore.content.gui.server.model.Punishment;
import com.onehumanawa.cfhcore.content.gui.server.model.ServerRules;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.UserBanList;
import net.minecraft.server.players.UserBanListEntry;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameType;
import org.jetbrains.annotations.Nullable;

public final class Moderation {
	static final Set<String> SPEAKING = Set.of("msg", "tell", "w", "me", "say", "teammsg", "tm", "staffchat", "sc", "broadcast", "bc", "tc");
	static final Set<String> WHILE_FROZEN = Set.of("msg", "tell", "w");
	private static final long TELL_GAP_MS = 2500L;
	private static final int HISTORY_MAX = 400;
	private static final long BROADCAST_GAP_MS = 1000L;
	static final String STAFF_INSERTION = "cfhgui_server:staff";
	static final String FOREVER = "∞";
	private final ServerService s;

	Moderation(ServerService s) {
		this.s = s;
	}

	static Node node(Punishment.Kind kind) {
		return switch (kind) {
			case MUTE -> Nodes.MOD_MUTE;
			case BAN -> Nodes.MOD_BAN;
			case WARN -> Nodes.MOD_WARN;
			case KICK -> Nodes.MOD_KICK;
			case FREEZE -> Nodes.MOD_FREEZE;
		};
	}

	public OpResult punish(Actor a, @Nullable ServerService.Target t, String wanted, @Nullable Punishment.Kind kind, @Nullable String rawReason, int seconds) {
		if (kind != null && seconds >= 0) {
			if (!this.s.may(a, node(kind))) {
				return OpResult.fail("no_permission");
			} else {
				boolean lasts = kind.lasts();
				int length = lasts ? Math.min(seconds, 315360000) : 0;
				if (kind == Punishment.Kind.BAN && length == 0 && !this.s.may(a, Nodes.MOD_BAN_PERMANENT)) {
					return OpResult.fail("no_permission");
				} else if (t == null) {
					return OpResult.fail("no_such_player", Text.line(wanted, 32));
				} else if (t.byNickname()) {
					return this.s.nicknamed(t);
				} else if (t.id().equals(a.id())) {
					return OpResult.fail("self");
				} else if (!this.s.outranks(a, t)) {
					return OpResult.fail("outranked", t.name());
				} else {
					Session session = t.session();
					if (kind == Punishment.Kind.KICK && session == null) {
						return OpResult.fail("player_offline", t.name());
					} else {
						PlayerRecord rec = t.record();
						long now = this.s.now();
						if (lasts && rec.active(kind, now) != null) {
							return OpResult.fail("already_punished", t.name());
						} else {
							Punishment p = new Punishment();
							p.id = Long.toHexString(UUID.randomUUID().getMostSignificantBits() & 1099511627775L);
							p.kind = kind;
							p.reason = Text.line(rawReason, 160);
							p.by = a.console() ? Actor.NOBODY : a.id();
							p.byName = a.name();
							p.at = now;
							p.expiresAt = lasts && length > 0 ? now + (long)length * 1000L : 0L;
							rec.punishments.add(p);
							int i = 0;

							while (rec.punishments.size() > 400 && i < rec.punishments.size()) {
								if (rec.punishments.get(i).active(now)) {
									i++;
								} else {
									rec.punishments.remove(i);
								}
							}

							this.s.touch(rec);
							String howLong = lasts ? (length > 0 ? Text.span((long)length) : "∞") : "";
							String why = p.reason.isEmpty() ? howLong : (howLong.isEmpty() ? p.reason : howLong + ", " + p.reason);
							this.s.audit(a, "punish_" + kind.key(), t.id(), t.name(), why);
							switch (kind) {
								case MUTE:
									if (session != null) {
										session.wasMuted = true;
										this.s.notice(session, "muted", 15026253, Msg.tr("cfhgui_server.chat.muted", a.name(), why), why);
									}
									break;
								case BAN:
									if (this.s.live) {
										UserBanList bans = this.s.server.getPlayerList().getBans();
										bans.add(
											new UserBanListEntry(
												new GameProfile(t.id(), t.name()), new Date(now), a.name(), p.expiresAt > 0L ? new Date(p.expiresAt) : null, p.reason.isEmpty() ? null : p.reason
											)
										);
										if (session != null && session.player != null) {
											session.player.connection.disconnect(this.banMessage(p));
										}
									}
									break;
								case WARN:
									if (session != null) {
										this.s
											.notice(
												session,
												"warned",
												14722136,
												Msg.tr("cfhgui_server.chat.warned", a.name(), p.reason.isEmpty() ? "-" : p.reason),
												p.reason.isEmpty() ? "-" : p.reason,
												a.name()
											);
									}
									break;
								case KICK:
									if (this.s.live && session.player != null) {
										session.player.connection.disconnect(this.kickMessage(p));
									}
									break;
								case FREEZE:
									if (session != null) {
										this.hold(session);
									}
							}

							if (session != null) {
								this.s.sync.mark(session.id, "self");
							}

							if (this.s.live) {
								ServerApi.PUNISHMENT_CHANGED.invoke(l -> l.changed(t.id(), p, false));
							}

							return OpResult.ok(t.name(), kind.key(), length, p.reason);
						}
					}
				}
			}
		} else {
			return OpResult.fail("invalid");
		}
	}

	private MutableComponent kickMessage(Punishment p) {
		return p.reason.isEmpty() ? Msg.tr("cfhgui_server.kick.kicked", p.byName) : Msg.tr("cfhgui_server.kick.kicked.reason", p.byName, p.reason);
	}

	private MutableComponent banMessage(Punishment p) {
		MutableComponent out = p.reason.isEmpty() ? Msg.tr("cfhgui_server.kick.banned", p.byName) : Msg.tr("cfhgui_server.kick.banned.reason", p.byName, p.reason);
		return out.append("\n")
			.append(p.expiresAt > 0L ? Msg.tr("cfhgui_server.kick.banned.until", Text.date(p.expiresAt)) : Msg.tr("cfhgui_server.kick.banned.forever"));
	}

	public OpResult pardon(Actor a, @Nullable ServerService.Target t, String wanted, @Nullable Punishment.Kind kind) {
		if (kind != null && kind.lasts()) {
			if (!this.s.may(a, node(kind))) {
				return OpResult.fail("no_permission");
			} else if (t == null) {
				return OpResult.fail("no_such_player", Text.line(wanted, 32));
			} else if (t.byNickname()) {
				return this.s.nicknamed(t);
			} else if (!t.id().equals(a.id()) && !this.s.outranks(a, t)) {
				return OpResult.fail("outranked", t.name());
			} else {
				PlayerRecord rec = t.record();
				long now = this.s.now();
				Punishment p = rec.active(kind, now);
				if (t.id().equals(a.id()) && p != null) {
					return OpResult.fail("self");
				} else {
					boolean gameBan = false;
					if (kind == Punishment.Kind.BAN && this.s.live) {
						UserBanList bans = this.s.server.getPlayerList().getBans();
						GameProfile profile = new GameProfile(t.id(), t.name());
						gameBan = bans.isBanned(profile);
						if (gameBan) {
							bans.remove(profile);
						}
					}

					if (p == null && !gameBan) {
						return OpResult.fail("not_punished", t.name());
					} else {
						if (p != null) {
							p.liftedAt = now;
							p.liftedByName = a.name();
							this.s.touch(rec);
						}

						this.s.audit(a, "pardon_" + kind.key(), t.id(), t.name(), "");
						Session session = t.session();
						if (session != null) {
							if (kind == Punishment.Kind.MUTE) {
								this.released(session, true);
							}

							if (kind == Punishment.Kind.FREEZE) {
								this.released(session, false);
							}
						}

						if (this.s.live && p != null) {
							ServerApi.PUNISHMENT_CHANGED.invoke(l -> l.changed(t.id(), p, true));
						}

						return OpResult.ok(t.name(), kind.key());
					}
				}
			}
		} else {
			return OpResult.fail("invalid");
		}
	}

	private void released(Session session, boolean mute) {
		if (mute) {
			session.wasMuted = false;
			this.s.notice(session, "unmuted", 6074742, Msg.tr("cfhgui_server.chat.unmuted"));
		} else {
			session.wasFrozen = false;
			session.frozenAt = null;
			this.s.notice(session, "unfrozen", 6074742, Msg.tr("cfhgui_server.chat.unfrozen"));
		}

		this.s.sync.mark(session.id, "self");
	}

	private void hold(Session session) {
		session.wasFrozen = true;
		session.frozenAt = session.place();
		this.s.teleports.drop(session);
		if (this.s.live && session.player != null) {
			session.player.closeContainer();
		}

		this.s.notice(session, "frozen", 6061306, Msg.tr("cfhgui_server.chat.frozen"));
		this.s.sync.mark(session.id, "self");
	}

	public OpResult history(Actor a, @Nullable ServerService.Target t, String wanted) {
		if (!this.s.may(a, Nodes.MOD_HISTORY)) {
			return OpResult.fail("no_permission");
		} else {
			return t == null
				? OpResult.fail("no_such_player", Text.line(wanted, 32))
				: OpResult.answer(new Moderation.History(t.name(), List.copyOf(t.record().punishments), this.notesFor(a, t)));
		}
	}

	List<PlayerRecord.Note> notesFor(Actor a, ServerService.Target t) {
		return t.id().equals(a.id()) ? List.of() : List.copyOf(t.record().notes);
	}

	public OpResult noteAdd(Actor a, @Nullable ServerService.Target t, String wanted, String text) {
		if (!this.s.may(a, Nodes.MOD_HISTORY)) {
			return OpResult.fail("no_permission");
		} else if (t == null) {
			return OpResult.fail("no_such_player", Text.line(wanted, 32));
		} else if (t.byNickname()) {
			return this.s.nicknamed(t);
		} else if (t.id().equals(a.id())) {
			return OpResult.fail("self");
		} else if (!Text.fits(text, 256)) {
			return OpResult.fail("text_invalid");
		} else {
			PlayerRecord rec = t.record();
			if (rec.notes.size() >= 100) {
				rec.notes.remove(0);
			}

			PlayerRecord.Note note = new PlayerRecord.Note();
			note.by = a.console() ? Actor.NOBODY : a.id();
			note.byName = a.name();
			note.text = Text.line(text, 256);
			note.at = this.s.now();
			rec.notes.add(note);
			this.s.touch(rec);
			this.s.audit(a, "note_add", t.id(), t.name(), note.text);
			return OpResult.ok(t.name());
		}
	}

	public OpResult noteDelete(Actor a, @Nullable ServerService.Target t, String wanted, int index) {
		if (!this.s.may(a, Nodes.MOD_HISTORY)) {
			return OpResult.fail("no_permission");
		} else if (t == null) {
			return OpResult.fail("no_such_player", Text.line(wanted, 32));
		} else if (t.byNickname()) {
			return this.s.nicknamed(t);
		} else if (t.id().equals(a.id())) {
			return OpResult.fail("self");
		} else {
			PlayerRecord rec = t.record();
			if (index >= 0 && index < rec.notes.size()) {
				PlayerRecord.Note note = rec.notes.get(index);
				if (!this.mayDelete(a, note)) {
					return OpResult.fail("outranked", note.byName);
				} else {
					rec.notes.remove(index);
					this.s.touch(rec);
					this.s.audit(a, "note_delete", t.id(), t.name(), note.text);
					return OpResult.ok(t.name());
				}
			} else {
				return OpResult.fail("invalid");
			}
		}
	}

	private boolean mayDelete(Actor a, PlayerRecord.Note note) {
		if (!a.console() && !note.by.equals(a.id()) && this.s.perms.rank(a) != Integer.MAX_VALUE) {
			ServerService.Target writer = note.by.equals(Actor.NOBODY) ? null : this.s.target(note.by, null);
			return writer != null && this.s.outranks(a, writer);
		} else {
			return true;
		}
	}

	public OpResult playerDo(Actor a, @Nullable ServerService.Target t, String wanted, @Nullable String what, @Nullable String rawValue) {
		String action = what == null ? "" : what;

		Node node = switch (action) {
			case "heal", "feed", "drink" -> Nodes.MOD_HEAL;
			case "fly" -> Nodes.MOD_FLY;
			case "god" -> Nodes.MOD_GOD;
			case "speed" -> Nodes.MOD_SPEED;
			case "gamemode" -> Nodes.MOD_GAMEMODE;
			case "vanish" -> Nodes.MOD_VANISH;
			case "invsee", "endersee" -> Nodes.MOD_INVSEE;
			case "goto", "bring" -> Nodes.MOD_TELEPORT;
			default -> null;
		};
		if (node == null) {
			return OpResult.fail("invalid");
		} else if (what.equals("drink") && !Thirst.present()) {
			return OpResult.fail("disabled");
		} else if (!this.s.may(a, node)) {
			return OpResult.fail("no_permission");
		} else if (t == null) {
			return OpResult.fail("no_such_player", Text.line(wanted, 32));
		} else if (t.byNickname()) {
			return this.s.nicknamed(t);
		} else {
			boolean selfTarget = t.id().equals(a.id());
			if (!selfTarget && !action.equals("goto") && !this.s.outranks(a, t)) {
				return OpResult.fail("outranked", t.name());
			} else {
				Session session = t.session();
				String value = rawValue == null ? "" : rawValue.trim().toLowerCase(Locale.ROOT);
				PlayerRecord rec = t.record();
				if (session == null && !what.equals("goto")) {
					return OpResult.fail("player_offline", t.name());
				} else {
					ServerPlayer player = session == null ? null : session.player;
					String detail = "";
					switch (what) {
						case "heal":
							if (player != null && this.s.live) {
								player.setHealth(player.getMaxHealth());
								player.clearFire();
								player.setAirSupply(player.getMaxAirSupply());
							}
							break;
						case "feed":
							if (player != null && this.s.live) {
								player.getFoodData().setFoodLevel(20);
								player.getFoodData().setSaturation(20.0F);
								player.getFoodData().setExhaustion(0.0F);
								Thirst.fill(player);
							}
							break;
						case "drink":
							if (player != null && this.s.live) {
								Thirst.fill(player);
							}
							break;
						case "fly":
						case "god":
							boolean fly = what.equals("fly");
							Boolean on = onOff(value, fly ? rec.fly : rec.god);
							if (on == null) {
								return OpResult.fail("value_invalid");
							}

							if (fly) {
								rec.fly = on;
							} else {
								rec.god = on;
							}

							this.s.touch(rec);
							if (fly && player != null && this.s.live) {
								this.applyFly(player, on);
							}

							detail = on ? "on" : "off";
							this.s.sync.mark(t.id(), "self");
							if (!selfTarget) {
								this.s.say(session, what + ":" + detail, Msg.tr("cfhgui_server.chat." + what + "_" + detail, a.name()));
							}
							break;
						case "speed":
							int speed;
							try {
								speed = Integer.parseInt(value);
							} catch (NumberFormatException var21) {
								return OpResult.fail("value_invalid");
							}

							if (speed < 0 || speed > 10) {
								return OpResult.fail("value_invalid");
							}

							int factor = speed == 0 ? 1 : speed;
							if (player != null && this.s.live) {
								AttributeInstance walk = player.getAttribute(Attributes.MOVEMENT_SPEED);
								if (walk != null) {
									walk.setBaseValue(0.1 * (double)factor);
								}

								player.getAbilities().setFlyingSpeed(0.05F * (float)factor);
								player.onUpdateAbilities();
							}

							detail = Integer.toString(factor);
							break;
						case "gamemode":
							GameType type = GameType.byName(value, null);
							if (type == null) {
								return OpResult.fail("value_invalid");
							}

							if (player != null && this.s.live) {
								player.setGameMode(type);
								if (rec.fly) {
									this.applyFly(player, true);
								}
							}

							detail = type.getName();
							break;
						case "vanish":
							Boolean vanished = onOff(value, rec.vanished);
							if (vanished == null) {
								return OpResult.fail("value_invalid");
							}

							this.s.vanish.set(session, vanished);
							detail = vanished ? "on" : "off";
							break;
						case "invsee":
						case "endersee":
							Session viewer = this.s.session(a);
							if (viewer == null) {
								return OpResult.fail("player_only");
							}

							boolean ender = what.equals("endersee");
							if (selfTarget && !ender) {
								return OpResult.fail("self");
							}

							if (viewer.player != null && player != null && this.s.live) {
								boolean edit = selfTarget || this.s.may(a, Nodes.MOD_INVSEE_EDIT);
								String shown = Text.shown(rec);
								viewer.player
									.openMenu(
										new SimpleMenuProvider(
											(id, inventory, p) -> ender ? Peek.enderChest(id, inventory, player, edit) : Peek.inventory(id, inventory, player, edit),
											Msg.tr(ender ? "cfhgui_server.container.enderchest" : "cfhgui_server.container.inventory", shown)
										)
									);
							}
							break;
						case "goto":
						case "bring":
							if (selfTarget) {
								return OpResult.fail("self");
							}

							Session actor = this.s.session(a);
							if (actor == null) {
								return OpResult.fail("player_only");
							}

							boolean bring = what.equals("bring");
							Place to = bring ? actor.place() : (session != null ? session.place() : rec.lastPlace);
							if (to == null) {
								return OpResult.fail("player_offline", t.name());
							}

							Teleports.Plan plan = new Teleports.Plan(ServerApi.Cause.STAFF, "player:" + (bring ? actor.name : t.name()), to.copy());
							plan.staff = true;
							Session traveller = bring ? session : actor;
							this.s.teleports.drop(traveller);
							OpResult r = this.s.teleports.begin(traveller, plan);
							if (!r.ok()) {
								return r;
							}

							if (bring) {
								this.s.say(session, "brought", Msg.tr("cfhgui_server.chat.brought", a.name()));
							}
							break;
						default:
							return OpResult.fail("invalid");
					}

					if (!selfTarget || what.equals("gamemode") || what.equals("vanish")) {
						this.s.audit(a, "player_" + what, t.id(), t.name(), detail);
					}

					return OpResult.ok(t.name(), detail);
				}
			}
		}
	}

	public static boolean thirstHere() {
		return Thirst.present();
	}

	@Nullable
	private static Boolean onOff(String value, boolean current) {
		return switch (value) {
			case "", "toggle" -> !current;
			case "on", "true", "1" -> Boolean.TRUE;
			case "off", "false", "0" -> Boolean.FALSE;
			default -> null;
		};
	}

	void applyFly(ServerPlayer player, boolean on) {
		boolean always = player.isCreative() || player.isSpectator();
		player.getAbilities().mayfly = on || always;
		if (!player.getAbilities().mayfly) {
			player.getAbilities().flying = false;
		}

		player.onUpdateAbilities();
	}

	private static boolean mayFly(ServerPlayer player) {
		return player.getAbilities().mayfly;
	}

	private void tellFlight(Session session) {
		if (this.s.trace != null) {
			this.s.trace.add(session.name + " <- flight");
		}

		ServerPlayer p = session.player;
		if (p != null && this.s.live) {
			this.applyFly(p, true);
		}
	}

	public OpResult broadcast(Actor a, String text) {
		if (!this.s.may(a, Nodes.MOD_BROADCAST)) {
			return OpResult.fail("no_permission");
		} else if (!Text.fits(text, 256)) {
			return OpResult.fail("text_invalid");
		} else {
			Session session = this.s.session(a);
			if (session != null) {
				OpResult muted = this.mutedResult(session);
				if (muted != null) {
					return muted;
				}

				if (!session.allow("broadcast", 1000L, this.s.now())) {
					return OpResult.fail("too_fast");
				}
			}

			String line = Text.line(text, 256);
			this.s.sayAll("broadcast", Msg.tr("cfhgui_server.chat.broadcast", Text.styled(line)).withStyle(style -> style.withColor(14722136)));
			this.s.audit(a, "broadcast", null, "", line);
			return OpResult.OK;
		}
	}

	public OpResult staffChat(Actor a, String message) {
		if (!this.s.rules().staffChat) {
			return OpResult.fail("disabled");
		} else if (!this.s.may(a, Nodes.MOD_STAFF_CHAT)) {
			return OpResult.fail("no_permission");
		} else if (!Text.fits(message, 256)) {
			return OpResult.fail("text_invalid");
		} else {
			Session from = this.s.session(a);
			if (from != null) {
				OpResult muted = this.mutedResult(from);
				if (muted != null) {
					return muted;
				}
			}

			String name = from == null ? a.name() : Text.shown(from.record);
			Component line = Msg.tr("cfhgui_server.chat.staff", name, Text.line(message, 256))
				.withStyle(style -> style.withColor(5621992).withInsertion("cfhgui_server:staff"));
			int heard = 0;

			for (Session other : this.s.online()) {
				if (this.s.may(other.actor(), Nodes.MOD_STAFF_CHAT)) {
					this.s.say(other, "staff", line);
					heard++;
				}
			}

			if (this.s.live) {
				CFHGuiServer.LOGGER.info("[CFHGui Server] staff chat: {}: {}", name, Text.line(message, 256));
			}

			return OpResult.ok(heard);
		}
	}

	private boolean exempt(Session session) {
		return this.s.may(session.actor(), Nodes.MOD_EXEMPT);
	}

	boolean frozen(Session session) {
		return session.record.active(Punishment.Kind.FREEZE, this.s.now()) != null && !this.exempt(session);
	}

	boolean muted(Session session) {
		return session.record.active(Punishment.Kind.MUTE, this.s.now()) != null && !this.exempt(session);
	}

	@Nullable
	public OpResult mutedResult(Session session) {
		long now = this.s.now();
		Punishment mute = session.record.active(Punishment.Kind.MUTE, now);
		if (mute != null && !this.exempt(session)) {
			String howLong = mute.expiresAt == 0L ? "∞" : Text.until(mute.expiresAt, now);
			return OpResult.fail("muted", mute.reason.isEmpty() ? howLong : mute.reason + ", " + howLong);
		} else {
			return null;
		}
	}

	boolean chatAllowed(Session session) {
		OpResult muted = this.mutedResult(session);
		if (muted == null) {
			return true;
		} else {
			this.s.say(session, "muted", muted.message().withStyle(style -> style.withColor(15026253)));
			return false;
		}
	}

	boolean commandRefused(Session session, String command) {
		String root = command.startsWith("/") ? command.substring(1) : command;
		int space = root.indexOf(32);
		if (space >= 0) {
			root = root.substring(0, space);
		}

		int colon = root.indexOf(58);
		if (colon >= 0) {
			root = root.substring(colon + 1);
		}

		root = root.toLowerCase(Locale.ROOT);
		if (this.frozen(session) && !WHILE_FROZEN.contains(root)) {
			this.tellHeld(session);
			return true;
		} else {
			if (SPEAKING.contains(root)) {
				OpResult muted = this.mutedResult(session);
				if (muted != null) {
					this.s.say(session, "muted", muted.message().withStyle(style -> style.withColor(15026253)));
					return true;
				}
			}

			return false;
		}
	}

	void tellHeld(Session session) {
		long now = System.currentTimeMillis();
		if (now - session.toldAt >= 2500L) {
			session.toldAt = now;
			this.s.say(session, "frozen", OpResult.fail("frozen").message().withStyle(style -> style.withColor(6061306)));
		}
	}

	boolean damageAllowed(Session session, ServerPlayer player, DamageSource source) {
		return !this.invulnerable(session, player) || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
	}

	boolean god(ServerPlayer player) {
		Session session = this.s.session(player.getUUID());
		return session != null && session.record.god;
	}

	boolean invulnerable(Session session, ServerPlayer player) {
		return session.record.god || player.getAbilities().invulnerable;
	}

	void steady(Session session) {
		ServerPlayer p = session.player;
		if (p != null && this.s.live) {
			if (this.invulnerable(session, p) && !p.isDeadOrDying()) {
				if (session.steady == null) {
					session.steady = Vitals.of(p);
				}

				session.steady.hold(p);
			} else {
				session.steady = null;
			}
		}
	}

	void joined(Session session) {
		PlayerRecord rec = session.record;
		long now = this.s.now();
		Punishment ban = rec.active(Punishment.Kind.BAN, now);
		if (ban != null && this.s.live) {
			ban.liftedAt = now;
			ban.liftedByName = "/pardon";
		}

		session.wasMuted = rec.active(Punishment.Kind.MUTE, now) != null;
		session.wasFrozen = rec.active(Punishment.Kind.FREEZE, now) != null;
		if (session.wasFrozen) {
			session.frozenAt = session.place();
		}

		if (rec.fly && session.player != null && this.s.live) {
			this.applyFly(session.player, true);
		}

		if (session.wasMuted && !this.exempt(session)) {
			OpResult muted = this.mutedResult(session);
			if (muted != null) {
				this.s.say(session, "muted", muted.message().withStyle(style -> style.withColor(15026253)));
			}
		}

		if (session.wasFrozen && !this.exempt(session)) {
			this.s.say(session, "frozen", Msg.tr("cfhgui_server.chat.frozen").withStyle(style -> style.withColor(6061306)));
		}
	}

	void copied(Session session, @Nullable ServerPlayer to) {
		if (to != null && session.record.fly && this.s.live) {
			this.applyFly(to, true);
		}

		session.replaced = true;
		session.steady = null;
		session.frozenAt = null;
	}

	void moved(Session session, @Nullable Place to) {
		if (session.wasFrozen && to != null) {
			session.frozenAt = to.copy();
		}
	}

	void tick() {
		if (!this.s.sessions.isEmpty()) {
			boolean second = this.s.ticks % 20 == 0;
			long now = this.s.now();

			for (Session session : this.s.online()) {
				if (session.wasFrozen) {
					if (session.record.active(Punishment.Kind.FREEZE, now) == null) {
						this.released(session, false);
						this.s.touch(session.record);
						this.announceExpiry(session, Punishment.Kind.FREEZE);
					} else if (!this.exempt(session)) {
						this.keepInPlace(session);
					}
				}

				if (session.replaced) {
					session.replaced = false;
					if (session.record.fly) {
						this.tellFlight(session);
					}
				}

				if (second) {
					if (session.wasMuted && session.record.active(Punishment.Kind.MUTE, now) == null) {
						this.released(session, true);
						this.s.touch(session.record);
						this.announceExpiry(session, Punishment.Kind.MUTE);
					}

					ServerPlayer p = session.player;
					if (p != null && this.s.live && session.record.fly && !mayFly(p)) {
						this.applyFly(p, true);
					}
				}
			}
		}
	}

	private void announceExpiry(Session session, Punishment.Kind kind) {
		if (this.s.live) {
			Punishment last = null;

			for (Punishment p : session.record.punishments) {
				if (p.kind == kind) {
					last = p;
				}
			}

			if (last != null && last.liftedAt == 0L) {
				Punishment ended = last;
				ServerApi.PUNISHMENT_CHANGED.invoke(l -> l.changed(session.id, ended, true));
			}
		}
	}

	private void keepInPlace(Session session) {
		ServerPlayer p = session.player;
		Place at = session.frozenAt;
		Place here = session.place();
		if (here != null) {
			if (at == null) {
				session.frozenAt = here;
			} else if (!at.dimension.equals(here.dimension)) {
				session.frozenAt = here;
			} else {
				if (here.y < at.y) {
					at.y = here.y;
				}

				double dx = here.x - at.x;
				double dz = here.z - at.z;
				if (!(dx * dx + dz * dz < 0.01) || !(here.y - at.y < 0.3)) {
					if (p == null) {
						session.fakePlace = at.copy();
					} else if (this.s.live) {
						if (p.isPassenger()) {
							p.stopRiding();
						}

						p.connection.teleport(at.x, at.y, at.z, p.getYRot(), p.getXRot());
						this.tellHeld(session);
					}
				}
			}
		}
	}

	@Nullable
	Component loginRefusal(GameProfile profile) {
		ServerRules rules = this.s.rules();
		if (!rules.maintenance || profile.getId() == null) {
			return null;
		} else if (this.s.server.isSingleplayerOwner(profile)) {
			return null;
		} else {
			Boolean bypass = this.s.perms.hasOffline(profile.getId(), this.s.server.getProfilePermissions(profile), false, Nodes.MAINTENANCE_BYPASS.id());
			return bypass != null && bypass ? null : this.maintenanceMessage();
		}
	}

	private Component maintenanceMessage() {
		String own = this.s.rules().maintenanceMessage;
		return own != null && !own.isBlank() ? Text.styled(own) : Msg.tr("cfhgui_server.kick.maintenance");
	}

	void enforceMaintenance() {
		if (this.s.live && this.s.rules().maintenance) {
			for (Session session : new ArrayList<>(this.s.online())) {
				ServerPlayer p = session.player;
				if (p != null && !this.s.server.isSingleplayerOwner(p.getGameProfile()) && !this.s.may(session.actor(), Nodes.MAINTENANCE_BYPASS)) {
					p.connection.disconnect(this.maintenanceMessage());
				}
			}
		}
	}

	public static record History(String name, List<Punishment> punishments, List<PlayerRecord.Note> notes) {
	}
}
