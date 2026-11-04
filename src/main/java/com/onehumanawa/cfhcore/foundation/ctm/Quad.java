package com.onehumanawa.cfhcore.foundation.ctm;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec2;
import net.neoforged.neoforge.client.model.IQuadTransformer;
import net.neoforged.neoforge.client.model.pipeline.QuadBakingVertexConsumer;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Ported from LDLib2's {@code Quad}. A baked quad pulled apart into vertex positions,
 * UVs and vertex data so it can be subdivided and re-mapped onto a CTM sprite.
 */
@ParametersAreNonnullByDefault
public class Quad {

	// Int offsets within one vertex of the BLOCK format
	private static final int STRIDE = IQuadTransformer.STRIDE;
	private static final int POSITION = IQuadTransformer.POSITION;
	private static final int COLOR = IQuadTransformer.COLOR;
	private static final int UV0 = IQuadTransformer.UV0;
	private static final int UV2 = IQuadTransformer.UV2;

	public static class UVs {

		private float minU, minV, maxU, maxV;
		private final TextureAtlasSprite sprite;
		private final Vec2[] data;

		private UVs(Vec2... data) {
			this(base(), data);
		}

		private UVs(TextureAtlasSprite sprite, Vec2... data) {
			this.data = data;
			this.sprite = sprite;

			float minU = Float.MAX_VALUE;
			float minV = Float.MAX_VALUE;
			float maxU = 0, maxV = 0;
			for (Vec2 v : data) {
				minU = Math.min(minU, v.x);
				minV = Math.min(minV, v.y);
				maxU = Math.max(maxU, v.x);
				maxV = Math.max(maxV, v.y);
			}
			this.minU = minU;
			this.minV = minV;
			this.maxU = maxU;
			this.maxV = maxV;
		}

		public UVs(float minU, float minV, float maxU, float maxV, TextureAtlasSprite sprite) {
			this.minU = minU;
			this.minV = minV;
			this.maxU = maxU;
			this.maxV = maxV;
			this.sprite = sprite;
			this.data = vectorize();
		}

		public float getMinU() {
			return minU;
		}

		public float getMinV() {
			return minV;
		}

		public float getMaxU() {
			return maxU;
		}

		public float getMaxV() {
			return maxV;
		}

		public TextureAtlasSprite getSprite() {
			return sprite;
		}

		public UVs transform(TextureAtlasSprite other, ISubmap submap) {
			UVs normal = normalize();
			submap = submap.normalize();

			float width = normal.maxU - normal.minU;
			float height = normal.maxV - normal.minV;

			float minU = submap.getXOffset();
			float minV = submap.getYOffset();
			minU += normal.minU * submap.getWidth();
			minV += normal.minV * submap.getHeight();

			float maxU = minU + (width * submap.getWidth());
			float maxV = minV + (height * submap.getHeight());

			return new UVs(other,
					new Vec2(data[0].x == this.minU ? minU : maxU, data[0].y == this.minV ? minV : maxV),
					new Vec2(data[1].x == this.minU ? minU : maxU, data[1].y == this.minV ? minV : maxV),
					new Vec2(data[2].x == this.minU ? minU : maxU, data[2].y == this.minV ? minV : maxV),
					new Vec2(data[3].x == this.minU ? minU : maxU, data[3].y == this.minV ? minV : maxV))
					.relativize();
		}

		UVs normalizeQuadrant() {
			UVs normal = normalize();

			int quadrant = normal.getQuadrant();
			float minUInterp = quadrant == 1 || quadrant == 2 ? 0.5f : 0;
			float minVInterp = quadrant < 2 ? 0.5f : 0;
			float maxUInterp = quadrant == 0 || quadrant == 3 ? 0.5f : 1;
			float maxVInterp = quadrant > 1 ? 0.5f : 1;

			normal = new UVs(sprite, normalize(new Vec2(minUInterp, minVInterp), new Vec2(maxUInterp, maxVInterp), normal.vectorize()));
			return normal.relativize();
		}

