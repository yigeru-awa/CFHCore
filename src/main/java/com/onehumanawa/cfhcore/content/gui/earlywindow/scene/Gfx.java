package com.onehumanawa.cfhcore.content.gui.earlywindow.scene;

import java.nio.ByteBuffer;
import org.lwjgl.opengl.GL32C;
import org.lwjgl.system.MemoryUtil;

public final class Gfx {
	static final int SOLID = 0;
	static final int TEXT = 1;
	static final int VIGNETTE = 2;
	static final int IMAGE = 3;
	private static final String VERTEX = "#version 150\nin vec2 aPos;\nin vec2 aUv;\nin vec4 aColor;\nuniform vec2 uScreen;\nuniform float uFlip;\nout vec2 vUv;\nout vec4 vColor;\nvoid main() {\n    vec2 p = aPos / uScreen * 2.0 - 1.0;\n    gl_Position = vec4(p.x, p.y * uFlip, 0.0, 1.0);\n    vUv = aUv;\n    vColor = aColor;\n}\n";
	private static final String FRAGMENT = "#version 150\nin vec2 vUv;\nin vec4 vColor;\nuniform sampler2D uTex;\nuniform int uMode;\nout vec4 fragColor;\nvoid main() {\n    if (uMode == 1) {\n        fragColor = vec4(vColor.rgb, vColor.a * texture(uTex, vUv).r);\n    } else if (uMode == 2) {\n        vec2 d = (vUv - 0.5) * 2.0;\n        fragColor = vec4(vColor.rgb, vColor.a * clamp(dot(d, d) * 0.5, 0.0, 1.0));\n    } else if (uMode == 3) {\n        fragColor = texture(uTex, vUv) * vColor;\n    } else {\n        fragColor = vColor;\n    }\n}\n";
	private static final int STRIDE = 20;
	private static final int CAPACITY = 12288;
	private int program;
	private int vao;
	private int vbo;
	private int uScreen;
	private int uMode;
	private int uTex;
	private int uFlip;
	private final ByteBuffer buffer = MemoryUtil.memAlloc(245760);
	private int vertices;
	private int mode = -1;
	private int texture;
	private float scale = 1.0F;
	private float alpha = 1.0F;
	private final GlSave saved = new GlSave();
	private boolean own;

	private void init() {
		this.program = link(
			"#version 150\nin vec2 aPos;\nin vec2 aUv;\nin vec4 aColor;\nuniform vec2 uScreen;\nuniform float uFlip;\nout vec2 vUv;\nout vec4 vColor;\nvoid main() {\n    vec2 p = aPos / uScreen * 2.0 - 1.0;\n    gl_Position = vec4(p.x, p.y * uFlip, 0.0, 1.0);\n    vUv = aUv;\n    vColor = aColor;\n}\n",
			"#version 150\nin vec2 vUv;\nin vec4 vColor;\nuniform sampler2D uTex;\nuniform int uMode;\nout vec4 fragColor;\nvoid main() {\n    if (uMode == 1) {\n        fragColor = vec4(vColor.rgb, vColor.a * texture(uTex, vUv).r);\n    } else if (uMode == 2) {\n        vec2 d = (vUv - 0.5) * 2.0;\n        fragColor = vec4(vColor.rgb, vColor.a * clamp(dot(d, d) * 0.5, 0.0, 1.0));\n    } else if (uMode == 3) {\n        fragColor = texture(uTex, vUv) * vColor;\n    } else {\n        fragColor = vColor;\n    }\n}\n",
			"aPos",
			"aUv",
			"aColor"
		);
		this.uScreen = GL32C.glGetUniformLocation(this.program, "uScreen");
		this.uMode = GL32C.glGetUniformLocation(this.program, "uMode");
		this.uTex = GL32C.glGetUniformLocation(this.program, "uTex");
		this.uFlip = GL32C.glGetUniformLocation(this.program, "uFlip");
		this.vao = GL32C.glGenVertexArrays();
		this.vbo = GL32C.glGenBuffers();
		GL32C.glBindVertexArray(this.vao);
		GL32C.glBindBuffer(34962, this.vbo);
		GL32C.glBufferData(34962, 245760L, 35040);
		GL32C.glEnableVertexAttribArray(0);
		GL32C.glVertexAttribPointer(0, 2, 5126, false, 20, 0L);
		GL32C.glEnableVertexAttribArray(1);
		GL32C.glVertexAttribPointer(1, 2, 5126, false, 20, 8L);
		GL32C.glEnableVertexAttribArray(2);
		GL32C.glVertexAttribPointer(2, 4, 5121, true, 20, 16L);
	}

