package com.onehumanawa.cfhcore.content.gui.teams.client.hud;

import com.onehumanawa.cfhcore.content.gui.core.client.CoreActions;
import com.onehumanawa.cfhcore.content.gui.teams.CFHGuiTeams;
import com.onehumanawa.cfhcore.content.gui.teams.TeamsConfig;
import com.onehumanawa.cfhcore.content.gui.teams.client.TeamsState;
import com.onehumanawa.cfhcore.content.gui.teams.model.PingData;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeamData;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeamSummary;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeammateState;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import org.jetbrains.annotations.Nullable;

final class HudDemo {
	static final boolean ON = Boolean.getBoolean("cfh.teams.hudDemo");
	@Nullable
	private static List<TeammateState> mates;
	@Nullable
	private static List<TeamData.Waypoint> waypoints;
	private static final List<TeamsState.ActivePing> PINGS = new ArrayList<>();
	@Nullable
	private static Map<Long, TeamsState.Claim> claims;
	private static String claimsDimension = "";
	private static double baseX;
	private static double baseY;
	private static double baseZ;
	private static float baseYaw;

	@Nullable
	static Collection<TeammateState> mates() {
		return mates;
	}

	@Nullable
	static List<TeamData.Waypoint> waypoints() {
		return waypoints;
	}

	@Nullable
	static TeamsState.Claim claimAt(String dimension, long chunk) {
		Map<Long, TeamsState.Claim> map = claims;
		return map != null && dimension.equals(claimsDimension) ? map.get(chunk) : null;
	}

	static boolean hasClaims() {
		return claims != null;
	}

	@Nullable
	static List<TeamsState.ActivePing> pings() {
		if (PINGS.isEmpty()) {
			return null;
		} else {
			long now = System.currentTimeMillis();
			PINGS.removeIf(p -> now - p.bornMs() > 9000L);
			if (PINGS.isEmpty()) {
				return null;
			} else {
				List<TeamsState.ActivePing> all = new ArrayList<>(PINGS);
				all.addAll(TeamsState.pings());
				return all;
			}
		}
	}

	static void init() {
		if (ON) {
			state("perch", HudDemo::perch);
			state("ground", HudDemo::ground);
			state("border_walk", HudDemo::borderWalk);
			state("border_land", HudDemo::borderLand);
			state("border_show", HudDemo::borderShow);
			state("scene", HudDemo::scene);
			state("hurt", HudDemo::hurt);
			state("fold", () -> TeamsConfig.get().partyMax = 3);
			state("ping", HudDemo::ping);
			state("wheel", () -> PingWheel.demo(1));
			state("wheel_idle", () -> PingWheel.demo(-1));
			state("wheel_send", () -> PingWheel.demoClose(true));
			state("wheel_mouse", () -> {
				PingWheel.demo(-1);
				PingWheel.onMouseLook(40.0, 55.0);
			});
			state("wheel_key", () -> {
				PingWheel.demo(-1);
				PingWheel.onKeyPress(52, 0);
			});
			state("land", () -> {
				TeamData t = TeamsState.team();
				LandNotice.demo(t == null ? null : t.id);
			});
			state("land_other", () -> LandNotice.demo(otherTeam()));
			state("wild", () -> LandNotice.demo(null));
			state("toast", HudDemo::toast);
			state("chat", HudKeys::openTeamChat);
			state("borders", ClaimBorders::show);
			state("reset", HudDemo::reset);
			CFHGuiTeams.LOGGER.info("[CFHGui Teams] HUD demo states registered (cfhgui_teams:hud/...)");
		}
	}

	private static void state(String id, Runnable set) {
		CoreActions.SCREEN_FACTORIES.put("cfhgui_teams:hud/" + id, parent -> {
			try {
				set.run();
			} catch (RuntimeException var4) {
				CFHGuiTeams.LOGGER.error("[CFHGui Teams] HUD demo state {} failed", id, var4);
			}

			Screen opened = Minecraft.getInstance().screen;
			return opened != null ? opened : parent;
		});
	}

	private static UUID id(String name) {
		return UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
	}

	@Nullable
	private static UUID otherTeam() {
		for (TeamSummary s : TeamsState.directory()) {
			if (!TeamsState.isOwn(s.id) && !TeamsState.isAlly(s.id)) {
				return s.id;
			}
		}

		return null;
	}

