package com.blockscanner.client.gui;

import com.blockscanner.client.BlockScannerClient;
import com.blockscanner.client.ScannerConfig;
import com.blockscanner.client.safety.SafetyTestScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

public final class ScannerConfigScreen extends Screen {
	private final Screen parent;
	private BlockScannerClient.Module awaitingKey;

	public ScannerConfigScreen(Screen parent) {
		super(Component.literal("BlockScanner Ultimate - Configuration"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int centerX = width / 2;
		int centerY = height / 2;
		int columnWidth = 132;
		int leftX = centerX - columnWidth - 4;
		int rightX = centerX + 4;
		addModuleControls(BlockScannerClient.Module.SCANNER, "Scan", leftX, centerY - 40);
		addEspControls(rightX, centerY - 40);
		addModuleControls(BlockScannerClient.Module.MINIMAP, "Radar", leftX, centerY - 10);
		addModuleControls(BlockScannerClient.Module.ALERTS, "Alerts", rightX, centerY - 10);
		addModuleControls(BlockScannerClient.Module.FREECAM, "Freecam", leftX, centerY + 20);
		addModuleControls(BlockScannerClient.Module.NAME_PROTECT, "Name", rightX, centerY + 20);
		Minecraft client = Minecraft.getInstance();
		addRenderableWidget(Button.builder(Component.literal("Safety Tests"), button ->
				client.gui.setScreen(new SafetyTestScreen(this))).bounds(centerX - 132, centerY + 56, 126, 20).build());
		addRenderableWidget(Button.builder(Component.literal("Done"), button -> onClose())
				.bounds(centerX + 6, centerY + 56, 126, 20).build());
	}

	private void addEspControls(int x, int y) {
		Minecraft client = Minecraft.getInstance();
		Button toggle = Button.builder(moduleMessage(BlockScannerClient.Module.ESP, "ESP"), widget -> {
			BlockScannerClient.setModuleEnabled(BlockScannerClient.Module.ESP,
					!BlockScannerClient.moduleEnabled(BlockScannerClient.Module.ESP), client);
			widget.setMessage(moduleMessage(BlockScannerClient.Module.ESP, "ESP"));
		}).bounds(x, y, 56, 20).build();
		addRenderableWidget(toggle);
		addRenderableWidget(Button.builder(bindingMessage(BlockScannerClient.Module.ESP), widget -> {
			awaitingKey = BlockScannerClient.Module.ESP;
			widget.setMessage(Component.literal("..."));
		}).bounds(x + 60, y, 42, 20).build());
		addRenderableWidget(Button.builder(Component.literal("Cfg"), button ->
				client.gui.setScreen(new ESPBlockSettingsScreen(this))).bounds(x + 106, y, 26, 20).build());
	}

	private void addModuleControls(BlockScannerClient.Module module, String label, int x, int y) {
		Minecraft client = Minecraft.getInstance();
		Button button = Button.builder(moduleMessage(module, label), widget -> {
			BlockScannerClient.setModuleEnabled(module, !BlockScannerClient.moduleEnabled(module), client);
			widget.setMessage(moduleMessage(module, label));
		}).bounds(x, y, 80, 20).build();
		if (module == BlockScannerClient.Module.FREECAM || module == BlockScannerClient.Module.NAME_PROTECT) {
			button.active = BlockScannerClient.isLocalWorld(client);
		}
		addRenderableWidget(button);
		addRenderableWidget(Button.builder(bindingMessage(module), widget -> {
			awaitingKey = module;
			widget.setMessage(Component.literal("..."));
		}).bounds(x + 84, y, 48, 20).build());
	}

	private Component bindingMessage(BlockScannerClient.Module module) {
		if (awaitingKey == module) {
			return Component.literal("...");
		}
		return BlockScannerClient.keyFor(module).getTranslatedKeyMessage();
	}

	private static Component moduleMessage(BlockScannerClient.Module module, String label) {
		return Component.literal(label + ": " + onOff(BlockScannerClient.moduleEnabled(module)));
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (awaitingKey == null) {
			return super.keyPressed(event);
		}
		if (event.key() == InputConstants.KEY_ESCAPE) {
			awaitingKey = null;
			init();
			return true;
		}

		KeyMapping keyBinding = BlockScannerClient.keyFor(awaitingKey);
		keyBinding.setKey(event.key() == InputConstants.KEY_BACKSPACE || event.key() == InputConstants.KEY_DELETE
				? InputConstants.UNKNOWN : InputConstants.getKey(event));
		Minecraft.getInstance().options.save();
		awaitingKey = null;
		init();
		return true;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
		extractBackground(context, mouseX, mouseY, delta);
		context.fill(0, 0, width, height, 0xEE111827);
		context.fill(width / 2 - 146, height / 2 - 106, width / 2 + 146, height / 2 + 86, 0xF017222C);
		context.fill(width / 2 - 146, height / 2 - 106, width / 2 + 146, height / 2 - 103, 0xFF0EA5E9);
		context.centeredText(font, title, width / 2, height / 2 - 87, 0xFF38BDF8);
		context.centeredText(font, Component.literal("Single-player test features only"),
				width / 2, height / 2 - 68, 0xFFB7D8E8);
		context.centeredText(font,
				Component.literal(awaitingKey == null ? "Click a key to rebind" : "Press a key (Backspace clears, Esc cancels)"),
				width / 2, height / 2 - 54, 0xFFB7D8E8);
		super.extractRenderState(context, mouseX, mouseY, delta);
	}

	@Override
	public void onClose() {
		ScannerConfig.save();
		Minecraft.getInstance().gui.setScreen(parent);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private static String onOff(boolean enabled) {
		return enabled ? "ON" : "OFF";
	}
}