package net.aoba.command;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CommandSuggesterTest {
	private CommandRegistry registry;

	@BeforeEach
	void setUp() {
		registry = new CommandRegistry();
		registry.register(new TestCommand("help", "on", "off", "fill to full"));
		registry.register(new TestCommand("history", "clear"), "h");
		registry.register(new TestCommand("fly", "on", "off"));
	}

	@Test
	void completesPartialPrefix() {
		CommandSuggester.Result result = CommandSuggester.suggest(".ao", 3, ".aoba", registry);

		assertThat(result.replacementStart()).isZero();
		assertThat(result.suggestions()).containsExactly(".aoba ");
	}

	@Test
	void completesCommandNamesAndAliases() {
		CommandSuggester.Result result = CommandSuggester.suggest(".aoba h", 7, ".aoba", registry);

		assertThat(result.replacementStart()).isEqualTo(6);
		assertThat(result.suggestions()).containsExactly("h ", "help ", "history ");
	}

	@Test
	void completesArgumentsUsingCommandAlias() {
		CommandSuggester.Result result = CommandSuggester.suggest(".aoba h c", 9, ".aoba", registry);

		assertThat(result.replacementStart()).isEqualTo(8);
		assertThat(result.suggestions()).containsExactly("clear");
	}

	@Test
	void filtersArgumentSuggestionsCaseInsensitively() {
		CommandSuggester.Result result = CommandSuggester.suggest(".aoba help O", 12, ".aoba", registry);

		assertThat(result.suggestions()).containsExactly("on", "off");
	}

	@Test
	void quotesSuggestionsContainingWhitespace() {
		CommandSuggester.Result result = CommandSuggester.suggest(".aoba help fill", 15, ".aoba", registry);

		assertThat(result.suggestions()).containsExactly("\"fill to full\"");
	}

	@Test
	void emptyArgumentReturnsAllCandidates() {
		CommandSuggester.Result result = CommandSuggester.suggest(".aoba fly ", 10, ".aoba", registry);

		assertThat(result.suggestions()).containsExactly("on", "off");
		assertThat(result.replacementStart()).isEqualTo(10);
	}

	@Test
	void unknownCommandOrWrongPrefixHasNoSuggestions() {
		assertThat(CommandSuggester.suggest(".aoba unknown value", 18, ".aoba", registry).suggestions()).isEmpty();
		assertThat(CommandSuggester.suggest(".aobafoo help", 12, ".aoba", registry).suggestions()).isEmpty();
	}

	@Test
	void replacementRangeRespectsCursorInsideLongerInput() {
		String input = ".aoba help o trailing";
		int cursor = 12;

		CommandSuggester.Result result = CommandSuggester.suggest(input, cursor, ".aoba", registry);

		assertThat(result.replacementStart()).isEqualTo(11);
		assertThat(result.suggestions()).containsExactly("on", "off");
	}
}
