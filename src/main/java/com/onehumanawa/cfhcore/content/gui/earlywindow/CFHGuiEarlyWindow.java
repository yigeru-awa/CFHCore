package com.onehumanawa.cfhcore.content.gui.earlywindow;

import com.onehumanawa.cfhcore.content.gui.earlywindow.scene.IslandScene;
import com.onehumanawa.cfhcore.content.gui.earlywindow.scene.LoadingScene;
import com.onehumanawa.cfhcore.content.gui.earlywindow.scene.PackWindow;
import com.onehumanawa.cfhcore.content.gui.earlywindow.scene.StartupScene;
import com.onehumanawa.cfhcore.content.gui.earlywindow.scene.StartupTimeline;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import net.neoforged.fml.earlydisplay.ColourScheme;
import net.neoforged.fml.earlydisplay.DisplayWindow;
import net.neoforged.fml.earlydisplay.EarlyFramebuffer;
import net.neoforged.fml.earlydisplay.RenderElement;
import net.neoforged.fml.earlydisplay.ColourScheme.Colour;
import net.neoforged.fml.earlydisplay.RenderElement.DisplayContext;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL32C;
import org.lwjgl.system.MemoryStack;

public class CFHGuiEarlyWindow extends DisplayWindow {
	static final String NAME = "cfhgui_early_window";
	private CFHGuiScene scene;
	private PackWindow pack;
	private StartupScene factory;
	private CompletableFuture<StartupScene> building;
	private final CFHGuiEarlyWindow.SceneClock clock = new CFHGuiEarlyWindow.SceneClock();
	private FmlFeed feed;
	private boolean overhaul;
	private boolean island;
	private boolean vanillaStyle;
	private String mcVersion = "";
	private String loaderVersion = "";
	private CompletableFuture<ModJars> jars;
	private Thread gameThread;
	private volatile boolean installed;
	private boolean failed;
	private boolean factoryFailed;
	private List<RenderElement> fmlElements;

	public String name() {
		return "cfhgui_early_window";
	}

	public Runnable initialize(String[] arguments) {
		StartupTimeline.mark("window");
		CFHGuiLook.restoreSelection();
		this.pack = CFHGuiLook.packWindow();
		if (this.pack != null) {
			CFHGuiLook.LOGGER.info("[CFHGui] the start-up window is the modpack's own ({})", this.pack);
		}

		this.overhaul = this.pack == null && "OVERHAUL".equals(CFHGuiLook.layout());
		if (CFHGuiLook.standalone()) {
			System.setProperty("cfh.earlywindow.standalone", "true");
			CFHGuiLook.LOGGER.info("[CFHGui] CFHGui UI is not among the mods: the start-up window shows the Overhaul layout by itself");
		}

		this.island = this.overhaul && ("island".equals(StartupScene.kind(CFHGuiLook.startupScene())) || !ModJars.createAmongMods());
		this.vanillaStyle = this.overhaul && CFHGuiLook.vanillaStyle();
		if (this.pack != null) {
			CFHGuiLook.recolour(this.pack.background & 16777215, this.pack.text & 16777215);
		} else if (this.island) {
			CFHGuiLook.recolour(IslandScene.sky(CFHGuiLook.darkLoading()), 16777215);
		} else if (!this.vanillaStyle) {
			CFHGuiLook.recolour();
		}

		this.gameThread = Thread.currentThread();
		Semaphore lock = null;

		try {
			this.mcVersion = arg(arguments, "--fml.mcVersion", "1.21.1");
			this.loaderVersion = arg(arguments, "--fml.neoForgeVersion", "").split("-")[0];
			this.scene = new CFHGuiScene(CFHGuiLook.accent(), CFHGuiLook.radius(), CFHGuiLook.pixelFont(), this.mcVersion, this.loaderVersion, this.pack);
			this.feed = new FmlFeed();
			if (this.overhaul) {
				String neoForm = arg(arguments, "--fml.neoFormVersion", "");
				boolean create = !this.island;
				this.jars = CompletableFuture.supplyAsync(() -> ModJars.find(this.mcVersion, neoForm, create));
			}

			lock = (Semaphore)FmlAccess.RENDER_LOCK.get(this);
			lock.acquireUninterruptibly();
		} catch (RuntimeException | LinkageError | ReflectiveOperationException var6) {
			CFHGuiLook.LOGGER.warn("[CFHGui] NeoForge's own start-up screen stays: {}", var6.toString());
			lock = null;
		}

		Runnable tick;
		try {
			tick = super.initialize(arguments);
		} catch (Error | RuntimeException var7) {
			if (lock != null) {
				lock.release();
			}

			throw var7;
		}

		Semaphore held = lock;
		Thread installer = new Thread(() -> this.install(held), "CFHGui early window");
		installer.setDaemon(true);
		installer.start();
		CFHGuiLook.slow("early");
		return tick;
	}

