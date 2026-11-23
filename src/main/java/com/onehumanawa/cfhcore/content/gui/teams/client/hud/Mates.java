package com.onehumanawa.cfhcore.content.gui.teams.client.hud;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Clock;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiAvatar;
import com.onehumanawa.cfhcore.content.gui.teams.client.TeamsState;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeamData;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeamSummary;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeammateState;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

final class Mates {
	private static final int TELL_MS = 500;
	private static final int MIN_GLIDE_MS = 150;
	private static final int MAX_GLIDE_MS = 1200;
	private static final int TRAIL_HOLD_MS = 420;
	private static final Map<UUID, Mates.Mate> MATES = new LinkedHashMap<>();
	private static final List<Mates.Mate> PARTY = new ArrayList<>();
	private static final List<Mates.Mate> ALL = new ArrayList<>();

	static void tick() {
		long now = Clock.nowMs();
		Collection<TeammateState> told = HudDemo.mates() != null ? HudDemo.mates() : TeamsState.teammates();

		for (Mates.Mate m : MATES.values()) {
			m.online = false;
		}

		for (TeammateState s : told) {
			Mates.Mate m = MATES.get(s.id());
			if (m == null) {
				m = new Mates.Mate(s, now);
				MATES.put(s.id(), m);
			} else if (m.state != s) {
				m.tell(s, now);
			}

			m.online = true;
			m.resolveName(now);
		}

		Iterator<Mates.Mate> it = MATES.values().iterator();

		while (it.hasNext()) {
			Mates.Mate m = it.next();
			if (!m.online && m.row.get() <= 0.01F && m.marker.get() <= 0.01F) {
				it.remove();
			}
		}

		ALL.clear();
		ALL.addAll(MATES.values());
		PARTY.clear();

		for (Mates.Mate m : ALL) {
			if (!m.ally()) {
				PARTY.add(m);
			}
		}

		TeamData team = TeamsState.team();
		if (team != null && PARTY.size() > 1) {
			Map<UUID, Integer> order = new HashMap<>();

			for (int i = 0; i < team.members.size(); i++) {
				order.put(team.members.get(i).id, i);
			}

			PARTY.sort((a, b) -> Integer.compare(order.getOrDefault(a.id, Integer.MAX_VALUE), order.getOrDefault(b.id, Integer.MAX_VALUE)));
		}
	}

	static void frame(float partialTick) {
		if (!ALL.isEmpty()) {
			long now = Clock.nowMs();
			float frameMs = Clock.frameDelta();
			ClientLevel level = Minecraft.getInstance().level;
			String dimension = TeamsState.currentDimension();

			for (Mates.Mate m : ALL) {
				m.place(level, m.state.dimension().equals(dimension), partialTick, now);
				m.ease(now, frameMs);
			}
		}
	}

	static List<Mates.Mate> party() {
		return PARTY;
	}

	static List<Mates.Mate> all() {
		return ALL;
	}

	static void clear() {
		MATES.clear();
		ALL.clear();
		PARTY.clear();
	}

	private Mates() {
	}

	static final class Mate {
		final UUID id;
		TeammateState state;
		String name = "";
		boolean online = true;
		double x;
		double y;
		double z;
		float height = 1.8F;
		private double fromX;
		private double fromY;
		private double fromZ;
		private long glideStart;
		private long toldAt;
		private int glideMs = 500;
		final Anim health = new Anim(1.0F, 380, Ease.OUT_CUBIC);
		final Anim armor = new Anim(0.0F, 380, Ease.OUT_CUBIC);
		float trail = 1.0F;
		private long trailHoldUntil;
		final Anim hurt = new Anim(0.0F, 520, Ease.OUT_CUBIC);
		final Anim row = new Anim(0.0F, 220, Ease.OUT_CUBIC);
		final Anim marker = new Anim(0.0F, 200, Ease.OUT_CUBIC);
		final Anim detail = new Anim(0.0F, 160, Ease.OUT_CUBIC);
		final Anim labelWidth = new Anim(0.0F, 160, Ease.OUT_CUBIC);
		final Anim labelLift = new Anim(0.0F, 160, Ease.OUT_CUBIC);
		boolean labelPlaced;
		final HudDraw.Distance distance = new HudDraw.Distance();
		String elsewhereOf = "";
		Component elsewhere = Component.empty();
		int elsewhereW;
		@Nullable
		Component where;
		int whereW;
		boolean whereArrow;
		private String frameNameOf = "";
		private Component frameNameWhole = Component.empty();
		private FormattedCharSequence frameName = FormattedCharSequence.EMPTY;
		private int frameNameW;
		private int frameNameRoom = -1;
		private String labelNameOf = "";
		FormattedCharSequence labelName = FormattedCharSequence.EMPTY;
		int labelNameW;
		@Nullable
		private ResourceLocation skin;
		private long skinAt;
		private long nameAt;

		private void fitFrameName() {
			if (!this.frameNameOf.equals(this.name)) {
				this.frameNameOf = this.name;
				this.frameNameWhole = Component.literal(this.name);
				this.frameNameW = CFHGuiDraw.width(this.frameNameWhole);
				this.frameNameRoom = -1;
			}
		}

		int frameNameWidth() {
			this.fitFrameName();
			return this.frameNameW;
		}

