package com.blockscanner.client.nameprotect;

import com.blockscanner.client.ScannerConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.Text;

public final class NameProtectScreen {
	private NameProtectScreen() {
	}

	public static ButtonWidget createToggle(int x, int y) {
		return ButtonWidget.builder(toggleLabel(), button -> {
			ScannerConfig.get().nameProtectEnabled = !ScannerConfig.get().nameProtectEnabled;
			button.setMessage(toggleLabel());
			ScannerConfig.save();
		}).dimensions(x, y, 128, 20).build();
}

	public static TextFieldWidget createAliasField(TextRenderer renderer, int x, int y, int width) {
		TextFieldWidget alias = new TextFieldWidget(renderer, x, y, width, 20,
				Text.literal("Protected display name"));
		alias.setMaxLength(16);
		alias.setPlaceholder(Text.literal("Type a pseudonym (max 16 characters)"));
		alias.setText(ScannerConfig.get().protectedName);
		alias.setChangedListener(value -> {
			ScannerConfig.get().protectedName = value;
			ScannerConfig.save();
		});
		return alias;
	}

	public static void renderHeader(DrawContext context, TextRenderer renderer, int x, int y) {
		context.drawTextWithShadow(renderer, Text.literal("NAME PROTECT  /  LOCAL DISPLAY ONLY"), x, y, 0xFF42F56C);
	}

	private static Text toggleLabel() {
		return Text.literal("Name Protect: " + (ScannerConfig.get().nameProtectEnabled ? "ON" : "OFF"));
	}
}