package com.onehumanawa.cfhcore.content.gui.quests.client.ui;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Fonts;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSounds;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiTooltips;
import com.onehumanawa.cfhcore.content.gui.core.widget.popup.Popup;
import com.onehumanawa.cfhcore.content.gui.core.widget.popup.Popups;
import com.onehumanawa.cfhcore.content.gui.quests.client.text.QuestText;
import com.onehumanawa.cfhcore.content.gui.quests.client.text.RichTextView;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.ItemSpecs;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.QuestIcons;
import com.onehumanawa.cfhcore.content.gui.quests.model.Reward;
import java.util.List;
import java.util.function.IntConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

final class RewardChoicePopup implements Popup {
	private static final int ROW = 22;
	private static final int PAD = 4;
	private static final int HEAD = 15;
	private static final int WIDTH = 156;
	private final Reward reward;
	private final IntConsumer onPick;
	private final int x;
	private final int y;
	private final int w;
	private final int h;
	private final boolean upward;
	private int hovered = -1;
	private int lastMouseX = Integer.MIN_VALUE;
	private int lastMouseY = Integer.MIN_VALUE;
	private final Anim open = new Anim(0.0F, 150, Ease.OUT_CUBIC);
	private boolean closing;

	RewardChoicePopup(int right, int top, Reward reward, IntConsumer onPick) {
		this.reward = reward;
		this.onPick = onPick;
		Minecraft mc = Minecraft.getInstance();
		int sw = mc.getWindow().getGuiScaledWidth();
		int sh = mc.getWindow().getGuiScaledHeight();
		int widest = CFHGuiDraw.width(Fonts.heading(Component.translatable("cfhgui_quests.sheet.choose.title")));

		for (Reward c : reward.choices) {
			widest = Math.max(widest, 26 + CFHGuiDraw.width(QuestText.label(c)));
		}

		this.w = Mth.clamp(widest + 8 + 6, 110, Math.min(216, sw - 8));
		this.h = 15 + reward.choices.size() * 22 + 8;
		this.x = Mth.clamp(right - this.w, 4, Math.max(4, sw - 4 - this.w));
		this.upward = top + this.h > sh - 4;
		this.y = this.upward ? Math.max(4, top - 20 - this.h) : top;
		this.open.snap(0.0F);
		this.open.set(1.0F);
	}

	public boolean contains(double mouseX, double mouseY) {
		return !this.closing && mouseX >= (double)this.x && mouseX < (double)(this.x + this.w) && mouseY >= (double)this.y && mouseY < (double)(this.y + this.h);
	}

	private int rowAt(double mouseY) {
		int i = (int)Math.floor((mouseY - (double)this.y - 15.0 - 4.0) / 22.0);
		return i >= 0 && i < this.reward.choices.size() ? i : -1;
	}

	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		Theme t = Theme.current();
		Palette p = t.palette();
		boolean van = t.isVanilla();
		Font font = CFHGuiDraw.font();
		float o = Mth.clamp(this.open.get(), 0.0F, 1.0F);
		float a = Math.min(1.0F, o * 1.4F);
		if (!(a <= 0.02F)) {
			if (!this.closing && (mouseX != this.lastMouseX || mouseY != this.lastMouseY)) {
				this.lastMouseX = mouseX;
				this.lastMouseY = mouseY;
				if (this.contains((double)mouseX, (double)mouseY)) {
					this.hovered = this.rowAt((double)mouseY);
				}
			}

			int yy = this.y + Math.round((1.0F - o) * (float)(this.upward ? 4 : -4));
			if (van) {
				g.fill(this.x, yy, this.x + this.w, yy + this.h, Colors.withAlpha(0, Math.round(240.0F * a)));
				CFHGuiDraw.outline(g, this.x, yy, this.w, this.h, Colors.withAlpha(16777215, Math.round(255.0F * a)), 0);
			} else {
				CFHGuiDraw.popupPlate(g, this.x, yy, this.w, this.h, p.surface(), p.borderStrong(), 0.6F, a);
			}

			g.drawString(
				font, Fonts.heading(Component.translatable("cfhgui_quests.sheet.choose.title")), this.x + 4 + 3, yy + 5, Colors.scaleAlpha(van ? -1 : p.textMuted(), a), van
			);

			for (int i = 0; i < this.reward.choices.size(); i++) {
				Reward c = this.reward.choices.get(i);
				int ry = yy + 15 + 4 + i * 22;
				boolean hov = i == this.hovered;
				if (hov) {
					CFHGuiDraw.pixelRound(g, this.x + 2, ry, this.w - 4, 21, Colors.scaleAlpha(van ? -12566464 : p.surfaceHover(), a), van ? 0 : 2);
				}

				SheetParts.slot(g, this.x + 4 + 1, ry + 2, van, a);
				if (a > 0.6F) {
					QuestIcons.of(c).draw(g, (float)(this.x + 4 + 10), (float)(ry + 11), 16.0F, van ? -1 : p.textMuted());
				}

				g.drawString(
					font,
					CFHGuiDraw.truncate(QuestText.label(c), this.w - 8 - 26),
					this.x + 4 + 24,
					ry + 7,
					Colors.scaleAlpha(hov ? (van ? -96 : p.text()) : (van ? -2039584 : p.textMuted()), a),
					van
				);
			}

			if (!this.closing && this.hovered >= 0 && this.contains((double)mouseX, (double)mouseY) && this.rowAt((double)mouseY) == this.hovered) {
				Reward cx = this.reward.choices.get(this.hovered);
				if (cx.is("item") && !cx.item.isBlank()) {
					List<Component> tip = RichTextView.itemTip(ItemSpecs.stack(cx.item), ItemSpecs.name(cx.item));
					if (!tip.isEmpty()) {
						CFHGuiTooltips.request(tip, null);
					}
				}
			}
		}
	}

	private void pick(int i) {
		if (i >= 0 && i < this.reward.choices.size()) {
			CFHGuiSounds.click();
			Popups.close(this);
			this.onPick.accept(i);
		}
	}

	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 0) {
			this.pick(this.rowAt(mouseY));
		}

		return true;
	}

	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		int n = this.reward.choices.size();
		if (n == 0) {
			return false;
		} else if (keyCode == 264) {
			this.hovered = Math.floorMod(this.hovered + 1, n);
			return true;
		} else if (keyCode == 265) {
			this.hovered = Math.floorMod(this.hovered < 0 ? n - 1 : this.hovered - 1, n);
			return true;
		} else if ((keyCode == 257 || keyCode == 335 || keyCode == 32) && this.hovered >= 0) {
			this.pick(this.hovered);
			return true;
		} else {
			return false;
		}
	}

	public boolean beginClose() {
		if (Theme.current().motion() <= 0.0F) {
			return false;
		} else {
			this.closing = true;
			this.open.set(0.0F, 100);
			return true;
		}
	}

	public boolean closeFinished() {
		return this.open.get() <= 0.02F;
	}
}
