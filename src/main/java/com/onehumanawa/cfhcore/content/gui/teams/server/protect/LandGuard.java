package com.onehumanawa.cfhcore.content.gui.teams.server.protect;

import com.onehumanawa.cfhcore.content.gui.teams.model.TeamData;
import com.onehumanawa.cfhcore.content.gui.teams.server.ClaimMap;
import com.onehumanawa.cfhcore.content.gui.teams.server.LandPins;
import com.onehumanawa.cfhcore.content.gui.teams.server.Msg;
import com.onehumanawa.cfhcore.content.gui.teams.server.TeamService;
import com.onehumanawa.cfhcore.content.gui.teams.server.TeamsServer;
import com.onehumanawa.cfhcore.content.gui.teams.server.platform.TeamsPlatform;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.Clearable;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.BlockAttachedEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BoatItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PlaceOnWaterBlockItem;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.CakeBlock;
import net.minecraft.world.level.block.DiodeBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import org.jetbrains.annotations.Nullable;

public final class LandGuard {
	public static final int REFUSE_BLOCK = 1;
	public static final int REFUSE_ITEM = 2;

	@Nullable
	private static TeamService active(Level level) {
		if (level.isClientSide()) {
			return null;
		} else {
			TeamService s = TeamsServer.service();
			return s != null && !s.claims().isEmpty() ? s : null;
		}
	}

	@Nullable
	private static ClaimMap.Claim claimAt(TeamService s, Level level, BlockPos pos) {
		return s.claims().at(level.dimension(), pos.getX() >> 4, pos.getZ() >> 4);
	}

	private static boolean refused(TeamService s, Player player, Level level, BlockPos pos, Protection.Kind kind, boolean tell) {
		ClaimMap.Claim c = claimAt(s, level, pos);
		if (c == null) {
			return false;
		} else {
			Protection p = s.protection;
			if (p.bypasses(player)) {
				return false;
			} else {
				TeamData land = p.refuses(player, c.team, kind);
				if (land == null) {
					return false;
				} else {
					if (tell) {
						p.tell(player, land);
					}

					return true;
				}
			}
		}
	}

	public static boolean breakRefused(Player player, Level level, BlockPos pos) {
		TeamService s = active(level);
		if (s != null && refused(s, player, level, pos, Protection.Kind.BUILD, true)) {
			restoreBlockEntity(player, level, pos);
			return true;
		} else {
			return false;
		}
	}

	private static void restoreBlockEntity(Player player, Level level, BlockPos pos) {
		if (player instanceof ServerPlayer sp && sp.connection != null && level.getBlockState(pos).hasBlockEntity()) {
			BlockEntity be = level.getBlockEntity(pos);
			Packet<ClientGamePacketListener> data = be == null ? null : be.getUpdatePacket();
			if (data != null) {
				sp.server.tell(new TickTask(sp.server.getTickCount(), () -> {
					if (sp.connection != null) {
						sp.connection.send(data);
					}
				}));
			}

			return;
		}
	}

	public static boolean placeRefused(@Nullable Entity entity, Level level, BlockPos pos) {
		TeamService s = active(level);
		if (s == null || entity == null) {
			return false;
		} else {
			return entity instanceof Player player
				? refused(s, player, level, pos, Protection.Kind.BUILD, true)
				: entity instanceof Enemy && mobGriefRefused(level, pos);
		}
	}

	public static boolean attackBlockRefused(Player player, Level level, BlockPos pos) {
		TeamService s = active(level);
		return s != null && refused(s, player, level, pos, Protection.Kind.BUILD, true);
	}

	public static boolean buildRefusedQuietly(Player player, Level level, BlockPos pos) {
		TeamService s = active(level);
		return s != null && refused(s, player, level, pos, Protection.Kind.BUILD, false);
	}

