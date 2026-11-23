package com.onehumanawa.cfhcore.content.gui.server.permission;

import com.onehumanawa.cfhcore.content.gui.core.perm.Perms;
import com.onehumanawa.cfhcore.content.gui.core.perm.Perms.Node;

public final class ExtraNodes {
	public static final Node TP_COMPANIONS = Perms.declare("cfhgui_server.tp.companions", 0);

	public static void init() {
	}

	private ExtraNodes() {
	}
}
