package com.onehumanawa.cfhcore.content.gui.profile.client;

import com.onehumanawa.cfhcore.content.gui.core.client.DevHarness;
import com.onehumanawa.cfhcore.content.gui.core.config.JsonConfig;
import com.onehumanawa.cfhcore.content.gui.core.platform.CFHGuiPlatform;
import com.onehumanawa.cfhcore.content.gui.profile.CFHGuiProfile;
import com.onehumanawa.cfhcore.content.gui.profile.Slot;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

public final class ProfileStore {
	private static final Map<UUID, ProfileStore.Account> OPEN = new HashMap<>();
	@Nullable
	private static JsonConfig<ProfileStore.AccountsFile> accounts;

	public static Path root() {
		if (DevHarness.sampleData()) {
			return CFHGuiPlatform.get().gameDir().resolve("cfhgui-sample-profile");
		} else {
			String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
			String home = System.getProperty("user.home", ".");
			if (!os.contains("win")) {
				return os.contains("mac") ? Path.of(home, "Library", "Application Support", "cfh", "profile") : Path.of(home, ".cfh", "profile");
			} else {
				String appData = System.getenv("APPDATA");
				return Path.of(appData != null && !appData.isBlank() ? appData : home, ".cfh", "profile");
			}
		}
	}

	private static synchronized JsonConfig<ProfileStore.AccountsFile> accountsFile() {
		if (accounts == null) {
			accounts = JsonConfig.at(root().resolve("accounts.json"), ProfileStore.AccountsFile.class, ProfileStore.AccountsFile::new);
		}

		return accounts;
	}

	public static List<ProfileStore.KnownAccount> known() {
		List<ProfileStore.KnownAccount> out = new ArrayList<>(((ProfileStore.AccountsFile)accountsFile().get()).accounts);
		out.sort((a, b) -> Long.compare(b.lastSeen, a.lastSeen));
		return out;
	}

	public static void seen(UUID uuid, String name, boolean microsoft) {
		accountsFile().update(f -> {
			ProfileStore.KnownAccount k = null;

			for (ProfileStore.KnownAccount a : f.accounts) {
				if (a.uuid.equalsIgnoreCase(uuid.toString())) {
					k = a;
				}
			}

			if (k == null) {
				k = new ProfileStore.KnownAccount();
				k.uuid = uuid.toString();
				f.accounts.add(k);
			}

			k.name = name;
			k.lastSeen = System.currentTimeMillis();
			k.microsoft = microsoft;
		});
	}

	public static synchronized ProfileStore.Account account(UUID uuid, String name) {
		return OPEN.computeIfAbsent(uuid, u -> new ProfileStore.Account(u, name));
	}

	private ProfileStore() {
	}

	public static final class Account {
		private final UUID uuid;
		private final Path folder;
		private final JsonConfig<ProfileStore.AccountFile> file;

		private Account(UUID uuid, String name) {
			this.uuid = uuid;
			this.folder = ProfileStore.root().resolve(uuid.toString());
			this.file = JsonConfig.at(this.folder.resolve("profile.json"), ProfileStore.AccountFile.class, ProfileStore.AccountFile::new);
			if (((ProfileStore.AccountFile)this.file.get()).uuid.isEmpty() || !name.isEmpty() && !name.equals(((ProfileStore.AccountFile)this.file.get()).name)) {
				this.file.update(f -> {
					f.uuid = uuid.toString();
					if (!name.isEmpty()) {
						f.name = name;
					}
				});
			}
		}

		public UUID uuid() {
			return this.uuid;
		}

		public String name() {
			return ((ProfileStore.AccountFile)this.file.get()).name;
		}

		public Path folder() {
			return this.folder;
		}

		public ProfileStore.AccountFile data() {
			return (ProfileStore.AccountFile)this.file.get();
		}

		public void save() {
			this.file.save();
		}

