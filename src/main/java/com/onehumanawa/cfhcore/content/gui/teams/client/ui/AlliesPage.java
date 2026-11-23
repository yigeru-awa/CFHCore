package com.onehumanawa.cfhcore.content.gui.teams.client.ui;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Fonts;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.core.screen.SidebarScreen;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiLabel;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiScrollPanel;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton.Variant;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiLabel.Style;
import com.onehumanawa.cfhcore.content.gui.teams.client.TeamActions;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.PaintedCard;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamsDraw;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamsText;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamsView;
import com.onehumanawa.cfhcore.content.gui.teams.model.Permission;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeamData;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeamSummary;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

final class AlliesPage extends TeamsPage {
	private CFHGuiScrollPanel panel;
	private CFHGuiLabel count;

	AlliesPage(TeamsHubScreen screen) {
		super(screen, "allies", Icon.SHIELD);
	}

	@Override
	protected void buildPage(SidebarScreen s, Rect area) {
		this.panel = null;
		TeamData t = TeamsView.team();
		if (t == null) {
			this.noTeam(s, area, Icon.SHIELD, TeamsText.t("allies.no_team"));
		} else {
			boolean may = TeamsView.may(Permission.ALLIES);
			CFHGuiButton declare = new CFHGuiButton(0, 0, 96, TeamsText.t("allies.declare"), () -> this.flows().allyPick()).variant(Variant.PRIMARY).icon(Icon.PLUS);
			declare.setWidth(Math.max(84, declare.preferredWidth() + 12));
			declare.setX(area.right() - declare.getWidth());
			declare.setY(area.y());
			boolean room = t.allies.size() < TeamsView.info().maxAllies;
			declare.tip(
					!may
						? TeamsText.t("allies.rank", TeamsText.rank(Permission.ALLIES.min()))
						: (room ? TeamsText.t("allies.declare.desc") : TeamsText.shared("result.ally_limit"))
				)
				.enabled(may && room);
			this.count = (CFHGuiLabel)s.addPageWidget(
				new CFHGuiLabel(area.x() + 2, area.y() + 6, area.w() - declare.getWidth() - 10, Component.empty()).style(Style.MUTED)
			);
			s.addPageWidget(declare);
			this.panel = (CFHGuiScrollPanel)s.addPageWidget(new CFHGuiScrollPanel(area.x(), area.y() + 26, area.w(), area.h() - 26));
			this.fill();
		}
	}

	@Override
	void stateChanged() {
		if (this.panel != null && TeamsView.team() != null) {
			refill(this.panel, this::fill);
		} else {
			super.stateChanged();
		}
	}

	private void fill() {
		TeamData t = TeamsView.team();
		if (t != null) {
			int w = this.panel.innerWidth();
			boolean may = TeamsView.may(Permission.ALLIES);
			this.count.text(TeamsText.t("allies.count", t.allies.size(), TeamsView.info().maxAllies));
			List<TeamSummary> theirs = new ArrayList<>();

			for (TeamSummary s : TeamsView.directory()) {
				if (TeamsView.relation(s) == TeamsView.Relation.THEIRS) {
					theirs.add(s);
				}
			}

			int y = 0;
			if (t.allies.isEmpty() && theirs.isEmpty()) {
				int h = Math.max(96, this.panel.getHeight() - 4);
				PaintedCard none = new PaintedCard(
						w,
						h,
						(g, f) -> TeamsDraw.empty(g, new Rect(f.x(), f.y(), f.w(), f.h()), Icon.SHIELD, TeamsText.t("allies.empty"), TeamsText.t("allies.empty.desc"), f.alpha())
					)
					.bare();
				this.panel.add(none, 0, 0);
				this.panel.setContentHeight(h);
			} else {
				if (!t.allies.isEmpty()) {
					y = section(this.panel, y, w, TeamsText.t("allies.ours", t.allies.size()));

					for (UUID id : t.allies) {
						this.panel.add(this.ally(id, TeamsView.summary(id), w, may), 0, y);
						y += 36;
					}

					y += 2;
				}

				if (!theirs.isEmpty()) {
					y = section(this.panel, y, w, TeamsText.t("allies.theirs", theirs.size()));

					for (TeamSummary sx : theirs) {
						this.panel.add(this.declared(sx, w, may), 0, y);
						y += 36;
					}

					y += 2;
				}

				Component note = TeamsText.t("allies.note");
				CFHGuiLabel label = new CFHGuiLabel(0, 0, w - 4, note).style(Style.CAPTION).wrap(true);
				this.panel.add(label, 2, y + 2);
				this.panel.setContentHeight(y + 2 + label.getHeight() + 4);
			}
		}
	}

