package net.aoba.gametest;

import net.aoba.Aoba;
import net.aoba.module.modules.movement.Speed;
import net.aoba.module.modules.movement.Step;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;

@SuppressWarnings("UnstableApiUsage")
public final class AobaClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
			singleplayer.getConnection().waitForChunksRender();
			context.runOnClient(AobaClientGameTest::verifyMovementModuleLifecycle);
			context.takeScreenshot("aoba-singleplayer-smoke");
		}
	}

	private static void verifyMovementModuleLifecycle(Minecraft minecraft) {
		LocalPlayer player = minecraft.player;
		if (player == null || Aoba.getInstance() == null || Aoba.getInstance().moduleManager == null) {
			throw new AssertionError("Aoba and the local player must be initialized in the game-test world");
		}

		Step step = Aoba.getInstance().moduleManager.step;
		Speed speed = Aoba.getInstance().moduleManager.speed;
		try {
			step.setStepHeight(1.5f);
			step.onEnable();
			assertBaseValue(player, Attributes.STEP_HEIGHT, 1.5, "Step enable");

			speed.onEnable();
			assertOptionValue(minecraft.options.fovEffectScale().get(), 0.0, "Speed enable FOV scale");
			speed.onDisable();
			assertOptionValue(minecraft.options.fovEffectScale().get(), 1.0, "Speed disable FOV scale");

			step.onDisable();
			assertBaseValue(player, Attributes.STEP_HEIGHT, 0.5, "Step disable");
		} finally {
			speed.onDisable();
			step.onDisable();
			step.setStepHeight(1.0f);
		}
	}

	private static void assertBaseValue(LocalPlayer player, Holder<Attribute> attribute, double expected,
			String operation) {
		double actual = player.getAttribute(attribute).getBaseValue();
		if (Math.abs(actual - expected) > 1.0E-6) {
			throw new AssertionError(operation + " expected " + expected + " but was " + actual);
		}
	}

	private static void assertOptionValue(double actual, double expected, String operation) {
		if (Math.abs(actual - expected) > 1.0E-6) {
			throw new AssertionError(operation + " expected " + expected + " but was " + actual);
		}
	}
}
