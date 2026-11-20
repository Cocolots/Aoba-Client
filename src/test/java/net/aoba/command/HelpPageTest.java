package net.aoba.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

class HelpPageTest {
	private final List<Integer> source = List.of(1, 2, 3, 4, 5, 6, 7);

	@Test
	void returnsRequestedPageWithoutOffByOne() {
		HelpPage<Integer> page = HelpPage.of(source, 2, 3);

		assertThat(page.entries()).containsExactly(4, 5, 6);
		assertThat(page.page()).isEqualTo(2);
		assertThat(page.pageCount()).isEqualTo(3);
		assertThat(page.totalEntries()).isEqualTo(7);
	}

	@Test
	void lastPageContainsOnlyRemainingEntries() {
		assertThat(HelpPage.of(source, 3, 3).entries()).containsExactly(7);
	}

	@Test
	void requestedPageIsClampedToAvailablePages() {
		assertThat(HelpPage.of(source, 0, 3).page()).isEqualTo(1);
		assertThat(HelpPage.of(source, 99, 3).page()).isEqualTo(3);
	}

	@Test
	void emptySourceHasOneEmptyPage() {
		HelpPage<Integer> page = HelpPage.of(List.of(), 1, 5);

		assertThat(page.page()).isEqualTo(1);
		assertThat(page.pageCount()).isEqualTo(1);
		assertThat(page.entries()).isEmpty();
	}

	@Test
	void pageEntriesAreImmutable() {
		HelpPage<Integer> page = HelpPage.of(source, 1, 3);

		assertThatThrownBy(() -> page.entries().add(8)).isInstanceOf(UnsupportedOperationException.class);
		assertThatThrownBy(() -> HelpPage.of(source, 1, 0)).isInstanceOf(IllegalArgumentException.class);
	}
}
