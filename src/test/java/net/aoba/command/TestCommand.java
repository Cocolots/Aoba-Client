package net.aoba.command;

final class TestCommand extends Command {
	private final String[] suggestions;
	String[] receivedArguments;

	TestCommand(String name, String... suggestions) {
		super(name, "Test command", "[args]");
		this.suggestions = suggestions;
	}

	@Override
	public void runCommand(String[] parameters) {
		receivedArguments = parameters;
	}

	@Override
	public String[] getAutocorrect(String previousParameter) {
		return suggestions;
	}
}
