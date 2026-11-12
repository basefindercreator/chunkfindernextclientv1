package com.blockscanner.client.freecam;

import com.blockscanner.client.BlockScannerClient;
import com.blockscanner.client.ScannerConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.GameOptions;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.util.math.Vec3d;

public final class FreecamManager {
	private Entity camera;
	private Vec3d bodyPosition;
	private Vec3d bodyVelocity;
	private boolean previousNoGravity;
	private boolean active;
	private boolean[] heldMovement = new boolean[7];

	public boolean isActive() {
		return active;
	}

	public void toggle(MinecraftClient client) {
		if (active) {
			deactivate(client);
		} else {
			activate(client);
		}
	}

	public boolean activate(MinecraftClient client) {
		if (!BlockScannerClient.isLocalWorld(client) || active) {
			return false;
		}
		ClientPlayerEntity player = client.player;
		ArmorStandEntity cameraEntity = EntityType.ARMOR_STAND.create(client.world, SpawnReason.COMMAND);
		if (cameraEntity == null) {
			return false;
		}
		bodyPosition = player.getPos();
		bodyVelocity = player.getVelocity();
		previousNoGravity = player.hasNoGravity();
		player.setVelocity(Vec3d.ZERO);
		player.setNoGravity(true);
		cameraEntity.refreshPositionAndAngles(player.getX(), player.getEyeY(), player.getZ(),
				player.getYaw(), player.getPitch());
		cameraEntity.setNoGravity(true);
		cameraEntity.noClip = true;
		camera = cameraEntity;
		client.setCameraEntity(cameraEntity);
		active = true;
		return true;
	}

	public void onStartTick(MinecraftClient client) {
		if (!active) {
			return;
		}
		if (!BlockScannerClient.isLocalWorld(client)) {
			deactivate(client);
			return;
		}
		var options = client.options;
		heldMovement = new boolean[] {
				options.forwardKey.isPressed(), options.backKey.isPressed(), options.leftKey.isPressed(),
				options.rightKey.isPressed(), options.jumpKey.isPressed(), options.sneakKey.isPressed(),
				options.sprintKey.isPressed()
		};
		setMovementKeys(options, false);
		pinBody(client.player);
	}

	public void onEndTick(MinecraftClient client) {
		if (!active || !(camera instanceof ArmorStandEntity cameraEntity) || client.player == null) {
			return;
		}
		ClientPlayerEntity player = client.player;
		pinBody(player);

		double forward = (heldMovement[0] ? 1.0 : 0.0) - (heldMovement[1] ? 1.0 : 0.0);
		double sideways = (heldMovement[3] ? 1.0 : 0.0) - (heldMovement[2] ? 1.0 : 0.0);
		double vertical = (heldMovement[4] ? 1.0 : 0.0) - (heldMovement[5] ? 1.0 : 0.0);
		double length = Math.sqrt(forward * forward + sideways * sideways + vertical * vertical);
		if (length > 0.0) {
			forward /= length;
			sideways /= length;
			vertical /= length;
			double yaw = Math.toRadians(cameraEntity.getYaw());
			double speed = ScannerConfig.get().freecamSpeed * (heldMovement[6] ? 2.0 : 1.0);
			double dx = (-Math.sin(yaw) * forward + Math.cos(yaw) * sideways) * speed;
			double dz = (Math.cos(yaw) * forward + Math.sin(yaw) * sideways) * speed;
			cameraEntity.setPosition(cameraEntity.getX() + dx,
					cameraEntity.getY() + vertical * speed, cameraEntity.getZ() + dz);
		}
		setMovementKeys(client.options, true);
	}

	public void deactivate(MinecraftClient client) {
		if (!active) {
			return;
		}
		if (client.player != null) {
			client.player.setPosition(bodyPosition);
			client.player.setVelocity(bodyVelocity == null ? Vec3d.ZERO : bodyVelocity);
			client.player.setNoGravity(previousNoGravity);
			client.setCameraEntity(client.player);
		}
		camera = null;
		bodyPosition = null;
		bodyVelocity = null;
		active = false;
		if (client.options != null) {
			setMovementKeys(client.options, true);
		}
	}

	private void pinBody(ClientPlayerEntity player) {
		if (bodyPosition != null) {
			player.setPosition(bodyPosition);
			player.setVelocity(Vec3d.ZERO);
			player.setNoGravity(true);
		}
	}

	private void setMovementKeys(GameOptions options, boolean restore) {
		boolean[] values = restore ? heldMovement : new boolean[7];
		options.forwardKey.setPressed(values[0]);
		options.backKey.setPressed(values[1]);
		options.leftKey.setPressed(values[2]);
		options.rightKey.setPressed(values[3]);
		options.jumpKey.setPressed(values[4]);
		options.sneakKey.setPressed(values[5]);
		options.sprintKey.setPressed(values[6]);
	}
}