	private static void perch() {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer p = mc.player;
		if (p != null && mc.level != null) {
			if (p.isCreative() || p.isSpectator()) {
				p.getAbilities().flying = true;
				p.onUpdateAbilities();
			}

			int top = mc.level.getHeight(Types.MOTION_BLOCKING, Mth.floor(p.getX()), Mth.floor(p.getZ()));
			p.connection.sendCommand("tp @s ~ " + (top + 9) + " ~ ~ 24");
		}
	}

	private static void ground() {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer p = mc.player;
		if (p != null && mc.level != null) {
			if (p.isCreative() || p.isSpectator()) {
				p.getAbilities().flying = true;
				p.onUpdateAbilities();
			}

			int ground = ClaimBorders.ground(mc.level, Mth.floor(p.getX()), Mth.floor(p.getZ()), new MutableBlockPos());
			p.connection.sendCommand("tp @s ~ " + ((double)ground + 0.3) + " ~ ~ 6");
		}
	}

	private static void borderWalk() {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer p = mc.player;
		ClientLevel level = mc.level;
		if (p != null && level != null) {
			if (p.isCreative() || p.isSpectator()) {
				p.getAbilities().flying = true;
				p.onUpdateAbilities();
			}

			String dimension = TeamsState.currentDimension();
			MutableBlockPos pos = new MutableBlockPos();
			int pcx = p.chunkPosition().x;
			int pcz = p.chunkPosition().z;
			double best = -1.0E9;
			int bx = 0;
			int by = 0;
			int bz = 0;
			int yaw = 0;

			for (int cx = pcx - 6; cx <= pcx + 6; cx++) {
				for (int cz = pcz - 6; cz <= pcz + 6; cz++) {
					TeamsState.Claim claim = TeamsState.claimAt(dimension, cx, cz);
					if (claim != null && TeamsState.isOwn(claim.team()) && level.hasChunk(cx, cz)) {
						for (int side = 0; side < 4; side++) {
							int ox = side == 0 ? -1 : (side == 1 ? 1 : 0);
							int oz = side == 2 ? -1 : (side == 3 ? 1 : 0);
							TeamsState.Claim next = TeamsState.claimAt(dimension, cx + ox, cz + oz);
							if ((next == null || !next.team().equals(claim.team())) && level.hasChunk(cx + ox, cz + oz)) {
								for (int along = 3; along <= 13; along += 5) {
									int sx = (cx << 4) + (side == 0 ? 8 : (side == 1 ? 7 : along));
									int sz = (cz << 4) + (side == 2 ? 8 : (side == 3 ? 7 : along));
									int stand = ClaimBorders.ground(level, sx, sz, pos);
									if (level.getHeight(Types.MOTION_BLOCKING, sx, sz) <= stand
										&& level.getBlockState(pos.set(sx, stand + 1, sz)).isAir()
										&& level.getBlockState(pos.set(sx, stand + 3, sz)).isAir()) {
										double score = 0.0;

										for (int out = 0; out <= 12; out++) {
											for (int across = -8; across <= 8; across += 2) {
												int x = sx + ox * out + (ox == 0 ? across : 0);
												int z = sz + oz * out + (oz == 0 ? across : 0);
												int ground = ClaimBorders.ground(level, x, z, pos);
												if (ground > stand + 1) {
													score -= 4.0;
												} else if (level.getHeight(Types.MOTION_BLOCKING, x, z) > ground) {
													score--;
												} else if (!level.getFluidState(pos.set(x, ground - 1, z)).isEmpty()) {
													score -= 0.5;
												} else {
													score++;
												}
											}
										}

										if (score > best) {
											best = score;
											bx = sx;
											bz = sz;
											by = stand;
											yaw = side == 0 ? 90 : (side == 1 ? -90 : (side == 2 ? 180 : 0));
										}
									}
								}
							}
						}
					}
				}
			}

			if (best > -1.0E8) {
				p.connection.sendCommand("tp @s " + ((double)bx + 0.5) + " " + ((double)by + 2.2) + " " + ((double)bz + 0.5) + " " + yaw + " 18");
			}
		}
	}

