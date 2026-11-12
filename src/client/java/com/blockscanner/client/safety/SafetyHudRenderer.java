package com.blockscanner.client.safety;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import com.blockscanner.client.ScannerConfig;
import com.blockscanner.client.BlockScannerClient;

public final class SafetyHudRenderer {
	private SafetyHudRenderer() {
	}

	public static void render(DrawContext context) {
		MinecraftClient client = MinecraftClient.getInstance();
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

	private static int drawBanner(DrawContext context, String message, int y, int accent) {
		MinecraftClient client = MinecraftClient.getInstance();
		int width = Math.min(client.textRenderer.getWidth(message) + 32, context.getScaledWindowWidth() - 24);
		int left = (context.getScaledWindowWidth() - width) / 2;
		context.fill(left, y, left + width, y + 22, 0xE51A1712);
		context.fill(left, y, left + 3, y + 22, accent);
		context.drawCenteredTextWithShadow(client.textRenderer, Text.literal(message),
				context.getScaledWindowWidth() / 2, y + 7, accent);
		return y + 26;
	}
}