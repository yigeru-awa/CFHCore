package com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Clock;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiTooltips;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiWidget;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeamData;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

public class ActivityFeed extends CFHGuiWidget {
	public static final int ROW = 14;
	private final List<TeamData.LogEntry> entries = new ArrayList<>();
	private boolean seen;
	private boolean cascade;
	private long shownAtMs;

	public ActivityFeed(int x, int y, int width, List<TeamData.LogEntry> log) {
		super(x, y, width, Math.max(14, log.size() * 14), TeamsText.t("overview.activity"));

		for (int i = log.size() - 1; i >= 0; i--) {
			this.entries.add(log.get(i));
		}

		this.active = false;
	}

	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		return false;
	}

	public ComponentPath nextFocusPath(FocusNavigationEvent event) {
		return null;
	}

	private static Icon sign(String type) {
		return switch (type) {
			case "created" -> Icon.SPARKLE;
			case "joined" -> Icon.INVITE;
			case "left" -> Icon.EXIT;
			case "kicked" -> Icon.BLOCKED;
			case "promoted" -> Icon.ARROW_UP;
			case "demoted" -> Icon.ARROW_DOWN;
			case "transferred" -> Icon.CROWN;
			case "ally", "unally" -> Icon.SHIELD;
			case "claimed", "unclaimed" -> Icon.MAP;
			case "expired" -> Icon.CLOCK;
			case "home" -> Icon.HOME;
			case "waypoint" -> Icon.PIN;
			case "quest" -> Icon.QUEST;
			case "chapter" -> Icon.TROPHY;
			case "level" -> Icon.STAR_FILLED;
			case "renamed" -> Icon.EDIT;
			default -> Icon.DOT;
		};
	}

	private static int tone(String type, Palette p) {
		return switch (type) {
			case "joined", "ally", "created" -> p.success();
			case "left", "kicked", "unally", "unclaimed", "expired" -> p.danger();
			case "level", "chapter", "quest", "transferred" -> p.warning();
			default -> p.textMuted();
		};
	}

	private static Component words(TeamData.LogEntry e, int strong) {
		Style name = Style.EMPTY.withColor(strong & 16777215);
		String actor = e.actorName == null ? "" : e.actorName;
		String subject = e.subject == null ? "" : e.subject;
		String key = "cfhgui_teams.log." + e.type;
		if (!I18n.exists(key)) {
			return TeamsText.log(e);
		} else {
			MutableComponent a = Component.literal(actor).withStyle(name);
			MutableComponent s = Component.literal(subject).withStyle(name);
			return Component.translatable(key, new Object[]{a, s});
		}
	}

	protected void renderDark(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		this.draw(g, mouseX, mouseY, false);
	}

	protected void renderVanilla(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		this.draw(g, mouseX, mouseY, true);
	}

	private void draw(GuiGraphics g, int mouseX, int mouseY, boolean van) {
		float a = this.effectiveAlpha();
		if (!this.seen) {
			this.seen = true;
			this.cascade = this.isEntering() && Theme.current().motion() > 0.0F;
		}

		if (!(a <= 0.004F)) {
			if (this.shownAtMs == 0L) {
				this.shownAtMs = Clock.nowMs();
			}

			Palette p = Theme.current().palette();
			int x = this.getX();
			int y = this.getY() + this.enterOffset();
			int w = this.getWidth();
			if (this.entries.isEmpty()) {
				TeamsDraw.text(g, TeamsText.t("overview.activity.empty"), x + 2, y + 3, p.textDim(), a);
			} else {
				CFHGuiDraw.vline(g, x + 6, y + 6, (this.entries.size() - 1) * 14, Colors.scaleAlpha(van ? 1627389951 : p.border(), a));
				boolean over = g.containsPointInScissor(mouseX, mouseY);
				float motion = Math.max(0.01F, Theme.current().motion());

				for (int i = 0; i < this.entries.size(); i++) {
					int ry = y + i * 14;
					float ra = a;
					int slide = 0;
					if (this.cascade) {
						float t = ((float)(Clock.nowMs() - this.shownAtMs) - (float)Math.min(i, 14) * 26.0F * motion) / (200.0F * motion);
						if (t <= 0.0F) {
							continue;
						}

						float e = Ease.OUT_CUBIC.apply(Math.min(1.0F, t));
						ra = a * e;
						slide = Math.round((1.0F - e) * 5.0F);
						if (i == this.entries.size() - 1 && t >= 1.0F) {
							this.cascade = false;
						}
					}

					TeamData.LogEntry e = this.entries.get(i);
					int tone = tone(e.type, p);
					CFHGuiDraw.pixelCircle(g, x + 6, ry + 6, 5, Colors.scaleAlpha(van ? -14671840 : p.bg2(), ra));
					CFHGuiDraw.pixelRing(g, x + 6, ry + 6, 5, Colors.scaleAlpha(Colors.withAlpha(tone, 176), ra));
					Icons.draw(g, sign(e.type), x + 2, ry + 2, 8, Colors.scaleAlpha(tone, ra));
					Component when = TeamsText.agoShort(e.at);
					int whenW = CFHGuiDraw.width(when);
					Component words = words(e, van ? 16777215 : p.text());
					int room = w - 18 - whenW - 8;
					TeamsDraw.textFit(g, words, x + 18 + slide, ry + 3, room, p.textMuted(), ra);
					TeamsDraw.text(g, when, x + w - whenW - 2, ry + 3, p.textDim(), ra);
					if (over && mouseX >= x && mouseX < x + w && mouseY >= ry && mouseY < ry + 14 && ra > 0.9F) {
						CFHGuiTooltips.request(List.of(words(e, 16777215), Component.literal(TeamsText.dateTime(e.at)).withStyle(ChatFormatting.GRAY)), this);
					}
				}
			}
		}
	}

	protected void updateWidgetNarration(NarrationElementOutput out) {
		out.add(NarratedElementType.TITLE, this.getMessage());
	}
}
