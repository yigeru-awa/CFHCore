package com.onehumanawa.cfhcore.content.gui.quests.server;

import com.onehumanawa.cfhcore.content.gui.quests.QuestsServerConfig;
import com.onehumanawa.cfhcore.content.gui.quests.api.QuestsApi;
import com.onehumanawa.cfhcore.content.gui.quests.logic.BookIndex;
import com.onehumanawa.cfhcore.content.gui.quests.model.Chapter;
import com.onehumanawa.cfhcore.content.gui.quests.model.Reward;
import com.onehumanawa.cfhcore.content.gui.quests.model.Task;
import com.onehumanawa.cfhcore.content.gui.quests.net.QuestsNet;
import java.util.function.Consumer;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.CriterionProgress;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import org.jetbrains.annotations.Nullable;

final class LiveWorld implements World {
	private static final double SIGHT = 16.0;
	private final MinecraftServer server;
	private final TeamLink teams;

	LiveWorld(MinecraftServer server, TeamLink teams) {
		this.server = server;
		this.teams = teams;
	}

	private ServerPlayer player(Seat seat) {
		ServerPlayer player = seat.player;
		if (player.isRemoved()) {
			ServerPlayer current = this.server.getPlayerList().getPlayer(seat.id);
			if (current != null && current != player) {
				player = current;
				seat.player = current;
			}
		}

		return player;
	}

	@Override
	public long now() {
		return System.currentTimeMillis();
	}

	@Override
	public QuestsServerConfig config() {
		return QuestsServerConfig.get();
	}

	@Override
	public TeamLink teams() {
		return this.teams;
	}

	@Override
	public Object datapacks() {
		return this.server.getAdvancements();
	}

	@Override
	public boolean hasClient(Seat seat) {
		return QuestsNet.has(this.player(seat));
	}

	@Override
	public void send(Seat seat, CustomPacketPayload payload) {
		QuestsNet.send(this.player(seat), payload);
	}

	@Override
	public void tell(Seat seat, Component message) {
		this.player(seat).sendSystemMessage(message);
	}

	@Override
	public void broadcast(Component message) {
		this.server.getPlayerList().broadcastSystemMessage(message, false);
	}

	@Override
	public Component displayName(Seat seat) {
		return this.player(seat).getDisplayName();
	}

	@Override
	public boolean announcesAdvancements() {
		return this.server.getGameRules().getBoolean(GameRules.RULE_ANNOUNCE_ADVANCEMENTS);
	}

	@Override
	public boolean mayEdit(Seat seat) {
		return QuestsServer.mayEdit(this.player(seat));
	}

	@Override
	public void inventory(Seat seat, Consumer<ItemStack> each) {
		Inventory inventory = this.player(seat).getInventory();
		int i = 0;

		for (int n = inventory.getContainerSize(); i < n; i++) {
			ItemStack stack = inventory.getItem(i);
			if (!stack.isEmpty()) {
				each.accept(stack);
			}
		}
	}

	@Override
	public long statistic(Seat seat, Stat<?> stat) {
		return (long)this.player(seat).getStats().getValue(stat);
	}

	@Override
	public boolean advancement(Seat seat, ResourceLocation id, String criterion) {
		AdvancementHolder holder = this.server.getAdvancements().get(id);
		if (holder == null) {
			return false;
		} else {
			AdvancementProgress progress = this.player(seat).getAdvancements().getOrStartProgress(holder);
			if (criterion.isEmpty()) {
				return progress.isDone();
			} else {
				CriterionProgress one = progress.getCriterion(criterion);
				return one != null && one.isDone();
			}
		}
	}

	@Override
	public boolean stage(Seat seat, String stage) {
		return Mods.hasStage(this.player(seat), stage);
	}

	@Override
	public long fluid(Seat seat, Specs.Named fluid) {
		return Carried.fluid(this.player(seat), f -> fluid.matches(f.builtInRegistryHolder(), Registries.FLUID));
	}

	@Override
	public long drain(Seat seat, Specs.Named fluid, long amount) {
		return Carried.drain(this.player(seat), f -> fluid.matches(f.builtInRegistryHolder(), Registries.FLUID), amount);
	}

	@Override
	public long takeEnergy(Seat seat, long amount) {
		return Carried.takeEnergy(this.player(seat), amount);
	}

	@Override
	public void bookSent(Seat seat) {
		QuestsServer.sendWords(this.player(seat));
	}

	@Override
	public int experience(Seat seat, boolean points) {
		return points ? points(this.player(seat)) : this.player(seat).experienceLevel;
	}

