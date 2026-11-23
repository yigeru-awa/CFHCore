package com.onehumanawa.cfhcore.content.gui.server.client.ui.admin;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Fonts;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.core.perm.Perms.Node;
import com.onehumanawa.cfhcore.content.gui.core.screen.SidebarScreen;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiContextMenu;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiIconButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiLabel;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiScrollPanel;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSpinner;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton.Variant;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiLabel.Style;
import com.onehumanawa.cfhcore.content.gui.core.widget.popup.MenuPopup.Item;
import com.onehumanawa.cfhcore.content.gui.server.Nodes;
import com.onehumanawa.cfhcore.content.gui.server.client.ServerActions;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.ServerPage;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.dialog.AdminForms;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.dialog.Forms;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.kit.PaintedCard;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.kit.Txt;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.kit.Ui;
import com.onehumanawa.cfhcore.content.gui.server.model.Kit;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

final class AdminKitsPage extends ServerPage {
	@Nullable
	private List<Kit> kits;
	private CFHGuiScrollPanel panel;
	private CFHGuiLabel count;
	private CFHGuiButton make;

	AdminKitsPage(AdminScreen screen) {
		super(screen, "kits", Txt.adminPage("kits"), Icon.PACK);
	}

	@Override
	protected Node node() {
		return Nodes.ADMIN_KITS;
	}

	@Override
	public void stateChanged() {
	}

	@Override
	protected void buildPage(SidebarScreen s, Rect area) {
		ServerPage.Bar bar = new ServerPage.Bar(s, area);
		this.make = new CFHGuiButton(0, 0, 120, Txt.t("kit.new"), () -> new AdminForms.KitForm(null, this.known(), this::ask).show())
			.variant(Variant.PRIMARY)
			.icon(Icon.PLUS);
		this.make.setWidth(this.make.preferredWidth() + 14);
		this.make.tip(Txt.t("kit.new.tip"));
		bar.right(this.make);
		this.count = bar.left(new CFHGuiLabel(0, 0, Math.max(40, bar.room() - 8), Component.empty()).style(Style.MUTED));
		Rect r = under(area);
		this.panel = (CFHGuiScrollPanel)s.addPageWidget(new CFHGuiScrollPanel(r.x(), r.y(), r.w(), r.h()));
		this.fill();
		if (this.kits == null) {
			this.ask();
		}
	}

	private void ask() {
		ServerActions.adminKits(answer -> {
			this.kits = answer.orElseGet(ArrayList::new);
			if (this.panel != null) {
				refill(this.panel, this::fill);
			}
		});
	}

	private List<Kit> known() {
		return this.kits == null ? List.of() : this.kits;
	}

	private void fill() {
		int w = this.panel.innerWidth();
		List<Kit> all = this.kits;
		this.make.active = all != null;
		if (all != null && !all.isEmpty()) {
			this.count.text(Txt.t("kit.admin.count", all.size()));
			int y = grid(this.panel, 0, w, all.size(), 230, 72, (i, cw) -> this.card(all.get(i), cw));
			this.panel.setContentHeight(y + 2);
		} else {
			int h = Math.max(96, this.panel.getHeight() - 4);
			this.panel.add(new PaintedCard(w, h, (g, f) -> {
				if (all == null) {
					CFHGuiSpinner.draw(g, f.x() + f.w() / 2 - 6, f.y() + f.h() / 2 - 6, 12, Ui.soft());
				} else {
					Ui.empty(g, new Rect(f.x(), f.y(), f.w(), f.h()), Icon.PACK, Txt.t("kit.admin.none.title"), Txt.t("kit.admin.none"), f.alpha());
				}
			}).bare(), 0, 0);
			this.panel.setContentHeight(h);
			this.count.text(Component.empty());
		}
	}

	private static List<AdminKitsPage.Entry> entries(Kit kit) {
		List<AdminKitsPage.Entry> out = new ArrayList<>();

		for (String snbt : kit.items) {
			try {
				CompoundTag tag = TagParser.parseTag(snbt);
				out.add(new AdminKitsPage.Entry(tag.getString("id"), tag.contains("count") ? tag.getInt("count") : (tag.contains("Count") ? tag.getInt("Count") : 1)));
			} catch (CommandSyntaxException var5) {
				out.add(new AdminKitsPage.Entry("", 1));
			}
		}

		return out;
	}

