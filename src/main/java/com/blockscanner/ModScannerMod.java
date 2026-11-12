package com.blockscanner;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ModScannerMod implements ModInitializer {
	public static final String MOD_ID = "blockscanner";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Initializing BlockScanner Ultimate for local security research and synthetic testing.");
	}
}