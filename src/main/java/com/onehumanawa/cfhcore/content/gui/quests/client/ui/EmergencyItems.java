package com.onehumanawa.cfhcore.content.gui.quests.client.ui;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiIconButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiToasts;
import com.onehumanawa.cfhcore.content.gui.quests.client.QuestActions;
import com.onehumanawa.cfhcore.content.gui.quests.client.QuestsState;
import com.onehumanawa.cfhcore.content.gui.quests.client.text.QuestText;
import com.onehumanawa.cfhcore.content.gui.quests.model.QuestBook;
import com.onehumanawa.cfhcore.content.gui.quests.model.Reward;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

public final class EmergencyItems implements BookExtensions.Extension {
	private static final EmergencyItems INSTANCE = new EmergencyItems();

	static void init() {
		BookExtensions.register(INSTANCE);
	}

	public static boolean has() {
		QuestBook book = QuestsState.book();
		return book != null && !book.emergency.isEmpty();
	}

	@Override
	public void header(QuestBookScreen screen, Consumer<AbstractWidget> add) {
		if (has()) {
			add.accept(new CFHGuiIconButton(0, 0, 20, Icon.HEART, Component.translatable("cfhgui_quests.emergency"), EmergencyItems::fetch).tip(tip()));
		}
	}

	public static List<Component> tip() {
		List<Component> out = new ArrayList<>();
		QuestBook book = QuestsState.book();
		out.add(Component.translatable("cfhgui_quests.emergency"));
		if (book == null) {
			return out;
		} else {
			for (int i = 0; i < book.emergency.size() && i < 12; i++) {
				Reward r = book.emergency.get(i);
				out.add(Component.literal("• ").append(QuestText.label(r)).withStyle(s -> s.withColor(10592151)));
			}

			out.add(
				Component.translatable("cfhgui_quests.emergency.tip", new Object[]{QuestText.span((long)book.emergencyCooldownSeconds * 1000L)})
					.withStyle(s -> s.withColor(10592151))
			);
			return out;
		}
	}

	public static void fetch() {
		QuestActions.send(
			"emergency",
			new CompoundTag(),
			result -> {
				if (result.ok()) {
					CFHGuiToasts.show(Component.translatable("cfhgui_quests.emergency"), Component.translatable("cfhgui_quests.emergency.done"), Icon.HEART);
				} else if ("emergency_wait".equals(result.code()) && !result.args().isEmpty()) {
					long seconds = 0L;

					try {
						seconds = Long.parseLong(result.args().get(0));
					} catch (NumberFormatException var4) {
						seconds = 0L;
					}

					CFHGuiToasts.show(
						Component.translatable("cfhgui_quests.emergency"),
						Component.translatable("cfhgui_quests.emergency.wait", new Object[]{QuestText.span(seconds * 1000L)}),
						Icon.CLOCK
					);
				} else {
					CFHGuiToasts.show(Component.translatable("cfhgui_quests.result.failed"), QuestActions.describe(result), Icon.WARNING);
				}
			}
		);
	}

	private EmergencyItems() {
	}
}
