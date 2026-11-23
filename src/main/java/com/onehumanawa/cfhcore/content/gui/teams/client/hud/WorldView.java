package com.onehumanawa.cfhcore.content.gui.teams.client.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import com.onehumanawa.cfhcore.content.gui.core.client.render.CFHGuiRenderEvents.WorldRenderContext;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;

final class WorldView {
	static final int EDGE_SIDE = 14;
	static final int EDGE_TOP = 14;
	static final int EDGE_BOTTOM = 50;
	private static final Matrix4f VIEW = new Matrix4f();
	private static final Matrix4f PROJECTION = new Matrix4f();
	private static final Vector4f POINT = new Vector4f();
	private static double camX;
	private static double camY;
	private static double camZ;
	private static float yaw;
	private static float partialTick;
	private static boolean fresh;

	static boolean place(WorldView.Spot spot, int w, int h, int halfW, int halfH, float[] out) {
		int left = halfW + 14;
		int top = halfH + 14;
		int right = halfW + 14;
		int bottom = halfH + 50;
		if (spot.front && spot.x >= (float)left && spot.x <= (float)(w - right) && spot.y >= (float)top && spot.y <= (float)(h - bottom)) {
			out[0] = spot.x;
			out[1] = spot.y;
			return false;
		} else {
			HudDraw.edgePoint(w, h, spot.dirX, spot.dirY, left, top, right, bottom, out);
			return true;
		}
	}

	static void capture(WorldRenderContext context) {
		VIEW.set(RenderSystem.getModelViewMatrix());
		PROJECTION.set(RenderSystem.getProjectionMatrix());
		Vec3 cam = context.camera().getPosition();
		camX = cam.x;
		camY = cam.y;
		camZ = cam.z;
		yaw = context.camera().getYRot();
		partialTick = context.partialTick();
		fresh = true;
	}

	static boolean ready() {
		return fresh;
	}

	static void endFrame() {
		fresh = false;
	}

	static double camX() {
		return camX;
	}

	static double camY() {
		return camY;
	}

	static double camZ() {
		return camZ;
	}

	static float yaw() {
		return yaw;
	}

	static float partialTick() {
		return partialTick;
	}

	static boolean project(double x, double y, double z, int w, int h, WorldView.Spot out) {
		POINT.set((float)(x - camX), (float)(y - camY), (float)(z - camZ), 1.0F);
		VIEW.transform(POINT);
		float vx = POINT.x;
		float vy = POINT.y;
		float vz = POINT.z;
		float dx = PROJECTION.m00() * vx * (float)w;
		float dy = -PROJECTION.m11() * vy * (float)h;
		float len = (float)Math.sqrt((double)(dx * dx + dy * dy));
		if (len > 1.0E-4F) {
			out.dirX = dx / len;
			out.dirY = dy / len;
		} else {
			out.dirX = 0.0F;
			out.dirY = 1.0F;
		}

		out.depth = -vz;
		PROJECTION.transform(POINT);
		out.front = POINT.w > 0.05F;
		if (out.front) {
			out.x = (POINT.x / POINT.w * 0.5F + 0.5F) * (float)w;
			out.y = (0.5F - POINT.y / POINT.w * 0.5F) * (float)h;
		}

		return out.front;
	}

	private WorldView() {
	}

	static final class Spot {
		float x;
		float y;
		boolean front;
		float depth;
		float dirX;
		float dirY;

		boolean within(int w, int h, int margin) {
			return this.front && this.x >= (float)margin && this.y >= (float)margin && this.x <= (float)(w - margin) && this.y <= (float)(h - margin);
		}

		float focus(int w, int h, float radius) {
			if (!this.front) {
				return 0.0F;
			} else {
				float dx = this.x - (float)w / 2.0F;
				float dy = this.y - (float)h / 2.0F;
				float d = (float)Math.sqrt((double)(dx * dx + dy * dy));
				return d >= radius ? 0.0F : 1.0F - d / radius;
			}
		}
	}
}