	private static void borderLand() {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer p = mc.player;
		ClientLevel level = mc.level;
		TeamData team = TeamsState.team();
		if (p != null && level != null && team != null) {
			if (p.isCreative() || p.isSpectator()) {
				p.getAbilities().flying = true;
				p.onUpdateAbilities();
			}

			MutableBlockPos pos = new MutableBlockPos();
			int px = Mth.floor(p.getX());
			int pz = Mth.floor(p.getZ());
			double best = -1.0E9;
			int bx = px;
			int bz = pz;

			for (int x = px - 96; x <= px + 96; x += 4) {
				for (int z = pz - 96; z <= pz + 96; z += 4) {
					if (level.hasChunk(x >> 4, z >> 4)) {
						int middle = ClaimBorders.ground(level, x, z, pos);
						double score = 0.0;

						for (int dx = -10; dx <= 10; dx += 2) {
							for (int dz = -10; dz <= 10; dz += 2) {
								if (!level.hasChunk(x + dx >> 4, z + dz >> 4)) {
									score -= 2.0;
								} else {
									int ground = ClaimBorders.ground(level, x + dx, z + dz, pos);
									if (Math.abs(ground - middle) > 2) {
										score -= 2.0;
									} else if (level.getHeight(Types.MOTION_BLOCKING, x + dx, z + dz) > ground) {
										score--;
									} else if (!level.getFluidState(pos.set(x + dx, ground - 1, z + dz)).isEmpty()) {
										score += 0.4;
									} else {
										score++;
									}
								}
							}
						}

						if (score > best) {
							best = score;
							bx = x;
							bz = z;
						}
					}
				}
			}

			int lineX = (bx >> 4 << 4) + (bx - (bx >> 4 << 4) < 8 ? 0 : 16);
			int standX = lineX - 7;
			int stand = ClaimBorders.ground(level, standX, bz, pos);
			p.connection.sendCommand("tp @s " + ((double)standX + 0.5) + " " + ((double)stand + 2.2) + " " + ((double)bz + 0.5) + " -90 16");
		}
	}

	private static void borderShow() {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer p = mc.player;
		TeamData team = TeamsState.team();
		if (p != null && team != null) {
			int cx = p.chunkPosition().x;
			int cz = p.chunkPosition().z;
			Map<Long, TeamsState.Claim> map = new HashMap<>();
			UUID other = null;

			for (TeamSummary s : TeamsState.directory()) {
				if (!TeamsState.isOwn(s.id)) {
					other = s.id;
					break;
				}
			}

			for (int dz = -1; dz <= 1; dz++) {
				for (int dx = -1; dx <= 0; dx++) {
					map.put(ChunkPos.asLong(cx + dx, cz + dz), new TeamsState.Claim(team.id, false));
				}

				if (other != null) {
					map.put(ChunkPos.asLong(cx + 1, cz + dz), new TeamsState.Claim(other, false));
				}
			}

			claims = map;
			claimsDimension = TeamsState.currentDimension();
			ClaimBorders.claimsChanged(claimsDimension);
		}
	}

	private static double[] at(double ahead, double right, double up) {
		float rad = baseYaw * (float) (Math.PI / 180.0);
		double fx = (double)(-Mth.sin(rad));
		double fz = (double)Mth.cos(rad);
		return new double[]{baseX + fx * ahead - fz * right, baseY + up, baseZ + fz * ahead + fx * right};
	}

	private static TeammateState mate(String name, String dimension, double ahead, double right, double up, float health, float max, int armor, int flags) {
		double[] p = at(ahead, right, up);
		return new TeammateState(id(name), dimension, (float)p[0], (float)p[1], (float)p[2], 0.0F, health, max, armor, 20, flags);
	}

	private static void scene() {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer p = mc.player;
		if (p != null && mc.level != null) {
			baseX = p.getX();
			baseY = p.getY();
			baseZ = p.getZ();
			baseYaw = p.getYRot();
			mates = sceneMates(17.0F);
			String here = TeamsState.currentDimension();
			List<TeamData.Waypoint> list = new ArrayList<>();
			list.add(waypoint("Keep", "home", 14254174, here, 30.0, 7.0, true));
			list.add(waypoint("Iron farm", "farm", 10134964, here, 96.0, -34.0, true));
			list.add(waypoint("Deep mine", "mine", 6007008, here, 52.0, 30.0, false));
			list.add(waypoint("Stronghold", "portal", 10192608, here, 1500.0, -420.0, false));
			list.add(waypoint("Fortress", "danger", 15026253, "minecraft:the_nether", 20.0, 0.0, false));
			waypoints = list;
		}
	}

