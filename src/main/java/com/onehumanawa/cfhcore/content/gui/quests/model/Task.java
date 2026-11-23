package com.onehumanawa.cfhcore.content.gui.quests.model;

import java.util.Locale;

public final class Task {
	public static final String ITEM = "item";
	public static final String CRAFT = "craft";
	public static final String KILL = "kill";
	public static final String BREAK = "break";
	public static final String PLACE = "place";
	public static final String ADVANCEMENT = "advancement";
	public static final String STAT = "stat";
	public static final String XP = "xp";
	public static final String LOCATION = "location";
	public static final String OBSERVE = "observe";
	public static final String CHECKMARK = "checkmark";
	public static final String QUEST = "quest";
	public static final String CHAPTER = "chapter";
	public static final String TEAM = "team";
	public static final String CUSTOM = "custom";
	public static final String FLUID = "fluid";
	public static final String ENERGY = "energy";
	public static final String STAGE = "stage";
	public static final String COUNTER = "counter";
	public String type = "";
	public String title = "";
	public String icon = "";
	public long count = 1L;
	public boolean optional;
	public String item = "";
	public boolean consume;
	public String entity = "";
	public String name = "";
	public String block = "";
	public String advancement = "";
	public String criterion = "";
	public String stat = "";
	public String dimension = "";
	public String biome = "";
	public String structure = "";
	public int x;
	public int y;
	public int z;
	public int radius;
	public int sizeX;
	public int sizeY;
	public int sizeZ;
	public boolean points;
	public int seconds = 1;
	public String quest = "";
	public String chapter = "";
	public String fluid = "";
	public String value = "";

	public boolean is(String kind) {
		return kind.equals(this.type);
	}

	public boolean manual() {
		return this.is("checkmark") || this.is("energy") || this.consume && (this.is("item") || this.is("xp") || this.is("fluid"));
	}

	public boolean box() {
		return this.sizeX > 0 && this.sizeY > 0 && this.sizeZ > 0;
	}

	public void sanitize() {
		if (this.title == null) {
			this.title = "";
		}

		if (this.icon == null) {
			this.icon = "";
		}

		if (this.item == null) {
			this.item = "";
		}

		if (this.entity == null) {
			this.entity = "";
		}

		if (this.name == null) {
			this.name = "";
		}

		if (this.block == null) {
			this.block = "";
		}

		if (this.advancement == null) {
			this.advancement = "";
		}

		if (this.criterion == null) {
			this.criterion = "";
		}

		if (this.stat == null) {
			this.stat = "";
		}

		if (this.dimension == null) {
			this.dimension = "";
		}

		if (this.biome == null) {
			this.biome = "";
		}

		if (this.structure == null) {
			this.structure = "";
		}

		if (this.quest == null) {
			this.quest = "";
		}

		if (this.chapter == null) {
			this.chapter = "";
		}

		if (this.fluid == null) {
			this.fluid = "";
		}

		if (this.value == null) {
			this.value = "";
		}

		if (this.type == null || this.type.isBlank()) {
			this.type = !this.item.isEmpty()
				? "item"
				: (
					!this.entity.isEmpty()
						? "kill"
						: (
							!this.advancement.isEmpty()
								? "advancement"
								: (
									!this.stat.isEmpty()
										? "stat"
										: (
											!this.block.isEmpty()
												? "break"
												: (
													!this.quest.isEmpty()
														? "quest"
														: (
															!this.chapter.isEmpty()
																? "chapter"
																: (
																	!this.fluid.isEmpty()
																		? "fluid"
																		: (this.dimension.isEmpty() && this.biome.isEmpty() && this.structure.isEmpty() && this.radius <= 0 && !this.box() ? "checkmark" : "location")
																)
														)
												)
										)
								)
						)
				);
		}

		this.type = this.type.trim().toLowerCase(Locale.ROOT);
		if (this.count < 1L) {
			this.count = 1L;
		}

		if (this.seconds < 0) {
			this.seconds = 0;
		}

		if (this.sizeX < 0) {
			this.sizeX = 0;
		}

		if (this.sizeY < 0) {
			this.sizeY = 0;
		}

		if (this.sizeZ < 0) {
			this.sizeZ = 0;
		}

		if (this.is("advancement") || this.is("location") || this.is("checkmark") || this.is("quest") || this.is("chapter") || this.is("stage")) {
			this.count = 1L;
		}

		if (this.is("observe")) {
			this.count = Math.max(1L, (long)this.seconds * 20L);
		}
	}
}
