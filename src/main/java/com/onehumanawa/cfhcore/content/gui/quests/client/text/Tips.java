package com.onehumanawa.cfhcore.content.gui.quests.client.text;

import com.onehumanawa.cfhcore.content.gui.quests.client.view.ItemSpecs;
import com.onehumanawa.cfhcore.content.gui.quests.model.Reward;
import com.onehumanawa.cfhcore.content.gui.quests.model.Task;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class Tips {
	private static final int LISTED = 12;

	public static List<Component> task(Task task, int grey) {
		List<Component> out = new ArrayList<>();
		String spec = !task.item.isBlank() ? task.item : task.block;
		List<String> parts = ItemSpecs.split(task.item);
		if (parts.size() > 1) {
			out.add(Component.translatable("cfhgui_quests.task.tip.either"));

			for (int i = 0; i < parts.size() && i < 12; i++) {
				out.add(Component.literal("• ").append(ItemSpecs.name(parts.get(i))).withStyle(s -> s.withColor(grey)));
			}

			if (parts.size() > 12) {
				out.add(Component.translatable("cfhgui_quests.tip.more", new Object[]{parts.size() - 12}).withStyle(s -> s.withColor(grey)));
			}
		} else if (!spec.isBlank()) {
			if (!ItemSpecs.isTag(spec) && !spec.startsWith("@") && !ItemSpecs.isFilter(spec)) {
				out.addAll(RichTextView.itemTip(ItemSpecs.stack(spec), ItemSpecs.name(spec)));
			} else {
				out.add(ItemSpecs.name(spec));
			}
		} else {
			out.add(QuestText.label(task));
		}

		for (String part : parts) {
			if (ItemSpecs.isFilter(part)) {
				String words = part.substring("ftbfilter:".length());
				out.add(Component.literal(words.length() > 60 ? words.substring(0, 60) + "…" : words).withStyle(s -> s.withColor(grey)));
				if (!ItemSpecs.filtersKnown()) {
					out.add(Component.translatable("cfhgui_quests.task.tip.filter").withStyle(s -> s.withColor(14722136)));
				}
			}
		}

		if (!task.name.isBlank()) {
			out.add(Component.translatable("cfhgui_quests.task.tip.named", new Object[]{task.name}).withStyle(s -> s.withColor(grey)));
		}

		String how = !task.is("item") && !task.is("xp")
			? (task.is("fluid") ? (task.consume ? "fluid.consume" : "fluid") : task.type)
			: (task.consume ? "consume" : "have");
		out.add(Component.translatable("cfhgui_quests.task.how." + how).withStyle(s -> s.withColor(grey)));
		if (task.optional) {
			out.add(Component.translatable("cfhgui_quests.task.tip.optional").withStyle(s -> s.withColor(grey)));
		}

		return out;
	}

	public static List<Component> reward(Reward reward, int grey) {
		if (reward.is("item") && !reward.item.isBlank()) {
			List<Component> item = new ArrayList<>(RichTextView.itemTip(ItemSpecs.stack(reward.item), ItemSpecs.name(reward.item)));
			if (reward.bonus > 0) {
				item.add(Component.translatable("cfhgui_quests.reward.tip.bonus", new Object[]{reward.count, reward.count + reward.bonus}).withStyle(s -> s.withColor(grey)));
			}

			return item;
		} else {
			List<Component> out = new ArrayList<>();
			out.add(QuestText.label(reward));
			if (reward.group()) {
				long total = (long)Math.max(0, reward.empty);

				for (Reward c : reward.choices) {
					total += (long)Math.max(1, c.weight);
				}

				for (int i = 0; i < reward.choices.size() && i < 12; i++) {
					Reward c = reward.choices.get(i);
					MutableComponent line = Component.literal("• ").append(QuestText.label(c));
					if (reward.is("random") && reward.choices.size() > 1) {
						line.append(" ").append(Component.translatable("cfhgui_quests.reward.tip.chance", new Object[]{chance((long)Math.max(1, c.weight), total)}));
					}

					out.add(line.withStyle(s -> s.withColor(grey)));
				}

				if (reward.choices.size() > 12) {
					out.add(Component.translatable("cfhgui_quests.tip.more", new Object[]{reward.choices.size() - 12}).withStyle(s -> s.withColor(grey)));
				}

				if (reward.is("random") && reward.empty > 0) {
					out.add(Component.translatable("cfhgui_quests.reward.tip.nothing", new Object[]{chance((long)reward.empty, total)}).withStyle(s -> s.withColor(grey)));
				}
			}

			if (reward.is("toast") && !reward.title.isBlank() && !reward.text.isBlank()) {
				out.add(QuestText.text(reward.text).withStyle(s -> s.withColor(grey)));
			}

			out.add(Component.translatable("cfhgui_quests.reward.how." + reward.type).withStyle(s -> s.withColor(grey)));
			return out;
		}
	}

	private static String chance(long part, long whole) {
		if (whole <= 0L) {
			return "0";
		} else {
			double percent = (double)part * 100.0 / (double)whole;
			if (!(percent >= 10.0) && percent != Math.rint(percent)) {
				return percent < 0.1 ? "<0.1" : String.format(Locale.ROOT, "%.1f", percent);
			} else {
				return Long.toString(Math.round(percent));
			}
		}
	}

	private Tips() {
	}
}
