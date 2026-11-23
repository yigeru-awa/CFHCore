package com.onehumanawa.cfhcore.content.gui.teams.client.vanilla;

import com.onehumanawa.cfhcore.content.gui.teams.client.ui.dialog.BannerForm;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.dialog.ConfirmForm;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.dialog.CreateTeamForm;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.dialog.FormPart;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.dialog.InviteForm;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.dialog.PickForm;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.dialog.CFHGuiTeamDialogs;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.dialog.TeamDialogs;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.dialog.TypedConfirmForm;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.dialog.WaypointForm;
import com.onehumanawa.cfhcore.content.gui.teams.model.BannerData;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeamData;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeamSummary;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

final class VanillaTeamDialogs implements TeamDialogs {
	private final Screen menu;

	VanillaTeamDialogs(Screen menu) {
		this.menu = menu;
	}

	private void show(FormPart form) {
		Minecraft.getInstance().setScreen(new VanillaFormScreen(this.menu, form));
	}

	private VanillaFormScreen screen(FormPart form) {
		VanillaFormScreen s = new VanillaFormScreen(this.menu, form);
		Minecraft.getInstance().setScreen(s);
		return s;
	}

	@Override
	public void confirm(Component title, Component body, Component okLabel, boolean danger, Runnable onOk) {
		this.show(new ConfirmForm(title, body, okLabel, danger, onOk));
	}

	@Override
	public void confirmTyped(Component title, Component body, String word, Component okLabel, Runnable onOk) {
		this.show(new TypedConfirmForm(title, body, word, okLabel, onOk));
	}

	@Override
	public void foundTeam() {
		this.show(new CreateTeamForm());
	}

	@Override
	public void invite() {
		InviteForm form = new InviteForm();
		form.host(this.screen(form));
	}

	@Override
	public void waypoint(@Nullable TeamData.Waypoint existing) {
		this.show(new WaypointForm(existing));
	}

	@Override
	public void banner(BannerData initial, Consumer<BannerData> onSave) {
		this.show(new BannerForm(initial, onSave));
	}

	@Override
	public void pickTeam(Component title, List<TeamSummary> from, Consumer<TeamSummary> onPick) {
		PickForm<TeamSummary> form = CFHGuiTeamDialogs.teamPicker(title, from, onPick);
		form.host(this.screen(form));
	}

	@Override
	public void pickMember(Component title, List<TeamData.Member> from, Consumer<TeamData.Member> onPick) {
		PickForm<TeamData.Member> form = CFHGuiTeamDialogs.memberPicker(title, from, onPick);
		form.host(this.screen(form));
	}
}
