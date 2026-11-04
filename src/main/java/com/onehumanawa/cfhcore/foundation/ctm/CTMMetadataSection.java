package com.onehumanawa.cfhcore.foundation.ctm;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.inventory.InventoryMenu;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Parses the {@code cfhcore} metadata section of a texture's {@code .png.mcmeta}, ported
 * from LDLib2's {@code LDLMetadataSection}. It currently only carries the connected
 * texture mapping:
 *
 * <pre>
 * {
 *   "cfhcore": {
 *     "connection": "kubejs:block/annealed_glass_ctm"
 *   }
 * }
 * </pre>
 *
 * The {@code connection} value points at the CTM sprite (a 4x4 tile sheet) that replaces
 * the base texture when the block connects to its neighbours.
 * <p>
 * The flags are collected once per resource reload by {@link #scan} — the resource
 * manager handed to a reload's prepare phase is the only one that actually sees the
 * packs being loaded — and are then served from {@link #packSections}. The live manager
 * of {@code Minecraft.getInstance()} still holds the previous reload's state while the
 * models are baking, so it is only used as a fallback.
 */
public class CTMMetadataSection {

	public static final String SECTION_NAME = "cfhcore";

	private static final Map<ResourceLocation, CTMMetadataSection> METADATA_CACHE = new ConcurrentHashMap<>();

	/** The connections detected by the last pack scan, keyed by absolute texture location. */
	private static volatile Map<ResourceLocation, CTMMetadataSection> packSections = Map.of();

	public static final CTMMetadataSection MISSING = new CTMMetadataSection(null);

	@Nullable
	public final ResourceLocation connection;

	public CTMMetadataSection(@Nullable ResourceLocation connection) {
		this.connection = connection;
	}

	/**
	 * Reads the {@code cfhcore} section of every texture in one pass, using the resource
	 * manager of the reload that is currently preparing. Only textures that actually
	 * declare a connection are kept, so the map stays small.
	 * <p>
	 * The mcmeta files themselves cannot be enumerated: 1.21.1's resource manager treats
	 * them as sidecars of their texture, so {@code listResources} only ever returns the
	 * base files, with the parsed section attached through {@link Resource#metadata()}.
	 */
	public static Map<ResourceLocation, CTMMetadataSection> scan(ResourceManager manager) {
		Map<ResourceLocation, CTMMetadataSection> sections = new HashMap<>();
		manager.listResources("textures", location -> location.getPath().endsWith(".png")).forEach((name, resource) -> {
			try {
				resource.metadata().getSection(Serializer.INSTANCE).ifPresent(section -> {
					if (section.connection != null)
						sections.put(name, section);
				});
			} catch (Exception ignored) {}
		});
		return sections;
	}

	/**
	 * Swaps the flags of the current pack state in. Callable from the prepare phase of a
	 * reload already, since the model manager only bakes in the apply phase after it.
	 */
	public static void publish(Map<ResourceLocation, CTMMetadataSection> sections) {
		packSections = Map.copyOf(sections);
		METADATA_CACHE.clear();
	}

	public static void clearCache() {
		METADATA_CACHE.clear();
	}

	public boolean isMissing() {
		return this == MISSING;
	}

	@Nonnull
	public static CTMMetadataSection getMetadata(ResourceLocation res) {
		CTMMetadataSection cached = METADATA_CACHE.get(res);
		if (cached != null)
			return cached;

		CTMMetadataSection ret = packSections.get(res);
		if (ret == null) {
			// Not flagged by the current pack state; still probe the live manager so
			// textures reloaded outside of a full reload are picked up too.
			ret = MISSING;
			var resourceOptional = Minecraft.getInstance().getResourceManager().getResource(res);
			if (resourceOptional.isPresent()) {
				Resource resource = resourceOptional.get();
				try {
					ret = resource.metadata().getSection(Serializer.INSTANCE).orElse(MISSING);
				} catch (Exception ignored) {}
			}
		}
		METADATA_CACHE.put(res, ret);
		return ret;
	}

	@Nonnull
	public static CTMMetadataSection getMetadata(TextureAtlasSprite sprite) {
		return getMetadata(spriteToAbsolute(sprite.contents().name()));
	}

	@Nullable
	public static TextureAtlasSprite getConnection(TextureAtlasSprite sprite) {
		CTMMetadataSection ret = getMetadata(spriteToAbsolute(sprite.contents().name()));
		return ret.connection == null ? null : getBlockSprite(ret.connection);
	}

	/** Looks up a sprite on the block atlas. */
	public static TextureAtlasSprite getBlockSprite(ResourceLocation location) {
		return Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(location);
	}

	/** Turns an atlas sprite name back into the absolute texture file location. */
	public static ResourceLocation spriteToAbsolute(ResourceLocation sprite) {
		if (!sprite.getPath().startsWith("textures/"))
			sprite = ResourceLocation.fromNamespaceAndPath(sprite.getNamespace(), "textures/" + sprite.getPath());
		if (!sprite.getPath().endsWith(".png"))
			sprite = ResourceLocation.fromNamespaceAndPath(sprite.getNamespace(), sprite.getPath() + ".png");
		return sprite;
	}

	public static class Serializer implements MetadataSectionSerializer<CTMMetadataSection> {

		static final Serializer INSTANCE = new Serializer();

		@Override
		@Nonnull
		public String getMetadataSectionName() {
			return SECTION_NAME;
		}

		@Override
		@Nonnull
		public CTMMetadataSection fromJson(@Nonnull JsonObject json) {
			ResourceLocation connection = null;
			if (json.isJsonObject()) {
				JsonObject obj = json.getAsJsonObject();
				if (obj.has("connection")) {
					JsonElement element = obj.get("connection");
					if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString())
						connection = ResourceLocation.tryParse(element.getAsString());
				}
			}
			return new CTMMetadataSection(connection);
		}
	}
}
