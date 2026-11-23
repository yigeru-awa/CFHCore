package com.onehumanawa.cfhcore.content.gui.teams.compat;

import com.onehumanawa.cfhcore.content.gui.teams.model.PingData;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;

public interface MapBridge {
	String id();

	boolean attach();

	default void marksChanged() {
	}

	default void claimsChanged(String dimension, @Nullable LongSet regions) {
	}

	default void ping(PingData ping) {
	}

	default void tick() {
	}

	default void abandon() {
	}

	@Nullable
	default Screen mapScreen() {
		return null;
	}

	default String describe() {
		return "";
	}
}
