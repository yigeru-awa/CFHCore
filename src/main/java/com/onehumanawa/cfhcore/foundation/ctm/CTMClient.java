package com.onehumanawa.cfhcore.foundation.ctm;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;

import com.onehumanawa.cfhcore.CFHCore;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

/**
 * Client-side wiring for the CTM module:
 * <ul>
 * <li>after the models bake, every model that uses a texture with a
 *     {@code cfhcore.connection} mcmeta entry is wrapped in a {@link CTMBakedModel} —
 *     no per-block opt-in, it takes effect from the resource pack alone;</li>
 * <li>the texture metadata cache is dropped on resource reloads so edited mcmeta
 *     files apply immediately.</li>
 * </ul>
 */
@OnlyIn(Dist.CLIENT)
public class CTMClient {

	private static final Direction[] SIDES_AND_NULL = { null, Direction.UP, Direction.DOWN, Direction.NORTH,
			Direction.SOUTH, Direction.EAST, Direction.WEST };

	public static void init(IEventBus modEventBus) {
		modEventBus.addListener(CTMClient::onModifyBakingResult);
		modEventBus.addListener(CTMClient::registerReloadListeners);
	}

	/**
	 * Wraps every baked model whose quads reference a texture with a CTM connection.
	 * Already wrapped models are skipped, so repeated bakes stay single-layered.
	 */
	private static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
		Map<ModelResourceLocation, BakedModel> models = event.getModels();
		RandomSource random = RandomSource.create();
		for (Map.Entry<ModelResourceLocation, BakedModel> entry : models.entrySet()) {
			BakedModel model = entry.getValue();
			if (model instanceof CTMBakedModel)
				continue;
			if (hasCTMTexture(model, random))
				entry.setValue(new CTMBakedModel(model));
		}
	}

	private static boolean hasCTMTexture(BakedModel model, RandomSource random) {
		for (Direction side : SIDES_AND_NULL) {
			List<BakedQuad> quads;
			try {
				quads = model.getQuads(null, side, random);
			} catch (Exception ignored) {
				// Some models cannot produce quads from a null block state — Create's
				// copycats for instance look the adjacent material up from the state and
				// throw otherwise. Their quads are only ever rebuilt with a real state, so
				// there is nothing to inspect here: skip the side instead of failing the
				// whole bake (an exception during this event aborts the resource reload).
				continue;
			}
			if (quads == null || quads.isEmpty())
				continue;
			for (BakedQuad quad : quads) {
				if (!CTMMetadataSection.getMetadata(quad.getSprite()).isMissing())
					return true;
			}
		}
		return false;
	}

	private static void registerReloadListeners(RegisterClientReloadListenersEvent event) {
		event.registerReloadListener(new SimplePreparableReloadListener<Map<ResourceLocation, CTMMetadataSection>>() {
			@Override
			protected Map<ResourceLocation, CTMMetadataSection> prepare(ResourceManager resourceManager,
					ProfilerFiller profiler) {
				Map<ResourceLocation, CTMMetadataSection> sections = CTMMetadataSection.scan(resourceManager);
				// Published already during the prepare phase on purpose: the model manager
				// bakes the models in the apply phase that follows, and it must see the
				// flags of the packs that are being loaded right now, not of the previous
				// reload — otherwise newly added connections only apply after a manual F3+T.
				CTMMetadataSection.publish(sections);
				return sections;
			}

			@Override
			protected void apply(Map<ResourceLocation, CTMMetadataSection> sections,
					ResourceManager resourceManager, ProfilerFiller profiler) {
				CTMMetadataSection.publish(sections);
				if (!sections.isEmpty())
					CFHCore.LOGGER.debug("Recognized {} CTM connection(s) from resource packs", sections.size());
			}
		});
	}
}
