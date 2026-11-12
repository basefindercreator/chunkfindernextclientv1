package com.blockscanner.client.hud;

import com.blockscanner.client.BlockScannerClient;
import com.blockscanner.client.ScannerConfig;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public final class ArrayListHudRenderer {
	private static final int ROW_HEIGHT = 12;
	private static final int MARGIN = 8;
	private static final int PAD_X = 7;

	private ArrayListHudRenderer() {
	}

	public static void render(GuiGraphicsExtractor context) {
		Minecraft client = Minecraft.getInstance();
		ScannerConfig config = ScannerConfig.get();
		if (!config.arrayListEnabled || client.player == null || client.gui.hud.isHidden()
				|| client.gui.screen() != null) {
			return;
		}

		List<String> active = BlockScannerClient.activeModules();
		if (active.isEmpty()) {
			return;
		}
		active.sort(Comparator.comparingInt((String name) -> client.font.width(name)).reversed());
		int maxTextWidth = active.stream().mapToInt(client.font::width).max().orElse(0);
		int panelWidth = maxTextWidth + PAD_X * 2 + 3;
		int panelHeight = active.size() * ROW_HEIGHT + 4;
		boolean right = config.arrayListCorner.endsWith("RIGHT");
		boolean bottom = config.arrayListCorner.startsWith("BOTTOM");
		int screenWidth = context.guiWidth();
		int screenHeight = context.guiHeight();
		int left = right ? screenWidth - panelWidth - MARGIN : MARGIN;
		int top = bottom ? screenHeight - panelHeight - MARGIN : MARGIN;
		int rightEdge = left + panelWidth;
		long time = System.currentTimeMillis();

		context.fill(left, top, rightEdge, top + panelHeight, 0xA50A1013);
		for (int index = 0; index < active.size(); index++) {
			String name = active.get(index);
			int rowTop = top + 2 + index * ROW_HEIGHT;
			int color = colorFor(config, index, active.size(), time);
			int textWidth = client.font.width(name);
			int textX = right ? rightEdge - PAD_X - textWidth : left + PAD_X + 3;
			int barX = right ? rightEdge - 3 : left;
			context.fill(barX, rowTop, barX + 2, rowTop + ROW_HEIGHT - 2, color);
			context.fill(right ? left : left + 3, rowTop, right ? rightEdge - 3 : rightEdge,
					rowTop + ROW_HEIGHT - 2, (index & 1) == 0 ? 0x24FFFFFF : 0x14FFFFFF);
			context.text(client.font, Component.literal(name), textX, rowTop + 1, 0xFFEAF2EE);
		}
	}

	private static int colorFor(ScannerConfig config, int index, int count, long time) {
		if ("STATIC".equals(config.arrayListColorMode)) {
			return 0xFF000000 | config.arrayListColor;
		}
		float hue = (time % 6000L) / 6000.0F + (count == 0 ? 0.0F : index / (float) count);
		return Color.HSBtoRGB(hue % 1.0F, 0.72F, 1.0F) | 0xFF000000;
	}
}