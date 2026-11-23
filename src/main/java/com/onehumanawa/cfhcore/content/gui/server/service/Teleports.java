package com.onehumanawa.cfhcore.content.gui.server.service;

import com.onehumanawa.cfhcore.content.gui.server.Nodes;
import com.onehumanawa.cfhcore.content.gui.server.CFHGuiServer;
import com.onehumanawa.cfhcore.content.gui.server.api.ServerApi;
import com.onehumanawa.cfhcore.content.gui.server.model.Place;
import com.onehumanawa.cfhcore.content.gui.server.model.PlayerRecord;
import com.onehumanawa.cfhcore.content.gui.server.model.ServerRules;
import com.onehumanawa.cfhcore.content.gui.server.permission.ExtraNodes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

final class Teleports {
	static final String COOLDOWN = "tp";
	static final String COOLDOWN_RANDOM = "rtp";
	static final String NOTICE_UNSAFE = "teleport_unsafe";
	static final String NOTICE_NO_PLACE = "teleport_no_place";
	static final int SEARCH_TRIES = 24;
	static final int SEARCHES_AT_ONCE = 8;
	private static final int CANDIDATE_TICKS = 100;
	private static final int CHUNK_TICKS = 200;
	private static final int LANDING_REACH = 8;
	private static final double MOVED_SQUARED = 0.25;
	private static final TicketType<ChunkPos> SEARCHING = TicketType.create("cfhgui_server_random_teleport", Comparator.comparingLong(ChunkPos::toLong), 200);
	private final ServerService s;
	private final RandomSource random = RandomSource.create();

	Teleports(ServerService s) {
		this.s = s;
	}

	OpResult begin(Session session, Teleports.Plan plan) {
		Actor actor = session.actor();
		Place from = session.place();
		if (from == null) {
			return OpResult.fail("player_only");
		} else if (session.pending != null) {
			return OpResult.fail("already_pending");
		} else {
			ServerRules rules = this.s.rules();
			PlayerRecord rec = session.record;
			long now = this.s.now();
			boolean random = plan.randomDimension != null;
			String toDimension = random ? plan.randomDimension : plan.to.dimension;
			String key = random ? "rtp" : "tp";
			int warmup = 0;
			int cooldown = 0;
			if (!plan.staff) {
				if (this.s.moderation.frozen(session)) {
					return OpResult.fail("frozen");
				}

				if (!this.s.may(actor, Nodes.TP_ANYWHERE)) {
					if (rules.noTeleportDimensions.contains(from.dimension) || rules.noTeleportDimensions.contains(toDimension)) {
						return OpResult.fail("dimension_closed");
					}

					if (!rules.crossDimension && !from.dimension.equals(toDimension)) {
						return OpResult.fail("no_cross_dimension");
					}
				}

				Long until = rec.cooldowns.get(key);
				if (until != null && until > now) {
					return OpResult.fail("cooldown", (until - now + 999L) / 1000L);
				}

				warmup = this.s.limit(actor, Nodes.TP_WARMUP, rules.warmupSeconds);
				cooldown = random ? this.s.limit(actor, Nodes.RTP_COOLDOWN, rules.randomCooldownSeconds) : this.s.limit(actor, Nodes.TP_COOLDOWN, rules.cooldownSeconds);
			}

			ServerLevel level = null;
			if (session.player != null) {
				level = this.level(toDimension);
				if (level == null) {
					return OpResult.fail("dimension_closed");
				}
			}

			Teleports.Pending p = new Teleports.Pending(plan, now + (long)warmup * 1000L, warmup, from, key, cooldown);
			if (random) {
				if (level != null) {
					BlockPos center = level.dimension() == Level.OVERWORLD
						? level.getSharedSpawnPos()
						: BlockPos.containing(level.getWorldBorder().getCenterX(), 0.0, level.getWorldBorder().getCenterZ());
					p.search = new Teleports.Search(level, center.getX(), center.getZ(), rules.randomMinDistance, rules.randomMaxDistance);
				} else {
					p.to = new Place(toDimension, from.x + (double)rules.randomMinDistance, from.y, from.z, 0.0F, 0.0F);
				}
			}

			if (warmup == 0 && p.search == null && !p.surface) {
				OpResult done = this.tryNow(session, p);
				if (done != null) {
					return done;
				}
			}

			session.pending = p;
			this.s.sync.mark(session.id, "self");
			return warmup > 0 ? OpResult.warmup(warmup) : OpResult.OK;
		}
	}

