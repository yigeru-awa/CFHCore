package com.onehumanawa.cfhcore.content.gui.server.neoforge;

import com.onehumanawa.cfhcore.content.gui.core.perm.Perms;
import com.onehumanawa.cfhcore.content.gui.core.perm.Perms.Node;
import com.onehumanawa.cfhcore.content.gui.server.CFHGuiServer;
import com.onehumanawa.cfhcore.content.gui.server.platform.ServerPlatform;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.server.permission.PermissionAPI;
import net.neoforged.neoforge.server.permission.events.PermissionGatherEvent.Nodes;
import net.neoforged.neoforge.server.permission.handler.DefaultPermissionHandler;
import net.neoforged.neoforge.server.permission.nodes.PermissionDynamicContext;
import net.neoforged.neoforge.server.permission.nodes.PermissionDynamicContextKey;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;
import net.neoforged.neoforge.server.permission.nodes.PermissionTypes;
import org.jetbrains.annotations.Nullable;

public final class NeoForgeServerPlatform implements ServerPlatform {
	private static final Map<String, PermissionNode<Boolean>> NODES = new ConcurrentHashMap<>();
	private static final ThreadLocal<Boolean> DEFAULTED = ThreadLocal.withInitial(() -> Boolean.FALSE);

	static void gather(Nodes event) {
		for (Node declared : Perms.declared()) {
			if (!declared.number() && !declared.path().isEmpty()) {
				try {
					PermissionNode<Boolean> node = NODES.computeIfAbsent(
						declared.id(), id -> new PermissionNode(declared.module(), declared.path(), PermissionTypes.BOOLEAN, (player, uuid, context) -> {
								DEFAULTED.set(Boolean.TRUE);
								return Boolean.FALSE;
							}, new PermissionDynamicContextKey[0])
					);
					event.addNodes(new PermissionNode[]{node});
				} catch (RuntimeException var4) {
					NODES.remove(declared.id());
					CFHGuiServer.LOGGER.debug("[CFHGui Server] {} is not offered to other permission mods: {}", declared.id(), var4.toString());
				}
			}
		}
	}

	@Nullable
	private static ResourceLocation activeHandler() {
		try {
			ResourceLocation id = PermissionAPI.getActivePermissionHandler();
			return id != null && !DefaultPermissionHandler.IDENTIFIER.equals(id) ? id : null;
		} catch (RuntimeException var1) {
			return null;
		}
	}

	@Nullable
	@Override
	public String externalPermissions() {
		ResourceLocation id = activeHandler();
		return id == null ? null : id.toString();
	}

	@Nullable
	@Override
	public Boolean externalPermission(ServerPlayer player, String node) {
		PermissionNode<Boolean> registered = NODES.get(node);
		if (registered != null && activeHandler() != null) {
			Object var5;
			try {
				DEFAULTED.set(Boolean.FALSE);
				Boolean answer = (Boolean)PermissionAPI.getPermission(player, registered, new PermissionDynamicContext[0]);
				return DEFAULTED.get() ? null : answer;
			} catch (RuntimeException var9) {
				var5 = null;
			} finally {
				DEFAULTED.set(Boolean.FALSE);
			}

			return (Boolean)var5;
		} else {
			return null;
		}
	}

	@Override
	public boolean isFakePlayer(Player player) {
		return player instanceof FakePlayer;
	}
}
