package com.onehumanawa.cfhcore.content.gui.server.client;

import com.onehumanawa.cfhcore.content.gui.core.client.DevHarness;
import com.onehumanawa.cfhcore.content.gui.core.event.Event;
import com.onehumanawa.cfhcore.content.gui.core.net.JsonWire;
import com.onehumanawa.cfhcore.content.gui.core.perm.Perms.Node;
import com.onehumanawa.cfhcore.content.gui.server.CFHGuiServer;
import com.onehumanawa.cfhcore.content.gui.server.model.AdminData;
import com.onehumanawa.cfhcore.content.gui.server.model.Hello;
import com.onehumanawa.cfhcore.content.gui.server.model.OnlinePlayer;
import com.onehumanawa.cfhcore.content.gui.server.model.PlayerRecord;
import com.onehumanawa.cfhcore.content.gui.server.model.SelfState;
import com.onehumanawa.cfhcore.content.gui.server.model.TeleportRequest;
import com.onehumanawa.cfhcore.content.gui.server.model.WarpList;
import com.onehumanawa.cfhcore.content.gui.server.net.ServerNet;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;

public final class ServerState {
	public static final Event<Runnable> CHANGED = new Event("SERVER_STATE_CHANGED");
	public static final Event<Runnable> HELLO_CHANGED = new Event("SERVER_HELLO_CHANGED");
	public static final Event<Runnable> SELF_CHANGED = new Event("SERVER_SELF_CHANGED");
	public static final Event<Runnable> WARPS_CHANGED = new Event("SERVER_WARPS_CHANGED");
	public static final Event<Runnable> REQUESTS_CHANGED = new Event("SERVER_REQUESTS_CHANGED");
	public static final Event<Runnable> ONLINE_CHANGED = new Event("SERVER_ONLINE_CHANGED");
	public static final Event<Runnable> MAIL_CHANGED = new Event("SERVER_MAIL_CHANGED");
	public static final Event<Runnable> DASHBOARD_CHANGED = new Event("SERVER_DASHBOARD_CHANGED");
	public static final Event<Consumer<ServerState.NoticeView>> NOTICE = new Event("SERVER_NOTICE");
	@Nullable
	private static Hello hello;
	private static SelfState self = new SelfState();
	private static WarpList warps = new WarpList();
	private static List<TeleportRequest> requests = List.of();
	private static List<OnlinePlayer> online = List.of();
	private static List<PlayerRecord.Mail> mail = List.of();
	@Nullable
	private static AdminData.Dashboard dashboard;
	private static boolean watching;
	private static boolean preview;
	private static ServerState.Pretend pretend = ServerState.Pretend.NOTHING;
	private static final boolean FORCE_PREVIEW = Boolean.getBoolean("cfh.server.forcePreview");

	public static boolean connected() {
		return hello != null && !preview;
	}

	public static boolean preview() {
		return preview;
	}

	public static boolean available() {
		return hello != null && pretend != ServerState.Pretend.NO_SERVER;
	}

	public static boolean newerServer() {
		return hello != null && (hello.protocol > 1 || pretend == ServerState.Pretend.NEWER);
	}

	public static Hello hello() {
		return hello != null ? hello : new Hello();
	}

	public static UUID selfId() {
		Minecraft mc = Minecraft.getInstance();
		return mc.player != null ? mc.player.getUUID() : mc.getUser().getProfileId();
	}

	public static boolean feature(String name) {
		return hello != null && hello.feature(name);
	}

	public static boolean may(Node node) {
		return hello != null && hello.may(node.id());
	}

	public static boolean may(String node) {
		return hello != null && hello.may(node);
	}

	public static int number(Node node) {
		return hello == null ? node.defaultNumber() : hello.number(node.id(), node.defaultNumber());
	}

	public static SelfState self() {
		return self;
	}

	public static WarpList warps() {
		return warps;
	}

	public static List<TeleportRequest> requests() {
		return requests;
	}

	public static List<OnlinePlayer> online() {
		return online;
	}

	public static List<PlayerRecord.Mail> mail() {
		return mail;
	}

	@Nullable
	public static AdminData.Dashboard dashboard() {
		return dashboard;
	}

	static void watching(boolean on) {
		watching = on;
		if (!on && dashboard != null) {
			dashboard = null;
			DASHBOARD_CHANGED.invoke(Runnable::run);
		}
	}

