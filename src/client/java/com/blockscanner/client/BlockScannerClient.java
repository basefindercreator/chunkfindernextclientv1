package com.blockscanner.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import com.blockscanner.client.gui.ScannerConfigScreen;
import com.blockscanner.client.render.ESPRenderer;
import com.blockscanner.client.render.MinimapHudRenderer;
import com.blockscanner.client.hud.ArrayListHudRenderer;
import com.blockscanner.client.safety.AntiCheatMonitor;
import com.blockscanner.client.safety.SafetyHudRenderer;
import com.blockscanner.client.safety.SafetyTestScreen;
import com.blockscanner.client.safety.StaffTracker;
import com.blockscanner.client.scanner.ChunkPatternAnalyzer;
import com.blockscanner.client.scanner.ScanTarget;
import com.blockscanner.client.freecam.FreecamManager;
import net.minecraft.util.math.ChunkPos;
import org.lwjgl.glfw.GLFW;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class BlockScannerClient implements ClientModInitializer {
	public enum Module { SCANNER, ESP, MINIMAP, ALERTS, FREECAM, NAME_PROTECT }
	public static KeyBinding openConfigKey;
	private static KeyBinding globalToggleKey;
	private static final Map<Module, KeyBinding> MODULE_KEYS = new EnumMap<>(Module.class);
	private static final ChunkPatternAnalyzer ANALYZER = new ChunkPatternAnalyzer();
	private static final Set<Long> PREVIOUS_FLAGS = new HashSet<>();
	public static final FreecamManager FREECAM = new FreecamManager();

	public static KeyBinding keyFor(Module module) {
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
			case NAME_PROTECT -> config.nameProtectEnabled && isLocalWorld(MinecraftClient.getInstance());
		};
	}

	public static List<String> activeModules() {
		MinecraftClient client = MinecraftClient.getInstance();
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

	public static void setModuleEnabled(Module module, boolean enabled, MinecraftClient client) {
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

	public static boolean isLocalWorld(MinecraftClient client) {
		return client.world != null && client.player != null && client.getServer() != null;
	}

	@Override
	public void onInitializeClient() {
		ScannerConfig.load();
		openConfigKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.blockscanner.open_config", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_O, "category.blockscanner"));
		globalToggleKey = register("toggle", GLFW.GLFW_KEY_RIGHT_SHIFT);
		MODULE_KEYS.put(Module.SCANNER, register("scanner", GLFW.GLFW_KEY_6));
		MODULE_KEYS.put(Module.ESP, register("esp", GLFW.GLFW_KEY_7));
		MODULE_KEYS.put(Module.MINIMAP, register("minimap", GLFW.GLFW_KEY_8));
		MODULE_KEYS.put(Module.ALERTS, register("alerts", GLFW.GLFW_KEY_9));
		MODULE_KEYS.put(Module.FREECAM, register("freecam", GLFW.GLFW_KEY_F));
		MODULE_KEYS.put(Module.NAME_PROTECT, register("name_protect", GLFW.GLFW_KEY_P));
		ClientTickEvents.START_CLIENT_TICK.register(FREECAM::onStartTick);
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			FREECAM.onEndTick(client);
			if (openConfigKey.wasPressed() && client.currentScreen == null) {
				client.setScreen(new ScannerConfigScreen(client.currentScreen));
			}
			if (globalToggleKey.wasPressed()) {
				ScannerConfig.get().enabled = !ScannerConfig.get().enabled;
				ScannerConfig.save();
			}
			if (client.currentScreen == null) {
				for (Module module : Module.values()) {
					KeyBinding key = MODULE_KEYS.get(module);
					if (key.wasPressed()) {
						setModuleEnabled(module, !moduleEnabled(module), client);
					}
				}
			}
			if (isLocalWorld(client)) {
				ANALYZER.tick(client);
				Set<Long> currentFlags = ANALYZER.flaggedChunks();
				if (ScannerConfig.get().alertsEnabled && client.inGameHud != null) {
					for (long key : currentFlags) {
						if (!PREVIOUS_FLAGS.contains(key)) {
							List<ScanTarget> targets = ANALYZER.results().getOrDefault(key, List.of());
						long chestPairs = targets.stream()
								.filter(target -> target.kind() == ScanTarget.Kind.CONTAINER_CLUSTER).count() / 2;
						long hoppers = targets.stream().filter(target -> target.kind() == ScanTarget.Kind.HOPPER).count();
						String details = chestPairs + " chest pairs, " + hoppers + " hoppers";
						client.inGameHud.getChatHud().addMessage(Text.literal("[BlockScanner] Flagged chunk ["
								+ ChunkPos.getPackedX(key) + ", " + ChunkPos.getPackedZ(key) + "]: " + details));
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
		WorldRenderEvents.AFTER_ENTITIES.register(context -> ESPRenderer.render(context, ANALYZER));
		HudRenderCallback.EVENT.register((drawContext, tickCounter) -> {
			MinimapHudRenderer.render(drawContext, ANALYZER);
			ArrayListHudRenderer.render(drawContext);
			SafetyHudRenderer.render(drawContext);
			if (FREECAM.isActive() && isLocalWorld(MinecraftClient.getInstance())) {
				drawContext.fill(8, 8, 136, 25, 0xC914261B);
				drawContext.fill(8, 8, 10, 25, 0xFF42F56C);
				drawContext.drawTextWithShadow(MinecraftClient.getInstance().textRenderer,
						Text.literal("FREECAM ACTIVE"), 16, 13, 0xFF8CFFA2);
			}
		});
	}

	private static KeyBinding register(String name, int keyCode) {
		return KeyBindingHelper.registerKeyBinding(new KeyBinding("key.blockscanner." + name,
				InputUtil.Type.KEYSYM, keyCode, "category.blockscanner"));
	}
}