	public static int useBlock(Player player, Level level, InteractionHand hand, BlockHitResult hit) {
		TeamService s = active(level);
		if (s == null) {
			return 0;
		} else {
			BlockPos pos = hit.getBlockPos();
			ClaimMap.Claim here = claimAt(s, level, pos);
			BlockPos beside = pos.relative(hit.getDirection());
			ClaimMap.Claim there = sameChunk(pos, beside) ? here : claimAt(s, level, beside);
			if (here == null && there == null) {
				return 0;
			} else {
				Protection p = s.protection;
				if (p.bypasses(player)) {
					return 0;
				} else {
					BlockState state = level.getBlockState(pos);
					int refused = 0;
					TeamData tell = null;
					if (here != null) {
						TeamData noContainers = p.refuses(player, here.team, Protection.Kind.CONTAINERS);
						TeamData noInteract = p.refuses(player, here.team, Protection.Kind.INTERACT);
						TeamData land = noContainers == noInteract ? noInteract : (holdsItems(level, pos, state) ? noContainers : noInteract);
						if (land != null) {
							refused |= 1;
							if (answersClicks(level, pos, state) && !skipsBlockUse(player)) {
								tell = land;
							}
						}
					}

					ItemStack stack = player.getItemInHand(hand);
					if (!stack.isEmpty()) {
						TeamData land = itemRefusal(p, player, stack, state, here, there);
						if (land != null) {
							refused |= 2;
							if (tell == null && stack.getItem() instanceof BlockItem) {
								tell = land;
							}
						}
					}

					if (tell != null) {
						p.tell(player, tell);
					}

					if ((refused & 2) != 0) {
						resync(player, hand);
					}

					return refused;
				}
			}
		}
	}

	public static boolean skipsBlockUse(Player player) {
		return player.isSecondaryUseActive() && (!player.getMainHandItem().isEmpty() || !player.getOffhandItem().isEmpty());
	}

	public static boolean useOnRefused(UseOnContext context) {
		Player player = context.getPlayer();
		Level level = context.getLevel();
		if (player == null) {
			return false;
		} else {
			TeamService s = active(level);
			if (s == null) {
				return false;
			} else {
				BlockPos pos = context.getClickedPos();
				ClaimMap.Claim here = claimAt(s, level, pos);
				BlockPos beside = pos.relative(context.getClickedFace());
				ClaimMap.Claim there = sameChunk(pos, beside) ? here : claimAt(s, level, beside);
				if (here == null && there == null) {
					return false;
				} else {
					Protection p = s.protection;
					if (p.bypasses(player)) {
						return false;
					} else {
						TeamData land = itemRefusal(p, player, context.getItemInHand(), level.getBlockState(pos), here, there);
						if (land == null) {
							return false;
						} else {
							if (context.getItemInHand().getItem() instanceof BlockItem) {
								p.tell(player, land);
							}

							resync(player, context.getHand());
							return true;
						}
					}
				}
			}
		}
	}

	@Nullable
	private static TeamData itemRefusal(
		Protection p, Player who, ItemStack stack, BlockState clicked, @Nullable ClaimMap.Claim here, @Nullable ClaimMap.Claim there
	) {
		if (stack.getItem() instanceof BlockItem) {
			ClaimMap.Claim target = clicked.canBeReplaced() ? here : there;
			return target == null ? null : p.refuses(who, target.team, Protection.Kind.BUILD);
		} else {
			TeamData land = here == null ? null : p.refuses(who, here.team, Protection.Kind.BUILD);
			if (land == null && there != null && there != here) {
				land = p.refuses(who, there.team, Protection.Kind.BUILD);
			}

			return land;
		}
	}

	public static boolean useItemRefused(Player player, Level level, InteractionHand hand) {
		TeamService s = active(level);
		if (s == null) {
			return false;
		} else {
			ItemStack stack = player.getItemInHand(hand);
			boolean bucket = stack.getItem() instanceof BucketItem;
			if (!bucket && !(stack.getItem() instanceof BoatItem) && !(stack.getItem() instanceof PlaceOnWaterBlockItem) && !(stack.getItem() instanceof SpawnEggItem)) {
				return false;
			} else if (s.protection.bypasses(player)) {
				return false;
			} else if (!rayRefused(s, player, level, Fluid.SOURCE_ONLY) && (!bucket || !rayRefused(s, player, level, Fluid.NONE))) {
				return false;
			} else {
				resync(player, hand);
				return true;
			}
		}
	}