	@Nullable
	private OpResult tryNow(Session session, Teleports.Pending p) {
		ServerPlayer player = session.player;
		Place to = p.to;
		if (player == null) {
			this.go(session, p, null, to);
			return OpResult.OK;
		} else {
			ServerLevel level = this.level(to.dimension);
			if (level == null) {
				return OpResult.fail("dimension_closed");
			} else {
				LevelChunk chunk = level.getChunkSource().getChunkNow(to.blockX() >> 4, to.blockZ() >> 4);
				if (chunk == null) {
					return null;
				} else {
					Vec3 landing = this.landing(level, chunk, to, player);
					if (landing == null) {
						return OpResult.fail("unsafe");
					} else {
						this.go(session, p, level, new Place(to.dimension, landing.x, landing.y, landing.z, to.yaw, to.pitch));
						return OpResult.OK;
					}
				}
			}
		}
	}

	OpResult cancel(Session session) {
		if (session.pending == null) {
			return OpResult.fail("nothing_pending");
		} else {
			this.drop(session);
			return OpResult.OK;
		}
	}

	void drop(Session session) {
		if (session.pending != null) {
			if (session.pending.search != null) {
				release(session.pending.search);
			}

			session.pending = null;
			this.s.sync.mark(session.id, "self");
		}
	}

	void hurt(Session session) {
		Teleports.Pending p = session.pending;
		if (p != null && p.warmup > 0 && !p.asked) {
			this.drop(session);
			this.s.notice(session, "cancelled_hurt", 0, Msg.tr("cfhgui_server.chat.cancelled_hurt"));
		}
	}

	private int searching() {
		int n = 0;

		for (Session other : this.s.online()) {
			if (other.pending != null && other.pending.search != null && other.pending.asked) {
				n++;
			}
		}

		return n;
	}

	private void ask(Session session, Teleports.Pending p, long now) {
		if (!p.asked) {
			p.asked = true;
			charge(session.record, p, now);
			this.s.touch(session.record);
			this.s.sync.mark(session.id, "self");
		}
	}

	private static void charge(PlayerRecord rec, Teleports.Pending p, long now) {
		rec.cooldowns.values().removeIf(until -> until <= now);
		if (p.cooldownSeconds > 0) {
			rec.cooldowns.put(p.cooldownKey, now + (long)p.cooldownSeconds * 1000L);
		}
	}

	void tick() {
		if (!this.s.sessions.isEmpty()) {
			List<Session> waiting = null;

			for (Session session : this.s.online()) {
				if (session.pending != null) {
					if (waiting == null) {
						waiting = new ArrayList<>(2);
					}

					waiting.add(session);
				}
			}

			if (waiting != null) {
				long now = this.s.now();

				for (Session sessionx : waiting) {
					if (sessionx.pending != null) {
						try {
							this.tick(sessionx, sessionx.pending, now);
						} catch (RuntimeException var7) {
							CFHGuiServer.LOGGER.error("[CFHGui Server] the teleport of {} failed", sessionx.name, var7);
							this.drop(sessionx);
						}
					}
				}
			}
		}
	}

	private void tick(Session session, Teleports.Pending p, long now) {
		Place here = session.place();
		if (here == null) {
			this.drop(session);
		} else if (p.warmup <= 0 || p.asked || here.dimension.equals(p.from.dimension) && !(distanceSquared(here, p.from) > 0.25)) {
			if (now >= p.at) {
				if (p.search != null && p.to == null) {
					if (!p.asked && this.searching() >= 8) {
						return;
					}

					this.ask(session, p, now);
					if (!this.search(session, p)) {
						return;
					}
				}

				if (p.to != null) {
					ServerPlayer player = session.player;
					if (player == null) {
						this.go(session, p, null, p.to);
					} else {
						Place to = p.to;
						ServerLevel level = this.level(to.dimension);
						if (level == null) {
							this.fail(session, "teleport_unsafe");
						} else {
							int cx = to.blockX() >> 4;
							int cz = to.blockZ() >> 4;
							LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
							if (chunk == null) {
								this.ask(session, p, now);
								if (p.waited % 4 == 0) {
									level.getChunkSource().addRegionTicket(TicketType.POST_TELEPORT, new ChunkPos(cx, cz), 1, player.getId());
								}

								if (++p.waited > 200) {
									this.fail(session, "teleport_unsafe");
								}
							} else {
								Place target = to;
								if (p.surface) {
									Double y = this.spot(level, chunk, to.blockX(), to.blockZ(), true);
									if (y == null) {
										this.fail(session, "teleport_unsafe");
										return;
									}

									target = new Place(to.dimension, (double)to.blockX() + 0.5, y, (double)to.blockZ() + 0.5, to.yaw, to.pitch);
								}

								Vec3 landing = this.landing(level, chunk, target, player);
								if (landing == null) {
									this.fail(session, "teleport_unsafe");
								} else {
									this.go(session, p, level, new Place(target.dimension, landing.x, landing.y, landing.z, target.yaw, target.pitch));
								}
							}
						}
					}
				}
			}
		} else {
			this.drop(session);
			this.s.notice(session, "cancelled_moved", 0, Msg.tr("cfhgui_server.chat.cancelled_moved"));
		}
	}

