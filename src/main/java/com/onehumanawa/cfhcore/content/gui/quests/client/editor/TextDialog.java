package com.onehumanawa.cfhcore.content.gui.quests.client.editor;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Fonts;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton.Variant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

final class TextDialog extends WidgetPopup {
	private static final int PAD = 10;
	private static final int TITLE_H = 17;
	private static final int TOOL = 16;
	private static final int FOOT_H = 20;
	private final Component title;
	private final Consumer<List<String>> onDone;
	private final TextArea area;
	private final Bits.RichBox preview;
	private final List<CFHGuiButton> tools = new ArrayList<>();
	private final CFHGuiButton cancel;
	private final CFHGuiButton done;

	TextDialog(Component title, List<String> lines, Consumer<List<String>> onDone) {
		this.title = title;
		this.onDone = onDone;
		Minecraft mc = Minecraft.getInstance();
		this.w = Math.min(500, mc.getWindow().getGuiScaledWidth() - 16);
		this.h = Math.min(264, mc.getWindow().getGuiScaledHeight() - 16);
		int half = (this.w - 20 - 6) / 2;
		int tall = TextArea.heightIn(this.h - 20 - 17 - 16 - 4 - 20 - 6);
		this.h = tall + 20 + 17 + 16 + 4 + 20 + 6;
		this.area = this.add(new TextArea(half - TextArea.barRoom(), tall, Tr.t("text.hint"), Tr.t("text")));
		this.preview = this.add(new Bits.RichBox(this.w - 20 - half - 6, tall, lines, Tr.t("text.preview.empty")));
		this.area.set(TextArea.join(lines));
		this.area.typing(text -> this.preview.set(TextArea.split(text)));
		this.tool("bold", true, () -> this.area.type("**" + sample("bold") + "**"));
		this.tool("italic", true, () -> this.area.type("*" + sample("italic") + "*"));
		this.tool("underline", true, () -> this.area.type("__" + sample("underline") + "__"));
		this.tool("code", false, () -> this.area.type("`" + sample("code") + "`")).icon(Icon.CODE);
		this.tool("heading", true, () -> this.area.type("# "));
		this.tool("list", false, () -> this.area.type("- ")).icon(Icon.LIST);
		this.tool("rule", false, () -> this.area.type("---\n")).icon(Icon.MINUS);
		this.tool("item", false, () -> Pickers.open(Pickers.What.GIFT, "", picked -> {
				if (!picked.isBlank()) {
					this.area.type("{item:" + picked + "}");
				}

				this.refocus();
			})).icon(Icon.PACK);
		this.tool("quest", false, () -> Pickers.open(Pickers.What.QUEST, "", picked -> {
				if (!picked.isBlank()) {
					this.area.type("{quest:" + picked + "}");
				}

				this.refocus();
			})).icon(Icon.QUEST);
		this.tool("link", false, () -> this.area.type("[" + sample("link") + "](https://)")).icon(Icon.LINK);
		this.cancel = this.add(new CFHGuiButton(0, 0, 70, 20, Component.translatable("gui.cancel"), this::close));
		this.done = this.add(new CFHGuiButton(0, 0, 70, 20, Tr.t("dialog.done"), this::finish).variant(Variant.PRIMARY));
		this.focus(this.area);
	}

	private CFHGuiButton tool(String name, boolean lettered, Runnable press) {
		CFHGuiButton b = this.add(new CFHGuiButton(0, 0, 18, 16, lettered ? Tr.t("text.letter." + name) : Component.empty(), () -> {
			press.run();
			this.refocus();
		}).variant(Variant.GHOST));
		b.iconSize(8);
		b.tip(Tr.t("text.tool." + name));
		this.tools.add(b);
		return b;
	}

	private static String sample(String name) {
		return Tr.t("text.sample." + name).getString();
	}

	private void refocus() {
		this.focus(this.area);
	}

	private void finish() {
		this.close();
		this.onDone.accept(TextArea.split(this.area.getValue()));
	}

	@Override
	protected void submit() {
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if ((keyCode == 257 || keyCode == 335) && (modifiers & 2) != 0) {
			this.finish();
			return true;
		} else {
			return super.keyPressed(keyCode, scanCode, modifiers);
		}
	}

	public boolean isModal() {
		return true;
	}

	@Override
	protected void layout() {
		Minecraft mc = Minecraft.getInstance();
		int sw = mc.getWindow().getGuiScaledWidth();
		int sh = mc.getWindow().getGuiScaledHeight();
		this.x = Math.max(2, (sw - this.w) / 2);
		this.y = Math.max(2, (sh - this.h) / 2);
		int tx = this.x + 10;
		int ty = this.y + 10 + 17;

		for (CFHGuiButton b : this.tools) {
			b.setX(tx);
			b.setY(ty);
			tx += b.getWidth() + 1;
		}

		int top = ty + 16 + 4;
		this.area.setX(this.x + 10);
		this.area.setY(top);
		this.preview.setX(this.x + this.w - 10 - this.preview.getWidth());
		this.preview.setY(top);
		this.done.setWidth(Math.max(60, this.done.preferredWidth() + 10));
		this.cancel.setWidth(Math.max(60, this.cancel.preferredWidth() + 10));
		this.done.setX(this.x + this.w - 10 - this.done.getWidth());
		this.done.setY(this.y + this.h - 10 - 20);
		this.cancel.setX(this.done.getX() - 6 - this.cancel.getWidth());
		this.cancel.setY(this.done.getY());
	}

	@Override
	protected void drawUnder(GuiGraphics g, int mouseX, int mouseY, float a) {
		Theme t = Theme.current();
		Palette p = t.palette();
		boolean van = t.isVanilla();
		Icons.draw(g, Icon.TEXT, this.x + 10, this.y + 10 - 2, 12, Colors.scaleAlpha(van ? -1 : p.accentInk(), a));
		g.drawString(
			CFHGuiDraw.font(),
			CFHGuiDraw.truncate(Fonts.heading(this.title), this.w - 20 - 16),
			this.x + 10 + 16,
			this.y + 10,
			Colors.scaleAlpha(van ? -1 : p.text(), a),
			van
		);
		Component label = Tr.t("text.preview");
		g.drawString(
			CFHGuiDraw.font(),
			label,
			this.preview.getX() + this.preview.getWidth() - CFHGuiDraw.width(label),
			this.y + 10 + 17 + 4,
			Colors.scaleAlpha(van ? -6250336 : p.textDim(), a),
			van
		);
		g.drawString(
			CFHGuiDraw.font(),
			CFHGuiDraw.truncate(Tr.t("text.foot"), this.cancel.getX() - this.x - 10 - 8),
			this.x + 10,
			this.y + this.h - 10 - 20 + 6,
			Colors.scaleAlpha(van ? -8355712 : p.textDim(), a),
			van
		);
	}
}
