package com.blockscanner.client.safety;

import com.blockscanner.client.BlockScannerClient;
import com.blockscanner.client.ScannerConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public final class SafetyTestScreen extends Screen {
	private final Screen parent;

	public SafetyTestScreen(Screen parent) {
		super(Text.literal("Safety Alert Tests"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int centerX = width / 2;
		int top = height / 2 - 46;
		addDrawableChild(ButtonWidget.builder(modeLabel(), button -> {
			ScannerConfig.get().safetyTestMode = !ScannerConfig.get().safetyTestMode;
			button.setMessage(modeLabel());
			ScannerConfig.save();
		}).dimensions(centerX - 115, top, 230, 20).build());
		boolean inSinglePlayer = BlockScannerClient.isLocalWorld(MinecraftClient.getInstance());
		ButtonWidget antiCheat = ButtonWidget.builder(Text.literal("Simulate anti-cheat alert"), button ->
				AntiCheatMonitor.simulateFlag(MinecraftClient.getInstance()))
				.dimensions(centerX - 115, top + 28, 230, 20).build();
		antiCheat.active = inSinglePlayer;
		addDrawableChild(antiCheat);
		ButtonWidget staffStart = ButtonWidget.builder(Text.literal("Simulate staff watching"), button ->
				StaffTracker.simulateWatching(MinecraftClient.getInstance()))
				.dimensions(centerX - 115, top + 56, 230, 20).build();
		staffStart.active = inSinglePlayer;
		addDrawableChild(staffStart);
		ButtonWidget staffStop = ButtonWidget.builder(Text.literal("Simulate staff stopped"), button ->
				StaffTracker.simulateStopped(MinecraftClient.getInstance()))
				.dimensions(centerX - 115, top + 84, 230, 20).build();
		staffStop.active = inSinglePlayer;
		addDrawableChild(staffStop);
		addDrawableChild(ButtonWidget.builder(Text.literal("Back"), button -> close())
				.dimensions(centerX - 115, top + 114, 230, 20).build());
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		renderBackground(context, mouseX, mouseY, delta);
		int left = width / 2 - 140;
		int top = height / 2 - 78;
		context.fill(left, top, left + 280, top + 222, 0xF0131C22);
		context.fill(left, top, left + 280, top + 3, 0xFFFFB84D);
		context.drawCenteredTextWithShadow(textRenderer, title, width / 2, top + 12, 0xFFFFD18B);
		context.drawCenteredTextWithShadow(textRenderer, Text.literal("LOCAL SIMULATION ONLY - NO SERVER MONITORING"),
				width / 2, top + 29, 0xFFB7C0C4);
		context.drawCenteredTextWithShadow(textRenderer, Text.literal("Alerts are synthetic and require an integrated world."),
				width / 2, top + 42, 0xFF87959B);
		super.render(context, mouseX, mouseY, delta);
	}

	@Override
	public void close() {
		MinecraftClient.getInstance().setScreen(parent);
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	private static Text modeLabel() {
		return Text.literal("Safety test mode: " + (ScannerConfig.get().safetyTestMode ? "ON" : "OFF"));
	}
}