package net.aoba.utils.input;

/**
 * Framework-independent caret and selection model for single-line text input.
 */
public final class TextEditingState {
	private String text = "";
	private int caret;
	private int selectionAnchor = -1;

	public String getText() {
		return text;
	}

	public void setText(String value) {
		text = value == null ? "" : value;
		caret = text.length();
		selectionAnchor = -1;
	}

	public int getCaret() {
		return caret;
	}

	public int getSelectionStart() {
		return selectionAnchor == -1 ? caret : Math.min(selectionAnchor, caret);
	}

	public int getSelectionEnd() {
		return selectionAnchor == -1 ? caret : Math.max(selectionAnchor, caret);
	}

	public boolean hasSelection() {
		return selectionAnchor != -1 && selectionAnchor != caret;
	}

	public String getSelectedText() {
		return hasSelection() ? text.substring(getSelectionStart(), getSelectionEnd()) : "";
	}

	public void selectAll() {
		selectionAnchor = 0;
		caret = text.length();
	}

	public void clearSelection() {
		selectionAnchor = -1;
	}

	public void startSelection(int position) {
		caret = clamp(position);
		selectionAnchor = caret;
	}

	public void dragSelection(int position) {
		if (selectionAnchor == -1) {
			selectionAnchor = caret;
		}
		caret = clamp(position);
	}

	public void moveCaret(int position, boolean selecting) {
		if (selecting && selectionAnchor == -1) {
			selectionAnchor = caret;
		}
		caret = clamp(position);
		if (!selecting) {
			selectionAnchor = -1;
		}
	}

	public void moveCaretBy(int delta, boolean selecting) {
		moveCaret(caret + delta, selecting);
	}

	public void insert(String value) {
		if (value == null || value.isEmpty()) {
			return;
		}

		int start = getSelectionStart();
		int end = getSelectionEnd();
		text = text.substring(0, start) + value + text.substring(end);
		caret = start + value.length();
		selectionAnchor = -1;
	}

	public String copy() {
		return getSelectedText();
	}

	public String cut() {
		String selected = getSelectedText();
		if (hasSelection()) {
			deleteSelection();
		}
		return selected;
	}

	public void deleteSelection() {
		if (!hasSelection()) {
			return;
		}
		int start = getSelectionStart();
		text = text.substring(0, start) + text.substring(getSelectionEnd());
		caret = start;
		selectionAnchor = -1;
	}

	public void backspace() {
		if (hasSelection()) {
			deleteSelection();
		} else if (caret > 0) {
			text = text.substring(0, caret - 1) + text.substring(caret);
			caret--;
		}
	}

	public void delete() {
		if (hasSelection()) {
			deleteSelection();
		} else if (caret < text.length()) {
			text = text.substring(0, caret) + text.substring(caret + 1);
		}
	}

	private int clamp(int position) {
		return Math.max(0, Math.min(text.length(), position));
	}
}
