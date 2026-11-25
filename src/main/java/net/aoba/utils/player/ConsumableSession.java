package net.aoba.utils.player;

/** Tracks one item-use action so every exit path can release input and slots. */
public final class ConsumableSession {
	private boolean active;
	private int previousSlot = -1;
	private int itemSlot = -1;
	private long startedAtMs;

	public boolean start(int selectedSlot, int itemSlot, long nowMs) {
		if (active || itemSlot < 0) {
			return false;
		}
		active = true;
		previousSlot = selectedSlot;
		this.itemSlot = itemSlot;
		startedAtMs = nowMs;
		return true;
	}

	public boolean isExpired(long nowMs, long timeoutMs) {
		return active && timeoutMs >= 0 && nowMs >= startedAtMs && nowMs - startedAtMs >= timeoutMs;
	}

	public int stop() {
		int slot = previousSlot;
		clear();
		return slot;
	}

	public void clear() {
		active = false;
		previousSlot = -1;
		itemSlot = -1;
		startedAtMs = 0;
	}

	public boolean isActive() {
		return active;
	}

	public int getPreviousSlot() {
		return previousSlot;
	}

	public int getItemSlot() {
		return itemSlot;
	}

	public long getStartedAtMs() {
		return startedAtMs;
	}
}