	private void fail(Session session, String notice) {
		this.drop(session);
		this.s.notice(session, notice, 0, Msg.tr("cfhgui_server.notice." + notice + ".body"));
	}

	private boolean search(Session session, Teleports.Pending p) {
		Teleports.Search q = p.search;
		if (q.held != null) {
			LevelChunk chunk = q.level.getChunkSource().getChunkNow(q.held.x, q.held.z);
			if (chunk == null) {
				if (++q.waited <= 100) {
					return true;
				}

				release(q);
			} else {
				Double y = this.spot(q.level, chunk, q.x, q.z, false);
				release(q);
				if (y != null) {
					p.to = new Place(q.level.dimension().location().toString(), (double)q.x + 0.5, y, (double)q.z + 0.5, this.random.nextFloat() * 360.0F - 180.0F, 0.0F);
					p.search = null;
					return true;
				}
			}
		}

		if (q.tries >= 24) {
			this.fail(session, "teleport_no_place");
			return false;
		} else {
			q.tries++;
			double min = (double)q.min;
			double max = (double)Math.max(q.min + 16, q.max);
			double radius = Math.sqrt(this.random.nextDouble() * (max * max - min * min) + min * min);
			double angle = this.random.nextDouble() * Math.PI * 2.0;
			q.x = q.centerX + (int)Math.round(Math.cos(angle) * radius);
			q.z = q.centerZ + (int)Math.round(Math.sin(angle) * radius);
			q.waited = 0;
			if (!q.level.getWorldBorder().isWithinBounds((double)q.x, (double)q.z)) {
				return true;
			} else {
				q.held = new ChunkPos(q.x >> 4, q.z >> 4);
				q.level.getChunkSource().addRegionTicket(SEARCHING, q.held, 0, q.held);
				return true;
			}
		}
	}

	private static void release(Teleports.Search q) {
		if (q.held != null) {
			q.level.getChunkSource().removeRegionTicket(SEARCHING, q.held, 0, q.held);
			q.held = null;
		}
	}

	private void go(Session session, Teleports.Pending p, @Nullable ServerLevel level, Place to) {
		PlayerRecord rec = session.record;
		Place before = session.place();
		ServerPlayer player = session.player;
		session.pending = null;
		if (player != null && level != null) {
			this.move(player, level, to, this.s.may(session.actor(), ExtraNodes.TP_COMPANIONS));
		} else {
			session.fakePlace = to.copy();
		}

		this.s.moderation.moved(session, to);
		rec.back = before;
		rec.backIsDeath = false;
		charge(rec, p, this.s.now());
		this.s.touch(rec);
		this.s.sync.mark(session.id, "self");
		if (p.arrived != null) {
			p.arrived.run();
		}

		if (this.s.live && player != null && before != null) {
			ServerApi.TELEPORTED.invoke(l -> l.teleported(player, before, to, p.cause));
		}
	}

	boolean now(ServerPlayer player, Place place, ServerApi.Cause cause) {
		Session session = this.s.session(player.getUUID());
		ServerLevel level = this.level(place.dimension);
		if (session != null && level != null) {
			LevelChunk chunk = level.getChunk(place.blockX() >> 4, place.blockZ() >> 4);
			Vec3 landing = this.landing(level, chunk, place, player);
			if (landing == null) {
				return false;
			} else {
				Teleports.Plan plan = new Teleports.Plan(cause, "", place);
				this.drop(session);
				this.go(
					session,
					new Teleports.Pending(plan, this.s.now(), 0, session.place(), "tp", 0),
					level,
					new Place(place.dimension, landing.x, landing.y, landing.z, place.yaw, place.pitch)
				);
				return true;
			}
		} else {
			return false;
		}
	}

