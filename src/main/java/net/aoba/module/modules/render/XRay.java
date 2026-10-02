/*
 * Aoba Hacked Client
 * Copyright (C) 2019-2024 coltonk9043
 *
 * Licensed under the GNU General Public License, Version 3 or later.
 * See <http://www.gnu.org/licenses/>.
 */

package net.aoba.module.modules.render;

import java.util.HashSet;

import net.aoba.settings.types.BooleanSetting;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.InputConstants;
import net.aoba.module.Category;
import net.aoba.module.Module;
import net.aoba.settings.types.BlocksSetting;

public class XRay extends Module {
	private final BlocksSetting blocks = BlocksSetting.builder().id("xray_blocks").displayName("Blocks")
			.description("Blocks that can be seen in Xray")
			.defaultValue(new HashSet<Block>(Lists.newArrayList(Blocks.EMERALD_ORE, Blocks.EMERALD_BLOCK,
					Blocks.DIAMOND_ORE, Blocks.DIAMOND_BLOCK, Blocks.GOLD_ORE, Blocks.GOLD_BLOCK, Blocks.IRON_ORE,
					Blocks.IRON_BLOCK, Blocks.COAL_ORE, Blocks.COAL_BLOCK, Blocks.REDSTONE_BLOCK, Blocks.REDSTONE_ORE,
					Blocks.LAPIS_ORE, Blocks.LAPIS_BLOCK, Blocks.NETHER_QUARTZ_ORE, Blocks.MOSSY_COBBLESTONE,
					Blocks.STONE_BRICKS, Blocks.OAK_PLANKS, Blocks.DEEPSLATE_EMERALD_ORE, Blocks.DEEPSLATE_DIAMOND_ORE,
					Blocks.DEEPSLATE_GOLD_ORE, Blocks.DEEPSLATE_IRON_ORE, Blocks.DEEPSLATE_COAL_ORE, Blocks.ANCIENT_DEBRIS)))
			.onUpdate(this::ReloadRenderer).build();

	public BooleanSetting fluids = BooleanSetting.builder().id("fluids").displayName("Show Fluids")
			.description("Show fluids (water/lava) when using Xray")
			.defaultValue(true)
			.onUpdate(this::ReloadRenderer).build();

	public XRay() {
		super("XRay", InputConstants.Type.KEYBOARD.getOrCreate(InputConstants.KEY_X));
		setCategory(Category.of("Render"));
		setDescription("Allows the player to see ores.");
		addSetting(blocks);
		addSetting(fluids);
	}

	@Override
	public void onDisable() {
		MC.levelRenderer.invalidateCompiledGeometry(MC.level, MC.options, MC.gameRenderer.mainCamera(), MC.getBlockColors());
	}

	@Override
	public void onEnable() {
		MC.levelRenderer.invalidateCompiledGeometry(MC.level, MC.options, MC.gameRenderer.mainCamera(), MC.getBlockColors());

	}

	@Override
	public void onToggle() {

	}

	public boolean isXRayBlock(Block b) {
		HashSet<Block> blockList = blocks.getValue();
        return blockList.contains(b);
    }

	public void ReloadRenderer(HashSet<Block> block) {
		if (MC.levelRenderer != null && state.getValue()) {
			MC.levelRenderer.invalidateCompiledGeometry(MC.level, MC.options, MC.gameRenderer.mainCamera(), MC.getBlockColors());
		}
	}

	public void ReloadRenderer(Boolean fluids) {
		if (MC.levelRenderer != null && state.getValue()) {
			MC.levelRenderer.invalidateCompiledGeometry(MC.level, MC.options, MC.gameRenderer.mainCamera(), MC.getBlockColors());
		}
	}

	public HashSet<Block> getBlocks() {
		return blocks.getValue();
	}
}