		public UVs normalize() {
			Vec2 min = new Vec2(sprite.getU0(), sprite.getV0());
			Vec2 max = new Vec2(sprite.getU1(), sprite.getV1());
			return new UVs(sprite, normalize(min, max, data));
		}

		public UVs relativize() {
			return relativize(sprite);
		}

		public UVs relativize(TextureAtlasSprite sprite) {
			Vec2 min = new Vec2(sprite.getU0(), sprite.getV0());
			Vec2 max = new Vec2(sprite.getU1(), sprite.getV1());
			return new UVs(sprite, lerp(min, max, data));
		}

		public Vec2[] vectorize() {
			return data == null
					? new Vec2[] { new Vec2(minU, minV), new Vec2(minU, maxV), new Vec2(maxU, maxV), new Vec2(maxU, minV) }
					: data;
		}

		private Vec2[] normalize(Vec2 min, Vec2 max, Vec2... vecs) {
			Vec2[] ret = new Vec2[vecs.length];
			for (int i = 0; i < ret.length; i++) {
				ret[i] = normalize(min, max, vecs[i]);
			}
			return ret;
		}

		private Vec2 normalize(Vec2 min, Vec2 max, Vec2 vec) {
			return new Vec2(Quad.normalize(min.x, max.x, vec.x), Quad.normalize(min.y, max.y, vec.y));
		}

		private Vec2[] lerp(Vec2 min, Vec2 max, Vec2... vecs) {
			Vec2[] ret = new Vec2[vecs.length];
			for (int i = 0; i < ret.length; i++) {
				ret[i] = lerp(min, max, vecs[i]);
			}
			return ret;
		}

		private Vec2 lerp(Vec2 min, Vec2 max, Vec2 vec) {
			return new Vec2(Quad.lerp(min.x, max.x, vec.x), Quad.lerp(min.y, max.y, vec.y));
		}

		public int getQuadrant() {
			if (maxU <= 0.5f) {
				if (maxV <= 0.5f) {
					return 3;
				} else {
					return 0;
				}
			} else {
				if (maxV <= 0.5f) {
					return 2;
				} else {
					return 1;
				}
			}
		}
	}

	private static TextureAtlasSprite base() {
		return CTMMetadataSection.getBlockSprite(MissingTextureAtlasSprite.getLocation());
	}

	private final Vector3f[] vertPos;
	private final Vec2[] vertUv;
	private final UVs uvs;
	private final Builder builder;
	private final int blocklight, skylight;

	private Quad(Vector3f[] verts, Vec2[] uvs, Builder builder, TextureAtlasSprite sprite) {
		this(verts, uvs, builder, sprite, 0, 0);
	}

	private Quad(Vector3f[] verts, Vec2[] uvs, Builder builder, TextureAtlasSprite sprite, int blocklight, int skylight) {
		this.vertPos = verts;
		this.vertUv = uvs;
		this.builder = builder;
		this.uvs = new UVs(sprite, uvs);
		this.blocklight = blocklight;
		this.skylight = skylight;
	}

	private Quad(Vector3f[] verts, UVs uvs, Builder builder, int blocklight, int skylight) {
		this(verts, uvs.vectorize(), builder, uvs.getSprite(), blocklight, skylight);
	}

	public Vector3f getVert(int index) {
		return new Vector3f(vertPos[index % 4]);
	}

	public Quad withVert(int index, Vector3f vert) {
		Vector3f[] newverts = new Vector3f[4];
		System.arraycopy(vertPos, 0, newverts, 0, newverts.length);
		newverts[index] = vert;
		return new Quad(newverts, getUvs(), builder, blocklight, skylight);
	}

	public Vec2 getUv(int index) {
		return new Vec2(vertUv[index % 4].x, vertUv[index % 4].y);
	}

	public Quad withUv(int index, Vec2 uv) {
		Vec2[] newuvs = new Vec2[4];
		System.arraycopy(getUvs().vectorize(), 0, newuvs, 0, newuvs.length);
		newuvs[index] = uv;
		return new Quad(vertPos, new UVs(newuvs), builder, blocklight, skylight);
	}

	public Vector3f[] getVertPos() {
		return vertPos;
	}

	public Vec2[] getVertUv() {
		return vertUv;
	}

