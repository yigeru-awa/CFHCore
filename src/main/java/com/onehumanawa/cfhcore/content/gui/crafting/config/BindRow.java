package com.onehumanawa.cfhcore.content.gui.crafting.config;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.InputConstants.Type;
import com.onehumanawa.cfhcore.content.gui.emi.input.EmiBind;
import com.onehumanawa.cfhcore.content.gui.emi.input.EmiInput;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;

final class BindRow extends SettingLine {
	private static final int KEY_MAX_W = 96;
	private static final int PLUS_W = 16;
	private final EmiBind bind;
	private final Runnable changed;
	private int waiting = -1;
	private int modifiers;
	private int lastModifier;

	BindRow(int width, EmiBind bind, @Nullable Component tooltip, Runnable changed) {
		super(width, Component.translatable(bind.translationKey), tooltip);
		this.bind = bind;
		this.changed = changed;
	}

	private boolean isPlus(int index) {
		return index > 0 && index < this.bind.boundKeys.size() && this.bind.boundKeys.get(index).isUnbound();
	}

	private Component text(int index) {
		if (index == this.waiting) {
			return this.lastModifier == 0
				? Component.literal("...")
				: new EmiBind.ModifiedKey(Type.KEYSYM.getOrCreate(this.lastModifier), this.modifiers).getKeyText(ChatFormatting.RESET);
		} else {
			EmiBind.ModifiedKey key = this.bind.boundKeys.get(index);
			return key.isUnbound() ? Component.translatable("cfhgui_crafting.settings.bind.none") : key.getKeyText(ChatFormatting.RESET);
		}
	}

	@Override
	protected int cellCount() {
		return this.bind.boundKeys.size();
	}

	@Override
	protected int cellWidth(int index) {
		if (this.isPlus(index) && index != this.waiting) {
			return 16;
		} else {
			int want = Math.max(28, CFHGuiDraw.width(this.text(index)) + 12);
			return Math.min(want, Math.max(96, this.spare(index)));
		}
	}

	private int spare(int index) {
		int others = 0;

		for (int i = 0; i < this.cellCount(); i++) {
			if (i != index) {
				others += 3 + (this.isPlus(i) && i != this.waiting ? 16 : Math.max(28, Math.min(96, CFHGuiDraw.width(this.text(i)) + 12)));
			}
		}

		return this.getWidth() - 22 - CFHGuiDraw.width(this.getMessage()) - 6 - 20 - 2 - others;
	}

	@Override
	protected void drawCell(GuiGraphics g, SettingLine.Cell cell, int y, boolean hovered, int mouseX, int mouseY) {
		int i = cell.index();
		boolean plus = this.isPlus(i) && i != this.waiting;
		boolean none = !plus && i != this.waiting && this.bind.boundKeys.get(i).isUnbound();
		int fg = face(g, cell.x(), y, cell.width(), hovered, i == this.waiting, plus || none);
		if (plus) {
			Icons.draw(g, Icon.PLUS, cell.x() + (cell.width() - 8) / 2, y + 4, 8, fg);
		} else {
			FormattedCharSequence line = CFHGuiDraw.truncate(this.text(i), cell.width() - 8);
			g.drawString(CFHGuiDraw.font(), line, cell.x() + (cell.width() - CFHGuiDraw.width(line)) / 2, CFHGuiDraw.textY(y, 16), fg, Theme.current().isVanilla());
		}
	}

	@Override
	protected void pressCell(int index, int button) {
		if (button == 0) {
			this.waiting = index;
			this.modifiers = 0;
			this.lastModifier = 0;
			this.setFocused(true);
		}
	}

	private void pushModifier(int modifier) {
		this.modifiers = this.modifiers | EmiInput.maskFromCode(this.lastModifier);
		this.lastModifier = modifier;
		this.modifiers = this.modifiers & ~EmiInput.maskFromCode(modifier);
	}

	private void take(EmiBind.ModifiedKey key) {
		this.bind.setBind(this.waiting, key);
		this.waiting = -1;
		this.changed.run();
	}

	@Override
	protected boolean capturePress(int button) {
		if (this.waiting < 0) {
			return false;
		} else {
			this.pushModifier(0);
			this.take(new EmiBind.ModifiedKey(Type.MOUSE.getOrCreate(button), this.modifiers));
			return true;
		}
	}

	public boolean keyPressed(int keyCode, int scanCode, int mods) {
		if (this.waiting < 0) {
			return false;
		} else {
			if (EmiInput.maskFromCode(keyCode) != 0) {
				this.pushModifier(keyCode);
			} else {
				this.pushModifier(0);
				if (keyCode == 256) {
					this.take(new EmiBind.ModifiedKey(InputConstants.UNKNOWN, 0));
				} else {
					this.take(new EmiBind.ModifiedKey(Type.KEYSYM.getOrCreate(keyCode), this.modifiers));
				}
			}

			return true;
		}
	}

	public boolean keyReleased(int keyCode, int scanCode, int mods) {
		if (this.waiting < 0) {
			return false;
		} else {
			this.modifiers = this.modifiers & ~EmiInput.maskFromCode(keyCode);
			if (keyCode == this.lastModifier) {
				this.take(new EmiBind.ModifiedKey(Type.KEYSYM.getOrCreate(keyCode), this.modifiers));
			}

			return true;
		}
	}

	public void setFocused(boolean focused) {
		super.setFocused(focused);
		if (!focused) {
			this.waiting = -1;
		}
	}

	private static List<EmiBind.ModifiedKey> bound(List<EmiBind.ModifiedKey> keys) {
		List<EmiBind.ModifiedKey> out = new ArrayList<>();

		for (EmiBind.ModifiedKey k : keys) {
			if (!k.isUnbound()) {
				out.add(k);
			}
		}

		return out;
	}

	@Override
	protected boolean isDefault() {
		return bound(this.bind.boundKeys).equals(bound(this.bind.defaultKeys));
	}

	@Override
	protected void reset() {
		this.waiting = -1;
		this.bind.setToDefault();
		this.changed.run();
	}
}
