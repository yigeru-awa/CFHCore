package com.onehumanawa.cfhcore.content.gui.earlywindow;

import com.onehumanawa.cfhcore.content.gui.earlywindow.scene.LoadingScene;
import com.onehumanawa.cfhcore.content.gui.earlywindow.scene.StartupClock;
import com.onehumanawa.cfhcore.content.gui.earlywindow.scene.StartupTimeline;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.fml.loading.progress.ProgressMeter;
import net.neoforged.fml.loading.progress.StartupNotificationManager;
import net.neoforged.fml.loading.progress.StartupNotificationManager.AgeMessage;

final class FmlFeed implements LoadingScene.Feed {
	private static final String GAME = "Minecraft Progress";
	private static final String EARLY = "EARLY";
	private static final int SHEETS = 20;
	private final Path dir = FMLPaths.CONFIGDIR.get().resolve("slate");
	private final StartupTimeline timeline = StartupTimeline.read(this.dir);
	private final AtomicLong gone = new AtomicLong();
	private volatile boolean polling = true;
	private boolean saved;
	private float overall;
	private float taskProgress = -1.0F;
	private String task = "Loading";
	private String phase = "Loading";
	private String count;
	private String detail;
	private String phaseDetail;
	private long used;
	private long max;
	private List<ProgressMeter> steps = List.of();
	private ProgressMeter counted;
	private final boolean timer = CFHGuiLook.startupTimer();

	FmlFeed() {
		Thread poller = new Thread(this::poll, "CFHGui start-up stages");
		poller.setDaemon(true);
		poller.start();
	}

	private void poll() {
		List<ProgressMeter> before = List.of();
		boolean gameSeen = false;

		while (this.polling) {
			try {
				List<ProgressMeter> meters = StartupNotificationManager.getCurrentProgress();
				boolean game = false;

				for (ProgressMeter m : meters) {
					if ("EARLY".equals(m.name())) {
						String label = label(m);
						if (label != null && !label.startsWith("Loaded language provider")) {
							StartupTimeline.mark(StartupTimeline.key("label", label));
						}
					} else if ("Minecraft Progress".equals(m.name())) {
						game = true;
						StartupTimeline.mark("reload");
					} else {
						StartupTimeline.mark(StartupTimeline.key("meter", m.name()));
					}
				}

				for (ProgressMeter mx : before) {
					if (!contains(meters, mx)) {
						this.gone.incrementAndGet();
					}
				}

				before = meters;
				if (gameSeen && !game && StartupTimeline.mark("end")) {
					CFHGuiLook.LOGGER.info("[CFHGui] the game's loading overlay is done: the start-up window's bar is full");
				}

				gameSeen |= game;
				if (StartupTimeline.marked("end") >= 0L) {
					return;
				}

				Thread.sleep(25L);
			} catch (InterruptedException var8) {
				return;
			} catch (RuntimeException var9) {
			}
		}
	}

	void frame() {
		List<ProgressMeter> meters = StartupNotificationManager.getCurrentProgress();
		ProgressMeter early = null;
		ProgressMeter game = null;
		List<ProgressMeter> work = new ArrayList<>(2);

		for (int i = meters.size() - 1; i >= 0; i--) {
			ProgressMeter m = meters.get(i);
			if ("EARLY".equals(m.name())) {
				early = m;
			} else if ("Minecraft Progress".equals(m.name())) {
				game = m;
			} else if (work.size() < 2) {
				work.add(m);
			}
		}

		this.steps = work;
		ProgressMeter current = !work.isEmpty() ? work.get(0) : null;
		String log = latestLog();
		if (game != null) {
			this.phase = "Loading resources";
		} else if (early != null) {
			this.phase = label(early);
		} else {
			this.phase = log != null ? log : "Loading";
		}

		if (this.phase == null || this.phase.isBlank()) {
			this.phase = "Loading";
		}

		this.task = current != null ? label(current) : this.phase;
		this.detail = log != null && !log.equals(this.task) ? log : null;
		this.phaseDetail = log != null && !log.equals(this.phase) ? log : null;
		this.counted = current != null && current.steps() > 0 ? current : null;
		this.count = this.counted != null ? Math.min(this.counted.current(), this.counted.steps()) + " / " + this.counted.steps() : null;
		this.overall = this.timeline.progress(StartupClock.elapsedMs(), this::countOf);
		this.taskProgress = this.counted != null ? clamp(this.counted.progress()) : this.overall * 20.0F - (float)Math.floor((double)(this.overall * 20.0F));
		if (!this.saved && this.timeline.ended()) {
			this.finished();
		}

		Runtime rt = Runtime.getRuntime();
		this.max = rt.maxMemory();
		this.used = rt.totalMemory() - rt.freeMemory();
	}

	private double countOf(String stage) {
		ProgressMeter m = this.counted;
		return m != null && stage.equals(StartupTimeline.key("meter", m.name())) ? (double)clamp(m.progress()) : -1.0;
	}

	void finished() {
		this.polling = false;
		if (!this.saved && StartupTimeline.marked("end") >= 0L) {
			this.saved = true;
			String failed = StartupTimeline.save(this.dir);
			if (failed != null) {
				CFHGuiLook.LOGGER.debug("[CFHGui] could not keep the start-up's times: {}", failed);
			}
		}
	}

	List<ProgressMeter> steps() {
		return this.steps;
	}

	String phase() {
		return this.phase;
	}

	String phaseDetail() {
		return this.phaseDetail;
	}

	int percent() {
		return StartupTimeline.percent(this.overall, this.timeline.ended());
	}

	@Override
	public float overall() {
		return this.overall;
	}

	@Override
	public String task() {
		return this.task;
	}

	@Override
	public float taskProgress() {
		return this.taskProgress;
	}

	@Override
	public String taskCount() {
		return this.count;
	}

	@Override
	public long tasksDone() {
		return this.gone.get() + (long)Math.floor((double)(this.overall * 20.0F));
	}

	@Override
	public String detail() {
		return this.detail;
	}

	@Override
	public long memoryUsed() {
		return this.used;
	}

	@Override
	public long memoryMax() {
		return this.max;
	}

	@Override
	public String elapsed() {
		return this.timer ? StartupClock.clock(StartupClock.elapsedMs()) : null;
	}

	private static boolean contains(List<ProgressMeter> list, ProgressMeter m) {
		for (ProgressMeter n : list) {
			if (n == m) {
				return true;
			}
		}

		return false;
	}

	private static float clamp(float v) {
		return Math.max(0.0F, Math.min(1.0F, v));
	}

	static String label(ProgressMeter m) {
		if (m == null) {
			return null;
		} else {
			String text = m.label() != null ? m.label().getText() : null;
			return text != null && !text.isBlank() ? text : m.name();
		}
	}

	private static String latestLog() {
		try {
			return StartupNotificationManager.getMessages()
				.stream()
				.filter(a -> a.age() < 6000)
				.min(Comparator.comparingInt(AgeMessage::age))
				.map(a -> a.message().getText())
				.filter(s -> s != null && !s.isBlank())
				.orElse(null);
		} catch (RuntimeException var1) {
			return null;
		}
	}
}