	public UVs getUvs() {
		return uvs;
	}

	public Builder getBuilder() {
		return builder;
	}

	/**
	 * Splits this quad into a 2x2 grid of quads, following the UV layout. Missing regions
	 * (when the UVs don't span the tile mid) are returned as null entries.
	 */
	public Quad[] subdivide(int count) {
		if (count == 1) {
			return new Quad[] { this };
		} else if (count != 4) {
			throw new UnsupportedOperationException();
		}

		List<Quad> rects = new ArrayList<>();

		Quad[] firstDivide = divide(false);
		Quad[] secondDivide = firstDivide[0].divide(true);
		rects.add(secondDivide[0]);

		if (firstDivide[1] != null) {
			Quad[] thirdDivide = firstDivide[1].divide(true);
			rects.add(thirdDivide[0]);
			rects.add(thirdDivide[1]);
		} else {
			rects.add(null);
			rects.add(null);
		}

		rects.add(secondDivide[1]);

		return rects.toArray(new Quad[0]);
	}

	private Quad[] divide(boolean vertical) {
		float min, max;
		UVs uvs = getUvs().normalize();
		if (vertical) {
			min = uvs.minV;
			max = uvs.maxV;
		} else {
			min = uvs.minU;
			max = uvs.maxU;
		}

		if (min < 0.5 && max > 0.5) {
			UVs first = new UVs(vertical ? uvs.minU : 0.5f, vertical ? 0.5f : uvs.minV, uvs.maxU, uvs.maxV, uvs.getSprite());
			UVs second = new UVs(uvs.minU, uvs.minV, vertical ? uvs.maxU : 0.5f, vertical ? 0.5f : uvs.maxV, uvs.getSprite());

			int firstIndex = 0;
			for (int i = 0; i < vertUv.length; i++) {
				if (vertUv[i].y == getUvs().minV && vertUv[i].x == getUvs().minU) {
					firstIndex = i;
					break;
				}
			}

			float f = (0.5f - min) / (max - min);

			Vector3f[] firstQuad = new Vector3f[4];
			Vector3f[] secondQuad = new Vector3f[4];
			for (int i = 0; i < 4; i++) {
				int idx = (firstIndex + i) % 4;
				firstQuad[i] = new Vector3f(vertPos[idx]);
				secondQuad[i] = new Vector3f(vertPos[idx]);
			}

			int i1 = 0;
			int i2 = vertical ? 1 : 3;
			int j1 = vertical ? 3 : 1;
			int j2 = 2;

			firstQuad[i1].set(lerp(firstQuad[i1].x(), firstQuad[i2].x(), f),
					lerp(firstQuad[i1].y(), firstQuad[i2].y(), f),
					lerp(firstQuad[i1].z(), firstQuad[i2].z(), f));
			firstQuad[j1].set(lerp(firstQuad[j1].x(), firstQuad[j2].x(), f),
					lerp(firstQuad[j1].y(), firstQuad[j2].y(), f),
					lerp(firstQuad[j1].z(), firstQuad[j2].z(), f));

			secondQuad[i2].set(lerp(secondQuad[i1].x(), secondQuad[i2].x(), f),
					lerp(secondQuad[i1].y(), secondQuad[i2].y(), f),
					lerp(secondQuad[i1].z(), secondQuad[i2].z(), f));
			secondQuad[j2].set(lerp(secondQuad[j1].x(), secondQuad[j2].x(), f),
					lerp(secondQuad[j1].y(), secondQuad[j2].y(), f),
					lerp(secondQuad[j1].z(), secondQuad[j2].z(), f));

			Quad q1 = new Quad(firstQuad, first.relativize(), builder, blocklight, skylight);
			Quad q2 = new Quad(secondQuad, second.relativize(), builder, blocklight, skylight);
			return new Quad[] { q1, q2 };
		} else {
			return new Quad[] { this, null };
		}
	}

	public static float lerp(float a, float b, float f) {
		return (a * (1 - f)) + (b * f);
	}

	public static float normalize(float min, float max, float x) {
		return (x - min) / (max - min);
	}

