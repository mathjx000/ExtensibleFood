package me.mathjx.extensiblefood.block.collision;

import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class CompoundCollisionEffect extends CollisionEffect {

	private final CollisionEffect[] collisionEffects;

	public CompoundCollisionEffect(final CollisionEffect[] collisionEffects) {
		this.collisionEffects = collisionEffects;
	}

	@Override
	public void onEntityCollision(final BlockState state, final World world, final BlockPos pos, final Entity entity) {
		for (final CollisionEffect ce : collisionEffects) ce.onEntityCollision(state, world, pos, entity);
	}

}
