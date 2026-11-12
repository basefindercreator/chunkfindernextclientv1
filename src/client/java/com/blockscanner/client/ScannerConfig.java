package com.blockscanner.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import net.fabricmc.loader.api.FabricLoader;

public final class ScannerConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("blockscanner.json");
	private static ScannerConfig instance = new ScannerConfig();

	public boolean enabled = true;
	public boolean espEnabled = true;
	public boolean minimapEnabled = true;
	public boolean alertsEnabled = true;
	public boolean nameProtectEnabled = false;
	public boolean arrayListEnabled = true;
	public boolean safetyTestMode = false;
	public boolean showEntities = true;
	public boolean showTestBanner = false;
	public float freecamSpeed = 0.55F;
	public int radarScopeChunks = 100;
	public int chunkColor = 0x42F56C;
	public int flaggedChunkColor = 0x42F56C;
	public int doubleChestColor = 0xFF8C00;
	public int entityColor = 0x47D7FF;
	public int arrayListColor = 0x42F56C;
	public String arrayListColorMode = "RAINBOW";
	public String arrayListCorner = "TOP_RIGHT";
	public Map<String, Integer> blockColors = defaultBlockColors();
	public String blockFilter = "chest,hopper";
	public String entityFilter = "";
	public String protectedName = "Player";

	private ScannerConfig() {
	}

	private static Map<String, Integer> defaultBlockColors() {
		Map<String, Integer> colors = new LinkedHashMap<>();
		colors.put("minecraft:chest", 0xFF3030);
		colors.put("minecraft:trapped_chest", 0xFF3030);
		colors.put("minecraft:hopper", 0x47D7FF);
		return colors;
	}

	public static ScannerConfig get() {
		return instance;
	}

	public static void load() {
		if (!Files.exists(FILE)) {
			save();
			return;
		}
		try {
			ScannerConfig loaded = GSON.fromJson(Files.readString(FILE), ScannerConfig.class);
			if (loaded != null) {
				loaded.normalize();
				instance = loaded;
			}
		} catch (IOException | RuntimeException exception) {
			System.err.println("BlockScanner config could not be loaded: " + exception.getMessage());
		}
	}

	public static void save() {
		try {
			Files.createDirectories(FILE.getParent());
			Files.writeString(FILE, GSON.toJson(instance));
		} catch (IOException exception) {
			System.err.println("BlockScanner config could not be saved: " + exception.getMessage());
		}
	}

	private void normalize() {
		radarScopeChunks = Math.max(1, Math.min(100, radarScopeChunks));
		chunkColor &= 0xFFFFFF;
		flaggedChunkColor &= 0xFFFFFF;
		doubleChestColor &= 0xFFFFFF;
		entityColor &= 0xFFFFFF;
		arrayListColor &= 0xFFFFFF;
		if (!"STATIC".equals(arrayListColorMode) && !"RAINBOW".equals(arrayListColorMode)) {
			arrayListColorMode = "RAINBOW";
		}
		if (arrayListCorner == null
				|| !java.util.Set.of("TOP_RIGHT", "TOP_LEFT", "BOTTOM_RIGHT", "BOTTOM_LEFT").contains(arrayListCorner)) {
			arrayListCorner = "TOP_RIGHT";
		}
		freecamSpeed = Math.max(0.1F, Math.min(5.0F, freecamSpeed));
		if (blockColors == null) {
			blockColors = new LinkedHashMap<>();
		}
		blockColors.replaceAll((id, color) -> color & 0xFFFFFF);
		blockColors.putIfAbsent("minecraft:chest", 0xFF3030);
		blockColors.putIfAbsent("minecraft:trapped_chest", 0xFF3030);
		blockColors.putIfAbsent("minecraft:hopper", 0x47D7FF);
		if (blockFilter == null) {
			blockFilter = "";
		}
		if (entityFilter == null) {
			entityFilter = "";
		}
		if (protectedName == null) {
			protectedName = "Player";
		}
		protectedName = protectedName.substring(0, Math.min(16, protectedName.length()));
	}
}