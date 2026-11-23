package com.onehumanawa.cfhcore.content.gui.server.storage;

import com.onehumanawa.cfhcore.content.gui.server.CFHGuiServer;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

public final class Saver {
	private final ExecutorService io = Executors.newSingleThreadExecutor(r -> {
		Thread t = new Thread(r, "CFHGui Server IO");
		t.setDaemon(true);
		return t;
	});

	public void text(Path file, String content) {
		this.write(file, tmp -> Files.writeString(tmp, content, StandardCharsets.UTF_8));
	}

	public void write(Path file, Saver.Write write) {
		this.submit(() -> {
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
				CFHGuiServer.LOGGER.error("[CFHGui Server] could not write {}", file, var5);
			}
		});
	}

	public void run(Runnable work) {
		this.submit(() -> {
			try {
				work.run();
			} catch (RuntimeException var2) {
				CFHGuiServer.LOGGER.error("[CFHGui Server] background work failed", var2);
			}
		});
	}

	public void delete(Path file) {
		this.submit(() -> {
			try {
				Files.deleteIfExists(file);
			} catch (IOException var2) {
				CFHGuiServer.LOGGER.warn("[CFHGui Server] could not remove {}: {}", file, var2.toString());
			}
		});
	}

	private void submit(Runnable task) {
		try {
			this.io.execute(task);
		} catch (RejectedExecutionException var3) {
			CFHGuiServer.LOGGER.debug("[CFHGui Server] disk work after the last save was dropped");
		}
	}

	public boolean await() {
		CountDownLatch done = new CountDownLatch(1);
		this.submit(done::countDown);

		try {
			return done.await(10L, TimeUnit.SECONDS);
		} catch (InterruptedException var3) {
			Thread.currentThread().interrupt();
			return false;
		}
	}

	public void close() {
		this.io.shutdown();

		try {
			if (!this.io.awaitTermination(30L, TimeUnit.SECONDS)) {
				CFHGuiServer.LOGGER.error("[CFHGui Server] the last save did not finish in time");
			}
		} catch (InterruptedException var2) {
			Thread.currentThread().interrupt();
		}
	}

	@FunctionalInterface
	public interface Write {
		void to(Path var1) throws IOException;
	}
}
