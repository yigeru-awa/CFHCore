package com.onehumanawa.cfhcore.foundation.ctm;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Ported from LDLib2's {@code CustomBakedModel}. A {@link BakedModel} wrapper that
 * re-bakes each face of the wrapped model with connected textures: quads whose sprite
 * declares a {@code cfhcore.connection} in its png.mcmeta are subdivided into a 2x2 grid
 * and the four pieces are re-mapped onto tiles of the CTM sheet according to the
 * block's neighbours.
 * <p>
 * The world-aware result is obtained through {@link #getCustomQuads}; the plain
 * {@link BakedModel#getQuads(BlockState, Direction, RandomSource)} entry points keep
 * returning the parent quads, so item rendering and anything without world context is
 * unaffected.
 */
public class CTMBakedModel implements BakedModel {

	protected final BakedModel parent;
	protected final ConcurrentMap<Direction, ConcurrentMap<Connections, List<BakedQuad>>> sideCache;
	protected final List<BakedQuad> noSideCache;

	public CTMBakedModel(BakedModel parent) {
		this.parent = parent;
		this.sideCache = new ConcurrentHashMap<>();
		this.noSideCache = new ArrayList<>();
	}

	public BakedModel getParent() {
		return parent;
	}

	/**
	 * The connection-aware quads for a face of a block in the world. Results are cached
	 * per (side, connections).
	 */
	@Nonnull
	public List<BakedQuad> getCustomQuads(BlockAndTintGetter level, BlockPos pos, @Nonnull BlockState state,
			@Nullable Direction side, RandomSource rand) {
		Connections connections = Connections.checkConnections(level, pos, state, side);
		if (side == null) {
			synchronized (noSideCache) {
				if (noSideCache.isEmpty()) {
					noSideCache.addAll(buildCustomQuads(connections, parent.getQuads(state, null, rand), 0.0f));
				}
			}
			return noSideCache;
		}
		return sideCache
				.computeIfAbsent(side, key -> new ConcurrentHashMap<>())
				.computeIfAbsent(connections, key -> buildCustomQuads(key, parent.getQuads(state, side, rand), 0.0f));
	}

	/**
	 * Subdivides and re-maps a bunch of already-baked quads for a block in the world.
	 * Use this from custom renderers that hold their own baked model instance.
	 */
	public static List<BakedQuad> rebuildQuads(List<BakedQuad> quads, BlockAndTintGetter level, BlockPos pos,
			@Nonnull BlockState state, @Nullable Direction side, float offset) {
		return buildCustomQuads(Connections.checkConnections(level, pos, state, side), quads, offset);
	}

	public static List<BakedQuad> buildCustomQuads(Connections connections, List<BakedQuad> base, float offset) {
		List<BakedQuad> result = new LinkedList<>();
		for (BakedQuad bakedQuad : base) {
			CTMMetadataSection section = CTMMetadataSection.getMetadata(bakedQuad.getSprite());
			TextureAtlasSprite connection = section.connection == null ? null
					: CTMMetadataSection.getBlockSprite(section.connection);
			if (connection == null) {
				result.add(Quad.from(bakedQuad, offset).rebake());
				continue;
			}

			Quad quad = Quad.from(bakedQuad, offset).derotate();
			Quad[] quads = quad.subdivide(4);

			int[] ctm = connections.getSubmapIndices();

			for (int j = 0; j < quads.length; j++) {
				Quad q = quads[j];
				if (q != null) {
					int ctmid = q.getUvs().normalize().getQuadrant();
					// Sub-tiles above index 15 fall back to quarters of the base texture
					quads[j] = q.grow().transformUVs(ctm[ctmid] > 15 ? bakedQuad.getSprite() : connection,
							Submap.uvs[ctm[ctmid]]);
				}
			}
			Arrays.stream(quads).filter(Objects::nonNull).map(Quad::rebake).forEach(result::add);
		}
		return result;
	}

	@Override
	@Nonnull
	@Deprecated
	public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
		// While a block is being rendered, the block's neighbourhood decides the tiles
		CTMRenderState.Context renderState = CTMRenderState.current();
		if (renderState != null && state != null)
			return getCustomQuads(renderState.level(), renderState.pos(), state, side, rand);
		return parent.getQuads(state, side, rand);
	}

	@Override
	public boolean useAmbientOcclusion() {
		return parent.useAmbientOcclusion();
	}

	@Override
	public boolean isGui3d() {
		return parent.isGui3d();
	}

	@Override
	public boolean usesBlockLight() {
		return parent.usesBlockLight();
	}

	@Override
	public boolean isCustomRenderer() {
		return parent.isCustomRenderer();
	}

	@Override
	@Nonnull
	public TextureAtlasSprite getParticleIcon() {
		return parent.getParticleIcon();
	}

	@Nonnull
	@Override
	public ItemTransforms getTransforms() {
		return parent.getTransforms();
	}

	@Nonnull
	@Override
	public ItemOverrides getOverrides() {
		return parent.getOverrides();
	}
}
