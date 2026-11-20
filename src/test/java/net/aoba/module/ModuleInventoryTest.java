package net.aoba.module;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import net.aoba.utils.FindItemResult;

class ModuleInventoryTest {
	private final List<Integer> slots = Collections.nCopies(36, 1);

	@Test
	void fullInventorySearchNeverReadsPastLastSlot() {
		FindItemResult result = Module.findInSlots(slots.size(), 0, slots.size() - 1, slots::get,
				item -> true, Integer::intValue);

		assertThat(result.slot()).isZero();
		assertThat(result.count()).isEqualTo(36);
	}

	@Test
	void searchClampsExternalRangeToInventoryBounds() {
		FindItemResult result = Module.findInSlots(slots.size(), -10, 100, slots::get,
				item -> true, Integer::intValue);

		assertThat(result.count()).isEqualTo(36);
	}

	@Test
	void emptyRangeReturnsNotFound() {
		FindItemResult result = Module.findInSlots(slots.size(), 20, 10, slots::get,
				item -> true, Integer::intValue);

		assertThat(result.found()).isFalse();
		assertThat(result.slot()).isEqualTo(-1);
		assertThat(result.count()).isZero();
	}

	@Test
	void countsEveryMatchingStackAndReturnsFirstSlot() {
		FindItemResult result = Module.findInSlots(slots.size(), 0, slots.size() - 1, slots::get,
				item -> item == 1, Integer::intValue);

		assertThat(result.slot()).isZero();
		assertThat(result.count()).isEqualTo(36);
	}
}
