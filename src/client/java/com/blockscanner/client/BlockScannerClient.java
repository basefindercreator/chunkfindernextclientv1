package com.blockscanner.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import com.mojang.blaze3d.platform.InputConstants;
import com.blockscanner.client.gui.ScannerConfigScreen;
import com.blockscanner.client.render.ESPRenderer;
import com.blockscanner.client.render.MinimapHudRenderer;
import com.blockscanner.client.render.BaseFoundHudRenderer;
import com.blockscanner.client.hud.ArrayListHudRenderer;
import com.blockscanner.client.safety.AntiCheatMonitor;
import com.blockscanner.client.safety.SafetyHudRenderer;
import com.blockscanner.client.safety.SafetyTestScreen;
import com.blockscanner.client.safety.StaffTracker;
import com.blockscanner.client.scanner.ChunkPatternAnalyzer;
import com.blockscanner.client.scanner.ScanTarget;
import com.blockscanner.client.freecam.FreecamManager;
import net.minecraft.world.level.ChunkPos;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class BlockScannerClient implements ClientModInitializer {
	public enum Module { SCANNER, ESP, MINIMAP, ALERTS, FREECAM, NAME_PROTECT }
	public static KeyMapping openConfigKey;
	private static KeyMapping globalToggleKey;
	private static final Map<Module, KeyMapping> MODULE_KEYS = new EnumMap<>(Module.class);
	private static final KeyMapping.Category KEY_CATEGORY =
			KeyMapping.Category.register(Identifier.fromNamespaceAndPath("blockscanner", "modules"));
	private static final ChunkPatternAnalyzer ANALYZER = new ChunkPatternAnalyzer();
	private static final Set<Long> PREVIOUS_FLAGS = new HashSet<>();
	public static final FreecamManager FREECAM = new FreecamManager();

	public static KeyMapping keyFor(Module module) {
		return MODULE_KEYS.get(module);
	}

	public static boolean moduleEnabled(Module module) {
		ScannerConfig config = ScannerConfig.get();
		return switch (module) {
			case SCANNER -> config.enabled;
			case ESP -> config.espEnabled;
			case MINIMAP -> config.minimapEnabled;
			case ALERTS -> config.alertsEnabled;
			case FREECAM -> FREECAM.isActive();
			case NAME_PROTECT -> config.nameProtectEnabled && isLocalWorld(Minecraft.getInstance());
		};
	}

	public static List<String> activeModules() {
		Minecraft client = Minecraft.getInstance();
		ScannerConfig config = ScannerConfig.get();
		boolean localWorld = isLocalWorld(client);
		List<String> active = new java.util.ArrayList<>();
		if (localWorld && config.enabled) active.add("Pattern Analyzer");
		if (localWorld && config.enabled && config.espEnabled) active.add("ESP");
		if (localWorld && config.enabled && config.minimapEnabled) active.add("Minimap");
		if (localWorld && config.enabled && config.alertsEnabled) active.add("Alerts");
		if (localWorld && FREECAM.isActive()) active.add("Freecam");
		if (config.nameProtectEnabled && localWorld) active.add("Name Protect");
		return active;
	}

	public static void setModuleEnabled(Module module, boolean enabled, Minecraft client) {
		ScannerConfig config = ScannerConfig.get();
		switch (module) {
			case SCANNER -> config.enabled = enabled;
			case ESP -> config.espEnabled = enabled;
			case MINIMAP -> config.minimapEnabled = enabled;
			case ALERTS -> config.alertsEnabled = enabled;
			case FREECAM -> {
				if (enabled != FREECAM.isActive()) {
					FREECAM.toggle(client);
				}
			}
			case NAME_PROTECT -> config.nameProtectEnabled = enabled && isLocalWorld(client);
		}
		ScannerConfig.save();
	}

	public static boolean isLocalWorld(Minecraft client) {
		return client.level != null && client.player != null && client.hasSingleplayerServer();
	}

	@Override
	public void onInitializeClient() {
		ScannerConfig.load();
		openConfigKey = register("open_modules", InputConstants.KEY_7);
		globalToggleKey = register("toggle", 344);
		MODULE_KEYS.put(Module.SCANNER, register("scanner", InputConstants.KEY_6));
		MODULE_KEYS.put(Module.ESP, register("esp", InputConstants.KEY_4));
		MODULE_KEYS.put(Module.MINIMAP, register("minimap", InputConstants.KEY_8));
		MODULE_KEYS.put(Module.ALERTS, register("alerts", InputConstants.KEY_9));
		MODULE_KEYS.put(Module.FREECAM, register("freecam", InputConstants.KEY_F));
		MODULE_KEYS.put(Module.NAME_PROTECT, register("name_protect", InputConstants.KEY_P));
		ClientTickEvents.START_CLIENT_TICK.register(FREECAM::onStartTick);
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			FREECAM.onEndTick(client);
			if (openConfigKey.consumeClick() && client.gui.screen() == null) {
				client.gui.setScreen(new ScannerConfigScreen(client.gui.screen()));
			}
			if (globalToggleKey.consumeClick()) {
				ScannerConfig.get().enabled = !ScannerConfig.get().enabled;
				ScannerConfig.save();
			}
			if (client.gui.screen() == null) {
				for (Module module : Module.values()) {
					KeyMapping key = MODULE_KEYS.get(module);
					if (key.consumeClick()) {
						setModuleEnabled(module, !moduleEnabled(module), client);
					}
				}
			}
			if (isLocalWorld(client)) {
				ANALYZER.tick(client);
				Set<Long> currentFlags = ANALYZER.flaggedChunks();
				if (ScannerConfig.get().alertsEnabled && client.gui != null) {
					for (long key : currentFlags) {
						if (!PREVIOUS_FLAGS.contains(key)) {
							List<ScanTarget> targets = ANALYZER.results().getOrDefault(key, List.of());
						long chestPairs = targets.stream()
								.filter(target -> target.kind() == ScanTarget.Kind.CONTAINER_CLUSTER).count() / 2;
						long hoppers = targets.stream().filter(target -> target.kind() == ScanTarget.Kind.HOPPER).count();
						String details = chestPairs + " chest pairs, " + hoppers + " hoppers";
						client.gui.hud.getChat().addClientSystemMessage(Component.literal("[BlockScanner] Flagged chunk ["
								+ ChunkPos.getX(key) + ", " + ChunkPos.getZ(key) + "]: " + details));
						}
					}
				}
				PREVIOUS_FLAGS.clear();
				PREVIOUS_FLAGS.addAll(currentFlags);
			} else {
				FREECAM.deactivate(client);
				ANALYZER.clear();
				PREVIOUS_FLAGS.clear();
				AntiCheatMonitor.clear();
				StaffTracker.clear();
			}
		});
		LevelRenderEvents.COLLECT_SUBMITS.register(context -> ESPRenderer.render(context, ANALYZER));
		HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("blockscanner", "hud"),
				(drawContext, tickCounter) -> renderHud(drawContext));
	}

	private static void renderHud(GuiGraphicsExtractor drawContext) {
		MinimapHudRenderer.render(drawContext, ANALYZER);
		BaseFoundHudRenderer.render(drawContext, ANALYZER);
		ArrayListHudRenderer.render(drawContext);
		SafetyHudRenderer.render(drawContext);
		if (FREECAM.isActive() && isLocalWorld(Minecraft.getInstance())) {
			drawContext.fill(8, 8, 136, 25, 0xC914261B);
			drawContext.fill(8, 8, 10, 25, 0xFF42F56C);
			drawContext.text(Minecraft.getInstance().font, Component.literal("FREECAM ACTIVE"), 16, 13, 0xFF8CFFA2);
		}
	}

	private static KeyMapping register(String name, int keyCode) {
		return KeyMappingHelper.registerKeyMapping(new KeyMapping("key.blockscanner." + name,
				InputConstants.Type.KEYBOARD, keyCode, KEY_CATEGORY));
	}
}