	private static boolean rayRefused(TeamService s, Player player, Level level, Fluid fluids) {
		Vec3 eye = player.getEyePosition();
		Vec3 end = eye.add(player.getViewVector(1.0F).scale(player.blockInteractionRange()));
		BlockHitResult hit = level.clip(new ClipContext(eye, end, Block.OUTLINE, fluids, player));
		return hit.getType() != Type.BLOCK
			? false
			: refused(s, player, level, hit.getBlockPos(), Protection.Kind.BUILD, true)
				|| refused(s, player, level, hit.getBlockPos().relative(hit.getDirection()), Protection.Kind.BUILD, true);
	}

	private static void resync(Player player, InteractionHand hand) {
		if (player instanceof ServerPlayer sp && sp.connection != null) {
			int slot = hand == InteractionHand.MAIN_HAND ? sp.getInventory().selected : 40;
			sp.connection.send(new ClientboundContainerSetSlotPacket(-2, 0, slot, sp.getItemInHand(hand).copy()));
			return;
		}
	}

	private static boolean holdsItems(Level level, BlockPos pos, BlockState state) {
		if (!state.hasBlockEntity()) {
			return false;
		} else {
			BlockEntity be = level.getBlockEntity(pos);
			return be == null ? false : be instanceof Container || be instanceof Clearable || TeamsPlatform.get().holdsItems(level, pos, state, be);
		}
	}

	private static boolean answersClicks(Level level, BlockPos pos, BlockState state) {
		if (state.hasBlockEntity() || state.getMenuProvider(level, pos) != null) {
			return true;
		} else if (!state.is(BlockTags.DOORS)
			&& !state.is(BlockTags.TRAPDOORS)
			&& !state.is(BlockTags.FENCE_GATES)
			&& !state.is(BlockTags.BUTTONS)
			&& !state.is(BlockTags.BEDS)
			&& !state.is(BlockTags.ANVIL)
			&& !state.is(BlockTags.CAULDRONS)
			&& !state.is(BlockTags.FLOWER_POTS)
			&& !state.is(BlockTags.CANDLE_CAKES)) {
			net.minecraft.world.level.block.Block b = state.getBlock();
			return b instanceof LeverBlock || b instanceof DiodeBlock || b instanceof NoteBlock || b instanceof CakeBlock;
		} else {
			return true;
		}
	}

	private static boolean sameChunk(BlockPos a, BlockPos b) {
		return a.getX() >> 4 == b.getX() >> 4 && a.getZ() >> 4 == b.getZ() >> 4;
	}

	@Nullable
	private static Protection.Kind entityKind(Entity e, boolean attack) {
		if (e instanceof Player || e instanceof Enemy) {
			return null;
		} else if (e instanceof BlockAttachedEntity || e instanceof ArmorStand || e instanceof EndCrystal) {
			return Protection.Kind.BUILD;
		} else if (e instanceof VehicleEntity) {
			return attack ? Protection.Kind.BUILD : (e instanceof Container ? Protection.Kind.CONTAINERS : Protection.Kind.INTERACT);
		} else {
			return e instanceof LivingEntity ? Protection.Kind.ENTITIES : null;
		}
	}

	private static boolean theirs(Player player, Entity e) {
		return e instanceof OwnableEntity o && player.getUUID().equals(o.getOwnerUUID()) || player.getVehicle() == e;
	}

	public static boolean useEntityRefused(Player player, Entity entity) {
		Level level = entity.level();
		TeamService s = active(level);
		if (s == null) {
			return false;
		} else {
			Protection.Kind kind = entityKind(entity, false);
			return kind != null && !theirs(player, entity) ? refused(s, player, level, entity.blockPosition(), kind, true) : false;
		}
	}

