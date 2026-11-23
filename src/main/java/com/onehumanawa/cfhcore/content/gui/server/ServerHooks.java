package com.onehumanawa.cfhcore.content.gui.server;

import com.onehumanawa.cfhcore.content.gui.core.event.Event;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;

public final class ServerHooks {
	public static final Event<ServerHooks.ChatFilter> CHAT = new Event("SERVER_CHAT");
	public static final Event<ServerHooks.DamageFilter> DAMAGE = new Event("SERVER_DAMAGE");
	public static final Event<Consumer<ServerPlayer>> DEATH = new Event("SERVER_DEATH");
	public static final Event<ServerHooks.Copy> COPIED = new Event("SERVER_COPIED");

	public static boolean allowChat(ServerPlayer player, String message) {
		return !CHAT.invokeUntilConsumed(l -> !l.allow(player, message));
	}

	public static boolean allowDamage(ServerPlayer player, DamageSource source, float amount) {
		return !DAMAGE.invokeUntilConsumed(l -> !l.allow(player, source, amount));
	}

	public static void died(ServerPlayer player) {
		DEATH.invoke(l -> l.accept(player));
	}

	public static void copied(ServerPlayer from, ServerPlayer to, boolean alive) {
		COPIED.invoke(l -> l.copied(from, to, alive));
	}

	private ServerHooks() {
	}

	@FunctionalInterface
	public interface ChatFilter {
		boolean allow(ServerPlayer var1, String var2);
	}

	@FunctionalInterface
	public interface Copy {
		void copied(ServerPlayer var1, ServerPlayer var2, boolean var3);
	}

	@FunctionalInterface
	public interface DamageFilter {
		boolean allow(ServerPlayer var1, DamageSource var2, float var3);
	}
}
