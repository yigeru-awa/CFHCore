package com.onehumanawa.cfhcore.content.gui.earlywindow;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import net.neoforged.fml.earlydisplay.DisplayWindow;
import net.neoforged.fml.earlydisplay.EarlyFramebuffer;
import net.neoforged.fml.earlydisplay.RenderElement.DisplayContext;

final class FmlAccess {
	static final Field ELEMENTS = field("elements");
	static final Field RENDER_LOCK = field("renderLock");
	static final Field WINDOW_TICK = field("windowTick");
	static final Field CONTEXT = field("context");
	static final Field FRAMEBUFFER = field("framebuffer");
	static final Field FB_WIDTH = field("fbWidth");
	static final Field FB_HEIGHT = field("fbHeight");
	static final Field WINDOW = field("window");
	static final Constructor<EarlyFramebuffer> NEW_FRAMEBUFFER;
	static final Method ACTIVATE;

	static boolean pace(DisplayWindow window, long millis) {
		try {
			Field scheduler = DisplayWindow.class.getDeclaredField("renderScheduler");
			scheduler.setAccessible(true);
			Method render = DisplayWindow.class.getDeclaredMethod("renderThreadFunc");
			render.setAccessible(true);
			ScheduledExecutorService service = (ScheduledExecutorService)scheduler.get(window);
			ScheduledFuture<?> old = (ScheduledFuture<?>)WINDOW_TICK.get(window);
			if (service != null && old != null) {
				Runnable tick = () -> {
					try {
						render.invoke(window);
					} catch (RuntimeException | ReflectiveOperationException var3x) {
					}
				};
				ScheduledFuture<?> faster = service.scheduleAtFixedRate(tick, millis, millis, TimeUnit.MILLISECONDS);
				WINDOW_TICK.set(window, faster);
				old.cancel(false);
				return true;
			} else {
				return false;
			}
		} catch (RuntimeException | ReflectiveOperationException var9) {
			return false;
		}
	}

	private static Field field(String name) {
		try {
			Field f = DisplayWindow.class.getDeclaredField(name);
			f.setAccessible(true);
			return f;
		} catch (NoSuchFieldException var2) {
			throw new IllegalStateException("DisplayWindow." + name, var2);
		}
	}

	private FmlAccess() {
	}

	static {
		try {
			NEW_FRAMEBUFFER = EarlyFramebuffer.class.getDeclaredConstructor(DisplayContext.class);
			NEW_FRAMEBUFFER.setAccessible(true);
			ACTIVATE = EarlyFramebuffer.class.getDeclaredMethod("activate");
			ACTIVATE.setAccessible(true);
		} catch (ReflectiveOperationException var1) {
			throw new IllegalStateException(var1);
		}
	}
}
