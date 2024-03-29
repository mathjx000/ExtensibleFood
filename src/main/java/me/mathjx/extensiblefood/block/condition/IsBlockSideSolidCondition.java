package me.mathjx.extensiblefood.block.condition;

import net.minecraft.block.BlockState;
import net.minecraft.block.SideShapeType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public class IsBlockSideSolidCondition extends BlockStayCondition {

	private final Direction side;
	private final SideShapeType shapeType;

	public IsBlockSideSolidCondition(final Direction side, final SideShapeType shapeType) {
		this.side = side;
		this.shapeType = shapeType;
	}

	@Override
	public boolean test(final BlockState state, final World world, BlockPos pos) {
		pos = pos.offset(side);
		return world.getBlockState(pos).isSideSolid(world, pos, side.getOpposite(), shapeType);
	}

}
