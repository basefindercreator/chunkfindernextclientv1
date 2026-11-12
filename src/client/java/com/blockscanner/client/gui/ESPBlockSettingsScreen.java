package com.blockscanner.client.gui;

import com.blockscanner.client.ScannerConfig;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class ESPBlockSettingsScreen extends Screen {
	private static final String DOUBLE_CHEST = "special:double_chest_pairs";
	private static final int LIST_ROW_HEIGHT = 18;
	private static final int LIST_ROWS = 8;

	private final Screen parent;
	private final List<String> allBlockIds = BuiltInRegistries.BLOCK.keySet().stream()
			.map(Identifier::toString).sorted(Comparator.naturalOrder()).toList();
	private EditBox searchField;
	private EditBox colorField;
	private String selectedBlockId;
	private String statusMessage = "Search block IDs; only blocks you add are highlighted.";
	private int searchScroll;
	private int trackedScroll;

	public ESPBlockSettingsScreen(Screen parent) {
		super(Component.literal("ESP Block Settings"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int centerX = width / 2;
		int centerY = height / 2;
		int leftX = centerX - 250;
		int rightX = centerX + 10;

		searchField = new EditBox(font, leftX, centerY - 126, 250, 20,
				Component.literal("Search blocks"));
		searchField.setMaxLength(64);
		searchField.setHint(Component.literal("Search block ID, e.g. minecraft:diamond_block"));
		addRenderableWidget(searchField);

		colorField = new EditBox(font, rightX, centerY - 126, 100, 20,
				Component.literal("RGB color"));
		colorField.setMaxLength(6);
		colorField.setHint(Component.literal("RRGGBB"));
		colorField.setValue(selectedColor());
		addRenderableWidget(colorField);

		addRenderableWidget(Button.builder(Component.literal("Add / update"), button -> addOrUpdate())
				.bounds(rightX + 106, centerY - 126, 124, 20).build());
		addRenderableWidget(Button.builder(Component.literal("Apply color"), button -> applyColor())
				.bounds(rightX, centerY - 98, 100, 20).build());
		Button remove = Button.builder(Component.literal("Remove"), button -> removeSelected())
				.bounds(rightX + 106, centerY - 98, 124, 20).build();
		remove.active = selectedBlockId != null && !DOUBLE_CHEST.equals(selectedBlockId);
		addRenderableWidget(remove);
		addRenderableWidget(Button.builder(Component.literal("Done"), button -> onClose())
				.bounds(centerX - 60, centerY + 151, 120, 20).build());
	}

	private List<String> searchResults() {
		String query = searchField == null ? "" : searchField.getValue().trim().toLowerCase();
		if (query.isEmpty()) {
			return List.of();
		}
		return allBlockIds.stream().filter(id -> id.toLowerCase().contains(query)).toList();
	}

	private List<String> trackedBlockIds() {
		List<String> tracked = new ArrayList<>(ScannerConfig.get().blockColors.keySet());
		tracked.sort(Comparator.naturalOrder());
		tracked.add(0, DOUBLE_CHEST);
		return tracked;
	}

	private String selectedColor() {
		if (selectedBlockId == null) {
			return "";
		}
		int color = DOUBLE_CHEST.equals(selectedBlockId)
				? ScannerConfig.get().doubleChestColor
				: ScannerConfig.get().blockColors.getOrDefault(selectedBlockId, 0xFFFFFF);
		return "%06X".formatted(color & 0xFFFFFF);
	}

	private void selectBlock(String id) {
		selectedBlockId = id;
		colorField.setValue(selectedColor());
		statusMessage = "Selected. Click Add / update to track this block.";
	}

	private void addOrUpdate() {
		if (selectedBlockId == null || DOUBLE_CHEST.equals(selectedBlockId)) {
			statusMessage = "Select a block from the search results first.";
			return;
		}
		Identifier id = Identifier.tryParse(selectedBlockId);
		if (id == null || !BuiltInRegistries.BLOCK.containsKey(id)) {
			statusMessage = "Choose a valid block from the search results.";
			return;
		}
		Integer color = parseColor();
		if (color == null) {
			return;
		}
		ScannerConfig.get().blockColors.put(id.toString(), color);
		ScannerConfig.save();
		statusMessage = "Now tracking " + id + ".";
	}

	private void applyColor() {
		if (selectedBlockId == null) {
			statusMessage = "Select a tracked block first.";
			return;
		}
		Integer color = parseColor();
		if (color == null) {
			return;
		}
		if (DOUBLE_CHEST.equals(selectedBlockId)) {
			ScannerConfig.get().doubleChestColor = color;
		} else if (ScannerConfig.get().blockColors.containsKey(selectedBlockId)) {
			ScannerConfig.get().blockColors.put(selectedBlockId, color);
		} else {
			statusMessage = "Add this block before applying its color.";
			return;
		}
		ScannerConfig.save();
		statusMessage = "Saved color for " + selectedBlockId + ".";
	}

	private Integer parseColor() {
		String value = colorField.getValue();
		if (value.length() != 6) {
			statusMessage = "Enter exactly six hexadecimal RGB digits (for example FF3030).";
			return null;
		}
		try {
			return Integer.parseInt(value, 16);
		} catch (NumberFormatException exception) {
			statusMessage = "Color must contain only hexadecimal digits.";
			return null;
		}
	}

	private void removeSelected() {
		if (selectedBlockId != null && !DOUBLE_CHEST.equals(selectedBlockId)) {
			ScannerConfig.get().blockColors.remove(selectedBlockId);
			ScannerConfig.save();
			selectedBlockId = null;
			colorField.setValue("");
			statusMessage = "Removed the block from ESP tracking.";
		}
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		double mouseX = event.x();
		double mouseY = event.y();
		if (super.mouseClicked(event, doubleClick)) {
			return true;
		}
		int centerY = height / 2;
		int leftX = width / 2 - 250;
		int rightX = width / 2 + 10;
		int listTop = centerY - 72;
		if (mouseX >= leftX && mouseX < leftX + 250 && mouseY >= listTop
				&& mouseY < listTop + LIST_ROWS * LIST_ROW_HEIGHT) {
			int row = (int) (mouseY - listTop) / LIST_ROW_HEIGHT + searchScroll;
			List<String> matches = searchResults();
			if (row < matches.size()) {
				selectBlock(matches.get(row));
				return true;
			}
		}
		if (mouseX >= rightX && mouseX < rightX + 250 && mouseY >= listTop
				&& mouseY < listTop + LIST_ROWS * LIST_ROW_HEIGHT) {
			int row = (int) (mouseY - listTop) / LIST_ROW_HEIGHT + trackedScroll;
			List<String> tracked = trackedBlockIds();
			if (row < tracked.size()) {
				selectBlock(tracked.get(row));
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		int centerY = height / 2;
		int leftX = width / 2 - 250;
		int rightX = width / 2 + 10;
		if (mouseX >= leftX && mouseX < leftX + 250 && mouseY >= centerY - 72
				&& mouseY < centerY - 72 + LIST_ROWS * LIST_ROW_HEIGHT) {
			searchScroll = clampScroll(searchScroll, searchResults().size(), verticalAmount);
			return true;
		}
		if (mouseX >= rightX && mouseX < rightX + 250 && mouseY >= centerY - 72
				&& mouseY < centerY - 72 + LIST_ROWS * LIST_ROW_HEIGHT) {
			trackedScroll = clampScroll(trackedScroll, trackedBlockIds().size(), verticalAmount);
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
	}

	private static int clampScroll(int current, int size, double amount) {
		return Math.max(0, Math.min(Math.max(0, size - LIST_ROWS), current - (int) Math.signum(amount)));
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
		extractBackground(context, mouseX, mouseY, delta);
		int centerX = width / 2;
		int centerY = height / 2;
		context.fill(0, 0, width, height, 0xEE111827);
		context.fill(centerX - 270, centerY - 155, centerX + 270, centerY + 170, 0xF017222C);
		context.fill(centerX - 270, centerY - 155, centerX + 270, centerY - 152, 0xFF0EA5E9);
		context.centeredText(font, title, centerX, centerY - 144, 0xFF38BDF8);
		context.text(font, Component.literal("Search results (not active until added)"),
				centerX - 250, centerY - 98, 0xFFB7D8E8);
		context.text(font, Component.literal("Tracked block colors"),
				centerX + 10, centerY - 84, 0xFFB7D8E8);

		drawListPanel(context, centerX - 250, centerY - 72, searchResults(), searchScroll, true);
		drawListPanel(context, centerX + 10, centerY - 72, trackedBlockIds(), trackedScroll, false);

		if (selectedBlockId != null) {
			int color = DOUBLE_CHEST.equals(selectedBlockId)
					? ScannerConfig.get().doubleChestColor
					: ScannerConfig.get().blockColors.getOrDefault(selectedBlockId, 0xFFFFFF);
			context.fill(centerX + 220, centerY - 126, centerX + 240, centerY - 106,
					0xFF000000 | color);
			context.text(font, Component.literal("Selected: " + selectedBlockId),
					centerX - 250, centerY + 90, 0xFFE2E8F0);
		}
		context.text(font, Component.literal(statusMessage),
				centerX - 250, centerY + 112, 0xFFB7D8E8);
		super.extractRenderState(context, mouseX, mouseY, delta);
	}

	private void drawListPanel(GuiGraphicsExtractor context, int x, int y, List<String> entries, int scroll,
			boolean results) {
		int width = 250;
		context.fill(x, y, x + width, y + LIST_ROWS * LIST_ROW_HEIGHT, 0xA0101722);
		int end = Math.min(entries.size(), scroll + LIST_ROWS);
		for (int index = scroll; index < end; index++) {
			String id = entries.get(index);
			int rowY = y + (index - scroll) * LIST_ROW_HEIGHT;
			boolean selected = id.equals(selectedBlockId);
			if (selected) {
				context.fill(x, rowY, x + width, rowY + LIST_ROW_HEIGHT, 0xFF164E63);
			}
			if (!results) {
				int color = DOUBLE_CHEST.equals(id)
						? ScannerConfig.get().doubleChestColor
						: ScannerConfig.get().blockColors.getOrDefault(id, 0xFFFFFF);
				context.fill(x + 4, rowY + 4, x + 15, rowY + 15, 0xFF000000 | color);
			}
			String label = DOUBLE_CHEST.equals(id) ? "Double chest pairs" : id;
			context.text(font, Component.literal(label), x + (results ? 5 : 20), rowY + 5,
					selected ? 0xFFFFFFFF : 0xFFD1D5DB);
		}
		if (entries.isEmpty()) {
			context.text(font,
					Component.literal(results ? "Type a block ID to search." : "No blocks tracked."),
					x + 5, y + 5, 0xFF9CA3AF);
		}
	}

	@Override
	public void onClose() {
		ScannerConfig.save();
		Minecraft.getInstance().gui.setScreen(parent);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
