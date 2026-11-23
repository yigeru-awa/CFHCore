package com.onehumanawa.cfhcore.content.gui.teams.client.ui.dialog;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiModal;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamsDraw;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamsText;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamsView;
import com.onehumanawa.cfhcore.content.gui.teams.model.BannerData;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeamData;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeamSummary;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public final class CFHGuiTeamDialogs implements TeamDialogs {
	@Override
	public void confirm(Component title, Component body, Component okLabel, boolean danger, Runnable onOk) {
		if (danger) {
			CFHGuiModal.confirmDanger(title, body, okLabel, onOk);
		} else {
			CFHGuiModal.confirm(title, body, okLabel, onOk);
		}
	}

	@Override
	public void confirmTyped(Component title, Component body, String word, Component okLabel, Runnable onOk) {
		FormDialog.open(new TypedConfirmForm(title, body, word, okLabel, onOk));
	}

	@Override
	public void foundTeam() {
		FormDialog.open(new CreateTeamForm());
	}

	@Override
	public void invite() {
		InviteForm form = new InviteForm();
		form.host(FormDialog.open(form));
	}

	@Override
	public void waypoint(@Nullable TeamData.Waypoint existing) {
		FormDialog.open(new WaypointForm(existing));
	}

	@Override
	public void banner(BannerData initial, Consumer<BannerData> onSave) {
		FormDialog.open(new BannerForm(initial, onSave));
	}

	@Override
	public void pickTeam(Component title, List<TeamSummary> from, Consumer<TeamSummary> onPick) {
		PickForm<TeamSummary> form = teamPicker(title, from, onPick);
		form.host(FormDialog.open(form));
	}

	@Override
	public void pickMember(Component title, List<TeamData.Member> from, Consumer<TeamData.Member> onPick) {
		PickForm<TeamData.Member> form = memberPicker(title, from, onPick);
		form.host(FormDialog.open(form));
	}

	public static PickForm<TeamSummary> teamPicker(Component title, List<TeamSummary> from, Consumer<TeamSummary> onPick) {
		return new PickForm<>(
			title, Icon.SHIELD, TeamsText.t("pick.choose"), from, s -> s.name + " " + s.tag, (g, s, index, x, y, w, h, hovered, selected, mouseX, mouseY) -> {
				Palette p = Theme.current().palette();
				TeamsDraw.bannerSmall(g, s.banner, x + 5, y + 2, 9, 1.0F);
				Component members = TeamsText.t("pick.members", s.members);
				int right = x + w - 5 - CFHGuiDraw.width(members);
				TeamsDraw.textFit(g, Component.literal(s.name), x + 20, y + (h - 8) / 2, right - x - 26, TeamsDraw.ink(s.color), 1.0F);
				TeamsDraw.text(g, members, right, y + (h - 8) / 2, TeamsDraw.caption(), 1.0F);
			}, 22, onPick
		);
	}

	public static PickForm<TeamData.Member> memberPicker(Component title, List<TeamData.Member> from, Consumer<TeamData.Member> onPick) {
		return new PickForm<>(title, Icon.CROWN, TeamsText.t("pick.choose"), from, m -> m.name, (g, m, index, x, y, w, h, hovered, selected, mouseX, mouseY) -> {
			Palette p = Theme.current().palette();
			TeamsDraw.head(g, m.id, m.name, x + 5, y + (h - 12) / 2, 12, TeamsView.online(m.id), 1.0F);
			Component rank = TeamsText.rank(m.rank);
			int right = x + w - 5 - CFHGuiDraw.width(rank);
			TeamsDraw.textFit(g, Component.literal(m.name), x + 22, y + (h - 8) / 2, right - x - 28, !hovered && !selected ? p.textMuted() : p.text(), 1.0F);
			TeamsDraw.text(g, rank, right, y + (h - 8) / 2, TeamsDraw.caption(), 1.0F);
		}, 18, onPick);
	}
}
