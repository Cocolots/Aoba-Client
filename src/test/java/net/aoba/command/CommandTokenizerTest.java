package net.aoba.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class CommandTokenizerTest {
	@Test
	void splitsWhitespaceWithoutEmptyTokens() throws Exception {
		assertThat(CommandTokenizer.tokenize("  help\t module\n toggle ")).containsExactly("help", "module", "toggle");
	}

	@Test
	void preservesQuotedWhitespaceAndEmptyArguments() throws Exception {
		assertThat(CommandTokenizer.tokenize("say \"hello world\" '' last"))
				.containsExactly("say", "hello world", "", "last");
	}

	@Test
	void supportsEscapedQuotesAndBackslashes() throws Exception {
		assertThat(CommandTokenizer.tokenize("say \"a \\\"quoted\\\" value\" C:\\\\temp"))
				.containsExactly("say", "a \"quoted\" value", "C:\\temp");
	}

	@Test
	void rejectsUnclosedQuotesAndTrailingEscapes() {
		assertThatThrownBy(() -> CommandTokenizer.tokenize("say \"unclosed"))
				.isInstanceOf(CommandParseException.class).hasMessageContaining("Unclosed");
		assertThatThrownBy(() -> CommandTokenizer.tokenize("say trailing\\"))
				.isInstanceOf(CommandParseException.class).hasMessageContaining("Trailing escape");
	}

	@Test
	void incompleteTokenizerKeepsTrailingInput() {
		assertThat(CommandTokenizer.tokenizeIncomplete("say \"open value"))
				.containsExactly("say", "open value");
		assertThat(CommandTokenizer.tokenizeIncomplete("say trailing\\")).containsExactly("say", "trailing\\");
	}

	@Test
	void prefixRequiresTokenBoundary() {
		assertThat(CommandTokenizer.removePrefix(".aoba help", ".aoba")).contains("help");
		assertThat(CommandTokenizer.removePrefix(".aoba", ".aoba")).contains("");
		assertThat(CommandTokenizer.removePrefix(".aobahelp", ".aoba")).isEmpty();
		assertThat(CommandTokenizer.removePrefix(".other help", ".aoba")).isEmpty();
	}

	@Test
	void tokenStartIgnoresWhitespaceInsideQuotes() {
		String input = "say \"hello world\" next";

		assertThat(CommandTokenizer.findTokenStart(input, input.indexOf("world"))).isEqualTo(4);
		assertThat(CommandTokenizer.findTokenStart(input, input.length())).isEqualTo(input.indexOf("next"));
	}
}
