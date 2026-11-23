package com.onehumanawa.cfhcore.content.gui.quests.client.editor;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton.Variant;
import com.onehumanawa.cfhcore.content.gui.core.widget.popup.MenuPopup;
import com.onehumanawa.cfhcore.content.gui.core.widget.popup.Popups;
import com.onehumanawa.cfhcore.content.gui.core.widget.popup.MenuPopup.Item;
import com.onehumanawa.cfhcore.content.gui.quests.client.text.QuestText;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.QuestIcons;
import com.onehumanawa.cfhcore.content.gui.quests.model.Reward;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

final class RewardDialog extends EditorDialog {
	private static final int DEEPEST = 2;
	private final Reward draft;
	private final boolean weighted;
	private final int depth;
	private final Consumer<Reward> onDone;
	@Nullable
	private List<String> only;

	RewardDialog(Reward reward, boolean fresh, boolean weighted, int depth, Consumer<Reward> onDone) {
		super(Tr.t(depth > 0 ? (fresh ? "reward.choice.new" : "reward.choice.edit") : (fresh ? "reward.new" : "reward.edit")), Icon.STAR_FILLED, 260);
		this.draft = Kinds.copy(reward);
		this.weighted = weighted;
		this.depth = depth;
		this.onDone = onDone;
		this.button(Component.translatable("gui.cancel"), Variant.SECONDARY, this::close);
		this.button(Tr.t(fresh ? "dialog.add" : "dialog.done"), Variant.PRIMARY, this::submit);
	}

	@Override
	protected void submit() {
		if (this.form != null) {
			this.form.flush();
		}

		this.close();
		this.onDone.accept(Kinds.tidy(Kinds.copy(this.draft)));
	}

	Reward draftForTest() {
		return this.draft;
	}

	RewardDialog only(String... kinds) {
		this.only = List.of(kinds);
		return this;
	}

	private List<String> kinds() {
		if (this.only != null) {
			return this.only;
		} else if (this.depth < 2) {
			return Kinds.REWARDS;
		} else {
			List<String> flat = new ArrayList<>(Kinds.REWARDS);
			flat.remove("choice");
			flat.remove("random");
			flat.remove("all");
			return flat;
		}
	}

	@Override
	protected void fill(Form f) {
		f.row(new Bits.Reading(f.width, () -> QuestIcons.of(this.draft), () -> QuestText.label(this.draft)));
		f.dropdown("reward.type", this.kinds(), this.draft.type, type -> Tr.value("reward", type), type -> {
			this.draft.type = type;
			this.rebuild();
		});
		String var2 = this.draft.type;
		switch (var2) {
			case "item":
				f.first(f.pick("reward.item", this.draft.item, Pickers.What.GIFT, v -> this.draft.item = v));
				f.number("reward.count", (long)this.draft.count, 1L, 1000000L, v -> this.draft.count = v.intValue());
				f.number("reward.bonus", (long)this.draft.bonus, 0L, 1000000L, v -> this.draft.bonus = v.intValue());
				break;
			case "all":
				this.choices(f);
				break;
			case "toast":
				f.first(f.text("reward.text", this.draft.text, 1024, v -> this.draft.text = v));
				f.note(Tr.t("reward.toast.note"));
				break;
			case "stage":
				f.first(f.text("reward.stage", this.draft.value, 256, v -> this.draft.value = v.trim()));
				f.toggle("reward.remove", this.draft.remove, v -> this.draft.remove = v);
				f.note(Tr.t("reward.stage.note"));
				break;
			case "counter":
				f.first(f.text("reward.counter", this.draft.value, 256, v -> this.draft.value = v.trim()));
				f.number("reward.amount", (long)this.draft.count, 1L, 1000000000L, v -> this.draft.count = v.intValue());
				f.note(Tr.t("reward.counter.note"));
				break;
			case "currency":
				f.first(f.number("reward.amount", (long)this.draft.count, 1L, 1000000000L, v -> this.draft.count = v.intValue()));
				f.note(Tr.t("reward.currency.note"));
				break;
			case "custom":
				f.first(f.text("reward.name", this.draft.value, 256, v -> this.draft.value = v.trim()));
				f.note(Tr.t("reward.custom.note"));
				break;
			case "xp":
				f.first(f.number("reward.xp", (long)this.draft.xp, 0L, 1000000000L, v -> this.draft.xp = v.intValue()));
				f.toggle("reward.levels", this.draft.levels, v -> this.draft.levels = v);
				break;
			case "command":
				f.first(f.text("reward.command", this.draft.command, 4096, v -> this.draft.command = v));
				f.number("reward.level", (long)this.draft.level, 0L, 4L, v -> this.draft.level = v.intValue());
				f.text("reward.said", this.draft.text, 1024, v -> this.draft.text = v);
				f.note(Tr.t("reward.command.note"));
				break;
			case "loot":
				f.first(f.pick("reward.loot", this.draft.loot, Pickers.What.LOOT, v -> this.draft.loot = v));
				break;
			case "choice":
				this.choices(f);
				break;
			case "random":
				this.choices(f);
				f.number("reward.rolls", (long)this.draft.rolls, 1L, 64L, v -> this.draft.rolls = v.intValue());
				f.number("reward.empty", (long)this.draft.empty, 0L, 1000000L, v -> this.draft.empty = v.intValue());
				break;
			case "team_xp":
				f.first(f.number("reward.xp", (long)this.draft.xp, 0L, 1000000000L, v -> this.draft.xp = v.intValue()));
				f.note(Tr.t("reward.team_xp.note"));
				break;
			default:
				f.first(f.pick("reward.advancement", this.draft.advancement, Pickers.What.ADVANCEMENT, v -> this.draft.advancement = v));
				f.text("reward.criterion", this.draft.criterion, 256, v -> this.draft.criterion = v);
		}

		if (this.weighted) {
			f.number("reward.weight", (long)this.draft.weight, 1L, 1000000L, v -> this.draft.weight = v.intValue());
		}

		if (this.depth == 0) {
			f.toggle("reward.team", this.draft.team, v -> this.draft.team = v);
			f.toggle("reward.auto", this.draft.auto, v -> this.draft.auto = v);
			f.toggle("reward.hidden", this.draft.hidden, v -> this.draft.hidden = v);
			f.toggle("reward.excluded", this.draft.excluded, v -> this.draft.excluded = v);
		}

		f.gap(2);
		f.text("reward.title", this.draft.title, 256, v -> this.draft.title = v)
			.hint(Tr.t(!this.draft.is("command") && !this.draft.is("loot") ? "reward.title.hint" : "reward.title.hint.needed"));
		f.icon("reward.icon", this.draft.icon, Pickers.What.ICON, () -> QuestIcons.of(this.draft), v -> this.draft.icon = v);
	}

