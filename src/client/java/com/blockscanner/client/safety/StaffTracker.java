package com.blockscanner.client.safety;

import com.blockscanner.client.BlockScannerClient;
import com.blockscanner.client.ScannerConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class StaffTracker {
	private static String simulatedStaffName;
	private static long visibleUntil;

	private StaffTracker() {
	}

	public static void simulateWatching(Minecraft client) {
		if (!BlockScannerClient.isLocalWorld(client) || !ScannerConfig.get().safetyTestMode) {
			return;
		}
		simulatedStaffName = "TestModerator";
		visibleUntil = System.currentTimeMillis() + 10000;
	}

	public static void simulateStopped(Minecraft client) {
		if (!BlockScannerClient.isLocalWorld(client) || !ScannerConfig.get().safetyTestMode
				|| simulatedStaffName == null) {
			return;
		}
		String name = simulatedStaffName;
		simulatedStaffName = null;
		visibleUntil = 0;
		if (client.gui != null) {
			client.gui.hud.getChat().addClientSystemMessage(
					Component.literal("[TEST] Staff member " + name + " stopped watching you."));
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