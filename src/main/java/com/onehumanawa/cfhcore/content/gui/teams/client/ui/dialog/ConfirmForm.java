package com.onehumanawa.cfhcore.content.gui.teams.client.ui.dialog;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton.Variant;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamsDraw;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;

public final class ConfirmForm implements FormPart {
	private final Component title;
	private final Component body;
	private final Component ok;
	private final boolean danger;
	private final Runnable onOk;
	private int height;

	public ConfirmForm(Component title, Component body, Component ok, boolean danger, Runnable onOk) {
		this.title = title;
		this.body = body;
		this.ok = ok;
		this.danger = danger;
		this.onOk = onOk;
	}

	@Override
	public Component title() {
		return this.title;
	}

	@Nullable
	@Override
	public Icon icon() {
		return this.danger ? Icon.WARNING : null;
	}

	@Override
	public int preferredWidth() {
		return 228;
	}

	@Override
	public int build(int width, FormPart.Placer placer) {
		this.height = Math.max(1, balanced(this.body, width).size()) * 10 + 2;
		return this.height;
	}

	private static List<FormattedCharSequence> balanced(Component text, int width) {
		Font font = CFHGuiDraw.font();
		int lines = font.split(text, width).size();
		if (lines <= 1) {
			return font.split(text, width);
		} else {
			int low = width / 2;
			int high = width;

			while (low < high) {
				int mid = (low + high) / 2;
				if (font.split(text, mid).size() <= lines) {
					high = mid;
				} else {
					low = mid + 1;
				}
			}

			return font.split(text, low);
		}
	}

	@Override
	public void render(GuiGraphics g, int x, int y, int width, float alpha, int mouseX, int mouseY) {
		List<FormattedCharSequence> lines = balanced(this.body, width);
		int ly = y + 1;

		for (FormattedCharSequence line : lines) {
			TeamsDraw.text(g, line, x + (width - CFHGuiDraw.font().width(line)) / 2, ly, Theme.current().palette().text(), alpha);
			ly += 10;
		}
	}

	@Override
	public Component submitLabel() {
		return this.ok;
	}

	@Override
	public Variant submitVariant() {
		return this.danger ? Variant.DANGER : Variant.PRIMARY;
	}

	@Override
	public boolean ready() {
		return true;
	}

	@Override
	public boolean mayBeRefused() {
		return false;
	}

	@Override
	public void submit(FormPart.Host host) {
		host.done();
		this.onOk.run();
	}
}
