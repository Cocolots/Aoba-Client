package net.aoba.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Case-insensitive command and alias registry with deterministic ordering. */
public final class CommandRegistry {
	private final Map<String, Command> commands = new LinkedHashMap<>();
	private final Map<String, String> aliases = new LinkedHashMap<>();

	public void register(Command command, String... aliases) {
		if (command == null) {
			throw new IllegalArgumentException("command is required");
		}
		String name = normalize(command.getName());
		if (commands.containsKey(name) || this.aliases.containsKey(name)) {
			throw new IllegalArgumentException("Duplicate command: " + name);
		}

		List<String> normalizedAliases = new ArrayList<>();
		if (aliases != null) {
			for (String alias : aliases) {
				String normalized = normalize(alias);
				if (normalized.equals(name) || normalizedAliases.contains(normalized)) {
					continue;
				}
				if (commands.containsKey(normalized) || this.aliases.containsKey(normalized)) {
					throw new IllegalArgumentException("Duplicate command alias: " + normalized);
				}
				normalizedAliases.add(normalized);
			}
		}

		commands.put(name, command);
		for (String alias : normalizedAliases) {
			this.aliases.put(alias, name);
		}
	}

	public boolean registerIfAbsent(Command command, String... aliases) {
		String name = normalize(command.getName());
		if (find(name).isPresent()) {
			return false;
		}
		register(command, aliases);
		return true;
	}

	public Optional<Command> find(String name) {
		if (name == null) {
			return Optional.empty();
		}
		String normalized = name.toLowerCase(Locale.ROOT);
		String canonical = aliases.getOrDefault(normalized, normalized);
		return Optional.ofNullable(commands.get(canonical));
	}

	public Map<String, Command> getCommands() {
		return Collections.unmodifiableMap(commands);
	}

	public List<String> getNames(boolean includeAliases) {
		List<String> names = new ArrayList<>(commands.keySet());
		if (includeAliases) {
			names.addAll(aliases.keySet());
		}
		Collections.sort(names);
		return names;
	}

	public int size() {
		return commands.size();
	}

	public void clear() {
		commands.clear();
		aliases.clear();
	}

	private static String normalize(String name) {
		if (name == null) {
			throw new IllegalArgumentException("Command name is required");
		}
		String normalized = name.strip().toLowerCase(Locale.ROOT);
		if (normalized.isEmpty() || normalized.chars().anyMatch(Character::isWhitespace)) {
			throw new IllegalArgumentException("Invalid command name: " + name);
		}
		return normalized;
	}
}
