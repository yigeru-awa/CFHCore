package com.onehumanawa.cfhcore.content.gui.server.neoforge;

import com.onehumanawa.cfhcore.content.gui.core.module.Modules;
import com.onehumanawa.cfhcore.content.gui.server.ServerHooks;
import com.onehumanawa.cfhcore.content.gui.server.CFHGuiServer;
import com.onehumanawa.cfhcore.content.gui.server.command.ServerCommands;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.Clone;
import net.neoforged.neoforge.server.permission.events.PermissionGatherEvent.Nodes;

@Mod("cfhcore")
public final class CFHGuiServerNeoForge {
	public CFHGuiServerNeoForge(IEventBus modBus, ModContainer container) {
		Modules.register(CFHGuiServer.MODULE);
		NeoForge.EVENT_BUS.addListener(EventPriority.LOW, RegisterCommandsEvent.class, e -> ServerCommands.register(e.getDispatcher(), e.getBuildContext()));
		NeoForge.EVENT_BUS.addListener(ServerChatEvent.class, e -> {
			if (!ServerHooks.allowChat(e.getPlayer(), e.getRawText())) {
				e.setCanceled(true);
			}
		});
		NeoForge.EVENT_BUS.addListener(LivingIncomingDamageEvent.class, e -> {
			if (e.getEntity() instanceof ServerPlayer player && !ServerHooks.allowDamage(player, e.getSource(), e.getAmount())) {
				e.setCanceled(true);
			}
		});
		NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, LivingDeathEvent.class, e -> {
			if (e.getEntity() instanceof ServerPlayer player) {
				ServerHooks.died(player);
			}
		});
		NeoForge.EVENT_BUS.addListener(Clone.class, e -> {
			if (e.getEntity() instanceof ServerPlayer to && e.getOriginal() instanceof ServerPlayer from) {
				ServerHooks.copied(from, to, !e.isWasDeath());
			}
		});
		NeoForge.EVENT_BUS.addListener(Nodes.class, NeoForgeServerPlatform::gather);
	}
}
