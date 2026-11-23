package com.onehumanawa.cfhcore.content.gui.server.mixin.server;

import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({CommandSourceStack.class})
public interface CommandSourceStackAccessor {
	@Accessor("source")
	CommandSource cfhServer$source();
}
