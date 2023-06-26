package mathjx.extensiblefood.block;

import com.mojang.datafixers.util.Pair;

import mathjx.extensiblefood.block.collision.CollisionEffect;
import mathjx.extensiblefood.block.condition.BlockStayCondition;
import mathjx.extensiblefood.block.particle.ParticleEmission;
import mathjx.extensiblefood.food.ExtendedFoodComponent;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateManager.Builder;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;

public final class ConsumableFoodBlock extends Block {

	private final int maxBites;
	private final IntProperty propertyBites;
	private final VoxelShape[] bitesToShape;
	private final boolean comparatorEnabled;
	private final ExtendedFoodComponent foodComponent;
	private final BlockStayCondition stayCondition;
	private final ParticleEmission particleEmission;
	private final CollisionEffect collisionEffect;

	ConsumableFoodBlock(final Settings settings, final int maxBites, final IntProperty propertyBites,
			final VoxelShape[] bitesToShape, final boolean comparatorEnabled, final ExtendedFoodComponent foodComponent,
			final BlockStayCondition condition, final ParticleEmission particleEmission,
			final CollisionEffect collisionEffect) {
		super(settings);

		this.maxBites = maxBites;
		this.propertyBites = propertyBites;
		this.bitesToShape = bitesToShape;
		this.comparatorEnabled = comparatorEnabled;
		this.foodComponent = foodComponent;
		stayCondition = condition;
		this.particleEmission = particleEmission;
		this.collisionEffect = collisionEffect;
	}

	@Override
	public VoxelShape getOutlineShape(final BlockState state, final BlockView world, final BlockPos pos,
			final ShapeContext context) {
		return bitesToShape[state.get(propertyBites)];
	}

	@Override
	public ActionResult onUse(final BlockState state, final World world, final BlockPos pos, final PlayerEntity player,
			final Hand hand, final BlockHitResult hit) {
		if (world.isClient) {
			final ItemStack stack = player.getStackInHand(hand);

			if (tryEat(world, pos, state, player).isAccepted()) return ActionResult.SUCCESS;
			if (stack.isEmpty()) return ActionResult.CONSUME;
		}

		return tryEat(world, pos, state, player);
	}

	private ActionResult tryEat(final WorldAccess world, final BlockPos pos, final BlockState state,
			final PlayerEntity player) {
		if (!player.canConsume(foodComponent.food.isAlwaysEdible())) return ActionResult.PASS;
		else {
			// player.incrementStat(Stats.EAT_CAKE_SLICE);
			player.getHungerManager().add(foodComponent.food.getHunger(), foodComponent.food.getSaturationModifier());

			if (!world.isClient()) {
				for (final Pair<StatusEffectInstance, Float> pair : foodComponent.food.getStatusEffects()) {
					if (pair.getSecond() >= player.getRandom().nextFloat()) player.addStatusEffect(pair.getFirst());
				}
			} else if (foodComponent.eatSound != null) player.playSound(foodComponent.eatSound, 1f, 1f);

			final int i = state.get(propertyBites);
			if (i < maxBites) world.setBlockState(pos, state.with(propertyBites, i + 1), 3);
			else world.removeBlock(pos, false);

			return ActionResult.SUCCESS;
		}
	}

	@SuppressWarnings("deprecation")
	@Override
	public BlockState getStateForNeighborUpdate(final BlockState state, final Direction direction,
			final BlockState newState, final WorldAccess world, final BlockPos pos, final BlockPos posFrom) {
		return canPlaceAt(state, world, pos)
				? super.getStateForNeighborUpdate(state, direction, newState, world, pos, posFrom)
				: Blocks.AIR.getDefaultState();
	}

	@Override
	public boolean canPlaceAt(final BlockState state, final WorldView world, final BlockPos pos) {
		return stayCondition == null ? true : stayCondition.test(state, (World) world, pos);
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void appendProperties(final Builder<Block, BlockState> builder) {
		builder.add(BlockParser.BRUH);
	}

	@Override
	public int getComparatorOutput(final BlockState state, final World world, final BlockPos pos) {
		return comparatorEnabled ? (int) (state.get(propertyBites).floatValue() / maxBites * 15F) : 0;
	}

	@Override
	public boolean hasComparatorOutput(final BlockState state) {
		return comparatorEnabled;
	}

	@Override
	public boolean canPathfindThrough(final BlockState state, final BlockView world, final BlockPos pos,
			final NavigationType type) {
		return false;
	}

	@Override
	public void randomDisplayTick(final BlockState state, final World world, final BlockPos pos, final Random random) {
		if (particleEmission != null) particleEmission.spawn(world, pos, random);
	}

	@Override
	public void onEntityCollision(final BlockState state, final World world, final BlockPos pos, final Entity entity) {
		if (collisionEffect != null) collisionEffect.onEntityCollision(state, world, pos, entity);
	}

}