	public static boolean attackEntityRefused(Player player, Entity entity) {
		Level level = entity.level();
		if (level.isClientSide()) {
			return false;
		} else {
			TeamService s = TeamsServer.service();
			if (s == null) {
				return false;
			} else if (entity instanceof Player victim) {
				return pvpRefused(s, player, victim);
			} else if (s.claims().isEmpty()) {
				return false;
			} else {
				Protection.Kind kind = entityKind(entity, true);
				return kind != null && !theirs(player, entity) ? refused(s, player, level, entity.blockPosition(), kind, true) : false;
			}
		}
	}

	private static boolean pvpRefused(TeamService s, Player attacker, Player victim) {
		if (attacker != victim && !s.protection.bypasses(attacker)) {
			TeamData own = s.store().ofPlayer(attacker.getUUID());
			if (own != null && !own.friendlyFire && own.isMember(victim.getUUID())) {
				if (attacker instanceof ServerPlayer sp && sp.connection != null) {
					sp.displayClientMessage(Msg.tr("cfhgui_teams.protect.friendly"), true);
				}

				return true;
			} else if (s.claims().isEmpty()) {
				return false;
			} else {
				LandPins pins = s.pins();
				ClaimMap.Claim at = claimAt(s, victim.level(), victim.blockPosition());
				TeamData land = at != null && !pins.pvp(at.team) ? at.team : null;
				if (land == null) {
					ClaimMap.Claim from = claimAt(s, attacker.level(), attacker.blockPosition());
					if (from != null && !pins.pvp(from.team)) {
						land = from.team;
					}
				}

				if (land == null) {
					return false;
				} else {
					if (attacker instanceof ServerPlayer sp && sp.connection != null) {
						sp.displayClientMessage(Msg.tr(s.isServerLand(land) ? "cfhgui_teams.protect.no_pvp_server" : "cfhgui_teams.protect.no_pvp", Msg.team(land)), true);
					}

					return true;
				}
			}
		} else {
			return false;
		}
	}

	public static boolean damageRefused(Entity victim, DamageSource source) {
		Level level = victim.level();
		if (level.isClientSide()) {
			return false;
		} else {
			TeamService s = TeamsServer.service();
			if (s == null) {
				return false;
			} else {
				Entity cause = source.getEntity();
				Player var10000;
				if (cause instanceof Player p) {
					var10000 = p;
				} else {
					label54: {
						if (cause instanceof OwnableEntity o && o.getOwner() instanceof Player owner) {
							var10000 = owner;
							break label54;
						}

						var10000 = null;
					}
				}

				Player actor = var10000;
				if (actor != null) {
					if (victim instanceof Player hit) {
						return pvpRefused(s, actor, hit);
					} else if (s.claims().isEmpty()) {
						return false;
					} else {
						Protection.Kind kind = entityKind(victim, true);
						return kind != null && !theirs(actor, victim) ? refused(s, actor, level, victim.blockPosition(), kind, true) : false;
					}
				} else if (source.is(DamageTypeTags.IS_EXPLOSION) && !s.claims().isEmpty() && entityKind(victim, true) != null) {
					ClaimMap.Claim c = claimAt(s, level, victim.blockPosition());
					return c != null && !s.pins().explosions(c.team);
				} else {
					return false;
				}
			}
		}
	}

