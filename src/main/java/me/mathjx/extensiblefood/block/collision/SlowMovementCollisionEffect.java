package me.mathjx.extensiblefood.block.collision;

import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public final class SlowMovementCollisionEffect extends CollisionEffect {

	private final EntityType<?>[] toIgnore;
	private final Vec3d multiplier;

	public SlowMovementCollisionEffect(final EntityType<?>[] toIgnore, final Vec3d multiplier) {
		this.toIgnore = toIgnore;
		this.multiplier = multiplier;
	}

	@Override
	public void onEntityCollision(final BlockState state, final World world, final BlockPos pos, final Entity entity) {
		if (entity instanceof LivingEntity) {
			final EntityType<?> t = entity.getType();

			for (int i = 0; i < toIgnore.length; i++) if (t == toIgnore[i])
				// this entity must not be affected
				return;

			entity.slowMovement(state, multiplier);
		}
	}

}
