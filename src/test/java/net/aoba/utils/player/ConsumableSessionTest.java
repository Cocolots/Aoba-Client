package net.aoba.utils.player;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ConsumableSessionTest {
	@Test
	void sessionStartsOnlyOnceAndRestoresOriginalSlot() {
		ConsumableSession session = new ConsumableSession();

		assertThat(session.start(4, 7, 100)).isTrue();
		assertThat(session.start(7, 2, 200)).isFalse();
		assertThat(session.getPreviousSlot()).isEqualTo(4);
		assertThat(session.getItemSlot()).isEqualTo(7);
		assertThat(session.stop()).isEqualTo(4);
		assertThat(session.isActive()).isFalse();
		assertThat(session.getPreviousSlot()).isEqualTo(-1);
	}

	@Test
	void invalidItemSlotDoesNotStartSession() {
		ConsumableSession session = new ConsumableSession();

		assertThat(session.start(4, -1, 100)).isFalse();
		assertThat(session.isActive()).isFalse();
	}

	@Test
	void timeoutUsesElapsedTimeOnly() {
		ConsumableSession session = new ConsumableSession();
		session.start(0, 1, 1000);

		assertThat(session.isExpired(1099, 100)).isFalse();
		assertThat(session.isExpired(1100, 100)).isTrue();
		assertThat(session.isExpired(900, 100)).isFalse();
		assertThat(session.isExpired(2000, -1)).isFalse();
	}

	@Test
	void clearForgetsAllState() {
		ConsumableSession session = new ConsumableSession();
		session.start(2, 5, 1000);

		session.clear();

		assertThat(session.isActive()).isFalse();
		assertThat(session.getItemSlot()).isEqualTo(-1);
		assertThat(session.getPreviousSlot()).isEqualTo(-1);
		assertThat(session.isExpired(5000, 1)).isFalse();
	}
}
