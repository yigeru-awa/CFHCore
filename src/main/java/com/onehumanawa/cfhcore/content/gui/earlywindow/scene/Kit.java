package com.onehumanawa.cfhcore.content.gui.earlywindow.scene;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

final class Kit {
	static final Sheet.Region SKIP = new Sheet.Region(0, 0, 0, 0, false);
	private static final String[] FACES = new String[]{"down", "up", "north", "south", "west", "east"};
	private static final int[][][] CORNERS = new int[][][]{
		{{0, 0, 1}, {0, 0, 0}, {1, 0, 0}, {1, 0, 1}},
		{{0, 1, 0}, {0, 1, 1}, {1, 1, 1}, {1, 1, 0}},
		{{1, 1, 0}, {1, 0, 0}, {0, 0, 0}, {0, 1, 0}},
		{{0, 1, 1}, {0, 0, 1}, {1, 0, 1}, {1, 1, 1}},
		{{0, 1, 0}, {0, 0, 0}, {0, 0, 1}, {0, 1, 1}},
		{{1, 1, 1}, {1, 0, 1}, {1, 0, 0}, {1, 1, 0}}
	};
	private final Assets assets;
	private final Sheet sheet;
	private final Map<String, Kit.Model> models = new HashMap<>();
	private final Map<String, Sheet.Region> textures = new HashMap<>();
	private final Map<String, int[]> pictures = new HashMap<>();
	private final float[] scratch = new float[8];
	int stoodIn;

	Kit(Assets assets, Sheet sheet) {
		this.assets = assets;
		this.sheet = sheet;
	}

	Sheet.Region texture(String id) {
		Sheet.Region known = this.textures.get(id);
		if (known != null) {
			return known;
		} else {
			String[] name = split(id);
			int[] argb = null;
			int w = 16;
			int h = 16;
			Picture picture = Picture.read(this.assets.read(name[0], "textures/" + name[1] + ".png"));
			if (picture != null) {
				argb = picture.argb();
				w = picture.w();
				h = picture.h();
				if (h > w && h % w == 0 && this.assets.read(name[0], "textures/" + name[1] + ".png.mcmeta") != null) {
					h = w;
					argb = Arrays.copyOf(argb, w * w);
				}
			}

			if (argb == null) {
				this.stoodIn++;
				h = 16;
				w = 16;
				argb = standIn(name[1]);
			}

			Sheet.Region region = this.sheet.add(argb, w, h);
			if (region == null) {
				throw new IllegalStateException("no room on the sheet for " + id);
			} else {
				this.textures.put(id, region);
				this.pictures.put(id, argb);
				return region;
			}
		}
	}

	Sheet.Region windowed(String id, int x0, int y0, int x1, int y1) {
		String key = id + " window " + x0 + " " + y0 + " " + x1 + " " + y1;
		Sheet.Region known = this.textures.get(key);
		if (known != null) {
			return known;
		} else {
			Sheet.Region whole = this.texture(id);
			int[] px = (int[])this.pictures.get(id).clone();

			for (int y = Math.max(0, y0); y < Math.min(whole.h(), y1); y++) {
				for (int x = Math.max(0, x0); x < Math.min(whole.w(), x1); x++) {
					px[y * whole.w() + x] = 0;
				}
			}

			Sheet.Region region = this.sheet.add(px, whole.w(), whole.h());
			if (region == null) {
				throw new IllegalStateException("no room on the sheet for " + key);
			} else {
				this.textures.put(key, region);
				return region;
			}
		}
	}

	int tone(String id) {
		this.texture(id);
		long r = 0L;
		long g = 0L;
		long b = 0L;
		long n = 0L;

		for (int c : this.pictures.get(id)) {
			if (c >>> 24 >= 128) {
				r += (long)(c >>> 16 & 0xFF);
				g += (long)(c >>> 8 & 0xFF);
				b += (long)(c & 0xFF);
				n++;
			}
		}

		return n == 0L ? -5197648 : 0xFF000000 | (int)(r / n) << 16 | (int)(g / n) << 8 | (int)(b / n);
	}

	boolean has(String id) {
		String[] name = split(id);
		return this.assets.read(name[0], "textures/" + name[1] + ".png") != null;
	}