	static int link(String vertex, String fragment, String... attributes) {
		int vs = shader(35633, vertex);
		int fs = shader(35632, fragment);
		int p = GL32C.glCreateProgram();
		GL32C.glAttachShader(p, vs);
		GL32C.glAttachShader(p, fs);

		for (int i = 0; i < attributes.length; i++) {
			GL32C.glBindAttribLocation(p, i, attributes[i]);
		}

		GL32C.glBindFragDataLocation(p, 0, "fragColor");
		GL32C.glLinkProgram(p);
		GL32C.glDeleteShader(vs);
		GL32C.glDeleteShader(fs);
		if (GL32C.glGetProgrami(p, 35714) == 0) {
			String log = GL32C.glGetProgramInfoLog(p);
			GL32C.glDeleteProgram(p);
			throw new IllegalStateException("link: " + log);
		} else {
			return p;
		}
	}

	private static int shader(int type, String source) {
		int s = GL32C.glCreateShader(type);
		GL32C.glShaderSource(s, source);
		GL32C.glCompileShader(s);
		if (GL32C.glGetShaderi(s, 35713) == 0) {
			String log = GL32C.glGetShaderInfoLog(s);
			GL32C.glDeleteShader(s);
			throw new IllegalStateException("compile: " + log);
		} else {
			return s;
		}
	}

	public void begin(int pixelW, int pixelH, float scale) {
		this.begin(pixelW, pixelH, scale, true);
	}

	public void begin(int pixelW, int pixelH, float scale, boolean flipped) {
		this.begin(pixelW, pixelH, scale, flipped, true);
	}

	void begin(int pixelW, int pixelH, float scale, boolean flipped, boolean save) {
		this.own = save;
		if (save) {
			this.saved.save();
		}

		if (this.program == 0) {
			this.init();
		}

		this.scale = scale;
		GL32C.glUseProgram(this.program);
		GL32C.glBindVertexArray(this.vao);
		GL32C.glBindBuffer(34962, this.vbo);
		GL32C.glUniform2f(this.uScreen, (float)pixelW, (float)pixelH);
		GL32C.glUniform1f(this.uFlip, flipped ? 1.0F : -1.0F);
		GL32C.glUniform1i(this.uTex, 0);
		GL32C.glActiveTexture(33984);
		GL32C.glDisable(2929);
		GL32C.glDisable(2884);
		GL32C.glDisable(3089);
		GL32C.glDisable(2960);
		GL32C.glDepthMask(false);
		GL32C.glColorMask(true, true, true, true);
		GL32C.glEnable(3042);
		GL32C.glBlendEquationSeparate(32774, 32774);
		GL32C.glBlendFuncSeparate(770, 771, 1, 771);
		this.mode = -1;
		this.vertices = 0;
		this.alpha = 1.0F;
	}

	public void end() {
		this.flush();
		if (this.own) {
			this.saved.restore();
		}
	}

	public void delete() {
		if (this.program != 0) {
			GL32C.glDeleteProgram(this.program);
			GL32C.glDeleteVertexArrays(this.vao);
			GL32C.glDeleteBuffers(this.vbo);
			this.program = 0;
		}

		MemoryUtil.memFree(this.buffer);
	}

	public void alpha(float a) {
		this.alpha = Math.max(0.0F, Math.min(1.0F, a));
	}

	public float scale() {
		return this.scale;
	}

	public void rect(float x, float y, float w, float h, int argb) {
		if (!(w <= 0.0F) && !(h <= 0.0F)) {
			this.quad(0, 0, x, y, x + w, y + h, 0.0F, 0.0F, 1.0F, 1.0F, argb, argb, argb, argb);
		}
	}

	public void hgradient(float x, float y, float w, float h, int left, int right) {
		if (!(w <= 0.0F) && !(h <= 0.0F)) {
			this.quad(0, 0, x, y, x + w, y + h, 0.0F, 0.0F, 1.0F, 1.0F, left, right, right, left);
		}
	}

	public void vgradient(float x, float y, float w, float h, int top, int bottom) {
		if (!(w <= 0.0F) && !(h <= 0.0F)) {
			this.quad(0, 0, x, y, x + w, y + h, 0.0F, 0.0F, 1.0F, 1.0F, top, top, bottom, bottom);
		}
	}

