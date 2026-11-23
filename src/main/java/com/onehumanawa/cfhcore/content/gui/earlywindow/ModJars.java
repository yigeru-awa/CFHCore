package com.onehumanawa.cfhcore.content.gui.earlywindow;

import com.onehumanawa.cfhcore.content.gui.earlywindow.scene.Assets;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.fml.loading.LibraryFinder;

final class ModJars implements Assets, AutoCloseable {
	private static final String CREATE = "assets/create/models/block/belt/start.json";
	private final Map<String, ZipFile> jars = new HashMap<>();

	static ModJars find(String mcVersion, String neoFormVersion, boolean withCreate) {
		ModJars found = new ModJars();
		ZipFile game = game(mcVersion, neoFormVersion);
		if (game != null) {
			found.jars.put("minecraft", game);
		}

		ZipFile create = withCreate ? create() : null;
		if (create != null) {
			found.jars.put("create", create);
		}

		return found;
	}

	static boolean createAmongMods() {
		Path dev = devCreateJar();
		if (dev != null) {
			try (ZipFile zip = new ZipFile(dev.toFile())) {
				if (zip.getEntry("assets/create/models/block/belt/start.json") != null) {
					return true;
				}
			} catch (RuntimeException | IOException var15) {
				CFHGuiLook.LOGGER.warn("[CFHGui] cfh.earlywindow.createJar is no jar of Create: {}", var15.toString());
			}
		}

		try {
			try (DirectoryStream<Path> mods = Files.newDirectoryStream(FMLPaths.MODSDIR.get(), "*.jar")) {
				for (Path jar : mods) {
					if (jar.getFileName().toString().toLowerCase(Locale.ROOT).startsWith("create")) {
						try (ZipFile zipx = new ZipFile(jar.toFile())) {
							if (zipx.getEntry("assets/create/models/block/belt/start.json") != null) {
								return true;
							}
						} catch (RuntimeException | IOException var11) {
						}
					}
				}
			}

			return false;
		} catch (RuntimeException | IOException var13) {
			return false;
		}
	}

	private static ZipFile create() {
		List<Path> named = new ArrayList<>();
		List<Path> others = new ArrayList<>();
		Path dev = devCreateJar();
		if (dev != null) {
			named.add(dev);
		}

		try (DirectoryStream<Path> mods = Files.newDirectoryStream(FMLPaths.MODSDIR.get(), "*.jar")) {
			for (Path jar : mods) {
				String name = jar.getFileName().toString().toLowerCase(Locale.ROOT);
				(name.startsWith("create-") ? named : others).add(jar);
			}
		} catch (RuntimeException | IOException var12) {
			if (named.isEmpty()) {
				return null;
			}
		}

		named.addAll(others);

		for (Path jar : named) {
			ZipFile zip = null;

			try {
				zip = new ZipFile(jar.toFile());
				if (zip.getEntry("assets/create/models/block/belt/start.json") != null) {
					return zip;
				}

				zip.close();
			} catch (RuntimeException | IOException var10) {
				if (zip != null) {
					try {
						zip.close();
					} catch (IOException var8) {
					}
				}
			}
		}

		return null;
	}

	private static Path devCreateJar() {
		try {
			String path = System.getProperty("cfh.earlywindow.createJar");
			if (path != null && !path.isBlank()) {
				Path jar = Path.of(path.trim());
				return Files.isRegularFile(jar) ? jar : null;
			} else {
				return null;
			}
		} catch (RuntimeException var2) {
			return null;
		}
	}

	private static ZipFile game(String mcVersion, String neoFormVersion) {
		try {
			Path jar = LibraryFinder.findPathForMaven("net.minecraft", "client", "", "extra", mcVersion + "-" + neoFormVersion);
			return Files.isRegularFile(jar) ? new ZipFile(jar.toFile()) : null;
		} catch (RuntimeException | LinkageError | IOException var3) {
			return null;
		}
	}

	boolean hasCreate() {
		return this.jars.containsKey("create");
	}

	@Override
	public byte[] read(String namespace, String path) {
		String file = "assets/" + namespace + "/" + path;
		ZipFile jar = this.jars.get(namespace);

		try {
			if (jar == null) {
				if (namespace.equals("minecraft")) {
					byte[] var15;
					try (InputStream in = ClassLoader.getSystemResourceAsStream(file)) {
						var15 = in == null ? null : in.readAllBytes();
					}

					return var15;
				} else {
					return null;
				}
			} else {
				ZipEntry entry = jar.getEntry(file);
				if (entry == null) {
					return null;
				} else {
					byte[] var7;
					try (InputStream in = jar.getInputStream(entry)) {
						var7 = in.readAllBytes();
					}

					return var7;
				}
			}
		} catch (RuntimeException | IOException var13) {
			return null;
		}
	}

	@Override
	public void close() {
		for (ZipFile jar : this.jars.values()) {
			try {
				jar.close();
			} catch (IOException var4) {
			}
		}

		this.jars.clear();
	}
}