	private static List<TeammateState> sceneMates(float nearHealth) {
		String here = TeamsState.currentDimension();
		List<TeammateState> list = new ArrayList<>();
		list.add(mate("Mossback", here, 20.0, -7.0, -6.0, nearHealth, 20.0F, 15, 0));
		list.add(mate("Quillfeather", "minecraft:the_nether", 3.0, 2.0, 0.0, 8.5F, 20.0F, 20, 0));
		list.add(mate("TinLantern", here, 60.0, 17.0, -2.0, 20.0F, 24.0F, 7, 2));
		list.add(mate("Brackenfoot", here, -34.0, -13.0, -5.0, 4.5F, 20.0F, 0, 0));
		list.add(mate("Saltwick", here, 4.0, 5.0, -7.0, 20.0F, 20.0F, 20, 0));
		list.add(mate("Fennelseed", here, 33.0, 15.0, -7.0, 0.0F, 20.0F, 4, 1));
		list.add(mate("OldHarrow", here, 120.0, -38.0, 2.0, 20.0F, 20.0F, 10, 4));
		return list;
	}

	private static TeamData.Waypoint waypoint(String name, String icon, int color, String dimension, double ahead, double right, boolean onGround) {
		Minecraft mc = Minecraft.getInstance();
		double[] p = at(ahead, right, 0.0);
		TeamData.Waypoint w = new TeamData.Waypoint();
		w.id = "demo_" + icon;
		w.name = name;
		w.dimension = dimension;
		w.x = Mth.floor(p[0]);
		w.z = Mth.floor(p[2]);
		boolean loaded = mc.level != null && mc.level.hasChunk(w.x >> 4, w.z >> 4);
		int ground = loaded ? mc.level.getHeight(Types.MOTION_BLOCKING, w.x, w.z) : Mth.floor(baseY) - 8;
		w.y = onGround ? ground : Mth.floor(baseY) - 30;
		w.color = color;
		w.icon = icon;
		return w;
	}

	private static void hurt() {
		if (mates != null) {
			mates = sceneMates(8.0F);
		}
	}

	private static void ping() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level != null) {
			String here = TeamsState.currentDimension();
			long now = System.currentTimeMillis();
			PINGS.clear();
			addPing("Mossback", here, 15.0, 3.0, 1, now);
			addPing("TinLantern", here, 30.0, -11.0, 2, now - 400L);
			addPing("Brackenfoot", here, -22.0, 9.0, 4, now - 800L);
		}
	}

	private static void addPing(String by, String dimension, double ahead, double right, int kind, long born) {
		Minecraft mc = Minecraft.getInstance();
		double[] p = at(ahead, right, 0.0);
		boolean loaded = mc.level != null && mc.level.hasChunk(Mth.floor(p[0]) >> 4, Mth.floor(p[2]) >> 4);
		int ground = loaded ? mc.level.getHeight(Types.MOTION_BLOCKING, Mth.floor(p[0]), Mth.floor(p[2])) : Mth.floor(baseY) - 8;
		PingData data = new PingData(id(by), dimension, p[0], (double)ground, p[2], kind, -1);
		PINGS.add(new TeamsState.ActivePing(data, born));
		Pings.arrived(data);
	}

	private static void toast() {
		TeamData t = TeamsState.team();
		int color = t == null ? 14254174 : t.color;
		String team = t == null ? "Emberwatch" : t.name;
		TeamsState.NOTICE.invoke(l -> l.accept(new TeamsState.NoticeView("joined", List.of("Cindermoth", team), color)));
		TeamsState.NOTICE.invoke(l -> l.accept(new TeamsState.NoticeView("promoted", List.of("admin", "Mossback"), color)));
		TeamsState.NOTICE.invoke(l -> l.accept(new TeamsState.NoticeView("level", List.of("8", team), color)));
	}

	private static void reset() {
		mates = null;
		waypoints = null;
		PINGS.clear();
		if (claims != null) {
			claims = null;
			ClaimBorders.claimsChanged(claimsDimension);
		}

		PingWheel.demoClose(false);
		TeamsConfig.file().load();
	}

	private HudDemo() {
	}
}
