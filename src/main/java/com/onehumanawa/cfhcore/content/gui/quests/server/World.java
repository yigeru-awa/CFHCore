package com.onehumanawa.cfhcore.content.gui.quests.server;

import com.onehumanawa.cfhcore.content.gui.quests.QuestsServerConfig;
import com.onehumanawa.cfhcore.content.gui.quests.logic.BookIndex;
import com.onehumanawa.cfhcore.content.gui.quests.model.Chapter;
import com.onehumanawa.cfhcore.content.gui.quests.model.Reward;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.stats.Stat;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

interface World {
	long now();

	QuestsServerConfig config();

	TeamLink teams();

	Object datapacks();

	boolean hasClient(Seat var1);

	void send(Seat var1, CustomPacketPayload var2);

	void tell(Seat var1, Component var2);

	void broadcast(Component var1);

	Component displayName(Seat var1);

	boolean announcesAdvancements();

	boolean mayEdit(Seat var1);

	void inventory(Seat var1, Consumer<ItemStack> var2);

	long statistic(Seat var1, Stat<?> var2);

	boolean advancement(Seat var1, ResourceLocation var2, String var3);

	boolean stage(Seat var1, String var2);

	long fluid(Seat var1, Specs.Named var2);

	long drain(Seat var1, Specs.Named var2, long var3);

	long takeEnergy(Seat var1, long var2);

	default void bookSent(Seat seat) {
	}

	int experience(Seat var1, boolean var2);

	boolean at(Seat var1, TriggerIndex.Ref var2);

	@Nullable
	World.Sight sight(Seat var1);

	long take(Seat var1, Specs.ItemSpec var2, long var3);

	void pay(Seat var1, int var2, boolean var3);

	void give(Seat var1, Reward var2, String var3);

	void questDone(Seat var1, BookIndex.Entry var2);

	void chapterDone(Seat var1, Chapter var2);

	public static record Sight(@Nullable Block block, @Nullable EntityType<?> entity) {
	}
}
