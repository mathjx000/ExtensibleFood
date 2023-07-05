package me.mathjx.extensiblefood.block.collision;

import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class ThornsCollisionEffect extends CollisionEffect {

	private final EntityType<?>[] toIgnore;
	private final float damagesAmount;
	private final double movementThreshold;

	public ThornsCollisionEffect(final EntityType<?>[] toIgnore, final float damagesAmount,
			final double movementThreshold) {
		this.toIgnore = toIgnore;
		this.damagesAmount = damagesAmount;
		this.movementThreshold = movementThreshold;
	}

	@Override
	public void onEntityCollision(final BlockState state, final World world, final BlockPos pos, final Entity entity) {
		if (world.isClient) return; // damages are only performed on server side and then sent to clients

		// ensure the collided entity can take damages
		if (entity instanceof LivingEntity) {
			final EntityType<?> t = entity.getType();

			for (int i = 0; i < toIgnore.length; i++) if (t == toIgnore[i])
				// this entity must not be affected
				return;

			if (movementThreshold > 0f) {
				final double d1 = Math.abs(entity.getX() - entity.lastRenderX);
				final double d2 = Math.abs(entity.getZ() - entity.lastRenderZ);

				if (d1 < movementThreshold && d2 < movementThreshold) return;
			}

			// TODO: customizable damage source
			entity.damage(world.getDamageSources().cactus(), damagesAmount);
		}
	}

}
