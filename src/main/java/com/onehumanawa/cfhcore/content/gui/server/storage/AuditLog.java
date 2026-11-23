package com.onehumanawa.cfhcore.content.gui.server.storage;

import com.onehumanawa.cfhcore.content.gui.core.net.JsonWire;
import com.onehumanawa.cfhcore.content.gui.server.CFHGuiServer;
import com.onehumanawa.cfhcore.content.gui.server.model.AdminData;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import org.jetbrains.annotations.Nullable;

public final class AuditLog {
	public static final int KEPT = 20000;
	@Nullable
	private final Path file;
	private final Saver saver;
	private final ArrayDeque<AdminData.AuditEntry> entries = new ArrayDeque<>();

	public AuditLog(@Nullable Path file, Saver saver) {
		this.file = file;
		this.saver = saver;
	}

	public void load(int keepDays, long now) {
		this.entries.clear();
		if (this.file != null && Files.isRegularFile(this.file)) {
			long oldest = keepDays <= 0 ? Long.MIN_VALUE : now - (long)keepDays * 86400000L;
			List<String> kept = new ArrayList<>();
			int dropped = 0;
			int broken = 0;

			try (BufferedReader r = Files.newBufferedReader(this.file, StandardCharsets.UTF_8)) {
				for (String line = r.readLine(); line != null; line = r.readLine()) {
					if (!line.isBlank()) {
						AdminData.AuditEntry e = null;

						try {
							e = (AdminData.AuditEntry)JsonWire.GSON.fromJson(line, AdminData.AuditEntry.class);
						} catch (RuntimeException var15) {
						}

						if (e == null) {
							broken++;
						} else if (e.at < oldest) {
							dropped++;
						} else {
							e.sanitize();
							kept.add(line);
							this.entries.addLast(e);
							if (this.entries.size() > 20000) {
								this.entries.removeFirst();
							}
						}
					}
				}
			} catch (IOException var17) {
				CFHGuiServer.LOGGER.error("[CFHGui Server] {} could not be read; the audit log starts empty in memory and is added to as it is", this.file, var17);
				return;
			}

			if (broken > 0) {
				try {
					Files.copy(this.file, this.file.resolveSibling(this.file.getFileName() + ".broken"), StandardCopyOption.REPLACE_EXISTING);
				} catch (IOException var14) {
				}

				CFHGuiServer.LOGGER.error("[CFHGui Server] {} lines of {} could not be read; the file as it was is kept as audit.jsonl.broken", broken, this.file);
			}

			if (broken > 0 || dropped > 0) {
				StringBuilder sb = new StringBuilder(kept.size() * 160);

				for (String linex : kept) {
					sb.append(linex).append('\n');
				}

				this.saver.text(this.file, sb.toString());
			}
		}
	}

	public void add(AdminData.AuditEntry entry) {
		this.entries.addLast(entry);
		if (this.entries.size() > 20000) {
			this.entries.removeFirst();
		}

		if (this.file != null) {
			String line = JsonWire.GSON.toJson(entry) + "\n";
			Path target = this.file;
			this.saver.run(() -> {
				try {
					Files.createDirectories(target.getParent());
					Files.writeString(target, line, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
				} catch (IOException var3x) {
					CFHGuiServer.LOGGER.error("[CFHGui Server] could not add to {}", target, var3x);
				}
			});
		}
	}

	public int size() {
		return this.entries.size();
	}

	@Nullable
	public AdminData.AuditEntry last() {
		return this.entries.peekLast();
	}

	public AdminData.AuditPage page(@Nullable String query, int page) {
		String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
		AdminData.AuditPage out = new AdminData.AuditPage();
		out.page = Math.max(0, page);
		int from = out.page * 50;
		int to = from + 50;
		int n = 0;
		Iterator<AdminData.AuditEntry> it = this.entries.descendingIterator();

		while (it.hasNext()) {
			AdminData.AuditEntry e = it.next();
			if (q.isEmpty() || matches(e, q)) {
				if (n >= from && n < to) {
					out.rows.add(e);
				}

				n++;
			}
		}

		out.total = n;
		return out;
	}

	private static boolean matches(AdminData.AuditEntry e, String q) {
		return e.actorName.toLowerCase(Locale.ROOT).contains(q)
			|| e.targetName.toLowerCase(Locale.ROOT).contains(q)
			|| e.action.toLowerCase(Locale.ROOT).contains(q)
			|| e.detail.toLowerCase(Locale.ROOT).contains(q);
	}
}
