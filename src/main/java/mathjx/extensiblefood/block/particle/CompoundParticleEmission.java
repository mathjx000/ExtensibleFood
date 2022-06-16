package mathjx.extensiblefood.block.particle;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

public final class CompoundParticleEmission extends ParticleEmission {

	private final ParticleEmission[] emissions;

	public CompoundParticleEmission(final ParticleEmission... emissions) {
		this.emissions = emissions;
	}

	@Override
	public void spawn(final World world, final BlockPos pos, final Random random) {
		for (int i = 0; i < emissions.length; i++) emissions[i].spawn(world, pos, random);
	}

}