	private PaintedCard card(Kit kit, int w) {
		List<AdminKitsPage.Entry> entries = entries(kit);
		String mark = !kit.icon.isEmpty() ? kit.icon : (!entries.isEmpty() && !entries.get(0).item().isEmpty() ? entries.get(0).item() : "PACK");
		PaintedCard card = new PaintedCard(
				w,
				72,
				(g, f) -> {
					Palette p = Theme.current().palette();
					float a = f.alpha();
					Ui.markTile(g, mark, Icon.PACK, f.x() + 8, f.y() + 8, 26, p.accentInk() & 16777215, a);
					int used = Ui.textFit(g, Fonts.heading(kit.name.isEmpty() ? kit.id : kit.name), f.x() + 42, f.y() + 9, f.w() - 76, Ui.strong(), a);
					if (used + 60 < f.w() - 76) {
						Ui.textFit(g, Component.literal("/kit " + kit.id), f.x() + 48 + used, f.y() + 9, f.w() - 82 - used, Ui.caption(), a);
					}

					int cx = f.x() + 42;
					cx += Ui.chip(
							g, kit.cooldownSeconds > 0 ? Txt.t("kit.every", Txt.span((long)kit.cooldownSeconds)) : Txt.t("kit.anytime"), cx, f.y() + 21, p.textMuted(), a
						)
						+ 4;
					if (kit.oneTime) {
						cx += Ui.chip(g, Txt.t("kit.flag.once"), cx, f.y() + 21, p.warning(), a) + 4;
					}

					if (kit.firstJoin) {
						cx += Ui.chip(g, Txt.t("kit.flag.first_join"), cx, f.y() + 21, p.success(), a) + 4;
					}

					if (kit.restricted && cx + 50 < f.x() + f.w()) {
						Ui.chip(g, Txt.t("kit.flag.restricted"), cx, f.y() + 21, p.danger(), a);
					}

					int ix = f.x() + 8;
					int iy = f.y() + 46;
					int most = Math.max(1, (f.w() - 20) / 19);

					for (int i = 0; i < entries.size() && i < most; i++) {
						AdminKitsPage.Entry e = entries.get(i);
						ItemStack stack = Ui.stack(e.item());
						Ui.well(g, ix, iy, 18, 18, a);
						if (!stack.isEmpty()) {
							Ui.item(g, stack, ix + 1, iy + 1, 16, a);
							if (e.count() > 1) {
								g.pose().pushPose();
								g.pose().translate(0.0F, 0.0F, 200.0F);
								String n = Integer.toString(e.count());
								g.drawString(CFHGuiDraw.font(), n, ix + 18 - CFHGuiDraw.width(n), iy + 10, -1, true);
								g.pose().popPose();
							}

							f.tip(ix - f.x(), iy - f.y(), 18, 18, Component.literal(e.count() + " × ").append(stack.getHoverName()));
						}

						ix += 19;
					}

					if (entries.isEmpty()) {
						Ui.text(g, Txt.t("kit.admin.empty"), f.x() + 8, iy + 5, Ui.caption(), a);
					} else if (entries.size() > most) {
						Ui.text(g, "+" + (entries.size() - most), ix + 2, iy + 5, Ui.caption(), a);
					}
				}
			)
			.flat()
			.narrate(Component.literal(kit.name));
		CFHGuiIconButton more = new CFHGuiIconButton(0, 0, 18, Icon.DOTS, Txt.t("more"), null);
		more.onPress(() -> this.menu(kit, more.getX() - 120, more.getY() + 19));
		card.add(more, w - 26, 6);
		card.onRightClick(() -> this.menu(kit, Ui.mouseX(), Ui.mouseY()));
		return card;
	}

	private void menu(Kit kit, int x, int y) {
		List<Item> items = new ArrayList<>();
		items.add(Item.of(Txt.t("kit.edit"), Icon.EDIT, () -> new AdminForms.KitForm(kit, this.known(), this::ask).show()));
		items.add(
			Item.of(
				Txt.t("kit.refill"),
				Icon.REFRESH,
				() -> Forms.confirm(
						Txt.t("kit.refill.title", kit.name), Txt.t("kit.refill.body"), Txt.t("kit.refill"), () -> ServerActions.kitSave(kit, true, AdminScreen.after(this::ask))
					)
			)
		);
		items.add(
			Item.of(
				Txt.t("kit.give"),
				Icon.USER,
				() -> new Forms.Prompt(
							Txt.t("kit.give.title", kit.name),
							Icon.PACK,
							Txt.t("kit.give.caption"),
							"",
							32,
							Txt.t("kit.give.ok"),
							v -> v.matches("[A-Za-z0-9_]{2,16}"),
							(v, then) -> {
								CompoundTag a = new CompoundTag();
								a.putString("player", v);
								a.putString("kit", kit.id);
								ServerActions.send("kit_give", a, then);
							}
						)
						.show()
			)
		);
		items.add(Item.sep());
		items.add(
			Item.danger(
				Txt.t("form.delete"),
				Icon.TRASH,
				() -> Forms.confirm(
						Txt.t("kit.delete.title", kit.name), Txt.t("kit.delete.body"), Txt.t("form.delete"), () -> ServerActions.kitDelete(kit.id, AdminScreen.after(this::ask))
					)
			)
		);
		CFHGuiContextMenu.open((double)x, (double)y, items);
	}

	private static record Entry(String item, int count) {
	}
}
