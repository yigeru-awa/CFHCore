package com.onehumanawa.cfhcore.content.gui.quests.server;

import com.onehumanawa.cfhcore.content.gui.quests.CFHGuiQuests;
import java.util.function.Consumer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public final class QuestEvents {
	private static boolean placesByLoader;
	private static boolean complained;
	@Nullable
	static Consumer<String> tap;

	public static void placesByLoader(boolean yes) {
		placesByLoader = yes;
	}

	public static void killed(ServerPlayer killer, Entity victim) {
		if (victim instanceof LivingEntity && victim != killer) {
			Consumer<String> listening = tap;
			if (listening != null) {
				listening.accept("killed " + BuiltInRegistries.ENTITY_TYPE.getKey(victim.getType()));
			} else {
				QuestEngine e = QuestsServer.engine();
				if (e != null) {
					try {
						Seat s = QuestsServer.seat(killer);
						if (s != null) {
							e.killed(s, victim.getType(), victim.getName().getString());
						}
					} catch (Throwable var5) {
						complain(var5);
					}
				}
			}
		}
	}

	public static void crafted(ServerPlayer player, ItemStack stack, int amount) {
		if (!stack.isEmpty() && amount > 0) {
			Consumer<String> listening = tap;
			if (listening != null) {
				listening.accept("crafted " + amount + " " + BuiltInRegistries.ITEM.getKey(stack.getItem()));
			} else {
				QuestEngine e = QuestsServer.engine();
				if (e != null) {
					try {
						Seat s = QuestsServer.seat(player);
						if (s != null) {
							e.crafted(s, stack, amount);
						}
					} catch (Throwable var6) {
						complain(var6);
					}
				}
			}
		}
	}

	public static void broke(ServerPlayer player, BlockState state) {
		if (!state.isAir()) {
			Consumer<String> listening = tap;
			if (listening != null) {
				listening.accept("broke " + BuiltInRegistries.BLOCK.getKey(state.getBlock()));
			} else {
				QuestEngine e = QuestsServer.engine();
				if (e != null) {
					try {
						Seat s = QuestsServer.seat(player);
						if (s != null) {
							e.broke(s, state.getBlock());
						}
					} catch (Throwable var5) {
						complain(var5);
					}
				}
			}
		}
	}

	public static void placedByItem(ServerPlayer player, BlockState state) {
		if (!placesByLoader) {
			placed(player, state);
		}
	}

	public static void placed(ServerPlayer player, BlockState state) {
		if (!state.isAir()) {
			Consumer<String> listening = tap;
			if (listening != null) {
				listening.accept("placed " + BuiltInRegistries.BLOCK.getKey(state.getBlock()));
			} else {
				QuestEngine e = QuestsServer.engine();
				if (e != null) {
					try {
						Seat s = QuestsServer.seat(player);
						if (s != null) {
							e.placed(s, state.getBlock());
						}
					} catch (Throwable var5) {
						complain(var5);
					}
				}
			}
		}
	}

	private static void complain(Throwable t) {
		if (!complained) {
			complained = true;
			CFHGuiQuests.LOGGER.error("[CFHGui Quests] counting something a player did failed (said once)", t);
		}
	}

	private QuestEvents() {
	}
}
