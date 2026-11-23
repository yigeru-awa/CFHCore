package com.onehumanawa.cfhcore.content.gui.server.service;

import com.onehumanawa.cfhcore.content.gui.core.platform.CFHGuiPlatform;
import com.onehumanawa.cfhcore.content.gui.server.CFHGuiServer;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

final class Thirst {
	private static final Thirst.Kind[] KINDS = new Thirst.Kind[]{
		new Thirst.Kind(
				"toughasnails",
				"Tough As Nails",
				new Thirst.Value("getThirst", "setThirst", 20.0F),
				new Thirst.Value("getHydration", "setHydration", 20.0F),
				new Thirst.Value("getExhaustion", "setExhaustion", 0.0F),
				(p, rec) -> run(rec, "setLastThirst", -1),
				call("toughasnails.api.thirst.ThirstHelper", "getThirst")
			)
			.when("toughasnails.api.thirst.ThirstHelper", "isThirstEnabled"),
		new Thirst.Kind(
			"thirst",
			"Thirst Was Taken",
			new Thirst.Value("getThirst", "setThirst", 20.0F),
			new Thirst.Value("getQuenched", "setQuenched", 20.0F),
			new Thirst.Value("getExhaustion", "setExhaustion", 0.0F),
			(p, rec) -> run(rec, "updateThirstData", p),
			attached("dev.ghen.thirst.foundation.common.capability.ModAttachment", "PLAYER_THIRST"),
			attached("dev.ghen.thirst.foundation.common.capability.ModCapabilities", "PLAYER_THIRST")
		),
		new Thirst.Kind(
			"legendarysurvivaloverhaul",
			"Legendary Survival Overhaul",
			new Thirst.Value("getHydrationLevel", "setHydrationLevel", 20.0F),
			new Thirst.Value("getSaturationLevel", "setSaturation", 20.0F),
			new Thirst.Value("getThirstExhaustion", "setExhaustion", 0.0F),
			(p, rec) -> run(rec, "setDirty"),
			call("sfiomn.legendarysurvivaloverhaul.util.AttachmentUtil", "getThirstAttachment"),
			call("sfiomn.legendarysurvivaloverhaul.util.CapabilityUtil", "getThirstCapability")
		),
		new Thirst.Kind(
			"homeostatic",
			"Homeostatic",
			new Thirst.Value("getWaterLevel", "setWaterLevel", 20.0F),
			new Thirst.Value("getWaterSaturationLevel", "setWaterSaturationLevel", 5.0F),
			new Thirst.Value("getWaterExhaustionLevel", "setWaterExhaustionLevel", 0.0F),
			(p, rec) -> {
				Object info = Class.forName("homeostatic.common.water.WaterInfo")
					.getConstructor(int.class, float.class, float.class)
					.newInstance(
						((Number)run(rec, "getWaterLevel")).intValue(),
						((Number)run(rec, "getWaterSaturationLevel")).floatValue(),
						((Number)run(rec, "getWaterExhaustionLevel")).floatValue()
					);
				run(Class.forName("homeostatic.platform.Services").getField("PLATFORM").get(null), "syncWaterData", p, info);
			},
			service("homeostatic.platform.Services", "PLATFORM", "getWaterCapabilty")
		),
		new Thirst.Kind(
			"dehydration",
			"Dehydration",
			new Thirst.Value("getThirstLevel", "setThirstLevel", 20.0F),
			null,
			new Thirst.Value("dehydration", null, 0.0F),
			null,
			own("net.dehydration.access.ThirstManagerAccess", "getThirstManager")
		),
		new Thirst.Kind("tfc", "TerraFirmaCraft", new Thirst.Value("getThirst", "setThirst", 100.0F), null, null, (p, rec) -> {
			if (has(rec, "forceUpdate")) {
				run(rec, "forceUpdate");
			}
		}, call("net.dries007.tfc.common.player.IPlayerInfo", "get"), food("net.dries007.tfc.common.capabilities.food.TFCFoodData"))
	};
	private static volatile boolean looked;

	static boolean present() {
		look();

		for (Thirst.Kind k : KINDS) {
			if (k.reach != null) {
				return true;
			}
		}

		return false;
	}

