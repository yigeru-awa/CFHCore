package com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit;

import com.onehumanawa.cfhcore.content.gui.teams.model.Access;
import com.onehumanawa.cfhcore.content.gui.teams.model.Privacy;
import com.onehumanawa.cfhcore.content.gui.teams.model.Rank;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeamData;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeammateState;
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class TeamsText {
	private static final long MINUTE = 60000L;
	private static final long HOUR = 3600000L;
	private static final long DAY = 86400000L;

	public static MutableComponent t(String key, Object... args) {
		return Component.translatable("cfhgui_teams.ui." + key, args);
	}

	public static MutableComponent shared(String key, Object... args) {
		return Component.translatable("cfhgui_teams." + key, args);
	}

	public static Component page(String pageId) {
		return shared("page." + pageId);
	}

	public static Component rank(@Nullable Rank rank) {
		return shared("rank." + (rank == null ? Rank.MEMBER : rank).key());
	}

	public static Component privacy(Privacy privacy) {
		return shared("privacy." + privacy.key());
	}

	public static Component privacyHint(Privacy privacy) {
		return shared("privacy." + privacy.key() + ".desc");
	}

	public static Component access(Access access) {
		return shared("access." + access.key());
	}

	public static Component accessShort(Access access) {
		return t("access." + access.key());
	}

	public static String tag(@Nullable String tag) {
		return tag != null && !tag.isBlank() ? "[" + tag + "]" : "";
	}

	public static Component ago(long at) {
		if (at <= 0L) {
			return t("time.unknown");
		} else {
			long d = System.currentTimeMillis() - at;
			if (d < 60000L) {
				return t("time.now");
			} else if (d < 120000L) {
				return t("time.minute");
			} else if (d < 3600000L) {
				return t("time.minutes", d / 60000L);
			} else if (d < 7200000L) {
				return t("time.hour");
			} else if (d < 86400000L) {
				return t("time.hours", d / 3600000L);
			} else if (d < 172800000L) {
				return t("time.day");
			} else {
				return d < 2678400000L ? t("time.days", d / 86400000L) : Component.literal(date(at));
			}
		}
	}

	public static Component agoShort(long at) {
		if (at <= 0L) {
			return Component.empty();
		} else {
			long d = System.currentTimeMillis() - at;
			if (d < 60000L) {
				return t("time.short.now");
			} else if (d < 3600000L) {
				return t("time.short.minutes", d / 60000L);
			} else if (d < 86400000L) {
				return t("time.short.hours", d / 3600000L);
			} else {
				return d < 2678400000L ? t("time.short.days", d / 86400000L) : Component.literal(date(at));
			}
		}
	}

	public static Component until(long at) {
		long d = at - System.currentTimeMillis();
		if (d < 60000L) {
			return t("time.short.now");
		} else if (d < 3600000L) {
			return t("time.short.minutes", (d + 60000L - 1L) / 60000L);
		} else {
			return d < 86400000L ? t("time.short.hours", d / 3600000L) : t("time.short.days", d / 86400000L);
		}
	}

	public static String dateTime(long at) {
		return DateFormat.getDateTimeInstance(2, 3, locale()).format(new Date(at));
	}

	public static String date(long at) {
		return DateFormat.getDateInstance(2, locale()).format(new Date(at));
	}

	private static Locale locale() {
		try {
			String code = Minecraft.getInstance().getLanguageManager().getSelected();
			String[] parts = code.split("_");
			return parts.length >= 2 ? Locale.of(parts[0], parts[1].toUpperCase(Locale.ROOT)) : Locale.of(parts[0]);
		} catch (Exception var2) {
			return Locale.getDefault();
		}
	}

	public static Component dimension(@Nullable String id) {
		ResourceLocation rl = id == null ? null : ResourceLocation.tryParse(id);
		if (rl == null) {
			return t("dimension.unknown");
		} else {
			if (rl.getNamespace().equals("minecraft")) {
				String key = rl.getPath();
				switch (key) {
					case "overworld":
						return t("dimension.overworld");
					case "the_nether":
						return t("dimension.the_nether");
					case "the_end":
						return t("dimension.the_end");
				}
			}

			String key = "dimension." + rl.getNamespace() + "." + rl.getPath().replace('/', '.');
			return I18n.exists(key) ? Component.translatable(key) : Component.literal(readable(rl.getPath()));
		}
	}

	public static String readable(String path) {
		String s = path.replace('_', ' ').replace('/', ' ').trim();
		return s.isEmpty() ? path : Character.toUpperCase(s.charAt(0)) + s.substring(1);
	}

	public static Component distance(double blocks) {
		return blocks < 1000.0 ? t("distance.m", Math.round(blocks)) : t("distance.km", String.format(Locale.ROOT, "%.1f", blocks / 1000.0));
	}

	public static String coords(int x, int y, int z) {
		return x + ", " + y + ", " + z;
	}

	public static String number(long n) {
		return String.format(locale(), "%,d", n);
	}

	public static Component chunks(int n) {
		return t(n == 1 ? "count.chunk" : "count.chunks", n);
	}

	public static Component where(TeamData.Member m) {
		if (m.id.equals(TeamsView.self())) {
			return (Component)(Minecraft.getInstance().level == null ? t("members.joined", ago(m.joinedAt)) : dimension(TeamsView.dimension()));
		} else {
			TeammateState s = TeamsView.where(m.id);
			if (s == null) {
				return m.lastSeen <= 0L ? t("members.offline") : t("members.seen", ago(m.lastSeen));
			} else {
				Component dimension = dimension(s.dimension());
				Vec3 me = TeamsView.position();
				if (me != null && s.dimension().equals(TeamsView.dimension())) {
					double dx = (double)s.x() - me.x;
					double dy = (double)s.y() - me.y;
					double dz = (double)s.z() - me.z;
					return t("members.where", dimension, distance(Math.sqrt(dx * dx + dy * dy + dz * dz)));
				} else {
					return dimension;
				}
			}
		}
	}

	public static Component log(TeamData.LogEntry e) {
		String actor = e.actorName == null ? "" : e.actorName;
		String subject = e.subject == null ? "" : e.subject;
		String key = "cfhgui_teams.log." + e.type;
		return !I18n.exists(key)
			? Component.literal(actor + ": " + e.type + (subject.isEmpty() ? "" : " " + subject))
			: Component.translatable(key, new Object[]{actor, subject});
	}

	private TeamsText() {
	}
}
