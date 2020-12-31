package mathjx.extensiblefood.block;

import com.mojang.datafixers.util.Pair;

import mathjx.extensiblefood.block.condition.BlockStayCondition;
import mathjx.extensiblefood.food.ExtendedFoodComponent;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
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
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;

public final class ExtensibleFoodBlock extends Block {

	private final int maxBites;
	private final IntProperty propertyBites;
	private final VoxelShape[] bitesToShape;
	private final boolean comparatorEnabled;
	private final ExtendedFoodComponent foodComponent;
	private final BlockStayCondition stayCondition;

	ExtensibleFoodBlock(Settings settings, int maxBites, IntProperty propertyBites, VoxelShape[] bitesToShape,
			boolean comparatorEnabled, ExtendedFoodComponent foodComponent, BlockStayCondition condition) {
		super(settings);

		this.maxBites = maxBites;
		this.propertyBites = propertyBites;
		this.bitesToShape = bitesToShape;
		this.comparatorEnabled = comparatorEnabled;
		this.foodComponent = foodComponent;
		this.stayCondition = condition;
	}

	@Override
	public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return bitesToShape[state.get(propertyBites)];
	}

	@Override
	public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand,
			BlockHitResult hit) {
		if (world.isClient) {
			ItemStack stack = player.getStackInHand(hand);

			if (this.tryEat(world, pos, state, player).isAccepted()) {
				return ActionResult.SUCCESS;
			}

			if (stack.isEmpty()) return ActionResult.CONSUME;
		}

		return this.tryEat(world, pos, state, player);
	}

	private ActionResult tryEat(WorldAccess world, BlockPos pos, BlockState state, PlayerEntity player) {
		if (!player.canConsume(foodComponent.food.isAlwaysEdible())) {
			return ActionResult.PASS;
		} else {
			// player.incrementStat(Stats.EAT_CAKE_SLICE);
			player.getHungerManager().add(foodComponent.food.getHunger(), foodComponent.food.getSaturationModifier());

			if (!world.isClient()) {
				for (Pair<StatusEffectInstance, Float> pair : foodComponent.food.getStatusEffects()) {
					if (pair.getSecond() >= player.getRandom().nextFloat()) {
						player.addStatusEffect(pair.getFirst());
					}
				}
			} else if (foodComponent.eatSound != null) player.playSound(foodComponent.eatSound, 1f, 1f);

			int i = state.get(propertyBites);
			if (i < maxBites) {
				world.setBlockState(pos, state.with(propertyBites, i + 1), 3);
			} else world.removeBlock(pos, false);

			return ActionResult.SUCCESS;
		}
	}

	@Override
	public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState newState,
			WorldAccess world, BlockPos pos, BlockPos posFrom) {
		return canPlaceAt(state, world, pos)
				? super.getStateForNeighborUpdate(state, direction, newState, world, pos, posFrom)
				: Blocks.AIR.getDefaultState();
	}

	@Override
	public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
		return stayCondition == null ? true : stayCondition.test(state, (World) world, pos);
	}

	@Override
	protected void appendProperties(Builder<Block, BlockState> builder) {
		builder.add(BlockDeserializer.BRUH);
	}

	@Override
	public int getComparatorOutput(BlockState state, World world, BlockPos pos) {
		return comparatorEnabled ? (int) (state.get(propertyBites).floatValue() / (float) maxBites * 15F) : 0;
	}

	@Override
	public boolean hasComparatorOutput(BlockState state) {
		return comparatorEnabled;
	}

	@Override
	public boolean canPathfindThrough(BlockState state, BlockView world, BlockPos pos, NavigationType type) {
		return false;
	}

}