	static void fill(ServerPlayer player) {
		look();

		for (Thirst.Kind k : KINDS) {
			Object rec = k.record(player);
			if (rec != null) {
				try {
					k.level.write(rec, k.level.full);
					if (k.quench != null) {
						k.quench.write(rec, k.quench.full);
					}

					if (k.wear != null) {
						k.wear.write(rec, 0.0F);
					}

					if (k.sync != null) {
						k.sync.sync(player, rec);
					}
				} catch (RuntimeException | LinkageError | ReflectiveOperationException var7) {
					k.broke(var7);
				}
			}
		}
	}

	@Nullable
	static float[][] read(ServerPlayer player) {
		look();
		float[][] out = null;

		for (int i = 0; i < KINDS.length; i++) {
			Thirst.Kind k = KINDS[i];
			Object rec = k.record(player);
			if (rec != null) {
				try {
					float[] v = new float[]{k.level.read(rec), k.quench == null ? Float.NaN : k.quench.read(rec), k.wear == null ? Float.NaN : k.wear.read(rec)};
					if (out == null) {
						out = new float[KINDS.length][];
					}

					out[i] = v;
				} catch (RuntimeException | LinkageError | ReflectiveOperationException var6) {
					k.broke(var6);
				}
			}
		}

		return out;
	}

	static void hold(ServerPlayer player, float[][] mark) {
		for (int i = 0; i < KINDS.length; i++) {
			float[] m = mark[i];
			if (m != null) {
				Thirst.Kind k = KINDS[i];
				Object rec = k.record(player);
				if (rec != null) {
					try {
						m[0] = keep(rec, k.level, m[0], true);
						if (k.quench != null) {
							m[1] = keep(rec, k.quench, m[1], true);
						}

						if (k.wear != null) {
							m[2] = keep(rec, k.wear, m[2], false);
						}
					} catch (RuntimeException | LinkageError | ReflectiveOperationException var7) {
						k.broke(var7);
					}
				}
			}
		}
	}

	private static float keep(Object rec, Thirst.Value v, float mark, boolean more) throws ReflectiveOperationException {
		float now = v.read(rec);
		if (more ? !(now < mark) : !(now > mark)) {
			return now;
		} else {
			v.write(rec, mark);
			return mark;
		}
	}

	private static void look() {
		if (!looked) {
			synchronized (Thirst.class) {
				if (!looked) {
					for (Thirst.Kind k : KINDS) {
						find(k);
					}

					looked = true;
				}
			}
		}
	}

	private static void find(Thirst.Kind k) {
		if (CFHGuiPlatform.get().isModLoaded(k.id)) {
			Throwable failed = null;

			for (Thirst.Resolve way : k.ways) {
				try {
					k.reach = way.resolve();
					break;
				} catch (RuntimeException | LinkageError | ReflectiveOperationException var8) {
					if (failed == null) {
						failed = var8;
					}
				}
			}

			if (k.reach == null) {
				k.broken = true;
				CFHGuiServer.LOGGER.warn("[CFHGui Server] {} is installed but its thirst could not be reached ({}); feeding and god mode leave it alone", k.name, failed);
			} else {
				if (k.on != null) {
					try {
						if (!Boolean.TRUE.equals(method(Class.forName(k.on[0]), k.on[1], 0, null).invoke(null))) {
							k.reach = null;
							CFHGuiServer.LOGGER.info("[CFHGui Server] {} is installed with its thirst switched off: there is no thirst to fill or hold", k.name);
							return;
						}
					} catch (RuntimeException | LinkageError | ReflectiveOperationException var7) {
					}
				}

				CFHGuiServer.LOGGER.info("[CFHGui Server] {} found: feeding and /drink fill its thirst, and it does not go down in god mode", k.name);
			}
		}
	}

	private static Thirst.Resolve call(String owner, String method) {
		return () -> {
			Method m = method(Class.forName(owner), method, 1, Thirst::takesPlayer);
			if (!Modifier.isStatic(m.getModifiers())) {
				throw new NoSuchMethodException(owner + "." + method + " is not static");
			} else {
				return p -> unwrap(m.invoke(null, p));
			}
		};
	}

