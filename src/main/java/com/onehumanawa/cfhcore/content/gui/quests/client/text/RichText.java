package com.onehumanawa.cfhcore.content.gui.quests.client.text;

import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.quests.client.QuestsState;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.ItemSpecs;
import com.onehumanawa.cfhcore.content.gui.quests.logic.BookIndex;
import java.io.DataInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class RichText {
	private static final int PARA = 0;
	private static final int HEADING = 1;
	private static final int SUBHEADING = 2;
	private static final int BULLET = 3;
	private static final int RULE = 4;
	private static final int IMAGE = 5;
	private static final int GAP = 6;
	private static final int LINE = 10;
	private static final int ICON = 10;
	private static final int BULLET_INDENT = 9;
	private final List<RichText.Block> blocks = new ArrayList<>();
	private final Map<Integer, RichText.Layout> layouts = new HashMap<>();
	private static final Map<ResourceLocation, int[]> SIZES = new HashMap<>();

	private RichText() {
	}

	public boolean isEmpty() {
		return this.blocks.isEmpty();
	}

	public static RichText of(@Nullable List<String> lines) {
		RichText text = new RichText();
		if (lines == null) {
			return text;
		} else {
			for (String raw : lines) {
				if (raw != null) {
					for (String line : translate(raw).split("\n", -1)) {
						text.blocks.add(block(line));
					}
				}
			}

			while (!text.blocks.isEmpty() && text.blocks.get(0).type == 6) {
				text.blocks.remove(0);
			}

			while (!text.blocks.isEmpty() && text.blocks.get(text.blocks.size() - 1).type == 6) {
				text.blocks.remove(text.blocks.size() - 1);
			}

			return text;
		}
	}

	private static String translate(String line) {
		String s = line.trim();
		if (s.length() > 2 && s.charAt(0) == '{' && s.charAt(s.length() - 1) == '}' && s.indexOf(32) < 0 && s.indexOf(58) < 0 && s.indexOf(125) == s.length() - 1) {
			String key = s.substring(1, s.length() - 1);
			if (I18n.exists(key)) {
				return I18n.get(key, new Object[0]);
			}
		}

		return line;
	}

	private static RichText.Block block(String raw) {
		RichText.Block b = new RichText.Block();
		String s = raw.strip();
		if (s.isEmpty()) {
			b.type = 6;
			return b;
		} else if (s.length() < 3 || !s.chars().allMatch(c -> c == 45) && !s.chars().allMatch(c -> c == 95) && !s.chars().allMatch(c -> c == 42)) {
			if (s.startsWith("![")) {
				int close = s.indexOf("](");
				int end = close < 0 ? -1 : s.indexOf(41, close + 2);
				ResourceLocation id = end < 0 ? null : ResourceLocation.tryParse(s.substring(close + 2, end).trim());
				if (id != null) {
					b.type = 5;
					b.image = id;
					int[] size = imageSize(id);
					b.imageW = size[0];
					b.imageH = size[1];
					int brace = s.indexOf(123, end);
					if (brace >= 0 && s.endsWith("}")) {
						int w = 0;
						int h = 0;

						for (String part : s.substring(brace + 1, s.length() - 1).split("[ ,]+")) {
							try {
								if (part.startsWith("w=")) {
									w = Integer.parseInt(part.substring(2));
								} else if (part.startsWith("h=")) {
									h = Integer.parseInt(part.substring(2));
								}
							} catch (NumberFormatException var15) {
							}
						}

						if (w > 0) {
							b.imageH = h > 0 ? h : Math.max(1, Math.round((float)w * ((float)b.imageH / (float)Math.max(1, b.imageW))));
							b.imageW = w;
						} else if (h > 0) {
							b.imageW = Math.max(1, Math.round((float)h * ((float)b.imageW / (float)Math.max(1, b.imageH))));
							b.imageH = h;
						}
					}

					return b;
				}
			}

			String rest = s;
			if (s.startsWith("## ") || s.startsWith("### ")) {
				b.type = 2;
				rest = s.substring(s.indexOf(32) + 1);
			} else if (s.startsWith("# ")) {
				b.type = 1;
				rest = s.substring(2);
			} else if (s.startsWith("- ") || s.startsWith("* ") || s.startsWith("• ")) {
				b.type = 3;
				rest = s.substring(2);
			}

			RichText.Reader r = new RichText.Reader(b.atoms, b.type != 1 && b.type != 2 ? Style.EMPTY : Theme.current().headingStyle());
			r.read(rest.strip());
			r.word();
			return b;
		} else {
			b.type = 4;
			return b;
		}
	}

	private static int[] imageSize(ResourceLocation id) {
		return SIZES.computeIfAbsent(id, k -> {
			try {
				int[] var5;
				try (
					InputStream in = Minecraft.getInstance().getResourceManager().open(k);
					DataInputStream data = new DataInputStream(in);
				) {
					data.skipBytes(16);
					int w = data.readInt();
					int h = data.readInt();
					if (w <= 0 || h <= 0 || w >= 8192 || h >= 8192) {
						return new int[]{16, 16};
					}

					var5 = new int[]{w, h};
				}

				return var5;
			} catch (Exception var10) {
				return new int[]{16, 16};
			}
		});
	}

	public RichText.Layout layout(int width) {
		return this.layouts.computeIfAbsent(Math.max(24, width), x$0 -> new RichText.Layout(this, x$0));
	}

	private static final class Atom {
		String text = "";
		Style style = Style.EMPTY;
		boolean space;
		boolean code;
		@Nullable
		String url;
		@Nullable
		String quest;
		@Nullable
		String item;
		@Nullable
		ItemStack icon;
		int width;

		boolean sameRun(RichText.Atom o) {
			return this.icon == null
				&& o.icon == null
				&& this.code == o.code
				&& this.style.equals(o.style)
				&& Objects.equals(this.url, o.url)
				&& Objects.equals(this.quest, o.quest)
				&& Objects.equals(this.item, o.item);
		}
	}

	private static final class Block {
		int type = 0;
		final List<RichText.Atom> atoms = new ArrayList<>();
		@Nullable
		ResourceLocation image;
		int imageW;
		int imageH;
	}

	public interface Host {
		void quest(String var1);

		void url(String var1);
	}

	public final class Layout {
		private final int width;
		private final List<RichText.Run> runs;
		private int height;
		@Nullable
		private RichText.Run hovered;

		Layout(final RichText owner, final int width) {
			this.width = width;
			this.runs = new java.util.ArrayList<RichText.Run>();
			Font font = CFHGuiDraw.font();
			int y = 0;
			int idx = 0;
			for (int i = 0; i < owner.blocks.size(); i++) {
				RichText.Block b = owner.blocks.get(i);
				switch (b.type) {
					case 4:
						y += 4;
						break;
					case 6:
						RichText.Run rule = new RichText.Run();
						rule.rule = true;
						rule.x = 0;
						rule.y = y + 3;
						rule.w = width;
						rule.h = 1;
						this.runs.add(rule);
						y += 9;
						break;
					case 5:
						RichText.Run image = new RichText.Run();
						image.image = b.image;
						image.w = Math.max(1, Math.round(Math.max(1.0F, (float)width) * b.imageW * 1.0F));
						image.h = Math.max(1, Math.round(Math.max(1.0F, (float)width) * b.imageH * 1.0F));
						image.x = (width - image.w) / 2;
						image.y = y + 1;
						this.runs.add(image);
						y += image.h + 5;
						break;
					case 1:
						if (y > 0) {
							y += 3;
						}
						int headH = b.type == 1 ? 13 : (b.type == 2 ? 12 : 10);
						int bulletX = b.type == 3 ? 9 : 0;
						if (b.type == 3) {
							RichText.Run bullet = new RichText.Run();
							bullet.bullet = true;
							bullet.x = 2;
							bullet.y = y + 3;
							bullet.w = 3;
							bullet.h = 3;
							this.runs.add(bullet);
						}

						y = this.wrap(font, b, bulletX, y, headH);
						y += b.type == 1 ? 3 : 4;
						break;
					default:
						int textH = b.type == 1 ? 13 : (b.type == 2 ? 12 : 10);
						if (b.type == 3) {
							RichText.Run bullet = new RichText.Run();
							bullet.bullet = true;
							bullet.x = 2;
							bullet.y = y + 3;
							bullet.w = 3;
							bullet.h = 3;
							this.runs.add(bullet);
						}

						y = this.wrap(font, b, b.type == 3 ? 9 : 0, y, textH);
						y += b.type == 1 ? 3 : 4;
				}
			}

			this.height = Math.max(0, y - 4);
		}

		private int wrap(Font font, RichText.Block b, int indent, int top, int lineH) {
			int y = top;
			int x = indent;
			RichText.Run run = null;
			RichText.Atom last = null;
			StringBuilder text = null;

			for (RichText.Atom a : b.atoms) {
				if (a.width == 0) {
					a.width = a.icon != null ? 10 : font.width(Component.literal(a.text).withStyle(a.style));
				}

				int spaceW = a.space && x > indent ? font.width(Component.literal(" ").withStyle(a.style)) : 0;
				if (x > indent && x + spaceW + a.width > this.width) {
					if (run != null) {
						this.close(font, run, text, last);
					}

					run = null;
					y += lineH;
					x = indent;
				}

				boolean lead = x == indent;
				if (run != null && !lead && last != null && a.sameRun(last)) {
					if (a.space) {
						text.append(' ');
					}

					text.append(a.text);
					x += (a.space ? spaceW : 0) + a.width;
					last = a;
				} else {
					if (run != null) {
						this.close(font, run, text, last);
					}

					run = new RichText.Run();
					run.x = x + (lead ? 0 : spaceW);
					run.y = y + (lineH - 10);
					run.h = 10;
					run.code = a.code;
					run.url = a.url;
					run.quest = a.quest;
					run.item = a.item;
					if (a.icon != null) {
						run.icon = a.icon;
						run.w = 10;
						this.runs.add(run);
						x = run.x + 10;
						run = null;
						last = a;
					} else {
						text = new StringBuilder(a.text);
						x = run.x + a.width;
						last = a;
					}
				}
			}

			if (run != null) {
				this.close(font, run, text, last);
			}

			return y + lineH;
		}

		private void close(Font font, RichText.Run run, StringBuilder text, RichText.Atom style) {
			Component c = Component.literal(text.toString()).withStyle(style.style);
			run.text = c.getVisualOrderText();
			run.w = font.width(c);
			this.runs.add(run);
		}

		public int height() {
			return this.height;
		}

		public int width() {
			return this.width;
		}

		public void draw(GuiGraphics g, int x, int y, int mouseX, int mouseY, float alpha) {
			Theme t = Theme.current();
			Palette p = t.palette();
			boolean van = t.isVanilla();
			Font font = CFHGuiDraw.font();
			int body = van ? -2039584 : p.text();
			int link = van ? -8406785 : p.accentInk();
			int codeC = van ? -8054 : Colors.mix(p.text(), p.warning(), 0.55F);
			this.hovered = null;

			for (RichText.Run r : this.runs) {
				int rx = x + r.x;
				int ry = y + r.y;
				boolean over = r.interactive() && mouseX >= rx - 1 && mouseX < rx + r.w + 1 && mouseY >= ry - 1 && mouseY < ry + r.h;
				if (over) {
					this.hovered = r;
				}
			}

			RichText.Run hot = this.hovered;

			for (RichText.Run rx : this.runs) {
				int rxx = x + rx.x;
				int ry = y + rx.y;
				if (rx.rule) {
					g.fill(rxx, ry, rxx + rx.w, ry + 1, Colors.scaleAlpha(van ? -9474193 : p.border(), alpha));
				} else if (rx.bullet) {
					g.fill(rxx, ry, rxx + rx.w, ry + rx.h, Colors.scaleAlpha(van ? -6250336 : p.accent(), alpha));
				} else if (rx.image != null) {
					CFHGuiDraw.blit(g, rx.image, rxx, ry, rx.w, rx.h, 0.0F, 0.0F, 16, 16, 16, 16, alpha);
				} else {
					boolean lit = hot != null
						&& rx.interactive()
						&& Objects.equals(rx.url, hot.url)
						&& Objects.equals(rx.quest, hot.quest)
						&& Objects.equals(rx.item, hot.item);
					if (rx.icon != null) {
						if (alpha > 0.6F) {
							g.pose().pushPose();
							g.pose().translate((float)rxx, (float)(ry - 1), 0.0F);
							g.pose().scale(0.625F, 0.625F, 1.0F);
							g.renderFakeItem(rx.icon, 0, 0);
							g.pose().popPose();
						}
					} else if (rx.text != null) {
						int color = body;
						if (rx.code) {
							color = codeC;
							g.fill(rxx - 2, ry - 1, rxx + rx.w + 2, ry + 9, Colors.scaleAlpha(van ? 1610612736 : Colors.withAlpha(p.surfaceActive(), 192), alpha));
						}

						if (rx.url == null && rx.quest == null) {
							if (rx.item != null) {
								color = lit ? (van ? -96 : Colors.brighten(p.text(), 0.4F)) : (van ? -1 : Colors.brighten(p.text(), 0.15F));
							}
						} else {
							color = lit ? Colors.brighten(link, 0.3F) : link;
							g.fill(rxx, ry + 9, rxx + rx.w, ry + 10, Colors.scaleAlpha(Colors.withAlpha(color, lit ? 255 : 112), alpha));
						}

						g.drawString(font, rx.text, rxx, ry, Colors.scaleAlpha(color, Math.max(0.02F, alpha)), van);
					}
				}
			}
		}

		@Nullable
		public RichText.Run hovered() {
			return this.hovered;
		}

		@Nullable
		public RichText.Run at(double x, double y) {
			for (RichText.Run r : this.runs) {
				if (r.interactive() && x >= (double)(r.x - 1) && x < (double)(r.x + r.w + 1) && y >= (double)(r.y - 1) && y < (double)(r.y + r.h)) {
					return r;
				}
			}

			return null;
		}

		public boolean click(double x, double y, RichText.Host host) {
			RichText.Run r = this.at(x, y);
			if (r == null) {
				return false;
			} else if (r.quest != null) {
				host.quest(r.quest);
				return true;
			} else if (r.url != null) {
				host.url(r.url);
				return true;
			} else {
				return false;
			}
		}
	}

	private static final class Reader {
		final List<RichText.Atom> out;
		final Style base;
		final StringBuilder word = new StringBuilder();
		boolean bold;
		boolean italic;
		boolean underline;
		boolean strike;
		boolean code;
		boolean space;
		@Nullable
		ChatFormatting color;
		@Nullable
		String url;
		@Nullable
		String quest;
		@Nullable
		String item;

		Reader(List<RichText.Atom> out, Style base) {
			this.out = out;
			this.base = base;
		}

		Style style() {
			Style s = this.base;
			if (this.bold) {
				s = s.withBold(true);
			}

			if (this.italic) {
				s = s.withItalic(true);
			}

			if (this.underline) {
				s = s.withUnderlined(true);
			}

			if (this.strike) {
				s = s.withStrikethrough(true);
			}

			if (this.color != null) {
				s = s.withColor(this.color);
			}

			return s;
		}

		void word() {
			if (this.word.length() != 0) {
				RichText.Atom a = new RichText.Atom();
				a.text = this.word.toString();
				a.style = this.style();
				a.space = this.space;
				a.code = this.code;
				a.url = this.url;
				a.quest = this.quest;
				a.item = this.item;
				this.out.add(a);
				this.word.setLength(0);
				this.space = false;
			}
		}

		void plain(String s) {
			for (int i = 0; i < s.length(); i++) {
				char c = s.charAt(i);
				if (Character.isWhitespace(c)) {
					this.word();
					this.space = true;
				} else {
					this.word.append(c);
				}
			}
		}

		void read(String s) {
			for (int i = 0; i < s.length(); i++) {
				char c = s.charAt(i);
				char next = i + 1 < s.length() ? s.charAt(i + 1) : 0;
				if (c == '\\' && next != 0) {
					this.word.append(next);
					i++;
				} else if (c == '`') {
					this.word();
					this.code = !this.code;
				} else if (this.code) {
					if (Character.isWhitespace(c)) {
						this.word();
						this.space = true;
					} else {
						this.word.append(c);
					}
				} else if (Character.isWhitespace(c)) {
					this.word();
					this.space = true;
				} else if (c == '*' && next == '*') {
					this.word();
					this.bold = !this.bold;
					i++;
				} else if (c == '_' && next == '_') {
					this.word();
					this.underline = !this.underline;
					i++;
				} else if (c == '~' && next == '~') {
					this.word();
					this.strike = !this.strike;
					i++;
				} else {
					if (c == '*') {
						char before = i > 0 ? s.charAt(i - 1) : 32;
						boolean opens = !this.italic && next != 0 && !Character.isWhitespace(next);
						boolean closes = this.italic && !Character.isWhitespace(before);
						if (opens || closes) {
							this.word();
							this.italic = !this.italic;
							continue;
						}
					}

					if (c == '&' && next != 0) {
						if (next == '&') {
							this.word.append('&');
							i++;
							continue;
						}

						ChatFormatting f = ChatFormatting.getByCode(Character.toLowerCase(next));
						if (f != null) {
							this.word();
							i++;
							if (f == ChatFormatting.RESET) {
								this.bold = this.italic = this.underline = this.strike = false;
								this.color = null;
							} else if (f.isColor()) {
								this.bold = this.italic = this.underline = this.strike = false;
								this.color = f;
							} else if (f == ChatFormatting.BOLD) {
								this.bold = true;
							} else if (f == ChatFormatting.ITALIC) {
								this.italic = true;
							} else if (f == ChatFormatting.UNDERLINE) {
								this.underline = true;
							} else if (f == ChatFormatting.STRIKETHROUGH) {
								this.strike = true;
							}
							continue;
						}
					}

					if (c == '[') {
						int close = s.indexOf("](", i + 1);
						int end = close < 0 ? -1 : s.indexOf(41, close + 2);
						if (end > 0) {
							this.word();
							String target = s.substring(close + 2, end).trim();
							String hadUrl = this.url;
							String hadQuest = this.quest;
							if (target.startsWith("quest:")) {
								this.quest = target.substring(6).trim();
							} else {
								this.url = target;
							}

							this.read(s.substring(i + 1, close));
							this.word();
							this.url = hadUrl;
							this.quest = hadQuest;
							i = end;
							continue;
						}
					}

					if (c == '{') {
						int end = s.indexOf(125, i + 1);
						if (end > 0 && this.braces(s.substring(i + 1, end).trim())) {
							i = end;
							continue;
						}
					}

					this.word.append(c);
				}
			}
		}

		private boolean braces(String inside) {
			if (inside.startsWith("item:")) {
				String spec = inside.substring(5).trim();
				ItemStack stack = ItemSpecs.stack(spec);
				this.word();
				String had = this.item;
				this.item = spec;
				if (!stack.isEmpty()) {
					RichText.Atom a = new RichText.Atom();
					a.icon = stack;
					a.space = this.space;
					a.item = spec;
					this.out.add(a);
					this.space = true;
				}

				boolean wasBold = this.bold;
				this.plain(ItemSpecs.name(spec).getString());
				this.word();
				this.bold = wasBold;
				this.item = had;
				return true;
			} else if (inside.startsWith("quest:")) {
				String key = inside.substring(6).trim();
				BookIndex.Entry e = QuestsState.entry(key);
				this.word();
				String had = this.quest;
				this.quest = key;
				this.plain(e == null ? key : QuestText.title(e).getString());
				this.word();
				this.quest = had;
				return true;
			} else if (!inside.isEmpty() && inside.indexOf(32) < 0 && I18n.exists(inside)) {
				this.plain(I18n.get(inside, new Object[0]));
				return true;
			} else {
				return false;
			}
		}
	}

	public static final class Run {
		int x;
		int y;
		int w;
		int h;
		@Nullable
		FormattedCharSequence text;
		@Nullable
		ItemStack icon;
		@Nullable
		ResourceLocation image;
		boolean code;
		boolean rule;
		boolean bullet;
		@Nullable
		String url;
		@Nullable
		String quest;
		@Nullable
		String item;

		@Nullable
		public String url() {
			return this.url;
		}

		@Nullable
		public String quest() {
			return this.quest;
		}

		@Nullable
		public String item() {
			return this.item;
		}

		public boolean interactive() {
			return this.url != null || this.quest != null || this.item != null;
		}
	}
}
