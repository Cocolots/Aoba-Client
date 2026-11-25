package net.aoba.command;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Cursor-aware command completion with deterministic replacement ranges. */
public final class CommandSuggester {
	public record Result(int replacementStart, List<String> suggestions) {
		public Result {
			suggestions = List.copyOf(suggestions);
		}

		static Result empty(int replacementStart) {
			return new Result(replacementStart, List.of());
		}
	}

	private CommandSuggester() {
	}

	public static Result suggest(String input, int cursor, String prefix, CommandRegistry registry) {
		if (input == null || registry == null || prefix == null || prefix.isEmpty()) {
			return Result.empty(0);
		}
		int safeCursor = Math.max(0, Math.min(cursor, input.length()));
		String beforeCursor = input.substring(0, safeCursor);
		if (beforeCursor.length() <= prefix.length()) {
			return prefix.startsWith(beforeCursor) ? new Result(0, List.of(prefix + " "))
					: Result.empty(safeCursor);
		}

		var bodyOptional = CommandTokenizer.removePrefix(beforeCursor, prefix);
		if (bodyOptional.isEmpty()) {
			return Result.empty(safeCursor);
		}
		String body = bodyOptional.get();
		int bodyOffset = beforeCursor.length() - body.length();
		int firstSpace = indexOfWhitespace(body);
		if (firstSpace < 0) {
			String partial = body.toLowerCase(Locale.ROOT);
			List<String> matches = registry.getNames(true).stream()
					.filter(name -> name.startsWith(partial)).map(name -> name + " ").toList();
			return new Result(bodyOffset, matches);
		}

		String commandName = body.substring(0, firstSpace);
		var commandOptional = registry.find(commandName);
		if (commandOptional.isEmpty()) {
			return Result.empty(bodyOffset);
		}

		int tokenStart = CommandTokenizer.findTokenStart(body, body.length());
		String currentToken = body.substring(tokenStart);
		String normalizedToken = currentToken.toLowerCase(Locale.ROOT);
		String[] candidates = commandOptional.get().getAutocorrect(currentToken);
		if (candidates == null) {
			return Result.empty(bodyOffset + tokenStart);
		}

		Set<String> matches = new LinkedHashSet<>();
		for (String candidate : candidates) {
			if (candidate != null && candidate.toLowerCase(Locale.ROOT).startsWith(normalizedToken)) {
				matches.add(quoteIfNeeded(candidate));
			}
		}
		return new Result(bodyOffset + tokenStart, new ArrayList<>(matches));
	}

	private static int indexOfWhitespace(String value) {
		for (int index = 0; index < value.length(); index++) {
			if (Character.isWhitespace(value.charAt(index))) {
				return index;
			}
		}
		return -1;
	}

	private static String quoteIfNeeded(String value) {
		if (value.chars().noneMatch(Character::isWhitespace)) {
			return value;
		}
		return '"' + value.replace("\\", "\\\\").replace("\"", "\\\"") + '"';
	}
}
