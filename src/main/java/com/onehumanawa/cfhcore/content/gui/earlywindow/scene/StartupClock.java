package com.onehumanawa.cfhcore.content.gui.earlywindow.scene;

import java.lang.management.ManagementFactory;
import java.util.Locale;

public final class StartupClock {
	private static long began = -1L;

	public static long began() {
		if (began < 0L) {
			long t;
			try {
				t = ManagementFactory.getRuntimeMXBean().getStartTime();
			} catch (LinkageError | RuntimeException var3) {
				t = System.currentTimeMillis();
			}

			began = t;
		}

		return began;
	}

	public static long elapsedMs() {
		return Math.max(0L, System.currentTimeMillis() - began());
	}

	public static String clock(long ms) {
		long s = ms / 1000L;
		return String.format(Locale.ROOT, "%d:%02d", s / 60L, s % 60L);
	}

	public static String taken(long ms) {
		long tenths = ms / 100L;
		return String.format(Locale.ROOT, "%d:%02d.%d", tenths / 600L, tenths / 10L % 60L, tenths % 10L);
	}

	private StartupClock() {
	}
}
