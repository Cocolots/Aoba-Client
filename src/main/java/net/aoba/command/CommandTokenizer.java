package net.aoba.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Shell-like tokenizer with quotes, escapes, and strict prefix boundaries. */
public final class CommandTokenizer {
	private CommandTokenizer() {
	}

	public static List<String> tokenize(String input) throws CommandParseException {
		return tokenize(input, false);
	}

	public static List<String> tokenizeIncomplete(String input) {
		try {
			return tokenize(input, true);
		} catch (CommandParseException impossible) {
			throw new IllegalStateException(impossible);
		}
	}

	private static List<String> tokenize(String input, boolean allowIncomplete) throws CommandParseException {
		List<String> tokens = new ArrayList<>();
		if (input == null || input.isBlank()) {
			return tokens;
		}

		StringBuilder token = new StringBuilder();
		char quote = 0;
		boolean escaped = false;
		boolean tokenStarted = false;
		for (int index = 0; index < input.length(); index++) {
			char current = input.charAt(index);
			if (escaped) {
				token.append(current);
				tokenStarted = true;
				escaped = false;
				continue;
			}
			if (current == '\\') {
				escaped = true;
				tokenStarted = true;
				continue;
			}
			if (quote != 0) {
				if (current == quote) {
					quote = 0;
				} else {
					token.append(current);
				}
				tokenStarted = true;
				continue;
			}
			if (current == '\'' || current == '"') {
				quote = current;
				tokenStarted = true;
				continue;
			}
			if (Character.isWhitespace(current)) {
				if (tokenStarted) {
					tokens.add(token.toString());
					token.setLength(0);
					tokenStarted = false;
				}
				continue;
			}
			token.append(current);
			tokenStarted = true;
		}

		if (escaped && !allowIncomplete) {
			throw new CommandParseException("Trailing escape character");
		}
		if (quote != 0 && !allowIncomplete) {
			throw new CommandParseException("Unclosed " + (quote == '\'' ? "single" : "double") + " quote");
		}
		if (escaped) {
			token.append('\\');
		}
		if (tokenStarted) {
			tokens.add(token.toString());
		}
		return tokens;
	}

	public static Optional<String> removePrefix(String input, String prefix) {
		if (input == null || prefix == null || prefix.isEmpty() || !input.startsWith(prefix)) {
			return Optional.empty();
		}
		if (input.length() > prefix.length() && !Character.isWhitespace(input.charAt(prefix.length()))) {
			return Optional.empty();
		}
		return Optional.of(input.substring(prefix.length()).stripLeading());
	}

	public static int findTokenStart(String input, int cursor) {
		int safeCursor = Math.max(0, Math.min(cursor, input.length()));
		char quote = 0;
		boolean escaped = false;
		int tokenStart = 0;
		for (int index = 0; index < safeCursor; index++) {
			char current = input.charAt(index);
			if (escaped) {
				escaped = false;
				continue;
			}
			if (current == '\\') {
				escaped = true;
				continue;
			}
			if (quote != 0) {
				if (current == quote) {
					quote = 0;
				}
				continue;
			}
			if (current == '\'' || current == '"') {
				quote = current;
				continue;
			}
			if (Character.isWhitespace(current)) {
				tokenStart = index + 1;
			}
		}
		return tokenStart;
	}
}
