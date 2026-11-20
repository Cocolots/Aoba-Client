package net.aoba.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record HelpPage<T>(List<T> entries, int page, int pageCount, int totalEntries) {
	public HelpPage {
		entries = Collections.unmodifiableList(new ArrayList<>(entries));
	}

	public static <T> HelpPage<T> of(List<T> source, int requestedPage, int pageSize) {
		if (source == null) {
			throw new IllegalArgumentException("source is required");
		}
		if (pageSize < 1) {
			throw new IllegalArgumentException("pageSize must be positive");
		}

		int pageCount = Math.max(1, (source.size() + pageSize - 1) / pageSize);
		int page = Math.max(1, Math.min(requestedPage, pageCount));
		int from = Math.min((page - 1) * pageSize, source.size());
		int to = Math.min(from + pageSize, source.size());
		return new HelpPage<>(source.subList(from, to), page, pageCount, source.size());
	}
}
