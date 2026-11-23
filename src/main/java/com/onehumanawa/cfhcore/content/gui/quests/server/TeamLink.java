package com.onehumanawa.cfhcore.content.gui.quests.server;

import java.util.UUID;

public interface TeamLink {
	TeamLink NONE = new TeamLink() {
		@Override
		public UUID holderOf(UUID player) {
			return player;
		}

		@Override
		public boolean isTeam(UUID holder) {
			return false;
		}

		@Override
		public String name(UUID holder) {
			return "";
		}

		@Override
		public long figure(UUID holder, String what) {
			return !"members".equals(what) && !"level".equals(what) ? 0L : 1L;
		}

		@Override
		public void addXp(UUID team, long amount) {
		}

		@Override
		public void count(UUID team, String counter, long delta) {
		}

		@Override
		public void log(UUID team, String type, UUID actor, String actorName, String subject) {
		}
	};

	UUID holderOf(UUID var1);

	boolean isTeam(UUID var1);

	String name(UUID var1);

	long figure(UUID var1, String var2);

	void addXp(UUID var1, long var2);

	void count(UUID var1, String var2, long var3);

	void log(UUID var1, String var2, UUID var3, String var4, String var5);
}
