package com.blockscanner.client.nameprotect;

import com.blockscanner.client.BlockScannerClient;
import com.blockscanner.client.ScannerConfig;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

public final class NameProtectManager {
	private NameProtectManager() {
	}

	public static String replace(String text) {
		String realName = protectedPlayerName();
		String alias = alias();
		return realName.isEmpty() || alias.isEmpty() || realName.equals(alias) ? text : text.replace(realName, alias);
	}

	public static FormattedCharSequence replace(FormattedCharSequence text) {
		String realName = protectedPlayerName();
		String alias = alias();
		if (realName.isEmpty() || alias.isEmpty() || realName.equals(alias)) {
			return text;
		}
		int[] search = realName.codePoints().toArray();
		if (!contains(text, search)) {
			return text;
		}

		List<Integer> codePoints = new ArrayList<>();
		List<Style> styles = new ArrayList<>();
		text.accept((index, style, codePoint) -> {
			codePoints.add(codePoint);
			styles.add(style);
			return true;
		});
		int[] replacement = alias.codePoints().toArray();
		List<Integer> outputPoints = new ArrayList<>(codePoints.size());
		List<Style> outputStyles = new ArrayList<>(styles.size());
		for (int index = 0; index < codePoints.size();) {
			if (matches(codePoints, index, search)) {
				Style style = styles.get(index);
				for (int point : replacement) {
					outputPoints.add(point);
					outputStyles.add(style);
				}
				index += search.length;
			} else {
				outputPoints.add(codePoints.get(index));
				outputStyles.add(styles.get(index));
				index++;
			}
		}
		return visitor -> {
			for (int index = 0; index < outputPoints.size(); index++) {
				if (!visitor.accept(index, outputStyles.get(index), outputPoints.get(index))) {
					return false;
				}
			}
			return true;
		};
	}

	private static boolean contains(FormattedCharSequence text, int[] search) {
		int[] recent = new int[search.length];
		long[] visited = {0};
		boolean[] found = {false};
		text.accept((index, style, codePoint) -> {
			recent[(int) (visited[0] % search.length)] = codePoint;
			visited[0]++;
			if (visited[0] >= search.length) {
				long start = visited[0] - search.length;
				for (int offset = 0; offset < search.length; offset++) {
					if (recent[(int) ((start + offset) % search.length)] != search[offset]) {
						return true;
					}
				}
				found[0] = true;
				return false;
			}
			return true;
		});
		return found[0];
	}

	private static boolean matches(List<Integer> source, int offset, int[] search) {
		if (offset + search.length > source.size()) {
			return false;
		}
		for (int index = 0; index < search.length; index++) {
			if (source.get(offset + index) != search[index]) {
				return false;
			}
		}
		return true;
	}

	private static String protectedPlayerName() {
		Minecraft client = Minecraft.getInstance();
		if (!ScannerConfig.get().nameProtectEnabled || !BlockScannerClient.isLocalWorld(client)) {
			return "";
		}
		return client.player.getGameProfile().name();
	}

	private static String alias() {
		return ScannerConfig.get().protectedName.trim();
	}
}