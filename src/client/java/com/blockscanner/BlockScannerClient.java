package com.blockscanner;

import net.fabricmc.api.ClientModInitializer;

public final class BlockScannerClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		new com.blockscanner.client.BlockScannerClient().onInitializeClient();
	}
}