	static int points(Player player) {
		int level = player.experienceLevel;
		long forLevels = level <= 16
			? (long)level * (long)level + 6L * (long)level
			: (
				level <= 31
					? Math.round(2.5 * (double)level * (double)level - 40.5 * (double)level + 360.0)
					: Math.round(4.5 * (double)level * (double)level - 162.5 * (double)level + 2220.0)
			);
		return (int)Math.min(2147483647L, forLevels + (long)Math.round(player.experienceProgress * (float)player.getXpNeededForNextLevel()));
	}

	@Override
	public boolean at(Seat seat, TriggerIndex.Ref place) {
		ServerPlayer player = this.player(seat);
		ServerLevel level = player.serverLevel();
		Task t = place.task;
		if (place.id != null && !level.dimension().location().equals(place.id)) {
			return false;
		} else {
			BlockPos pos = player.blockPosition();
			if (t.box()) {
				if (pos.getX() < t.x || pos.getX() >= t.x + t.sizeX || pos.getY() < t.y || pos.getY() >= t.y + t.sizeY || pos.getZ() < t.z || pos.getZ() >= t.z + t.sizeZ) {
					return false;
				}
			} else if (t.radius > 0 && player.distanceToSqr((double)t.x + 0.5, (double)t.y + 0.5, (double)t.z + 0.5) > (double)t.radius * (double)t.radius) {
				return false;
			}

			if (place.biome != null && !place.biome.matches(level.getBiome(pos), Registries.BIOME)) {
				return false;
			} else {
				if (place.structure != null) {
					if (!level.isLoaded(pos)) {
						return false;
					}

					if (!level.structureManager().getStructureWithPieceAt(pos, s -> place.structure.matches(s, Registries.STRUCTURE)).isValid()) {
						return false;
					}
				}

				return true;
			}
		}
	}

	@Nullable
	@Override
	public World.Sight sight(Seat seat) {
		ServerPlayer player = this.player(seat);
		Vec3 eye = player.getEyePosition();
		Vec3 reach = player.getViewVector(1.0F).scale(16.0);
		Vec3 end = eye.add(reach);
		BlockHitResult block = player.level().clip(new ClipContext(eye, end, Block.OUTLINE, Fluid.NONE, player));
		boolean hit = block.getType() != Type.MISS;
		double within = hit ? block.getLocation().distanceToSqr(eye) : 256.0;
		AABB box = player.getBoundingBox().expandTowards(reach).inflate(1.0);
		EntityHitResult entity = ProjectileUtil.getEntityHitResult(player, eye, end, box, e -> !e.isSpectator() && e.isPickable(), within);
		if (entity != null) {
			return new World.Sight(null, entity.getEntity().getType());
		} else if (!hit) {
			return null;
		} else {
			net.minecraft.world.level.block.Block looked = player.level().getBlockState(block.getBlockPos()).getBlock();
			return new World.Sight(looked, null);
		}
	}

	@Override
	public long take(Seat seat, Specs.ItemSpec items, long count) {
		Inventory inventory = this.player(seat).getInventory();
		long left = count;
		int i = 0;

		for (int n = inventory.getContainerSize(); i < n && left > 0L; i++) {
			if (i < inventory.items.size() || i >= inventory.items.size() + inventory.armor.size()) {
				ItemStack stack = inventory.getItem(i);
				if (items.test(stack)) {
					int taken = (int)Math.min(left, (long)stack.getCount());
					stack.shrink(taken);
					left -= (long)taken;
					if (stack.isEmpty()) {
						inventory.setItem(i, ItemStack.EMPTY);
					}
				}
			}
		}

		if (left < count) {
			inventory.setChanged();
			this.player(seat).containerMenu.broadcastChanges();
		}

		return count - left;
	}

	@Override
	public void pay(Seat seat, int amount, boolean points) {
		if (amount > 0) {
			if (points) {
				this.player(seat).giveExperiencePoints(-amount);
			} else {
				this.player(seat).giveExperienceLevels(-amount);
			}
		}
	}

	@Override
	public void give(Seat seat, Reward reward, String quest) {
		boolean team = this.teams.isTeam(seat.team.id);
		Rewards.give(
			this.server,
			this.player(seat),
			reward,
			new Rewards.About(
				this.teams.name(seat.team.id),
				seat.team.id.toString(),
				team ? this.teams.figure(seat.team.id, "members") : 1L,
				(long)Math.max(1, seat.team.seats.size()),
				quest
			)
		);
	}

	@Override
	public void questDone(Seat doer, BookIndex.Entry entry) {
		QuestsApi.QUEST_DONE.invoke(l -> l.done(this.player(doer), entry.key()));
	}

	@Override
	public void chapterDone(Seat seat, Chapter chapter) {
		QuestsApi.CHAPTER_DONE.invoke(l -> l.done(this.player(seat), chapter.id));
	}
}