		FormattedCharSequence frameName(int room) {
			this.fitFrameName();
			if (room != this.frameNameRoom) {
				this.frameNameRoom = room;
				this.frameName = this.frameNameW <= room ? this.frameNameWhole.getVisualOrderText() : CFHGuiDraw.truncate(this.frameNameWhole, Math.max(0, room));
			}

			return this.frameName;
		}

		void fitLabelName(int max) {
			if (!this.labelNameOf.equals(this.name)) {
				this.labelNameOf = this.name;
				Component c = Component.literal(this.name);
				this.labelName = CFHGuiDraw.truncate(c, max);
				this.labelNameW = Math.min(max, CFHGuiDraw.width(c));
			}
		}

		Mate(TeammateState s, long now) {
			this.id = s.id();
			this.state = s;
			this.x = this.fromX = (double)s.x();
			this.y = this.fromY = (double)s.y();
			this.z = this.fromZ = (double)s.z();
			this.toldAt = this.glideStart = now;
			float h = fraction(s);
			this.health.snap(h);
			this.trail = h;
			this.armor.snap(Mth.clamp((float)s.armor() / 20.0F, 0.0F, 1.0F));
		}

		private static float fraction(TeammateState s) {
			if (s.dead()) {
				return 0.0F;
			} else {
				return s.maxHealth() <= 0.0F ? 0.0F : Mth.clamp(s.health() / s.maxHealth(), 0.0F, 1.0F);
			}
		}

		private void tell(TeammateState s, long now) {
			boolean moved = !s.dimension().equals(this.state.dimension());
			this.fromX = moved ? (double)s.x() : this.x;
			this.fromY = moved ? (double)s.y() : this.y;
			this.fromZ = moved ? (double)s.z() : this.z;
			this.glideMs = Mth.clamp((int)(now - this.toldAt), 150, 1200);
			this.glideStart = this.toldAt = now;
			float before = this.health.target();
			float after = fraction(s);
			if (after < before - 0.004F) {
				this.trail = Math.max(this.trail, this.health.get());
				this.trailHoldUntil = now + 420L;
				this.hurt.snap(1.0F);
				this.hurt.set(0.0F);
			}

			this.health.set(after);
			this.armor.set(Mth.clamp((float)s.armor() / 20.0F, 0.0F, 1.0F));
			this.state = s;
		}

		private void place(@Nullable ClientLevel level, boolean sameDimension, float partialTick, long now) {
			Player entity = level != null && sameDimension ? level.getPlayerByUUID(this.id) : null;
			if (entity != null && !entity.isRemoved()) {
				Vec3 p = entity.getPosition(partialTick);
				this.x = p.x;
				this.y = p.y;
				this.z = p.z;
				this.height = entity.getBbHeight();
			} else {
				float t = Mth.clamp((float)(now - this.glideStart) / (float)this.glideMs, 0.0F, 1.0F);
				this.x = Mth.lerp((double)t, this.fromX, (double)this.state.x());
				this.y = Mth.lerp((double)t, this.fromY, (double)this.state.y());
				this.z = Mth.lerp((double)t, this.fromZ, (double)this.state.z());
				this.height = this.state.sneaking() ? 1.5F : 1.8F;
			}
		}

		private void ease(long now, float frameMs) {
			float shown = this.health.get();
			if (shown >= this.trail || HudDraw.theme().motion() <= 0.0F) {
				this.trail = shown;
			} else if (now >= this.trailHoldUntil) {
				this.trail = Math.max(shown, this.trail - frameMs / 700.0F);
			}
		}

		ResourceLocation skin() {
			long now = Clock.nowMs();
			if (this.skin == null || now - this.skinAt > 3000L) {
				this.skinAt = now;
				ClientPacketListener connection = Minecraft.getInstance().getConnection();
				PlayerInfo info = connection == null ? null : connection.getPlayerInfo(this.id);
				this.skin = info != null ? info.getSkin().texture() : CFHGuiAvatar.skinFor(this.id, this.name);
			}

			return this.skin;
		}

		private void resolveName(long now) {
			if (this.name.isEmpty() || now - this.nameAt >= 2000L) {
				this.nameAt = now;
				TeamData team = TeamsState.team();
				TeamData.Member member = team == null ? null : team.member(this.id);
				if (member != null && !member.name.isEmpty()) {
					this.name = member.name;
				} else {
					ClientPacketListener connection = Minecraft.getInstance().getConnection();
					PlayerInfo info = connection == null ? null : connection.getPlayerInfo(this.id);
					if (info != null && info.getProfile().getName() != null) {
						this.name = info.getProfile().getName();
					} else {
						for (TeamSummary s : TeamsState.directory()) {
							if (this.id.equals(s.owner) && !s.ownerName.isEmpty()) {
								this.name = s.ownerName;
								return;
							}
						}

						if (this.name.isEmpty() && this.state.ally()) {
							this.name = I18n.get("cfhgui_teams.hud.ally", new Object[0]);
						}
					}
				}
			}
		}

		boolean ally() {
			return this.state.ally();
		}

		boolean dead() {
			return this.state.dead();
		}

		boolean here() {
			return this.state.dimension().equals(TeamsState.currentDimension());
		}

		double distanceTo(Vec3 from) {
			double dx = this.x - from.x;
			double dy = this.y - from.y;
			double dz = this.z - from.z;
			return Math.sqrt(dx * dx + dy * dy + dz * dz);
		}
	}
}