	private void move(ServerPlayer player, ServerLevel level, Place to, boolean companions) {
		Entity mount = null;
		List<Entity> leashed = new ArrayList<>(0);
		if (companions) {
			Entity vehicle = player.getVehicle();
			if (vehicle instanceof LivingEntity && !(vehicle instanceof ServerPlayer) && vehicle.getPassengers().size() == 1 && vehicle.getVehicle() == null) {
				mount = vehicle;
			}

			for (Entity e : player.serverLevel().getEntities(player, player.getBoundingBox().inflate(12.0), x -> {
				if (x instanceof Leashable lx && lx.getLeashHolder() == player) {
					return true;
				}

				return false;
			})) {
				if (e != mount) {
					leashed.add(e);
				}
			}
		}

		if (player.isSleeping()) {
			player.stopSleepInBed(true, true);
		}

		level.getChunkSource().addRegionTicket(TicketType.POST_TELEPORT, new ChunkPos(BlockPos.containing(to.x, to.y, to.z)), 1, player.getId());
		player.teleportTo(level, to.x, to.y, to.z, to.yaw, to.pitch);
		player.setDeltaMovement(Vec3.ZERO);
		player.resetFallDistance();

		try {
			if (mount != null) {
				Entity there = follow(mount, level, to);
				if (there != null) {
					player.startRiding(there, true);
				}
			}

			for (Entity ex : leashed) {
				Entity there = follow(ex, level, to);
				if (there instanceof Leashable) {
					Leashable l = (Leashable)there;
					if (l.getLeashHolder() != player) {
						l.setLeashedTo(player, true);
					}
				}
			}
		} catch (RuntimeException var11) {
			CFHGuiServer.LOGGER.warn("[CFHGui Server] an animal could not follow {}: {}", player.getGameProfile().getName(), var11.toString());
		}
	}

	@Nullable
	private static Entity follow(Entity e, ServerLevel level, Place to) {
		if (e.level() == level) {
			e.teleportTo(level, to.x, to.y, to.z, Set.of(), e.getYRot(), e.getXRot());
			e.resetFallDistance();
			return e;
		} else {
			return !e.canChangeDimensions(e.level(), level)
				? null
				: e.changeDimension(new DimensionTransition(level, new Vec3(to.x, to.y, to.z), Vec3.ZERO, e.getYRot(), e.getXRot(), DimensionTransition.DO_NOTHING));
		}
	}

	@Nullable
	ServerLevel level(String dimension) {
		ResourceLocation id = ResourceLocation.tryParse(dimension);
		return id == null ? null : this.s.server.getLevel(ResourceKey.create(Registries.DIMENSION, id));
	}

	@Nullable
	Vec3 landing(ServerLevel level, BlockGetter chunk, Place to, ServerPlayer player) {
		if (!level.getWorldBorder().isWithinBounds(to.x, to.z)) {
			return null;
		} else {
			int min = level.getMinBuildHeight();
			int max = level.getMaxBuildHeight();
			boolean carefree = player.isCreative()
				|| player.isSpectator()
				|| player.getAbilities().invulnerable
				|| player.getAbilities().flying
				|| this.s.moderation.god(player);
			if (this.s.rules().safeLanding && !carefree) {
				BlockPos base = BlockPos.containing(to.x, to.y, to.z);

				for (int i = 0; i <= 16; i++) {
					int dy = i == 0 ? 0 : (i % 2 == 1 ? (i + 1) / 2 : -(i / 2));
					BlockPos feet = base.above(dy);
					if (feet.getY() > min && feet.getY() < max - 1) {
						double y = standable(chunk, feet, false);
						if (!Double.isNaN(y)) {
							return new Vec3(to.x, y, to.z);
						}
					}
				}

				return null;
			} else {
				return new Vec3(to.x, Math.max((double)min, Math.min((double)(max + 64), to.y)), to.z);
			}
		}
	}

	private static double standable(BlockGetter world, BlockPos feet, boolean dry) {
		BlockState at = world.getBlockState(feet);
		BlockState head = world.getBlockState(feet.above());
		if (free(world, feet, at) && free(world, feet.above(), head) && head.getFluidState().isEmpty()) {
			BlockPos floor = feet.below();
			BlockState below = world.getBlockState(floor);
			if (!at.getFluidState().isEmpty()) {
				return dry ? Double.NaN : (double)feet.getY();
			} else if (!harmful(below) && (!dry || below.getFluidState().isEmpty())) {
				VoxelShape shape = below.getCollisionShape(world, floor);
				if (shape.isEmpty()) {
					return Double.NaN;
				} else {
					double top = shape.max(Axis.Y);
					return top > 1.0 && !free(world, feet.above(2), world.getBlockState(feet.above(2))) ? Double.NaN : (double)floor.getY() + top;
				}
			} else {
				return Double.NaN;
			}
		} else {
			return Double.NaN;
		}
	}

