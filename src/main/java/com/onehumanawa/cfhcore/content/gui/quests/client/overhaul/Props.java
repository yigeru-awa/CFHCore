package com.onehumanawa.cfhcore.content.gui.quests.client.overhaul;

import com.onehumanawa.cfhcore.content.gui.core.stage.StageLevel;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageRenderContext;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageResources;
import com.onehumanawa.cfhcore.content.gui.core.stage.mesh.BlockMesh;
import com.onehumanawa.cfhcore.content.gui.quests.CFHGuiQuests;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

final class Props implements AutoCloseable {
	private static final int REACH = 2;
	private static final int HEIGHT = 6;
	private static int held;
	private final StageLevel level;
	private final BlockPos origin;
	private final Map<String, BlockMesh> meshes = new HashMap<>();
	private int generation = -1;
	private boolean soft;

	static int held() {
		return held;
	}

	Props(StageLevel level) {
		this.level = level;
		this.origin = level.allocateRegion();
	}

	@Nullable
	BlockMesh get(String key, boolean softNow, Consumer<Props.Placer> build) {
		int gen = StageResources.generation();
		if (gen != this.generation || softNow != this.soft) {
			this.close();
			this.generation = gen;
			this.soft = softNow;
		}

		if (this.meshes.containsKey(key)) {
			return this.meshes.get(key);
		} else {
			BlockMesh mesh = null;
			BlockPos max = this.origin.offset(4, 5, 4);

			try {
				build.accept((x, y, z, state) -> {
					if (Math.abs(x) <= 2 && Math.abs(z) <= 2 && y >= 0 && y < 6) {
						this.level.place(this.origin.offset(2 + x, y, 2 + z), state, null, false);
					}
				});
				mesh = BlockMesh.build(this.level, this.origin, max, this.origin.offset(2, 0, 2), softNow);
				held++;
			} catch (Exception var11) {
				CFHGuiQuests.LOGGER.warn("[CFHGui Quests] a piece of the quest island could not be built ({}): {}", key, var11.toString());
			} finally {
				this.level.clear(this.origin, max);
			}

			this.meshes.put(key, mesh);
			return mesh;
		}
	}

	static void draw(@Nullable BlockMesh mesh, StageRenderContext ctx, float alpha) {
		if (mesh != null && !mesh.isEmpty()) {
			ctx.pose.pushPose();
			ctx.pose.translate(-0.5F, 0.0F, -0.5F);
			if (ctx.soft) {
				mesh.drawSoft(false, ctx.pose.last().pose(), ctx.camera.projection(), alpha);
			} else {
				mesh.drawOpaque(ctx.pose.last().pose(), ctx.camera.projection());
			}

			ctx.pose.popPose();
		}
	}

	@Override
	public void close() {
		for (BlockMesh mesh : this.meshes.values()) {
			if (mesh != null) {
				mesh.close();
				held--;
			}
		}

		this.meshes.clear();
	}

	@FunctionalInterface
	interface Placer {
		void put(int var1, int var2, int var3, BlockState var4);
	}
}