	public Quad rotate(int amount) {
		Vec2[] uvs = new Vec2[4];

		TextureAtlasSprite s = getUvs().getSprite();

		for (int i = 0; i < 4; i++) {
			Vec2 normalized = new Vec2(normalize(s.getU0(), s.getU1(), vertUv[i].x), normalize(s.getV0(), s.getV1(), vertUv[i].y));
			Vec2 uv = switch (amount) {
				case 1 -> new Vec2(normalized.y, 1 - normalized.x);
				case 2 -> new Vec2(1 - normalized.x, 1 - normalized.y);
				case 3 -> new Vec2(1 - normalized.y, normalized.x);
				default -> new Vec2(normalized.x, normalized.y);
			};
			uvs[i] = uv;
		}

		for (int i = 0; i < uvs.length; i++) {
			uvs[i] = new Vec2(lerp(s.getU0(), s.getU1(), uvs[i].x), lerp(s.getV0(), s.getV1(), uvs[i].y));
		}

		return new Quad(vertPos, uvs, builder, getUvs().getSprite(), blocklight, skylight);
	}

	public Quad derotate() {
		int start = 0;
		for (int i = 0; i < 4; i++) {
			if (vertUv[i].x <= getUvs().minU && vertUv[i].y <= getUvs().minV) {
				start = i;
				break;
			}
		}

		Vec2[] uvs = new Vec2[4];
		for (int i = 0; i < 4; i++) {
			uvs[i] = vertUv[(i + start) % 4];
		}
		return new Quad(vertPos, uvs, builder, getUvs().getSprite(), blocklight, skylight);
	}

	public Quad setLight(int blocklight, int skylight) {
		return new Quad(this.vertPos, uvs, builder, Math.max(this.blocklight, blocklight), Math.max(this.skylight, skylight));
	}

	public BakedQuad rebake() {
		QuadBakingVertexConsumer builder = new QuadBakingVertexConsumer();
		builder.setDirection(this.builder.quadOrientation);
		builder.setTintIndex(this.builder.quadTint);
		builder.setShade(this.builder.applyDiffuseLighting);
		builder.setSprite(this.uvs.getSprite());

		for (int v = 0; v < 4; v++) {
			for (VertexFormatElement ele : DefaultVertexFormat.BLOCK.getElements()) {
				switch (ele.usage()) {
					case POSITION -> {
						Vector3f p = vertPos[v];
						builder.addVertex(p.x(), p.y(), p.z());
					}
					case COLOR -> {
						int[] c = getBuilder().colors[v];
						builder.setColor(c[0], c[1], c[2], c[3]);
					}
					case UV -> {
						if (ele == VertexFormatElement.UV2) {
							// the lightmap
							builder.setUv2(blocklight << 4, skylight << 4);
						} else if (ele == VertexFormatElement.UV0) {
							Vec2 uv = vertUv[v];
							builder.setUv(uv.x, uv.y);
						} else {
							builder.misc(ele, this.builder.packedByElement.get(ele)[v]);
						}
					}
					default -> builder.misc(ele, this.builder.packedByElement.get(ele)[v]);
				}
			}
		}

		return builder.bakeQuad();
	}

	public Quad transformUVs(TextureAtlasSprite sprite) {
		return transformUVs(sprite, Submap.FULL_TEXTURE.normalize());
	}

	public Quad transformUVs(TextureAtlasSprite sprite, ISubmap submap) {
		return new Quad(vertPos, getUvs().transform(sprite, submap), builder, blocklight, skylight);
	}

	public Quad grow() {
		return new Quad(vertPos, getUvs().normalizeQuadrant(), builder, blocklight, skylight);
	}

	public static Quad from(BakedQuad baked) {
		Builder b = new Builder(baked.getSprite());
		b.copyFrom(baked, 0);
		return b.build();
	}

	public static Quad from(BakedQuad baked, float offset) {
		Builder b = new Builder(baked.getSprite());
		b.copyFrom(baked, offset);
		return b.build();
	}

	public static class Builder {

		/** Int offsets of each vertex format element within a vertex. */
		public static final Map<VertexFormatElement, Integer> ELEMENT_OFFSETS;

