package com.onehumanawa.cfhcore.content.gui.crafting.team;

import com.google.gson.JsonObject;
import com.onehumanawa.cfhcore.content.gui.core.event.CFHGuiEvents;
import com.onehumanawa.cfhcore.content.gui.core.net.JsonWire;
import com.onehumanawa.cfhcore.content.gui.crafting.CFHGuiCrafting;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import org.jetbrains.annotations.Nullable;

public final class TeamPinsServer {
	private static final int PER_PAYLOAD = 16;
	private static final int SAVE_AFTER = 40;
	private static TeamsLink teams;
	@Nullable
	private static MinecraftServer server;
	@Nullable
	private static PinStore store;
	private static int saveIn;

	static void init(TeamsLink link) {
		teams = link;
		CFHGuiEvents.SERVER_STARTED.register(TeamPinsServer::started);
		CFHGuiEvents.SERVER_STOPPING.register(TeamPinsServer::stopping);
		CFHGuiEvents.SERVER_TICK_END.register(TeamPinsServer::tick);
		CFHGuiEvents.PLAYER_JOINED.register(TeamPinsServer::sync);
		link.listen((team, player) -> sync(player), (team, player) -> sync(player), TeamPinsServer::disbanded);
	}

	private static void started(MinecraftServer started) {
		server = started;
		store = new PinStore(started.getWorldPath(LevelResource.ROOT).resolve("slate").resolve("crafting").resolve("team-pins.json"));
		store.load();
	}

	private static void stopping(MinecraftServer stopping) {
		PinStore s = store;
		store = null;
		server = null;
		if (s != null) {
			s.save();
		}
	}

	private static void tick(MinecraftServer ticking) {
		PinStore s = store;
		if (s != null && s.dirty()) {
			if (++saveIn >= 40) {
				saveIn = 0;
				s.save();
			}
		}
	}

	private static void sync(UUID player) {
		MinecraftServer srv = server;
		ServerPlayer online = srv == null ? null : srv.getPlayerList().getPlayer(player);
		if (online != null) {
			sync(online);
		}
	}

	private static void sync(ServerPlayer player) {
		PinStore s = store;
		if (s != null && TeamPinsNet.has(player)) {
			TeamsLink.TeamInfo team = teams.teamOf(player.getUUID());
			TeamPinsNet.send(player, new TeamPinsNet.Reset(s.world(), team == null ? "" : team.id().toString()));
			if (team != null) {
				List<Pin> pins = s.pins(team.id());

				for (int from = 0; from < pins.size(); from += 16) {
					List<Pin> part = pins.subList(from, Math.min(pins.size(), from + 16));
					TeamPinsNet.send(player, new TeamPinsNet.Put(true, JsonWire.encode(part.toArray(new Pin[0]))));
				}
			}
		}
	}

	private static void tell(TeamsLink.TeamInfo team, CustomPacketPayload payload) {
		MinecraftServer srv = server;
		if (srv != null) {
			for (UUID member : team.members()) {
				ServerPlayer online = srv.getPlayerList().getPlayer(member);
				if (online != null) {
					TeamPinsNet.send(online, payload);
				}
			}
		}
	}

	private static void disbanded(UUID team) {
		PinStore s = store;
		if (s != null) {
			s.drop(team);
		}
	}

	static void onAsk(ServerPlayer player, TeamPinsNet.Ask ask) {
		String refusal;
		try {
			refusal = act(player, ask);
		} catch (RuntimeException var4) {
			CFHGuiCrafting.LOGGER.error("[CFHGui Crafting] the team pin request {} of {} failed", new Object[]{ask.op(), player.getGameProfile().getName(), var4});
			refusal = "invalid";
		}

		TeamPinsNet.send(player, new TeamPinsNet.Answer(ask.request(), refusal == null, refusal == null ? "ok" : refusal));
	}

