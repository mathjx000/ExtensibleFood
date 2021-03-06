package mathjx.extensiblefood.block.condition;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

abstract class CompoundBlockStayCondition extends BlockStayCondition {

	protected final BlockStayCondition[] conditions;

	CompoundBlockStayCondition(BlockStayCondition[] conditions) {
		this.conditions = conditions;
	}

	@Override
	public abstract boolean test(BlockState state, World world, BlockPos pos);

	static class AND_Condition extends CompoundBlockStayCondition {

		AND_Condition(BlockStayCondition[] conditions) {
			super(conditions);
		}

		@Override
		public boolean test(BlockState state, World world, BlockPos pos) {
			for (BlockStayCondition condition : conditions) if (!condition.test(state, world, pos)) return false;
			return true;
		}

	}

	static class OR_Condition extends CompoundBlockStayCondition {

		OR_Condition(BlockStayCondition[] conditions) {
			super(conditions);
		}

		@Override
		public boolean test(BlockState state, World world, BlockPos pos) {
			for (BlockStayCondition condition : conditions) if (condition.test(state, world, pos)) return true;
			return false;
		}

	}

	static class XOR_Condition extends CompoundBlockStayCondition {

		XOR_Condition(BlockStayCondition[] conditions) {
			super(conditions);
		}

		@Override
		public boolean test(BlockState state, World world, BlockPos pos) {
			boolean bool = false;

			for (BlockStayCondition condition : conditions) {
				if (condition.test(state, world, pos)) {
					if (bool) {
						return false;
					} else bool = true;
				}
			}
			
			return bool;
		}

	}

}
