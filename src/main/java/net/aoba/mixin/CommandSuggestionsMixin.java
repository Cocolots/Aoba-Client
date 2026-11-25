/*
 * Aoba Hacked Client
 * Copyright (C) 2019-2024 coltonk9043
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, Version 3 or later.
 * See <http://www.gnu.org/licenses/>.
 */

package net.aoba.mixin;

import java.util.concurrent.CompletableFuture;

import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import net.aoba.Aoba;
import net.aoba.command.CommandSuggester;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CommandSuggestions.class)
public abstract class CommandSuggestionsMixin {
	@Shadow
	private EditBox input;
	@Shadow
	private CompletableFuture<Suggestions> pendingSuggestions;

	@Shadow
	public abstract void showSuggestions(boolean narrateFirstSuggestion);

	@Inject(at = {
			@At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/EditBox;getCursorPosition()I", ordinal = 0) }, method = "updateCommandInfo()V", cancellable = true)
	private void aoba$updateSuggestions(CallbackInfo ci) {
		if (Aoba.getInstance().commandManager == null) {
			return;
		}

		String value = input.getValue();
		int cursor = input.getCursorPosition();
		CommandSuggester.Result result = Aoba.getInstance().commandManager.suggest(value, cursor);
		if (result.suggestions().isEmpty()) {
			return;
		}

		String beforeCursor = value.substring(0, cursor);
		SuggestionsBuilder builder = new SuggestionsBuilder(beforeCursor, result.replacementStart());
		for (String suggestion : result.suggestions()) {
			builder.suggest(suggestion);
		}
		pendingSuggestions = builder.buildFuture();
		showSuggestions(false);
		ci.cancel();
	}
}