	@Nullable
	private static String act(ServerPlayer player, TeamPinsNet.Ask ask) {
		PinStore s = store;
		if (s == null) {
			return "no_team";
		} else {
			UUID me = player.getUUID();
			TeamsLink.TeamInfo team = teams.teamOf(me);
			String var5 = ask.op();

			return switch (var5) {
				case "pin_recipe" -> {
					Pin pin = new Pin();
					pin.kind = "recipe";
					pin.recipe = ask.subject();
					yield add(s, team, ask.team(), pin, player);
				}
				case "pin_tree" -> {
					if (ask.tree().length != 0 && ask.tree().length <= 30000) {
						JsonObject tree = (JsonObject)JsonWire.decode(ask.tree(), JsonObject.class);
						if (tree == null) {
							yield "invalid";
						} else {
							Pin pin = new Pin();
							pin.kind = "tree";
							pin.recipe = ask.subject();
							pin.tree = tree;
							pin.steps = ask.steps();
							yield add(s, team, ask.team(), pin, player);
						}
					} else {
						yield "too_big";
					}
				}
				case "unpin" -> {
					Pin pin = team == null ? null : find(s.pins(team.id()), ask.subject());
					String refusal = PinRules.refuseUnpin(team, ask.team(), me, pin);
					if (refusal != null) {
						yield refusal;
					} else {
						s.remove(team.id(), pin);
						saveIn = 0;
						tell(team, new TeamPinsNet.Gone(pin.id));
						yield null;
					}
				}
				default -> "invalid";
			};
		}
	}

	@Nullable
	private static String add(PinStore s, @Nullable TeamsLink.TeamInfo team, String askedTeam, Pin pin, ServerPlayer player) {
		MinecraftServer srv = server;
		if (srv == null) {
			return "no_team";
		} else {
			List<Pin> pins = team == null ? List.of() : s.pins(team.id());
			String refusal = PinRules.refusePin(team, askedTeam, pins, pin, id -> recipeOfServer(srv, id));
			if (refusal != null) {
				return refusal;
			} else {
				pin.id = UUID.randomUUID().toString();
				pin.by = player.getUUID();
				pin.byName = player.getGameProfile().getName();
				pin.at = System.currentTimeMillis();
				s.add(team.id(), pin);
				saveIn = 0;
				tell(team, new TeamPinsNet.Put(false, JsonWire.encode(new Pin[]{pin})));
				return null;
			}
		}
	}

	private static boolean recipeOfServer(MinecraftServer srv, ResourceLocation id) {
		return srv.getRecipeManager().byKey(id).isPresent();
	}

	@Nullable
	private static Pin find(List<Pin> pins, String id) {
		for (Pin p : pins) {
			if (p.id.equals(id)) {
				return p;
			}
		}

		return null;
	}

	public static String devPin(UUID player, String recipe, String byName) {
		PinStore s = store;
		MinecraftServer srv = server;
		if (s == null || srv == null) {
			return "no server";
		} else if (System.getProperty("cfh.autoScreens") == null && !Boolean.getBoolean("cfh.autoWorld")) {
			return "not a harness run";
		} else {
			TeamsLink.TeamInfo team = teams.teamOf(player);
			if (team == null) {
				return "no_team";
			} else {
				Pin pin = new Pin();
				pin.recipe = recipe;
				String refusal = PinRules.refusePin(team, team.id().toString(), s.pins(team.id()), pin, id -> recipeOfServer(srv, id));
				if (refusal != null) {
					return refusal;
				} else {
					pin.id = UUID.randomUUID().toString();
					pin.by = UUID.nameUUIDFromBytes(("cfhgui-dev-member:" + byName).getBytes(StandardCharsets.UTF_8));
					pin.byName = byName;
					pin.at = System.currentTimeMillis() - 420000L;
					s.add(team.id(), pin);
					tell(team, new TeamPinsNet.Put(false, JsonWire.encode(new Pin[]{pin})));
					return "pinned";
				}
			}
		}
	}

	private TeamPinsServer() {
	}
}
