package com.onehumanawa.cfhcore.content.gui.earlywindow;

import com.onehumanawa.cfhcore.content.gui.earlywindow.scene.PackWindow;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.ServiceLoader.Provider;
import java.util.concurrent.Semaphore;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import net.neoforged.fml.earlydisplay.ColourScheme;
import net.neoforged.fml.earlydisplay.DisplayWindow;
import net.neoforged.fml.earlydisplay.ColourScheme.Colour;
import net.neoforged.fml.loading.FMLConfig;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.fml.loading.FMLConfig.ConfigValue;
import net.neoforged.neoforgespi.earlywindow.ImmediateWindowProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class CFHGuiLook {
	static final Logger LOGGER = LoggerFactory.getLogger("CFHGui early window");
	static final String FML_DEFAULT = "fmlearlywindow";
	private static final Colour BACKGROUND = new Colour(22, 22, 21);
	private static final Colour FOREGROUND = new Colour(236, 234, 228);
	private static final Pattern SKIN = Pattern.compile("\"skin\"\\s*:\\s*\"(\\w+)\"");
	private static final Pattern ACCENT = Pattern.compile("\"accent\"\\s*:\\s*\"#?([0-9a-fA-F]{6})\"");
	private static final Pattern RADIUS = Pattern.compile("\"radius\"\\s*:\\s*(\\d+)");
	private static final int DEFAULT_ACCENT = -7726052;
	private static final int DEFAULT_RADIUS = 3;
	private static final int FORMER_DEFAULT_ACCENT = -2523042;
	private static final Pattern ACCENT_MIGRATED = Pattern.compile("\"accentMigrated\"\\s*:\\s*true");
	private static final Pattern LAYOUT = Pattern.compile("\"customLayout\"\\s*:\\s*(true|false)");
	private static final Pattern LOADING = Pattern.compile("\"loadingScreens\"\\s*:\\s*(true|false)");
	private static final Pattern HEADING_FONT = Pattern.compile("\"headingFont\"\\s*:\\s*(true|false)");
	private static final Pattern PIXEL_FONT = Pattern.compile("\"pixelFont\"\\s*:\\s*\"(\\w+)\"");
	private static final Pattern STARTUP_SCENE = Pattern.compile("\"startupScene\"\\s*:\\s*\"(\\w+)\"");
	private static String coreJson;
	private static String menuJson;
	private static int guiScale = -1;
	private static boolean unicodeFont;
	private static boolean darkLoading;
	private static Properties props;
	private static boolean propsRead;
	private static String replaced;
	static final String STANDALONE = "cfh.earlywindow.standalone";
	private static final Pattern MENU_MOD_ID = Pattern.compile("modId\\s*=\\s*[\"']cfhgui_menu[\"']\\s*(#.*)?");
	private static Boolean standalone;
	private static PackWindow pack;
	private static boolean packRead;

	static boolean enabled() {
		if (standalone()) {
			return true;
		} else {
			Properties p = props();
			if (p != null) {
				String layout = p.getProperty("layout", "CUSTOM").trim();
				return "VANILLA".equalsIgnoreCase(layout) ? false : "OVERHAUL".equalsIgnoreCase(layout) || !vanillaStyle() || packWindow() != null;
			} else {
				return (darkSkin() || packWindow() != null) && flag(LAYOUT, core()) && flag(LOADING, menu());
			}
		}
	}

	static PackWindow packWindow() {
		if (packRead) {
			return pack;
		} else {
			packRead = true;

			try {
				pack = PackWindow.read(FMLPaths.CONFIGDIR.get().resolve("slate"), FMLPaths.GAMEDIR.get(), m -> LOGGER.warn("[CFHGui] {}", m));
			} catch (LinkageError | RuntimeException var1) {
				LOGGER.warn("[CFHGui] the modpack's start-up window could not be read: {}", var1.toString());
			}

			return pack;
		}
	}

	static boolean vanillaStyle() {
		Properties p = props();
		return p != null ? "VANILLA".equalsIgnoreCase(p.getProperty("style", "CFH").trim()) : !darkSkin();
	}

	static String layout() {
		if (standalone()) {
			return "OVERHAUL";
		} else {
			Properties p = props();
			String v = p == null ? null : p.getProperty("layout");
			if (v != null) {
				return v.trim().toUpperCase(Locale.ROOT);
			} else if (core().isEmpty()) {
				return "OVERHAUL";
			} else {
				return flag(LAYOUT, core()) ? "CUSTOM" : "VANILLA";
			}
		}
	}

	private static Properties props() {
		if (propsRead) {
			return props;
		} else {
			propsRead = true;

			try {
				Path file = FMLPaths.CONFIGDIR.get().resolve("slate").resolve("earlywindow.properties");
				if (!Files.isRegularFile(file)) {
					return null;
				}

				Properties p = new Properties();

				try (BufferedReader in = Files.newBufferedReader(file)) {
					p.load(in);
				}

				props = p;
			} catch (Exception var7) {
				LOGGER.warn("[CFHGui] could not read earlywindow.properties: {}", var7.toString());
			}

			return props;
		}
	}

	private static boolean darkSkin() {
		Matcher m = SKIN.matcher(core());
		return !m.find() || !"VANILLA".equalsIgnoreCase(m.group(1));
	}

	private static boolean flag(Pattern pattern, String json) {
		Matcher m = pattern.matcher(json);
		return !m.find() || Boolean.parseBoolean(m.group(1));
	}

	static int accent() {
		int saved = savedAccent();
		return saved == -2523042 && !ACCENT_MIGRATED.matcher(core()).find() ? -7726052 : saved;
	}

	private static int savedAccent() {
		Properties p = props();
		if (p != null) {
			String v = p.getProperty("accent", "").trim().replace("#", "");
			if (v.matches("[0-9a-fA-F]{6}")) {
				return 0xFF000000 | Integer.parseInt(v, 16);
			}
		}

		Matcher m = ACCENT.matcher(core());
		return m.find() ? 0xFF000000 | Integer.parseInt(m.group(1), 16) : -7726052;
	}

	static String pixelFont() {
		Properties p = props();
		boolean headingFont = p != null ? Boolean.parseBoolean(p.getProperty("headingFont", "true")) : flag(HEADING_FONT, core());
		if (!headingFont) {
			return "monocraft";
		} else {
			Matcher m = PIXEL_FONT.matcher(core());
			String key = p != null ? p.getProperty("pixelFont", "").trim().toLowerCase(Locale.ROOT) : (m.find() ? m.group(1).toLowerCase(Locale.ROOT) : "");
			return !key.equals("monocraft") && !key.equals("pixelify") ? "pixeloid" : key;
		}
	}

	static String startupScene() {
		Properties p = props();
		String v = p == null ? null : p.getProperty("startupScene");
		if (v != null) {
			return v.trim();
		} else {
			Matcher m = STARTUP_SCENE.matcher(core());
			return m.find() ? m.group(1) : "factory";
		}
	}

	static boolean startupTimer() {
		Properties p = props();
		return p == null || !"false".equalsIgnoreCase(p.getProperty("startupTimer", "true").trim());
	}

	static String titleLayout() {
		if (standalone()) {
			return "VANILLA";
		} else {
			Properties p = props();
			String v = p == null ? null : p.getProperty("titleLayout");
			return v != null ? v.trim().toUpperCase(Locale.ROOT) : layout();
		}
	}

	static boolean standalone() {
		if (standalone == null) {
			standalone = !uiInstalled();
		}

		return standalone;
	}

	private static boolean uiInstalled() {
		if (ClassLoader.getSystemResource("dev/fallingcloud/slate/menu/CFHGuiMenu.class") != null) {
			return true;
		} else {
			try {
				try (DirectoryStream<Path> mods = Files.newDirectoryStream(FMLPaths.MODSDIR.get(), "*.jar")) {
					for (Path jar : mods) {
						if (jar.getFileName().toString().toLowerCase(Locale.ROOT).contains("slate")) {
							try (ZipFile zip = new ZipFile(jar.toFile())) {
								for (String name : new String[]{"META-INF/neoforge.mods.toml", "META-INF/mods.toml"}) {
									ZipEntry entry = zip.getEntry(name);
									if (entry != null) {
										try (InputStream in = zip.getInputStream(entry)) {
											if (declaresMenu(new String(in.readAllBytes(), StandardCharsets.UTF_8))) {
												return true;
											}
										}
									}
								}
							} catch (RuntimeException | IOException var17) {
							}
						}
					}
				}

				return false;
			} catch (RuntimeException | IOException var19) {
				LOGGER.warn("[CFHGui] could not look for CFHGui UI among the mods: {}", var19.toString());
				return true;
			}
		}
	}

	private static boolean declaresMenu(String toml) {
		boolean mods = false;

		for (String raw : toml.split("\\R")) {
			String line = raw.trim();
			if (line.startsWith("[")) {
				mods = line.replace(" ", "").startsWith("[[mods]]");
			} else if (mods && MENU_MOD_ID.matcher(line).matches()) {
				return true;
			}
		}

		return false;
	}

	static int guiScale(int w, int h) {
		options();
		int i = 1;

		while (i != guiScale && i < w && i < h && w / (i + 1) >= 320 && h / (i + 1) >= 240) {
			i++;
		}

		if (unicodeFont && i % 2 != 0) {
			i++;
		}

		return i;
	}

	static boolean darkLoading() {
		options();
		return darkLoading;
	}

	private static void options() {
		if (guiScale < 0) {
			guiScale = 0;

			try {
				Path file = FMLPaths.GAMEDIR.get().resolve("options.txt");
				if (!Files.isRegularFile(file)) {
					return;
				}

				for (String line : Files.readAllLines(file)) {
					int colon = line.indexOf(58);
					if (colon >= 0) {
						String key = line.substring(0, colon).trim();
						String value = line.substring(colon + 1).trim();
						switch (key) {
							case "guiScale":
								try {
									guiScale = Math.max(0, Integer.parseInt(value));
								} catch (NumberFormatException var9) {
								}
								break;
							case "forceUnicodeFont":
								unicodeFont = Boolean.parseBoolean(value);
								break;
							case "darkMojangStudiosBackground":
								darkLoading = Boolean.parseBoolean(value);
						}
					}
				}
			} catch (Exception var10) {
				LOGGER.debug("[CFHGui] could not read options.txt: {}", var10.toString());
			}
		}
	}

	static int radius() {
		Properties p = props();
		if (p != null) {
			try {
				return Math.max(0, Math.min(4, Integer.parseInt(p.getProperty("radius", "3").trim())));
			} catch (NumberFormatException var2) {
			}
		}

		Matcher m = RADIUS.matcher(core());
		return m.find() ? Math.min(4, Integer.parseInt(m.group(1))) : 3;
	}

	private static String core() {
		if (coreJson == null) {
			coreJson = read("core.json");
		}

		return coreJson;
	}

	private static String menu() {
		if (menuJson == null) {
			menuJson = read("menu.json");
		}

		return menuJson;
	}

	private static String read(String name) {
		try {
			Path file = FMLPaths.CONFIGDIR.get().resolve("slate").resolve(name);
			return Files.isRegularFile(file) ? Files.readString(file) : "";
		} catch (Exception var2) {
			return "";
		}
	}

	static void choose(String name, boolean cfh) {
		String current = FMLConfig.getConfigValue(ConfigValue.EARLY_WINDOW_PROVIDER);
		if (name.equals(current)) {
			if (cfh) {
				replaced = "fmlearlywindow";
			} else {
				setProvider("fmlearlywindow");
			}
		} else if ("fmlearlywindow".equals(current)) {
			if (cfh && setProvider(name)) {
				replaced = current;
			}
		} else if (current != null && !current.isBlank() && !offered(current)) {
			LOGGER.warn(
				"[CFHGui] config/fml.toml names a start-up window no jar offers (\"{}\"): {} shows instead, and the file gets NeoForge's default back",
				current,
				cfh ? "CFHGui's" : "NeoForge's own"
			);
			if (setProvider(cfh ? name : "fmlearlywindow")) {
				replaced = "fmlearlywindow";
			}
		}
	}

	private static boolean offered(String provider) {
		try {
			ModuleLayer layer = CFHGuiLook.class.getModule().getLayer();
			ServiceLoader<ImmediateWindowProvider> loader = layer != null
				? ServiceLoader.load(layer, ImmediateWindowProvider.class)
				: ServiceLoader.load(ImmediateWindowProvider.class, CFHGuiLook.class.getClassLoader());
			Iterator<Provider<ImmediateWindowProvider>> it = loader.stream().iterator();

			while (it.hasNext()) {
				try {
					if (provider.equals(it.next().get().name())) {
						return true;
					}
				} catch (RuntimeException | LinkageError | ServiceConfigurationError var5) {
				}
			}

			return false;
		} catch (RuntimeException | LinkageError | ServiceConfigurationError var6) {
			return true;
		}
	}

	static void slow(String stage) {
		String spec = System.getProperty("cfh.startupSlow");
		if (spec != null && !spec.isBlank()) {
			for (String part : spec.split("[;,]")) {
				int colon = part.indexOf(58);
				if (colon >= 0 && part.substring(0, colon).trim().equalsIgnoreCase(stage)) {
					try {
						long ms = Long.parseLong(part.substring(colon + 1).trim());
						LOGGER.info("[CFHGui] development: the start's {} stage is made {} ms slower (cfh.startupSlow)", stage, ms);
						Thread.sleep(Math.max(0L, Math.min(ms, 300000L)));
					} catch (NumberFormatException var9) {
						LOGGER.warn("[CFHGui] cfh.startupSlow: not a number of milliseconds for {}: {}", stage, part);
					} catch (InterruptedException var10) {
						Thread.currentThread().interrupt();
					}

					return;
				}
			}
		}
	}

	static void restoreSelection() {
		if (replaced != null) {
			setProvider(replaced);
		}

		replaced = null;
	}

	private static boolean setProvider(String name) {
		try {
			Field instance = FMLConfig.class.getDeclaredField("INSTANCE");
			Field data = FMLConfig.class.getDeclaredField("configData");
			instance.setAccessible(true);
			data.setAccessible(true);
			Method set = null;

			for (Method m : ConfigValue.class.getDeclaredMethods()) {
				if (m.getName().equals("setConfigValue") && m.getParameterCount() == 2) {
					set = m;
				}
			}

			if (set == null) {
				throw new NoSuchMethodException("FMLConfig.ConfigValue.setConfigValue");
			} else {
				set.setAccessible(true);
				set.invoke(ConfigValue.EARLY_WINDOW_PROVIDER, data.get(instance.get(null)), name);
				return true;
			}
		} catch (RuntimeException | ReflectiveOperationException var8) {
			LOGGER.warn("[CFHGui] NeoForge's own loading screen stays: {}", var8.toString());
			return false;
		}
	}

	static void recolour() {
		recolour(BACKGROUND, FOREGROUND);
	}

	static void recolour(int background, int foreground) {
		recolour(
			new Colour(background >> 16 & 0xFF, background >> 8 & 0xFF, background & 0xFF),
			new Colour(foreground >> 16 & 0xFF, foreground >> 8 & 0xFF, foreground & 0xFF)
		);
	}

	private static void recolour(Colour background, Colour foreground) {
		try {
			Field bg = ColourScheme.class.getDeclaredField("background");
			Field fg = ColourScheme.class.getDeclaredField("foreground");
			bg.setAccessible(true);
			fg.setAccessible(true);

			for (ColourScheme s : ColourScheme.values()) {
				bg.set(s, background);
				fg.set(s, foreground);
			}
		} catch (RuntimeException | ReflectiveOperationException var8) {
			LOGGER.warn("[CFHGui] loading screen keeps NeoForge's colours: {}", var8.toString());
		}
	}

	static boolean dropFox(DisplayWindow window) {
		try {
			Field elements = DisplayWindow.class.getDeclaredField("elements");
			Field lock = DisplayWindow.class.getDeclaredField("renderLock");
			elements.setAccessible(true);
			lock.setAccessible(true);
			if (!(elements.get(window) instanceof List<?> current)) {
				return false;
			}

			Semaphore renderLock = (Semaphore)lock.get(window);
			renderLock.acquire();

			try {
				List<?> now = (List<?>)elements.get(window);
				if (now.size() >= 5) {
					List<Object> rest = new ArrayList<>((Collection<? extends Object>)now);
					rest.remove(0);
					elements.set(window, rest);
				}
			} finally {
				renderLock.release();
			}
		} catch (InterruptedException var12) {
			Thread.currentThread().interrupt();
		} catch (RuntimeException | ReflectiveOperationException var13) {
			LOGGER.warn("[CFHGui] loading screen keeps its fox: {}", var13.toString());
		}

		return true;
	}

	private CFHGuiLook() {
	}
}
