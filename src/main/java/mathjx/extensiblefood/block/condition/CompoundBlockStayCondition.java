package mathjx.extensiblefood.block.condition;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

abstract class CompoundBlockStayCondition extends BlockStayCondition {

	protected final BlockStayCondition[] conditions;

	CompoundBlockStayCondition(final BlockStayCondition[] conditions) {
		this.conditions = conditions;
	}

	@Override
	public abstract boolean test(BlockState state, World world, BlockPos pos);

	static class AND_Condition extends CompoundBlockStayCondition {

		AND_Condition(final BlockStayCondition[] conditions) {
			super(conditions);
		}

		@Override
		public boolean test(final BlockState state, final World world, final BlockPos pos) {
			for (final BlockStayCondition condition : conditions) if (!condition.test(state, world, pos)) return false;
			return true;
		}

	}

	static class OR_Condition extends CompoundBlockStayCondition {

		OR_Condition(final BlockStayCondition[] conditions) {
			super(conditions);
		}

		@Override
		public boolean test(final BlockState state, final World world, final BlockPos pos) {
			for (final BlockStayCondition condition : conditions) if (condition.test(state, world, pos)) return true;
			return false;
		}

	}

	static class XOR_Condition extends CompoundBlockStayCondition {

		XOR_Condition(final BlockStayCondition[] conditions) {
			super(conditions);
		}

		@Override
		public boolean test(final BlockState state, final World world, final BlockPos pos) {
			boolean bool = false;

			for (final BlockStayCondition condition : conditions) {
				if (condition.test(state, world, pos)) {
					if (bool) {
						return false;
					} else bool = true;
				}
			}

			return bool;
		}

	}

	static class NAND_Condition extends AND_Condition {

		NAND_Condition(BlockStayCondition[] conditions) {
			super(conditions);
		}

		@Override
		public boolean test(BlockState state, World world, BlockPos pos) {
			return !super.test(state, world, pos);
		}

	}

}