	private static void paintTeam(
		GuiGraphics g, PaintedCard.Frame f, @Nullable TeamSummary s, Component line, int lineColor, Icon sign, int signColor, int rightRoom
	) {
		Theme th = Theme.current();
		Palette p = th.palette();
		float a = f.alpha();
		if (s == null) {
			Icons.draw(g, Icon.QUESTION, f.x() + 8, f.y() + 10, 12, Colors.scaleAlpha(p.textDim(), a));
			TeamsDraw.text(g, TeamsText.t("map.unknown_team"), f.x() + 28, f.y() + 6, p.textMuted(), a);
			TeamsDraw.textFit(g, line, f.x() + 28, f.y() + 18, f.w() - 28 - rightRoom, lineColor, a);
		} else {
			int ink = TeamsDraw.ink(s.color);
			TeamsDraw.wash(g, f.x() + 1, f.y() + 1, Math.min(f.w() - 2, 120), f.h() - 2, s.color, a);
			TeamsDraw.bannerSmall(g, s.banner, f.x() + 8, f.y() + 4, 12, a);
			int tx = f.x() + 28;
			int room = f.w() - 28 - rightRoom;
			Component name = Fonts.heading(s.name);
			int used = CFHGuiDraw.width(CFHGuiDraw.truncate(name, room));
			g.drawString(CFHGuiDraw.font(), CFHGuiDraw.truncate(name, room), tx, f.y() + 6, Colors.scaleAlpha(ink, a), th.isVanilla());
			if (!s.tag.isEmpty() && used + CFHGuiDraw.width(s.tag) + 16 < room) {
				TeamsDraw.chip(g, Component.literal(s.tag), tx + used + 5, f.y() + 4, ink, a);
			}

			Icons.draw(g, sign, tx, f.y() + 19, 8, Colors.scaleAlpha(signColor, a));
			TeamsDraw.textFit(g, line, tx + 11, f.y() + 19, room - 11, lineColor, a);
			f.tip(
				0,
				0,
				f.w() - rightRoom,
				f.h(),
				List.of(
					Component.literal(s.name).withStyle(st -> st.withColor(ink & 16777215)),
					TeamsText.t("allies.tip", s.members, s.online, s.level).withStyle(ChatFormatting.GRAY),
					s.motto.isEmpty()
						? TeamsText.t("allies.tip.owner", s.ownerName).withStyle(ChatFormatting.GRAY)
						: Component.literal(s.motto).withStyle(ChatFormatting.GRAY)
				)
			);
		}
	}

	private PaintedCard ally(UUID id, @Nullable TeamSummary s, int w, boolean may) {
		boolean mutual = s != null && TeamsView.relation(s) == TeamsView.Relation.MUTUAL;
		int rightRoom = may ? 62 : 8;
		PaintedCard card = new PaintedCard(
				w,
				32,
				(g, f) -> {
					Palette p = Theme.current().palette();
					paintTeam(
						g,
						f,
						s,
						TeamsText.t(mutual ? "allies.mutual" : "allies.one_way"),
						mutual ? p.textMuted() : p.textDim(),
						mutual ? Icon.CHECK : Icon.CLOCK,
						mutual ? p.success() : p.textDim(),
						rightRoom
					);
				}
			)
			.flat();
		if (may) {
			CFHGuiButton end = new CFHGuiButton(0, 0, 52, 18, TeamsText.t("allies.end"), () -> {
				if (s != null) {
					this.flows().unally(s);
				} else {
					TeamActions.unally(id);
				}
			}).variant(Variant.DANGER);
			card.add(end, w - 57, 7);
		}

		return card;
	}

	private PaintedCard declared(TeamSummary s, int w, boolean may) {
		int rightRoom = may ? 84 : 8;
		PaintedCard card = new PaintedCard(w, 32, (g, f) -> {
			Palette p = Theme.current().palette();
			paintTeam(g, f, s, TeamsText.t("allies.waiting"), p.textDim(), Icon.ARROW_LEFT, p.warning(), rightRoom);
		}).flat();
		if (may) {
			card.add(
				new CFHGuiButton(0, 0, 74, 18, TeamsText.t("allies.back"), () -> this.flows().ally(s)).variant(Variant.PRIMARY).icon(Icon.CHECK).iconSize(8), w - 79, 7
			);
		}

		return card;
	}
}