	private static int[] standIn(String path) {
		int base = path.contains("dark_oak")
			? -12834282
			: (path.contains("spruce") ? -9744593 : (!path.contains("log") && !path.contains("planks") ? -8355712 : -7706046));
		int[] px = new int[256];

		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				px[y * 16 + x] = Atlas.shade(base, 0.9F + Atlas.hash(x / 2, y, 41) * 0.2F);
			}
		}

		return px;
	}

	private static String[] split(String id) {
		int colon = id.indexOf(58);
		return colon < 0 ? new String[]{"minecraft", id} : new String[]{id.substring(0, colon), id.substring(colon + 1)};
	}

	Kit.Model model(String id) {
		Kit.Model known = this.models.get(id);
		if (known != null) {
			return known;
		} else {
			Kit.Model model = new Kit.Model();
			Map<String, String> names = new LinkedHashMap<>();
			List<Object> elements = null;
			String at = id;

			for (int depth = 0; at != null && depth < 8; depth++) {
				String[] name = split(at);
				byte[] file = this.assets.read(name[0], "models/" + name[1] + ".json");
				if (file == null) {
					if (depth == 0) {
						throw new IllegalStateException("no model " + id);
					}
					break;
				}

				Map<String, Object> json = Json.object(Json.parse(new String(file, StandardCharsets.UTF_8)));

				for (Entry<String, Object> e : Json.object(json.get("textures")).entrySet()) {
					if (e.getValue() instanceof String s) {
						names.putIfAbsent(e.getKey(), s);
					}
				}

				if (elements == null && json.get("elements") instanceof List) {
					elements = Json.list(json.get("elements"));
				}

				at = Json.string(json.get("parent"), null);
			}

			if (elements != null) {
				for (Object element : elements) {
					this.box(model, Json.object(element), names);
				}
			}

			this.models.put(id, model);
			return model;
		}
	}

	private void box(Kit.Model model, Map<String, Object> element, Map<String, String> names) {
		float[] from = three(element.get("from"), 0.0F);
		float[] to = three(element.get("to"), 16.0F);
		Map<String, Object> rotation = Json.object(element.get("rotation"));
		float angle = Json.number(rotation.get("angle"), 0.0F);
		String axis = Json.string(rotation.get("axis"), "y");
		float[] origin = three(rotation.get("origin"), 8.0F);
		boolean rescale = Boolean.TRUE.equals(rotation.get("rescale"));
		float[] turn = Mat4.identity();
		if (angle != 0.0F) {
			Mat4.rotate(turn, angle, axis.equals("x") ? 1.0F : 0.0F, axis.equals("y") ? 1.0F : 0.0F, axis.equals("z") ? 1.0F : 0.0F);
		}

		float stretch = rescale && angle != 0.0F ? (float)(1.0 / Math.cos(Math.toRadians((double)Math.abs(angle)))) : 1.0F;
		float[] out = new float[4];
		Map<String, Object> faces = Json.object(element.get("faces"));

		for (int f = 0; f < FACES.length; f++) {
			if (faces.get(FACES[f]) instanceof Map) {
				Map<String, Object> face = Json.object(faces.get(FACES[f]));
				String texture = resolve(Json.string(face.get("texture"), ""), names);
				if (texture != null) {
					Kit.Quad q = new Kit.Quad();
					q.texture = texture;
					q.region = this.texture(texture);

					for (int c = 0; c < 4; c++) {
						int[] corner = CORNERS[f][c];
						float x = corner[0] == 1 ? to[0] : from[0];
						float y = corner[1] == 1 ? to[1] : from[1];
						float z = corner[2] == 1 ? to[2] : from[2];
						if (angle != 0.0F) {
							Mat4.transform(turn, x - origin[0], y - origin[1], z - origin[2], out);
							x = out[0] * (axis.equals("x") ? 1.0F : stretch) + origin[0];
							y = out[1] * (axis.equals("y") ? 1.0F : stretch) + origin[1];
							z = out[2] * (axis.equals("z") ? 1.0F : stretch) + origin[2];
						}

						q.p[c * 3] = x / 16.0F;
						q.p[c * 3 + 1] = y / 16.0F;
						q.p[c * 3 + 2] = z / 16.0F;
					}

					float e1x = q.p[3] - q.p[0];
					float e1y = q.p[4] - q.p[1];
					float e1z = q.p[5] - q.p[2];
					float e2x = q.p[9] - q.p[0];
					float e2y = q.p[10] - q.p[1];
					float e2z = q.p[11] - q.p[2];
					float nx = e1y * e2z - e1z * e2y;
					float ny = e1z * e2x - e1x * e2z;
					float nz = e1x * e2y - e1y * e2x;
					float length = (float)Math.sqrt((double)(nx * nx + ny * ny + nz * nz));
					if (!(length < 1.0E-7F)) {
						q.n[0] = nx / length;
						q.n[1] = ny / length;
						q.n[2] = nz / length;
						float[] uv = face.get("uv") instanceof List ? four(face.get("uv")) : ownUv(f, from, to);
						int quarter = Math.round(Json.number(face.get("rotation"), 0.0F) / 90.0F) & 3;

						for (int c = 0; c < 4; c++) {
							int j = c + quarter & 3;
							q.uv[c * 2] = j != 0 && j != 1 ? uv[2] : uv[0];
							q.uv[c * 2 + 1] = j != 0 && j != 3 ? uv[3] : uv[1];
						}

						model.quads.add(q);
					}
				}
			}
		}
	}

	private static float[] ownUv(int face, float[] from, float[] to) {
		return switch (face) {
			case 0 -> new float[]{from[0], 16.0F - to[2], to[0], 16.0F - from[2]};
			case 1 -> new float[]{from[0], from[2], to[0], to[2]};
			case 2 -> new float[]{16.0F - to[0], 16.0F - to[1], 16.0F - from[0], 16.0F - from[1]};
			case 3 -> new float[]{from[0], 16.0F - to[1], to[0], 16.0F - from[1]};
			case 4 -> new float[]{from[2], 16.0F - to[1], to[2], 16.0F - from[1]};
			default -> new float[]{16.0F - to[2], 16.0F - to[1], 16.0F - from[2], 16.0F - from[1]};
		};
	}

	private static String resolve(String name, Map<String, String> names) {
		String at = name;

		for (int i = 0; i < 8 && at != null && at.startsWith("#"); i++) {
			at = names.get(at.substring(1));
		}

		return at != null && !at.isEmpty() && !at.startsWith("#") ? at : null;
	}

	private static float[] three(Object value, float fallback) {
		List<Object> list = Json.list(value);
		float[] v = new float[]{fallback, fallback, fallback};

		for (int i = 0; i < 3 && i < list.size(); i++) {
			v[i] = Json.number(list.get(i), fallback);
		}

		return v;
	}

	private static float[] four(Object value) {
		List<Object> list = Json.list(value);
		float[] v = new float[]{0.0F, 0.0F, 16.0F, 16.0F};

		for (int i = 0; i < 4 && i < list.size(); i++) {
			v[i] = Json.number(list.get(i), v[i]);
		}

		return v;
	}

	Kit.Model sprite(String id) {
		String key = "sprite " + id;
		Kit.Model known = this.models.get(key);
		if (known != null) {
			return known;
		} else {
			Sheet.Region region = this.texture(id);
			int[] px = this.pictures.get(id);
			int w = region.w();
			int h = region.h();
			Kit.Model model = new Kit.Model();
			float z0 = 0.46875F;
			float z1 = 0.53125F;
			flat(model, id, region, 0.0F, 0.0F, 0.53125F, 1.0F, 1.0F, 0.53125F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 16.0F, 16.0F);
			flat(model, id, region, 1.0F, 0.0F, 0.46875F, 0.0F, 1.0F, 0.46875F, 0.0F, 0.0F, -1.0F, 16.0F, 0.0F, 0.0F, 16.0F);

			for (int y = 0; y < h; y++) {
				for (int x = 0; x < w; x++) {
					if (!clear(px, w, h, x, y)) {
						float x0 = (float)x / (float)w;
						float x1 = (float)(x + 1) / (float)w;
						float y1 = 1.0F - (float)y / (float)h;
						float y0 = 1.0F - (float)(y + 1) / (float)h;
						float u = ((float)x + 0.5F) * 16.0F / (float)w;
						float v = ((float)y + 0.5F) * 16.0F / (float)h;
						if (clear(px, w, h, x - 1, y)) {
							side(model, id, region, x0, y0, 0.46875F, x0, y1, 0.53125F, -1.0F, 0.0F, 0.0F, u, v);
						}

						if (clear(px, w, h, x + 1, y)) {
							side(model, id, region, x1, y0, 0.53125F, x1, y1, 0.46875F, 1.0F, 0.0F, 0.0F, u, v);
						}

						if (clear(px, w, h, x, y - 1)) {
							side(model, id, region, x0, y1, 0.53125F, x1, y1, 0.46875F, 0.0F, 1.0F, 0.0F, u, v);
						}

						if (clear(px, w, h, x, y + 1)) {
							side(model, id, region, x0, y0, 0.46875F, x1, y0, 0.53125F, 0.0F, -1.0F, 0.0F, u, v);
						}
					}
				}
			}

			this.models.put(key, model);
			return model;
		}
	}

	private static boolean clear(int[] px, int w, int h, int x, int y) {
		return x < 0 || y < 0 || x >= w || y >= h || px[y * w + x] >>> 24 < 26;
	}

	private static void flat(
		Kit.Model model,
		String id,
		Sheet.Region region,
		float x0,
		float y0,
		float z,
		float x1,
		float y1,
		float zz,
		float nx,
		float ny,
		float nz,
		float u0,
		float v0,
		float u1,
		float v1
	) {
		Kit.Quad q = new Kit.Quad();
		q.texture = id;
		q.region = region;
		set(q, 0, x0, y1, z, u0, v0);
		set(q, 1, x0, y0, z, u0, v1);
		set(q, 2, x1, y0, z, u1, v1);
		set(q, 3, x1, y1, z, u1, v0);
		q.n[0] = nx;
		q.n[1] = ny;
		q.n[2] = nz;
		model.quads.add(q);
	}

	private static void side(
		Kit.Model model, String id, Sheet.Region region, float ax, float ay, float az, float bx, float by, float bz, float nx, float ny, float nz, float u, float v
	) {
		Kit.Quad q = new Kit.Quad();
		q.texture = id;
		q.region = region;
		if (nx != 0.0F) {
			set(q, 0, ax, by, az, u, v);
			set(q, 1, ax, ay, az, u, v);
			set(q, 2, ax, ay, bz, u, v);
			set(q, 3, ax, by, bz, u, v);
		} else {
			set(q, 0, ax, ay, bz, u, v);
			set(q, 1, ax, ay, az, u, v);
			set(q, 2, bx, ay, az, u, v);
			set(q, 3, bx, ay, bz, u, v);
		}

		q.n[0] = nx;
		q.n[1] = ny;
		q.n[2] = nz;
		float e1x = q.p[3] - q.p[0];
		float e1y = q.p[4] - q.p[1];
		float e1z = q.p[5] - q.p[2];
		float e2x = q.p[9] - q.p[0];
		float e2y = q.p[10] - q.p[1];
		float e2z = q.p[11] - q.p[2];
		float cx = e1y * e2z - e1z * e2y;
		float cy = e1z * e2x - e1x * e2z;
		float cz = e1x * e2y - e1y * e2x;
		if (cx * nx + cy * ny + cz * nz < 0.0F) {
			for (int i = 0; i < 3; i++) {
				float t = q.p[3 + i];
				q.p[3 + i] = q.p[9 + i];
				q.p[9 + i] = t;
			}
		}

		model.quads.add(q);
	}

	private static void set(Kit.Quad q, int corner, float x, float y, float z, float u, float v) {
		q.p[corner * 3] = x;
		q.p[corner * 3 + 1] = y;
		q.p[corner * 3 + 2] = z;
		q.uv[corner * 2] = u;
		q.uv[corner * 2 + 1] = v;
	}

	void emit(Mesh solid, Mesh sheer, Kit.Model model, Kit.Skin skin) {
		float[] uv = this.scratch;

		for (Kit.Quad q : model.quads) {
			System.arraycopy(q.uv, 0, uv, 0, 8);
			Sheet.Region region = skin == null ? null : skin.skin(q, uv);
			if (region != SKIP) {
				if (region == null) {
					region = q.region;
				}

				float mu = 0.0F;
				float mv = 0.0F;

				for (int c = 0; c < 4; c++) {
					mu += uv[c * 2] / 4.0F;
					mv += uv[c * 2 + 1] / 4.0F;
				}

				Mesh into = region.sheer() && sheer != null ? sheer : solid;
				if (into != solid) {
					into.copyMatrix(solid);
				}

				float[] p = q.p;
				into.rawQuad(
					p[0],
					p[1],
					p[2],
					p[3],
					p[4],
					p[5],
					p[6],
					p[7],
					p[8],
					p[9],
					p[10],
					p[11],
					q.n[0],
					q.n[1],
					q.n[2],
					region.u(near(uv[0], mu)),
					region.v(near(uv[1], mv)),
					region.u(near(uv[2], mu)),
					region.v(near(uv[3], mv)),
					region.u(near(uv[4], mu)),
					region.v(near(uv[5], mv)),
					region.u(near(uv[6], mu)),
					region.v(near(uv[7], mv))
				);
			}
		}
	}

	private static float near(float value, float middle) {
		return value + Math.signum(middle - value) * 0.004F;
	}

	static void joined(float[] uv, boolean left, boolean right, boolean up, boolean down) {
		int column = left && right ? 2 : (left ? 3 : (right ? 1 : 0));
		int row = up && down ? 1 : (up ? 2 : (down ? 0 : 3));

		for (int c = 0; c < 4; c++) {
			uv[c * 2] = ((float)column * 16.0F + uv[c * 2]) / 4.0F;
			uv[c * 2 + 1] = ((float)row * 16.0F + uv[c * 2 + 1]) / 4.0F;
		}
	}

	static void joined(Kit.Quad q, float[] uv, int[] at, int[] size) {
		float[] p = q.p;
		float e1x = p[3] - p[0];
		float e1y = p[4] - p[1];
		float e1z = p[5] - p[2];
		float e2x = p[9] - p[0];
		float e2y = p[10] - p[1];
		float e2z = p[11] - p[2];
		float du1 = uv[2] - uv[0];
		float dv1 = uv[3] - uv[1];
		float du2 = uv[6] - uv[0];
		float dv2 = uv[7] - uv[1];
		int[] right;
		int[] down;
		if (Math.abs(du1) > Math.abs(dv1)) {
			right = way(e1x, e1y, e1z, du1);
			down = way(e2x, e2y, e2z, dv2);
		} else {
			right = way(e2x, e2y, e2z, du2);
			down = way(e1x, e1y, e1z, dv1);
		}

		joined(uv, in(at, size, right, -1), in(at, size, right, 1), in(at, size, down, -1), in(at, size, down, 1));
	}

	private static int[] way(float x, float y, float z, float grows) {
		float ax = Math.abs(x);
		float ay = Math.abs(y);
		float az = Math.abs(z);
		int s = grows < 0.0F ? -1 : 1;
		if (ax >= ay && ax >= az) {
			return new int[]{x < 0.0F ? -s : s, 0, 0};
		} else {
			return ay >= az ? new int[]{0, y < 0.0F ? -s : s, 0} : new int[]{0, 0, z < 0.0F ? -s : s};
		}
	}

	private static boolean in(int[] at, int[] size, int[] way, int step) {
		for (int i = 0; i < 3; i++) {
			int v = at[i] + way[i] * step;
			if (v < 0 || v >= size[i]) {
				return false;
			}
		}

		return true;
	}

	static final class Model {
		final List<Kit.Quad> quads = new ArrayList<>();
	}

	static final class Quad {
		final float[] p = new float[12];
		final float[] n = new float[3];
		final float[] uv = new float[8];
		String texture;
		Sheet.Region region;
	}

	@FunctionalInterface
	interface Skin {
		Sheet.Region skin(Kit.Quad var1, float[] var2);
	}
}
