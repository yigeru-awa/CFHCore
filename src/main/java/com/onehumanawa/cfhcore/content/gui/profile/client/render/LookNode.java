package com.onehumanawa.cfhcore.content.gui.profile.client.render;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageRenderContext;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageSoft;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.PlayerNode;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.PlayerNode.Part;
import com.onehumanawa.cfhcore.content.gui.profile.CFHGuiProfile;
import com.onehumanawa.cfhcore.content.gui.profile.client.Cosmetic;
import com.onehumanawa.cfhcore.content.gui.profile.client.ProfileStore;
import com.onehumanawa.cfhcore.content.gui.profile.client.SkinComposer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.client.resources.PlayerSkin.Model;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

public final class LookNode extends PlayerNode {
	private static final AtomicInteger COUNTER = new AtomicInteger();
	@Nullable
	private ProfileStore.Look look;
	@Nullable
	private Cosmetic sample;
	private boolean sampleSlim;
	private String made = "";
	@Nullable
	private ResourceLocation texture;
	private PlayerSkin skin = DefaultPlayerSkin.get(new UUID(0L, 0L));
	private List<Cosmetic> models = List.of();
	@Nullable
	private Cosmetic tryOn;

	private LookNode() {
		super(() -> DefaultPlayerSkin.get(new UUID(0L, 0L)));
	}

	public LookNode(ProfileStore.Look look) {
		this();
		this.look = look;
		this.named(look.name());
	}

	public static LookNode sample(Cosmetic layer, boolean slim) {
		LookNode n = new LookNode();
		n.sample = layer;
		n.sampleSlim = slim;
		n.named(layer.name());
		return n;
	}

	public static LookNode ofTexture(ResourceLocation skin, boolean slim) {
		LookNode n = new LookNode();
		n.skin = new PlayerSkin(skin, null, null, null, slim ? Model.SLIM : Model.WIDE, true);
		n.made = "fixed";
		return n;
	}

	@Nullable
	public ProfileStore.Look look() {
		return this.look;
	}

	public LookNode tryOn(@Nullable Cosmetic cosmetic) {
		if (this.tryOn != cosmetic) {
			this.tryOn = cosmetic;
			this.made = "";
		}

		return this;
	}

	public PlayerSkin skin() {
		return this.skin;
	}

	private void ensure() {
		if (this.look != null || this.sample != null) {
			String now = this.look != null
				? this.look.signature() + SkinComposer.sourceKey(this.look) + "|" + (this.tryOn == null ? "" : this.tryOn.id())
				: "sample:" + this.sample.id();
			if (!now.equals(this.made)) {
				this.made = now;
				List<Cosmetic> built = new ArrayList<>();
				NativeImage image;
				boolean slim;
				if (this.look != null) {
					if (this.tryOn != null && !this.tryOn.isModel()) {
						Cosmetic before = this.look.in(this.tryOn.slot());
						this.look.hold(this.tryOn.slot(), this.tryOn);
						image = SkinComposer.compose(this.look);
						this.look.hold(this.tryOn.slot(), before);
					} else {
						image = SkinComposer.compose(this.look);
					}

					slim = this.look.slim();

					for (Cosmetic c : this.look.items()) {
						if (c.isModel() && (this.tryOn == null || this.tryOn.slot() != c.slot())) {
							built.add(c);
						}
					}

					if (this.tryOn != null && this.tryOn.isModel()) {
						built.add(this.tryOn);
					}
				} else {
					image = SkinComposer.sample(this.sample, this.sampleSlim);
					slim = this.sampleSlim;
				}

				ResourceLocation id = CFHGuiProfile.id("stage_look/" + COUNTER.incrementAndGet());
				Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(image));
				if (this.texture != null) {
					Minecraft.getInstance().getTextureManager().release(this.texture);
				}

				this.texture = id;
				this.skin = new PlayerSkin(id, null, null, null, slim ? Model.SLIM : Model.WIDE, true);
				this.models = built;
			}
		}
	}

	public void update(StageRenderContext ctx) {
		this.ensure();
		super.update(ctx);
	}

	protected void drawExtras(StageRenderContext ctx, PlayerModel<LivingEntity> model) {
		if (!this.models.isEmpty()) {
			float age = ctx.timeMs / 50.0F;

			for (Cosmetic c : this.models) {
				if (this.rides(c)) {
					if (ctx.soft) {
						BufferBuilder bb = StageSoft.begin(StageSoft.ENTITY);
						CosmeticRenderer.draw(ctx.pose, model, c, age, ctx.light, OverlayTexture.NO_OVERLAY, -1, cosmetic -> bb);
						StageSoft.end(bb, StageSoft.ENTITY, c.texture(), c.translucent() ? Math.min(0.999F, this.alpha()) : this.alpha(), false);
					} else {
						int color = Math.round(this.alpha() * 255.0F) << 24 | 16777215;
						CosmeticRenderer.draw(
							ctx.pose,
							model,
							c,
							age,
							ctx.light,
							OverlayTexture.NO_OVERLAY,
							color,
							cosmetic -> ctx.buffers.getBuffer(RenderType.entityTranslucent(cosmetic.texture()))
						);
					}
				}
			}
		}
	}

	private boolean rides(Cosmetic c) {
		return switch (c.anchor()) {
			case HEAD, BESIDE -> this.shows(Part.HEAD);
			case RIGHT_ARM -> this.shows(Part.RIGHT_ARM);
			case LEFT_ARM -> this.shows(Part.LEFT_ARM);
			case RIGHT_LEG -> this.shows(Part.RIGHT_LEG);
			case LEFT_LEG -> this.shows(Part.LEFT_LEG);
			default -> this.shows(Part.BODY);
		};
	}

	public void dispose() {
		if (this.texture != null) {
			Minecraft.getInstance().getTextureManager().release(this.texture);
			this.texture = null;
		}

		super.dispose();
	}
}
