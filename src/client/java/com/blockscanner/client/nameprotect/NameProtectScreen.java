package com.blockscanner.client.nameprotect;

import com.blockscanner.client.ScannerConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

public final class NameProtectScreen {
	private NameProtectScreen() {
	}

	public static Button createToggle(int x, int y) {
		return Button.builder(toggleLabel(), button -> {
			ScannerConfig.get().nameProtectEnabled = !ScannerConfig.get().nameProtectEnabled;
			button.setMessage(toggleLabel());
			ScannerConfig.save();
		}).bounds(x, y, 128, 20).build();
}

	public static EditBox createAliasField(Font renderer, int x, int y, int width) {
		EditBox alias = new EditBox(renderer, x, y, width, 20,
				Component.literal("Protected display name"));
		alias.setMaxLength(16);
		alias.setHint(Component.literal("Type a pseudonym (max 16 characters)"));
		alias.setValue(ScannerConfig.get().protectedName);
		alias.setResponder(value -> {
			ScannerConfig.get().protectedName = value;
			ScannerConfig.save();
		});
		return alias;
	}

	public static void renderHeader(GuiGraphicsExtractor context, Font renderer, int x, int y) {
		context.text(renderer, Component.literal("NAME PROTECT  /  LOCAL DISPLAY ONLY"), x, y, 0xFF42F56C);
	}

	private static Component toggleLabel() {
		return Component.literal("Name Protect: " + (ScannerConfig.get().nameProtectEnabled ? "ON" : "OFF"));
	}
}