package mathjx.extensiblefood.block.particle;

import net.minecraft.util.math.random.Random;

import net.minecraft.particle.ParticleEffect;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public final class EffectParticleEmission extends ParticleEmission {

	private final Box[] boxes;
	private final ParticleEffect effect;

	private final float batchChance;
	private final int perBatchAmount;
	private final float chance;
	private final boolean important, alwaysSpawn;
	private final Vec3d baseVelocity, randVelocity;

	public EffectParticleEmission(final Box[] boxes, final ParticleEffect effect, final float batchChance,
			final int perBatchAmount, final float chance, final boolean important, final boolean alwaysSpawn,
			final Vec3d baseVelocity, final Vec3d randVelocity) {
		this.boxes = boxes;
		this.effect = effect;
		this.batchChance = batchChance;
		this.perBatchAmount = perBatchAmount;
		this.chance = chance;
		this.important = important;
		this.alwaysSpawn = alwaysSpawn;
		this.baseVelocity = baseVelocity;
		this.randVelocity = randVelocity;
	}

	@Override
	public void spawn(final World world, final BlockPos pos, final Random random) {
		if (random.nextFloat() <= batchChance) {
			for (int i = 0; i < perBatchAmount; i++) {
				if (random.nextFloat() <= chance) {
					final Box box = boxes[random.nextInt(boxes.length)];

					final double x = pos.getX() + box.minX + random.nextDouble() * (box.maxX - box.minX);
					final double y = pos.getY() + box.minY + random.nextDouble() * (box.maxY - box.minY);
					final double z = pos.getZ() + box.minZ + random.nextDouble() * (box.maxZ - box.minZ);

					final double dx = baseVelocity.x + random.nextDouble() * randVelocity.x;
					final double dy = baseVelocity.y + random.nextDouble() * randVelocity.y;
					final double dz = baseVelocity.z + random.nextDouble() * randVelocity.z;

					if (important) world.addImportantParticle(effect, x, y, z, dx, dy, dz);
					else world.addParticle(effect, alwaysSpawn, x, y, z, dx, dy, dz);
				}
			}
		}
	}

}
