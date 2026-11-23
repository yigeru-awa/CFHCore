package com.onehumanawa.cfhcore.content.gui.quests.client.view;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import com.onehumanawa.cfhcore.content.gui.quests.model.Quest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.advancements.AdvancementWidgetType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public final class CanvasDraw {
	private static final Map<Long, int[][]> SPANS = new HashMap<>();
	private static final Map<Long, int[]> RIMS = new HashMap<>();
	public static final int ADVANCEMENT_INK = -13949406;

	public static void batch(GuiGraphics g, Runnable drawing) {
		g.drawManaged(drawing);
	}

	public static void layer(GuiGraphics g) {
		g.flush();
		RenderSystem.clear(256, Minecraft.ON_OSX);
	}

	public static void shape(GuiGraphics g, Quest.Shape shape, int r, int color) {
		if (r > 0 && color >>> 24 != 0) {
			rows(g, spans(shape, r), r, 0, 2 * r + 1, color);
		}
	}

	public static void shape(GuiGraphics g, Quest.Shape shape, int r, int fill, int border) {
		shape(g, shape, r, border);
		shape(g, shape, r - 1, fill);
	}

	public static void shapeTop(GuiGraphics g, Quest.Shape shape, int r, int rows, int color) {
		if (r > 0 && rows > 0 && color >>> 24 != 0) {
			rows(g, spans(shape, r), r, 0, Math.min(2 * r + 1, rows), color);
		}
	}

	private static void rows(GuiGraphics g, int[][] rows, int r, int first, int end, int color) {
		int from = first;

		for (int i = first + 1; i <= end; i++) {
			if (i >= end || !same(rows[i], rows[from])) {
				int[] row = rows[from];

				for (int s = 0; s + 1 < row.length; s += 2) {
					g.fill(row[s], from - r, row[s + 1], i - r, color);
				}

				from = i;
			}
		}
	}

	public static void rim(GuiGraphics g, Quest.Shape shape, int r, float upTo, int color) {
		if (r > 1 && !(upTo <= 0.0F) && color >>> 24 != 0) {
			int[] rim = rim(shape, r);
			int n = Math.min(rim.length / 2, Math.round((float)rim.length / 2.0F * Mth.clamp(upTo, 0.0F, 1.0F)));
			run(g, rim, 0, n, color);
		}
	}

	public static void rimDashes(GuiGraphics g, Quest.Shape shape, int r, int period, int on, float phase, int color) {
		if (r > 1 && period > 0 && color >>> 24 != 0) {
			int[] rim = rim(shape, r);
			int n = rim.length / 2;
			int shift = Math.floorMod((int)phase, period);

			for (int i = 0; i < n; i++) {
				if (Math.floorMod(i - shift, period) < on) {
					int x = rim[i * 2];
					int y = rim[i * 2 + 1];
					g.fill(x, y, x + 1, y + 1, color);
				}
			}
		}
	}

	private static void run(GuiGraphics g, int[] rim, int from, int to, int color) {
		int i = from;

		while (i < to) {
			int y = rim[i * 2 + 1];
			int x0 = rim[i * 2];
			int x1 = x0;

			int j;
			for (j = i + 1; j < to && rim[j * 2 + 1] == y && Math.abs(rim[j * 2] - x1) == 1; j++) {
				x1 = rim[j * 2];
			}

			g.fill(Math.min(x0, x1), y, Math.max(x0, x1) + 1, y + 1, color);
			i = j;
		}
	}

	public static float inscribed(Quest.Shape shape) {
		return switch (shape) {
			case DIAMOND -> 0.7F;
			case STAR -> 0.48F;
			case GEAR -> 0.76F;
			case PENTAGON, SHIELD -> 0.78F;
			case HEART -> 0.62F;
			case HEXAGON -> 0.85F;
			case OCTAGON, ROUNDED -> 0.92F;
			default -> 1.0F;
		};
	}

	private static boolean same(int[] a, int[] b) {
		if (a.length != b.length) {
			return false;
		} else {
			for (int i = 0; i < a.length; i++) {
				if (a[i] != b[i]) {
					return false;
				}
			}

			return true;
		}
	}

	private static int[][] spans(Quest.Shape shape, int r) {
		return SPANS.computeIfAbsent((long)shape.ordinal() << 32 | (long)r, k -> build(shape, r));
	}

	private static int[] rim(Quest.Shape shape, int r) {
		return RIMS.computeIfAbsent((long)shape.ordinal() << 32 | (long)r, k -> buildRim(shape, r));
	}

	private static int[] buildRim(Quest.Shape shape, int r) {
		int size = 2 * r + 1;
		boolean[][] in = new boolean[size + 2][size + 2];
		int[][] rows = spans(shape, r);

		for (int y = 0; y < size; y++) {
			int[] row = rows[y];

			for (int s = 0; s + 1 < row.length; s += 2) {
				for (int x = row[s]; x < row[s + 1]; x++) {
					if (x >= -r && x <= r) {
						in[y + 1][x + r + 1] = true;
					}
				}
			}
		}

		List<int[]> pixels = new ArrayList<>();

		for (int y = 1; y <= size; y++) {
			for (int xx = 1; xx <= size; xx++) {
				if (in[y][xx]) {
					boolean edge = false;

					for (int dy = -1; dy <= 1 && !edge; dy++) {
						for (int dx = -1; dx <= 1; dx++) {
							if (!in[y + dy][xx + dx]) {
								edge = true;
								break;
							}
						}
					}

					if (edge) {
						pixels.add(new int[]{xx - r - 1, y - r - 1});
					}
				}
			}
		}

		pixels.sort((a, b) -> Double.compare(clock(a[0], a[1]), clock(b[0], b[1])));
		int[] out = new int[pixels.size() * 2];

		for (int i = 0; i < pixels.size(); i++) {
			out[i * 2] = pixels.get(i)[0];
			out[i * 2 + 1] = pixels.get(i)[1];
		}

		return out;
	}

	private static double clock(int x, int y) {
		double a = Math.atan2((double)x, (double)(-y));
		return a < 0.0 ? a + (Math.PI * 2) : a;
	}

	private static int[][] build(Quest.Shape shape, int r) {
		int[][] rows = new int[2 * r + 1][];
		switch (shape) {
			case DIAMOND:
				for (int y = -r; y <= r; y++) {
					rows[y + r] = span(r - Math.abs(y));
				}
				break;
			case STAR:
				polygon(rows, r, 10, -90.0F, 1.0F, 0.5F);
				break;
			case GEAR:
				float body = (float)r * 0.78F;

				for (int y = -r; y <= r; y++) {
					rows[y + r] = toothRow(y, r, body);
				}
				break;
			case PENTAGON:
				polygon(rows, r, 5, -90.0F, 1.0F, 1.0F);
				break;
			case SHIELD:
				int turn = Math.round((float)r * 0.15F);

				for (int y = -r; y <= r; y++) {
					rows[y + r] = span(y <= turn ? r : Math.round((float)r * (1.0F - (float)(y - turn) / (float)(r - turn))));
				}
				break;
			case HEART:
				for (int y = -r; y <= r; y++) {
					rows[y + r] = heartRow(y, r);
				}
				break;
			case HEXAGON:
				polygon(rows, r, 6, 0.0F, 1.0F, 1.0F);
				break;
			case OCTAGON:
				for (int y = -r; y <= r; y++) {
					rows[y + r] = span(Math.min(r, Math.round((float)r * 1.4142F) - Math.abs(y)));
				}
				break;
			case ROUNDED:
				int cut = Math.max(1, Math.round((float)r * 0.34F));

				for (int y = -r; y <= r; y++) {
					rows[y + r] = span(r - Math.max(0, cut - (r - Math.abs(y))));
				}
				break;
			case SQUARE:
				for (int y = -r; y <= r; y++) {
					rows[y + r] = span(r);
				}
				break;
			case NONE:
				for (int y = -r; y <= r; y++) {
					rows[y + r] = new int[0];
				}
				break;
			default:
				for (int y = -r; y <= r; y++) {
					rows[y + r] = span((int)Math.floor(Math.sqrt((double)(((float)r + 0.35F) * ((float)r + 0.35F) - (float)(y * y)))));
				}
		}

		return rows;
	}

	private static int[] toothRow(int y, int r, float body) {
		ArrayList<Integer> out = new ArrayList<>();
		int start = Integer.MIN_VALUE;

		for (int x = -r; x <= r + 1; x++) {
			boolean in = false;
			if (x <= r) {
				float d = Mth.sqrt((float)(x * x + y * y));
				if (d <= body) {
					in = true;
				} else if (d <= (float)r + 0.3F) {
					float a = (float)(Math.atan2((double)y, (double)x) / (Math.PI / 4));
					in = Math.abs(a - (float)Math.round(a)) < 0.27F;
				}
			}

			if (in && start == Integer.MIN_VALUE) {
				start = x;
			} else if (!in && start != Integer.MIN_VALUE) {
				out.add(start);
				out.add(x);
				start = Integer.MIN_VALUE;
			}
		}

		int[] row = new int[out.size()];

		for (int i = 0; i < row.length; i++) {
			row[i] = out.get(i);
		}

		return row;
	}

	private static int[] heartRow(int y, int r) {
		ArrayList<Integer> out = new ArrayList<>();
		float v = 0.12F - (float)y * 1.12F / (float)Math.max(1, r);
		int start = Integer.MIN_VALUE;

		for (int x = -r; x <= r + 1; x++) {
			boolean in = false;
			if (x <= r) {
				float u = (float)x * 1.14F / (float)Math.max(1, r);
				float d = u * u + v * v - 1.0F;
				in = d * d * d - u * u * v * v * v <= 0.0F;
			}

			if (in && start == Integer.MIN_VALUE) {
				start = x;
			} else if (!in && start != Integer.MIN_VALUE) {
				out.add(start);
				out.add(x);
				start = Integer.MIN_VALUE;
			}
		}

		int[] row = new int[out.size()];

		for (int i = 0; i < row.length; i++) {
			row[i] = out.get(i);
		}

		return row;
	}

	private static int[] span(int halfWidth) {
		return halfWidth < 0 ? new int[0] : new int[]{-halfWidth, halfWidth + 1};
	}

	private static void polygon(int[][] rows, int r, int corners, float startDeg, float outer, float inner) {
		float[] px = new float[corners];
		float[] py = new float[corners];

		for (int i = 0; i < corners; i++) {
			double a = Math.toRadians((double)startDeg + (double)i * 360.0 / (double)corners);
			float rad = ((float)r + 0.4F) * (i % 2 == 0 ? outer : inner);
			px[i] = (float)Math.cos(a) * rad;
			py[i] = (float)Math.sin(a) * rad;
		}

		for (int y = -r; y <= r; y++) {
			ArrayList<Float> hits = new ArrayList<>();

			for (int i = 0; i < corners; i++) {
				int j = (i + 1) % corners;
				float y0 = py[i];
				float y1 = py[j];
				if (y0 != y1 && !((float)y < Math.min(y0, y1)) && !((float)y >= Math.max(y0, y1))) {
					hits.add(px[i] + ((float)y - y0) / (y1 - y0) * (px[j] - px[i]));
				}
			}

			hits.sort(Float::compare);
			int[] row = new int[hits.size() / 2 * 2];

			for (int ix = 0; ix + 1 < hits.size(); ix += 2) {
				row[ix] = Math.round(hits.get(ix));
				row[ix + 1] = Math.max(row[ix] + 1, Math.round(hits.get(ix + 1)) + 1);
			}

			rows[y + r] = row;
		}
	}

	public static void ring(GuiGraphics g, float cx, float cy, float radius, int color) {
		if (!(radius < 1.0F) && color >>> 24 != 0) {
			int r = Math.round(radius);
			int x0 = Math.round(cx);
			int y0 = Math.round(cy);
			float rr = (float)r + 0.5F;
			int prev = -1;

			for (int dy = -r; dy <= 0; dy++) {
				int dx = (int)Math.floor(Math.sqrt((double)(rr * rr - (float)(dy * dy))));
				int from = prev < 0 ? 0 : Math.min(dx, prev + 1);
				int w = dx - from + 1;
				if (from == 0) {
					g.fill(x0 - dx, y0 + dy, x0 + dx + 1, y0 + dy + 1, color);
					if (dy != 0) {
						g.fill(x0 - dx, y0 - dy, x0 + dx + 1, y0 - dy + 1, color);
					}
				} else {
					g.fill(x0 - dx, y0 + dy, x0 - dx + w, y0 + dy + 1, color);
					g.fill(x0 + from, y0 + dy, x0 + dx + 1, y0 + dy + 1, color);
					if (dy != 0) {
						g.fill(x0 - dx, y0 - dy, x0 - dx + w, y0 - dy + 1, color);
						g.fill(x0 + from, y0 - dy, x0 + dx + 1, y0 - dy + 1, color);
					}
				}

				prev = dx;
			}
		}
	}

	public static void line(GuiGraphics g, float x1, float y1, float x2, float y2, float width, int color) {
		if (color >>> 24 != 0) {
			float dx = x2 - x1;
			float dy = y2 - y1;
			if (!(Math.abs(dy) < 0.01F) && !(Math.abs(dx) < 0.01F)) {
				float len = Mth.sqrt(dx * dx + dy * dy);
				if (!(len < 0.5F)) {
					g.pose().pushPose();
					g.pose().translate(x1, y1, 0.0F);
					g.pose().mulPose(Axis.ZP.rotation((float)Math.atan2((double)dy, (double)dx)));
					g.pose().scale(0.1F, 0.1F, 1.0F);
					int half = Math.max(1, Math.round(width * 5.0F));
					g.fill(0, -half, Math.round(len * 10.0F), half, color);
					g.pose().popPose();
				}
			} else {
				int w = Math.max(1, Math.round(width));
				if (Math.abs(dy) < 0.01F) {
					int y = Math.round(y1 - (float)w / 2.0F);
					g.fill(Math.round(Math.min(x1, x2)), y, Math.round(Math.max(x1, x2)), y + w, color);
				} else {
					int x = Math.round(x1 - (float)w / 2.0F);
					g.fill(x, Math.round(Math.min(y1, y2)), x + w, Math.round(Math.max(y1, y2)), color);
				}
			}
		}
	}

	public static void dashes(GuiGraphics g, float x1, float y1, float x2, float y2, float width, float dash, float gap, float phase, int color) {
		if (color >>> 24 != 0) {
			float dx = x2 - x1;
			float dy = y2 - y1;
			float len = Mth.sqrt(dx * dx + dy * dy);
			if (!(len < 0.5F) && !(dash <= 0.0F)) {
				float step = dash + Math.max(0.0F, gap);
				g.pose().pushPose();
				g.pose().translate(x1, y1, 0.0F);
				g.pose().mulPose(Axis.ZP.rotation((float)Math.atan2((double)dy, (double)dx)));
				g.pose().scale(0.1F, 0.1F, 1.0F);
				int half = Math.max(1, Math.round(width * 5.0F));

				for (float at = (phase % step + step) % step - step; at < len; at += step) {
					float a = Math.max(0.0F, at);
					float b = Math.min(len, at + dash);
					if (b > a) {
						g.fill(Math.round(a * 10.0F), -half, Math.round(b * 10.0F), half, color);
					}
				}

				g.pose().popPose();
			}
		}
	}

	public static void current(GuiGraphics g, float x1, float y1, float x2, float y2, float width, float tail, float every, float phase, int color) {
		if (color >>> 24 != 0) {
			float dx = x2 - x1;
			float dy = y2 - y1;
			float len = Mth.sqrt(dx * dx + dy * dy);
			if (!(len < 2.0F) && !(every <= 0.0F)) {
				int alpha = color >>> 24;
				g.pose().pushPose();
				g.pose().translate(x1, y1, 0.0F);
				g.pose().mulPose(Axis.ZP.rotation((float)Math.atan2((double)dy, (double)dx)));
				g.pose().scale(0.1F, 0.1F, 1.0F);
				int half = Math.max(1, Math.round(width * 5.0F));
				float piece = tail / 3.0F;

				for (float head = (phase % every + every) % every; head - tail < len; head += every) {
					for (int k = 0; k < 3; k++) {
						float b = Math.min(len, head - (float)k * piece);
						float a = Math.max(0.0F, head - (float)(k + 1) * piece);
						if (!(b <= a)) {
							int c = Math.round((float)alpha * (1.0F - (float)k * 0.34F)) << 24 | color & 16777215;
							g.fill(Math.round(a * 10.0F), -half, Math.round(b * 10.0F), half, c);
						}
					}
				}

				g.pose().popPose();
			}
		}
	}

	public static void arrow(GuiGraphics g, float x, float y, float dx, float dy, float size, float width, int color) {
		if (color >>> 24 != 0) {
			float len = Mth.sqrt(dx * dx + dy * dy);
			if (!(len < 0.001F)) {
				g.pose().pushPose();
				g.pose().translate(x, y, 0.0F);
				g.pose().mulPose(Axis.ZP.rotation((float)Math.atan2((double)dy, (double)dx)));
				g.pose().scale(0.1F, 0.1F, 1.0F);
				int steps = Math.max(2, Math.round(size));

				for (int i = 0; i < steps; i++) {
					int half = Math.round(((float)i + 0.5F) * width * 5.0F * 0.9F);
					g.fill(Math.round((float)(-(i + 1)) * 10.0F), -half, Math.round((float)(-i) * 10.0F), half, color);
				}

				g.pose().popPose();
			}
		}
	}

	public static void elbow(GuiGraphics g, float x1, float y1, float x2, float y2, float width, int color) {
		float mid = (x1 + x2) / 2.0F;
		line(g, x1, y1, mid, y1, width, color);
		line(g, mid, y1, mid, y2, width, color);
		line(g, mid, y2, x2, y2, width, color);
	}

	public static void advancementFrame(GuiGraphics g, Quest.Frame frame, boolean done, float light, float alpha) {
		if (!(alpha <= 0.004F)) {
			AdvancementType type = switch (frame) {
				case GOAL -> AdvancementType.GOAL;
				case CHALLENGE -> AdvancementType.CHALLENGE;
				default -> AdvancementType.TASK;
			};
			ResourceLocation sprite = (done ? AdvancementWidgetType.OBTAINED : AdvancementWidgetType.UNOBTAINED).frameSprite(type);
			g.setColor(light, light, light, alpha);
			RenderSystem.enableBlend();
			g.blitSprite(sprite, -13, -13, 26, 26);
			g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
			RenderSystem.disableBlend();
		}
	}

	private CanvasDraw() {
	}
}
