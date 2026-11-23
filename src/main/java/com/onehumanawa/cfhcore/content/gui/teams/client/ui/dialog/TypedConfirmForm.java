package com.onehumanawa.cfhcore.content.gui.teams.client.ui.dialog;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiTextField;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton.Variant;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.HintField;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamsDraw;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamsText;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;

public final class TypedConfirmForm implements FormPart {
	private final Component title;
	private final Component body;
	private final Component ok;
	private final String word;
	private final Runnable onOk;
	private CFHGuiTextField field;
	private int textHeight;

	public TypedConfirmForm(Component title, Component body, String word, Component ok, Runnable onOk) {
		this.title = title;
		this.body = body;
		this.word = word;
		this.ok = ok;
		this.onOk = onOk;
	}

	@Override
	public Component title() {
		return this.title;
	}

	@Override
	public Icon icon() {
		return Icon.WARNING;
	}

	@Override
	public int preferredWidth() {
		return 228;
	}

	@Override
	public int build(int width, FormPart.Placer placer) {
		this.textHeight = CFHGuiDraw.font().split(this.body, width).size() * 10 + 4 + 10;
		this.field = new HintField(0, 0, width, Component.literal(this.word)).maxLength(Math.max(32, this.word.length()));
		placer.place(this.field, 0, this.textHeight + 4);
		return this.textHeight + 4 + 20;
	}

	@Override
	public void render(GuiGraphics g, int x, int y, int width, float alpha, int mouseX, int mouseY) {
		List<FormattedCharSequence> lines = CFHGuiDraw.font().split(this.body, width);
		int ly = y;

		for (FormattedCharSequence line : lines) {
			TeamsDraw.text(g, line, x, ly, Theme.current().palette().textMuted(), alpha);
			ly += 10;
		}

		TeamsDraw.textFit(g, TeamsText.t("ask.type", this.word), x, ly + 4, width, TeamsDraw.caption(), alpha);
	}

	@Override
	public Component submitLabel() {
		return this.ok;
	}

	@Override
	public Variant submitVariant() {
		return Variant.DANGER;
	}

	@Override
	public boolean ready() {
		return this.field.getValue().trim().equalsIgnoreCase(this.word.trim());
	}

	@Override
	public boolean mayBeRefused() {
		return false;
	}

	@Override
	public void submit(FormPart.Host host) {
		if (this.ready()) {
			host.done();
			this.onOk.run();
		}
	}

	@Nullable
	@Override
	public AbstractWidget firstFocus() {
		return this.field;
	}
}