	private static boolean free(BlockGetter world, BlockPos pos, BlockState state) {
		return state.getCollisionShape(world, pos).isEmpty() && !harmful(state);
	}

	private static boolean harmful(BlockState state) {
		return state.getFluidState().is(FluidTags.LAVA)
			|| state.is(BlockTags.FIRE)
			|| state.is(Blocks.MAGMA_BLOCK)
			|| state.is(Blocks.CACTUS)
			|| state.is(Blocks.POWDER_SNOW)
			|| state.is(Blocks.SWEET_BERRY_BUSH)
			|| state.is(Blocks.WITHER_ROSE)
			|| state.is(Blocks.POINTED_DRIPSTONE)
			|| state.is(Blocks.NETHER_PORTAL)
			|| state.is(Blocks.END_PORTAL)
			|| state.is(Blocks.END_GATEWAY)
			|| state.is(BlockTags.CAMPFIRES) && state.hasProperty(CampfireBlock.LIT) && (Boolean)state.getValue(CampfireBlock.LIT);
	}

	@Nullable
	Double spot(ServerLevel level, ChunkAccess chunk, int x, int z, boolean wet) {
		int min = level.getMinBuildHeight();
		if (level.dimensionType().hasCeiling()) {
			int top = Math.min(level.getMaxBuildHeight(), min + level.getLogicalHeight()) - 2;

			for (int y = top - 4; y > min + 1; y--) {
				double at = standable(chunk, new BlockPos(x, y, z), true);
				if (!Double.isNaN(at)) {
					return at;
				}
			}

			return null;
		} else {
			int yx = chunk.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, x & 15, z & 15) + 1;
			if (yx > min && yx < level.getMaxBuildHeight() - 1) {
				BlockPos feet = new BlockPos(x, yx, z);
				BlockState below = chunk.getBlockState(feet.below());
				if (!below.getFluidState().isEmpty()) {
					return wet && !below.getFluidState().is(FluidTags.LAVA) && free(chunk, feet, chunk.getBlockState(feet)) ? (double)yx : null;
				} else if (below.is(BlockTags.LEAVES)) {
					return null;
				} else {
					double at = standable(chunk, feet, true);
					return Double.isNaN(at) ? null : at;
				}
			} else {
				return null;
			}
		}
	}

	private static double distanceSquared(Place a, Place b) {
		double dx = a.x - b.x;
		double dy = a.y - b.y;
		double dz = a.z - b.z;
		return dx * dx + dy * dy + dz * dz;
	}

	static final class Pending {
		final ServerApi.Cause cause;
		final String label;
		final long at;
		final int warmup;
		final Place from;
		final String cooldownKey;
		final int cooldownSeconds;
		@Nullable
		Place to;
		boolean surface;
		@Nullable
		final Runnable arrived;
		@Nullable
		Teleports.Search search;
		int waited;
		boolean asked;

		Pending(Teleports.Plan plan, long at, int warmup, Place from, String cooldownKey, int cooldownSeconds) {
			this.cause = plan.cause;
			this.label = plan.label;
			this.at = at;
			this.warmup = warmup;
			this.from = from;
			this.cooldownKey = cooldownKey;
			this.cooldownSeconds = cooldownSeconds;
			this.to = plan.to;
			this.surface = plan.surface;
			this.arrived = plan.arrived;
		}
	}

	static final class Plan {
		final ServerApi.Cause cause;
		final String label;
		@Nullable
		Place to;
		boolean staff;
		boolean surface;
		@Nullable
		String randomDimension;
		@Nullable
		Runnable arrived;

		Plan(ServerApi.Cause cause, String label, @Nullable Place to) {
			this.cause = cause;
			this.label = label;
			this.to = to;
		}
	}

	static final class Search {
		final ServerLevel level;
		final int centerX;
		final int centerZ;
		final int min;
		final int max;
		int tries;
		int x;
		int z;
		int waited;
		@Nullable
		ChunkPos held;

		Search(ServerLevel level, int centerX, int centerZ, int min, int max) {
			this.level = level;
			this.centerX = centerX;
			this.centerZ = centerZ;
			this.min = min;
			this.max = max;
		}
	}
}
