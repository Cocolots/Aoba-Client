package net.aoba.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class CommandHistory {
	private final int maximumSize;
	private final List<String> entries = new ArrayList<>();

	public CommandHistory(int maximumSize) {
		if (maximumSize < 1) {
			throw new IllegalArgumentException("History size must be positive");
		}
		this.maximumSize = maximumSize;
	}

	public void add(String command) {
		if (command == null || command.isBlank()) {
			return;
		}
		entries.add(command);
		if (entries.size() > maximumSize) {
			entries.remove(0);
		}
	}

	public List<String> getEntries() {
		return Collections.unmodifiableList(new ArrayList<>(entries));
	}

	public void clear() {
		entries.clear();
	}

	public int size() {
		return entries.size();
	}
}
