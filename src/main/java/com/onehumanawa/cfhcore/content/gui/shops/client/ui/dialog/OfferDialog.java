package com.onehumanawa.cfhcore.content.gui.shops.client.ui.dialog;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiTextField;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiTooltips;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton.Variant;
import com.onehumanawa.cfhcore.content.gui.shops.client.ShopsActions;
import com.onehumanawa.cfhcore.content.gui.shops.client.ShopsState;
import com.onehumanawa.cfhcore.content.gui.shops.client.ui.kit.ShopDialog;
import com.onehumanawa.cfhcore.content.gui.shops.client.ui.kit.Txt;
import com.onehumanawa.cfhcore.content.gui.shops.client.ui.kit.Ui;
import com.onehumanawa.cfhcore.content.gui.shops.model.Offer;
import com.onehumanawa.cfhcore.content.gui.shops.model.Shop;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class OfferDialog extends ShopDialog {
	private final Shop shop;
	@Nullable
	private final Offer edit;
	@Nullable
	private final OfferDialog.SlotGrid grid;
	private final CFHGuiTextField price;
	private final CFHGuiTextField sellPrice;
	private final CFHGuiButton ok;

	public OfferDialog(Shop shop, @Nullable Offer edit) {
		super(Txt.t(edit == null ? "offer.new.title" : "offer.edit.title"), Icon.TAG, 300);
		this.shop = shop;
		this.edit = edit;
		if (edit == null) {
			this.caption(Txt.t("offer.new.item"), 0, this.cursor);
			this.grid = this.add(new OfferDialog.SlotGrid(), (this.bodyWidth() - 162) / 2, this.cursor + 11);
			this.cursor += 94;
		} else {
			this.grid = null;
			this.cursor = 30;
		}

		int half = (this.bodyWidth() - 8) / 2;
		this.caption(Txt.t("offer.field.price"), 0, this.cursor);
		this.caption(Txt.t("offer.field.sell_price"), half + 8, this.cursor);
		this.price = this.add(number(edit == null ? "" : Long.toString(edit.price), half), 0, this.cursor + 11);
		this.sellPrice = this.add(number(edit == null ? "" : Long.toString(edit.sellPrice), half), half + 8, this.cursor + 11);
		this.cursor += 33;
		this.hint(Txt.t("offer.field.hint"), 0, this.cursor);
		this.cursor += 18;
		this.ok = this.buttons(Txt.t(edit == null ? "offer.new.ok" : "form.save"), Variant.PRIMARY, this::go);
		this.firstFocus(this.price);
	}

	private static CFHGuiTextField number(String value, int width) {
		CFHGuiTextField f = new CFHGuiTextField(0, 0, width, Component.empty()).maxLength(10);
		f.setFilter(s -> s.matches("\\d{0,10}"));
		f.setValue(value);
		f.placeholder(Component.literal("0"));
		return f;
	}

	private static long value(CFHGuiTextField f) {
		try {
			return f.getValue().isEmpty() ? 0L : Long.parseLong(f.getValue());
		} catch (NumberFormatException var2) {
			return -1L;
		}
	}

	@Override
	protected void update() {
		long p = value(this.price);
		long s = value(this.sellPrice);
		boolean prices = p >= 0L && s >= 0L && p <= 1000000000L && s <= 1000000000L && p + s > 0L;
		this.ok.active = prices && !this.waiting && (this.grid == null || this.grid.selected >= 0);
	}

	private void go() {
		long p = value(this.price);
		long s = value(this.sellPrice);
		if (this.edit == null) {
			this.send(then -> ShopsActions.offerAdd(this.shop.id, this.grid.selected, p, s, then));
		} else {
			this.send(then -> ShopsActions.offerEdit(this.shop.id, this.edit.id, p, s, then));
		}
	}

	@Override
	protected void renderBody(GuiGraphics g, int x, int y, int w, float alpha, int mouseX, int mouseY) {
		if (this.edit != null) {
			ItemStack stack = ShopsState.stack(this.edit);
			Ui.well(g, x, y + 1, 22, 22, alpha);
			if (!stack.isEmpty()) {
				Ui.item(g, stack, x + 3, y + 4, 16, alpha);
			}

			Ui.textFit(
				g,
				stack.isEmpty() ? Txt.t("offer.unknown") : Component.literal(stack.getCount() + " × ").append(stack.getHoverName()),
				x + 28,
				y + 8,
				w - 28,
				Ui.strong(),
				alpha
			);
		}
	}

	private static final class SlotGrid extends AbstractWidget {
		static final int CELL = 18;
		static final int W = 162;
		static final int H = 76;
		int selected = -1;

		SlotGrid() {
			super(0, 0, 162, 76, Component.empty());
		}

		private static List<ItemStack> slots() {
			Minecraft mc = Minecraft.getInstance();
			return (List<ItemStack>)(mc.player == null ? List.of() : mc.player.getInventory().items);
		}

		private static int slotAt(int col, int row) {
			return row < 3 ? 9 + row * 9 + col : col;
		}

		private int cellY(int row) {
			return this.getY() + row * 18 + (row == 3 ? 4 : 0);
		}

		protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
			List<ItemStack> slots = slots();
			float a = this.alpha;
			int accent = Theme.current().palette().accentInk();

			for (int row = 0; row < 4; row++) {
				for (int col = 0; col < 9; col++) {
					int slot = slotAt(col, row);
					int cx = this.getX() + col * 18;
					int cy = this.cellY(row);
					Ui.well(g, cx + 1, cy + 1, 16, 16, a);
					ItemStack stack = slot < slots.size() ? slots.get(slot) : ItemStack.EMPTY;
					if (!stack.isEmpty()) {
						g.renderItem(stack, cx + 1, cy + 1);
						g.renderItemDecorations(CFHGuiDraw.font(), stack, cx + 1, cy + 1);
						boolean over = mouseX >= cx && mouseX < cx + 18 && mouseY >= cy && mouseY < cy + 18;
						if (over) {
							CFHGuiDraw.rect(g, cx + 1, cy + 1, 16, 16, 822083583);
							CFHGuiTooltips.request(Screen.getTooltipFromItem(Minecraft.getInstance(), stack), this);
						}

						if (slot == this.selected) {
							CFHGuiDraw.outline(g, cx, cy, 18, 18, Colors.scaleAlpha(accent, a), 0);
						}
					}
				}
			}
		}

		public void onClick(double mouseX, double mouseY) {
			for (int row = 0; row < 4; row++) {
				int cy = this.cellY(row);
				if (!(mouseY < (double)cy) && !(mouseY >= (double)(cy + 18))) {
					int col = (int)((mouseX - (double)this.getX()) / 18.0);
					if (col >= 0 && col <= 8) {
						int slot = slotAt(col, row);
						List<ItemStack> slots = slots();
						if (slot < slots.size() && !slots.get(slot).isEmpty()) {
							this.selected = slot;
						}

						return;
					}

					return;
				}
			}
		}

		protected void updateWidgetNarration(NarrationElementOutput out) {
			out.add(NarratedElementType.TITLE, Txt.t("offer.new.item"));
		}
	}
}
