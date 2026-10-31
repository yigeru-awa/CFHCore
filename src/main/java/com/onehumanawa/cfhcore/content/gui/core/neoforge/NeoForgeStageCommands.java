package com.onehumanawa.cfhcore.content.gui.core.neoforge;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.onehumanawa.cfhcore.content.gui.core.stage.platform.StageCommands;
import com.onehumanawa.cfhcore.content.gui.core.stage.scene.SceneCommandHandler;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.common.NeoForge;

public final class NeoForgeStageCommands implements StageCommands {
   public void register(SceneCommandHandler handler) {
      NeoForge.EVENT_BUS.addListener(RegisterClientCommandsEvent.class, (e) -> e.getDispatcher().register((LiteralArgumentBuilder)Commands.literal("cfhgui").then(((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("scene").then(Commands.literal("capture").then(Commands.argument("template", StringArgumentType.string()).then(((RequiredArgumentBuilder)Commands.argument("scene", StringArgumentType.string()).executes((c) -> reply(c, handler.capture(StringArgumentType.getString(c, "template"), StringArgumentType.getString(c, "scene"), (BlockPos)null)))).then(Commands.argument("x", IntegerArgumentType.integer()).then(Commands.argument("y", IntegerArgumentType.integer()).then(Commands.argument("z", IntegerArgumentType.integer()).executes((c) -> reply(c, handler.capture(StringArgumentType.getString(c, "template"), StringArgumentType.getString(c, "scene"), new BlockPos(IntegerArgumentType.getInteger(c, "x"), IntegerArgumentType.getInteger(c, "y"), IntegerArgumentType.getInteger(c, "z")))))))))))).then(Commands.literal("list").executes((c) -> reply(c, handler.list())))).then(Commands.literal("reload").executes((c) -> reply(c, handler.reload()))))));
   }

   private static int reply(CommandContext<CommandSourceStack> c, Component message) {
      ((CommandSourceStack)c.getSource()).sendSuccess(() -> message, false);
      return 1;
   }
}
