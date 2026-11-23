package com.onehumanawa.cfhcore.content.gui.quests.client.editor;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton.Variant;
import com.onehumanawa.cfhcore.content.gui.quests.client.text.QuestText;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.QuestIcons;
import com.onehumanawa.cfhcore.content.gui.quests.model.Task;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

final class TaskDialog extends EditorDialog {
	private final Task draft;
	private final Consumer<Task> onDone;

	TaskDialog(Task task, boolean fresh, Consumer<Task> onDone) {
		super(Tr.t(fresh ? "task.new" : "task.edit"), Icon.QUEST, 260);
		this.draft = Kinds.copy(task);
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

	Task draftForTest() {
		return this.draft;
	}

	@Override
	protected void fill(Form f) {
		f.row(new Bits.Reading(f.width, () -> QuestIcons.of(this.draft), () -> QuestText.label(this.draft)));
		f.dropdown("task.type", Kinds.TASKS, this.draft.type, type -> Tr.value("task", type), type -> {
			this.draft.type = type;
			if (type.equals("team") && !Kinds.TEAM_VALUES.contains(this.draft.value)) {
				this.draft.value = "members";
			}

			this.rebuild();
		});
		String var2 = this.draft.type;
		switch (var2) {
			case "item":
				f.first(f.pick("task.item", this.draft.item, Pickers.What.ITEM, v -> this.draft.item = v));
				this.count(f);
				f.toggle("task.consume", this.draft.consume, v -> this.draft.consume = v);
				break;
			case "craft":
				f.first(f.pick("task.item", this.draft.item, Pickers.What.ITEM, v -> this.draft.item = v));
				this.count(f);
				break;
			case "kill":
				f.first(f.pick("task.entity", this.draft.entity, Pickers.What.MOB, v -> this.draft.entity = v));
				this.count(f);
				f.text("task.mobname", this.draft.name, 256, v -> this.draft.name = v);
				break;
			case "fluid":
				f.first(f.pick("task.fluid", this.draft.fluid, Pickers.What.FLUID, v -> this.draft.fluid = v.trim()));
				f.number("task.millibuckets", this.draft.count, 1L, 1000000000000L, v -> this.draft.count = v);
				f.toggle("task.consume", this.draft.consume, v -> this.draft.consume = v);
				f.note(Tr.t("task.fluid.note"));
				break;
			case "energy":
				f.first(this.count(f));
				f.note(Tr.t("task.energy.note"));
				break;
			case "stage":
				f.first(f.text("task.stage", this.draft.value, 256, v -> this.draft.value = v.trim()));
				f.note(Tr.t("task.stage.note"));
				break;
			case "counter":
				f.first(f.text("task.counter", this.draft.value, 256, v -> this.draft.value = v.trim()));
				this.count(f);
				f.note(Tr.t("task.counter.note"));
				break;
			case "break":
			case "place":
				f.first(f.pick("task.block", this.draft.block, Pickers.What.BLOCK, v -> this.draft.block = v));
				this.count(f);
				break;
			case "advancement":
				f.first(f.pick("task.advancement", this.draft.advancement, Pickers.What.ADVANCEMENT, v -> this.draft.advancement = v));
				f.text("task.criterion", this.draft.criterion, 256, v -> this.draft.criterion = v);
				break;
			case "stat":
				f.first(f.pick("task.stat", this.draft.stat, Pickers.What.STAT, v -> this.draft.stat = v));
				this.count(f);
				f.note(Tr.t("task.stat.note"));
				break;
			case "xp":
				f.first(this.count(f));
				f.toggle("task.points", this.draft.points, v -> this.draft.points = v);
				f.toggle("task.consume", this.draft.consume, v -> this.draft.consume = v);
				break;
			case "location":
				f.first(f.pick("task.dimension", this.draft.dimension, Pickers.What.DIMENSION, v -> this.draft.dimension = v));
				f.pick("task.biome", this.draft.biome, Pickers.What.BIOME, v -> this.draft.biome = v);
				f.pick("task.structure", this.draft.structure, Pickers.What.STRUCTURE, v -> this.draft.structure = v);
				f.label(Tr.t("task.place"));
				f.trio(
					new String[]{"task.x", "task.y", "task.z"},
					new int[]{this.draft.x, this.draft.y, this.draft.z},
					List.of(v -> this.draft.x = v, v -> this.draft.y = v, v -> this.draft.z = v)
				);
				f.number("task.radius", (long)this.draft.radius, 0L, 100000L, v -> this.draft.radius = v.intValue());
				f.label(Tr.t("task.box"));
				f.trio(
					new String[]{"task.sizeX", "task.sizeY", "task.sizeZ"},
					new int[]{this.draft.sizeX, this.draft.sizeY, this.draft.sizeZ},
					List.of(v -> this.draft.sizeX = Math.max(0, v), v -> this.draft.sizeY = Math.max(0, v), v -> this.draft.sizeZ = Math.max(0, v))
				);
				if (Minecraft.getInstance().player != null) {
					f.button("task.here", Tr.t("task.here"), Icon.COMPASS, this::here);
				}

				f.note(Tr.t("task.location.note"));
				break;
			case "observe":
				f.first(f.pick("task.block", this.draft.block, Pickers.What.BLOCK, v -> this.draft.block = v));
				f.pick("task.entity", this.draft.entity, Pickers.What.MOB, v -> this.draft.entity = v);
				f.number("task.seconds", (long)this.draft.seconds, 0L, 3600L, v -> this.draft.seconds = v.intValue());
				break;
			case "checkmark":
				f.note(Tr.t("task.checkmark.note"));
				break;
			case "quest":
				f.first(f.pick("task.quest", this.draft.quest, Pickers.What.QUEST, v -> this.draft.quest = v));
				break;
			case "chapter":
				f.first(f.pick("task.chapter", this.draft.chapter, Pickers.What.CHAPTER, v -> this.draft.chapter = v));
				break;
			case "team":
				f.choice(
					"task.value",
					Kinds.TEAM_VALUES,
					Kinds.TEAM_VALUES.contains(this.draft.value) ? this.draft.value : "members",
					v -> Tr.value("team", v),
					v -> this.draft.value = v
				);
				this.count(f);
				f.note(Tr.t("task.team.note"));
				break;
			default:
				f.first(f.text("task.name", this.draft.value, 256, v -> this.draft.value = v));
				this.count(f);
				f.note(Tr.t("task.custom.note"));
		}

		f.gap(2);
		f.toggle("task.optional", this.draft.optional, v -> this.draft.optional = v);
		f.text("task.title", this.draft.title, 256, v -> this.draft.title = v).hint(Tr.t("task.title.hint"));
		f.icon("task.icon", this.draft.icon, Pickers.What.ICON, () -> QuestIcons.of(this.draft), v -> this.draft.icon = v);
	}

	private EditField count(Form f) {
		return f.number("task.count", this.draft.count, 1L, 1000000000000L, v -> this.draft.count = v);
	}

	private void here() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null && mc.level != null) {
			this.draft.x = mc.player.getBlockX();
			this.draft.y = mc.player.getBlockY();
			this.draft.z = mc.player.getBlockZ();
			if (this.draft.radius <= 0) {
				this.draft.radius = 16;
			}

			this.draft.dimension = mc.level.dimension().location().toString();
			this.rebuild();
		}
	}
}
