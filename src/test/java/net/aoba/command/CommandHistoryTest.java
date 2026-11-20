package net.aoba.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class CommandHistoryTest {
	@Test
	void keepsNewestEntriesWithinLimit() {
		CommandHistory history = new CommandHistory(3);

		for (int index = 1; index <= 5; index++) {
			history.add("command " + index);
		}

		assertThat(history.getEntries()).containsExactly("command 3", "command 4", "command 5");
	}

	@Test
	void ignoresBlankEntries() {
		CommandHistory history = new CommandHistory(3);
		history.add(null);
		history.add(" ");
		history.add("valid");

		assertThat(history.getEntries()).containsExactly("valid");
	}

	@Test
	void returnedSnapshotIsIndependentAndImmutable() {
		CommandHistory history = new CommandHistory(3);
		history.add("first");
		var snapshot = history.getEntries();
		history.add("second");

		assertThat(snapshot).containsExactly("first");
		assertThatThrownBy(() -> snapshot.add("third")).isInstanceOf(UnsupportedOperationException.class);
	}

	@Test
	void clearAndSizeValidationWork() {
		CommandHistory history = new CommandHistory(2);
		history.add("entry");
		history.clear();

		assertThat(history.size()).isZero();
		assertThatThrownBy(() -> new CommandHistory(0)).isInstanceOf(IllegalArgumentException.class);
	}
}
