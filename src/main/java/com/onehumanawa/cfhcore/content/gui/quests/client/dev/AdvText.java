package com.onehumanawa.cfhcore.content.gui.quests.client.dev;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Component.Serializer;
import org.jetbrains.annotations.Nullable;

final class AdvText {
	static Component component(@Nullable String json, String fallback) {
		if (json != null && !json.isEmpty()) {
			try {
				ClientPacketListener connection = Minecraft.getInstance().getConnection();
				Component c = Serializer.fromJson(json, connection == null ? RegistryAccess.EMPTY : connection.registryAccess());
				return (Component)(c == null ? Component.literal(fallback) : c);
			} catch (RuntimeException var4) {
				return Component.literal(fallback);
			}
		} else {
			return Component.literal(fallback);
		}
	}

	private AdvText() {
	}
}