	public void initWindow(String mcVersion) {
		super.initWindow(mcVersion);
		if (this.scene != null) {
			long window = 0L;

			try {
				Field schemeField = DisplayWindow.class.getDeclaredField("colourScheme");
				schemeField.setAccessible(true);
				Colour bg = ((ColourScheme)schemeField.get(this)).background();
				window = FmlAccess.WINDOW.getLong(this);
				GLFW.glfwMakeContextCurrent(window);
				GL.createCapabilities();
				GL32C.glClearColor(bg.redf(), bg.greenf(), bg.bluef(), 1.0F);
				GL32C.glClear(16384);
				GLFW.glfwSwapBuffers(window);
			} catch (RuntimeException | LinkageError | ReflectiveOperationException var14) {
				CFHGuiLook.LOGGER.debug("[CFHGui] the start-up window was not cleared before its first frame: {}", var14.toString());
			} finally {
				if (window != 0L) {
					try {
						GLFW.glfwMakeContextCurrent(0L);
					} catch (LinkageError | RuntimeException var13) {
					}
				}
			}
		}
	}

	public long setupMinecraftWindow(IntSupplier width, IntSupplier height, Supplier<String> title, LongSupplier monitor) {
		StartupTimeline.mark("handover");
		return super.setupMinecraftWindow(width, height, title, monitor);
	}

	private void install(Semaphore lock) {
		if (lock != null) {
			try {
				long until = System.nanoTime() + 20000000000L;

				while (FmlAccess.WINDOW_TICK.get(this) == null && System.nanoTime() <= until) {
					Thread.sleep(2L);
				}

				if (FmlAccess.WINDOW_TICK.get(this) != null) {
					this.fmlElements = (List<RenderElement>)FmlAccess.ELEMENTS.get(this);
					FmlAccess.ELEMENTS.set(this, new ArrayList<>(List.of(SceneElement.create(this::paint))));
					this.installed = true;
					FmlAccess.pace(this, 16L);
				}
			} catch (InterruptedException var10) {
				Thread.currentThread().interrupt();
			} catch (RuntimeException | LinkageError | ReflectiveOperationException var11) {
				CFHGuiLook.LOGGER.warn("[CFHGui] NeoForge's own start-up screen stays: {}", var11.toString());
			} finally {
				lock.release();
			}
		}

		if (!this.installed) {
			long until = System.nanoTime() + 30000000000L;

			while (System.nanoTime() < until && !CFHGuiLook.dropFox(this)) {
				try {
					Thread.sleep(5L);
				} catch (InterruptedException var9) {
					return;
				}
			}
		}
	}

	private void paint(DisplayContext context) {
		if (!this.failed) {
			try {
				DisplayContext canvas = this.fitCanvas(context);
				int w = canvas.scaledWidth();
				int h = canvas.scaledHeight();
				String thread = Thread.currentThread() == this.gameThread ? "game" : "early";
				this.feed.frame();
				if (this.overhaul && !this.factoryFailed) {
					StartupScene made = this.factory(canvas);
					if (made != null) {
						try {
							made.draw(w, h, made instanceof IslandScene ? (float)CFHGuiLook.guiScale(w, h) : (float)CFHGuiScene.scaleFor(w, h), true, 1.0F, this.feed);
							Shots.frame(w, h, thread, this.feed.overall());
							return;
						} catch (LinkageError | RuntimeException var9) {
							CFHGuiLook.LOGGER.error("[CFHGui] the Overhaul start-up screen failed, the Custom one takes over", var9);
							this.dropFactory();
							this.factoryFailed = true;
						}
					} else if (!this.factoryFailed) {
						Colour bg = canvas.colourScheme().background();
						GL32C.glClearColor(bg.redf(), bg.greenf(), bg.bluef(), 1.0F);
						GL32C.glClear(16384);
						Shots.frame(w, h, thread, this.feed.overall());
						return;
					}
				}

				if (this.overhaul && this.vanillaStyle) {
					throw new IllegalStateException("the Overhaul start-up screen is not there, and the Custom one has no Vanilla style");
				}

				this.scene.draw(w, h, this.feed);
				Shots.frame(w, h, thread, this.feed.overall());
			} catch (RuntimeException | LinkageError | ReflectiveOperationException var10) {
				this.failed = true;
				CFHGuiLook.LOGGER.error("[CFHGui] start-up screen failed, NeoForge's own takes over", var10);

				try {
					FmlAccess.ELEMENTS.set(this, new ArrayList<>(this.fmlElements));
				} catch (RuntimeException | ReflectiveOperationException var8) {
				}
			}
		}
	}

