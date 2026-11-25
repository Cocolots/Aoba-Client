package net.aoba.module.modules.render;

public final class FocusFpsPolicy {
	private FocusFpsPolicy() {
	}

	public static int resolveLimit(int currentLimit, int configuredLimit, boolean enabled, boolean windowFocused) {
		if (!enabled || windowFocused) {
			return currentLimit;
		}
		return Math.min(currentLimit, Math.max(1, configuredLimit));
	}
}
