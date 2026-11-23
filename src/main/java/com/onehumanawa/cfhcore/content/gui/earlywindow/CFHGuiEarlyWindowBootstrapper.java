package com.onehumanawa.cfhcore.content.gui.earlywindow;

import net.neoforged.neoforgespi.earlywindow.GraphicsBootstrapper;

public class CFHGuiEarlyWindowBootstrapper implements GraphicsBootstrapper {
	public String name() {
		return "cfhgui_early_window";
	}

	public void bootstrap(String[] arguments) {
		CFHGuiLook.choose("cfhgui_early_window", CFHGuiLook.enabled());
	}
}
