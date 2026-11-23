package com.onehumanawa.cfhcore.content.gui.server.service;

import com.onehumanawa.cfhcore.content.gui.server.Nodes;
import com.onehumanawa.cfhcore.content.gui.server.mixin.server.ChunkMapAccessor;
import com.onehumanawa.cfhcore.content.gui.server.mixin.server.TrackedEntityInvoker;
import com.onehumanawa.cfhcore.content.gui.server.model.PlayerRecord;
import com.onehumanawa.cfhcore.content.gui.server.storage.WorldStore;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

final class Vanish {
	private final ServerService s;

	Vanish(ServerService s) {
		this.s = s;
	}

	boolean is(UUID id) {
		Session session = this.s.session(id);
		if (session != null) {
			return session.record.vanished;
		} else {
			WorldStore.Known k = this.s.store.known(id);
			return k != null && k.vanished;
		}
	}

	boolean hiddenFrom(ServerPlayer subject, ServerPlayer viewer) {
		return subject != viewer && this.is(subject.getUUID()) ? !this.s.may(Actor.of(viewer), Nodes.MOD_SEE_VANISHED) : false;
	}

	void set(Session session, boolean on) {
		PlayerRecord rec = session.record;
		if (rec.vanished != on) {
			ServerPlayer player = session.player;
			if (on && this.s.live && player != null) {
				this.farewell(player, true);
			}

			rec.vanished = on;
			this.s.touch(rec);
			this.s.statistics.changed();
			this.s.sync.mark(session.id, "self");
			this.s.sync.markAll("online");
			if (this.s.trace != null) {
				this.s.trace.add(session.name + " <- vanish:" + on);
			}

			if (this.s.live && player != null) {
				for (Session other : this.s.online()) {
					ServerPlayer viewer = other.player;
					if (viewer != null && viewer != player && !this.s.may(other.actor(), Nodes.MOD_SEE_VANISHED)) {
						if (on) {
							viewer.connection.send(new ClientboundPlayerInfoRemovePacket(List.of(player.getUUID())));
						} else {
							viewer.connection.send(ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(List.of(player)));
						}
					}
				}

				retrack(player);
				player.serverLevel().updateSleepingPlayerList();
				this.s.server.invalidateStatus();
				if (!on) {
					this.farewell(player, false);
				}

				player.sendSystemMessage(Msg.tr(on ? "cfhgui_server.chat.vanish_on" : "cfhgui_server.chat.vanish_off").withStyle(ChatFormatting.GRAY));
			}
		}
	}

	private void farewell(ServerPlayer player, boolean leaving) {
		Component line = Component.translatable(leaving ? "multiplayer.player.left" : "multiplayer.player.joined", new Object[]{player.getDisplayName()})
			.withStyle(ChatFormatting.YELLOW);

		for (Session other : this.s.online()) {
			ServerPlayer viewer = other.player;
			if (viewer != null && viewer != player && !this.s.may(other.actor(), Nodes.MOD_SEE_VANISHED)) {
				viewer.sendSystemMessage(line);
			}
		}
	}

	static boolean tracked(ServerPlayer player) {
		return ((ChunkMapAccessor)player.serverLevel().getChunkSource().chunkMap).cfhServer$entityMap().get(player.getId()) instanceof TrackedEntityInvoker;
	}

	private static void retrack(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		if (((ChunkMapAccessor)level.getChunkSource().chunkMap).cfhServer$entityMap().get(player.getId()) instanceof TrackedEntityInvoker entity) {
			entity.cfhServer$updatePlayers(new ArrayList<>(level.players()));
		}
	}

	void joined(Session session) {
		ServerPlayer player = session.player;
		if (this.s.live && player != null) {
			if (!this.s.may(session.actor(), Nodes.MOD_SEE_VANISHED)) {
				List<UUID> hidden = null;

				for (Session other : this.s.online()) {
					if (other != session && other.record.vanished) {
						if (hidden == null) {
							hidden = new ArrayList<>(1);
						}

						hidden.add(other.id);
					}
				}

				if (hidden != null) {
					player.connection.send(new ClientboundPlayerInfoRemovePacket(hidden));
				}
			}

			if (session.record.vanished) {
				player.sendSystemMessage(Msg.tr("cfhgui_server.chat.vanish_still").withStyle(ChatFormatting.GRAY));
			}
		}
	}

	List<ServerPlayer> shown(List<ServerPlayer> players, @Nullable ServerPlayer viewer) {
		List<ServerPlayer> out = null;
		boolean sees = viewer != null && this.s.may(Actor.of(viewer), Nodes.MOD_SEE_VANISHED);
		if (sees) {
			return players;
		} else {
			for (int i = 0; i < players.size(); i++) {
				ServerPlayer p = players.get(i);
				boolean hide = p != viewer && this.is(p.getUUID());
				if (hide && out == null) {
					out = new ArrayList<>(players.subList(0, i));
				} else if (!hide && out != null) {
					out.add(p);
				}
			}

			return out == null ? players : out;
		}
	}
}
