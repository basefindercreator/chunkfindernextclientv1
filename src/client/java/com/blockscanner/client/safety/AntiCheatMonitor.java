package com.blockscanner.client.safety;

import com.blockscanner.client.BlockScannerClient;
import com.blockscanner.client.ScannerConfig;
import net.minecraft.client.MinecraftClient;

public final class AntiCheatMonitor {
	private static long warningUntil;

	private AntiCheatMonitor() {
	}

	public static void simulateFlag(MinecraftClient client) {
		if (BlockScannerClient.isLocalWorld(client) && ScannerConfig.get().safetyTestMode) {
			warningUntil = System.currentTimeMillis() + 7000;
		}
	}

	public static boolean isWarningVisible() {
		return System.currentTimeMillis() < warningUntil;
	}

	public static void clear() {
		warningUntil = 0;
	}
}