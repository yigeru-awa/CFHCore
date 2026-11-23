package com.onehumanawa.cfhcore.content.gui.veinminer.config;

import com.onehumanawa.cfhcore.content.gui.core.config.JsonConfig;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.veinminer.VeinShape;
import java.util.Locale;
import java.util.function.Consumer;

public final class VeinClientConfig {
	public String activation = "HOLD";
	public boolean scrollCycles = true;
	public String shape = "shapeless";
	public boolean diagonals = false;
	public boolean treeDiagonals = true;
	public boolean preview = true;
	public String previewStyle = "OUTLINE";
	public boolean throughWalls = true;
	public String color = "ACCENT";
	public String customColor = "#E0A458";
	public int maxPreview = 256;
	public boolean hud = true;
	public String hudPosition = "CROSSHAIR";
	private static JsonConfig<VeinClientConfig> file;
	private static volatile int version;

	public VeinClientConfig.Activation activation() {
		try {
			return VeinClientConfig.Activation.valueOf(this.activation.trim().toUpperCase(Locale.ROOT));
		} catch (RuntimeException var2) {
			return VeinClientConfig.Activation.HOLD;
		}
	}

	public VeinClientConfig.Style style() {
		try {
			return VeinClientConfig.Style.valueOf(this.previewStyle.trim().toUpperCase(Locale.ROOT));
		} catch (RuntimeException var2) {
			return VeinClientConfig.Style.OUTLINE;
		}
	}

	public VeinClientConfig.ColorMode colorMode() {
		try {
			return VeinClientConfig.ColorMode.valueOf(this.color.trim().toUpperCase(Locale.ROOT));
		} catch (RuntimeException var2) {
			return VeinClientConfig.ColorMode.ACCENT;
		}
	}

	public VeinClientConfig.HudPosition hudPosition() {
		try {
			return VeinClientConfig.HudPosition.valueOf(this.hudPosition.trim().toUpperCase(Locale.ROOT));
		} catch (RuntimeException var2) {
			return VeinClientConfig.HudPosition.CROSSHAIR;
		}
	}

	public VeinShape shape() {
		return VeinShape.parse(this.shape, VeinShape.SHAPELESS);
	}

	public int customArgb() {
		return 0xFF000000 | Colors.fromHex(this.customColor, -2055080);
	}

	public VeinClientConfig sanitize() {
		if (this.activation == null) {
			this.activation = "HOLD";
		}

		this.activation = this.activation().name();
		if (this.previewStyle == null) {
			this.previewStyle = "OUTLINE";
		}

		this.previewStyle = this.style().name();
		if (this.color == null) {
			this.color = "ACCENT";
		}

		this.color = this.colorMode().name();
		if (this.hudPosition == null) {
			this.hudPosition = "CROSSHAIR";
		}

		this.hudPosition = this.hudPosition().name();
		this.shape = this.shape().id();
		if (this.customColor == null || this.customColor.isBlank()) {
			this.customColor = "#E0A458";
		}

		this.maxPreview = Math.max(1, Math.min(4096, this.maxPreview));
		return this;
	}

	public static synchronized JsonConfig<VeinClientConfig> file() {
		if (file == null) {
			file = JsonConfig.of("veinminer", VeinClientConfig.class, VeinClientConfig::new);
			((VeinClientConfig)file.get()).sanitize();
		}

		return file;
	}

	public static VeinClientConfig get() {
		return (VeinClientConfig)file().get();
	}

	public static synchronized void reload() {
		file().load();
		((VeinClientConfig)file.get()).sanitize();
		version++;
	}

	public static synchronized void edit(Consumer<VeinClientConfig> change) {
		file().update(c -> {
			change.accept(c);
			c.sanitize();
		});
		version++;
	}

	public static int version() {
		return version;
	}

	public static enum Activation {
		HOLD,
		TOGGLE;
	}

	public static enum ColorMode {
		ACCENT,
		CUSTOM;
	}

	public static enum HudPosition {
		CROSSHAIR,
		BELOW_CROSSHAIR,
		ABOVE_HOTBAR,
		TOP_LEFT;
	}

	public static enum Style {
		OUTLINE,
		OUTLINE_FILL,
		FILL;
	}
}
