package net.aoba.module.modules.misc;

final class AutoEatPlanner {
	private AutoEatPlanner() {
	}

	static boolean shouldStart(int foodLevel, float health, float hungerThreshold, float healthThreshold) {
		return foodLevel <= hungerThreshold || health <= healthThreshold;
	}

	static boolean shouldContinue(int foodLevel, float hungerThreshold, boolean fillToFull) {
		return fillToFull ? foodLevel < 20 : foodLevel <= hungerThreshold;
	}

	static int foodScore(int nutrition, boolean goldenApple, boolean enchantedGoldenApple, boolean prioritizeApples) {
		if (!prioritizeApples) {
			return nutrition;
		}
		if (enchantedGoldenApple) {
			return 2_000_000 + nutrition;
		}
		if (goldenApple) {
			return 1_000_000 + nutrition;
		}
		return nutrition;
	}
}