	private static Thirst.Resolve service(String owner, String field, String method) {
		return () -> {
			Object target = Class.forName(owner).getField(field).get(null);
			if (target == null) {
				throw new NoSuchFieldException(owner + "." + field + " is not set");
			} else {
				Method m = method(target.getClass(), method, 1, Thirst::takesPlayer);
				return p -> unwrap(m.invoke(target, p));
			}
		};
	}

	private static Thirst.Resolve own(String owner, String method) {
		return () -> {
			Class<?> type = Class.forName(owner);
			Method m = method(type, method, 0, null);
			return p -> type.isInstance(p) ? m.invoke(p) : null;
		};
	}

	private static Thirst.Resolve attached(String owner, String field) {
		return () -> {
			Object key = Class.forName(owner).getField(field).get(null);
			if (key == null) {
				throw new NoSuchFieldException(owner + "." + field + " is not set");
			} else {
				Method m = null;

				for (String name : new String[]{"getData", "getCapability"}) {
					m = takes(ServerPlayer.class, name, key.getClass());
					if (m != null) {
						break;
					}
				}

				if (m == null) {
					throw new NoSuchMethodException("no getData or getCapability on players takes " + key.getClass().getName());
				} else {
					Method get = m;
					return p -> unwrap(get.invoke(p, key));
				}
			}
		};
	}

	private static Thirst.Resolve food(String owner) {
		return () -> {
			Class<?> type = Class.forName(owner);
			return p -> type.isInstance(p.getFoodData()) ? p.getFoodData() : null;
		};
	}

	@Nullable
	private static Object unwrap(@Nullable Object o) throws ReflectiveOperationException {
		if (o instanceof Optional<?> opt) {
			return opt.orElse(null);
		} else {
			return o != null && o.getClass().getName().endsWith("LazyOptional") ? unwrap(run(o, "resolve")) : o;
		}
	}

	private static boolean numeric(Class<?> c) {
		return c == int.class || c == float.class || c == double.class;
	}

	private static boolean takesPlayer(Class<?> c) {
		return c.isAssignableFrom(ServerPlayer.class);
	}

	private static Method method(Class<?> owner, String name, int params, @Nullable Predicate<Class<?>> param) throws NoSuchMethodException {
		for (Method m : owner.getMethods()) {
			if (m.getName().equals(name) && m.getParameterCount() == params && (param == null || param.test(m.getParameterTypes()[0]))) {
				return callable(m);
			}
		}

		throw new NoSuchMethodException(owner.getName() + "." + name);
	}

	@Nullable
	private static Method takes(Class<?> owner, String name, Class<?> arg) throws NoSuchMethodException {
		for (Method m : owner.getMethods()) {
			if (m.getName().equals(name) && m.getParameterCount() == 1 && m.getParameterTypes()[0].isAssignableFrom(arg)) {
				return callable(m);
			}
		}

		return null;
	}

	private static Method callable(Method m) {
		if (Modifier.isPublic(m.getDeclaringClass().getModifiers())) {
			return m;
		} else {
			ArrayDeque<Class<?>> todo = new ArrayDeque<>(List.of(m.getDeclaringClass()));

			while (!todo.isEmpty()) {
				Class<?> c = todo.poll();
				if (Modifier.isPublic(c.getModifiers())) {
					try {
						Method found = c.getMethod(m.getName(), m.getParameterTypes());
						if (Modifier.isPublic(found.getDeclaringClass().getModifiers())) {
							return found;
						}
					} catch (NoSuchMethodException var4) {
					}
				}

				if (c.getSuperclass() != null) {
					todo.add(c.getSuperclass());
				}

				todo.addAll(List.of(c.getInterfaces()));
			}

			m.setAccessible(true);
			return m;
		}
	}

	private static boolean has(Object target, String name) {
		for (Method m : target.getClass().getMethods()) {
			if (m.getName().equals(name)) {
				return true;
			}
		}

		return false;
	}

	@Nullable
	private static Object run(Object target, String name, Object... args) throws ReflectiveOperationException {
		for (Method m : target.getClass().getMethods()) {
			if (m.getName().equals(name) && m.getParameterCount() == args.length) {
				Class<?>[] types = m.getParameterTypes();
				boolean fits = true;

				for (int i = 0; i < args.length && fits; i++) {
					fits = fits(types[i], args[i]);
				}

				if (fits) {
					return callable(m).invoke(target, args);
				}
			}
		}

		throw new NoSuchMethodException(target.getClass().getName() + "." + name);
	}

