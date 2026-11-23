package com.onehumanawa.cfhcore.content.gui.server.client.ui.admin;

import com.onehumanawa.cfhcore.content.gui.core.screen.SidebarPage;
import com.onehumanawa.cfhcore.content.gui.core.screen.SidebarScreen;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import org.jetbrains.annotations.Nullable;

public final class AdminPages {
	private static final List<AdminPages.Extra> EXTRAS = new CopyOnWriteArrayList<>();

	public static void add(AdminPages.Extra extra) {
		EXTRAS.removeIf(e -> e.id().equals(extra.id()));
		EXTRAS.add(extra);
	}

	static List<AdminPages.Extra> all() {
		return EXTRAS;
	}

	@Nullable
	static AdminPages.Extra get(String id) {
		for (AdminPages.Extra e : EXTRAS) {
			if (e.id().equals(id)) {
				return e;
			}
		}

		return null;
	}

	private AdminPages() {
	}

	public static record Extra(String id, BooleanSupplier shown, Function<SidebarScreen, SidebarPage> make) {
	}
}
