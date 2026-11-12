package com.blockscanner.client.safety;

import com.blockscanner.client.BlockScannerClient;
import com.blockscanner.client.ScannerConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public final class StaffTracker {
	private static String simulatedStaffName;
	private static long visibleUntil;

	private StaffTracker() {
	}

	public static void simulateWatching(MinecraftClient client) {
		if (!BlockScannerClient.isLocalWorld(client) || !ScannerConfig.get().safetyTestMode) {
			return;
		}
		simulatedStaffName = "TestModerator";
		visibleUntil = System.currentTimeMillis() + 10000;
	}

	public static void simulateStopped(MinecraftClient client) {
		if (!BlockScannerClient.isLocalWorld(client) || !ScannerConfig.get().safetyTestMode
				|| simulatedStaffName == null) {
			return;
		}
		String name = simulatedStaffName;
		simulatedStaffName = null;
		visibleUntil = 0;
		if (client.inGameHud != null) {
			client.inGameHud.getChatHud().addMessage(Text.literal("[TEST] Staff member " + name + " stopped watching you."));
		}
	}

	public static String activeBanner() {
		return simulatedStaffName != null && System.currentTimeMillis() < visibleUntil
				? "[TEST] ADMIN IS WATCHING YOU: " + simulatedStaffName : "";
	}

	public static void clear() {
		simulatedStaffName = null;
		visibleUntil = 0;
	}
}