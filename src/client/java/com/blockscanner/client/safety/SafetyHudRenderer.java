package com.blockscanner.client.safety;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import com.blockscanner.client.ScannerConfig;
import com.blockscanner.client.BlockScannerClient;

public final class SafetyHudRenderer {
	private SafetyHudRenderer() {
	}

	public static void render(GuiGraphicsExtractor context) {
		Minecraft client = Minecraft.getInstance();
		if (!BlockScannerClient.isLocalWorld(client) || !ScannerConfig.get().safetyTestMode) {
			AntiCheatMonitor.clear();
			StaffTracker.clear();
			return;
		}
		int y = 31;
		if (AntiCheatMonitor.isWarningVisible()) {
			y = drawBanner(context, "[TEST] BLOCKSCANNER: SIMULATED ANTI-CHEAT FLAG", y, 0xFFFFB84D);
		}
		String staffBanner = StaffTracker.activeBanner();
		if (!staffBanner.isEmpty()) {
			drawBanner(context, staffBanner, y, 0xFFFFB84D);
		}
	}

	private static int drawBanner(GuiGraphicsExtractor context, String message, int y, int accent) {
		Minecraft client = Minecraft.getInstance();
		int width = Math.min(client.font.width(message) + 32, context.guiWidth() - 24);
		int left = (context.guiWidth() - width) / 2;
		context.fill(left, y, left + width, y + 22, 0xE51A1712);
		context.fill(left, y, left + 3, y + 22, accent);
		context.centeredText(client.font, Component.literal(message),
				context.guiWidth() / 2, y + 7, accent);
		return y + 26;
	}
}