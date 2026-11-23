package com.onehumanawa.cfhcore.content.gui.teams.server;

import com.onehumanawa.cfhcore.content.gui.teams.CFHGuiTeams;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

final class Saver {
	private final ExecutorService io = Executors.newSingleThreadExecutor(r -> {
		Thread t = new Thread(r, "CFHGui Teams IO");
		t.setDaemon(true);
		return t;
	});

	void text(Path file, String content) {
		this.write(file, tmp -> Files.writeString(tmp, content, StandardCharsets.UTF_8));
	}

	void write(Path file, Saver.Write write) {
		this.io.execute(() -> {
			try {
				Files.createDirectories(file.getParent());
				Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
				write.to(tmp);

				try {
					Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
				} catch (AtomicMoveNotSupportedException var4) {
					Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
				}
			} catch (RuntimeException | IOException var5) {
				CFHGuiTeams.LOGGER.error("[CFHGui Teams] could not write {}", file, var5);
			}
		});
	}

	void delete(Path file) {
		this.io.execute(() -> {
			try {
				Files.deleteIfExists(file);
			} catch (IOException var2) {
				CFHGuiTeams.LOGGER.warn("[CFHGui Teams] could not remove {}: {}", file, var2.toString());
			}
		});
	}

	void close() {
		this.io.shutdown();

		try {
			if (!this.io.awaitTermination(30L, TimeUnit.SECONDS)) {
				CFHGuiTeams.LOGGER.error("[CFHGui Teams] the last save did not finish in time");
			}
		} catch (InterruptedException var2) {
			Thread.currentThread().interrupt();
		}
	}

	@FunctionalInterface
	interface Write {
		void to(Path var1) throws IOException;
	}
}
