package net.aoba.utils.discord;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RPCManagerTest {
	@Test
	void rejectsBundledX86LibraryOnAppleSilicon() {
		assertThat(RPCManager.isNativeLibrarySupported("Mac OS X", "aarch64")).isFalse();
		assertThat(RPCManager.isNativeLibrarySupported("macOS", "arm64")).isFalse();
	}

	@Test
	void allowsPlatformsWhoseNativeLibraryCanBeLoaded() {
		assertThat(RPCManager.isNativeLibrarySupported("Mac OS X", "x86_64")).isTrue();
		assertThat(RPCManager.isNativeLibrarySupported("Windows 11", "amd64")).isTrue();
		assertThat(RPCManager.isNativeLibrarySupported("Linux", "amd64")).isTrue();
	}
}
