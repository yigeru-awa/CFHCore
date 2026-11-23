package com.onehumanawa.cfhcore.content.gui.teams.client.overhaul;

import com.onehumanawa.cfhcore.content.gui.core.stage.Stage;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.BlocksNode;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.state.BlockState;

final class Piece {
	final String key;
	private final BlocksNode node;
	private final int minX;
	private final int minY;
	private final int minZ;
	private final int maxX;
	private final int maxY;
	private final int maxZ;

	Piece(Stage stage, String key, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
		this.key = key;
		this.minX = minX;
		this.minY = minY;
		this.minZ = minZ;
		this.maxX = maxX;
		this.maxY = maxY;
		this.maxZ = maxZ;
		this.node = new BlocksNode(stage.level(), maxX - minX + 1, maxY - minY + 1, maxZ - minZ + 1);
		this.node.named(key);
	}

	boolean covers(int x, int y, int z) {
		return x >= this.minX && x <= this.maxX && y >= this.minY && y <= this.maxY && z >= this.minZ && z <= this.maxZ;
	}

	Piece put(int x, int y, int z, BlockState state) {
		this.node.put(x - this.minX, y - this.minY, z - this.minZ, state);
		return this;
	}

	Piece put(int x, int y, int z, String spec) {
		return this.put(x, y, z, Build.of(spec));
	}

	Piece fill(int x0, int y0, int z0, int x1, int y1, int z1, BlockState state) {
		for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++) {
			for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++) {
				for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) {
					this.put(x, y, z, state);
				}
			}
		}

		return this;
	}

	Piece fill(int x0, int y0, int z0, int x1, int y1, int z1, String spec) {
		return this.fill(x0, y0, z0, x1, y1, z1, Build.of(spec));
	}

	BlockState at(int x, int y, int z) {
		return this.covers(x, y, z) ? this.node.at(x - this.minX, y - this.minY, z - this.minZ) : Blocks.AIR.defaultBlockState();
	}

	boolean empty(int x, int y, int z) {
		return this.at(x, y, z).isAir();
	}

	private void connect() {
		for (int x = this.minX; x <= this.maxX; x++) {
			for (int y = this.minY; y <= this.maxY; y++) {
				for (int z = this.minZ; z <= this.maxZ; z++) {
					BlockState s = this.at(x, y, z);
					if (s.getBlock() instanceof FenceBlock || s.getBlock() instanceof IronBarsBlock) {
						this.put(
							x,
							y,
							z,
							(BlockState)((BlockState)((BlockState)((BlockState)s.setValue(CrossCollisionBlock.NORTH, joins(s, this.at(x, y, z - 1))))
										.setValue(CrossCollisionBlock.SOUTH, joins(s, this.at(x, y, z + 1))))
									.setValue(CrossCollisionBlock.WEST, joins(s, this.at(x - 1, y, z))))
								.setValue(CrossCollisionBlock.EAST, joins(s, this.at(x + 1, y, z)))
						);
					}
				}
			}
		}
	}

	private static boolean joins(BlockState self, BlockState other) {
		if (other.isAir()) {
			return false;
		} else {
			if (self.getBlock() instanceof FenceBlock) {
				if (other.getBlock() instanceof FenceBlock || other.getBlock() instanceof FenceGateBlock) {
					return true;
				}
			} else if (other.getBlock() instanceof IronBarsBlock) {
				return true;
			}

			return other.isSolidRender(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
		}
	}

	BlocksNode done(Stage stage) {
		this.connect();
		this.node.done().softLit(true).centered();
		((BlocksNode)stage.add(this.node)).at((float)(this.minX + this.maxX) / 2.0F, (float)this.minY, (float)(this.minZ + this.maxZ) / 2.0F);
		return this.node;
	}
}
