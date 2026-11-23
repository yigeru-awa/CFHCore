package com.onehumanawa.cfhcore.content.gui.quests.server.ftb;

import com.mojang.serialization.Dynamic;
import com.onehumanawa.cfhcore.content.gui.quests.CFHGuiQuests;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.References;
import org.jetbrains.annotations.Nullable;

public final class LegacyItems {
	static final String LEFT = "\u0000left";
	private static boolean said;
	private static boolean began;

	public static void install() {
		FtbItems.upgrader = LegacyItems::upgrade;
	}

	@Nullable
	private static Map<String, Object> upgrade(Map<String, Object> item, int version) {
		try {
			int now = SharedConstants.getCurrentVersion().getDataVersion().getVersion();
			if (version >= now) {
				return null;
			} else {
				if (!began) {
					began = true;
					CFHGuiQuests.LOGGER
						.info("[CFHGui Quests] bringing the items of an older book up to this version of the game (the game's data fixer takes a moment the first time)");
				}

				Map<String, Object> clean = new LinkedHashMap<>(item);
				clean.keySet().removeIf(k -> k.startsWith("\u0000") || k.equals("ForgeCaps"));
				if (!clean.containsKey("Count")) {
					clean.put("Count", new Snbt.Num("1b", 1.0));
				}

				CompoundTag tag = TagParser.parseTag(Snbt.write(clean));
				Tag fixed = (Tag)DataFixers.getDataFixer().update(References.ITEM_STACK, new Dynamic(NbtOps.INSTANCE, tag), version, now).getValue();
				if (fixed instanceof CompoundTag out && !out.getString("id").isBlank()) {
					Map<String, Object> made = new LinkedHashMap<>(Snbt.map(Snbt.parse(out.toString())));
					Map<String, Object> components = new LinkedHashMap<>(Snbt.map(made.get("components")));
					if (components.remove("minecraft:custom_data") != null) {
						made.put("\u0000left", Boolean.TRUE);
					}

					if (version <= 1343 && !out.getString("id").startsWith("minecraft:") && components.remove("minecraft:damage") != null) {
						made.put("\u0000left", Boolean.TRUE);
					}

					if (components.isEmpty()) {
						made.remove("components");
					} else {
						made.put("components", components);
					}

					return made;
				}

				return null;
			}
		} catch (Throwable var9) {
			if (!said) {
				said = true;
				CFHGuiQuests.LOGGER
					.warn("[CFHGui Quests] an item of an older version could not be brought up to this one and keeps only its id ({}); said once", var9.toString());
			}

			return null;
		}
	}

	private LegacyItems() {
	}
}
