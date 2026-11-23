package com.onehumanawa.cfhcore.content.gui.server.client.ui.hub;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Fonts;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.core.perm.Perms.Node;
import com.onehumanawa.cfhcore.content.gui.core.screen.SidebarScreen;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiScrollPanel;
import com.onehumanawa.cfhcore.content.gui.server.Nodes;
import com.onehumanawa.cfhcore.content.gui.server.client.ServerActions;
import com.onehumanawa.cfhcore.content.gui.server.client.ServerState;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.ServerPage;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.ServerScreen;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.kit.PaintedCard;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.kit.Txt;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.kit.Ui;
import com.onehumanawa.cfhcore.content.gui.server.model.Kit;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

final class KitsPage extends ServerPage {
	private CFHGuiScrollPanel panel;
	private boolean anyWaiting;
	private int ticks;

	KitsPage(ServerScreen screen) {
		super(screen, "kits", Txt.page("kits"), Icon.PACK);
	}

	@Override
	protected String feature() {
		return "kits";
	}

	@Override
	protected Node node() {
		return Nodes.KIT_USE;
	}

	@Override
	protected void buildPage(SidebarScreen s, Rect area) {
		this.panel = (CFHGuiScrollPanel)s.addPageWidget(new CFHGuiScrollPanel(area.x(), area.y(), area.w(), area.h()));
		this.fill();
	}

	@Override
	public void stateChanged() {
		if (this.panel == null) {
			super.stateChanged();
		} else {
			refill(this.panel, this::fill);
		}
	}

	public void tick() {
		if (this.panel != null && this.anyWaiting && ++this.ticks % 20 == 0) {
			long now = System.currentTimeMillis();

			for (Kit.View k : ServerState.self().kits) {
				if (k.readyAt > 0L && k.readyAt <= now) {
					this.stateChanged();
					return;
				}
			}
		}
	}

	private void fill() {
		List<Kit.View> kits = ServerState.self().kits;
		int w = this.panel.innerWidth();
		this.anyWaiting = false;
		if (kits.isEmpty()) {
			int h = Math.max(96, this.panel.getHeight() - 4);
			this.panel
				.add(
					new PaintedCard(w, h, (g, f) -> Ui.empty(g, new Rect(f.x(), f.y(), f.w(), f.h()), Icon.PACK, Txt.t("kit.none.title"), Txt.t("kit.none"), f.alpha()))
						.bare(),
					0,
					0
				);
			this.panel.setContentHeight(h);
		} else {
			int y = grid(this.panel, 0, w, kits.size(), 220, 76, (i, cw) -> this.kit(kits.get(i), cw));
			this.panel.setContentHeight(y + 2);
		}
	}

	private PaintedCard kit(Kit.View kit, int w) {
		long now = System.currentTimeMillis();
		boolean taken = kit.readyAt < 0L;
		boolean waiting = kit.readyAt > now;
		if (waiting) {
			this.anyWaiting = true;
		}

		String mark = !kit.icon.isEmpty() ? kit.icon : (kit.contents.isEmpty() ? "PACK" : kit.contents.get(0).item);
		PaintedCard card = new PaintedCard(w, 76, (g, f) -> {
			Theme t = Theme.current();
			Palette p = t.palette();
			float a = f.alpha() * (taken ? 0.6F : 1.0F);
			Ui.markTile(g, mark, Icon.PACK, f.x() + 8, f.y() + 8, 26, p.accentInk() & 16777215, a);
			Ui.textFit(g, Fonts.heading(kit.name.isEmpty() ? kit.id : kit.name), f.x() + 42, f.y() + 9, f.w() - 50, Ui.strong(), a);
			int total = 0;

			for (Kit.View.Entry e : kit.contents) {
				total += e.count;
			}

			Ui.textFit(g, Txt.t("kit.contents", kit.contents.size(), total), f.x() + 42, f.y() + 21, f.w() - 50, Ui.soft(), a);
			int ix = f.x() + 8;
			int iy = f.y() + 44;
			int most = Math.max(1, (f.w() - 16 - 92) / 19);

			for (int i = 0; i < kit.contents.size() && i < most; i++) {
				Kit.View.Entry e = kit.contents.get(i);
				ItemStack stack = Ui.stack(e.item);
				Ui.well(g, ix, iy, 18, 18, a);
				if (!stack.isEmpty()) {
					Ui.item(g, stack, ix + 1, iy + 1, 16, a);
					if (e.count > 1) {
						g.pose().pushPose();
						g.pose().translate(0.0F, 0.0F, 200.0F);
						String n = Integer.toString(e.count);
						g.drawString(CFHGuiDraw.font(), n, ix + 18 - CFHGuiDraw.width(n), iy + 10, -1, true);
						g.pose().popPose();
					}

					f.tip(ix - f.x(), iy - f.y(), 18, 18, Component.literal(e.count + " × ").append(stack.getHoverName()));
				}

				ix += 19;
			}

			if (kit.contents.size() > most) {
				Ui.text(g, "+" + (kit.contents.size() - most), ix + 2, iy + 5, Ui.caption(), a);
			}

			if (taken) {
				Ui.chipRight(g, Txt.t("kit.taken"), f.x() + f.w() - 8, iy + 3, p.textMuted(), f.alpha());
			} else if (kit.readyAt > System.currentTimeMillis()) {
				Ui.chipRight(g, Txt.t("kit.wait", Txt.left(kit.readyAt)), f.x() + f.w() - 8, iy + 3, p.warning(), f.alpha());
			}
		}).flat().narrate(Component.literal(kit.name));
		if (!taken && !waiting) {
			CFHGuiButton take = new CFHGuiButton(0, 0, 76, 18, Txt.t("kit.take"), () -> ServerActions.kitClaim(kit.id, null)).icon(Icon.DOWNLOAD).iconSize(8);
			card.add(take, w - 84, 44);
		}

		return card;
	}
}
