package com.onehumanawa.cfhcore.content.gui.teams.client.ui.dialog;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamsText;
import com.onehumanawa.cfhcore.content.gui.teams.model.BannerData;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public final class BannerForm implements FormPart {
	private final BannerData initial;
	private final Consumer<BannerData> onSave;
	private BannerEditor editor;

	public BannerForm(BannerData initial, Consumer<BannerData> onSave) {
		this.initial = initial;
		this.onSave = onSave;
	}

	@Override
	public Component title() {
		return TeamsText.t("banner.title");
	}

	@Override
	public Icon icon() {
		return Icon.BRUSH;
	}

	@Override
	public int preferredWidth() {
		return width(0);
	}

	private static int width(int size) {
		return BannerEditor.minWidth(size) + 2;
	}

	@Override
	public int widthFor(int roomWidth, int roomHeight) {
		return Math.min(width(BannerEditor.sizeFor(roomWidth - 2, roomHeight)), roomWidth);
	}

	@Override
	public int build(int width, FormPart.Placer placer) {
		int size = 0;

		for (int s = 2; s > 0; s--) {
			if (width(s) <= width) {
				size = s;
				break;
			}
		}

		boolean captions = size > 0 || Minecraft.getInstance().getWindow().getGuiScaledHeight() >= 256;
		int editorW = Math.min(width, width(size));
		this.editor = new BannerEditor(0, 0, editorW, this.initial, captions, size);
		placer.place(this.editor, (width - editorW) / 2, 0);
		return this.editor.getHeight();
	}

	@Override
	public Component submitLabel() {
		return TeamsText.t("banner.save");
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
		this.onSave.accept(this.editor.value());
		host.done();
	}

	@Nullable
	@Override
	public AbstractWidget firstFocus() {
		return this.editor;
	}
}