	private static boolean fits(Class<?> type, Object arg) {
		if (!type.isPrimitive()) {
			return type.isInstance(arg);
		} else {
			return type == int.class
				? arg instanceof Integer
				: (type == float.class ? arg instanceof Float : (type == boolean.class ? arg instanceof Boolean : type == double.class && arg instanceof Double));
		}
	}

	private Thirst() {
	}

	private static final class Kind {
		final String id;
		final String name;
		final Thirst.Value level;
		@Nullable
		final Thirst.Value quench;
		@Nullable
		final Thirst.Value wear;
		@Nullable
		final Thirst.Sync sync;
		final Thirst.Resolve[] ways;
		@Nullable
		String[] on;
		@Nullable
		Thirst.Reach reach;
		boolean broken;

		Kind(
			String id, String name, Thirst.Value level, @Nullable Thirst.Value quench, @Nullable Thirst.Value wear, @Nullable Thirst.Sync sync, Thirst.Resolve... ways
		) {
			this.id = id;
			this.name = name;
			this.level = level;
			this.quench = quench;
			this.wear = wear;
			this.sync = sync;
			this.ways = ways;
		}

		Thirst.Kind when(String owner, String method) {
			this.on = new String[]{owner, method};
			return this;
		}

		@Nullable
		Object record(ServerPlayer player) {
			Thirst.Reach r = this.reach;
			if (r != null && !this.broken) {
				try {
					Object rec = r.of(player);
					if (rec != null && !this.level.resolved()) {
						this.level.resolve(rec.getClass());
						if (this.quench != null) {
							this.quench.resolve(rec.getClass());
						}

						if (this.wear != null) {
							this.wear.resolve(rec.getClass());
						}
					}

					return rec;
				} catch (RuntimeException | LinkageError | ReflectiveOperationException var4) {
					this.broke(var4);
					return null;
				}
			} else {
				return null;
			}
		}

		void broke(Throwable e) {
			if (!this.broken) {
				Throwable var10000;
				label15: {
					this.broken = true;
					this.reach = null;
					if (e instanceof InvocationTargetException ite && ite.getCause() != null) {
						var10000 = ite.getCause();
						break label15;
					}

					var10000 = e;
				}

				Throwable cause = var10000;
				CFHGuiServer.LOGGER
					.warn("[CFHGui Server] {}'s thirst could not be read or set ({}); feeding and god mode leave it alone from now on", this.name, cause.toString());
			}
		}
	}

	@FunctionalInterface
	private interface Reach {
		@Nullable
		Object of(ServerPlayer var1) throws ReflectiveOperationException;
	}

	@FunctionalInterface
	private interface Resolve {
		Thirst.Reach resolve() throws ReflectiveOperationException;
	}

	@FunctionalInterface
	private interface Sync {
		void sync(ServerPlayer var1, Object var2) throws ReflectiveOperationException;
	}

	private static final class Value {
		final String get;
		@Nullable
		final String set;
		final float full;
		@Nullable
		Method getter;
		@Nullable
		Method setter;
		@Nullable
		Field field;
		@Nullable
		Class<?> type;

		Value(String get, @Nullable String set, float full) {
			this.get = get;
			this.set = set;
			this.full = full;
		}

		boolean resolved() {
			return this.type != null;
		}

		void resolve(Class<?> owner) throws ReflectiveOperationException {
			if (this.set == null) {
				Field f = owner.getField(this.get);
				this.field = f;
				this.type = f.getType();
			} else {
				this.getter = Thirst.method(owner, this.get, 0, null);
				Method s = Thirst.method(owner, this.set, 1, Thirst::numeric);
				this.setter = s;
				this.type = s.getParameterTypes()[0];
			}
		}

		float read(Object rec) throws ReflectiveOperationException {
			Object v = this.field != null ? this.field.get(rec) : this.getter.invoke(rec);
			return ((Number)v).floatValue();
		}

		void write(Object rec, float v) throws ReflectiveOperationException {
			Object boxed = this.type == int.class ? Math.round(v) : (this.type == double.class ? (double)v : v);
			if (this.field != null) {
				this.field.set(rec, boxed);
			} else {
				this.setter.invoke(rec, boxed);
			}
		}
	}
}
