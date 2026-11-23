package com.onehumanawa.cfhcore.content.gui.server.mixin.server;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.onehumanawa.cfhcore.content.gui.server.service.Hooks;
import java.util.List;
import java.util.function.Function;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.commands.ListPlayersCommand;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({ListPlayersCommand.class})
public abstract class ListPlayersCommandMixin {
	@WrapOperation(
		method = {"format"},
		at = {@At(
			value = "INVOKE",
			target = "Lnet/minecraft/server/players/PlayerList;getPlayers()Ljava/util/List;"
		)}
	)
	private static List<ServerPlayer> cfhServer$withoutTheVanished(
		PlayerList list, Operation<List<ServerPlayer>> original, CommandSourceStack source, Function<ServerPlayer, Component> nameExtractor
	) {
		List<ServerPlayer> players = (List<ServerPlayer>)original.call(new Object[]{list});
		return source.getEntity() instanceof ServerPlayer viewer ? Hooks.shown(players, viewer) : players;
	}
}