		public String bio() {
			return ((ProfileStore.AccountFile)this.file.get()).bio == null ? "" : ((ProfileStore.AccountFile)this.file.get()).bio;
		}

		public void bio(String text) {
			this.file.update(f -> f.bio = text == null ? "" : text);
		}

		public List<ProfileStore.Look> looks() {
			List<ProfileStore.Look> out = new ArrayList<>();

			for (ProfileStore.LookEntry e : ((ProfileStore.AccountFile)this.file.get()).looks) {
				out.add(new ProfileStore.Look(this, e));
			}

			return out;
		}

		@Nullable
		public ProfileStore.Look look(@Nullable String id) {
			if (id != null && !id.isEmpty()) {
				for (ProfileStore.LookEntry e : ((ProfileStore.AccountFile)this.file.get()).looks) {
					if (e.id.equals(id)) {
						return new ProfileStore.Look(this, e);
					}
				}

				return null;
			} else {
				return null;
			}
		}

		@Nullable
		public ProfileStore.Look worn() {
			return this.look(((ProfileStore.AccountFile)this.file.get()).worn);
		}

		public void wear(@Nullable ProfileStore.Look look) {
			this.file.update(f -> f.worn = look == null ? "" : look.id());
		}

		public ProfileStore.Look create(String name, String skin, boolean slim) {
			ProfileStore.LookEntry e = new ProfileStore.LookEntry();
			e.id = Long.toString(System.currentTimeMillis(), 36) + Integer.toString((int)(Math.random() * 1296.0), 36);
			e.name = name;
			e.skin = skin;
			e.slim = slim;
			this.file.update(f -> f.looks.add(e));
			return new ProfileStore.Look(this, e);
		}

		public ProfileStore.Look duplicate(ProfileStore.Look of, String name) {
			ProfileStore.Look copy = this.create(name, of.entry().skin, of.slim());
			copy.entry().items.putAll(of.entry().items);
			if (of.skinFile() != null && Files.isRegularFile(of.skinFile())) {
				try {
					Files.createDirectories(this.folder.resolve("looks"));
					Files.copy(of.skinFile(), this.folder.resolve("looks").resolve(copy.id() + ".png"), StandardCopyOption.REPLACE_EXISTING);
					copy.entry().skin = "file";
				} catch (IOException var5) {
					CFHGuiProfile.LOGGER.warn("[CFHGui Profile] cannot copy the skin of {}: {}", of.name(), var5.toString());
				}
			}

			this.save();
			return copy;
		}

		public void remove(ProfileStore.Look look) {
			this.file.update(f -> {
				f.looks.removeIf(e -> e.id.equals(look.id()));
				if (f.worn.equals(look.id())) {
					f.worn = "";
				}
			});

			try {
				Files.deleteIfExists(this.folder.resolve("looks").resolve(look.id() + ".png"));
			} catch (IOException var3) {
			}
		}

		@Nullable
		public Path picture() {
			String p = ((ProfileStore.AccountFile)this.file.get()).picture;
			if (p != null && !p.isEmpty()) {
				Path path = this.folder.resolve(p);
				return Files.isRegularFile(path) ? path : null;
			} else {
				return null;
			}
		}

		public void picture(@Nullable Path source) {
			if (source == null) {
				this.file.update(f -> f.picture = "");
			} else {
				try {
					Files.createDirectories(this.folder);
					String name = source.getFileName().toString().toLowerCase(Locale.ROOT);
					String kept = "picture" + (!name.endsWith(".jpg") && !name.endsWith(".jpeg") ? ".png" : ".jpg");
					Files.copy(source, this.folder.resolve(kept), StandardCopyOption.REPLACE_EXISTING);
					this.file.update(f -> f.picture = kept);
				} catch (IOException var4) {
					CFHGuiProfile.LOGGER.warn("[CFHGui Profile] cannot keep the picture {}: {}", source, var4.toString());
				}
			}
		}
	}

