package com.onehumanawa.cfhcore.content.gui.teams.server;

import com.onehumanawa.cfhcore.content.gui.teams.CFHGuiTeams;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeamData;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

final class Stashes {
	private static final long SAVE_AFTER_MS = 5000L;
	private final TeamService teams;
	private final Map<UUID, Stashes.Stash> loaded = new HashMap<>();

	Stashes(TeamService teams) {
		this.teams = teams;
	}

	void open(ServerPlayer player, TeamData team) {
		Stashes.Stash stash = this.stash(team);
		int rows = stash.getContainerSize() / 9;
		player.openMenu(
			new SimpleMenuProvider((id, inventory, p) -> new ChestMenu(menuType(rows), id, inventory, stash, rows), Msg.tr("cfhgui_teams.stash.title", team.name))
		);
	}

	void closeFor(ServerPlayer player, TeamData team) {
		Stashes.Stash stash = this.loaded.get(team.id);
		if (stash != null && player.containerMenu instanceof ChestMenu menu && menu.getContainer() == stash) {
			player.closeContainer();
		}
	}

	void dissolve(TeamData team, @Nullable ServerPlayer at) {
		if (this.teams.live) {
			Path file = this.file(team.id);
			boolean onDisk = file != null && Files.isRegularFile(file);
			if (this.loaded.containsKey(team.id) || onDisk) {
				if (at == null) {
					Stashes.Stash stash = this.loaded.remove(team.id);
					if (stash != null && stash.dirty) {
						this.save(stash);
					}

					if (onDisk || stash != null) {
						CFHGuiTeams.LOGGER.info("[CFHGui Teams] the stash of the disbanded team {} was kept as {}", team.name, file);
					}
				} else {
					Stashes.Stash stashx = this.stash(team);
					this.loaded.remove(team.id);

					for (ServerPlayer p : this.teams.server.getPlayerList().getPlayers()) {
						AbstractContainerMenu var9 = p.containerMenu;
						if (var9 instanceof ChestMenu) {
							ChestMenu menu = (ChestMenu)var9;
							if (menu.getContainer() == stashx) {
								p.closeContainer();
							}
						}
					}

					Containers.dropContents(at.level(), at, stashx);
					if (file != null && this.teams.saver != null) {
						this.teams.saver.delete(file);
					}
				}
			}
		}
	}

	void tick() {
		if (!this.loaded.isEmpty()) {
			long now = System.currentTimeMillis();

			for (Stashes.Stash s : this.loaded.values()) {
				if (s.dirty && now - s.dirtySince >= 5000L) {
					this.save(s);
				}
			}
		}
	}

	void saveAll() {
		for (Stashes.Stash s : this.loaded.values()) {
			if (s.dirty) {
				this.save(s);
			}
		}
	}

	private Stashes.Stash stash(TeamData team) {
		Stashes.Stash known = this.loaded.get(team.id);
		if (known != null) {
			return known;
		} else {
			int wanted = Math.max(1, Math.min(6, this.teams.cfg().stashRows)) * 9;
			CompoundTag tag = null;
			Path file = this.file(team.id);
			if (file != null && Files.isRegularFile(file)) {
				try {
					tag = NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
				} catch (Exception var10) {
					CFHGuiTeams.LOGGER.error("[CFHGui Teams] the stash {} could not be read; it opens empty and the file is left alone until something is put in", file, var10);
				}
			}

			int size = Math.max(wanted, tag == null ? 0 : Math.min(54, (tag.getInt("Size") + 8) / 9 * 9));
			Stashes.Stash stash = new Stashes.Stash(team.id, size);
			if (tag != null) {
				NonNullList<ItemStack> items = NonNullList.withSize(size, ItemStack.EMPTY);
				ContainerHelper.loadAllItems(tag, items, this.teams.server.registryAccess());

				for (int i = 0; i < size; i++) {
					stash.getItems().set(i, (ItemStack)items.get(i));
				}
			}

			this.loaded.put(team.id, stash);
			return stash;
		}
	}

	private void save(Stashes.Stash stash) {
		stash.dirty = false;
		Path file = this.file(stash.team);
		if (file != null && this.teams.saver != null) {
			CompoundTag tag = new CompoundTag();
			tag.putInt("Size", stash.getContainerSize());
			ContainerHelper.saveAllItems(tag, stash.getItems(), this.teams.server.registryAccess());
			this.teams.saver.write(file, tmp -> NbtIo.writeCompressed(tag, tmp));
		}
	}

	@Nullable
	private Path file(UUID team) {
		return this.teams.dir == null ? null : this.teams.dir.resolve("stash").resolve(team + ".nbt");
	}

	private static MenuType<?> menuType(int rows) {
		return switch (rows) {
			case 1 -> MenuType.GENERIC_9x1;
			case 2 -> MenuType.GENERIC_9x2;
			case 3 -> MenuType.GENERIC_9x3;
			case 4 -> MenuType.GENERIC_9x4;
			case 5 -> MenuType.GENERIC_9x5;
			default -> MenuType.GENERIC_9x6;
		};
	}

	private final class Stash extends SimpleContainer {
		final UUID team;
		boolean dirty;
		long dirtySince;

		Stash(final UUID team, final int size) {
			super(size);
			this.team = team;
		}

		public boolean stillValid(Player player) {
			TeamData t = Stashes.this.teams.store.ofPlayer(player.getUUID());
			return t != null && t.id.equals(this.team) && Stashes.this.teams.cfg().stash;
		}

		public void setChanged() {
			super.setChanged();
			if (!this.dirty) {
				this.dirtySince = System.currentTimeMillis();
			}

			this.dirty = true;
		}
	}
}