	public static void onState(ServerNet.State p) {
		if (!FORCE_PREVIEW) {
			if ("hello".equals(p.kind()) || connected()) {
				String var1 = p.kind();
				switch (var1) {
					case "hello":
						Hello h = (Hello)JsonWire.decode(p.data(), Hello.class);
						if (h == null) {
							return;
						}

						if (preview) {
							clear();
						}

						hello = h.sanitize();
						HELLO_CHANGED.invoke(Runnable::run);
						break;
					case "self":
						SelfState s = (SelfState)JsonWire.decode(p.data(), SelfState.class);
						if (s == null) {
							return;
						}

						self = s.sanitize();
						SELF_CHANGED.invoke(Runnable::run);
						break;
					case "warps":
						WarpList w = (WarpList)JsonWire.decode(p.data(), WarpList.class);
						if (w == null) {
							return;
						}

						warps = w.sanitize();
						WARPS_CHANGED.invoke(Runnable::run);
						break;
					case "requests":
						TeleportRequest[] r = (TeleportRequest[])JsonWire.decode(p.data(), TeleportRequest[].class);
						if (r == null) {
							return;
						}

						List<TeleportRequest> list = new ArrayList<>(r.length);

						for (TeleportRequest txx : r) {
							if (txx != null) {
								list.add(txx.sanitize());
							}
						}

						requests = List.copyOf(list);
						REQUESTS_CHANGED.invoke(Runnable::run);
						break;
					case "online":
						OnlinePlayer[] o = (OnlinePlayer[])JsonWire.decode(p.data(), OnlinePlayer[].class);
						if (o == null) {
							return;
						}

						List<OnlinePlayer> onlineList = new ArrayList<>(o.length);

						for (OnlinePlayer tx : o) {
							if (tx != null) {
								onlineList.add(tx.sanitize());
							}
						}

						online = List.copyOf(onlineList);
						ONLINE_CHANGED.invoke(Runnable::run);
						break;
					case "mail":
						PlayerRecord.Mail[] m = (PlayerRecord.Mail[])JsonWire.decode(p.data(), PlayerRecord.Mail[].class);
						if (m == null) {
							return;
						}

						List<PlayerRecord.Mail> mailList = new ArrayList<>(m.length);

						for (PlayerRecord.Mail t : m) {
							if (t != null) {
								mailList.add(t.sanitize());
							}
						}

						mail = List.copyOf(mailList);
						MAIL_CHANGED.invoke(Runnable::run);
						break;
					case "dashboard":
						AdminData.Dashboard d = (AdminData.Dashboard)JsonWire.decode(p.data(), AdminData.Dashboard.class);
						if (d != null && watching) {
							dashboard = d.sanitize();
							DASHBOARD_CHANGED.invoke(Runnable::run);
							return;
						}

						return;
					default:
						return;
				}

				CHANGED.invoke(Runnable::run);
			}
		}
	}

	public static void onNotice(ServerNet.Notice p) {
		ServerState.NoticeView v = new ServerState.NoticeView(p.key(), p.args(), p.color());
		NOTICE.invoke(l -> l.accept(v));
	}

	static void installPreview(
		Hello h, SelfState s, WarpList w, List<TeleportRequest> r, List<OnlinePlayer> o, List<PlayerRecord.Mail> m, @Nullable AdminData.Dashboard d
	) {
		if (hello == null || preview) {
			preview = true;
			hello = h.sanitize();
			self = s.sanitize();
			warps = w.sanitize();
			requests = List.copyOf(r);
			online = List.copyOf(o);
			mail = List.copyOf(m);
			dashboard = watching ? d : null;
			fireAll();
		}
	}

	public static void pretend(ServerState.Pretend p) {
		if (pretend != p) {
			pretend = p;
			fireAll();
		}
	}

	static void previewPut(String kind, Object value) {
		if (preview) {
			switch (kind) {
				case "hello":
					hello = ((Hello)value).sanitize();
					HELLO_CHANGED.invoke(Runnable::run);
					break;
				case "self":
					self = ((SelfState)value).sanitize();
					SELF_CHANGED.invoke(Runnable::run);
					break;
				case "warps":
					warps = ((WarpList)value).sanitize();
					WARPS_CHANGED.invoke(Runnable::run);
					break;
				case "requests":
					requests = List.copyOf((List)value);
					REQUESTS_CHANGED.invoke(Runnable::run);
					break;
				case "online":
					online = List.copyOf((List)value);
					ONLINE_CHANGED.invoke(Runnable::run);
					break;
				case "mail":
					mail = List.copyOf((List)value);
					MAIL_CHANGED.invoke(Runnable::run);
					break;
				case "dashboard":
					if (watching) {
						dashboard = (AdminData.Dashboard)value;
						DASHBOARD_CHANGED.invoke(Runnable::run);
					}

					return;
				default:
					return;
			}

			CHANGED.invoke(Runnable::run);
		}
	}

	public static void resetPreview(String variant) {
		if (hello == null || preview) {
			try {
				hello = null;
				SampleServer.fill(variant);
			} catch (Exception var2) {
				CFHGuiServer.LOGGER.warn("[CFHGui Server] the sample server could not be made: {}", var2.toString());
			}
		}
	}

	public static void previewScene(String scene) {
		if (preview) {
			SampleServer.scene(scene);
		}
	}

	static void previewNotice(String key, int color, String... args) {
		if (preview) {
			ServerState.NoticeView v = new ServerState.NoticeView(key, List.of(args), color);
			NOTICE.invoke(l -> l.accept(v));
		}
	}

	static void disconnected() {
		if (!FORCE_PREVIEW) {
			clear();
			fireAll();
			ensurePreview();
		}
	}

	private static void clear() {
		hello = null;
		self = new SelfState();
		warps = new WarpList();
		requests = List.of();
		online = List.of();
		mail = List.of();
		dashboard = null;
		watching = false;
		preview = false;
	}

	private static void fireAll() {
		HELLO_CHANGED.invoke(Runnable::run);
		SELF_CHANGED.invoke(Runnable::run);
		WARPS_CHANGED.invoke(Runnable::run);
		REQUESTS_CHANGED.invoke(Runnable::run);
		ONLINE_CHANGED.invoke(Runnable::run);
		MAIL_CHANGED.invoke(Runnable::run);
		DASHBOARD_CHANGED.invoke(Runnable::run);
		CHANGED.invoke(Runnable::run);
	}

	static void ensurePreview() {
		if (hello == null && (DevHarness.sampleData() || FORCE_PREVIEW)) {
			try {
				SampleServer.fill("full");
			} catch (Exception var1) {
				CFHGuiServer.LOGGER.warn("[CFHGui Server] the sample server could not be made: {}", var1.toString());
			}
		}
	}

	private ServerState() {
	}

	public static record NoticeView(String key, List<String> args, int color) {
	}

	public static enum Pretend {
		NOTHING,
		NO_SERVER,
		NEWER;
	}
}
