package com.onehumanawa.cfhcore.content.gui.shops.service;

import com.onehumanawa.cfhcore.content.gui.core.perm.Perms;
import com.onehumanawa.cfhcore.content.gui.core.perm.Perms.Node;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

public record Actor(@Nullable UUID id, String name, @Nullable ServerPlayer player, @Nullable Pocket pocket, Predicate<Node> may) {
	public boolean may(Node node) {
		return this.may.test(node);
	}

	public static Actor of(ServerPlayer player) {
		return new Actor(player.getUUID(), player.getGameProfile().getName(), player, pocketOf(player), node -> Perms.has(player, node));
	}

	public static Actor of(CommandSourceStack source) {
		ServerPlayer player = source.getPlayer();
		return player != null ? of(player) : new Actor(null, source.getTextName(), null, null, node -> Perms.has(source, node));
	}

	public static Pocket pocketOf(ServerPlayer player) {
		return new Pocket(player.getInventory().items, () -> {
			player.getInventory().setChanged();
			player.containerMenu.broadcastChanges();
		});
	}
}