	private StartupScene factory(DisplayContext canvas) {
		if (this.factory != null) {
			return this.factory;
		} else if (this.building == null) {
			Colour bg = canvas.colourScheme().background();
			int background = 0xFF000000 | (bg.red() & 0xFF) << 16 | (bg.green() & 0xFF) << 8 | bg.blue() & 0xFF;
			this.building = new CompletableFuture<>();
			Thread maker = new Thread(() -> {
				try {
					this.building.complete(this.make(background));
				} catch (Throwable var3x) {
					this.building.completeExceptionally(var3x);
				}
			}, "CFHGui start-up scene");
			maker.setDaemon(true);
			maker.start();
			return null;
		} else if (!this.building.isDone()) {
			return null;
		} else {
			try {
				this.factory = this.building.join();
				this.clock.resume();
				return this.factory;
			} catch (CancellationException | CompletionException var5) {
				this.factoryFailed = true;
				CFHGuiLook.LOGGER
					.error("[CFHGui] the Overhaul start-up screen could not be made, the Custom one takes over", (Throwable)(var5.getCause() != null ? var5.getCause() : var5));
				return null;
			}
		}
	}

	private StartupScene make(int background) {
		String font = this.vanillaStyle ? "monocraft" : CFHGuiLook.pixelFont();
		ModJars mods = null;

		try {
			if (this.jars != null) {
				mods = this.jars.get(4000L, TimeUnit.MILLISECONDS);
			}
		} catch (InterruptedException var10) {
			Thread.currentThread().interrupt();
		} catch (TimeoutException | ExecutionException var11) {
			CFHGuiLook.LOGGER.debug("[CFHGui] no mod jars for the start-up screen: {}", var11.toString());
		}

		StartupScene var13;
		try {
			StartupScene made = StartupScene.make(
				this.island ? "island" : "factory",
				new LoadingScene.Look(
					CFHGuiLook.accent(),
					this.vanillaStyle,
					background,
					font,
					"Minecraft " + this.mcVersion,
					this.loaderVersion.isEmpty() ? "" : "NeoForge " + this.loaderVersion
				),
				file -> CFHGuiScene.bytes("/cfhgui_earlywindow/" + file),
				this.clock,
				mods,
				CFHGuiLook.titleLayout(),
				e -> CFHGuiLook.LOGGER.warn("[CFHGui] no island for the start-up screen, the factory stands in: {}", e.toString())
			);
			if (made instanceof IslandScene) {
				CFHGuiLook.LOGGER.info("[CFHGui] the start-up scene is the island of the game's own blocks");
			}

			if (made instanceof LoadingScene scene) {
				if (scene.create()) {
					CFHGuiLook.LOGGER.info("[CFHGui] the start-up factory is built of Create's blocks");
				} else if (mods == null) {
					CFHGuiLook.LOGGER.warn("[CFHGui] the mods folder was not read in time: the start-up factory is CFHGui's own, with or without Create");
				} else if (mods.hasCreate()) {
					CFHGuiLook.LOGGER
						.warn(
							"[CFHGui] Create is installed, but the start-up factory could not be built of its blocks (CFHGui's own stands in): {}",
							String.valueOf(scene.createFailure())
						);
				} else {
					CFHGuiLook.LOGGER.info("[CFHGui] the start-up factory is CFHGui's own (Create is not among the mods)");
				}
			}

			var13 = made;
		} finally {
			if (mods != null) {
				mods.close();
			}
		}

		return var13;
	}

	private void dropFactory() {
		StartupScene old = this.factory;
		this.factory = null;
		if (old != null) {
			try {
				old.dispose();
			} catch (LinkageError | RuntimeException var3) {
			}
		}
	}

