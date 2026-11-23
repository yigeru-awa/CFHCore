package com.onehumanawa.cfhcore.content.gui.quests.server;

import com.onehumanawa.cfhcore.content.gui.quests.logic.BookIndex;
import com.onehumanawa.cfhcore.content.gui.quests.model.Chapter;
import com.onehumanawa.cfhcore.content.gui.quests.model.Quest;
import com.onehumanawa.cfhcore.content.gui.quests.model.QuestBook;
import com.onehumanawa.cfhcore.content.gui.quests.model.Task;
import java.util.function.Function;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.HoverEvent.Action;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

final class ServerText {
	static MutableComponent msg(String key, String english, Object... args) {
		return Component.translatableWithFallback(key, english, args);
	}

	static MutableComponent text(@Nullable String s) {
		if (s != null && !s.isEmpty()) {
			return s.length() > 2 && s.charAt(0) == '{' && s.charAt(s.length() - 1) == '}' && s.indexOf(32) < 0
				? Component.translatable(s.substring(1, s.length() - 1))
				: Component.literal(codes(s));
		} else {
			return Component.empty();
		}
	}

	static String codes(String s) {
		if (s.indexOf(38) < 0) {
			return s;
		} else {
			StringBuilder sb = new StringBuilder(s.length());

			for (int i = 0; i < s.length(); i++) {
				char c = s.charAt(i);
				if (c == '&' && i + 1 < s.length()) {
					char n = Character.toLowerCase(s.charAt(i + 1));
					if (n == '&') {
						sb.append('&');
						i++;
						continue;
					}

					if ("0123456789abcdefklmnor".indexOf(n) >= 0) {
						sb.append('§').append(n);
						i++;
						continue;
					}
				}

				sb.append(c);
			}

			return sb.toString();
		}
	}

	static String plain(@Nullable String s) {
		if (s == null) {
			return "";
		} else {
			String coded = codes(s);
			StringBuilder sb = new StringBuilder(coded.length());

			for (int i = 0; i < coded.length(); i++) {
				if (coded.charAt(i) == 167 && i + 1 < coded.length()) {
					i++;
				} else {
					sb.append(coded.charAt(i));
				}
			}

			return sb.toString();
		}
	}

	static Component title(Chapter chapter) {
		return text(chapter.title.isBlank() ? chapter.id : chapter.title);
	}

	static Component title(QuestBook book, Quest quest) {
		if (!quest.title.isBlank()) {
			return text(quest.title);
		} else {
			if (!quest.tasks.isEmpty()) {
				Component subject = subject(quest.tasks.get(0));
				if (subject != null) {
					return subject;
				}
			}

			if (quest.isGateway()) {
				Chapter sub = book.chapter(quest.subtree);
				if (sub != null) {
					return title(sub);
				}
			}

			return msg("cfhgui_quests.quest.untitled", "Quest");
		}
	}

	static String plainTitle(BookIndex.Entry entry) {
		String title = plain(entry.quest().title);
		return title.isBlank() ? entry.quest().id : title;
	}

	@Nullable
	private static Component subject(Task task) {
		if (!task.item.isBlank()) {
			return thing(task.item, id -> BuiltInRegistries.ITEM.getOptional(id).<Component>map(Item::getDescription).orElse(null));
		} else if (!task.entity.isBlank()) {
			return thing(task.entity, id -> BuiltInRegistries.ENTITY_TYPE.getOptional(id).<Component>map(EntityType::getDescription).orElse(null));
		} else {
			return !task.block.isBlank() ? thing(task.block, id -> BuiltInRegistries.BLOCK.getOptional(id).<Component>map(Block::getName).orElse(null)) : null;
		}
	}

	private static Component thing(String spec, Function<ResourceLocation, Component> name) {
		String s = spec.trim();
		int bracket = s.indexOf(91);
		if (s.startsWith("*")) {
			return msg("cfhgui_quests.task.item.name", "Item");
		} else if (s.startsWith("#")) {
			return msg("cfhgui_quests.any", "Any %s", readable(bracket < 0 ? s.substring(1) : s.substring(1, bracket)));
		} else {
			ResourceLocation id = ResourceLocation.tryParse(bracket < 0 ? s : s.substring(0, bracket));
			Component named = id == null ? null : name.apply(id);
			return (Component)(named != null ? named : Component.literal(readable(s)));
		}
	}

	static String readable(String id) {
		String path = id.indexOf(58) >= 0 ? id.substring(id.indexOf(58) + 1) : id;
		if (path.indexOf(47) >= 0) {
			path = path.substring(path.lastIndexOf(47) + 1);
		}

		StringBuilder sb = new StringBuilder();

		for (String word : path.split("[_.]")) {
			if (!word.isEmpty()) {
				if (sb.length() > 0) {
					sb.append(' ');
				}

				sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
			}
		}

		return sb.toString();
	}

	static Component badge(QuestBook book, Quest quest, ChatFormatting color) {
		Component title = title(book, quest);
		MutableComponent hover = ComponentUtils.mergeStyles(title.copy(), Style.EMPTY.withColor(color));
		String about = !quest.subtitle.isBlank() ? quest.subtitle : (quest.description.isEmpty() ? "" : unmarked(quest.description.get(0)));
		if (!about.isBlank()) {
			hover.append("\n").append(text(about).withStyle(ChatFormatting.WHITE));
		}

		Component name = title.copy().withStyle(style -> style.withHoverEvent(new HoverEvent(Action.SHOW_TEXT, hover)));
		return ComponentUtils.wrapInSquareBrackets(name).withStyle(color);
	}

	private static String unmarked(String line) {
		String s = line.replace("**", "").replace("__", "").replace("~~", "").replace("`", "");
		if (s.startsWith("# ")) {
			s = s.substring(2);
		} else if (s.startsWith("- ")) {
			s = s.substring(2);
		}

		return s.trim();
	}

	private ServerText() {
	}
}
