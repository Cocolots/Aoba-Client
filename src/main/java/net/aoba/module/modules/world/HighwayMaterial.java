package net.aoba.module.modules.world;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public enum HighwayMaterial {
	OBSIDIAN(Blocks.OBSIDIAN),
	STONE(Blocks.STONE),
	NETHERRACK(Blocks.NETHERRACK),
	ICE(Blocks.ICE),
	PACKED_ICE(Blocks.PACKED_ICE),
	BLUE_ICE(Blocks.BLUE_ICE),
	RAIL(Blocks.RAIL);

	private final Block block;

	HighwayMaterial(Block block) {
		this.block = block;
	}

	public Block block() {
		return block;
	}
}