	private DisplayContext fitCanvas(DisplayContext context) throws ReflectiveOperationException {
		int w;
		int h;
		if (Thread.currentThread() == this.gameThread) {
			MemoryStack stack = MemoryStack.stackPush();

			try {
				IntBuffer pw = stack.mallocInt(1);
				IntBuffer ph = stack.mallocInt(1);
				GLFW.glfwGetFramebufferSize(FmlAccess.WINDOW.getLong(this), pw, ph);
				w = pw.get(0);
				h = ph.get(0);
			} catch (Throwable var10) {
				if (stack != null) {
					try {
						stack.close();
					} catch (Throwable var9) {
						var10.addSuppressed(var9);
					}
				}

				throw var10;
			}

			if (stack != null) {
				stack.close();
			}
		} else {
			w = FmlAccess.FB_WIDTH.getInt(this);
			h = FmlAccess.FB_HEIGHT.getInt(this);
		}

		if (w >= 320 && h >= 240 && w <= 16384 && h <= 16384) {
			if (context.width() == w && context.height() == h && context.scale() == 1) {
				return context;
			} else {
				DisplayContext next = new DisplayContext(w, h, 1, context.elementShader(), context.colourScheme(), context.performance());
				EarlyFramebuffer old = (EarlyFramebuffer)FmlAccess.FRAMEBUFFER.get(this);
				int active = GL32C.glGetInteger(34016);
				GL32C.glActiveTexture(33984);
				int bound = GL32C.glGetInteger(32873);
				EarlyFramebuffer framebuffer = FmlAccess.NEW_FRAMEBUFFER.newInstance(next);
				GL32C.glBindTexture(3553, bound);
				GL32C.glActiveTexture(active);
				FmlAccess.ACTIVATE.invoke(framebuffer);
				GL32C.glViewport(0, 0, w, h);
				FmlAccess.CONTEXT.set(this, next);
				FmlAccess.FRAMEBUFFER.set(this, framebuffer);
				old.close();
				return next;
			}
		} else {
			return context;
		}
	}

	public void addMojangTexture(int textureId) {
		if (!this.installed || this.failed) {
			super.addMojangTexture(textureId);
		}
	}

	public void close() {
		if (StartupTimeline.mark("end")) {
			CFHGuiLook.LOGGER.info("[CFHGui] the start-up window closes: the start is over");
		}

		if (this.feed != null) {
			this.feed.finished();
		}

		if (this.scene != null && this.installed) {
			try {
				this.dropFactory();
				this.scene.dispose();
			} catch (LinkageError | RuntimeException var2) {
				CFHGuiLook.LOGGER.warn("[CFHGui] start-up screen cleanup: {}", var2.toString());
			}
		}

		super.close();
	}

	public void updateModuleReads(ModuleLayer layer) {
		Module neoforge = layer.findModule("neoforge").orElseThrow();
		CFHGuiEarlyWindow.class.getModule().addReads(neoforge);

		try {
			Class<?> overlay = Class.forName(neoforge, "net.neoforged.neoforge.client.loading.NeoForgeLoadingOverlay");
			Method factory = null;

			for (Method m : overlay.getMethods()) {
				if (Modifier.isStatic(m.getModifiers()) && m.getName().equals("newInstance")) {
					factory = m;
				}
			}

			Field field = DisplayWindow.class.getDeclaredField("loadingOverlay");
			field.setAccessible(true);
			field.set(this, factory);
		} catch (RuntimeException | ReflectiveOperationException var9) {
			throw new IllegalStateException("CFHGui early window: NeoForge's loading overlay not found", var9);
		}
	}

	private static String arg(String[] args, String name, String fallback) {
		for (int i = 0; i + 1 < args.length; i++) {
			if (name.equals(args[i])) {
				return args[i + 1];
			}
		}

		return fallback;
	}

	static final class SceneClock implements LongSupplier {
		private static final long NONE = Long.MIN_VALUE;
		private volatile long held = Long.MIN_VALUE;
		private volatile long shift = Long.MIN_VALUE;

		@Override
		public long getAsLong() {
			long now = System.nanoTime();
			if (this.shift != Long.MIN_VALUE) {
				return now - this.shift;
			} else {
				if (this.held == Long.MIN_VALUE) {
					this.held = now;
				}

				return this.held;
			}
		}

		void resume() {
			if (this.shift == Long.MIN_VALUE) {
				this.shift = this.held == Long.MIN_VALUE ? 0L : System.nanoTime() - this.held;
			}
		}
	}
}
