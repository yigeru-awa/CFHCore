package com.onehumanawa.cfhcore.content.gui.server.service;

import com.onehumanawa.cfhcore.content.gui.core.perm.Perms;
import com.onehumanawa.cfhcore.content.gui.server.Nodes;
import com.onehumanawa.cfhcore.content.gui.server.api.ServerApi;
import com.onehumanawa.cfhcore.content.gui.server.model.Home;
import com.onehumanawa.cfhcore.content.gui.server.model.Place;
import com.onehumanawa.cfhcore.content.gui.server.model.PlayerRecord;
import com.onehumanawa.cfhcore.content.gui.server.model.ServerRules;
import com.onehumanawa.cfhcore.content.gui.server.model.Warp;
import java.util.Locale;
import java.util.regex.Pattern;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.ClickEvent.Action;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public final class Travel {
	static final int MAX_WARPS = 512;
	static final int ICON_MAX = 64;
	private static final Pattern ICON = Pattern.compile("[A-Z0-9_]{1,48}|[a-z0-9_.-]{1,32}:[a-z0-9_./-]{1,48}");
	private final ServerService s;

	Travel(ServerService s) {
		this.s = s;
	}

	static String icon(@Nullable String raw) {
		if (raw == null) {
			return "";
		} else {
			String icon = raw.trim();
			return icon.length() <= 64 && ICON.matcher(icon).matches() ? icon : "";
		}
	}

	public OpResult homeSet(Actor a, @Nullable String rawName, @Nullable String icon, @Nullable Integer color) {
		ServerRules rules = this.s.rules();
		if (!rules.homes) {
			return OpResult.fail("disabled");
		} else if (!this.s.may(a, Nodes.HOME_SET)) {
			return OpResult.fail("no_permission");
		} else {
			Session session = this.s.session(a);
			Place here = session == null ? null : session.place();
			if (here == null) {
				return OpResult.fail("player_only");
			} else {
				String name = Text.name(rawName != null && !rawName.isBlank() ? rawName : "home");
				if (name == null) {
					return OpResult.fail("name_invalid");
				} else if (rules.noHomeDimensions.contains(here.dimension) && !this.s.may(a, Nodes.TP_ANYWHERE)) {
					return OpResult.fail("dimension_closed");
				} else {
					PlayerRecord rec = session.record;
					Home home = rec.home(name);
					if (home == null) {
						int max = this.s.limit(a, Nodes.HOMES_MAX, rules.maxHomes);
						if (rec.homes.size() >= max) {
							return OpResult.fail("home_limit", max);
						}

						home = new Home();
						home.name = name;
						home.createdAt = this.s.now();
						rec.homes.add(home);
					}

					home.place = here;
					if (icon != null) {
						home.icon = icon(icon);
					}

					if (color != null) {
						home.color = color & 16777215;
					}

					this.s.touch(rec);
					this.s.sync.mark(rec.id, "self");
					return OpResult.ok(home.name);
				}
			}
		}
	}

	public OpResult homeEdit(Actor a, String name, @Nullable String rename, @Nullable String icon, @Nullable Integer color) {
		if (!this.s.rules().homes) {
			return OpResult.fail("disabled");
		} else if (!this.s.may(a, Nodes.HOME_SET)) {
			return OpResult.fail("no_permission");
		} else {
			Session session = this.s.session(a);
			if (session == null) {
				return OpResult.fail("player_only");
			} else {
				PlayerRecord rec = session.record;
				Home home = name == null ? null : rec.home(name);
				if (home == null) {
					return OpResult.fail("no_such_home", Text.line(name, 24));
				} else {
					if (rename != null && !rename.equals(home.name)) {
						String to = Text.name(rename);
						if (to == null) {
							return OpResult.fail("name_invalid");
						}

						Home other = rec.home(to);
						if (other != null && other != home) {
							return OpResult.fail("name_taken");
						}

						home.name = to;
					}

					if (icon != null) {
						home.icon = icon(icon);
					}

					if (color != null) {
						home.color = color & 16777215;
					}

					this.s.touch(rec);
					this.s.sync.mark(rec.id, "self");
					return OpResult.ok(home.name);
				}
			}
		}
	}

	public OpResult homeDelete(Actor a, String name, @Nullable ServerService.Target owner) {
		if (!this.s.rules().homes) {
			return OpResult.fail("disabled");
		} else {
			boolean others = owner != null && !owner.id().equals(a.id());
			if (!this.s.may(a, others ? Nodes.HOME_OTHERS : Nodes.HOME_SET)) {
				return OpResult.fail("no_permission");
			} else {
				PlayerRecord rec;
				if (others) {
					if (owner.byNickname()) {
						return this.s.nicknamed(owner);
					}

					if (!this.s.outranks(a, owner)) {
						return OpResult.fail("outranked", owner.name());
					}

					rec = owner.record();
				} else {
					Session session = this.s.session(a);
					if (session == null) {
						return OpResult.fail("player_only");
					}

					rec = session.record;
				}

				Home home = name == null ? null : rec.home(name);
				if (home == null) {
					return OpResult.fail("no_such_home", Text.line(name, 24));
				} else {
					rec.homes.remove(home);
					this.s.touch(rec);
					this.s.sync.mark(rec.id, "self");
					if (others) {
						this.s.audit(a, "home_delete", owner.id(), owner.name(), home.name);
					}

					return OpResult.ok(home.name);
				}
			}
		}
	}

	public OpResult homeGo(Actor a, @Nullable String name, @Nullable ServerService.Target owner) {
		if (!this.s.rules().homes) {
			return OpResult.fail("disabled");
		} else {
			boolean others = owner != null && !owner.id().equals(a.id());
			if (!this.s.may(a, others ? Nodes.HOME_OTHERS : Nodes.HOME_USE)) {
				return OpResult.fail("no_permission");
			} else {
				Session session = this.s.session(a);
				if (session == null) {
					return OpResult.fail("player_only");
				} else {
					PlayerRecord rec = others ? owner.record() : session.record;
					Home home = pick(rec, name);
					if (home == null) {
						return OpResult.fail("no_such_home", name != null && !name.isBlank() ? Text.line(name, 24) : "home");
					} else {
						Teleports.Plan plan = new Teleports.Plan(ServerApi.Cause.HOME, "home:" + home.name, home.place.copy());
						OpResult r = this.s.teleports.begin(session, plan);
						if (r.ok() && others) {
							this.s.audit(a, "home_go", owner.id(), owner.name(), home.name);
						}

						return r.ok() && r.args().isEmpty() ? OpResult.ok(home.name) : r;
					}
				}
			}
		}
	}

	public OpResult homes(Actor a, @Nullable ServerService.Target owner) {
		if (!this.s.rules().homes) {
			return OpResult.fail("disabled");
		} else {
			boolean others = owner != null && !owner.id().equals(a.id());
			if (others && !this.s.may(a, Nodes.HOME_OTHERS)) {
				return OpResult.fail("no_permission");
			} else {
				PlayerRecord rec;
				if (others) {
					rec = owner.record();
				} else {
					Session session = this.s.session(a);
					if (session == null) {
						return OpResult.fail("player_only");
					}

					rec = session.record;
				}

				Home[] out = new Home[rec.homes.size()];

				for (int i = 0; i < out.length; i++) {
					out[i] = Sync.copy(rec.homes.get(i));
				}

				return OpResult.answer(out);
			}
		}
	}

	@Nullable
	static Home pick(PlayerRecord rec, @Nullable String name) {
		if (name != null && !name.isBlank()) {
			return rec.home(name.trim());
		} else {
			Home standard = rec.home("home");
			if (standard != null) {
				return standard;
			} else {
				return rec.homes.size() == 1 ? rec.homes.get(0) : null;
			}
		}
	}

	public OpResult back(Actor a) {
		ServerRules rules = this.s.rules();
		if (!rules.back) {
			return OpResult.fail("disabled");
		} else if (!this.s.may(a, Nodes.BACK_USE)) {
			return OpResult.fail("no_permission");
		} else {
			Session session = this.s.session(a);
			if (session == null) {
				return OpResult.fail("player_only");
			} else {
				PlayerRecord rec = session.record;
				if (rec.back == null) {
					return OpResult.fail("no_back");
				} else {
					boolean death = rec.backIsDeath;
					if (death && !rules.backToDeath) {
						return OpResult.fail("no_back");
					} else if (death && !this.s.may(a, Nodes.BACK_DEATH)) {
						return OpResult.fail("no_permission");
					} else {
						Teleports.Plan plan = new Teleports.Plan(ServerApi.Cause.BACK, "back", rec.back.copy());
						if (death) {
							plan.arrived = () -> {
								rec.death = null;
								rec.deathAt = 0L;
								this.s.touch(rec);
							};
						}

						return this.s.teleports.begin(session, plan);
					}
				}
			}
		}
	}

	Place spawn() {
		Place set = this.s.store.spawn();
		if (set != null) {
			return set;
		} else {
			ServerLevel overworld = this.s.server.overworld();
			BlockPos pos = overworld.getSharedSpawnPos();
			return new Place(
				Level.OVERWORLD.location().toString(), (double)pos.getX() + 0.5, (double)pos.getY(), (double)pos.getZ() + 0.5, overworld.getSharedSpawnAngle(), 0.0F
			);
		}
	}

	public OpResult spawnGo(Actor a) {
		if (!this.s.rules().spawn) {
			return OpResult.fail("disabled");
		} else if (!this.s.may(a, Nodes.SPAWN_USE)) {
			return OpResult.fail("no_permission");
		} else {
			Session session = this.s.session(a);
			return session == null
				? OpResult.fail("player_only")
				: this.s.teleports.begin(session, new Teleports.Plan(ServerApi.Cause.SPAWN, "spawn", this.spawn().copy()));
		}
	}

	public OpResult spawnSet(Actor a) {
		if (!this.s.rules().spawn) {
			return OpResult.fail("disabled");
		} else if (!this.s.may(a, Nodes.SPAWN_SET)) {
			return OpResult.fail("no_permission");
		} else {
			Session session = this.s.session(a);
			Place here = session == null ? null : session.place();
			if (here == null) {
				return OpResult.fail("player_only");
			} else {
				this.s.store.spawn(here);
				if (this.s.live && session.player != null && session.player.level().dimension() == Level.OVERWORLD) {
					this.s.server.overworld().setDefaultSpawnPos(session.player.blockPosition(), here.yaw);
				}

				this.s.sync.markAll("warps");
				this.s.audit(a, "spawn_set", null, "", describe(here));
				return OpResult.ok(describe(here));
			}
		}
	}

	static String describe(Place p) {
		return p.blockX() + " " + p.blockY() + " " + p.blockZ() + " (" + p.dimension + ")";
	}

	public OpResult warpGo(Actor a, String name) {
		if (!this.s.rules().warps) {
			return OpResult.fail("disabled");
		} else if (!this.s.may(a, Nodes.WARP_USE)) {
			return OpResult.fail("no_permission");
		} else {
			Session session = this.s.session(a);
			if (session == null) {
				return OpResult.fail("player_only");
			} else {
				Warp warp = this.s.store.warp(name);
				if (warp != null && (!warp.restricted || this.s.mayNamed(a, warp.node()) || this.s.may(a, Nodes.WARP_SET))) {
					Teleports.Plan plan = new Teleports.Plan(ServerApi.Cause.WARP, "warp:" + warp.name, warp.place.copy());
					plan.arrived = () -> {
						warp.visits++;
						this.s.store.touchWarps();
					};
					OpResult r = this.s.teleports.begin(session, plan);
					return r.ok() && r.args().isEmpty() ? OpResult.ok(warp.name) : r;
				} else {
					return OpResult.fail("no_such_warp", Text.line(name, 24));
				}
			}
		}
	}

	public OpResult warpSet(Actor a, String rawName, @Nullable String icon, @Nullable Integer color, @Nullable String description, @Nullable Boolean restricted) {
		if (!this.s.rules().warps) {
			return OpResult.fail("disabled");
		} else if (!this.s.may(a, Nodes.WARP_SET)) {
			return OpResult.fail("no_permission");
		} else {
			Session session = this.s.session(a);
			Place here = session == null ? null : session.place();
			if (here == null) {
				return OpResult.fail("player_only");
			} else {
				String name = Text.name(rawName);
				if (name == null) {
					return OpResult.fail("name_invalid");
				} else {
					Warp warp = this.s.store.warp(name);
					if (warp == null) {
						if (this.s.store.warps().size() >= 512) {
							return OpResult.fail("invalid");
						}

						warp = new Warp();
						warp.name = name;
						warp.createdBy = a.console() ? Actor.NOBODY : a.id();
						warp.createdByName = a.name();
						warp.createdAt = this.s.now();
						this.s.store.addWarp(warp);
					}

					warp.place = here;
					this.apply(warp, icon, color, description, restricted);
					this.s.store.touchWarps();
					this.s.sync.markAll("warps");
					this.s.audit(a, "warp_set", null, "", warp.name);
					return OpResult.ok(warp.name);
				}
			}
		}
	}

	public OpResult warpEdit(
		Actor a, String name, @Nullable String rename, @Nullable String icon, @Nullable Integer color, @Nullable String description, @Nullable Boolean restricted
	) {
		if (!this.s.rules().warps) {
			return OpResult.fail("disabled");
		} else if (!this.s.may(a, Nodes.WARP_SET)) {
			return OpResult.fail("no_permission");
		} else {
			Warp warp = this.s.store.warp(name);
			if (warp == null) {
				return OpResult.fail("no_such_warp", Text.line(name, 24));
			} else {
				if (rename != null && !rename.equals(warp.name)) {
					String to = Text.name(rename);
					if (to == null) {
						return OpResult.fail("name_invalid");
					}

					Warp other = this.s.store.warp(to);
					if (other != null && other != warp) {
						return OpResult.fail("name_taken");
					}

					warp.name = to;
				}

				this.apply(warp, icon, color, description, restricted);
				this.s.store.touchWarps();
				this.s.sync.markAll("warps");
				this.s.audit(a, "warp_set", null, "", warp.name);
				return OpResult.ok(warp.name);
			}
		}
	}

	private void apply(Warp warp, @Nullable String icon, @Nullable Integer color, @Nullable String description, @Nullable Boolean restricted) {
		if (icon != null) {
			warp.icon = icon(icon);
		}

		if (color != null) {
			warp.color = color & 16777215;
		}

		if (description != null) {
			warp.description = Text.line(description, 120);
		}

		if (restricted != null) {
			warp.restricted = restricted;
		}

		if (warp.restricted && this.s.live) {
			Perms.declare(warp.node(), 2);
		}
	}

	public OpResult warpDelete(Actor a, String name) {
		if (!this.s.rules().warps) {
			return OpResult.fail("disabled");
		} else if (!this.s.may(a, Nodes.WARP_SET)) {
			return OpResult.fail("no_permission");
		} else {
			Warp warp = this.s.store.warp(name);
			if (warp == null) {
				return OpResult.fail("no_such_warp", Text.line(name, 24));
			} else {
				this.s.store.removeWarp(warp);
				this.s.sync.markAll("warps");
				this.s.audit(a, "warp_delete", null, "", warp.name);
				return OpResult.ok(warp.name);
			}
		}
	}

	public OpResult position(Actor a, double x, @Nullable Double y, double z, @Nullable String dimension) {
		if (!this.s.may(a, Nodes.TP_POSITION)) {
			return OpResult.fail("no_permission");
		} else {
			Session session = this.s.session(a);
			Place here = session == null ? null : session.place();
			if (here == null) {
				return OpResult.fail("player_only");
			} else if (Double.isFinite(x) && Double.isFinite(z) && !(Math.abs(x) > 2.9999984E7) && !(Math.abs(z) > 2.9999984E7)) {
				if (y == null || Double.isFinite(y) && !(Math.abs(y) > 4096.0)) {
					String dim = dimension != null && !dimension.isBlank() ? dimension.trim().toLowerCase(Locale.ROOT) : here.dimension;
					if (session.player != null && this.s.teleports.level(dim) == null) {
						return OpResult.fail("invalid");
					} else {
						Teleports.Plan plan = new Teleports.Plan(ServerApi.Cause.POSITION, "position", new Place(dim, x, y == null ? 0.0 : y, z, here.yaw, here.pitch));
						plan.surface = y == null;
						return this.s.teleports.begin(session, plan);
					}
				} else {
					return OpResult.fail("invalid");
				}
			} else {
				return OpResult.fail("invalid");
			}
		}
	}

	public OpResult random(Actor a, @Nullable String dimension) {
		ServerRules rules = this.s.rules();
		if (!rules.randomTeleport) {
			return OpResult.fail("disabled");
		} else if (!this.s.may(a, Nodes.RTP_USE)) {
			return OpResult.fail("no_permission");
		} else {
			Session session = this.s.session(a);
			Place here = session == null ? null : session.place();
			if (here == null) {
				return OpResult.fail("player_only");
			} else {
				String dim = dimension != null && !dimension.isBlank() ? dimension.trim().toLowerCase(Locale.ROOT) : null;
				if (dim == null) {
					dim = rules.randomDimensions.contains(here.dimension) ? here.dimension : (rules.randomDimensions.isEmpty() ? null : rules.randomDimensions.get(0));
				}

				if (dim != null && rules.randomDimensions.contains(dim)) {
					Teleports.Plan plan = new Teleports.Plan(ServerApi.Cause.RANDOM, "rtp", null);
					plan.randomDimension = dim;
					return this.s.teleports.begin(session, plan);
				} else {
					return OpResult.fail("dimension_closed");
				}
			}
		}
	}

	public OpResult cancel(Actor a) {
		Session session = this.s.session(a);
		return session == null ? OpResult.fail("player_only") : this.s.teleports.cancel(session);
	}

	void died(Session session) {
		ServerRules rules = this.s.rules();
		Place here = session.place();
		if (here != null) {
			PlayerRecord rec = session.record;
			this.s.teleports.drop(session);
			rec.death = here;
			rec.deathAt = this.s.now();
			boolean back = rules.back && rules.backToDeath;
			if (back) {
				rec.back = here.copy();
				rec.backIsDeath = true;
			}

			this.s.touch(rec);
			this.s.sync.mark(rec.id, "self");
			if (rules.deathPlace) {
				MutableComponent line = Msg.tr("cfhgui_server.chat.death_place", here.blockX(), here.blockY(), here.blockZ());
				if (back && this.s.may(session.actor(), Nodes.BACK_USE) && this.s.may(session.actor(), Nodes.BACK_DEATH)) {
					line.append(" ").append(button("cfhgui_server.chat.button.back", "/back"));
				}

				this.s.notice(session, "death_place", 15026253, line, here.blockX(), here.blockY(), here.blockZ(), here.dimension);
			}
		}
	}

	static MutableComponent button(String key, String command) {
		return Component.literal("[")
			.append(Msg.tr(key))
			.append("]")
			.withStyle(
				style -> style.withColor(5621992)
						.withClickEvent(new ClickEvent(Action.RUN_COMMAND, command))
						.withHoverEvent(new HoverEvent(net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT, Component.literal(command)))
			);
	}
}
