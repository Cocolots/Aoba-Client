package net.aoba.utils.input;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TextEditingStateTest {
	private TextEditingState state;

	@BeforeEach
	void setUp() {
		state = new TextEditingState();
		state.setText("aoba");
	}

	@Test
	void insertsAtCollapsedCaretInsteadOfAppending() {
		state.moveCaret(2, false);

		state.insert("-client");

		assertThat(state.getText()).isEqualTo("ao-clientba");
		assertThat(state.getCaret()).isEqualTo(9);
	}

	@Test
	void tracksForwardAndBackwardSelections() {
		state.startSelection(1);
		state.dragSelection(3);
		assertThat(state.getSelectionStart()).isEqualTo(1);
		assertThat(state.getSelectionEnd()).isEqualTo(3);
		assertThat(state.getSelectedText()).isEqualTo("ob");

		state.startSelection(4);
		state.dragSelection(1);
		assertThat(state.getSelectionStart()).isEqualTo(1);
		assertThat(state.getSelectionEnd()).isEqualTo(4);
		assertThat(state.getSelectedText()).isEqualTo("oba");
	}

	@Test
	void copyDoesNotMutateAndCutRemovesOnlySelection() {
		state.startSelection(1);
		state.dragSelection(3);

		assertThat(state.copy()).isEqualTo("ob");
		assertThat(state.getText()).isEqualTo("aoba");
		assertThat(state.cut()).isEqualTo("ob");
		assertThat(state.getText()).isEqualTo("aa");
		assertThat(state.getCaret()).isEqualTo(1);
		assertThat(state.hasSelection()).isFalse();
	}

	@Test
	void pasteReplacesSelectionAtEitherDirection() {
		state.startSelection(3);
		state.dragSelection(1);

		state.insert("O");

		assertThat(state.getText()).isEqualTo("aOa");
		assertThat(state.getCaret()).isEqualTo(2);
	}

	@Test
	void backspaceAndDeleteRespectCaretAndSelection() {
		state.moveCaret(2, false);
		state.backspace();
		assertThat(state.getText()).isEqualTo("aba");
		state.delete();
		assertThat(state.getText()).isEqualTo("aa");

		state.selectAll();
		state.backspace();
		assertThat(state.getText()).isEmpty();
		assertThat(state.getCaret()).isZero();
	}

	@Test
	void selectionMovementAndPositionsAreClamped() {
		state.moveCaret(1, false);
		state.moveCaretBy(2, true);
		assertThat(state.getSelectedText()).isEqualTo("ob");

		state.moveCaretBy(100, true);
		assertThat(state.getSelectionEnd()).isEqualTo(4);
		state.moveCaret(100, false);
		assertThat(state.getCaret()).isEqualTo(4);
		assertThat(state.hasSelection()).isFalse();
		state.moveCaret(-1, false);
		assertThat(state.getCaret()).isZero();
	}
}
