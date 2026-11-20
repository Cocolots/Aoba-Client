package net.aoba.module.modules.misc;

/** Reel/recast state machine with duplicate splash suppression. */
final class AutoFishPlanner {
	enum Action {
		NONE,
		REEL,
		RECAST
	}

	private long recastAtMs = -1;
	private long lastSplashAtMs = Long.MIN_VALUE;

	Action onSplash(long nowMs, long recastDelayMs) {
		if (recastAtMs >= 0 || lastSplashAtMs != Long.MIN_VALUE && nowMs - lastSplashAtMs < 250) {
			return Action.NONE;
		}
		lastSplashAtMs = nowMs;
		recastAtMs = nowMs + Math.max(0, recastDelayMs);
		return Action.REEL;
	}

	Action onTick(long nowMs) {
		if (recastAtMs >= 0 && nowMs >= recastAtMs) {
			recastAtMs = -1;
			return Action.RECAST;
		}
		return Action.NONE;
	}

	void scheduleRecast(long nowMs, long recastDelayMs) {
		if (recastAtMs < 0) {
			recastAtMs = nowMs + Math.max(0, recastDelayMs);
		}
	}

	void reset() {
		recastAtMs = -1;
		lastSplashAtMs = Long.MIN_VALUE;
	}

	boolean isWaitingForRecast() {
		return recastAtMs >= 0;
	}
}
