package com.onehumanawa.cfhcore.foundation.ctm;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Holds the block currently being rendered, so that {@link CTMBakedModel} can re-map its
 * quads according to the block's neighbours even though {@code BakedModel.getQuads}
 * itself has no world context. A {@link com.onehumanawa.cfhcore.foundation.ctm.mixin.ModelBlockRendererMixin}
 * pushes a context around every {@code ModelBlockRenderer#tesselateBlock} call.
 * <p>
 * The stack makes it safe for a wrapped model's quads to be re-rendered from within the
 * same render pass, and keeps render threads separate.
 */
public final class CTMRenderState {

	private static final ThreadLocal<Deque<Context>> STACK = ThreadLocal.withInitial(ArrayDeque::new);

	public record Context(BlockAndTintGetter level, BlockPos pos) {}

	private CTMRenderState() {}

	public static void push(BlockAndTintGetter level, BlockPos pos) {
		STACK.get().push(new Context(level, pos));
	}

	public static void pop() {
		Deque<Context> stack = STACK.get();
		stack.pop();
		if (stack.isEmpty())
			STACK.remove();
	}

	@Nullable
	public static Context current() {
		return STACK.get().peek();
	}
}