	public static final class AccountFile {
		public int format = 1;
		public String uuid = "";
		public String name = "";
		public String bio = "";
		public String picture = "";
		public String worn = "";
		public List<ProfileStore.LookEntry> looks = new ArrayList<>();
	}

	public static final class AccountsFile {
		public List<ProfileStore.KnownAccount> accounts = new ArrayList<>();
	}

	public static final class KnownAccount {
		public String uuid = "";
		public String name = "";
		public long lastSeen;
		public boolean microsoft;
	}

	public static final class Look {
		public static final List<String> DEFAULTS = List.of("steve", "alex", "ari", "efe", "kai", "makena", "noor", "sunny", "zuri");
		private final ProfileStore.Account account;
		private final ProfileStore.LookEntry entry;

		private Look(ProfileStore.Account account, ProfileStore.LookEntry entry) {
			this.account = account;
			this.entry = entry;
			if (entry.items == null) {
				entry.items = new LinkedHashMap<>();
			}
		}

		ProfileStore.LookEntry entry() {
			return this.entry;
		}

		public ProfileStore.Account account() {
			return this.account;
		}

		public String id() {
			return this.entry.id;
		}

		public String name() {
			return this.entry.name == null ? "" : this.entry.name;
		}

		public void name(String name) {
			this.entry.name = name;
			this.account.save();
		}

		public boolean slim() {
			return this.entry.slim;
		}

		public void slim(boolean slim) {
			this.entry.slim = slim;
			this.account.save();
		}

		public String skin() {
			return this.entry.skin != null && !this.entry.skin.isEmpty() ? this.entry.skin : "account";
		}

		public void skin(String source) {
			this.entry.skin = source;
			this.account.save();
		}

		public Path skinFile() {
			return this.account.folder().resolve("looks").resolve(this.entry.id + ".png");
		}

		public boolean upload(Path png) {
			try {
				Files.createDirectories(this.skinFile().getParent());
				Files.copy(png, this.skinFile(), StandardCopyOption.REPLACE_EXISTING);
				this.entry.skin = "file";
				this.account.save();
				return true;
			} catch (IOException var3) {
				CFHGuiProfile.LOGGER.warn("[CFHGui Profile] cannot keep the skin {}: {}", png, var3.toString());
				return false;
			}
		}

		@Nullable
		public Cosmetic in(Slot slot) {
			Cosmetic c = Cosmetics.get(this.entry.items.get(slot.key()));
			return c != null && c.slot() == slot ? c : null;
		}

		public void put(Slot slot, @Nullable Cosmetic cosmetic) {
			if (cosmetic == null) {
				this.entry.items.remove(slot.key());
			} else {
				this.entry.items.put(slot.key(), cosmetic.id());
			}

			this.account.save();
		}

		public void hold(Slot slot, @Nullable Cosmetic cosmetic) {
			if (cosmetic == null) {
				this.entry.items.remove(slot.key());
			} else {
				this.entry.items.put(slot.key(), cosmetic.id());
			}
		}

		public List<Cosmetic> items() {
			List<Cosmetic> out = new ArrayList<>();

			for (Slot s : Slot.values()) {
				Cosmetic c = this.in(s);
				if (c != null) {
					out.add(c);
				}
			}

			return out;
		}

		public String signature() {
			StringBuilder b = new StringBuilder(this.entry.id).append('|').append(this.skin()).append('|').append(this.entry.slim);

			for (Slot s : Slot.values()) {
				b.append('|').append(this.entry.items.getOrDefault(s.key(), ""));
			}

			if ("file".equals(this.skin())) {
				try {
					b.append('|').append(Files.getLastModifiedTime(this.skinFile()).toMillis());
				} catch (IOException var6) {
				}
			}

			return b.toString();
		}
	}

	public static final class LookEntry {
		public String id = "";
		public String name = "";
		public boolean slim = false;
		public String skin = "account";
		public Map<String, String> items = new LinkedHashMap<>();
	}
}
