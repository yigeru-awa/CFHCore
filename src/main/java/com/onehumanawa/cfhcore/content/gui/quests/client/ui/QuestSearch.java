package com.onehumanawa.cfhcore.content.gui.quests.client.ui;

import com.onehumanawa.cfhcore.content.gui.quests.client.QuestsState;
import com.onehumanawa.cfhcore.content.gui.quests.client.text.QuestText;
import com.onehumanawa.cfhcore.content.gui.quests.logic.BookIndex;
import com.onehumanawa.cfhcore.content.gui.quests.logic.Rules;
import com.onehumanawa.cfhcore.content.gui.quests.model.Quest;
import com.onehumanawa.cfhcore.content.gui.quests.model.Reward;
import com.onehumanawa.cfhcore.content.gui.quests.model.Task;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.jetbrains.annotations.Nullable;

public final class QuestSearch {
	@Nullable
	private static BookIndex indexed;
	private static final List<QuestSearch.Doc> DOCS = new ArrayList<>();

	private static void index() {
		BookIndex index = QuestsState.index();
		if (index != indexed) {
			indexed = index;
			DOCS.clear();
			if (index != null) {
				for (BookIndex.Entry e : index.all()) {
					Quest q = e.quest();
					StringBuilder rest = new StringBuilder();
					rest.append(q.subtitle).append(' ');

					for (String line : q.description) {
						rest.append(line).append(' ');
					}

					for (String tag : q.tags) {
						rest.append(tag).append(' ');
					}

					for (Task t : q.tasks) {
						rest.append(QuestText.label(t).getString()).append(' ');
					}

					for (Reward r : q.rewards) {
						rest.append(QuestText.label(r).getString()).append(' ');

						for (Reward c : r.choices) {
							rest.append(QuestText.label(c).getString()).append(' ');
						}
					}

					rest.append(QuestText.title(e.chapter()).getString()).append(' ').append(q.id);
					DOCS.add(new QuestSearch.Doc(e, QuestText.title(e).getString().toLowerCase(Locale.ROOT), rest.toString().toLowerCase(Locale.ROOT)));
				}
			}
		}
	}

	public static List<QuestSearch.Hit> find(String query, int limit) {
		List<QuestSearch.Hit> out = new ArrayList<>();
		String q = query.trim().toLowerCase(Locale.ROOT);
		if (q.isEmpty()) {
			return out;
		} else {
			index();
			String[] words = q.split("\\s+");

			for (QuestSearch.Doc d : DOCS) {
				int score = 0;
				boolean all = true;

				for (String w : words) {
					int at = d.title.indexOf(w);
					if (at == 0) {
						score += 100;
					} else if (at > 0) {
						score += d.title.charAt(at - 1) == ' ' ? 80 : 60;
					} else {
						if (!d.rest.contains(w)) {
							all = false;
							break;
						}

						score += 20;
					}
				}

				if (all) {
					Rules.Status status = QuestsState.status(d.entry.key());
					if (status != Rules.Status.HIDDEN && (status != Rules.Status.LOCKED || d.entry.quest().visibility != Quest.Visibility.SECRET)) {
						if (status.open()) {
							score += 6;
						} else if (status == Rules.Status.CLAIMABLE) {
							score += 4;
						} else if (status == Rules.Status.LOCKED) {
							score -= 4;
						}

						if (d.title.equals(q)) {
							score += 50;
						}

						out.add(new QuestSearch.Hit(d.entry, score));
					}
				}
			}

			out.sort((a, b) -> b.score() != a.score() ? Integer.compare(b.score(), a.score()) : a.entry().key().compareTo(b.entry().key()));
			return (List<QuestSearch.Hit>)(out.size() > limit ? new ArrayList<>(out.subList(0, limit)) : out);
		}
	}

	private QuestSearch() {
	}

	private static record Doc(BookIndex.Entry entry, String title, String rest) {
	}

	public static record Hit(BookIndex.Entry entry, int score) {
	}
}
