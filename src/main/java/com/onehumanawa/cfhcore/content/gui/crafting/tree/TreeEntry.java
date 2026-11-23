package com.onehumanawa.cfhcore.content.gui.crafting.tree;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.onehumanawa.cfhcore.content.gui.emi.api.stack.EmiStack;
import com.onehumanawa.cfhcore.content.gui.emi.bom.BoM;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public final class TreeEntry {
	public final String id;
	public boolean saved;
	public boolean pinned;
	public long lastUsed;
	@Nullable
	public JsonObject json;
	@Nullable
	TreeSession session;
	private int readAt = -1;
	private boolean unreadable;
	int written = -1;
	int looked = -1;

	TreeEntry(String id) {
		this.id = id;
	}

	@Nullable
	public TreeSession session() {
		if (this.session != null && this.readAt == BoM.generation) {
			return this.session;
		} else {
			if (this.session != null) {
				this.write();
			}

			if (this.json == null) {
				this.readAt = BoM.generation;
				return this.session;
			} else if (this.unreadable && this.readAt == BoM.generation) {
				return null;
			} else {
				TreeSession old = this.session;
				this.session = TreeCodec.read(this.json);
				this.readAt = BoM.generation;
				this.unreadable = this.session == null;
				this.looked = -1;
				if (this.session != null) {
					this.written = this.session.changes();
					if (old != null) {
						this.session.selected = old.selected;
						this.session.foldMode = old.foldMode;
						this.session.firstPoint = old.firstPoint;
					}
				}

				return this.session;
			}
		}
	}

	void adopt(TreeSession live) {
		this.session = live;
		this.readAt = BoM.generation;
		this.unreadable = false;
		this.written = -1;
		this.looked = -1;
	}

	public boolean unreadable() {
		return this.session() == null;
	}

	boolean write() {
		TreeSession s = this.session;
		if (s != null && this.written != s.changes()) {
			this.written = s.changes();
			JsonObject now = TreeCodec.write(s.tree, s.marks);
			if (now == null && this.json == null) {
				return false;
			} else {
				this.json = now;
				return true;
			}
		} else {
			return false;
		}
	}

	@Nullable
	public EmiStack icon() {
		TreeSession s = this.session;
		if (s != null) {
			return s.tree.goal.ingredient.getEmiStacks().get(0);
		} else {
			return this.json == null ? null : TreeCodec.goal(this.json);
		}
	}

	public Component name() {
		TreeSession s = this.session;
		if (s != null) {
			return s.displayName();
		} else {
			JsonElement name = this.json == null ? null : this.json.get("name");
			if (name != null && name.isJsonPrimitive() && !name.getAsString().isBlank()) {
				return Component.literal(name.getAsString());
			} else {
				EmiStack icon = this.icon();
				return (Component)(icon != null ? icon.getName() : Component.translatable("cfhgui_crafting.tree.unknown"));
			}
		}
	}

	public boolean writable() {
		return this.json != null;
	}
}
