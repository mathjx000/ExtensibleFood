package mathjx.extensiblefood.block;

import mathjx.extensiblefood.block.collision.CollisionEffect;
import mathjx.extensiblefood.block.condition.BlockStayCondition;
import mathjx.extensiblefood.block.particle.ParticleEmission;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.CropBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager.Builder;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;

public final class CropFoodBlock extends CropBlock {

	private final int maxAge;
	private final boolean fertilizable;
	private final int minLightToGrow, maxLightToGrow;
	public Item seedItem;

	private final IntProperty propertyAge;
	private final VoxelShape[] ageToShape;
	private final BlockStayCondition stayCondition;
	private final ParticleEmission particleEmission;
	private final CollisionEffect collisionEffect;

	public CropFoodBlock(final Settings settings, final int maxAge, final boolean fertilizable,
			final int minLightToGrow, final int maxLightToGrow, final IntProperty propertyAge,
			final VoxelShape[] ageToShape, final BlockStayCondition stayCondition,
			final ParticleEmission particleEmission, final CollisionEffect collisionEffect) {
		super(settings);

		this.maxAge = maxAge;
		this.fertilizable = fertilizable;
		this.minLightToGrow = minLightToGrow;
		this.maxLightToGrow = maxLightToGrow;
		this.propertyAge = propertyAge;
		this.ageToShape = ageToShape;
		this.stayCondition = stayCondition;
		this.particleEmission = particleEmission;
		this.collisionEffect = collisionEffect;
	}

	@Override
	public int getMaxAge() {
		return maxAge;
	}

	@Override
	public IntProperty getAgeProperty() {
		return propertyAge == null ? BlockParser.BRUH : propertyAge;
	}

	@Override
	protected ItemConvertible getSeedsItem() {
		return seedItem;
	}

	@Override
	public void randomTick(final BlockState state, final ServerWorld world, final BlockPos pos, final Random random) {
		final int light = world.getBaseLightLevel(pos, 0);

		if (light >= minLightToGrow && light <= maxLightToGrow) {
			final int i = getAge(state);

			if (i < getMaxAge()) {
				final float f = getAvailableMoisture(this, world, pos);

				if (random.nextInt((int) (25.0F / f) + 1) == 0) world.setBlockState(pos, withAge(i + 1), 2);
			}
		}
	}

	@Override
	public VoxelShape getOutlineShape(final BlockState state, final BlockView world, final BlockPos pos,
			final ShapeContext context) {
		return ageToShape[state.get(getAgeProperty())];
	}

	@Override
	public void onEntityCollision(final BlockState state, final World world, final BlockPos pos, final Entity entity) {
		if (collisionEffect != null) collisionEffect.onEntityCollision(state, world, pos, entity);

		super.onEntityCollision(state, world, pos, entity);
	}

	@Override
	public boolean canPlaceAt(final BlockState state, final WorldView world, final BlockPos pos) {
		return stayCondition == null ? super.canPlaceAt(state, world, pos)
				: stayCondition.test(state, (World) world, pos);
	}

	@Override
	protected void appendProperties(final Builder<Block, BlockState> builder) {
		builder.add(BlockParser.BRUH);
	}

	@Override
	public boolean isFertilizable(final BlockView world, final BlockPos pos, final BlockState state,
			final boolean isClient) {
		return fertilizable && super.isFertilizable(world, pos, state, isClient);
	}

	@Override
	public void randomDisplayTick(final BlockState state, final World world, final BlockPos pos, final Random random) {
		if (particleEmission != null) particleEmission.spawn(world, pos, random);
	}

}
