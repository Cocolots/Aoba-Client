package net.aoba.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CommandRegistryTest {
	private CommandRegistry registry;

	@BeforeEach
	void setUp() {
		registry = new CommandRegistry();
	}

	@Test
	void findsCommandsAndAliasesCaseInsensitively() {
		TestCommand command = new TestCommand("Help");
		registry.register(command, "h", "?");

		assertThat(registry.find("help")).contains(command);
		assertThat(registry.find("HELP")).contains(command);
		assertThat(registry.find("H")).contains(command);
		assertThat(registry.find("?")).contains(command);
		assertThat(registry.find("missing")).isEmpty();
	}

	@Test
	void rejectsDuplicateNamesAndAliases() {
		registry.register(new TestCommand("help"), "h");

		assertThatThrownBy(() -> registry.register(new TestCommand("HELP")))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Duplicate command");
		assertThatThrownBy(() -> registry.register(new TestCommand("other"), "H"))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Duplicate command alias");
	}

	@Test
	void registrationIsAtomicWhenAliasValidationFails() {
		registry.register(new TestCommand("help"), "h");

		assertThatThrownBy(() -> registry.register(new TestCommand("other"), "valid", "h"))
				.isInstanceOf(IllegalArgumentException.class);
		assertThat(registry.find("other")).isEmpty();
		assertThat(registry.find("valid")).isEmpty();
	}

	@Test
	void namesAreSortedAndAliasesOptional() {
		registry.register(new TestCommand("zeta"), "z");
		registry.register(new TestCommand("alpha"), "a");

		assertThat(registry.getNames(false)).containsExactly("alpha", "zeta");
		assertThat(registry.getNames(true)).containsExactly("a", "alpha", "z", "zeta");
	}

	@Test
	void commandMapCannotBeModified() {
		registry.register(new TestCommand("help"));

		assertThatThrownBy(() -> registry.getCommands().clear()).isInstanceOf(UnsupportedOperationException.class);
		assertThat(registry.size()).isEqualTo(1);
	}

	@Test
	void registerIfAbsentDoesNotReplaceExistingCommand() {
		TestCommand original = new TestCommand("help");
		registry.register(original);

		assertThat(registry.registerIfAbsent(new TestCommand("HELP"))).isFalse();
		assertThat(registry.find("help")).contains(original);
	}

	@Test
	void rejectsInvalidNames() {
		assertThatThrownBy(() -> registry.register(new TestCommand("two words")))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> registry.register(new TestCommand(" ")))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> registry.register(null)).isInstanceOf(IllegalArgumentException.class);
	}
}