	public void pixelRound(float x, float y, float w, float h, int argb, int radius) {
		int r = (int)Math.max(0.0F, Math.min((float)radius, Math.min(w, h) / 2.0F));
		if (r == 0) {
			this.rect(x, y, w, h, argb);
		} else {
			this.rect(x, y + (float)r, w, h - (float)(2 * r), argb);

			for (int i = 0; i < r; i++) {
				int inset = r - i;
				this.rect(x + (float)inset, y + (float)i, w - (float)(2 * inset), 1.0F, argb);
				this.rect(x + (float)inset, y + h - 1.0F - (float)i, w - (float)(2 * inset), 1.0F, argb);
			}
		}
	}

	public void outline(float x, float y, float w, float h, int argb, int radius) {
		int r = (int)Math.max(0.0F, Math.min((float)radius, Math.min(w, h) / 2.0F));
		this.rect(x + (float)r, y, w - (float)(2 * r), 1.0F, argb);
		this.rect(x + (float)r, y + h - 1.0F, w - (float)(2 * r), 1.0F, argb);
		this.rect(x, y + (float)r, 1.0F, h - (float)(2 * r), argb);
		this.rect(x + w - 1.0F, y + (float)r, 1.0F, h - (float)(2 * r), argb);

		for (int i = 1; i <= r; i++) {
			int px = r - i;
			int py = i - 1;
			this.rect(x + (float)px, y + (float)py + 1.0F, 1.0F, 1.0F, argb);
			this.rect(x + w - 1.0F - (float)px, y + (float)py + 1.0F, 1.0F, 1.0F, argb);
			this.rect(x + (float)px, y + h - 2.0F - (float)py, 1.0F, 1.0F, argb);
			this.rect(x + w - 1.0F - (float)px, y + h - 2.0F - (float)py, 1.0F, 1.0F, argb);
		}
	}

	public void shadow(float x, float y, float w, float h, int argb, int radius) {
		this.pixelRound(x + 2.0F, y + h - (float)radius - 2.0F, w, (float)(radius + 4), argb, radius);
		this.rect(x + w, y + 2.0F, 2.0F, h - (float)radius - 4.0F, argb);
	}

	public void vignette(float w, float h, int argb) {
		this.quad(2, 0, 0.0F, 0.0F, w, h, 0.0F, 0.0F, 1.0F, 1.0F, argb, argb, argb, argb);
	}

	public void image(int tex, float x, float y, float w, float h, float u0, float v0, float u1, float v1, int argb) {
		if (!(w <= 0.0F) && !(h <= 0.0F)) {
			this.quad(3, tex, x, y, x + w, y + h, u0, v0, u1, v1, argb, argb, argb, argb);
		}
	}

	void glyph(int atlas, float x0, float y0, float x1, float y1, float u0, float v0, float u1, float v1, int argb) {
		this.quadPx(1, atlas, x0, y0, x1, y1, u0, v0, u1, v1, argb, argb, argb, argb);
	}

	private void quad(int m, int tex, float x0, float y0, float x1, float y1, float u0, float v0, float u1, float v1, int c0, int c1, int c2, int c3) {
		this.quadPx(m, tex, x0 * this.scale, y0 * this.scale, x1 * this.scale, y1 * this.scale, u0, v0, u1, v1, c0, c1, c2, c3);
	}

	private void quadPx(int m, int tex, float x0, float y0, float x1, float y1, float u0, float v0, float u1, float v1, int c0, int c1, int c2, int c3) {
		boolean textured = m == 1 || m == 3;
		if (m != this.mode || textured && tex != this.texture) {
			this.flush();
			this.mode = m;
			this.texture = tex;
		}

		if (this.vertices + 6 > 12288) {
			this.flush();
		}

		this.vertex(x0, y0, u0, v0, c0);
		this.vertex(x1, y0, u1, v0, c1);
		this.vertex(x1, y1, u1, v1, c2);
		this.vertex(x0, y0, u0, v0, c0);
		this.vertex(x1, y1, u1, v1, c2);
		this.vertex(x0, y1, u0, v1, c3);
	}

	private void vertex(float x, float y, float u, float v, int argb) {
		int a = Math.round((float)(argb >>> 24 & 0xFF) * this.alpha);
		this.buffer.putFloat(x).putFloat(y).putFloat(u).putFloat(v).put((byte)(argb >>> 16)).put((byte)(argb >>> 8)).put((byte)argb).put((byte)a);
		this.vertices++;
	}

	private void flush() {
		if (this.vertices != 0) {
			this.buffer.flip();
			GL32C.glBufferSubData(34962, 0L, this.buffer);
			GL32C.glUniform1i(this.uMode, this.mode);
			if (this.mode == 1 || this.mode == 3) {
				GL32C.glBindTexture(3553, this.texture);
			}

			GL32C.glDrawArrays(4, 0, this.vertices);
			this.buffer.clear();
			this.vertices = 0;
		}
	}
}