	public static void filterExplosion(Level level, List<BlockPos> blocks, @Nullable List<Entity> entities) {
		TeamService s = active(level);
		if (s != null) {
			ClaimMap.Dim d = s.claims().dim(level.dimension());
			if (d != null && !d.chunks.isEmpty()) {
				LandPins pins = s.pins();
				if (!blocks.isEmpty()) {
					blocks.removeIf(pos -> {
						ClaimMap.Claim c = (ClaimMap.Claim)d.chunks.get(ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4));
						return c != null && !pins.explosions(c.team);
					});
				}

				if (entities != null && !entities.isEmpty()) {
					entities.removeIf(e -> {
						if (entityKind(e, true) == null) {
							return false;
						} else {
							BlockPos pos = e.blockPosition();
							ClaimMap.Claim c = (ClaimMap.Claim)d.chunks.get(ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4));
							return c != null && !pins.explosions(c.team);
						}
					});
				}
			}
		}
	}

	public static boolean mobGriefRefused(Level level, BlockPos pos) {
		TeamService s = active(level);
		if (s == null) {
			return false;
		} else {
			ClaimMap.Claim c = claimAt(s, level, pos);
			return c != null && !s.pins().mobGriefing(c.team);
		}
	}

	public static boolean monsterGriefRefused(Entity entity) {
		Entity actor = entity instanceof Projectile p ? p.getOwner() : entity;
		return actor instanceof Enemy && mobGriefRefused(entity.level(), entity.blockPosition());
	}

	public static boolean trampleRefused(Level level, BlockPos pos, @Nullable Entity entity) {
		TeamService s = active(level);
		if (s == null) {
			return false;
		} else {
			return entity instanceof Player player ? refused(s, player, level, pos, Protection.Kind.BUILD, false) : mobGriefRefused(level, pos);
		}
	}

	public static boolean stepRefused(Level level, BlockPos pos, Entity entity) {
		if (!((entity instanceof Projectile p ? p.getOwner() : entity) instanceof Player player)) {
			return false;
		} else {
			TeamService s = active(level);
			return s != null && refused(s, player, level, pos, Protection.Kind.INTERACT, false);
		}
	}

	public static boolean fireRefused(Level level, BlockPos pos) {
		TeamService s = active(level);
		if (s == null) {
			return false;
		} else {
			ClaimMap.Claim c = claimAt(s, level, pos);
			return c != null && !s.pins().fireSpread(c.team);
		}
	}

	public static boolean reachRefused(Level level, BlockPos from, BlockPos to) {
		if (sameChunk(from, to)) {
			return false;
		} else {
			TeamService s = active(level);
			if (s == null) {
				return false;
			} else {
				ResourceKey<Level> dim = level.dimension();
				ClaimMap.Claim target = s.claims().at(dim, to.getX() >> 4, to.getZ() >> 4);
				return target == null ? false : !s.protection.mayReachInto(s.claims().at(dim, from.getX() >> 4, from.getZ() >> 4), target);
			}
		}
	}

	public static boolean pistonRefused(Level level, BlockPos piston, Direction facing, boolean extending) {
		TeamService s = active(level);
		if (s == null) {
			return false;
		} else {
			ResourceKey<Level> dim = level.dimension();
			ClaimMap.Dim d = s.claims().dim(dim);
			if (d != null && !d.chunks.isEmpty()) {
				int cx = piston.getX() >> 4;
				int cz = piston.getZ() >> 4;
				ClaimMap.Claim own = s.claims().at(dim, cx, cz);
				boolean plain = true;

				for (int dx = -1; dx <= 1 && plain; dx++) {
					for (int dz = -1; dz <= 1 && plain; dz++) {
						ClaimMap.Claim c = s.claims().at(dim, cx + dx, cz + dz);
						if (c != null && (own == null || c.team != own.team)) {
							plain = false;
						}
					}
				}

				if (plain) {
					return false;
				} else {
					Protection p = s.protection;
					BlockPos head = piston.relative(facing);
					if (reaches(p, s, dim, own, head)) {
						return true;
					} else {
						PistonStructureResolver resolver = new PistonStructureResolver(level, piston, facing, extending);
						if (!resolver.resolve()) {
							return false;
						} else {
							Direction push = extending ? facing : facing.getOpposite();

							for (BlockPos pos : resolver.getToPush()) {
								if (reaches(p, s, dim, own, pos) || reaches(p, s, dim, own, pos.relative(push))) {
									return true;
								}
							}

							for (BlockPos posx : resolver.getToDestroy()) {
								if (reaches(p, s, dim, own, posx)) {
									return true;
								}
							}

							return false;
						}
					}
				}
			} else {
				return false;
			}
		}
	}

	private static boolean reaches(Protection p, TeamService s, ResourceKey<Level> dim, @Nullable ClaimMap.Claim own, BlockPos pos) {
		ClaimMap.Claim c = s.claims().at(dim, pos.getX() >> 4, pos.getZ() >> 4);
		return c != null && c != own && !p.mayReachInto(own, c);
	}

	private LandGuard() {
	}
}
