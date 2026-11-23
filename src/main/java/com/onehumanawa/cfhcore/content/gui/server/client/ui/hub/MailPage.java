package com.onehumanawa.cfhcore.content.gui.server.client.ui.hub;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.core.perm.Perms.Node;
import com.onehumanawa.cfhcore.content.gui.core.screen.SidebarScreen;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiLabel;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiScrollPanel;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton.Variant;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiLabel.Style;
import com.onehumanawa.cfhcore.content.gui.server.Nodes;
import com.onehumanawa.cfhcore.content.gui.server.client.ServerActions;
import com.onehumanawa.cfhcore.content.gui.server.client.ServerState;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.ServerPage;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.ServerScreen;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.dialog.Forms;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.kit.PaintedCard;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.kit.Txt;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.kit.Ui;
import com.onehumanawa.cfhcore.content.gui.server.model.PlayerRecord;
import java.util.List;
import net.minecraft.network.chat.Component;

final class MailPage extends ServerPage {
	private CFHGuiScrollPanel panel;
	private CFHGuiLabel count;
	private CFHGuiButton readAll;
	private CFHGuiButton clear;

	MailPage(ServerScreen screen) {
		super(screen, "mail", Txt.page("mail"), Icon.SEND);
	}

	@Override
	protected String feature() {
		return "mail";
	}

	@Override
	protected Node node() {
		return Nodes.MAIL_USE;
	}

	public int badge() {
		int n = 0;

		for (PlayerRecord.Mail m : ServerState.mail()) {
			if (!m.read) {
				n++;
			}
		}

		return Math.max(n, ServerState.mail().isEmpty() ? ServerState.self().unreadMail : 0);
	}

	@Override
	protected void buildPage(SidebarScreen s, Rect area) {
		ServerPage.Bar bar = new ServerPage.Bar(s, area);
		CFHGuiButton write = new CFHGuiButton(0, 0, 90, Txt.t("mail.write"), () -> new Forms.MailForm(null).show()).variant(Variant.PRIMARY).icon(Icon.EDIT);
		write.setWidth(write.preferredWidth() + 14);
		bar.right(write);
		this.clear = new CFHGuiButton(
				0, 0, 90, Txt.t("mail.clear"), () -> Forms.confirmDelete(Txt.t("mail.clear.title"), Txt.t("mail.clear.body"), () -> ServerActions.mailDelete(null))
			)
			.variant(Variant.GHOST);
		this.clear.setWidth(this.clear.preferredWidth() + 10);
		bar.right(this.clear);
		this.readAll = new CFHGuiButton(0, 0, 90, Txt.t("mail.read_all"), () -> ServerActions.mailRead(null)).variant(Variant.GHOST);
		this.readAll.setWidth(this.readAll.preferredWidth() + 10);
		bar.right(this.readAll);
		this.count = bar.left(new CFHGuiLabel(0, 0, Math.max(40, bar.room() - 8), Component.empty()).style(Style.MUTED));
		Rect r = under(area);
		this.panel = (CFHGuiScrollPanel)s.addPageWidget(new CFHGuiScrollPanel(r.x(), r.y(), r.w(), r.h()));
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

	private void fill() {
		List<PlayerRecord.Mail> mail = ServerState.mail();
		int w = this.panel.innerWidth();
		int unread = 0;
		int read = 0;

		for (PlayerRecord.Mail m : mail) {
			if (m.read) {
				read++;
			} else {
				unread++;
			}
		}

		this.count.text(mail.isEmpty() ? Component.empty() : (unread > 0 ? Txt.t("mail.count.unread", mail.size(), unread) : Txt.t("mail.count", mail.size())));
		this.readAll.active = unread > 0;
		this.clear.active = read > 0;
		if (mail.isEmpty()) {
			int h = Math.max(96, this.panel.getHeight() - 4);
			this.panel
				.add(
					new PaintedCard(w, h, (g, f) -> Ui.empty(g, new Rect(f.x(), f.y(), f.w(), f.h()), Icon.SEND, Txt.t("mail.none.title"), Txt.t("mail.none"), f.alpha()))
						.bare(),
					0,
					0
				);
			this.panel.setContentHeight(h);
		} else {
			int y = 0;

			for (PlayerRecord.Mail mx : mail) {
				PaintedCard card = this.letter(mx, w);
				this.panel.add(card, 0, y);
				y += card.getHeight() + 6;
			}

			this.panel.setContentHeight(y);
		}
	}

	private PaintedCard letter(PlayerRecord.Mail m, int w) {
		Component text = Component.literal(m.text);
		int textW = w - 42 - 10;
		int lines = Ui.wrappedHeight(text, textW, 8);
		int h = 22 + lines + 26;
		PaintedCard card = new PaintedCard(w, h, (g, f) -> {
			Palette p = Theme.current().palette();
			float a = f.alpha();
			if (!m.read) {
				Ui.wash(g, f.x() + 1, f.y() + 1, 140, f.h() - 2, p.accent() & 16777215, a);
				CFHGuiDraw.rect(g, f.x() + 1, f.y() + 4, 2, f.h() - 8, Colors.scaleAlpha(p.accent(), a));
			}

			Ui.head(g, m.from, m.fromName, f.x() + 10, f.y() + 8, 22, null, a);
			int right = f.x() + f.w() - 10;
			Component when = Txt.ago(m.at);
			Ui.textRight(g, when, right, f.y() + 9, Ui.caption(), a);
			right -= CFHGuiDraw.width(when) + 8;
			if (!m.read) {
				right = Ui.chipRight(g, Txt.t("mail.new"), right, f.y() + 7, p.accentInk(), a) - 6;
			}

			Ui.textFit(g, Component.literal(m.fromName), f.x() + 42, f.y() + 9, right - f.x() - 46, Ui.strong(), a);
			Ui.wrapped(g, text, f.x() + 42, f.y() + 22, textW, 8, m.read ? p.textMuted() : p.text(), a);
			f.tip(f.w() - 90, 4, 86, 14, Component.literal(Txt.date(m.at)));
		}).flat().narrate(Component.literal(m.fromName + ": " + m.text));
		CFHGuiButton delete = new CFHGuiButton(
				0,
				0,
				60,
				16,
				Txt.t("form.delete"),
				() -> Forms.confirmDelete(Txt.t("mail.delete.title", m.fromName), Txt.t("mail.delete.body"), () -> ServerActions.mailDelete(m.id))
			)
			.variant(Variant.GHOST);
		delete.setWidth(delete.preferredWidth() + 10);
		card.add(delete, w - 8 - delete.getWidth(), h - 22);
		CFHGuiButton reply = new CFHGuiButton(0, 0, 60, 16, Txt.t("mail.reply"), () -> {
			if (!m.read) {
				ServerActions.mailRead(m.id);
			}

			new Forms.MailForm(m.fromName).show();
		}).icon(Icon.REPLY).iconSize(8);
		reply.setWidth(reply.preferredWidth() + 12);
		card.add(reply, w - 12 - delete.getWidth() - reply.getWidth(), h - 22);
		if (!m.read) {
			CFHGuiButton read = new CFHGuiButton(0, 0, 60, 16, Txt.t("mail.mark_read"), () -> ServerActions.mailRead(m.id)).variant(Variant.GHOST);
			read.setWidth(read.preferredWidth() + 10);
			card.add(read, w - 16 - delete.getWidth() - reply.getWidth() - read.getWidth(), h - 22);
		}

		return card;
	}
}
