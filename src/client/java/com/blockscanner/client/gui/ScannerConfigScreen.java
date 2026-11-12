package com.blockscanner.client.gui;

import com.blockscanner.client.BlockScannerClient;
import com.blockscanner.client.ScannerConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public final class ScannerConfigScreen extends Screen {
	private final Screen parent;

	public ScannerConfigScreen(Screen parent) {
		super(Text.literal("BlockScanner Ultimate - Configuration"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int centerX = width / 2;
		int centerY = height / 2;
		addDrawableChild(ButtonWidget.builder(Text.literal("Mod: " + onOff(ScannerConfig.get().enabled)), button -> {
			ScannerConfig.get().enabled = !ScannerConfig.get().enabled;
			button.setMessage(Text.literal("Mod: " + onOff(ScannerConfig.get().enabled)));
			ScannerConfig.save();
		}).dimensions(centerX - 100, centerY - 68, 200, 20).build());
		ButtonWidget freecam = ButtonWidget.builder(Text.literal("Freecam: " + onOff(BlockScannerClient.FREECAM.isActive())),
				button -> {
					MinecraftClient client = MinecraftClient.getInstance();
					if (BlockScannerClient.isLocalWorld(client)) {
						BlockScannerClient.FREECAM.toggle(client);
						button.setMessage(Text.literal("Freecam: " + onOff(BlockScannerClient.FREECAM.isActive())));
					}
				}).dimensions(centerX - 100, centerY - 40, 200, 20).build();
		freecam.active = BlockScannerClient.isLocalWorld(MinecraftClient.getInstance());
		addDrawableChild(freecam);
		addDrawableChild(ButtonWidget.builder(Text.literal("Done"), button -> close())
				.dimensions(centerX - 100, centerY + 36, 200, 20).build());
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		renderBackground(context, mouseX, mouseY, delta);
		context.fill(0, 0, width, height, 0xEE111827);
		context.fill(width / 2 - 118, height / 2 - 96, width / 2 + 118, height / 2 + 86, 0xF017222C);
		context.fill(width / 2 - 118, height / 2 - 96, width / 2 + 118, height / 2 - 93, 0xFF0EA5E9);
		context.drawCenteredTextWithShadow(textRenderer, title, width / 2, height / 2 - 82, 0xFF38BDF8);
		context.drawCenteredTextWithShadow(textRenderer, Text.literal("Single-player test features only"),
				width / 2, height / 2 - 22, 0xFFB7D8E8);
		super.render(context, mouseX, mouseY, delta);
	}

	@Override
	public void close() {
		ScannerConfig.save();
		MinecraftClient.getInstance().setScreen(parent);
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	private static String onOff(boolean enabled) {
		return enabled ? "ON" : "OFF";
	}
}