package com.onehumanawa.cfhcore.content.gui.quests.client.view;

import com.onehumanawa.cfhcore.content.gui.quests.logic.Rules;
import java.util.Locale;

public enum TreeFilter {
	ALL,
	TODO,
	REWARDS,
	DONE;

	public boolean shows(Rules.Status status) {
		return switch (this) {
			case ALL -> true;
			case TODO -> status.open();
			case REWARDS -> status == Rules.Status.CLAIMABLE;
			case DONE -> status.done();
		};
	}

	public String key() {
		return this.name().toLowerCase(Locale.ROOT);
	}
}