		static {
			ELEMENT_OFFSETS = new IdentityHashMap<>();
			for (VertexFormatElement element : DefaultVertexFormat.BLOCK.getElements())
				ELEMENT_OFFSETS.put(element, DefaultVertexFormat.BLOCK.getOffset(element) / 4);
		}


		private final TextureAtlasSprite sprite;

		private int quadTint = -1;
		private @Nullable Direction quadOrientation;
		private boolean applyDiffuseLighting;

		public final float[][] positions = new float[4][];
		public final float[][] uvs = new float[4][];
		public final int[][] uvs2 = new int[4][];
		public final int[][] colors = new int[4][];

		private final Map<VertexFormatElement, int[][]> packedByElement = new HashMap<>();

		public Builder(TextureAtlasSprite sprite) {
			this.sprite = sprite;
		}

		public TextureAtlasSprite getSprite() {
			return sprite;
		}

		public int getQuadTint() {
			return quadTint;
		}

		public Builder setQuadTint(int quadTint) {
			this.quadTint = quadTint;
			return this;
		}

		public @Nullable Direction getQuadOrientation() {
			return quadOrientation;
		}

		public Builder setQuadOrientation(@Nullable Direction quadOrientation) {
			this.quadOrientation = quadOrientation;
			return this;
		}

		public boolean isApplyDiffuseLighting() {
			return applyDiffuseLighting;
		}

		public Builder setApplyDiffuseLighting(boolean applyDiffuseLighting) {
			this.applyDiffuseLighting = applyDiffuseLighting;
			return this;
		}

		public void copyFrom(BakedQuad baked, float directionOffset) {
			setQuadTint(baked.getTintIndex());
			setQuadOrientation(baked.getDirection());
			setApplyDiffuseLighting(baked.isShade());

			int[] vertices = baked.getVertices();
			for (int i = 0; i < 4; i++) {
				int offset = i * STRIDE;
				this.positions[i] = new float[] {
						Float.intBitsToFloat(vertices[offset + POSITION]),
						Float.intBitsToFloat(vertices[offset + POSITION + 1]),
						Float.intBitsToFloat(vertices[offset + POSITION + 2]) };
				if (quadOrientation != null && directionOffset != 0) {
					this.positions[i][0] += directionOffset * quadOrientation.getStepX();
					this.positions[i][1] += directionOffset * quadOrientation.getStepY();
					this.positions[i][2] += directionOffset * quadOrientation.getStepZ();
				}
				int packedColor = vertices[offset + COLOR];
				this.colors[i] = new int[] {
						packedColor & 0xFF,
						(packedColor >> 8) & 0xFF,
						(packedColor >> 16) & 0xFF,
						(packedColor >> 24) & 0xFF };
				this.uvs[i] = new float[] {
						Float.intBitsToFloat(vertices[offset + UV0]),
						Float.intBitsToFloat(vertices[offset + UV0 + 1]) };
				int lightMap = vertices[offset + UV2];
				this.uvs2[i] = new int[] {
						lightMap & 0xFFFF,
						(lightMap >> 16) & 0xFFFF };
			}
			for (var e : ELEMENT_OFFSETS.entrySet()) {
				int offset = e.getValue();
				int[][] data = new int[4][e.getKey().byteSize() / 4];
				for (int v = 0; v < 4; v++) {
					for (int i = 0; i < data[v].length; i++) {
						data[v][i] = vertices[v * STRIDE + offset + i];
					}
				}
				this.packedByElement.put(e.getKey(), data);
			}
		}

		public Quad build() {
			Vector3f[] verts = new Vector3f[4];
			Vec2[] uvs = new Vec2[4];
			for (int i = 0; i < verts.length; i++) {
				verts[i] = new Vector3f(this.positions[i][0], this.positions[i][1], this.positions[i][2]);
				uvs[i] = new Vec2(this.uvs[i][0], this.uvs[i][1]);
			}
			return new Quad(verts, uvs, this, getSprite(), uvs2[0][0] >> 4, uvs2[0][1] >> 4);
		}
	}
}
