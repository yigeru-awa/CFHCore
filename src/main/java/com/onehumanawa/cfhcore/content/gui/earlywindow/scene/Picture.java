package com.onehumanawa.cfhcore.content.gui.earlywindow.scene;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import org.lwjgl.opengl.GL32C;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

record Picture(int w, int h, int[] argb) {
	static Picture read(byte[] file) {
		if (file != null && file.length != 0) {
			ByteBuffer in = MemoryUtil.memAlloc(file.length).put(file).flip();

			try {
				MemoryStack stack = MemoryStack.stackPush();

				Object var29;
				label187: {
					Picture var30;
					try {
						IntBuffer pw = stack.mallocInt(1);
						IntBuffer ph = stack.mallocInt(1);
						IntBuffer pc = stack.mallocInt(1);
						ByteBuffer rgba = STBImage.stbi_load_from_memory(in, pw, ph, pc, 4);
						if (rgba == null) {
							var29 = null;
							break label187;
						}

						try {
							int w = pw.get(0);
							int h = ph.get(0);
							int[] argb = new int[w * h];

							for (int i = 0; i < argb.length; i++) {
								int r = rgba.get(i * 4) & 255;
								int g = rgba.get(i * 4 + 1) & 255;
								int b = rgba.get(i * 4 + 2) & 255;
								int a = rgba.get(i * 4 + 3) & 255;
								argb[i] = a << 24 | r << 16 | g << 8 | b;
							}

							var30 = new Picture(w, h, argb);
						} finally {
							STBImage.stbi_image_free(rgba);
						}
					} catch (Throwable var27) {
						if (stack != null) {
							try {
								stack.close();
							} catch (Throwable var25) {
								var27.addSuppressed(var25);
							}
						}

						throw var27;
					}

					if (stack != null) {
						stack.close();
					}

					return var30;
				}

				if (stack != null) {
					stack.close();
				}

				return (Picture)var29;
			} finally {
				MemoryUtil.memFree(in);
			}
		} else {
			return null;
		}
	}

	int texture() {
		return this.texture(false);
	}

	int texture(boolean crisp) {
		ByteBuffer bytes = MemoryUtil.memAlloc(this.argb.length * 4);

		int var11;
		try {
			for (int c : this.argb) {
				bytes.put((byte)(c >>> 16)).put((byte)(c >>> 8)).put((byte)c).put((byte)(c >>> 24));
			}

			bytes.flip();
			int texture = GL32C.glGenTextures();
			GL32C.glBindTexture(3553, texture);
			GlSave.plainUnpack();
			GL32C.glTexImage2D(3553, 0, 32856, this.w, this.h, 0, 6408, 5121, bytes);
			GL32C.glTexParameteri(3553, 10241, crisp ? 9728 : 9729);
			GL32C.glTexParameteri(3553, 10240, crisp ? 9728 : 9729);
			GL32C.glTexParameteri(3553, 10242, 33071);
			GL32C.glTexParameteri(3553, 10243, 33071);
			var11 = texture;
		} finally {
			MemoryUtil.memFree(bytes);
		}

		return var11;
	}
}
