package com.onehumanawa.cfhcore.content.gui.server.client.ui.kit;

import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiTextField;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.network.chat.Component;

public final class CommitField extends CFHGuiTextField {
	private final Predicate<String> valid;
	private final Consumer<String> commit;
	private String committed;

	public CommitField(int width, Component narration, String value, Predicate<String> valid, Consumer<String> commit) {
		super(0, 0, width, 18, narration);
		this.valid = valid;
		this.commit = commit;
		this.committed = value == null ? "" : value;
		this.setMaxLength(256);
		this.setValue(this.committed);
		this.setResponder(s -> this.setInvalid(!valid.test(s.trim())));
		this.onEnter(this::commit);
	}

	public static CommitField number(int width, Component narration, long value, long min, long max, Consumer<Long> commit) {
		CommitField f = new CommitField(width, narration, Long.toString(value), s -> {
			try {
				long v = Long.parseLong(s);
				return v >= min && v <= max;
			} catch (NumberFormatException var7) {
				return false;
			}
		}, s -> commit.accept(Long.parseLong(s)));
		f.setFilter(s -> s.matches("-?[0-9]{0,12}"));
		f.setMaxLength(13);
		return f;
	}

	public void sync(String value) {
		boolean untouched = this.getValue().trim().equals(this.committed);
		this.committed = value == null ? "" : value;
		if (!this.isFocused() || untouched) {
			this.setValue(this.committed);
			this.setInvalid(false);
		}
	}

	private void commit() {
		String v = this.getValue().trim();
		if (!v.equals(this.committed)) {
			if (this.valid.test(v)) {
				this.committed = v;
				this.commit.accept(v);
			}
		}
	}

	public void setFocused(boolean focused) {
		boolean was = this.isFocused();
		super.setFocused(focused);
		if (was && !focused) {
			if (this.valid.test(this.getValue().trim())) {
				this.commit();
			} else {
				this.setValue(this.committed);
				this.setInvalid(false);
			}
		}
	}
}
