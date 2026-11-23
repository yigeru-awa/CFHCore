package com.onehumanawa.cfhcore.content.gui.quests.server.adv;

import com.onehumanawa.cfhcore.content.gui.core.platform.ModInfo;
import com.onehumanawa.cfhcore.content.gui.core.platform.CFHGuiPlatform;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component.Serializer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;

public final class AdvTrees {
	private static final List<String> GAME = List.of("story/root", "nether/root", "end/root", "adventure/root", "husbandry/root");

	public static List<AdvancementNode> roots(MinecraftServer server) {
		List<AdvancementNode> out = new ArrayList<>();

		for (AdvancementNode root : server.getAdvancements().tree().roots()) {
			if (root.advancement().display().isPresent()) {
				out.add(root);
			}
		}

		out.sort(Comparator.<AdvancementNode>comparingInt(n -> rank(n.holder().id())).thenComparing(n -> n.holder().id().toString()));
		return out;
	}

	private static int rank(ResourceLocation id) {
		if (!id.getNamespace().equals("minecraft")) {
			return 100;
		} else {
			int i = GAME.indexOf(id.getPath());
			return i < 0 ? 50 : i;
		}
	}

	public static List<AdvancementNode> find(MinecraftServer server, String word) {
		String w = word.trim().toLowerCase(Locale.ROOT);
		List<AdvancementNode> all = roots(server);
		List<AdvancementNode> out = new ArrayList<>();
		if (w.isEmpty()) {
			return out;
		} else if (w.equals("*")) {
			return all;
		} else {
			for (AdvancementNode root : all) {
				ResourceLocation id = root.holder().id();
				if (id.toString().equals(w) || id.getNamespace().equals("minecraft") && id.getPath().equals(w)) {
					return List.of(root);
				}
			}

			for (AdvancementNode rootx : all) {
				if (rootx.holder().id().getNamespace().equals(w)) {
					out.add(rootx);
				}
			}

			return out;
		}
	}

	public static String modName(String namespace) {
		try {
			String name = CFHGuiPlatform.get().modInfo(namespace).<String>map(ModInfo::name).orElse("");
			if (!name.isBlank()) {
				return name;
			}
		} catch (RuntimeException var6) {
		}

		StringBuilder sb = new StringBuilder();

		for (String part : namespace.split("[_.-]")) {
			if (!part.isEmpty()) {
				if (sb.length() > 0) {
					sb.append(' ');
				}

				sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
			}
		}

		return sb.length() == 0 ? namespace : sb.toString();
	}

	static AdvTrees.Tree describe(MinecraftServer server, AdvancementNode root) {
		ResourceLocation id = root.holder().id();
		DisplayInfo display = (DisplayInfo)root.advancement().display().orElseThrow();
		AdvTrees.Tree t = new AdvTrees.Tree();
		t.root = id.toString();
		t.namespace = id.getNamespace();
		t.mod = modName(id.getNamespace());
		t.title = Serializer.toJson(display.getTitle(), server.registryAccess());
		t.description = Serializer.toJson(display.getDescription(), server.registryAccess());
		t.icon = display.getIcon().isEmpty() ? "" : BuiltInRegistries.ITEM.getKey(display.getIcon().getItem()).toString();
		t.chapter = AdvConverter.chapterId(id);
		Deque<AdvancementNode> nodes = new ArrayDeque<>();
		nodes.push(root);

		while (!nodes.isEmpty()) {
			AdvancementNode node = nodes.pop();
			DisplayInfo d = (DisplayInfo)node.advancement().display().orElse(null);
			if (d != null) {
				t.count++;
				if (d.isHidden()) {
					t.hidden++;
				}
			}

			for (AdvancementNode child : node.children()) {
				nodes.push(child);
			}
		}

		return t;
	}

	private AdvTrees() {
	}

	public static final class Tree {
		public String root = "";
		public String namespace = "";
		public String mod = "";
		public String title = "";
		public String description = "";
		public String icon = "";
		public int count;
		public int hidden;
		public String chapter = "";
		public boolean exists;
		public String converted = "";
		@Nullable
		public AdvOptions options;
	}
}
