package com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit;

import com.onehumanawa.cfhcore.content.gui.teams.model.Access;
import com.onehumanawa.cfhcore.content.gui.teams.model.LandRules;
import com.onehumanawa.cfhcore.content.gui.teams.model.Permission;
import com.onehumanawa.cfhcore.content.gui.teams.model.Rank;
import java.util.List;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public final class LandRuleList {
	public static final List<LandRuleList.Rule> ALL = List.of(
		new LandRuleList.Rule("build", LandRuleList.Kind.ACCESS),
		new LandRuleList.Rule("interact", LandRuleList.Kind.ACCESS),
		new LandRuleList.Rule("containers", LandRuleList.Kind.ACCESS),
		new LandRuleList.Rule("entities", LandRuleList.Kind.ACCESS),
		new LandRuleList.Rule("explosions", LandRuleList.Kind.SWITCH),
		new LandRuleList.Rule("mobGriefing", LandRuleList.Kind.SWITCH),
		new LandRuleList.Rule("fireSpread", LandRuleList.Kind.SWITCH),
		new LandRuleList.Rule("pvp", LandRuleList.Kind.SWITCH),
		new LandRuleList.Rule("claimRank", LandRuleList.Kind.RANK)
	);
	public static final List<Access> OWN_ACCESS = List.of(Access.MEMBERS, Access.EVERYONE);
	public static final List<Rank> CLAIM_RANKS = List.of(Rank.MEMBER, Rank.MODERATOR, Rank.ADMIN, Rank.OWNER);

	public static List<LandRuleList.Rule> forLand() {
		return TeamsView.personal() == null ? ALL : ALL.subList(0, ALL.size() - 1);
	}

	public static Component ownAccess(Access access) {
		return (Component)(access == Access.EVERYONE ? TeamsText.access(access) : TeamsText.t("access.own.long"));
	}

	public static Component ownAccessShort(Access access) {
		return (Component)(access == Access.EVERYONE ? TeamsText.accessShort(access) : TeamsText.t("access.own"));
	}

	private LandRuleList() {
	}

	public static enum Kind {
		ACCESS,
		SWITCH,
		RANK;
	}

	public static record Rule(String field, LandRuleList.Kind kind) {
		public Component label() {
			return TeamsText.t("land.rule." + this.field);
		}

		public Component description() {
			return TeamsText.t("land.rule." + this.field + ".desc");
		}

		public boolean pinned() {
			return TeamsView.info().pinnedLandRules.contains(this.field);
		}

		public boolean editable() {
			return TeamsView.mayLand(Permission.SETTINGS) && !this.pinned();
		}

		@Nullable
		public Component whyLocked() {
			if (this.pinned()) {
				return TeamsText.t("land.rule.pinned");
			} else {
				return !TeamsView.mayLand(Permission.SETTINGS) ? TeamsText.t("land.rule.rank", TeamsText.rank(Permission.SETTINGS.min())) : null;
			}
		}

		public Access access(LandRules rules) {
			String var2 = this.field;

			return switch (var2) {
				case "build" -> rules.build;
				case "interact" -> rules.interact;
				case "containers" -> rules.containers;
				default -> rules.entities;
			};
		}

		public boolean on(LandRules rules) {
			String var2 = this.field;

			return switch (var2) {
				case "explosions" -> rules.explosions;
				case "mobGriefing" -> rules.mobGriefing;
				case "fireSpread" -> rules.fireSpread;
				default -> rules.pvp;
			};
		}

		public Component valueText(LandRules rules) {
			return (Component)(switch (this.kind) {
				case ACCESS -> TeamsView.personal() != null ? LandRuleList.ownAccess(this.access(rules)) : TeamsText.access(this.access(rules));
				case SWITCH -> Component.translatable(this.on(rules) ? "options.on" : "options.off");
				case RANK -> TeamsText.rank(rules.claimRank);
			});
		}
	}
}
