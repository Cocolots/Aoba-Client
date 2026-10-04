/*
 * Aoba Hacked Client
 * Copyright (C) 2019-2024 coltonk9043
 *
 * Licensed under the GNU General Public License, Version 3 or later.
 * See <http://www.gnu.org/licenses/>.
 */

package net.aoba.module.modules.world;

import net.aoba.Aoba;
import net.aoba.event.events.BlockStateEvent;
import net.aoba.event.events.TickEvent.Post;
import net.aoba.event.events.TickEvent.Pre;
import net.aoba.event.listeners.BlockStateListener;
import net.aoba.event.listeners.TickListener;
import net.aoba.module.Category;
import net.aoba.module.Module;
import net.aoba.settings.types.BooleanSetting;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class AutoTool extends Module implements BlockStateListener, TickListener {

    private final BooleanSetting autoToggle = BooleanSetting.builder().id("autotool_autotoggle").displayName("Auto Toggle")
            .description("Automatically toggles off if any critical combat module has been enabled.").defaultValue(true)
            .build();
    private final BooleanSetting swapBack = BooleanSetting.builder().id("autotool_swapback").displayName("Swap Back")
            .description("Switches back to previous slot when done.").defaultValue(true)
            .build();
    private final BooleanSetting miningOnly = BooleanSetting.builder().id("autotool_miningonly").displayName("Mining Only")
            .description("Changes the tool only when starting to mine.").defaultValue(true)
            .build();

    public AutoTool() {
        super("AutoTool");
        setCategory(Category.of("World"));
        setDescription("Automatically switches to the right tool when mining.");

        addSetting(autoToggle);
        addSetting(swapBack);
        addSetting(miningOnly);
    }

    @Override
    public void onDisable() {
        Aoba.getInstance().eventManager.RemoveListener(TickListener.class, this);
        Aoba.getInstance().eventManager.RemoveListener(BlockStateListener.class, this);
    }

    @Override
    public void onEnable() {
        Aoba.getInstance().eventManager.AddListener(TickListener.class, this);
        Aoba.getInstance().eventManager.AddListener(BlockStateListener.class, this);
    }

    @Override
    public void onToggle() {

    }

    @Override
    public void onTick(Pre event) {
        if (MC.player == null || MC.hitResult == null) 
        	return;

        HitResult ray = MC.hitResult;
        if (ray.getType() != HitResult.Type.BLOCK) {
            if (swapBack.getValue()) swapBack();
            return;
        }

        BlockPos pos = ((BlockHitResult) ray).getBlockPos();
        BlockState state = MC.level.getBlockState(pos);

        boolean isMining = MC.options.keyAttack.isDown();
        int currentSlot = MC.player.getInventory().getSelectedSlot();
        int bestSlot = findFastestTool(state).slot();

        if (miningOnly.getValue()) {
            if (isMining) {
                if (bestSlot >= 0 && bestSlot <= 9 && bestSlot != currentSlot) {
                    swap(bestSlot, swapBack.getValue());
                }
            } else {
                if (swapBack.getValue()) swapBack();
            }
        } else {
            if (bestSlot >= 0 && bestSlot <= 9 && bestSlot != currentSlot) {
                swap(bestSlot, swapBack.getValue());
            }
        }

        if (autoToggle.getValue()) {
            if (AOBA_CLIENT.moduleManager.killaura.getStatus().equals("Enabled")
                    || AOBA_CLIENT.moduleManager.bedAura.getStatus().equals("Enabled")
                    || AOBA_CLIENT.moduleManager.maceaura.getStatus().equals("Enabled")
                    || AOBA_CLIENT.moduleManager.autocrystal.getStatus().equals("Enabled")
                    || AOBA_CLIENT.moduleManager.autoanchor.getStatus().equals("Enabled")) {

                this.toggle();
            }
        }
    }

    @Override
    public void onTick(Post event) {

    }

    @Override
    public void onBlockStateChanged(BlockStateEvent event) {

    }
}
