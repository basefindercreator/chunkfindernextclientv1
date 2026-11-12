package com.blockscanner.client.render;

import com.blockscanner.client.BlockScannerClient;
import com.blockscanner.client.ScannerConfig;
import com.blockscanner.client.scanner.ChunkPatternAnalyzer;
import com.blockscanner.client.scanner.ScanTarget;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Comparator;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;

public final class ESPRenderer {
	private static final int MAX_TRACERS = 128;

	private ESPRenderer() {
	}

	public static void render(LevelRenderContext context, ChunkPatternAnalyzer analyzer) {
		Minecraft client = Minecraft.getInstance();
		ScannerConfig config = ScannerConfig.get();
		if (!BlockScannerClient.isLocalWorld(client) || !config.enabled || !config.espEnabled || client.level == null) {
			return;
		}

		Vec3 camera = client.gameRenderer.mainCamera().position();
		var pose = context.poseStack();
		var submits = context.submitNodeCollector();
		pose.pushPose();
		pose.translate(-camera.x, -camera.y, -camera.z);
		try {
			for (long packedChunk : analyzer.flaggedChunks()) {
				int x = ChunkPos.getX(packedChunk);
				int z = ChunkPos.getZ(packedChunk);
				AABB chunkBounds = new AABB(x * 16.0, client.level.getMinY(), z * 16.0,
						(x + 1) * 16.0, client.level.getMaxY(), (z + 1) * 16.0);
				submits.submitShapeOutline(pose, Shapes.create(chunkBounds),
						RenderTypes.linesTranslucentNoDepthWrite(), config.flaggedChunkColor | 0xFF000000, 2.0F, false);
			}
			for (long packedChunk : analyzer.baseFlaggedChunks()) {
				int x = ChunkPos.getX(packedChunk);
				int z = ChunkPos.getZ(packedChunk);
				AABB chunkBounds = new AABB(x * 16.0, client.level.getMinY(), z * 16.0,
						(x + 1) * 16.0, client.level.getMaxY(), (z + 1) * 16.0);
				submits.submitShapeOutline(pose, Shapes.create(chunkBounds),
						RenderTypes.linesTranslucentNoDepthWrite(), 0xFF38BDF8, 2.0F, false);
			}

			Vec3 eye = new Vec3(client.player.getX(), client.player.getEyeY(), client.player.getZ());
			List<ScanTarget> targets = new ArrayList<>();
			for (Map.Entry<Long, List<ScanTarget>> entry : analyzer.results().entrySet()) {
				for (ScanTarget target : entry.getValue()) {
					if (config.blockColors.containsKey(target.blockId())) {
						targets.add(target);
					}
				}
			}
			targets.sort(Comparator.comparingDouble(target -> {
				BlockPos pos = target.pos();
				double dx = pos.getX() + 0.5 - eye.x;
				double dy = pos.getY() + 0.5 - eye.y;
				double dz = pos.getZ() + 0.5 - eye.z;
				return dx * dx + dy * dy + dz * dz;
			}));
			for (int index = 0; index < Math.min(MAX_TRACERS, targets.size()); index++) {
				ScanTarget target = targets.get(index);
				BlockPos pos = target.pos();
				int rgb = target.kind() == ScanTarget.Kind.CONTAINER_CLUSTER
						? config.doubleChestColor
						: config.blockColors.getOrDefault(target.blockId(), config.chunkColor);
				int color = 0xFF000000 | (rgb & 0xFFFFFF);
				submits.submitShapeOutline(pose, Shapes.block().move(pos.getX(), pos.getY(), pos.getZ()),
						RenderTypes.linesTranslucentNoDepthWrite(), color, 2.0F, false);
				Vec3 destination = Vec3.atCenterOf(pos);
				submits.submitCustomGeometry(pose, RenderTypes.linesTranslucentNoDepthWrite(),
						(linePose, vertices) -> drawTracer(linePose, vertices, eye, destination, color));
			}
		} finally {
			pose.popPose();
		}
	}

	private static void drawTracer(com.mojang.blaze3d.vertex.PoseStack.Pose pose, VertexConsumer vertices,
			Vec3 start, Vec3 end, int color) {
		Vec3 direction = end.subtract(start).normalize();
		vertices.addVertex(pose, (float) start.x, (float) start.y, (float) start.z)
				.setColor(color).setNormal(pose, (float) direction.x, (float) direction.y, (float) direction.z)
				.setLineWidth(2.0F);
		vertices.addVertex(pose, (float) end.x, (float) end.y, (float) end.z)
				.setColor(color).setNormal(pose, (float) direction.x, (float) direction.y, (float) direction.z)
				.setLineWidth(2.0F);
	}
}
