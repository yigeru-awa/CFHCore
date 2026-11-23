package com.onehumanawa.cfhcore.content.gui.crafting.team;

import com.onehumanawa.cfhcore.content.gui.core.config.JsonConfig;
import com.onehumanawa.cfhcore.content.gui.crafting.CFHGuiCrafting;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

final class PinStore {
	@Nullable
	private final Path file;
	private PinStore.Data data = new PinStore.Data();
	private boolean dirty;

	PinStore(@Nullable Path file) {
		this.file = file;
	}

	String world() {
		if (this.data.world == null || this.data.world.isBlank() || this.data.world.length() > 64) {
			this.data.world = UUID.randomUUID().toString();
			this.dirty = true;
		}

		return this.data.world;
	}

	List<Pin> pins(UUID team) {
		List<Pin> pins = this.data.teams.get(team.toString());
		return pins == null ? List.of() : pins;
	}

	void add(UUID team, Pin pin) {
		this.data.teams.computeIfAbsent(team.toString(), k -> new ArrayList<>()).add(pin);
		this.dirty = true;
	}

	void remove(UUID team, Pin pin) {
		List<Pin> pins = this.data.teams.get(team.toString());
		if (pins != null && pins.remove(pin)) {
			if (pins.isEmpty()) {
				this.data.teams.remove(team.toString());
			}

			this.dirty = true;
		}
	}

	void drop(UUID team) {
		if (this.data.teams.remove(team.toString()) != null) {
			this.dirty = true;
		}
	}

	boolean dirty() {
		return this.dirty;
	}

	void load() {
		this.data = new PinStore.Data();
		this.dirty = false;
		if (this.file != null && Files.isRegularFile(this.file)) {
			try (Reader r = Files.newBufferedReader(this.file, StandardCharsets.UTF_8)) {
				PinStore.Data read = (PinStore.Data)JsonConfig.GSON.fromJson(r, PinStore.Data.class);
				if (read != null) {
					this.data = read;
				}
			} catch (Exception var6) {
				CFHGuiCrafting.LOGGER.warn("[CFHGui Crafting] could not read the team pins of {} ({}); the teams start without pins", this.file, var6.toString());
			}
		}

		if (this.data.teams == null) {
			this.data.teams = new LinkedHashMap<>();
		}

		this.data.teams.entrySet().removeIf(ex -> ex.getValue() == null);

		for (List<Pin> pins : this.data.teams.values()) {
			pins.removeIf(p -> p == null || !p.sanitize());
		}

		this.data.teams.entrySet().removeIf(ex -> ((List)ex.getValue()).isEmpty());
		this.world();
	}

	void save() {
		if (this.dirty) {
			this.dirty = false;
			if (this.file != null) {
				try {
					Files.createDirectories(this.file.getParent());
					Path tmp = this.file.resolveSibling(this.file.getFileName() + ".tmp");
					Files.writeString(tmp, JsonConfig.GSON.toJson(this.data), StandardCharsets.UTF_8);

					try {
						Files.move(tmp, this.file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
					} catch (IOException var3) {
						Files.move(tmp, this.file, StandardCopyOption.REPLACE_EXISTING);
					}
				} catch (IOException var4) {
					CFHGuiCrafting.LOGGER.error("[CFHGui Crafting] could not write the team pins to {}", this.file, var4);
				}
			}
		}
	}

	private static final class Data {
		int version = 1;
		String world = "";
		Map<String, List<Pin>> teams = new LinkedHashMap<>();
	}
}
