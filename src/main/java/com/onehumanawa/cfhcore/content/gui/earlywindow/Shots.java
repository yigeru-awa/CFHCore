package com.onehumanawa.cfhcore.content.gui.earlywindow;

import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import org.lwjgl.opengl.GL32C;
import org.lwjgl.stb.STBImageWrite;
import org.lwjgl.system.MemoryUtil;

final class Shots {
	private static final Path DIR;
	private static final long EVERY;
	private static final int MAX = 150;
	private static final int WIDEST = 1600;
	private static long last;
	private static int count;
	private static boolean broken;
	private static volatile Thread painter;
	private static volatile long painted;
	private static Thread watchdog;

	static void frame(int w, int h, String thread, float overall) {
		if (DIR != null) {
			watch("early".equals(thread));
			if (!broken && count < 150) {
				long now = System.nanoTime();
				if (last == 0L || now - last >= EVERY * 1000000L) {
					last = now;
					ByteBuffer pixels = null;
					ByteBuffer small = null;

					try {
						pixels = MemoryUtil.memAlloc(w * h * 4);
						GL32C.glFinish();
						GL32C.glReadPixels(0, 0, w, h, 6408, 5121, pixels);
						int step = 1;

						while (w / step > 1600) {
							step *= 2;
						}

						int sw = w / step;
						int sh = h / step;
						small = step == 1 ? pixels : MemoryUtil.memAlloc(sw * sh * 4);
						if (step > 1) {
							for (int y = 0; y < sh; y++) {
								for (int x = 0; x < sw; x++) {
									small.putInt((y * sw + x) * 4, pixels.getInt((y * step * w + x * step) * 4));
								}
							}
						}

						for (int i = 3; i < sw * sh * 4; i += 4) {
							small.put(i, (byte)-1);
						}

						Path file = DIR.resolve(String.format("earlywindow-%03d-%s-p%03d.png", count++, thread, overall < 0.0F ? 0 : Math.round(overall * 100.0F)));
						if (STBImageWrite.stbi_write_png(file.toString(), sw, sh, 4, small, sw * 4)) {
							CFHGuiLook.LOGGER.info("[CFHGui] picture of the start-up window: {}", file.getFileName());
						} else {
							broken = true;
						}
					} catch (LinkageError | RuntimeException var16) {
						broken = true;
						CFHGuiLook.LOGGER.warn("[CFHGui] no more pictures of the start-up window: {}", var16.toString());
					} finally {
						if (small != null && small != pixels) {
							MemoryUtil.memFree(small);
						}

						if (pixels != null) {
							MemoryUtil.memFree(pixels);
						}
					}
				}
			}
		}
	}

	private static void watch(boolean early) {
		if (!early) {
			painter = null;
		} else {
			painter = Thread.currentThread();
			painted = System.nanoTime();
			if (watchdog == null) {
				watchdog = new Thread(() -> {
					long reported = 0L;

					while (true) {
						try {
							Thread.sleep(250L);
						} catch (InterruptedException var10) {
							return;
						}

						Thread t = painter;
						if (t == null) {
							return;
						}

						long since = (System.nanoTime() - painted) / 1000000L;
						if (since >= 1000L && painted != reported) {
							reported = painted;
							StringBuilder where = new StringBuilder();

							for (StackTraceElement e : t.getStackTrace()) {
								where.append("\n\tat ").append(e);
							}

							CFHGuiLook.LOGGER.warn("[CFHGui] the start-up window's thread has not painted for {} ms ({}){}", new Object[]{since, t.getState(), where});
						}
					}
				}, "CFHGui start-up window watchdog");
				watchdog.setDaemon(true);
				watchdog.start();
			}
		}
	}

	private Shots() {
	}

	static {
		Path dir = null;
		long every = 600L;

		try {
			String folder = System.getProperty("cfh.earlywindow.shots");
			if (folder != null && !folder.isBlank()) {
				dir = Path.of(folder.trim());
				Files.createDirectories(dir);
			}

			every = Math.max(50L, Long.getLong("cfh.earlywindow.shotsEvery", 600L));
		} catch (Exception var4) {
			CFHGuiLook.LOGGER.warn("[CFHGui] no pictures of the start-up window: {}", var4.toString());
			dir = null;
		}

		DIR = dir;
		EVERY = every;
	}
}
