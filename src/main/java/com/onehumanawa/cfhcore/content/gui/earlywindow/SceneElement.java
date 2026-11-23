package com.onehumanawa.cfhcore.content.gui.earlywindow;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.function.Consumer;
import net.neoforged.fml.earlydisplay.RenderElement;
import net.neoforged.fml.earlydisplay.RenderElement.DisplayContext;

final class SceneElement {
	private static final String RENDERER = "net.neoforged.fml.earlydisplay.RenderElement$Renderer";
	private static final String INITIALIZER = "net.neoforged.fml.earlydisplay.RenderElement$Initializer";

	static RenderElement create(Consumer<DisplayContext> paint) throws ReflectiveOperationException {
		ClassLoader loader = RenderElement.class.getClassLoader();
		Class<?> rendererType = Class.forName("net.neoforged.fml.earlydisplay.RenderElement$Renderer", false, loader);
		Class<?> initializerType = Class.forName("net.neoforged.fml.earlydisplay.RenderElement$Initializer", false, loader);
		Object renderer = proxy(loader, rendererType, "accept", args -> {
			paint.accept((DisplayContext)args[1]);
			return null;
		});
		Object initializer = proxy(loader, initializerType, "get", args -> renderer);
		return RenderElement.class.getConstructor(initializerType).newInstance(initializer);
	}

	private static Object proxy(ClassLoader loader, Class<?> type, String method, SceneElement.Call call) {
		InvocationHandler handler = (self, m, args) -> {
			String name = m.getName();

			switch (name) {
				case "hashCode" -> {
					return System.identityHashCode(self);
				}
				case "equals" -> {
					return self == args[0];
				}
				case "toString" -> {
					return "CFHGui start-up screen";
				}
				default -> {
					if (!name.equals(method)) {
						throw new UnsupportedOperationException(name);
					}

					return call.run(args);
				}
			}
				};
		return Proxy.newProxyInstance(loader, new Class[]{type}, handler);
	}

	private SceneElement() {
	}

	private interface Call {
		Object run(Object[] var1);
	}
}
