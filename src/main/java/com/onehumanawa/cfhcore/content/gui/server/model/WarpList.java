package com.onehumanawa.cfhcore.content.gui.server.model;

import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.Nullable;

public final class WarpList {
	public List<Warp> warps = new ArrayList<>();
	@Nullable
	public Place spawn;

	public WarpList sanitize() {
		if (this.warps == null) {
			this.warps = new ArrayList<>();
		}

		this.warps.removeIf(wx -> wx == null);

		for (Warp w : this.warps) {
			w.sanitize();
		}

		if (this.spawn != null) {
			this.spawn.sanitize();
		}

		return this;
	}
}
