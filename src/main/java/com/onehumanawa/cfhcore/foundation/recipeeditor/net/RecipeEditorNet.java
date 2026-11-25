package com.onehumanawa.cfhcore.foundation.recipeeditor.net;

import com.onehumanawa.cfhcore.content.gui.core.net.CFHGuiNetwork;
import com.onehumanawa.cfhcore.content.gui.core.net.Flow;
import com.onehumanawa.cfhcore.foundation.recipeeditor.CFHGuiRecipeEditor;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public final class RecipeEditorNet {
	public static void register() {
		CFHGuiNetwork net = CFHGuiNetwork.get();
		net.register(RecipeEditorPayload.TYPE, RecipeEditorPayload.CODEC, Flow.C2S, (p, ctx) -> {
			// Handle payload on server side
			if (ctx.sender() != null) {
				// Server side handling
			}
		});
	}

	public record RecipeEditorPayload(String action, String data) implements CustomPacketPayload {
		public static final CustomPacketPayload.Type<RecipeEditorPayload> TYPE = new CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("cfhgui", "recipe_editor"));
		public static final StreamCodec<RegistryFriendlyByteBuf, RecipeEditorPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8, RecipeEditorPayload::action,
			ByteBufCodecs.STRING_UTF8, RecipeEditorPayload::data,
			RecipeEditorPayload::new
		);

		@Override
		public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	private RecipeEditorNet() {
	}
}