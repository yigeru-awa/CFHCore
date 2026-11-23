package com.onehumanawa.cfhcore.content.gui.quests.compat;

import com.onehumanawa.cfhcore.content.gui.quests.CFHGuiQuests;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

public final class FtbPreflight {
	private static final String WRITTEN_AGAINST = "FTB Quests 2101.1.33";
	private static final String[][] CLIENT = new String[][]{
		{"dev/ftb/mods/ftbquests/client/ClientQuestFile", "openQuestGui", "canEdit"},
		{"dev/ftb/mods/ftbquests/client/FTBQuestsNetClient", "notifyPlayerOfCompletion"}
	};
	private static final String[][] BOTH = new String[][]{{"dev/ftb/mods/ftbquests/quest/TeamData", "setProgress", "isLocked", "claimReward"}};
	private static boolean ran;

	public static synchronized void run(boolean client) {
		if (!ran) {
			ran = true;

			try {
				if (members(BOTH[0][0]) == null && members(CLIENT[0][0]) == null) {
					return;
				}

				List<String> missing = new ArrayList<>();
				missing(BOTH, missing);
				if (client) {
					missing(CLIENT, missing);
				}

				if (missing.isEmpty()) {
					CFHGuiQuests.LOGGER
						.info(
							"[CFHGui Quests] FTB Quests is installed beside CFHGui Quests: with a book converted from it, its key and buttons open CFHGui's book and its own counting rests"
						);
					return;
				}

				CFHGuiQuests.LOGGER
					.warn(
						"[CFHGui Quests] FTB Quests here is not the version CFHGui Quests' hooks were written against ({}): there is no {}. What hangs on that is left out: FTB Quests' own book and counting go on as that mod has them.",
						"FTB Quests 2101.1.33",
						String.join(", ", missing)
					);
			} catch (Throwable var2) {
				CFHGuiQuests.LOGGER.debug("[CFHGui Quests] could not look through FTB Quests' classes: {}", var2.toString());
			}
		}
	}

	private static void missing(String[][] expected, List<String> out) throws Exception {
		for (String[] entry : expected) {
			String simple = entry[0].substring(entry[0].lastIndexOf(47) + 1);
			Set<String> members = members(entry[0]);
			if (members == null) {
				out.add("class " + simple);
			} else {
				for (int i = 1; i < entry.length; i++) {
					if (!members.contains(entry[i])) {
						out.add(simple + "." + entry[i]);
					}
				}
			}
		}
	}

	private static Set<String> members(String name) throws Exception {
		Object var3;
		try (InputStream in = FtbPreflight.class.getClassLoader().getResourceAsStream(name + ".class")) {
			if (in == null) {
				return null;
			}

			final Set<String> names = new HashSet<>();
			new ClassReader(in).accept(new ClassVisitor(589824) {
				public MethodVisitor visitMethod(int access, String method, String descriptor, String signature, String[] exceptions) {
					names.add(method);
					return null;
				}
			}, 7);
			var3 = names;
		}

		return (Set<String>)var3;
	}

	private FtbPreflight() {
	}
}
