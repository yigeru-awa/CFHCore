package com.onehumanawa.cfhcore.content.gui.profile.client;

import com.onehumanawa.cfhcore.content.gui.profile.Slot;

final class SampleLooks {
	static void ensure(ProfileStore.Account account) {
		if (account.data().looks.isEmpty()) {
			ProfileStore.Look traveller = look(account, "Traveller", "ari", false, "hat_straw", "back_pack", "pants_jeans", "shirt_striped", "leg_stocking_left");
			look(account, "Tinkerer", "kai", false, "face_glasses", "arm_iron_right", "pet_bee", "pants_shorts");
			look(account, "Royal", "makena", true, "hat_crown", "shirt_hoodie", "back_wings");
			look(account, "Old salt", "steve", false, "hat_top", "leg_wood_right", "arm_bandage_left", "face_bandana");
			look(account, "Swamp friend", "sunny", true, "pet_slime", "pants_jeans", "leg_stocking_right");
			account.bio("Builds by day, digs by night. Will trade cake for redstone.");
			account.wear(traveller);
		}
	}

	private static ProfileStore.Look look(ProfileStore.Account account, String name, String skin, boolean slim, String... items) {
		ProfileStore.Look look = account.create(name, "default:" + skin, slim);

		for (String id : items) {
			Cosmetic c = Cosmetics.get(id);
			if (c != null) {
				look.put(c.slot(), c);
			}
		}

		return look;
	}

	static String none(Slot slot) {
		return "cfhgui_profile.slot." + slot.key() + ".none";
	}

	private SampleLooks() {
	}
}