	private void choices(Form f) {
		boolean random = this.draft.is("random");
		f.label(Tr.t(random ? "reward.choices.random" : (this.draft.is("all") ? "reward.choices.all" : "reward.choices")));
		if (this.draft.choices.isEmpty()) {
			f.note(Tr.t("reward.choices.none"));
		}

		int total = random ? Math.max(0, this.draft.empty) : 0;

		for (Reward c : this.draft.choices) {
			total += Math.max(1, c.weight);
		}

		for (int i = 0; i < this.draft.choices.size(); i++) {
			int at = i;
			Reward c = this.draft.choices.get(i);
			Component note = random ? Component.literal(Math.round(100.0F * (float)Math.max(1, c.weight) / (float)Math.max(1, total)) + "%") : null;
			Bits.ListRow row = new Bits.ListRow(
				f.width, QuestIcons.of(c), QuestText.label(c), note, () -> new RewardDialog(c, false, random, this.depth + 1, changed -> {
						this.draft.choices.set(at, changed);
						this.rebuild();
					}).show()
			);
			row.actions(at > 0 ? () -> {
				Collections.swap(this.draft.choices, at, at - 1);
				this.rebuild();
			} : null, at < this.draft.choices.size() - 1 ? () -> {
				Collections.swap(this.draft.choices, at, at + 1);
				this.rebuild();
			} : null, () -> {
				this.draft.choices.remove(at);
				this.rebuild();
			});
			f.stop("reward.choice." + i, row);
			f.row(row);
		}

		CFHGuiButton[] add = new CFHGuiButton[1];
		add[0] = f.button("reward.choice.add", Tr.t("reward.choice.add"), Icon.PLUS, () -> {
			List<Item> items = new ArrayList<>();

			for (String type : Kinds.REWARDS) {
				if (!type.equals("choice") && !type.equals("random") && !type.equals("all")) {
					items.add(Item.of(Tr.value("reward", type), Kinds.rewardIcon(type), () -> new RewardDialog(Kinds.reward(type), true, random, this.depth + 1, made -> {
							this.draft.choices.add(made);
							this.rebuild();
						}).show()));
				}
			}

			Popups.open(new MenuPopup(add[0].getX(), add[0].getY() + add[0].getHeight() + 1, items, add[0].getWidth()));
		});
		add[0].active = this.draft.choices.size() < 64;
	}
}
