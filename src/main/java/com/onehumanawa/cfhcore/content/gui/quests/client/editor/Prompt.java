package com.onehumanawa.cfhcore.content.gui.quests.client.editor;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton.Variant;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

final class Prompt extends EditorDialog {
	private final Component about;
	private final String initial;
	private final Function<String, Component> wrong;
	private final Consumer<String> onOk;
	private final CFHGuiButton ok;
	private String value;

	Prompt(Component title, @Nullable Icon icon, Component about, String initial, Component okLabel, Function<String, Component> wrong, Consumer<String> onOk) {
		super(title, icon, 240);
		this.about = about;
		this.initial = initial;
		this.value = initial;
		this.wrong = wrong;
		this.onOk = onOk;
		this.button(Component.translatable("gui.cancel"), Variant.SECONDARY, this::close);
		this.ok = this.button(okLabel, Variant.PRIMARY, this::submit);
		this.check();
	}

	private void check() {
		Component problem = this.wrong.apply(this.value.trim());
		this.fail(!this.value.trim().isEmpty() && !this.value.equals(this.initial) ? problem : null);
		this.ok.active = problem == null && !this.value.trim().isEmpty();
	}

	@Override
	protected void fill(Form f) {
		f.note(this.about);
		EditField field = new EditField(f.width, this.about, this.value).max(128);
		field.typing(text -> {
			this.value = text;
			this.check();
		});
		f.stop("value", field);
		f.first((EditField)f.row(field));
		field.moveCursorToEnd(false);
		field.setHighlightPos(0);
	}

	@Override
	protected void submit() {
		if (this.ok.active) {
			this.close();
			this.onOk.accept(this.value.trim());
		}
	}
}
