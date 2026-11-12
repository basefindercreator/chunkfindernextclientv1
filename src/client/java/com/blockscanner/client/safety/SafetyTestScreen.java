package com.blockscanner.client.safety;

import com.blockscanner.client.BlockScannerClient;
import com.blockscanner.client.ScannerConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class SafetyTestScreen extends Screen {
	private final Screen parent;

	public SafetyTestScreen(Screen parent) {
		super(Component.literal("Safety Alert Tests"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int centerX = width / 2;
		int top = height / 2 - 46;
		addRenderableWidget(Button.builder(modeLabel(), button -> {
			ScannerConfig.get().safetyTestMode = !ScannerConfig.get().safetyTestMode;
			button.setMessage(modeLabel());
			ScannerConfig.save();
		}).bounds(centerX - 115, top, 230, 20).build());
		boolean inSinglePlayer = BlockScannerClient.isLocalWorld(Minecraft.getInstance());
		Button antiCheat = Button.builder(Component.literal("Simulate anti-cheat alert"), button ->
				AntiCheatMonitor.simulateFlag(Minecraft.getInstance()))
				.bounds(centerX - 115, top + 28, 230, 20).build();
		antiCheat.active = inSinglePlayer;
		addRenderableWidget(antiCheat);
		Button staffStart = Button.builder(Component.literal("Simulate staff watching"), button ->
				StaffTracker.simulateWatching(Minecraft.getInstance()))
				.bounds(centerX - 115, top + 56, 230, 20).build();
		staffStart.active = inSinglePlayer;
		addRenderableWidget(staffStart);
		Button staffStop = Button.builder(Component.literal("Simulate staff stopped"), button ->
				StaffTracker.simulateStopped(Minecraft.getInstance()))
				.bounds(centerX - 115, top + 84, 230, 20).build();
		staffStop.active = inSinglePlayer;
		addRenderableWidget(staffStop);
		addRenderableWidget(Button.builder(Component.literal("Back"), button -> onClose())
				.bounds(centerX - 115, top + 114, 230, 20).build());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
		extractBackground(context, mouseX, mouseY, delta);
		int left = width / 2 - 140;
		int top = height / 2 - 78;
		context.fill(left, top, left + 280, top + 222, 0xF0131C22);
		context.fill(left, top, left + 280, top + 3, 0xFFFFB84D);
		context.centeredText(font, title, width / 2, top + 12, 0xFFFFD18B);
		context.centeredText(font, Component.literal("LOCAL SIMULATION ONLY - NO SERVER MONITORING"),
				width / 2, top + 29, 0xFFB7C0C4);
		context.centeredText(font, Component.literal("Alerts are synthetic and require an integrated world."),
				width / 2, top + 42, 0xFF87959B);
		super.extractRenderState(context, mouseX, mouseY, delta);
	}

	@Override
	public void onClose() {
		Minecraft.getInstance().gui.setScreen(parent);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private static Component modeLabel() {
		return Component.literal("Safety test mode: " + (ScannerConfig.get().safetyTestMode ? "ON" : "OFF"));
	}
}