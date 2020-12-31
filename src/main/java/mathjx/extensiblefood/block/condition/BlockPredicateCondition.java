package mathjx.extensiblefood.block.condition;

import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.block.BlockState;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.command.argument.BlockPredicateArgumentType.BlockPredicate;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;

public final class BlockPredicateCondition extends BlockStayCondition {

	private final Vec3i offset;
	private final BlockPredicate predicate;

	
	public BlockPredicateCondition(Vec3i offset, BlockPredicate predicate) {
		this.offset = offset;
		this.predicate = predicate;
	}


	@Override
	public boolean test(BlockState state, World world, BlockPos pos) {
		try {
			return predicate.create(world.getTagManager()).test(new CachedBlockPosition(world, pos.add(offset), false));
		} catch (CommandSyntaxException e) {
			e.printStackTrace();
			return false;
		}
	}

}
