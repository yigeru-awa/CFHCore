package com.onehumanawa.cfhcore.content.gui.shops.storage;

import com.onehumanawa.cfhcore.content.gui.shops.CFHGuiShops;
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
		Thread t = new Thread(r, "CFHGui Shops IO");
		t.setDaemon(true);
		return t;
	});

	public void text(Path file, String content) {
		this.submit(() -> {
			try {
				Files.createDirectories(file.getParent());
				Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
				Files.writeString(tmp, content, StandardCharsets.UTF_8);

				try {
					Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
				} catch (AtomicMoveNotSupportedException var4) {
					Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
				}
			} catch (RuntimeException | IOException var5) {
				CFHGuiShops.LOGGER.error("[CFHGui Shops] could not write {}", file, var5);
			}
		});
	}

	private void submit(Runnable task) {
		try {
			this.io.execute(task);
		} catch (RejectedExecutionException var3) {
			CFHGuiShops.LOGGER.debug("[CFHGui Shops] disk work after the last save was dropped");
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
				CFHGuiShops.LOGGER.error("[CFHGui Shops] the last save did not finish in time");
			}
		} catch (InterruptedException var2) {
			Thread.currentThread().interrupt();
		}
	}
}
